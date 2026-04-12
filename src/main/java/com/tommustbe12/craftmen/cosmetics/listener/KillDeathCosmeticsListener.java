package com.tommustbe12.craftmen.cosmetics.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.meta.FireworkMeta;

public final class KillDeathCosmeticsListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        Profile killerProfile = Craftmen.get().getProfileManager().getProfile(killer);
        Profile deadProfile = Craftmen.get().getProfileManager().getProfile(dead);
        if (killerProfile == null || deadProfile == null) return;

        Location loc = dead.getLocation();

        applyKillEffect(killerProfile, loc);
        applyDeathEffect(deadProfile, loc);
        playKillSound(killerProfile, killer);
        playDeathSound(deadProfile, dead);
    }

    private void applyKillEffect(Profile profile, Location loc) {
        String id = profile.getSelectedKillEffect();
        if (id == null) return;
        switch (id) {
            case "kill.lightning" -> loc.getWorld().strikeLightningEffect(loc);
            case "kill.firework" -> spawnFirework(loc, Color.AQUA);
        }
    }

    private void applyDeathEffect(Profile profile, Location loc) {
        String id = profile.getSelectedDeathEffect();
        if (id == null) return;
        switch (id) {
            case "death.lightning" -> loc.getWorld().strikeLightningEffect(loc);
            case "death.firework" -> spawnFirework(loc, Color.RED);
        }
    }

    private void playKillSound(Profile profile, Player player) {
        String raw = profile.getSelectedKillSound();
        if (raw == null) return;
        Sound sound = parseSound(raw);
        if (sound == null) return;
        player.playSound(player.getLocation(), sound, 1.0f, 1.2f);
    }

    private void playDeathSound(Profile profile, Player player) {
        String raw = profile.getSelectedDeathSound();
        if (raw == null) return;
        Sound sound = parseSound(raw);
        if (sound == null) return;
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    private Sound parseSound(String raw) {
        try {
            return Sound.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void spawnFirework(Location loc, Color color) {
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
        // Detonate immediately (next tick) so it acts like an effect.
        org.bukkit.Bukkit.getScheduler().runTaskLater(Craftmen.get(), fw::detonate, 1L);
    }
}

