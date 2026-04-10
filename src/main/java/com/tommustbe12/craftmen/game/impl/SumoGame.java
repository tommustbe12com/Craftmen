package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SumoGame extends Game {

    public SumoGame() {
        super("Sumo", new ItemStack(Material.LEAD));
    }

    @Override
    public Kit createDefaultKit() {
        return Kit.empty();
    }

    @Override
    protected void afterLoadoutApplied(Player player) {
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
