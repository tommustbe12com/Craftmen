package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class BoxingGame extends Game {

    public BoxingGame() {
        super("Boxing", new ItemStack(Material.POTATO));
    }

    @Override
    public Kit createDefaultKit() {
        return Kit.empty();
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
