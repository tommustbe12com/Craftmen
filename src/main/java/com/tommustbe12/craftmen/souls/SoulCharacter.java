package com.tommustbe12.craftmen.souls;

public enum SoulCharacter {
    GOOP("Goop"),
    DEVILS_FROST("The Devil's Frost"),
    VOICE_OF_THE_SEA("Voice of the Sea"),
    MAGNET("Magnet"),
    ARTIFICIAL_GENOCIDE("Code Cracker"),
    COSMIC_DESTROYER("Cosmic Destroyer"),
    SORCERER("Sorcerer"),
    KING_OF_HEAT("The Eternal Flame"),
    ARCHANGEL("Archangel"),
    BOUNTY_HUNTER("Bounty Hunter"),
    COPYCAT("Copycat"),
    BLOODY_MONARCH("Bloody Monarch"),
    DARK_KNIGHT("Dark Knight"),
    RAILGUN("Railgun"),
    UNTAMED_BEAST("Untamed Beast");

    private final String displayName;

    SoulCharacter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
