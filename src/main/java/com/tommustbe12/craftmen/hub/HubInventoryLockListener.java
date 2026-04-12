package com.tommustbe12.craftmen.hub;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;

public final class HubInventoryLockListener implements Listener {

    private boolean shouldLock(Player player) {
        if (player == null) return false;
        if (Craftmen.get().getEndFightManager().isInGame(player)) return false;
        if (Craftmen.get().getFfaManager().isInFfa(player)) return false;
        if (Craftmen.get().getMatchManager().getMatch(player) != null) return false;
        // Allow OPs in creative to edit/build and manage inventory in spawn.
        if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) return false;
        return Craftmen.get().getProfileManager().getProfile(player).getState() == PlayerState.LOBBY;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!shouldLock(player)) return;

        // Only lock the player's own inventory; allow interacting with GUIs (they cancel themselves).
        if (e.getClickedInventory() == null) return;
        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!shouldLock(player)) return;
        // Dragging into player inventory should be blocked.
        e.setCancelled(true);
    }
}
