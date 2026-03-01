package com.tommustbe12.craftmen.arena;

import org.bukkit.Location;

public class Arena {

    private final String name;
    private final String category;
    private final Location spawn1;
    private final Location spawn2;

    public Arena(String name, String category, Location spawn1, Location spawn2) {
        this.name = name;
        this.category = category;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public Location getSpawn1() { return spawn1; }
    public Location getSpawn2() { return spawn2; }
}