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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaManager {

    private final Map<String, List<Arena>> arenasByCategory = new HashMap<>();
    private final File baseFolder = new File("plugins/Craftmen/arenas");

    public ArenaManager() {
        if (!baseFolder.exists()) baseFolder.mkdirs();
        loadAllArenas();
    }

    /**
     * Scans /arenas/<category>/ folders and loads all schematics as arenas
     */
    private void loadAllArenas() {
        File[] categories = baseFolder.listFiles(File::isDirectory);
        if (categories == null) return;

        for (File category : categories) {
            List<Arena> list = new ArrayList<>();
            File[] files = category.listFiles((dir, name) -> name.endsWith(".schem"));
            if (files == null) continue;

            for (File schem : files) {
                list.add(new Arena(schem.getName().replace(".schem", ""), category.getName(), null, null));
            }

            arenasByCategory.put(category.getName(), list);
        }
    }

    /**
     * Get all arenas in a category
     */
    public List<Arena> getArenas(String category) {
        return arenasByCategory.getOrDefault(category, new ArrayList<>());
    }

    /**
     * Paste a schematic arena at a location (auto sets spawn1/spawn2)
     */
    public void pasteArena(String category, String arenaName, Location pasteLocation, Match match) {
        try {
            File file = new File(baseFolder, category + "/" + arenaName + ".schem");
            if (!file.exists()) {
                Bukkit.getLogger().warning("Schematic " + arenaName + " not found in category " + category);
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

                // Paste at the location
                BlockVector3 to = BlockVector3.at(pasteLocation.getX(), pasteLocation.getY(), pasteLocation.getZ());
                Operations.complete(holder.createPaste(session)
                        .to(to)
                        .ignoreAirBlocks(false)
                        .build());
                session.close();

                BlockVector3 clipOrigin = clipboard.getOrigin();
                BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                BlockVector3 max = clipboard.getRegion().getMaximumPoint();
                BlockVector3 offset = min.subtract(clipOrigin);

                Location minCorner = pasteLocation.clone().add(offset.getX(), offset.getY(), offset.getZ());
                Location maxCorner = minCorner.clone().add(
                        max.getX() - min.getX(),
                        max.getY() - min.getY(),
                        max.getZ() - min.getZ()
                );

                if (match != null) {
                    match.setPasteMinLocation(minCorner);
                    match.setPasteMaxLocation(maxCorner);
                }

                // Save arena in memory
                List<Arena> list = arenasByCategory.get(category);
                if (list == null) {
                    list = new ArrayList<>();
                    arenasByCategory.put(category, list);
                }
                list.removeIf(a -> a.getName().equals(arenaName)); // remove old
                list.add(new Arena(arenaName, category, minCorner, null)); // spawn2 null if not needed
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void removeArenaAtLocation(String arenaName, Location min, Location max) {

        if (min == null || min.getWorld() == null) {
            Bukkit.getLogger().warning("[ArenaManager] Cannot remove arena '" + arenaName + "' - min of location is null.");
            return;
        }

        if (max == null || max.getWorld() == null) {
            Bukkit.getLogger().warning("[ArenaManager] Cannot remove arena '" + arenaName + "' - max of location is null.");
            return;
        }

        World world = min.getWorld();

        int minX = min.getBlockX();
        int maxX = max.getBlockX();
        int minY = min.getBlockY();
        int maxY = max.getBlockY();
        int minZ = min.getBlockZ();
        int maxZ = max.getBlockZ();

        Bukkit.getLogger().info("[ArenaManager] MIN:" + minX + " " +  minY + " " + minZ + ", MAX: " + maxX + " " + maxY + " " + maxZ + ".");

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
                "'. Blocks cleared: " + blocksCleared);
    }

    public boolean isAreaEmpty(Location loc, int width, int length, World world) {
        int yStart = loc.getBlockY();
        int yEnd = yStart + 10; // check 10 blocks vertically; adjust if needed

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                for (int y = yStart; y <= yEnd; y++) {
                    if (!world.getBlockAt(loc.getBlockX() + x, y, loc.getBlockZ() + z).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Return width (X dimension) of a schematic in blocks
    public int getSchematicWidth(String category, String arenaName) {
        try {
            File file = new File(baseFolder, category + "/" + arenaName + ".schem");
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) return 0;

            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                Clipboard clipboard = reader.read();
                return clipboard.getDimensions().getX();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getSchematicLength(String category, String arenaName) {
        try {
            File file = new File(baseFolder, category + "/" + arenaName + ".schem");
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) return 0;

            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                Clipboard clipboard = reader.read();
                return clipboard.getDimensions().getZ();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getSchematicHeight(String category, String arenaName) {
        try {
            File file = new File(baseFolder, category + "/" + arenaName + ".schem");
            if (!file.exists()) return 0;

            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) return 0;

            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                Clipboard clipboard = reader.read();
                return clipboard.getDimensions().getY();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}