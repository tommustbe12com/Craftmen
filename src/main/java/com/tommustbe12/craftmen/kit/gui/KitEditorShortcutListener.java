package com.tommustbe12.craftmen.kit.gui;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class KitEditorShortcutListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) return;

        String name = item.getItemMeta().getDisplayName();
        if (!"Â§bKit Editor".equals(name)) return;

        e.setCancelled(true);
        Craftmen.get().getKitEditorMenu().openSelect(e.getPlayer());
    }
}

