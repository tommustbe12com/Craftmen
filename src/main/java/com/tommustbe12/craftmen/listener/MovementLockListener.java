package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;

public class MovementLockListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        PlayerState state = Craftmen.get().getProfileManager().getProfile(e.getPlayer()).getState();
        if (state == PlayerState.COUNTDOWN) {
            Location from = e.getFrom();
            Location to = e.getTo();
            if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
                // Keep X/Z locked, allow Y (jump)
                e.setTo(new Location(from.getWorld(), from.getX(), to.getY(), from.getZ(), to.getYaw(), to.getPitch()));
            }
        }
    }
}