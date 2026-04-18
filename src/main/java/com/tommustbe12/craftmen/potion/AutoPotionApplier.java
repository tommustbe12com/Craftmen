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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class AutoPotionApplier {

    private AutoPotionApplier() {}

    public static void applyAtStartIfPresent(Player player) {
        if (player == null) return;

        // Detect which "common" potions exist in the current loadout and auto-splash them once.
        EnumSet<AutoPotion> found = EnumSet.noneOf(AutoPotion.class);

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            AutoPotion p = detectAllowedSplashPotion(it);
            if (p == null) continue;
            found.add(p);
        }

        if (found.isEmpty()) return;

        // Splash each potion type above the player's head, downward.
        Location base = player.getLocation().clone().add(0, 2.0, 0);
        for (AutoPotion p : found) {
            ItemStack potion = potionItemFor(p);
            if (potion == null) continue;
            spawnSplash(player, base, potion);
        }
    }

    private static AutoPotion detectAllowedSplashPotion(ItemStack it) {
        if (it == null || it.getType() != Material.SPLASH_POTION) return null;
        if (!(it.getItemMeta() instanceof PotionMeta meta)) return null;

        // Exclude mixed custom-effect potions (e.g., Turtle Master) and anything not speed/strength.
        if (meta.hasCustomEffects()) {
            Set<PotionEffectType> types = new HashSet<>();
            for (PotionEffect e : meta.getCustomEffects()) types.add(e.getType());
            if (types.size() != 1) return null;
            PotionEffectType only = types.iterator().next();
            if (only.equals(PotionEffectType.SPEED)) return AutoPotion.SPEED;
            if (only.equals(PotionEffectType.STRENGTH)) return AutoPotion.STRENGTH;
            if (only.equals(PotionEffectType.FIRE_RESISTANCE)) return AutoPotion.FIRE_RESISTANCE;
            return null;
        }

        PotionType base = meta.getBasePotionType();
        if (base == null) return null;

        return switch (base) {
            case SWIFTNESS, LONG_SWIFTNESS, STRONG_SWIFTNESS -> AutoPotion.SPEED;
            case STRENGTH, LONG_STRENGTH, STRONG_STRENGTH -> AutoPotion.STRENGTH;
            case FIRE_RESISTANCE, LONG_FIRE_RESISTANCE -> AutoPotion.FIRE_RESISTANCE;
            default -> null;
        };
    }

    private static ItemStack potionItemFor(AutoPotion p) {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return null;

        PotionType type = switch (p) {
            case SPEED -> PotionType.STRONG_SWIFTNESS;
            case STRENGTH -> PotionType.STRONG_STRENGTH;
            case FIRE_RESISTANCE -> PotionType.LONG_FIRE_RESISTANCE;
        };
        meta.setBasePotionType(type);
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
}
