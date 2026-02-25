package com.tommustbe12.craftmen.queue;

import com.tommustbe12.craftmen.game.Game;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Queue {

    private final Game game;
    private final List<Player> players = new ArrayList<>();

    public Queue(Game game) {
        this.game = game;
    }

    public Game getGame() { return game; }

    public void addPlayer(Player player) {
        if (!players.contains(player)) players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public boolean hasEnoughPlayers() {
        return players.size() >= 2;
    }

    public List<Player> getPlayers() { return players; }

    public Player[] pollPlayers() {
        if (!hasEnoughPlayers()) return null;
        Player p1 = players.remove(0);
        Player p2 = players.remove(0);
        return new Player[]{p1, p2};
    }
}