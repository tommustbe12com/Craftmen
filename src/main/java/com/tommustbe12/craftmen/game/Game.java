package com.tommustbe12.craftmen.game;

import com.tommustbe12.craftmen.match.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public abstract class Game {

    private final String name;

    public Game(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public abstract void applyLoadout(Player player);
    public abstract void onStart(Match match);
    public abstract void onEnd(Match match);
    public void onDamage(EntityDamageByEntityEvent event) {}
    public abstract void onDeath(Player player, Player killer, Match match);
}