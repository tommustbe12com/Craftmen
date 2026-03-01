package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementLockListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        PlayerState state = Craftmen.get().getProfileManager().getProfile(player).getState();

        // Countdown movement lock
        if (state == PlayerState.COUNTDOWN) {
            Location from = e.getFrom();
            Location to = e.getTo();
            if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
                e.setTo(new Location(from.getWorld(), from.getX(), to.getY(), from.getZ(), to.getYaw(), to.getPitch()));
            }
            return;
        }

        // Sumo water elimination
        if (state != PlayerState.IN_MATCH) return;

        Match match = Craftmen.get().getMatchManager().getMatch(player);
        if (match == null) return;
        if (!match.getGame().getName().equals("Sumo")) return;

        if (player.getLocation().getBlock().getType() == Material.WATER) {
            Player opponent = match.getP1().equals(player) ? match.getP2() : match.getP1();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
            Craftmen.get().getMatchManager().endMatch(match, opponent);
        }
    }
}