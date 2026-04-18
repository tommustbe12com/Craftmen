package com.tommustbe12.craftmen.cosmetics;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CosmeticsDamageListener implements Listener {

    private final NamespacedKey fireworkOwnerKey = new NamespacedKey(Craftmen.get(), "cosmetic_firework_owner");
    private final Map<UUID, Long> lightningProtectUntil = new HashMap<>();

    public void protectFromLightning(UUID playerId, long untilMillis) {
        if (playerId == null) return;
        lightningProtectUntil.put(playerId, untilMillis);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireworkDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Firework fw)) return;

        String raw = fw.getPersistentDataContainer().get(fireworkOwnerKey, PersistentDataType.STRING);
        if (raw == null) return;
        UUID owner;
        try {
            owner = UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        if (victim.getUniqueId().equals(owner)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLightningDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) return;

        Long until = lightningProtectUntil.get(player.getUniqueId());
        if (until == null) return;
        if (System.currentTimeMillis() <= until) {
            e.setCancelled(true);
        } else {
            lightningProtectUntil.remove(player.getUniqueId());
        }
    }
}

