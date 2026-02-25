package com.tommustbe12.craftmen.arena;

import org.bukkit.Location;

public class Arena {

    private final String name;
    private final Location spawn1;
    private final Location spawn2;

    public Arena(String name, Location spawn1, Location spawn2) {
        this.name = name;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
    }
    // evil arena sigma sigma

    public String getName() { return name; }
    public Location getSpawn1() { return spawn1; }
    public Location getSpawn2() { return spawn2; }
}