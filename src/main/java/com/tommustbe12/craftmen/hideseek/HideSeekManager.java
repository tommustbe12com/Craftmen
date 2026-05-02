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

    private final JavaPlugin plugin;

    private final List<UUID> queuedPlayers = new ArrayList<>();
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Set<UUID> outPlayers = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Map<UUID, Long> lastTauntMillis = new HashMap<>();
    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();

    private Scoreboard hideScoreboard;
    private Team hiddenNameTeam;

    private UUID seeker;

    private boolean starting = false;
    private boolean running = false;

    private World world;
    private Location spawn;
    private int worldId = 0;

    private BukkitRunnable startTask;

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
        return running && activePlayers.contains(a) && activePlayers.contains(b);
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

        if (running) {
            addSpectator(player, ChatColor.GREEN + "Joined Hide & Seek as a spectator.");
            return;
        }

        UUID id = player.getUniqueId();
        if (queuedPlayers.contains(id)) {
            player.sendMessage(ChatColor.RED + "You are already queued for Hide & Seek.");
            return;
        }

        queuedPlayers.add(id);
        profile.setState(PlayerState.HIDESEEK_QUEUED);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Queued for Hide & Seek.");

        if (queuedPlayers.size() >= 2 && !starting) {
            beginPreStartCountdown();
        } else if (queuedPlayers.size() < 2) {
            player.sendMessage(ChatColor.GRAY + "Need at least 2 players to start.");
        }
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
        broadcastToAll(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Hide & Seek match starting soon!");
        broadcastToAll(ChatColor.GRAY + "Join now from the queue menu to participate.");

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

        worldId++;
        String worldName = "hideseek_" + worldId;

        WorldCreator wc = new WorldCreator(worldName);
        wc.environment(World.Environment.NORMAL);
        wc.type(WorldType.NORMAL);

        world = Bukkit.createWorld(wc);
        if (world == null) {
            broadcastToQueued(ChatColor.RED + "Failed to create Hide & Seek world.");
            endGame();
            return;
        }

        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setTime(6000);

        File schematic = pickRandomSchematic();
        if (schematic == null) {
            broadcastToQueued(ChatColor.RED + "No Hide & Seek map found in arenas/hideSeek/.");
            endGame();
            return;
        }

        PasteResult paste = pasteSchematic(world, schematic, new Location(world, 0.5, 100, 0.5));
        if (paste == null) {
            broadcastToQueued(ChatColor.RED + "Failed to load Hide & Seek map.");
            endGame();
            return;
        }

        spawn = paste.spawn != null ? paste.spawn : paste.origin.clone().add(0, 1, 0);

        setupNameTagHiding();

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

        // Hide nametags for hiders from everyone (including seeker/spectators).
        applyHiddenNametags();

        new BukkitRunnable() {
            @Override
            public void run() {
                Player seekerP = Bukkit.getPlayer(seeker);
                if (seekerP != null) {
                    seekerP.removePotionEffect(PotionEffectType.BLINDNESS);
                    seekerP.sendTitle(ChatColor.RED + "READY OR NOT", ChatColor.YELLOW + "HERE I COME!", 0, 60, 10);
                    seekerP.playSound(seekerP.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
                }
                broadcastToAll(ChatColor.GRAY + "Seeker released!");
            }
        }.runTaskLater(plugin, HIDERS_HIDE_TICKS);
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
        applyScoreboard(player);
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

        Player seekerP = seeker == null ? null : Bukkit.getPlayer(seeker);
        if (seekerP != null) {
            Location l = player.getLocation();
            int x = l.getBlockX();
            int z = l.getBlockZ();
            seekerP.sendMessage(ChatColor.LIGHT_PURPLE + "Taunt: " + ChatColor.YELLOW + player.getName()
                    + ChatColor.GRAY + " is near " + ChatColor.AQUA + x + ChatColor.GRAY + ", " + ChatColor.AQUA + z);
            seekerP.playSound(seekerP.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        }

        player.sendMessage(ChatColor.LIGHT_PURPLE + "You taunted the seeker.");
        player.playSound(player.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1.0f, 1.2f);
    }

    public void endGame() {
        running = false;
        starting = false;

        if (startTask != null) {
            startTask.cancel();
            startTask = null;
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
            restoreScoreboard(p);
            Profile profile = Craftmen.get().getProfileManager().getProfile(p);
            if (profile != null) profile.setState(PlayerState.LOBBY);
            PlayerReset.resetToHub(p);
        }

        previousScoreboards.clear();
        hideScoreboard = null;
        hiddenNameTeam = null;

        if (world != null) {
            String name = world.getName();
            Bukkit.unloadWorld(world, false);
            world = null;
            spawn = null;

            // Delete world folder later (avoid file locks).
            new BukkitRunnable() {
                @Override
                public void run() {
                    File folder = new File(Bukkit.getWorldContainer(), name);
                    if (name.startsWith("hideseek_")) deleteWorld(folder);
                }
            }.runTaskLater(plugin, 100L);
        }
    }

    private void deleteWorld(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) deleteWorld(f);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
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

    private void setupNameTagHiding() {
        var mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;
        hideScoreboard = mgr.getNewScoreboard();
        hiddenNameTeam = hideScoreboard.registerNewTeam("hs_hidden");
        hiddenNameTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        // Keep collision normal; only hide nametags.
    }

    private void applyHiddenNametags() {
        if (hideScoreboard == null || hiddenNameTeam == null) return;

        // Add all current hiders (not seeker) to the hidden team.
        for (UUID id : activePlayers) {
            if (id.equals(seeker)) continue;
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            hiddenNameTeam.addEntry(p.getName());
        }

        // Everyone involved should use this scoreboard so the nametag rule applies.
        for (UUID id : activePlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) applyScoreboard(p);
        }
        for (UUID id : spectators) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) applyScoreboard(p);
        }
        for (UUID id : outPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) applyScoreboard(p);
        }
    }

    private void applyScoreboard(Player player) {
        if (player == null) return;
        if (hideScoreboard == null) return;
        previousScoreboards.putIfAbsent(player.getUniqueId(), player.getScoreboard());
        player.setScoreboard(hideScoreboard);
    }

    private void restoreScoreboard(Player player) {
        if (player == null) return;
        Scoreboard prev = previousScoreboards.remove(player.getUniqueId());
        if (prev != null) {
            player.setScoreboard(prev);
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

        PasteResult(Location origin, int width, int height, int length, Location spawn) {
            this.origin = origin;
            this.width = width;
            this.height = height;
            this.length = length;
            this.spawn = spawn;
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

            // Find an iron block spawn inside pasted bounds.
            Location iron = findFirstBlock(world, origin, width, height, length, Material.IRON_BLOCK);
            Location spawn = iron == null ? null : iron.clone().add(0.5, 1.0, 0.5);

            return new PasteResult(origin, width, height, length, spawn);
        } catch (Exception e) {
            plugin.getLogger().warning("HideSeek paste failed: " + e.getMessage());
            return null;
        }
    }

    private Location findFirstBlock(World world, Location origin, int width, int height, int length, Material type) {
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < length; z++) {
                    if (world.getBlockAt(ox + x, oy + y, oz + z).getType() == type) {
                        return new Location(world, ox + x, oy + y, oz + z);
                    }
                }
            }
        }
        return null;
    }
}
