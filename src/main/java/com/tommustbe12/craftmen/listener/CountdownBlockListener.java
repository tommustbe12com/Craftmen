package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityShootBowEvent;

public class CountdownBlockListener implements Listener {

    private boolean isCountdown(Player player) {
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        return profile != null && profile.getState() == PlayerState.COUNTDOWN;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!isCountdown(player)) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (!isCountdown(player)) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onPearl(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!isCountdown(player)) return;

        if (event.getItem() != null && event.getItem().getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (!isCountdown(player)) return;

        event.setCancelled(true);
    }
}