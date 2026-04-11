package com.tommustbe12.craftmen.profile;

public enum PlayerState {
    LOBBY,
    QUEUED,
    CUSTOM_KIT_QUEUED,
    COUNTDOWN, // player frozen, PvP disabled
    IN_MATCH, // player can move and PvP works
    CUSTOM_KIT_PLAYING // hub-only player kit duel
}
