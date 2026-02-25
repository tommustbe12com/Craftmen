package com.tommustbe12.craftmen.profile;

import org.bukkit.entity.Player;

public class Profile {

    private final Player player;
    private PlayerState state;

    public Profile(Player player) {
        this.player = player;
        this.state = PlayerState.LOBBY;
    }

    public Player getPlayer() { return player; }
    public PlayerState getState() { return state; }
    public void setState(PlayerState state) { this.state = state; }
}