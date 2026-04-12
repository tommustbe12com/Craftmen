package com.tommustbe12.craftmen.ffa;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.customkit.CustomKit;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.party.ffa.PartyFfaSession;
import com.tommustbe12.craftmen.cosmetics.CosmeticsApplier;
import com.tommustbe12.craftmen.player.PlayerReset;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class FfaManager implements Listener {

    private static final long RESET_EVERY_MILLIS = 15L * 60L * 1000L;
    private static final int MAX_PLAYERS_PER_INSTANCE = 10;

    private final Craftmen plugin;
    private final File ffaFolder = new File("plugins/Craftmen/arenas/FFA");
    private final File ffaCustomKitFolder = new File("plugins/Craftmen/arenas/FFA_CustomKits");

    // Public FFA instances per game name (Sword, Axe, ...)
    private final Map<String, List<FfaInstance>> publicInstances = new HashMap<>();

    // Custom kit instances are 1-per-kit (existing behavior)
    private final Map<UUID, FfaInstance> customKitInstances = new HashMap<>(); // kitId -> instance

    // Private party instances (1 active per party)
    private final Map<UUID, FfaInstance> privatePartyInstances = new HashMap<>(); // partyId -> instance
    private final Map<UUID, PartyFfaSession> partySessions = new HashMap<>(); // partyId -> session

    private final Map<UUID, UUID> playerInstance = new HashMap<>(); // player -> instanceId
    private final Map<UUID, FfaInstance> instancesById = new HashMap<>();

    public FfaManager(Craftmen plugin) {
        this.plugin = plugin;
        if (!ffaFolder.exists()) ffaFolder.mkdirs();
        if (!ffaCustomKitFolder.exists()) ffaCustomKitFolder.mkdirs();

        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 20L, 20L * 5L); // every 5s
    }

    public int getPlayersInFfa(Game game) {
        List<FfaInstance> list = publicInstances.get(game.getName());
        if (list == null) return 0;
        return list.stream().mapToInt(inst -> inst.players.size()).sum();
    }

    public boolean isInFfa(Player player) {
        return playerInstance.containsKey(player.getUniqueId());
    }

    public Game getGame(Player player) {
        if (player == null) return null;
        UUID instId = playerInstance.get(player.getUniqueId());
        if (instId == null) return null;
        FfaInstance inst = instancesById.get(instId);
        return inst == null ? null : inst.game;
    }

    public boolean allowDamage(Player damager, Player damaged) {
        UUID i1 = playerInstance.get(damager.getUniqueId());
        if (i1 == null) return false;
        UUID i2 = playerInstance.get(damaged.getUniqueId());
        if (i2 == null || !i1.equals(i2)) return false;

        FfaInstance inst = instancesById.get(i1);
        if (inst != null && inst.isPrivate) {
            PartyFfaSession session = getSession(inst);
            if (session != null) {
                if (session.spectators.contains(damager.getUniqueId())) return false;
                if (session.spectators.contains(damaged.getUniqueId())) return false;
            }
        }
        return true;
    }

    // --------------------
    // Joining
    // --------------------

    public void join(Player player, Game game) {
        if (player == null || game == null) return;

        if (Craftmen.get().getEndFightManager().isInGame(player)) {
            player.sendMessage(ChatColor.RED + "You cannot join FFA while in End Fight.");
            return;
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null || profile.getState() != PlayerState.LOBBY) {
            player.sendMessage(ChatColor.RED + "You can only join FFA from the hub.");
            return;
        }

        // Ensure match-end flight/invulnerability never leaks into FFA.
        PlayerReset.clearTransientState(player);

        leave(player, false);

        FfaInstance instance = pickOrCreatePublicInstance(game);
        if (instance == null) {
            player.sendMessage(ChatColor.RED + "No FFA schematics found in arenas/FFA/.");
            return;
        }

        if (instance.players.size() >= MAX_PLAYERS_PER_INSTANCE) {
            // should never happen because pickOrCreate enforces it, but keep safe
            player.sendMessage(ChatColor.RED + "That FFA is full. Try again.");
            return;
        }

        joinIntoInstance(player, instance);

        game.applyLoadout(player);
        player.updateInventory();

        broadcast(instance, ChatColor.GREEN + player.getName() + " joined FFA (" + instance.players.size() + "/" + MAX_PLAYERS_PER_INSTANCE + ").");
    }

    public void joinCustomKit(Player player, CustomKit kit) {
        if (player == null || kit == null) return;

        if (Craftmen.get().getEndFightManager().isInGame(player)) {
            player.sendMessage(ChatColor.RED + "You cannot join FFA while in End Fight.");
            return;
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null || profile.getState() != PlayerState.LOBBY) {
            player.sendMessage(ChatColor.RED + "You can only join FFA from the hub.");
            return;
        }

        PlayerReset.clearTransientState(player);

        leave(player, false);

        FfaInstance instance = customKitInstances.get(kit.getId());
        if (instance == null) {
            instance = createCustomKitInstance(kit);
            if (instance == null) {
                player.sendMessage(ChatColor.RED + "No FFA schematics found in arenas/FFA_CustomKits/.");
                return;
            }
            customKitInstances.put(kit.getId(), instance);
            instancesById.put(instance.id, instance);
        }

        if (instance.players.size() >= MAX_PLAYERS_PER_INSTANCE) {
            player.sendMessage(ChatColor.RED + "This Custom Kit FFA is full (10).");
            return;
        }

        joinIntoInstance(player, instance);

        kit.getKit().apply(player);
        Craftmen.get().getArmorTrimManager().apply(player);
        player.updateInventory();

        broadcast(instance, ChatColor.GREEN + player.getName() + " joined Custom Kit FFA (" + instance.players.size() + "/" + MAX_PLAYERS_PER_INSTANCE + ").");
    }

    /**
     * Creates (or reuses) a private FFA instance for a party and moves all online members into it.
     */
    public void joinPrivateParty(UUID partyId, Collection<UUID> partyMembers, Game game) {
        joinPrivateParty(partyId, partyMembers, game, 1);
    }

    public void joinPrivateParty(UUID partyId, Collection<UUID> partyMembers, Game game, int rounds) {
        if (partyId == null || partyMembers == null || game == null) return;

        PartyFfaSession session = partySessions.get(partyId);
        if (session == null) {
            session = new PartyFfaSession(partyId, game, rounds);
            partySessions.put(partyId, session);
        }

        // Reuse existing private instance if present; otherwise create a new one.
        FfaInstance instance = privatePartyInstances.get(partyId);
        if (instance == null) {
            instance = createInstance(game);
            if (instance == null) return;
            instance.isPrivate = true;
            instance.ownerPartyId = partyId;
            instance.allowedPlayers = new HashSet<>(partyMembers);
            privatePartyInstances.put(partyId, instance);
            instancesById.put(instance.id, instance);
        } else {
            instance.allowedPlayers = new HashSet<>(partyMembers);
        }

        boolean startingNow = !session.running;
        if (startingNow) {
            session.running = true;
            session.currentRound = 0;
            session.roundWins.clear();
        }

        // Join online members. If the session is already running, late joiners become spectators until the round ends.
        for (UUID memberId : partyMembers) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) continue;
            if (Craftmen.get().getEndFightManager().isInGame(member)) continue;

            Profile profile = Craftmen.get().getProfileManager().getProfile(member);
            if (profile == null || profile.getState() != PlayerState.LOBBY) continue;

            PlayerReset.clearTransientState(member);
            leave(member, false);

            if (instance.players.size() >= MAX_PLAYERS_PER_INSTANCE) break;
            if (instance.allowedPlayers != null && !instance.allowedPlayers.contains(memberId)) continue;

            joinIntoInstance(member, instance);
            if (!startingNow) {
                setSpectator(member, session);
                member.sendMessage(ChatColor.YELLOW + "You joined mid-FFA and are spectating until the round ends.");
            }
        }

        if (startingNow) {
            startNextRound(instance, session);
        }

        broadcast(instance, ChatColor.YELLOW + "Party FFA running (" + instance.players.size() + "/" + MAX_PLAYERS_PER_INSTANCE + ").");
    }

    public boolean hasPrivatePartyFfa(UUID partyId) {
        return partyId != null && privatePartyInstances.containsKey(partyId);
    }

    public void endPrivatePartyFfa(UUID partyId) {
        if (partyId == null) return;
        FfaInstance inst = privatePartyInstances.get(partyId);
        if (inst == null) return;
        partySessions.remove(partyId);

        var party = Craftmen.get().getPartyManager().getPartyById(partyId);
        UUID leaderId = party == null ? null : party.getLeader();

        // Copy first because leave() mutates sets/maps.
        Set<UUID> members = new HashSet<>(inst.players);
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                leave(p, true);
            } else {
                // offline cleanup
                playerInstance.remove(uuid);
                inst.players.remove(uuid);
            }
        }

        if (inst.players.isEmpty()) {
            destroyInstance(inst);
            instancesById.remove(inst.id);
            privatePartyInstances.remove(partyId);
        }

        if (leaderId != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player leader = Bukkit.getPlayer(leaderId);
                if (leader != null) Craftmen.get().getPartyFfaMenu().givePlayAgain(leader);
            }, 2L);
        }
    }

    private PartyFfaSession getSession(FfaInstance inst) {
        if (inst == null || !inst.isPrivate || inst.ownerPartyId == null) return null;
        return partySessions.get(inst.ownerPartyId);
    }

    private void startNextRound(FfaInstance inst, PartyFfaSession session) {
        clearDroppedItems(inst);
        session.currentRound++;
        session.alive.clear();
        session.spectators.clear();

        for (UUID uuid : new HashSet<>(inst.players)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;

            session.alive.add(uuid);
            setParticipant(p);
            inst.game.applyLoadout(p);
            p.updateInventory();
            teleportToSafeSpawn(p, inst);
        }

        broadcast(inst, ChatColor.GOLD + "Round " + session.currentRound + "/" + session.totalRounds + " started!");
        for (UUID uuid : session.alive) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.6f);
        }
    }

    private void setParticipant(Player p) {
        p.setGameMode(org.bukkit.GameMode.SURVIVAL);
        p.setInvulnerable(false);
        p.setAllowFlight(false);
        p.setFlying(false);
    }

    private void setSpectator(Player p, PartyFfaSession session) {
        session.spectators.add(p.getUniqueId());
        p.setGameMode(org.bukkit.GameMode.SPECTATOR);
        p.setInvulnerable(true);
        p.setAllowFlight(true);
        p.setFlying(true);
    }

    private void handlePrivateDeath(FfaInstance inst, PartyFfaSession session, Player dead) {
        UUID id = dead.getUniqueId();
        session.alive.remove(id);
        setSpectator(dead, session);
        teleportToSafeSpawn(dead, inst);

        if (session.alive.size() > 1) return;

        UUID winnerId = session.alive.stream().findFirst().orElse(null);
        Player winner = winnerId == null ? null : Bukkit.getPlayer(winnerId);
        if (winner != null) {
            session.roundWins.put(winnerId, session.roundWins.getOrDefault(winnerId, 0) + 1);
            broadcast(inst, ChatColor.GREEN + winner.getName() + " won round " + session.currentRound + "!");
            winner.playSound(winner.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } else {
            broadcast(inst, ChatColor.YELLOW + "Round " + session.currentRound + " ended.");
        }

        if (session.currentRound >= session.totalRounds) {
            UUID top = null;
            int best = -1;
            for (var entry : session.roundWins.entrySet()) {
                if (entry.getValue() > best) {
                    best = entry.getValue();
                    top = entry.getKey();
                }
            }
            Player finalWinner = top == null ? null : Bukkit.getPlayer(top);
            if (finalWinner != null) {
                broadcast(inst, ChatColor.GOLD + finalWinner.getName() + " won the FFA!");
                finalWinner.playSound(finalWinner.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            } else {
                broadcast(inst, ChatColor.GOLD + "FFA ended!");
            }

            UUID partyId = inst.ownerPartyId;
            if (partyId != null) {
                endPrivatePartyFfa(partyId);
            }
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> startNextRound(inst, session), 20L * 3L);
    }

    private void joinIntoInstance(Player player, FfaInstance instance) {
        instance.players.add(player.getUniqueId());
        playerInstance.put(player.getUniqueId(), instance.id);

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null) profile.setState(PlayerState.FFA_FIGHTING);

        teleportToSafeSpawn(player, instance);
    }

    private FfaInstance pickOrCreatePublicInstance(Game game) {
        List<FfaInstance> list = publicInstances.computeIfAbsent(game.getName(), k -> new ArrayList<>());

        for (FfaInstance inst : list) {
            if (inst.isPrivate) continue;
            if (inst.players.size() < MAX_PLAYERS_PER_INSTANCE) return inst;
        }

        FfaInstance created = createInstance(game);
        if (created == null) return null;
        list.add(created);
        instancesById.put(created.id, created);
        return created;
    }

    // --------------------
    // Leaving
    // --------------------

    public void leave(Player player, boolean message) {
        if (player == null) return;

        UUID instId = playerInstance.remove(player.getUniqueId());
        if (instId == null) return;

        FfaInstance inst = instancesById.get(instId);
        if (inst != null) {
            inst.players.remove(player.getUniqueId());
            broadcast(inst, ChatColor.RED + player.getName() + " left FFA (" + inst.players.size() + "/" + MAX_PLAYERS_PER_INSTANCE + ").");
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null) profile.setState(PlayerState.LOBBY);

        PlayerReset.resetToHub(player);
        if (message) player.sendMessage(ChatColor.RED + "Left FFA.");

        if (inst != null && inst.players.isEmpty()) {
            destroyInstance(inst);
            instancesById.remove(inst.id);

            if (inst.isPrivate && inst.ownerPartyId != null) {
                privatePartyInstances.remove(inst.ownerPartyId);
            } else {
                List<FfaInstance> list = publicInstances.get(inst.game.getName());
                if (list != null) {
                    list.removeIf(v -> v.id.equals(inst.id));
                    if (list.isEmpty()) publicInstances.remove(inst.game.getName());
                }
                customKitInstances.values().removeIf(v -> v.id.equals(inst.id));
            }
        }
    }

    // --------------------
    // Maintenance
    // --------------------

    private void tick() {
        long now = System.currentTimeMillis();

        for (List<FfaInstance> list : new ArrayList<>(publicInstances.values())) {
            for (FfaInstance inst : new ArrayList<>(list)) {
                if (inst.players.isEmpty()) continue;
                if (now - inst.lastResetAtMillis >= RESET_EVERY_MILLIS) {
                    resetInstance(inst);
                }
            }
        }

        for (FfaInstance inst : new ArrayList<>(customKitInstances.values())) {
            if (inst.players.isEmpty()) continue;
            if (now - inst.lastResetAtMillis >= RESET_EVERY_MILLIS) {
                resetInstance(inst);
            }
        }

        for (FfaInstance inst : new ArrayList<>(privatePartyInstances.values())) {
            if (inst.players.isEmpty()) continue;
            if (now - inst.lastResetAtMillis >= RESET_EVERY_MILLIS) {
                resetInstance(inst);
            }
        }
    }

    private FfaInstance createInstance(Game game) {
        File schem = pickRandomSchematic(game);
        if (schem == null) return null;

        World world = Bukkit.getWorld("world");
        if (world == null) return null;

        int[] dims = readSchematicDims(schem);
        int width = Math.max(1, dims[0]);
        int height = Math.max(1, dims[1]);
        int length = Math.max(1, dims[2]);

        Location origin = computePasteOrigin(world, schem, width, length);
        FfaInstance inst = new FfaInstance(UUID.randomUUID(), game, schem, origin, width, height, length);
        paste(inst);
        inst.lastResetAtMillis = System.currentTimeMillis();
        return inst;
    }

    private FfaInstance createCustomKitInstance(CustomKit kit) {
        File schem = pickRandomSchematicFrom(ffaCustomKitFolder);
        if (schem == null) return null;

        World world = Bukkit.getWorld("world");
        if (world == null) return null;

        int[] dims = readSchematicDims(schem);
        int width = Math.max(1, dims[0]);
        int height = Math.max(1, dims[1]);
        int length = Math.max(1, dims[2]);

        Location origin = computePasteOrigin(world, schem, width, length);
        Game dummy = Craftmen.get().getGameManager().getGame("Sword");
        if (dummy == null && !Craftmen.get().getGameManager().getGames().isEmpty()) {
            dummy = Craftmen.get().getGameManager().getGames().iterator().next();
        }
        if (dummy == null) return null;

        FfaInstance inst = new FfaInstance(UUID.randomUUID(), dummy, schem, origin, width, height, length);
        paste(inst);
        inst.lastResetAtMillis = System.currentTimeMillis();
        return inst;
    }

    private void resetInstance(FfaInstance inst) {
        clearDroppedItems(inst);
        clear(inst);

        File next = pickRandomSchematic(inst.game);
        if (next == null) return;

        inst.lastResetAtMillis = System.currentTimeMillis();
        inst.minCorner = null;
        inst.maxCorner = null;
        inst.players.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

        int[] dims = readSchematicDims(next);
        inst.width = Math.max(1, dims[0]);
        inst.height = Math.max(1, dims[1]);
        inst.length = Math.max(1, dims[2]);

        inst.pasteOrigin = computePasteOrigin(inst.pasteOrigin.getWorld(), next, inst.width, inst.length);
        inst.schematicFile = next;

        paste(inst);

        for (UUID uuid : inst.players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            teleportToSafeSpawn(p, inst);
            inst.game.applyLoadout(p);
            p.updateInventory();
            p.sendMessage(ChatColor.YELLOW + "FFA arena refreshed.");
        }
    }

    private void destroyInstance(FfaInstance inst) {
        clearDroppedItems(inst);
        clear(inst);
    }

    private void broadcast(FfaInstance inst, String msg) {
        for (UUID uuid : inst.players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }

    // --------------------
    // WorldEdit helpers
    // --------------------

    private File pickRandomSchematic(Game game) {
        return pickRandomSchematicFrom(ffaFolder);
    }

    private File pickRandomSchematicFrom(File folder) {
        File[] candidates = listSchems(folder);
        if (candidates == null || candidates.length == 0) return null;
        return candidates[ThreadLocalRandom.current().nextInt(candidates.length)];
    }

    private static File[] listSchems(File dir) {
        if (dir == null || !dir.exists()) return null;
        return dir.listFiles((d, name) -> name.endsWith(".schem"));
    }

    private Location computePasteOrigin(World world, File schematicFile, int width, int length) {
        var wb = world.getWorldBorder();
        Location center = wb.getCenter();
        int half = (int) Math.floor(wb.getSize() / 2.0);

        int margin = 256;
        int minX = center.getBlockX() - half + margin;
        int maxX = center.getBlockX() + half - margin - width;
        int minZ = center.getBlockZ() - half + margin;
        int maxZ = center.getBlockZ() + half - margin - length;

        Location hub = Craftmen.get().getHubLocation();
        int minDistFromHub = 2000;
        int buffer = 64;

        for (int tries = 0; tries < 600; tries++) {
            int x = ThreadLocalRandom.current().nextInt(minX, Math.max(minX + 1, maxX));
            int z = ThreadLocalRandom.current().nextInt(minZ, Math.max(minZ + 1, maxZ));
            int y = Math.max(world.getMinHeight() + 5, world.getHighestBlockYAt(x, z) + 5);
            Location candidate = new Location(world, x, y, z);

            if (hub != null && hub.getWorld() != null && hub.getWorld().equals(world)) {
                if (candidate.distanceSquared(hub) < (double) minDistFromHub * minDistFromHub) continue;
            }

            if (!Craftmen.get().getArenaManager().isAreaEmpty(candidate, width, length, world)) continue;
            if (overlapsExisting(candidate, width, length, buffer)) continue;

            return candidate;
        }

        int x = maxX;
        int z = maxZ;
        int y = Math.max(world.getMinHeight() + 5, world.getHighestBlockYAt(x, z) + 5);
        return new Location(world, x, y, z);
    }

    private boolean overlapsExisting(Location origin, int width, int length, int buffer) {
        int ox1 = origin.getBlockX() - buffer;
        int oz1 = origin.getBlockZ() - buffer;
        int ox2 = origin.getBlockX() + width + buffer;
        int oz2 = origin.getBlockZ() + length + buffer;

        for (List<FfaInstance> list : publicInstances.values()) {
            for (FfaInstance inst : list) {
                if (inst.minCorner == null || inst.maxCorner == null) continue;
                int ix1 = Math.min(inst.minCorner.getBlockX(), inst.maxCorner.getBlockX()) - buffer;
                int iz1 = Math.min(inst.minCorner.getBlockZ(), inst.maxCorner.getBlockZ()) - buffer;
                int ix2 = Math.max(inst.minCorner.getBlockX(), inst.maxCorner.getBlockX()) + buffer;
                int iz2 = Math.max(inst.minCorner.getBlockZ(), inst.maxCorner.getBlockZ()) + buffer;

                boolean overlapX = ox1 <= ix2 && ox2 >= ix1;
                boolean overlapZ = oz1 <= iz2 && oz2 >= iz1;
                if (overlapX && overlapZ) return true;
            }
        }
        for (FfaInstance inst : customKitInstances.values()) {
            if (inst.minCorner == null || inst.maxCorner == null) continue;
            int ix1 = Math.min(inst.minCorner.getBlockX(), inst.maxCorner.getBlockX()) - buffer;
            int iz1 = Math.min(inst.minCorner.getBlockZ(), inst.maxCorner.getBlockZ()) - buffer;
            int ix2 = Math.max(inst.minCorner.getBlockX(), inst.maxCorner.getBlockX()) + buffer;
            int iz2 = Math.max(inst.minCorner.getBlockZ(), inst.maxCorner.getBlockZ()) + buffer;

            boolean overlapX = ox1 <= ix2 && ox2 >= ix1;
            boolean overlapZ = oz1 <= iz2 && oz2 >= iz1;
            if (overlapX && overlapZ) return true;
        }
        for (FfaInstance inst : privatePartyInstances.values()) {
            if (inst.minCorner == null || inst.maxCorner == null) continue;
            int ix1 = Math.min(inst.minCorner.getBlockX(), inst.maxCorner.getBlockX()) - buffer;
            int iz1 = Math.min(inst.minCorner.getBlockZ(), inst.maxCorner.getBlockZ()) - buffer;
            int ix2 = Math.max(inst.minCorner.getBlockX(), inst.maxCorner.getBlockX()) + buffer;
            int iz2 = Math.max(inst.minCorner.getBlockZ(), inst.maxCorner.getBlockZ()) + buffer;

            boolean overlapX = ox1 <= ix2 && ox2 >= ix1;
            boolean overlapZ = oz1 <= iz2 && oz2 >= iz1;
            if (overlapX && overlapZ) return true;
        }
        return false;
    }

    private int[] readSchematicDims(File file) {
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) return new int[]{128, 64, 128};
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                Clipboard clipboard = reader.read();
                return new int[]{
                        clipboard.getDimensions().getX(),
                        clipboard.getDimensions().getY(),
                        clipboard.getDimensions().getZ()
                };
            }
        } catch (Exception ignored) {
            return new int[]{128, 64, 128};
        }
    }

    private void paste(FfaInstance inst) {
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(inst.schematicFile);
            if (format == null) return;

            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(inst.schematicFile))) {
                clipboard = reader.read();
            }

            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(inst.pasteOrigin.getWorld());
            try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                BlockVector3 to = BlockVector3.at(
                        inst.pasteOrigin.getX(),
                        inst.pasteOrigin.getY(),
                        inst.pasteOrigin.getZ()
                );

                Operations.complete(holder.createPaste(session)
                        .to(to)
                        .ignoreAirBlocks(false)
                        .build());

                session.close();

                BlockVector3 clipOrigin = clipboard.getOrigin();
                BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                BlockVector3 max = clipboard.getRegion().getMaximumPoint();
                BlockVector3 offset = min.subtract(clipOrigin);

                Location minCorner = inst.pasteOrigin.clone().add(offset.getX(), offset.getY(), offset.getZ());
                Location maxCorner = minCorner.clone().add(max.getX() - min.getX(), max.getY() - min.getY(), max.getZ() - min.getZ());

                inst.minCorner = minCorner;
                inst.maxCorner = maxCorner;
                inst.lastResetAtMillis = System.currentTimeMillis();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to paste FFA schematic: " + e.getMessage());
        }
    }

    private void clear(FfaInstance inst) {
        if (inst.minCorner == null || inst.maxCorner == null) return;
        Craftmen.get().getArenaManager().removeArenaAtLocation("FFA", inst.minCorner, inst.maxCorner);
    }

    private void clearDroppedItems(FfaInstance inst) {
        if (inst == null) return;
        if (inst.minCorner == null || inst.maxCorner == null) return;
        World world = inst.minCorner.getWorld();
        if (world == null) return;

        int minX = Math.min(inst.minCorner.getBlockX(), inst.maxCorner.getBlockX());
        int maxX = Math.max(inst.minCorner.getBlockX(), inst.maxCorner.getBlockX());
        int minY = Math.min(inst.minCorner.getBlockY(), inst.maxCorner.getBlockY());
        int maxY = Math.max(inst.minCorner.getBlockY(), inst.maxCorner.getBlockY());
        int minZ = Math.min(inst.minCorner.getBlockZ(), inst.maxCorner.getBlockZ());
        int maxZ = Math.max(inst.minCorner.getBlockZ(), inst.maxCorner.getBlockZ());

        for (Item item : world.getEntitiesByClass(Item.class)) {
            Location l = item.getLocation();
            int x = l.getBlockX();
            int y = l.getBlockY();
            int z = l.getBlockZ();
            if (x < minX || x > maxX) continue;
            if (y < minY || y > maxY) continue;
            if (z < minZ || z > maxZ) continue;
            item.remove();
        }
    }

    private void teleportToSafeSpawn(Player player, FfaInstance inst) {
        Location loc = findSafeSpawn(inst);
        if (loc == null) loc = inst.pasteOrigin.clone().add(0, 5, 0);
        player.teleport(loc);
    }

    private Location findSafeSpawn(FfaInstance inst) {
        if (inst.minCorner == null || inst.maxCorner == null) return null;
        World world = inst.minCorner.getWorld();
        if (world == null) return null;

        int minX = Math.min(inst.minCorner.getBlockX(), inst.maxCorner.getBlockX());
        int maxX = Math.max(inst.minCorner.getBlockX(), inst.maxCorner.getBlockX());
        int minY = Math.min(inst.minCorner.getBlockY(), inst.maxCorner.getBlockY());
        int maxY = Math.max(inst.minCorner.getBlockY(), inst.maxCorner.getBlockY());
        int minZ = Math.min(inst.minCorner.getBlockZ(), inst.maxCorner.getBlockZ());
        int maxZ = Math.max(inst.minCorner.getBlockZ(), inst.maxCorner.getBlockZ());

        for (int tries = 0; tries < 250; tries++) {
            int x = ThreadLocalRandom.current().nextInt(minX + 1, maxX);
            int z = ThreadLocalRandom.current().nextInt(minZ + 1, maxZ);

            for (int y = maxY; y >= minY; y--) {
                Block below = world.getBlockAt(x, y, z);
                if (below.getType() == Material.AIR) continue;
                if (below.getType() == Material.BARRIER) continue;

                Block feet = world.getBlockAt(x, y + 1, z);
                Block head = world.getBlockAt(x, y + 2, z);
                if (feet.getType() != Material.AIR) continue;
                if (head.getType() != Material.AIR) continue;

                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }

    // --------------------
    // Events
    // --------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        leave(e.getPlayer(), false);
    }

    @EventHandler
    public void onFfaDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player damaged)) return;
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!isInFfa(damaged) && !isInFfa(damager)) return;
        if (!allowDamage(damager, damaged)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        UUID instId = playerInstance.get(dead.getUniqueId());
        if (instId == null) return;

        e.setDeathMessage(null);

        Player killer = dead.getKiller();
        FfaInstance inst = instancesById.get(instId);
        if (inst == null) return;

        if (killer != null) {
            CosmeticsApplier.applyKillDeath(killer, dead, dead.getLocation());
        }

        // Party private FFA: keep players in-instance as spectators; no stats.
        if (inst.isPrivate) {
            PartyFfaSession session = getSession(inst);
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Auto-respawn to avoid "Click to respawn" screen.
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (dead.isOnline()) dead.spigot().respawn();
                }, 1L);

                String msg;
                if (killer != null && allowDamage(killer, dead)) {
                    msg = ChatColor.RED + dead.getName() + ChatColor.GRAY + " was killed by " + ChatColor.GREEN + killer.getName();
                } else {
                    msg = ChatColor.RED + dead.getName() + ChatColor.GRAY + " died";
                }
                broadcast(inst, msg);
                if (session != null) handlePrivateDeath(inst, session, dead);
            });
            return;
        }

        if (killer != null && allowDamage(killer, dead)) {
            Profile pk = Craftmen.get().getProfileManager().getProfile(killer);
            if (pk != null) pk.addFfaKill();
        }
        Profile pd = Craftmen.get().getProfileManager().getProfile(dead);
        if (pd != null) pd.addFfaDeath();

        // FFA death should not respawn you in FFA: you leave and return to hub.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (dead.isOnline()) dead.spigot().respawn();
            }, 1L);

            String msg;
            if (killer != null && allowDamage(killer, dead)) {
                msg = ChatColor.RED + dead.getName() + ChatColor.GRAY + " was killed by " + ChatColor.GREEN + killer.getName();
            } else {
                msg = ChatColor.RED + dead.getName() + ChatColor.GRAY + " died";
            }
            broadcast(inst, msg);

            leave(dead, true);
        });
    }
}
