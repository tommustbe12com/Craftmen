package com.tommustbe12.craftmen.potion;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AutoPotionApplier {

    private AutoPotionApplier() {}

    public static void applyAtStartIfPresent(Player player) {
        if (player == null) return;

        // Detect which "common" potions exist in the current loadout and auto-splash them once.
        Map<AutoPotion, DetectedPotion> found = new EnumMap<>(AutoPotion.class);

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            DetectedPotion detected = detectAllowedSplashPotion(it);
            if (detected == null) continue;

            DetectedPotion prev = found.get(detected.kind);
            if (prev == null || detected.isStrongerThan(prev)) {
                found.put(detected.kind, detected);
            }
        }

        if (found.isEmpty()) return;

        // Splash each potion type above the player's head, downward.
        Location base = player.getLocation().clone().add(0, 2.0, 0);
        for (DetectedPotion p : found.values()) {
            ItemStack potion = potionItemFor(p);
            if (potion == null) continue;
            spawnSplash(player, base, potion);
        }
    }

    private static DetectedPotion detectAllowedSplashPotion(ItemStack it) {
        if (it == null || it.getType() != Material.SPLASH_POTION) return null;
        if (!(it.getItemMeta() instanceof PotionMeta meta)) return null;

        // Support both base potion types and custom-effect variants.
        // We only auto-splash for Speed/Strength/Fire Res, but we tolerate "variants" where servers add
        // a redundant extra custom effect (still treat as Speed/Strength/Fire Res).
        if (meta.hasCustomEffects()) {
            Set<PotionEffectType> types = new HashSet<>();
            int speedAmp = -1;
            int speedDur = 0;
            int strengthAmp = -1;
            int strengthDur = 0;
            int fireResDur = 0;

            for (PotionEffect e : meta.getCustomEffects()) {
                if (e == null || e.getType() == null) continue;
                PotionEffectType t = e.getType();
                types.add(t);
                if (t.equals(PotionEffectType.SPEED)) {
                    speedAmp = Math.max(speedAmp, e.getAmplifier());
                    speedDur = Math.max(speedDur, e.getDuration());
                } else if (t.equals(PotionEffectType.STRENGTH)) {
                    strengthAmp = Math.max(strengthAmp, e.getAmplifier());
                    strengthDur = Math.max(strengthDur, e.getDuration());
                } else if (t.equals(PotionEffectType.FIRE_RESISTANCE)) {
                    fireResDur = Math.max(fireResDur, e.getDuration());
                }
            }

            // If a custom potion includes the effect we care about (even with extra "variant" effects),
            // prefer custom matching so Speed I vs II works reliably.
            if (speedAmp >= 0) return DetectedPotion.custom(AutoPotion.SPEED, speedAmp, speedDur);
            if (strengthAmp >= 0) return DetectedPotion.custom(AutoPotion.STRENGTH, strengthAmp, strengthDur);
            if (fireResDur > 0) return DetectedPotion.custom(AutoPotion.FIRE_RESISTANCE, 0, fireResDur);
        }

        PotionType base = meta.getBasePotionType();
        if (base == null) return null;

        return switch (base) {
            case SWIFTNESS, LONG_SWIFTNESS, STRONG_SWIFTNESS -> DetectedPotion.base(AutoPotion.SPEED, base);
            case STRENGTH, LONG_STRENGTH, STRONG_STRENGTH -> DetectedPotion.base(AutoPotion.STRENGTH, base);
            case FIRE_RESISTANCE, LONG_FIRE_RESISTANCE -> DetectedPotion.base(AutoPotion.FIRE_RESISTANCE, base);
            default -> null;
        };
    }

    private static ItemStack potionItemFor(DetectedPotion p) {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return null;

        if (p.custom) {
            PotionEffectType type = switch (p.kind) {
                case SPEED -> PotionEffectType.SPEED;
                case STRENGTH -> PotionEffectType.STRENGTH;
                case FIRE_RESISTANCE -> PotionEffectType.FIRE_RESISTANCE;
            };
            meta.addCustomEffect(new PotionEffect(type, Math.max(1, p.durationTicks), Math.max(0, p.amplifier)), true);
        } else {
            // If it's a base potion, splash the same base variant (Speed I vs II, long vs strong, etc).
            meta.setBasePotionType(p.baseType);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static void spawnSplash(Player player, Location loc, ItemStack potion) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawn(loc, ThrownPotion.class, thrown -> {
            thrown.setItem(potion);
            thrown.setShooter(player);
            thrown.setVelocity(new Vector(0, -0.6, 0));
        });
        // Ensure the entity has a tick to apply effects in tight start timings.
        org.bukkit.Bukkit.getScheduler().runTask(Craftmen.get(), player::updateInventory);
    }

    private static final class DetectedPotion {
        private final AutoPotion kind;
        private final boolean custom;
        private final PotionType baseType;
        private final int amplifier;
        private final int durationTicks;

        private DetectedPotion(AutoPotion kind, boolean custom, PotionType baseType, int amplifier, int durationTicks) {
            this.kind = kind;
            this.custom = custom;
            this.baseType = baseType;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
        }

        static DetectedPotion base(AutoPotion kind, PotionType baseType) {
            return new DetectedPotion(kind, false, baseType, 0, 0);
        }

        static DetectedPotion custom(AutoPotion kind, int amplifier, int durationTicks) {
            return new DetectedPotion(kind, true, null, amplifier, durationTicks);
        }

        boolean isStrongerThan(DetectedPotion other) {
            if (other == null) return true;
            if (this.kind != other.kind) return false;

            // Prefer custom over base (more precise), then higher amplifier, then longer duration.
            if (this.custom && !other.custom) return true;
            if (!this.custom && other.custom) return false;
            if (this.amplifier != other.amplifier) return this.amplifier > other.amplifier;
            return this.durationTicks > other.durationTicks;
        }
    }
}
