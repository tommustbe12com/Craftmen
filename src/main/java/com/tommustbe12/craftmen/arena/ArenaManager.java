package com.tommustbe12.craftmen.arena;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class ArenaManager {

    private final List<Arena> arenas = new ArrayList<>();

    public void loadArenas() {
        for (String key : Craftmen.get().getConfig().getConfigurationSection("arenas").getKeys(false)) {
            // positions for each
            double x1 = Craftmen.get().getConfig().getDouble("arenas." + key + ".pos1.x");
            double y1 = Craftmen.get().getConfig().getDouble("arenas." + key + ".pos1.y");
            double z1 = Craftmen.get().getConfig().getDouble("arenas." + key + ".pos1.z");

            double x2 = Craftmen.get().getConfig().getDouble("arenas." + key + ".pos2.x");
            double y2 = Craftmen.get().getConfig().getDouble("arenas." + key + ".pos2.y");
            double z2 = Craftmen.get().getConfig().getDouble("arenas." + key + ".pos2.z");

            Location spawn1 = new Location(Bukkit.getWorld("world"), x1, y1, z1);
            Location spawn2 = new Location(Bukkit.getWorld("world"), x2, y2, z2);

            arenas.add(new Arena(key, spawn1, spawn2));
        }
    }

    public List<Arena> getArenas() { return arenas; }
}