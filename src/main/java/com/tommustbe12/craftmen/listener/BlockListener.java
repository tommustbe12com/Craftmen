package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        PlayerState state = Craftmen.get().getProfileManager().getProfile(player).getState();

        if (state != PlayerState.IN_MATCH) {
            e.setCancelled(true);
            player.sendMessage("§cYou can't break this here!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        PlayerState state = Craftmen.get().getProfileManager().getProfile(player).getState();

        if (state != PlayerState.IN_MATCH) {
            e.setCancelled(true);
            player.sendMessage("§cYou can't place this here!");
        }
    }
}