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
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

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
    private final Map<UUID, Location> privateRespawnLocation = new HashMap<>();
    private final Map<UUID, UUID> privateSpectateTarget = new HashMap<>();

    private record PartyTeamSettings(boolean enabled, Map<UUID, Integer> teamByPlayer) {}

    private final Map<UUID, PartyTeamSettings> pendingTeamSettingsByParty = new HashMap<>();

    private static final long LAST_DAMAGER_WINDOW_MILLIS = 15_000L;
    private final Map<UUID, UUID> lastDamagerByVictim = new HashMap<>();
    private final Map<UUID, Long> lastDamagerAtMillisByVictim = new HashMap<>();

    // Track ownership for crystal/anchor explosions so team-friendly-fire can be prevented.
    private final Map<UUID, UUID> ownerByCrystalEntity = new HashMap<>(); // crystal entity id -> player id
    private final Map<String, UUID> ownerByAnchorBlock = new HashMap<>(); // world:x:y:z -> player id
    private final Map<String, Long> ownerByAnchorAtMillis = new HashMap<>();

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

    public UUID getPlayerInstanceId(Player player) {
        if (player == null) return null;
        return playerInstance.get(player.getUniqueId());
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
                if (session.teamsEnabled) {
                    Integer t1 = session.teamByPlayer.get(damager.getUniqueId());
                    Integer t2 = session.teamByPlayer.get(damaged.getUniqueId());
                    if (t1 != null && t2 != null && t1.intValue() == t2.intValue()) return false;
                }
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

        var party = Craftmen.get().getPartyManager().getParty(player);
        if (party != null && !party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the party leader can join activities.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
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
        joinPrivateParty(partyId, partyMembers, game, rounds, false, null);
    }

    public void joinPrivateParty(UUID partyId, Collection<UUID> partyMembers, Game game, int rounds, boolean teamsEnabled, Map<UUID, Integer> teamByPlayer) {
        joinPrivateParty(partyId, partyMembers, game, rounds, teamsEnabled, teamByPlayer, null);
    }

    public void joinPrivateParty(UUID partyId, Collection<UUID> partyMembers, Game game, int rounds, boolean teamsEnabled, Map<UUID, Integer> teamByPlayer, Map<Integer, Game> kitByTeam) {
        if (partyId == null || partyMembers == null || game == null) return;

        boolean resolvedTeamsEnabled = teamsEnabled;
        Map<UUID, Integer> resolvedTeamByPlayer = teamByPlayer;
        if (resolvedTeamByPlayer == null) {
            PartyTeamSettings pending = pendingTeamSettingsByParty.get(partyId);
            if (pending != null) {
                resolvedTeamsEnabled = pending.enabled;
                resolvedTeamByPlayer = pending.teamByPlayer;
            }
        }

        PartyFfaSession session = partySessions.get(partyId);
        if (session == null) {
            session = new PartyFfaSession(partyId, game, rounds);
            partySessions.put(partyId, session);
        }
        session.teamsEnabled = resolvedTeamsEnabled;
        session.teamByPlayer.clear();
        if (resolvedTeamByPlayer != null) session.teamByPlayer.putAll(resolvedTeamByPlayer);
        session.kitByTeamName.clear();
        if (kitByTeam != null) {
            for (var entry : kitByTeam.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                session.kitByTeamName.put(entry.getKey(), entry.getValue().getName());
            }
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
            session.teamRoundWins.clear();
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
                session.alive.remove(memberId); // never count mid-round joiners as alive
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
        Set<UUID> inInstance = new HashSet<>(inst.players);
        for (UUID uuid : inInstance) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                leave(p, false, false);
            } else {
                // offline cleanup
                playerInstance.remove(uuid);
                inst.players.remove(uuid);
            }
        }

        // Safety net: ensure all party members end up in hub even if their instance tracking glitched.
        if (party != null) {
            for (UUID memberId : party.getMembers()) {
                Player p = Bukkit.getPlayer(memberId);
                if (p == null) continue;
                UUID instId = playerInstance.get(memberId);
                if (instId != null) {
                    FfaInstance cur = instancesById.get(instId);
                    if (cur != null && cur.isPrivate && partyId.equals(cur.ownerPartyId)) {
                        leave(p, false, false);
                        continue;
                    }
                }
                if (Craftmen.get().getMatchManager().getMatch(p) == null && !Craftmen.get().getEndFightManager().isInGame(p)) {
                    PlayerReset.resetToHub(p);
                }
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

    public void setPendingPartyTeamSettings(UUID partyId, boolean enabled, Map<UUID, Integer> teamByPlayer) {
        if (partyId == null) return;
        Map<UUID, Integer> copy = new HashMap<>();
        if (teamByPlayer != null) copy.putAll(teamByPlayer);
        pendingTeamSettingsByParty.put(partyId, new PartyTeamSettings(enabled, copy));

        PartyFfaSession session = partySessions.get(partyId);
        if (session != null) {
            session.teamsEnabled = enabled;
            session.teamByPlayer.clear();
            session.teamByPlayer.putAll(copy);
        }
    }

    private PartyFfaSession getSession(FfaInstance inst) {
        if (inst == null || !inst.isPrivate || inst.ownerPartyId == null) return null;
        return partySessions.get(inst.ownerPartyId);
    }

    private Game resolveKitForPlayer(FfaInstance inst, PartyFfaSession session, Player player) {
        if (inst == null || session == null || player == null) return inst == null ? null : inst.game;
        if (!session.teamsEnabled) return inst.game;

        Integer team = session.teamByPlayer.get(player.getUniqueId());
        if (team == null) return inst.game;
        String gameName = session.kitByTeamName.get(team);
        if (gameName == null) return inst.game;
        Game g = Craftmen.get().getGameManager().getGame(gameName);
        return g == null ? inst.game : g;
    }

    private void startNextRound(FfaInstance inst, PartyFfaSession session) {
        clearDroppedItems(inst);
        session.currentRound++;
        session.alive.clear();
        session.spectators.clear();
        session.roundParticipants.clear();

        Map<Integer, Location> baseSpawnByTeam = new HashMap<>();
        Map<Integer, Integer> indexByTeam = new HashMap<>();

        for (UUID uuid : new HashSet<>(inst.players)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;

            session.alive.add(uuid);
            session.roundParticipants.add(uuid);
            // Full round reset
            PlayerReset.clearTransientState(p);
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setSaturation(20f);
            p.getInventory().clear();
            setParticipant(p);
            Game kitGame = resolveKitForPlayer(inst, session, p);
            kitGame.applyLoadout(p);
            p.updateInventory();

            // Team spawns: keep teammates near each other (private party FFA only).
            Location spawn = null;
            if (session.teamsEnabled) {
                int team = session.teamByPlayer.getOrDefault(uuid, 1);
                Location base = baseSpawnByTeam.computeIfAbsent(team, t -> {
                    Location loc = findSafeSpawn(inst);
                    return loc != null ? loc : inst.pasteOrigin.clone().add(0, 5, 0);
                });
                int idx = indexByTeam.getOrDefault(team, 0);
                indexByTeam.put(team, idx + 1);
                spawn = findNearbySafe(base, inst, idx);
            }
            if (spawn == null) spawn = findSafeSpawn(inst);
            if (spawn == null) spawn = inst.pasteOrigin.clone().add(0, 5, 0);
            p.teleport(spawn);

            if (session.teamsEnabled) {
                int team = session.teamByPlayer.getOrDefault(uuid, 1);
                String mates = formatTeammates(session, uuid, team);
                p.sendMessage(ChatColor.GOLD + "Team " + team + ChatColor.GRAY + " teammates: " + ChatColor.YELLOW + mates);
            }
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
        // Respawn them near the killer if known, otherwise at their death location.
        UUID killerId = privateSpectateTarget.get(id);
        Player killer = killerId == null ? null : Bukkit.getPlayer(killerId);
        privateRespawnLocation.put(id, killer != null ? killer.getLocation() : dead.getLocation());

        if (session.teamsEnabled) {
            Set<Integer> aliveTeams = new HashSet<>();
            for (UUID u : session.alive) {
                Integer t = session.teamByPlayer.get(u);
                if (t != null) aliveTeams.add(t);
            }
            if (aliveTeams.size() > 1) return;

            Integer winningTeam = aliveTeams.stream().findFirst().orElse(null);
            if (winningTeam != null) {
                session.teamRoundWins.put(winningTeam, session.teamRoundWins.getOrDefault(winningTeam, 0) + 1);
                broadcastBox(inst, ChatColor.GREEN + "Round " + session.currentRound + " Winner",
                        List.of(
                                ChatColor.YELLOW + "Team " + winningTeam,
                                ChatColor.GOLD + "Winners: " + ChatColor.YELLOW + formatTeamMembers(session, winningTeam)
                        ));
            } else {
                broadcastBox(inst, ChatColor.YELLOW + "Round " + session.currentRound + " ended",
                        List.of(ChatColor.GRAY + "No winner"));
            }
        } else {
            if (session.alive.size() > 1) return;

            UUID winnerId = session.alive.stream().findFirst().orElse(null);
            Player winner = winnerId == null ? null : Bukkit.getPlayer(winnerId);
            if (winner != null) {
                session.roundWins.put(winnerId, session.roundWins.getOrDefault(winnerId, 0) + 1);
                broadcastBox(inst, ChatColor.GREEN + "Round " + session.currentRound + " Winner",
                        List.of(ChatColor.YELLOW + winner.getName()));
                winner.playSound(winner.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            } else {
                broadcastBox(inst, ChatColor.YELLOW + "Round " + session.currentRound + " ended",
                        List.of(ChatColor.GRAY + "No winner"));
            }
        }

        if (session.currentRound >= session.totalRounds) {
            if (session.teamsEnabled) {
                Integer topTeam = null;
                int best = -1;
                for (var entry : session.teamRoundWins.entrySet()) {
                    if (entry.getValue() > best) {
                        best = entry.getValue();
                        topTeam = entry.getKey();
                    }
                }
                broadcastBox(inst, ChatColor.GOLD + "" + ChatColor.BOLD + "Party FFA Finished",
                        topTeam == null
                                ? List.of(ChatColor.GRAY + "No winner")
                                : List.of(
                                        ChatColor.GOLD + "Winning Team: " + ChatColor.YELLOW + "Team " + topTeam + ChatColor.GRAY + " (" + best + " rounds)",
                                        ChatColor.GOLD + "Winners: " + ChatColor.YELLOW + formatTeamMembers(session, topTeam)
                                ));
            } else {
                UUID top = null;
                int best = -1;
                for (var entry : session.roundWins.entrySet()) {
                    if (entry.getValue() > best) {
                        best = entry.getValue();
                        top = entry.getKey();
                    }
                }
                Player finalWinner = top == null ? null : Bukkit.getPlayer(top);
                broadcastBox(inst, ChatColor.GOLD + "" + ChatColor.BOLD + "Party FFA Finished",
                        finalWinner == null
                                ? List.of(ChatColor.GRAY + "No winner")
                                : List.of(ChatColor.GOLD + "Winner: " + ChatColor.YELLOW + finalWinner.getName() + ChatColor.GRAY + " (" + best + " rounds)"));
                if (finalWinner != null) finalWinner.playSound(finalWinner.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }

            UUID partyId = inst.ownerPartyId;
            if (partyId != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> endPrivatePartyFfa(partyId), 20L * 3L);
            }
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> startNextRound(inst, session), 20L * 3L);
    }

    // --------------------
    // Party -> Public FFA join
    // --------------------

    public void joinPublicParty(com.tommustbe12.craftmen.party.Party party, Game game) {
        if (party == null || game == null) return;

        // Only join players that are online + in LOBBY and not in other modes.
        List<Player> joinable = new ArrayList<>();
        for (UUID memberId : party.getMembers()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p == null) continue;
            if (Craftmen.get().getEndFightManager().isInGame(p)) continue;
            if (Craftmen.get().getMatchManager().getMatch(p) != null) continue;
            if (isInFfa(p)) continue;

            Profile profile = Craftmen.get().getProfileManager().getProfile(p);
            if (profile == null || profile.getState() != PlayerState.LOBBY) continue;
            joinable.add(p);
        }

        if (joinable.isEmpty()) return;

        FfaInstance inst = pickOrCreatePublicInstance(game, joinable.size());
        if (inst == null) return;

        for (Player p : joinable) {
            PlayerReset.clearTransientState(p);
            leave(p, false);
            joinIntoInstance(p, inst);
            game.applyLoadout(p);
            Craftmen.get().getArmorTrimManager().apply(p);
            p.updateInventory();
        }

        broadcast(inst, ChatColor.GREEN + "Party joined FFA (" + inst.players.size() + "/" + MAX_PLAYERS_PER_INSTANCE + ").");
    }

    private void joinIntoInstance(Player player, FfaInstance instance) {
        instance.players.add(player.getUniqueId());
        playerInstance.put(player.getUniqueId(), instance.id);

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null) profile.setState(PlayerState.FFA_FIGHTING);

        teleportToSafeSpawn(player, instance);
    }

    private FfaInstance pickOrCreatePublicInstance(Game game) {
        return pickOrCreatePublicInstance(game, 1);
    }

    private FfaInstance pickOrCreatePublicInstance(Game game, int requiredSlots) {
        List<FfaInstance> list = publicInstances.computeIfAbsent(game.getName(), k -> new ArrayList<>());

        for (FfaInstance inst : list) {
            if (inst.isPrivate) continue;
            if (inst.players.size() + Math.max(1, requiredSlots) <= MAX_PLAYERS_PER_INSTANCE) return inst;
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
        leave(player, message, true);
    }

    public void leave(Player player, boolean message, boolean broadcastLeave) {
        if (player == null) return;

        UUID instId = playerInstance.remove(player.getUniqueId());
        if (instId == null) return;

        FfaInstance inst = instancesById.get(instId);
        if (inst != null) {
            inst.players.remove(player.getUniqueId());
            if (broadcastLeave) {
                broadcast(inst, ChatColor.RED + player.getName() + " left FFA (" + inst.players.size() + "/" + MAX_PLAYERS_PER_INSTANCE + ").");
            }
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null) profile.setState(PlayerState.LOBBY);

        // If the player is quitting, don't teleport/inventory reset.
        if (player.isOnline()) {
            PlayerReset.resetToHub(player);
            if (message) player.sendMessage(ChatColor.RED + "Left FFA.");
        }

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

        // Never auto-refresh private party FFAs.
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

        Player source = resolveDamagingPlayer(e.getDamager());
        if (source != null) {
            if (!isInFfa(damaged) && !isInFfa(source)) return;
            if (!allowDamage(source, damaged)) {
                e.setCancelled(true);
                return;
            }

            lastDamagerByVictim.put(damaged.getUniqueId(), source.getUniqueId());
            lastDamagerAtMillisByVictim.put(damaged.getUniqueId(), System.currentTimeMillis());
        } else {
            // No resolved player source (e.g., environmental); allow damage as-is.
            if (!isInFfa(damaged)) return;
        }

        if (e.getFinalDamage() >= damaged.getHealth()) {
            // Let vanilla totem pop logic handle saves (only when held in hand like normal vanilla behavior).
            if (hasHandTotem(damaged)) return;

            e.setCancelled(true);
            handleFfaLethal(damaged, source);
        }
    }

    private Location findNearbySafe(Location base, FfaInstance inst, int idx) {
        if (base == null) return null;
        World world = base.getWorld();
        if (world == null) return null;

        int[] dx = {0, 1, -1, 2, -2, 0, 1, -1, 2, -2};
        int[] dz = {0, 0, 0, 0, 0, 1, -1, 1, -1, 2};
        int start = Math.max(0, idx);
        for (int i = 0; i < 10; i++) {
            int k = (start + i) % dx.length;
            Location candidate = base.clone().add(dx[k], 0, dz[k]);
            int x = candidate.getBlockX();
            int y = candidate.getBlockY();
            int z = candidate.getBlockZ();
            Block below = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (below.getType() == Material.AIR || below.getType() == Material.BARRIER) continue;
            if (feet.getType() != Material.AIR) continue;
            if (head.getType() != Material.AIR) continue;
            return new Location(world, x + 0.5, y, z + 0.5);
        }
        return base;
    }

    private static String formatTeammates(PartyFfaSession session, UUID self, int team) {
        if (session == null) return "None";
        List<String> names = new ArrayList<>();
        for (var entry : session.teamByPlayer.entrySet()) {
            if (entry.getKey().equals(self)) continue;
            Integer t = entry.getValue();
            if (t == null || t.intValue() != team) continue;
            Player p = Bukkit.getPlayer(entry.getKey());
            names.add(p != null ? p.getName() : entry.getKey().toString());
        }
        if (names.isEmpty()) return "None";
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return String.join(ChatColor.GRAY + ", " + ChatColor.YELLOW, names);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFfaDamageByBlock(EntityDamageByBlockEvent e) {
        if (!(e.getEntity() instanceof Player damaged)) return;
        if (!isInFfa(damaged)) return;
        if (e.isCancelled()) return;

        Block damager = e.getDamager();
        if (damager == null) return;

        if (damager.getType() == Material.RESPAWN_ANCHOR) {
            UUID owner = ownerByAnchorBlock.get(anchorKey(damager));
            if (owner != null) {
                Player source = Bukkit.getPlayer(owner);
                if (source != null && !allowDamage(source, damaged)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFfaAnyDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player damaged)) return;
        UUID instId = playerInstance.get(damaged.getUniqueId());
        if (instId == null) return;
        if (e.isCancelled()) return;
        if (e.getFinalDamage() < damaged.getHealth()) return;

        // Let vanilla totem pop logic handle saves (only when held in hand).
        if (hasHandTotem(damaged)) return;

        Player killer = null;
        UUID lastId = lastDamagerByVictim.get(damaged.getUniqueId());
        Long at = lastDamagerAtMillisByVictim.get(damaged.getUniqueId());
        if (lastId != null && at != null && System.currentTimeMillis() - at <= LAST_DAMAGER_WINDOW_MILLIS) {
            killer = Bukkit.getPlayer(lastId);
        }

        e.setCancelled(true);
        handleFfaLethal(damaged, killer);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFfaInteractExplosives(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (player == null) return;
        if (!isInFfa(player)) return;

        UUID instId = playerInstance.get(player.getUniqueId());
        if (instId == null) return;
        FfaInstance inst = instancesById.get(instId);
        if (inst == null || !inst.isPrivate) return;
        PartyFfaSession session = getSession(inst);
        if (session == null || !session.teamsEnabled) return;

        // End crystal placement: attach the placer as the owner for the spawned crystal entity.
        if (e.getClickedBlock() != null
                && e.getItem() != null
                && e.getItem().getType() == Material.END_CRYSTAL
                && (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
            var placedAgainst = e.getClickedBlock().getRelative(BlockFace.UP).getLocation().add(0.5, 0.0, 0.5);
            UUID placerId = player.getUniqueId();

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                EnderCrystal best = null;
                double bestDist = 2.5 * 2.5;
                for (Entity ent : placedAgainst.getWorld().getNearbyEntities(placedAgainst, 2.5, 2.5, 2.5)) {
                    if (!(ent instanceof EnderCrystal c)) continue;
                    double d = c.getLocation().distanceSquared(placedAgainst);
                    if (d <= bestDist) {
                        bestDist = d;
                        best = c;
                    }
                }
                if (best != null) {
                    ownerByCrystalEntity.put(best.getUniqueId(), placerId);
                }
            }, 1L);
            // ignore task handle; one-shot
        }

        // Respawn anchor interaction: record the clicker as "owner" for a short window.
        if (e.getClickedBlock() != null
                && e.getClickedBlock().getType() == Material.RESPAWN_ANCHOR
                && (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
            String key = anchorKey(e.getClickedBlock());
            ownerByAnchorBlock.put(key, player.getUniqueId());
            ownerByAnchorAtMillis.put(key, System.currentTimeMillis());
        }

        cleanupAnchorOwners();
    }

    private void cleanupAnchorOwners() {
        long now = System.currentTimeMillis();
        ownerByAnchorAtMillis.entrySet().removeIf(ent -> {
            if (now - ent.getValue() <= 10_000L) return false;
            ownerByAnchorBlock.remove(ent.getKey());
            return true;
        });
    }

    private static String anchorKey(Block b) {
        return b.getWorld().getUID() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    private Player resolveDamagingPlayer(Entity damager) {
        if (damager == null) return null;
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            Object shooter = proj.getShooter();
            if (shooter instanceof Player p) return p;
            return null;
        }
        if (damager instanceof EnderCrystal crystal) {
            UUID owner = ownerByCrystalEntity.get(crystal.getUniqueId());
            return owner == null ? null : Bukkit.getPlayer(owner);
        }
        return null;
    }

    private static String formatTeamMembers(PartyFfaSession session, int teamId) {
        if (session == null) return "Unknown";
        List<String> names = new ArrayList<>();
        for (var entry : session.teamByPlayer.entrySet()) {
            Integer t = entry.getValue();
            if (t == null || t.intValue() != teamId) continue;
            Player p = Bukkit.getPlayer(entry.getKey());
            names.add(p != null ? p.getName() : entry.getKey().toString());
        }
        if (names.isEmpty()) return "Unknown";
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return String.join(ChatColor.GRAY + ", " + ChatColor.YELLOW, names);
    }

    private void broadcastBox(FfaInstance inst, String title, List<String> lines) {
        if (inst == null || title == null) return;
        broadcast(inst, ChatColor.DARK_GRAY + "»»»»»»»»»»»»»»»»»»»»");
        broadcast(inst, " " + title);
        if (lines != null) {
            for (String l : lines) {
                if (l == null) continue;
                broadcast(inst, " " + l);
            }
        }
        broadcast(inst, ChatColor.DARK_GRAY + "««««««««««««««««««««");
    }

    private boolean hasHandTotem(Player player) {
        if (player == null) return false;
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return (main != null && main.getType() == Material.TOTEM_OF_UNDYING)
                || (off != null && off.getType() == Material.TOTEM_OF_UNDYING);
    }

    private void handleFfaLethal(Player dead, Player killer) {
        UUID instId = playerInstance.get(dead.getUniqueId());
        if (instId == null) return;
        FfaInstance inst = instancesById.get(instId);
        if (inst == null) return;

        lastDamagerByVictim.remove(dead.getUniqueId());
        lastDamagerAtMillisByVictim.remove(dead.getUniqueId());

        // Normally prevented by lethal-damage cancellation, but keep as a fallback if another plugin forces death.
        if (killer != null) CosmeticsApplier.applyKillDeath(killer, dead, dead.getLocation());

        String msg;
        if (killer != null && allowDamage(killer, dead)) {
            msg = ChatColor.RED + dead.getName() + ChatColor.GRAY + " was killed by " + ChatColor.GREEN + killer.getName();
        } else {
            msg = ChatColor.RED + dead.getName() + ChatColor.GRAY + " died";
        }
        broadcast(inst, msg);

        if (!inst.isPrivate && killer != null) {
            killer.setHealth(20.0);
            killer.setFoodLevel(20);
            killer.setSaturation(20f);
        }

        if (inst.isPrivate) {
            PartyFfaSession session = getSession(inst);
            if (killer != null) privateSpectateTarget.put(dead.getUniqueId(), killer.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!dead.isOnline()) return;
                UUID curInst = playerInstance.get(dead.getUniqueId());
                if (curInst == null || !curInst.equals(inst.id)) return;
                dead.setHealth(20.0);
                dead.setFoodLevel(20);
                dead.setSaturation(20f);

                if (session != null) handlePrivateDeath(inst, session, dead);

                Location loc = privateRespawnLocation.get(dead.getUniqueId());
                if (loc == null) loc = inst.pasteOrigin.clone().add(0, 5, 0);
                dead.teleport(loc);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    UUID targetId = privateSpectateTarget.get(dead.getUniqueId());
                    if (targetId != null) {
                        Player target = Bukkit.getPlayer(targetId);
                        if (target != null) dead.setSpectatorTarget(target);
                    }
                }, 1L);
            });
            return;
        }

        if (killer != null && allowDamage(killer, dead)) {
            Profile pk = Craftmen.get().getProfileManager().getProfile(killer);
            if (pk != null) pk.addFfaKill();
        }
        Profile pd = Craftmen.get().getProfileManager().getProfile(dead);
        if (pd != null) pd.addFfaDeath();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!dead.isOnline()) return;
            dead.setHealth(20.0);
            dead.setFoodLevel(20);
            dead.setSaturation(20f);
            leave(dead, true);
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        UUID instId = playerInstance.get(dead.getUniqueId());
        if (instId == null) return;

        e.setDeathMessage(null);
        e.getDrops().clear();
        e.setDroppedExp(0);

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

                if (killer != null) privateSpectateTarget.put(dead.getUniqueId(), killer.getUniqueId());

                String msg;
                if (killer != null && allowDamage(killer, dead)) {
                    msg = ChatColor.RED + dead.getName() + ChatColor.GRAY + " was killed by " + ChatColor.GREEN + killer.getName();
                } else {
                    msg = ChatColor.RED + dead.getName() + ChatColor.GRAY + " died";
                }
                broadcast(inst, msg);
                if (session != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!dead.isOnline()) return;
                        UUID curInst = playerInstance.get(dead.getUniqueId());
                        if (curInst == null || !curInst.equals(inst.id)) return;
                        dead.setHealth(20.0);
                        dead.setFoodLevel(20);
                        dead.setSaturation(20f);
                        handlePrivateDeath(inst, session, dead);
                        Location loc = privateRespawnLocation.get(dead.getUniqueId());
                        if (loc == null) loc = inst.pasteOrigin.clone().add(0, 5, 0);
                        dead.teleport(loc);
                    }, 2L);
                }
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

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!dead.isOnline()) return;
                dead.setHealth(20.0);
                dead.setFoodLevel(20);
                dead.setSaturation(20f);
                leave(dead, true);
            }, 2L);
        });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        UUID instId = playerInstance.get(player.getUniqueId());
        if (instId == null) return;
        FfaInstance inst = instancesById.get(instId);
        if (inst == null || !inst.isPrivate) return;

        Location loc = privateRespawnLocation.remove(player.getUniqueId());
        if (loc == null) loc = inst.pasteOrigin.clone().add(0, 5, 0);
        e.setRespawnLocation(loc);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PartyFfaSession session = getSession(inst);
            if (session != null) setSpectator(player, session);
            UUID targetId = privateSpectateTarget.remove(player.getUniqueId());
            if (targetId != null) {
                Player target = Bukkit.getPlayer(targetId);
                if (target != null) player.setSpectatorTarget(target);
            }
        }, 1L);
    }
}
