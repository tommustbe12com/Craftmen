package com.tommustbe12.craftmen.potion;

import org.bukkit.potion.PotionEffectType;

public enum AutoPotion {
    SPEED(PotionEffectType.SPEED),
    STRENGTH(PotionEffectType.STRENGTH),
    FIRE_RESISTANCE(PotionEffectType.FIRE_RESISTANCE);

    private final PotionEffectType effectType;

    AutoPotion(PotionEffectType effectType) {
        this.effectType = effectType;
    }

    public PotionEffectType effectType() {
        return effectType;
    }
}
