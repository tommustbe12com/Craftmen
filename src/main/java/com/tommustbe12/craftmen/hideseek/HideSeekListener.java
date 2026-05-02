package com.tommustbe12.craftmen.hideseek;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class HideSeekListener implements Listener {

    private final HideSeekManager manager;

    public HideSeekListener(HideSeekManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTag(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player damaged)) return;
        if (!(e.getDamager() instanceof Player damager)) return;

        // Only care about Hide & Seek combat.
        if (!manager.allowCombat(damager, damaged)) return;

        // Never deal real damage; tags are what matter.
        e.setCancelled(true);
        e.setDamage(0.0);
        damaged.setNoDamageTicks(0);

        if (manager.isSeeker(damager)) {
            manager.handleTag(damager, damaged);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTaunt(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (player == null) return;

        var profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null) return;
        if (profile.getState() != PlayerState.HIDESEEK_PLAYING) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() == Material.AIR) return;
        if (item.getType() != Material.FIREWORK_STAR) return;

        if (!item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) return;
        String name = item.getItemMeta().getDisplayName();
        if (!name.contains("Taunt")) return;

        e.setCancelled(true);
        manager.handleTaunt(player);
    }
}

