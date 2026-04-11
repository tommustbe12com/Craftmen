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
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

    private final Craftmen plugin;
    private final File ffaFolder = new File("plugins/Craftmen/arenas/FFA");

    private final Map<String, FfaInstance> instances = new HashMap<>();
    private final Map<UUID, String> playerFfaGame = new HashMap<>();

    public FfaManager(Craftmen plugin) {
        this.plugin = plugin;
        if (!ffaFolder.exists()) ffaFolder.mkdirs();

        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 20L, 20L * 5L); // every 5s
    }

    public int getPlayersInFfa(Game game) {
        FfaInstance inst = instances.get(game.getName());
        if (inst == null) return 0;
        return inst.players.size();
    }

    public boolean isInFfa(Player player) {
        return playerFfaGame.containsKey(player.getUniqueId());
    }

    public boolean allowDamage(Player damager, Player damaged) {
        String g1 = playerFfaGame.get(damager.getUniqueId());
        if (g1 == null) return false;
        String g2 = playerFfaGame.get(damaged.getUniqueId());
        return g1.equals(g2);
    }

    public void join(Player player, Game game) {
        if (player == null || game == null) return;

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null || profile.getState() != PlayerState.LOBBY) {
            player.sendMessage(ChatColor.RED + "You can only join FFA from the hub.");
            return;
        }

        leave(player, false);

        FfaInstance instance = instances.get(game.getName());
        if (instance == null) {
            instance = createInstance(game);
            if (instance == null) {
                player.sendMessage(ChatColor.RED + "No FFA schematics found in arenas/FFA/.");
                return;
            }
            instances.put(game.getName(), instance);
        }

        instance.players.add(player.getUniqueId());
        playerFfaGame.put(player.getUniqueId(), game.getName());

        profile.setState(PlayerState.FFA_FIGHTING);

        teleportToSafeSpawn(player, instance);
        game.applyLoadout(player);
        player.updateInventory();

        broadcast(instance, ChatColor.GREEN + player.getName() + " joined FFA (" + instance.players.size() + " players).");
    }

    public void leave(Player player, boolean message) {
        String gameName = playerFfaGame.remove(player.getUniqueId());
        if (gameName == null) return;

        FfaInstance inst = instances.get(gameName);
        if (inst != null) {
            inst.players.remove(player.getUniqueId());
            broadcast(inst, ChatColor.RED + player.getName() + " left FFA (" + inst.players.size() + " players).");
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null) profile.setState(PlayerState.LOBBY);

        Craftmen.get().getHubManager().giveHubItems(player);
        player.teleport(Craftmen.get().getHubLocation());
        if (message) player.sendMessage(ChatColor.RED + "Left FFA.");

        if (inst != null && inst.players.isEmpty()) {
            destroyInstance(inst);
            instances.remove(gameName);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (FfaInstance inst : new ArrayList<>(instances.values())) {
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

        Location origin = computePasteOrigin(world, schem);
        FfaInstance inst = new FfaInstance(game, schem, origin);
        paste(inst);
        inst.lastResetAtMillis = System.currentTimeMillis();
        return inst;
    }

    private void resetInstance(FfaInstance inst) {
        // clear old, paste new random, respawn everyone
        clear(inst);
        File next = pickRandomSchematic(inst.game);
        if (next != null) {
            inst.lastResetAtMillis = System.currentTimeMillis();
            inst.minCorner = null;
            inst.maxCorner = null;
            inst.players.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
            FfaInstance replacement = new FfaInstance(inst.game, next, inst.pasteOrigin);
            replacement.players.addAll(inst.players);
            paste(replacement);

            // carry bounds + file + timestamp
            inst.minCorner = replacement.minCorner;
            inst.maxCorner = replacement.maxCorner;
            inst.lastResetAtMillis = replacement.lastResetAtMillis;

            for (UUID uuid : inst.players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                teleportToSafeSpawn(p, inst);
                inst.game.applyLoadout(p);
                p.getInventory().setItem(8, leaveItem());
                p.updateInventory();
                p.sendMessage(ChatColor.YELLOW + "FFA arena refreshed.");
            }
        }
    }

    private void destroyInstance(FfaInstance inst) {
        clear(inst);
    }

    private void broadcast(FfaInstance inst, String msg) {
        for (UUID uuid : inst.players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }

    private File pickRandomSchematic(Game game) {
        // Always random from arenas/FFA/ (no other sources).
        File[] candidates = listSchems(ffaFolder);
        if (candidates == null || candidates.length == 0) return null;
        return candidates[ThreadLocalRandom.current().nextInt(candidates.length)];
    }

    private static File[] listSchems(File dir) {
        if (dir == null || !dir.exists()) return null;
        return dir.listFiles((d, name) -> name.endsWith(".schem"));
    }

    private Location computePasteOrigin(World world, File schematicFile) {
        // Find a paste location automatically near the worldborder corner, scanning for an empty box.
        int[] dims = readSchematicDims(schematicFile);
        int width = Math.max(1, dims[0]);
        int height = Math.max(1, dims[1]);
        int length = Math.max(1, dims[2]);

        var wb = world.getWorldBorder();
        Location center = wb.getCenter();
        int half = (int) Math.floor(wb.getSize() / 2.0);

        int margin = 64;
        int startX = center.getBlockX() + half - margin - width;
        int startZ = center.getBlockZ() + half - margin - length;

        int minX = center.getBlockX() - half + margin;
        int minZ = center.getBlockZ() - half + margin;

        // scan inward in a grid until we find an empty space
        int step = 64;
        for (int dz = 0; startZ - dz > minZ; dz += step) {
            for (int dx = 0; startX - dx > minX; dx += step) {
                int x = startX - dx;
                int z = startZ - dz;
                int y = Math.max(world.getMinHeight() + 5, world.getHighestBlockYAt(x, z) + 5);
                Location candidate = new Location(world, x, y, z);
                if (Craftmen.get().getArenaManager().isAreaEmpty(candidate, width, length, world)) {
                    return candidate;
                }
            }
        }

        // fallback: still keep it far out
        int y = Math.max(world.getMinHeight() + 5, world.getHighestBlockYAt(startX, startZ) + 5);
        return new Location(world, startX, y, startZ);
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
            try (ClipboardReader reader = format.getReader(new FileInputStream(inst.schematicFile))) {
                Clipboard clipboard = reader.read();

                ClipboardHolder holder = new ClipboardHolder(clipboard);
                EditSession session = WorldEdit.getInstance()
                        .newEditSession(BukkitAdapter.adapt(inst.pasteOrigin.getWorld()));

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
        String gameName = playerFfaGame.get(dead.getUniqueId());
        if (gameName == null) return;

        e.setDeathMessage(null);
        e.getDrops().clear();
        e.setDroppedExp(0);

        Player killer = dead.getKiller();
        if (killer != null && allowDamage(killer, dead)) {
            Profile pk = Craftmen.get().getProfileManager().getProfile(killer);
            if (pk != null) pk.addFfaKill();
        }
        Profile pd = Craftmen.get().getProfileManager().getProfile(dead);
        if (pd != null) pd.addFfaDeath();

        FfaInstance inst = instances.get(gameName);
        if (inst == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!isInFfa(dead)) return;
            teleportToSafeSpawn(dead, inst);
            inst.game.applyLoadout(dead);
            dead.updateInventory();
        });
    }
}
