package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

public class BoxingGame extends Game {

    public BoxingGame() {
        super("Boxing", Material.POTATO);
    }

    @Override
    public void applyLoadout(Player player) {
        player.getInventory().clear();
    }

    @Override
    public void onStart(Match match) {
        // n/a
    }

    @Override
    public void onEnd(Match match) {
        match.getP1().sendMessage("§6Match ended!");
        match.getP2().sendMessage("§6Match ended!");
    }
}