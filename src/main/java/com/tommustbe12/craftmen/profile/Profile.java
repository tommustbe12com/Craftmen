package com.tommustbe12.craftmen.profile;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

public class Profile {

    private final Player player;
    private PlayerState state;

    private int wins;
    private int losses;

    private final Map<String, Integer> gameWins = new HashMap<>();
    private final Map<String, Integer> gameLosses = new HashMap<>();

    private String lastPlayedGame;

    public Profile(Player player) {
        this.player = player;
        this.state = PlayerState.LOBBY;
    }

    public Player getPlayer() { return player; }

    public PlayerState getState() { return state; }
    public void setState(PlayerState state) { this.state = state; }

    public int getWins() { return wins; }
    public void addWin(String gameName) {
        this.wins++;
        gameWins.merge(gameName, 1, Integer::sum);
    }

    public int getLosses() { return losses; }
    public void addLoss(String gameName) {
        this.losses++;
        gameLosses.merge(gameName, 1, Integer::sum);
    }

    public Map<String, Integer> getGameWins() { return gameWins; }
    public Map<String, Integer> getGameLosses() { return gameLosses; }

    public int getGameWins(String gameName) { return gameWins.getOrDefault(gameName, 0); }
    public int getGameLosses(String gameName) { return gameLosses.getOrDefault(gameName, 0); }

    public void setWins(int wins) { this.wins = wins; }
    public void setLosses(int losses) { this.losses = losses; }
    public void setGameWins(String gameName, int value) { gameWins.put(gameName, value); }
    public void setGameLosses(String gameName, int value) { gameLosses.put(gameName, value); }

    public void addStat(String stat, int amount) {
        switch (stat.toLowerCase()) {
            case "wins" -> this.wins += amount;
            case "losses" -> this.losses += amount;
        }
    }

    public void setStat(String stat, int value) {
        switch (stat.toLowerCase()) {
            case "wins" -> this.wins = value;
            case "losses" -> this.losses = value;
        }
    }

    public String getLastPlayedGame() {
        return lastPlayedGame != null ? lastPlayedGame : "None";
    }

    public void setLastPlayedGame(String gameName) {
        this.lastPlayedGame = gameName;
    }
}