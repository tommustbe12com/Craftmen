package com.tommustbe12.craftmen.cosmetics;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

public final class CosmeticsApplier {

    private CosmeticsApplier() {}

    public static void applyKillDeath(Player killer, Player dead, Location location) {
        if (killer == null || dead == null || location == null) return;
        Profile killerProfile = Craftmen.get().getProfileManager().getProfile(killer);
        Profile deadProfile = Craftmen.get().getProfileManager().getProfile(dead);
        if (killerProfile == null || deadProfile == null) return;

        applyKillEffect(killer, killerProfile, location);
        applyDeathEffect(killer, deadProfile, location);
        playKillSound(killerProfile, killer);
        playDeathSound(deadProfile, dead);

        // Streak tracking and gem rewards (per kill).
        killerProfile.setKillsInARow(Math.min(100, killerProfile.getKillsInARow() + 1));
        killerProfile.setLossesInARow(0);
        deadProfile.setLossesInARow(deadProfile.getLossesInARow() + 1);
        deadProfile.setKillsInARow(0);

        int streak = killerProfile.getKillsInARow();
        int bonus = Craftmen.get().getGemManager().getKillStreakBonus(streak);
        int gems = 1 + bonus;
        killerProfile.addGems(gems);
        Craftmen.get().saveProfile(killerProfile);
        Craftmen.get().saveProfile(deadProfile);
    }

    private static void applyKillEffect(Player killer, Profile profile, Location loc) {
        String id = profile.getSelectedKillEffect();
        if (id == null) return;
        switch (id) {
            case "kill.lightning" -> {
                loc.getWorld().strikeLightningEffect(loc);
                // Safety: if anything ends up causing lightning damage, never hurt the killer.
                Craftmen.get().getCosmeticsDamageListener().protectFromLightning(
                        killer.getUniqueId(),
                        System.currentTimeMillis() + 1500L
                );
            }
            case "kill.firework" -> spawnFirework(loc, Color.AQUA, killer);
        }
    }

    private static void applyDeathEffect(Player killer, Profile profile, Location loc) {
        String id = profile.getSelectedDeathEffect();
        if (id == null) return;
        switch (id) {
            case "death.lightning" -> {
                loc.getWorld().strikeLightningEffect(loc);
                Craftmen.get().getCosmeticsDamageListener().protectFromLightning(
                        killer.getUniqueId(),
                        System.currentTimeMillis() + 1500L
                );
            }
            case "death.firework" -> spawnFirework(loc, Color.RED, killer);
        }
    }

    private static void playKillSound(Profile profile, Player player) {
        String raw = profile.getSelectedKillSound();
        if (raw == null) return;
        Sound sound = parseSound(raw);
        if (sound == null) return;
        player.playSound(player.getLocation(), sound, 1.0f, 1.2f);
    }

    private static void playDeathSound(Profile profile, Player player) {
        String raw = profile.getSelectedDeathSound();
        if (raw == null) return;
        Sound sound = parseSound(raw);
        if (sound == null) return;
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    private static Sound parseSound(String raw) {
        try {
            return Sound.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void spawnFirework(Location loc, Color color, Player owner) {
        if (loc.getWorld() == null) return;
        Firework fw = loc.getWorld().spawn(loc.clone().add(0, 0.2, 0), Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.clearEffects();
            meta.addEffect(FireworkEffect.builder()
                    .withColor(color)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .flicker(true)
                    .trail(true)
                    .build());
            meta.setPower(0);
            firework.setFireworkMeta(meta);
        });
        if (owner != null) {
            fw.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(Craftmen.get(), "cosmetic_firework_owner"),
                    PersistentDataType.STRING,
                    owner.getUniqueId().toString()
            );
        }
        org.bukkit.Bukkit.getScheduler().runTaskLater(Craftmen.get(), fw::detonate, 1L);
    }
}
