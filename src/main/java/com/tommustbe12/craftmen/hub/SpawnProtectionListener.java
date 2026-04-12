package com.tommustbe12.craftmen.hub;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class SpawnProtectionListener implements Listener {

    private static final int RADIUS = 100;

    private boolean inProtectedSpawn(Player player) {
        if (player == null) return false;
        Location hub = Craftmen.get().getHubLocation();
        if (hub == null || hub.getWorld() == null) return false;
        if (player.getWorld() == null || !player.getWorld().equals(hub.getWorld())) return false;

        // Only protect the hub-ish area around (0,0), per request.
        Location l = player.getLocation();
        int x = Math.abs(l.getBlockX());
        int z = Math.abs(l.getBlockZ());
        if (x > RADIUS || z > RADIUS) return false;

        int y = l.getBlockY();
        int hy = hub.getBlockY();
        return y >= (hy - RADIUS) && y <= (hy + RADIUS);
    }

    private boolean shouldProtect(Player player) {
        if (!inProtectedSpawn(player)) return false;
        if (Craftmen.get().getEndFightManager().isInGame(player)) return false;
        if (Craftmen.get().getFfaManager().isInFfa(player)) return false;
        Match match = Craftmen.get().getMatchManager().getMatch(player);
        return match == null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!shouldProtect(player)) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        if (!inProtectedSpawn(dead)) return;
        // Just in case a death slips through (plugin damage, void, etc), don't drop items at spawn.
        e.setDeathMessage(null);
        e.getDrops().clear();
        e.setDroppedExp(0);
    }
}

