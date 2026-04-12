package com.tommustbe12.craftmen.cosmetics.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.cosmetics.CosmeticsApplier;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class KillDeathCosmeticsListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        Location loc = dead.getLocation();

        // Covers vanilla death flows (e.g. End Fight). For matches/FFA where death is custom-handled,
        // we invoke CosmeticsApplier from those managers/listeners.
        CosmeticsApplier.applyKillDeath(killer, dead, loc);
    }
}
