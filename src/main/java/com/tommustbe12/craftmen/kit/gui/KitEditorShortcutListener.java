package com.tommustbe12.craftmen.kit.gui;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.ChatColor;
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
        if (!(ChatColor.AQUA + "Kit Editor").equals(name)) return;

        e.setCancelled(true);
        if (Craftmen.get().getProfileManager().getProfile(e.getPlayer()).getState() != PlayerState.LOBBY) {
            e.getPlayer().sendMessage(ChatColor.RED + "You can only edit kits in the hub.");
            return;
        }
        Craftmen.get().getKitEditorMenu().openSelect(e.getPlayer());
    }
}
