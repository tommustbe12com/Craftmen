package com.tommustbe12.craftmen.souls;

public enum SoulCharacter {
    GOOP("Goop"),
    DEVILS_FROST("The Devil's Frost"),
    VOICE_OF_THE_SEA("Voice of the Sea"),
    MAGNET("Magnet"),
    ARTIFICIAL_GENOCIDE("Artificial Genocide"),
    COSMIC_DESTROYER("Cosmic Destroyer");

    private final String displayName;

    SoulCharacter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
