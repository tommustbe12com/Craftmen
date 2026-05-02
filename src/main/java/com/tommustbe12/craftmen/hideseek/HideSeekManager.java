package com.tommustbe12.craftmen.hideseek;

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
import com.tommustbe12.craftmen.arena.ArenaManager;
import com.tommustbe12.craftmen.player.PlayerReset;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class HideSeekManager {

    private static final String SCHEM_FOLDER_NAME = "hideSeek";
    private static final long PRE_START_WAIT_TICKS = 30L * 20L;
    private static final long HIDERS_HIDE_TICKS = 20L * 20L;
    private static final long TAUNT_COOLDOWN_MILLIS = 10_000L;
    private static final long GAME_TIME_LIMIT_TICKS = 10L * 60L * 20L;
    // Reserved paste region in the main world (kept far from other arenas).
    private static final int BASE_PASTE_X = 10000;
    private static final int BASE_PASTE_Y = 80;
    private static final int BASE_PASTE_Z = 10000;
    private static final int INSTANCE_SPACING = 600; // blocks between pastes (X axis)
    // Hardcoded spawn for the Hide & Seek spaceship map (absolute world coords).
    private static final double SPAWN_X = 10067.5;
    private static final double SPAWN_Y = 58.0;
    private static final double SPAWN_Z = 9909.5;

    private final JavaPlugin plugin;

    private final List<UUID> queuedPlayers = new ArrayList<>();
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Set<UUID> outPlayers = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Map<UUID, Long> lastTauntMillis = new HashMap<>();
    private boolean loadingArena = false;


    private UUID seeker;
    private boolean seekerReleased = false;

    private boolean starting = false;
    private boolean running = false;

    private World world;
    private Location spawn;
    private int worldId = 0;
    private Location pasteOrigin;
    private int pastedWidth;
    private int pastedHeight;
    private int pastedLength;
    private boolean arenaLoaded = false;

    private BukkitRunnable startTask;
    private BukkitRunnable nametagTask;
    private BukkitRunnable timeLimitTask;

    public HideSeekManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isInGame(Player player) {
        if (player == null) return false;
        UUID id = player.getUniqueId();
        return queuedPlayers.contains(id) || activePlayers.contains(id) || spectators.contains(id) || outPlayers.contains(id);
    }

    public boolean allowCombat(Player damager, Player damaged) {
        if (damager == null || damaged == null) return false;
        UUID a = damager.getUniqueId();
        UUID b = damaged.getUniqueId();
        return running && seekerReleased && activePlayers.contains(a) && activePlayers.contains(b);
    }

    public boolean isSeeker(Player player) {
        return player != null && seeker != null && seeker.equals(player.getUniqueId());
    }

    public void join(Player player) {
        if (player == null) return;

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null) return;

        if (profile.getState() != PlayerState.LOBBY) {
            player.sendMessage(ChatColor.RED + "You can't join Hide & Seek right now.");
            return;
        }

        PlayerReset.clearTransientState(player);
        player.getInventory().clear();

        ensureWorldAndSpawn();

        if (running) {
            addSpectator(player, ChatColor.GREEN + "Joined Hide & Seek as a spectator.");
            return;
        }

        if (spawn != null) {
            prepareForQueue(player);
            player.teleport(spawn);
        }

        UUID id = player.getUniqueId();
        if (queuedPlayers.contains(id)) {
            player.sendMessage(ChatColor.RED + "You are already queued for Hide & Seek.");
            return;
        }

        queuedPlayers.add(id);
        profile.setState(PlayerState.HIDESEEK_QUEUED);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Queued for Hide & Seek.");

        if (queuedPlayers.size() >= 2 && !starting && !running) {
            beginPreStartCountdown();
        } else if (queuedPlayers.size() < 2) {
            player.sendMessage(ChatColor.GRAY + "Need at least 2 players to start.");
        }
    }

    private void prepareForQueue(Player player) {
        if (player == null) return;
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getActivePotionEffects().forEach(pe -> player.removePotionEffect(pe.getType()));
    }

    public void remove(Player player, boolean forfeit) {
        if (player == null) return;
        UUID id = player.getUniqueId();

        queuedPlayers.remove(id);
        activePlayers.remove(id);
        outPlayers.remove(id);
        spectators.remove(id);
        lastTauntMillis.remove(id);

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null) profile.setState(PlayerState.LOBBY);
        PlayerReset.resetToHub(player);

        if (forfeit && running) {
            if (seeker != null && seeker.equals(id)) {
                broadcastToAll(ChatColor.RED + "The seeker left. Hiders win!");
                endGame();
                return;
            }
            checkWinCondition();
        }

        if (!running && starting && queuedPlayers.size() < 2) {
            cancelPreStartCountdown();
        }
    }

    private void beginPreStartCountdown() {
        starting = true;
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "Hide & Seek starting in 30 seconds!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Join from Queue -> Mini Games to play (spectates if already running).");
        broadcastToQueued(ChatColor.GRAY + "Starting in " + ChatColor.AQUA + "30" + ChatColor.GRAY + " seconds...");

        startTask = new BukkitRunnable() {
            @Override
            public void run() {
                startTask = null;
                if (queuedPlayers.size() < 2) {
                    starting = false;
                    broadcastToAll(ChatColor.RED + "Hide & Seek cancelled (not enough players).");
                    return;
                }
                startMatchNow();
            }
        };
        startTask.runTaskLater(plugin, PRE_START_WAIT_TICKS);

        scheduleCountdownMessage(20L * 20L, 10);
        scheduleCountdownMessage(25L * 20L, 5);
        scheduleCountdownMessage(26L * 20L, 4);
        scheduleCountdownMessage(27L * 20L, 3);
        scheduleCountdownMessage(28L * 20L, 2);
        scheduleCountdownMessage(29L * 20L, 1);
    }

    private void scheduleCountdownMessage(long delayTicks, int secondsLeft) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!starting) return;
                if (queuedPlayers.size() < 2) return;
                broadcastToQueued(ChatColor.GRAY + "Starting in " + ChatColor.AQUA + secondsLeft + ChatColor.GRAY + "...");
            }
        }.runTaskLater(plugin, delayTicks);
    }

    private void cancelPreStartCountdown() {
        if (startTask != null) {
            startTask.cancel();
            startTask = null;
        }
        starting = false;
        broadcastToQueued(ChatColor.RED + "Hide & Seek cancelled (not enough players).");
    }

    private void startMatchNow() {
        starting = false;
        running = true;
        seekerReleased = false;

        worldId++;
        ensureWorldAndSpawn();

        // Promote queued -> active
        activePlayers.clear();
        outPlayers.clear();
        spectators.clear();
        seeker = null;

        for (UUID id : new ArrayList<>(queuedPlayers)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            activePlayers.add(id);
        }
        queuedPlayers.clear();

        if (activePlayers.size() < 2) {
            broadcastToAll(ChatColor.RED + "Hide & Seek cancelled (not enough players).");
            endGame();
            return;
        }

        List<UUID> pick = new ArrayList<>(activePlayers);
        seeker = pick.get(new Random().nextInt(pick.size()));

        for (UUID id : activePlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            prepareForGame(p);
            p.teleport(spawn);
        }

        Player seekerPlayer = Bukkit.getPlayer(seeker);
        if (seekerPlayer != null) {
            seekerPlayer.sendMessage(ChatColor.RED + "You are the SEEKER!");
        }
        for (UUID id : activePlayers) {
            if (id.equals(seeker)) continue;
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(ChatColor.GREEN + "You are a HIDER!");
        }

        broadcastToAll(ChatColor.LIGHT_PURPLE + "Hide & Seek has started!");
        broadcastToAll(ChatColor.GRAY + "Hiders: you have 20 seconds to hide.");

        // Blind seeker while hiders run away
        if (seekerPlayer != null) {
            seekerPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (HIDERS_HIDE_TICKS + 40), 1, false, false, false));
            seekerPlayer.sendTitle(ChatColor.RED + "BLINDED", ChatColor.GRAY + "Wait for \"Ready or not...\"", 0, 60, 10);
        }

        // Give hider gadgets
        for (UUID id : activePlayers) {
            if (id.equals(seeker)) continue;
            Player p = Bukkit.getPlayer(id);
            if (p != null) giveHiderItems(p);
        }

        // Remove nametags (enforced periodically so nothing can override it).
        startNametagEnforcer();

        final UUID seekerIdSnapshot = seeker;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (seekerIdSnapshot == null) return;
                Player seekerP = Bukkit.getPlayer(seekerIdSnapshot);
                if (seekerP != null) {
                    seekerP.removePotionEffect(PotionEffectType.BLINDNESS);
                    seekerP.sendTitle(ChatColor.RED + "READY OR NOT", ChatColor.YELLOW + "HERE I COME!", 0, 60, 10);
                    seekerP.playSound(seekerP.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
                }
                seekerReleased = true;
                broadcastToAll(ChatColor.GRAY + "Seeker released!");
            }
        }.runTaskLater(plugin, HIDERS_HIDE_TICKS);

        if (timeLimitTask != null) timeLimitTask.cancel();
        timeLimitTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) return;
                broadcastToAll(ChatColor.GOLD + "Time's up! Hiders win!");
                endGame();
            }
        };
        timeLimitTask.runTaskLater(plugin, GAME_TIME_LIMIT_TICKS);
    }

    private void prepareForGame(Player player) {
        if (player == null) return;
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null) profile.setState(PlayerState.HIDESEEK_PLAYING);
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getActivePotionEffects().forEach(pe -> player.removePotionEffect(pe.getType()));
    }

    private void giveHiderItems(Player player) {
        ItemStack taunt = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = taunt.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Taunt Seeker");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right-click to reveal your",
                    ChatColor.GRAY + "approx location to the seeker.",
                    ChatColor.DARK_GRAY + "Cooldown: 10s"
            ));
            taunt.setItemMeta(meta);
        }
        player.getInventory().setItem(4, taunt);
    }

    private void addSpectator(Player player, String message) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        spectators.add(id);

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null) profile.setState(PlayerState.HIDESEEK_SPECTATING);

        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setGameMode(GameMode.SPECTATOR);

        if (world != null && spawn != null) {
            player.teleport(spawn.clone().add(0, 8, 0));
        }
        // Spectators should still not see hider nametags.
        applyHiddenNametagsForViewer(player);
        player.sendMessage(message);
    }

    public void handleTag(Player seekerPlayer, Player hider) {
        if (!running) return;
        if (seekerPlayer == null || hider == null) return;
        if (!isSeeker(seekerPlayer)) return;
        UUID hid = hider.getUniqueId();
        if (!activePlayers.contains(hid)) return;
        if (hid.equals(seeker)) return;

        outPlayers.add(hid);
        activePlayers.remove(hid);
        addSpectator(hider, ChatColor.RED + "You were tagged and are out!");

        broadcastToAll(ChatColor.GRAY + hider.getName() + " was tagged.");
        checkWinCondition();
    }

    private void checkWinCondition() {
        if (!running) return;
        int hidersLeft = 0;
        UUID lastHider = null;
        for (UUID id : activePlayers) {
            if (id.equals(seeker)) continue;
            hidersLeft++;
            lastHider = id;
        }

        if (hidersLeft <= 0) {
            Player seekerP = seeker == null ? null : Bukkit.getPlayer(seeker);
            if (seekerP != null) broadcastToAll(ChatColor.GOLD + seekerP.getName() + " (Seeker) wins!");
            else broadcastToAll(ChatColor.GOLD + "Seeker wins!");
            endGame();
            return;
        }

        if (hidersLeft == 1 && lastHider != null) {
            Player p = Bukkit.getPlayer(lastHider);
            if (p != null) p.sendMessage(ChatColor.YELLOW + "You're the last hider!");
        }
    }

    public void handleTaunt(Player player) {
        if (!running) return;
        if (player == null) return;
        UUID id = player.getUniqueId();
        if (!activePlayers.contains(id)) return;
        if (id.equals(seeker)) {
            player.sendMessage(ChatColor.RED + "Seekers can't taunt.");
            return;
        }

        long now = System.currentTimeMillis();
        long last = lastTauntMillis.getOrDefault(id, 0L);
        long remaining = (last + TAUNT_COOLDOWN_MILLIS) - now;
        if (remaining > 0) {
            long sec = Math.max(1, (long) Math.ceil(remaining / 1000.0));
            player.sendMessage(ChatColor.RED + "Taunt cooldown: " + sec + "s");
            return;
        }
        lastTauntMillis.put(id, now);

        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false, true));

        Player seekerP = seeker == null ? null : Bukkit.getPlayer(seeker);
        if (seekerP != null) {
            seekerP.sendMessage(ChatColor.LIGHT_PURPLE + "Taunt! " + ChatColor.YELLOW + player.getName()
                    + ChatColor.GRAY + " revealed themselves.");
            seekerP.playSound(seekerP.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        }

        player.sendMessage(ChatColor.LIGHT_PURPLE + "Taunted!");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.3f);
    }

    public void endGame() {
        running = false;
        starting = false;
        seekerReleased = false;

        if (startTask != null) {
            startTask.cancel();
            startTask = null;
        }
        if (timeLimitTask != null) {
            timeLimitTask.cancel();
            timeLimitTask = null;
        }
        if (nametagTask != null) {
            nametagTask.cancel();
            nametagTask = null;
        }

        List<UUID> toReturn = new ArrayList<>();
        toReturn.addAll(activePlayers);
        toReturn.addAll(outPlayers);
        toReturn.addAll(spectators);
        toReturn.addAll(queuedPlayers);

        activePlayers.clear();
        outPlayers.clear();
        spectators.clear();
        queuedPlayers.clear();
        lastTauntMillis.clear();
        seeker = null;

        for (UUID id : toReturn) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            Profile profile = Craftmen.get().getProfileManager().getProfile(p);
            if (profile != null) profile.setState(PlayerState.LOBBY);
            PlayerReset.resetToHub(p);
        }

        clearHiddenNametags(toReturn);

        // Arena stays placed; only clear runtime state.
    }

    private void cleanupPastedArena() {
        if (pasteOrigin == null || world == null) return;
        if (pastedWidth <= 0 || pastedHeight <= 0 || pastedLength <= 0) return;

        Location min = pasteOrigin.clone();
        Location max = pasteOrigin.clone().add(pastedWidth, pastedHeight, pastedLength);
        ArenaManager arenaManager = Craftmen.get().getArenaManager();
        if (arenaManager != null) {
            arenaManager.removeArenaAtLocation("HideSeek", min, max);
        }

        pasteOrigin = null;
        pastedWidth = pastedHeight = pastedLength = 0;
        arenaLoaded = false;
    }

    private void ensureArenaLoaded() {
        // Deprecated: arena is placed once via /placehs.
    }

    private void ensureWorldAndSpawn() {
        if (world == null) {
            world = Craftmen.get().getHubLocation() != null ? Craftmen.get().getHubLocation().getWorld() : Bukkit.getWorld("world");
        }
        if (world != null && spawn == null) {
            spawn = new Location(world, SPAWN_X, SPAWN_Y, SPAWN_Z);
        }
    }

    private final Map<Scoreboard, Map<String, Team.OptionStatus>> rememberedVisibility = new WeakHashMap<>();

    private void rememberTeamVisibility(Scoreboard board, Team team) {
        if (board == null || team == null) return;
        Map<String, Team.OptionStatus> map = rememberedVisibility.computeIfAbsent(board, b -> new HashMap<>());
        map.putIfAbsent(team.getName(), team.getOption(Team.Option.NAME_TAG_VISIBILITY));
    }

    private void restoreRememberedTeamVisibility() {
        for (Map.Entry<Scoreboard, Map<String, Team.OptionStatus>> e : new ArrayList<>(rememberedVisibility.entrySet())) {
            Scoreboard board = e.getKey();
            Map<String, Team.OptionStatus> teams = e.getValue();
            if (board == null || teams == null) continue;
            for (Map.Entry<String, Team.OptionStatus> t : teams.entrySet()) {
                Team team = board.getTeam(t.getKey());
                if (team == null) continue;
                try {
                    team.setOption(Team.Option.NAME_TAG_VISIBILITY, t.getValue());
                } catch (IllegalStateException ignored) {
                }
            }
        }
        rememberedVisibility.clear();
    }

    public boolean placeArena(Player admin) {
        if (loadingArena) return false;
        if (running || starting) return false;

        loadingArena = true;
        try {
            world = Craftmen.get().getHubLocation() != null ? Craftmen.get().getHubLocation().getWorld() : Bukkit.getWorld("world");
            if (world == null) return false;

            File schematic = pickRandomSchematic();
            if (schematic == null) return false;

            cleanupPastedArena();

            Location origin = new Location(world, BASE_PASTE_X, BASE_PASTE_Y, BASE_PASTE_Z);
            PasteResult paste = pasteSchematic(world, schematic, origin);
            if (paste == null) return false;

            spawn = new Location(world, SPAWN_X, SPAWN_Y, SPAWN_Z);

            // Save cleanup bounds (even though we don't expect to remove, it lets /placehs re-place cleanly).
            pasteOrigin = paste.origin;
            if (paste.min != null && paste.max != null) {
                pasteOrigin = paste.min;
                pastedWidth = Math.abs(paste.max.getBlockX() - paste.min.getBlockX()) + 1;
                pastedHeight = Math.abs(paste.max.getBlockY() - paste.min.getBlockY()) + 1;
                pastedLength = Math.abs(paste.max.getBlockZ() - paste.min.getBlockZ()) + 1;
            } else {
                pastedWidth = paste.width;
                pastedHeight = paste.height;
                pastedLength = paste.length;
            }

            arenaLoaded = true;

            if (admin != null) {
                admin.sendMessage(ChatColor.LIGHT_PURPLE + "Placed Hide & Seek arena.");
            }
            return true;
        } finally {
            loadingArena = false;
        }
    }

    private void broadcastToAll(String msg) {
        Bukkit.broadcastMessage(msg);
    }

    private void broadcastToQueued(String msg) {
        for (UUID id : new ArrayList<>(queuedPlayers)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(msg);
        }
    }

    private void applyHiddenNametags() {
        // Hide ALL participants' nametags from everyone (including spectators).
        List<String> hiddenNames = new ArrayList<>();
        for (UUID id : activePlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) hiddenNames.add(p.getName());
        }

        for (UUID viewerId : activePlayers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null) applyHiddenNametagsForViewer(viewer, hiddenNames);
        }
        for (UUID viewerId : spectators) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null) applyHiddenNametagsForViewer(viewer, hiddenNames);
        }
        for (UUID viewerId : outPlayers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null) applyHiddenNametagsForViewer(viewer, hiddenNames);
        }
    }

    private void startNametagEnforcer() {
        if (nametagTask != null) nametagTask.cancel();
        nametagTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) return;
                applyHiddenNametags();
            }
        };
        nametagTask.runTaskTimer(plugin, 1L, 40L);
    }

    public boolean shouldFreeze(Player player) {
        return running && isSeeker(player) && !seekerReleased;
    }

    public void applyNametagHidingFor(Player viewer) {
        if (!running) return;
        applyHiddenNametagsForViewer(viewer);
    }

    private void applyHiddenNametagsForViewer(Player viewer) {
        applyHiddenNametagsForViewer(viewer, null);
    }

    private void applyHiddenNametagsForViewer(Player viewer, List<String> hiderNames) {
        if (viewer == null) return;
        Scoreboard board = viewer.getScoreboard();
        if (board == null) return;

        // An entry can only be in ONE team per scoreboard. Players are often already in a rank/nametag team,
        // and other systems will keep re-asserting that. If we try to move entries to our own team, nametags
        // will flicker. Instead, force the EXISTING entry team (if any) to hide nametags.

        if (hiderNames == null) {
            hiderNames = new ArrayList<>();
            for (UUID id : activePlayers) {
                if (id.equals(seeker)) continue;
                Player p = Bukkit.getPlayer(id);
                if (p != null) hiderNames.add(p.getName());
            }
        }

        for (String name : hiderNames) {
            Team existing = board.getEntryTeam(name);
            if (existing != null) {
                rememberTeamVisibility(board, existing);
                existing.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            } else {
                Team team = board.getTeam("hs_hidden");
                if (team == null) team = board.registerNewTeam("hs_hidden");
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
                team.addEntry(name);
            }
        }
    }

    private void clearHiddenNametags(Collection<UUID> players) {
        restoreRememberedTeamVisibility();
        for (UUID id : players) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            Scoreboard board = p.getScoreboard();
            if (board == null) continue;
            Team team = board.getTeam("hs_hidden");
            if (team != null) {
                try {
                    team.unregister();
                } catch (IllegalStateException ignored) {
                }
            }
        }
    }

    private File pickRandomSchematic() {
        File base = new File("plugins/Craftmen/arenas");
        File folder = new File(base, SCHEM_FOLDER_NAME);
        if (!folder.exists()) folder.mkdirs();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".schem"));
        if (files == null || files.length == 0) return null;
        return files[new Random().nextInt(files.length)];
    }

    private static class PasteResult {
        final Location origin;
        final int width;
        final int height;
        final int length;
        final Location spawn;
        final Location min;
        final Location max;

        PasteResult(Location origin, int width, int height, int length, Location spawn, Location min, Location max) {
            this.origin = origin;
            this.width = width;
            this.height = height;
            this.length = length;
            this.spawn = spawn;
            this.min = min;
            this.max = max;
        }
    }

    private PasteResult pasteSchematic(World world, File file, Location origin) {
        if (world == null || file == null || origin == null) return null;
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) return null;

            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                clipboard = reader.read();
            }

            try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                BlockVector3 to = BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
                Operations.complete(holder.createPaste(session).to(to).ignoreAirBlocks(false).build());
            }

            int width = clipboard.getDimensions().getX();
            int height = clipboard.getDimensions().getY();
            int length = clipboard.getDimensions().getZ();

            // Compute actual pasted bounding box using the same math as ArenaManager:
            // minCorner = pasteLocation + (regionMin - clipboardOrigin)
            BlockVector3 clipOrigin = clipboard.getOrigin();
            BlockVector3 regMin = clipboard.getRegion().getMinimumPoint();
            BlockVector3 regMax = clipboard.getRegion().getMaximumPoint();
            BlockVector3 offset = regMin.subtract(clipOrigin);

            Location min = origin.clone().add(offset.getX(), offset.getY(), offset.getZ());
            Location max = min.clone().add(
                    regMax.getX() - regMin.getX(),
                    regMax.getY() - regMin.getY(),
                    regMax.getZ() - regMin.getZ()
            );

            // Spawn is hardcoded elsewhere; we still compute bounds for cleanup.
            Location spawn = null;

            return new PasteResult(origin, width, height, length, spawn, min, max);
        } catch (Exception e) {
            plugin.getLogger().warning("HideSeek paste failed: " + e.getMessage());
            return null;
        }
    }

    private Location findFirstBlockInBox(World world, Location min, Location max, Material type) {
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.getBlockAt(x, y, z).getType() == type) {
                        return new Location(world, x, y, z);
                    }
                }
            }
        }
        return null;
    }
}
