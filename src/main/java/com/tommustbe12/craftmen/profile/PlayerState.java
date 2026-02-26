package com.tommustbe12.craftmen.profile;

public enum PlayerState {
    LOBBY,
    QUEUED,
    COUNTDOWN, // player frozen, PvP disabled
    IN_MATCH // player can move and PvP works
}