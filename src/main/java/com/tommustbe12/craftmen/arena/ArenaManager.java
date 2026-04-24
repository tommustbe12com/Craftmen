package com.tommustbe12.craftmen.arena;

import com.tommustbe12.craftmen.match.Match;
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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class ArenaManager {

    private final Map<String, List<Arena>> arenasByCategory = new HashMap<>();

    private final File baseFolder = new File("plugins/Craftmen/arenas");
    private final File mainFolder = new File(baseFolder, "main");

    public ArenaManager() {
        if (!baseFolder.exists()) baseFolder.mkdirs();
        if (!mainFolder.exists()) mainFolder.mkdirs();
        loadAllArenas();
    }

    /**
     * Load arenas from:
     * arenas/main/ (default pool)
     * arenas/<gamemode>/ (override pools)
     */
    private void loadAllArenas() {

        arenasByCategory.clear();

        // Load main arenas
        List<Arena> mainArenas = new ArrayList<>();

        File[] mainFiles = mainFolder.listFiles((dir, name) -> name.endsWith(".schem"));
        if (mainFiles != null) {
            for (File schem : mainFiles) {
                mainArenas.add(new Arena(
                        schem.getName().replace(".schem", ""),
                        "main",
                        null,
                        null
                ));
            }
        }

        arenasByCategory.put("main", mainArenas);

        // Load gamemode folders
        File[] folders = baseFolder.listFiles(File::isDirectory);

        if (folders == null) return;

        for (File folder : folders) {

            if (folder.getName().equalsIgnoreCase("main")) continue;

            List<Arena> list = new ArrayList<>();

            File[] files = folder.listFiles((dir, name) -> name.endsWith(".schem"));
            if (files == null) continue;

            for (File schem : files) {
                list.add(new Arena(
                        schem.getName().replace(".schem", ""),
                        folder.getName(),
                        null,
                        null
                ));
            }

            arenasByCategory.put(folder.getName(), list);
        }
    }

    /**
     * Get arenas for gamemode
     * Falls back to main if none exist
     */
    public List<Arena> getArenas(String category) {

        if (arenasByCategory.containsKey(category) &&
                !arenasByCategory.get(category).isEmpty()) {
            return arenasByCategory.get(category);
        }

        return arenasByCategory.getOrDefault("main", new ArrayList<>());
    }

    /**
     * Paste schematic
     */
    public void pasteArena(String category, String arenaName, Location pasteLocation, Match match) {

        try {

            File file = new File(baseFolder, category + "/" + arenaName + ".schem");

            if (!file.exists()) {
                file = new File(mainFolder, arenaName + ".schem");
            }

            if (!file.exists()) {
                Bukkit.getLogger().warning("Schematic " + arenaName + " not found.");
                return;
            }

            ClipboardFormat format = ClipboardFormats.findByFile(file);

            if (format == null) {
                Bukkit.getLogger().warning("Unknown schematic format for " + arenaName);
                return;
            }

            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {

                Clipboard clipboard = reader.read();

                ClipboardHolder holder = new ClipboardHolder(clipboard);

                EditSession session = WorldEdit.getInstance()
                        .newEditSession(BukkitAdapter.adapt(pasteLocation.getWorld()));

                BlockVector3 to = BlockVector3.at(
                        pasteLocation.getX(),
                        pasteLocation.getY(),
                        pasteLocation.getZ()
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

                Location minCorner = pasteLocation.clone().add(
                        offset.getX(),
                        offset.getY(),
                        offset.getZ()
                );

                Location maxCorner = minCorner.clone().add(
                        max.getX() - min.getX(),
                        max.getY() - min.getY(),
                        max.getZ() - min.getZ()
                );

                if (match != null) {
                    match.setPasteMinLocation(minCorner);
                    match.setPasteMaxLocation(maxCorner);
                }

                List<Arena> list = arenasByCategory.get(category);

                if (list == null) {
                    list = new ArrayList<>();
                    arenasByCategory.put(category, list);
                }

                list.removeIf(a -> a.getName().equals(arenaName));
                list.add(new Arena(arenaName, category, minCorner, null));

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeArenaAtLocation(String arenaName, Location min, Location max) {

        if (min == null || min.getWorld() == null) {
            Bukkit.getLogger().warning("[ArenaManager] Cannot remove arena '" + arenaName + "' - min location null.");
            return;
        }

        if (max == null || max.getWorld() == null) {
            Bukkit.getLogger().warning("[ArenaManager] Cannot remove arena '" + arenaName + "' - max location null.");
            return;
        }

        World world = min.getWorld();

        int minX = min.getBlockX();
        int maxX = max.getBlockX();
        int minY = min.getBlockY();
        int maxY = max.getBlockY();
        int minZ = min.getBlockZ();
        int maxZ = max.getBlockZ();

        // Remove ALL non-player entities in/near the arena so explosives/crystals/projectiles don't leak into the next match.
        int margin = 6;
        int entMinX = Math.min(minX, maxX) - margin;
        int entMaxX = Math.max(minX, maxX) + margin;
        int entMinY = Math.min(minY, maxY) - margin;
        int entMaxY = Math.max(minY, maxY) + margin;
        int entMinZ = Math.min(minZ, maxZ) - margin;
        int entMaxZ = Math.max(minZ, maxZ) + margin;

        int entitiesRemoved = 0;
        for (Entity ent : new ArrayList<>(world.getEntities())) {
            if (ent instanceof Player) continue;
            Location l = ent.getLocation();
            int x = l.getBlockX();
            int y = l.getBlockY();
            int z = l.getBlockZ();
            if (x < entMinX || x > entMaxX) continue;
            if (y < entMinY || y > entMaxY) continue;
            if (z < entMinZ || z > entMaxZ) continue;
            ent.remove();
            entitiesRemoved++;
        }

        int blocksCleared = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {

                    Block block = world.getBlockAt(x, y, z);

                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR, false);
                        blocksCleared++;
                    }
                }
            }
        }

        Bukkit.getLogger().info("[ArenaManager] Finished removing arena '" + arenaName +
                "'. Blocks cleared: " + blocksCleared + ", entities removed: " + entitiesRemoved);
    }

    public boolean isAreaEmpty(Location loc, int width, int length, World world) {

        int yStart = loc.getBlockY();
        int yEnd = yStart + 10;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                for (int y = yStart; y <= yEnd; y++) {

                    if (!world.getBlockAt(
                            loc.getBlockX() + x,
                            y,
                            loc.getBlockZ() + z).isEmpty()) {

                        return false;
                    }
                }
            }
        }

        return true;
    }

    public int getSchematicWidth(String category, String arenaName) {
        return getDimension(category, arenaName, "x");
    }

    public int getSchematicLength(String category, String arenaName) {
        return getDimension(category, arenaName, "z");
    }

    public int getSchematicHeight(String category, String arenaName) {
        return getDimension(category, arenaName, "y");
    }

    private int getDimension(String category, String arenaName, String axis) {

        try {

            File file = new File(baseFolder, category + "/" + arenaName + ".schem");

            if (!file.exists()) {
                file = new File(mainFolder, arenaName + ".schem");
            }

            if (!file.exists()) return 0;

            ClipboardFormat format = ClipboardFormats.findByFile(file);

            if (format == null) return 0;

            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {

                Clipboard clipboard = reader.read();

                switch (axis) {
                    case "x": return clipboard.getDimensions().getX();
                    case "y": return clipboard.getDimensions().getY();
                    case "z": return clipboard.getDimensions().getZ();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
