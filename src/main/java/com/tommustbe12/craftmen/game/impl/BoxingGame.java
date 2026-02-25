package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.entity.Player;

public class BoxingGame extends Game {

    public BoxingGame() { super("Boxing"); }

    @Override
    public void applyLoadout(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }

    @Override
    public void onStart(Match match) { }

    @Override
    public void onEnd(Match match) { }

    @Override
    public void onDeath(Player player, Player killer, Match match) {
        match.end(killer);
    }
}