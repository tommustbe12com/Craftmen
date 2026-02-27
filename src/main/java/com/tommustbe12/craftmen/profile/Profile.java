package com.tommustbe12.craftmen.profile;

import org.bukkit.entity.Player;

public class Profile {

    private final Player player;
    private PlayerState state;

    private int wins;
    private int losses;
    private int deaths;

    public Profile(Player player) {
        this.player = player;
        this.state = PlayerState.LOBBY;
    }

    public Player getPlayer() { return player; }

    public PlayerState getState() { return state; }
    public void setState(PlayerState state) { this.state = state; }

    // ===== STATS =====

    public int getWins() { return wins; }
    public void addWin() { this.wins++; }

    public int getLosses() { return losses; }
    public void addLoss() { this.losses++; }

    public int getDeaths() { return deaths; }
    public void addDeath() { this.deaths++; }

    public void setWins(int wins) { this.wins = wins; }
    public void setLosses(int losses) { this.losses = losses; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
}