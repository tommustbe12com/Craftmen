package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SumoGame extends Game {

    public SumoGame() {
        super("Sumo", Material.LEAD);
    }

    @Override
    public void applyLoadout(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    @Override
    public void onStart(Match match) {
        // nothing needed — all handled by global listeners
    }

    @Override
    public void onEnd(Match match) {
        // nothing needed — all handled by global listeners
    }
}