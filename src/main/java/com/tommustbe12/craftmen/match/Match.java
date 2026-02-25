package com.tommustbe12.craftmen.match;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.arena.Arena;
import com.tommustbe12.craftmen.game.Game;
import org.bukkit.entity.Player;

public class Match {

    private final Player p1;
    private final Player p2;
    private final Game game;
    private final Arena arena;
    private boolean ended = false;

    public Match(Player p1, Player p2, Game game, Arena arena) {
        this.p1 = p1;
        this.p2 = p2;
        this.game = game;
        this.arena = arena;
    }

    public void start() {
        // teleport players
        p1.teleport(arena.getSpawn1());
        p2.teleport(arena.getSpawn2());

        // apply loadouts
        game.applyLoadout(p1);
        game.applyLoadout(p2);

        game.onStart(this);
    }

    public void end(Player winner) {
        if (ended) return;
        ended = true;

        game.onEnd(this);

        // reset players
        p1.getInventory().clear();
        p2.getInventory().clear();

        p1.teleport(arena.getSpawn1()); // or spawn location
        p2.teleport(arena.getSpawn2());

        // set profiles back to lobby
        Craftmen.get().getProfileManager().getProfile(p1).setState(com.tommustbe12.craftmen.profile.PlayerState.LOBBY);
        Craftmen.get().getProfileManager().getProfile(p2).setState(com.tommustbe12.craftmen.profile.PlayerState.LOBBY);

        // message winner
        winner.sendMessage("§6You won the match!");
    }

    public Game getGame() { return game; }
    public Player getP1() { return p1; }
    public Player getP2() { return p2; }
    public Arena getArena() { return arena; }
}