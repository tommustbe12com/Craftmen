package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class CombatListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) return;

        Player damaged = (Player) e.getEntity();
        Player damager = (Player) e.getDamager();

        PlayerState damagedState = Craftmen.get().getProfileManager().getProfile(damaged).getState();
        PlayerState damagerState = Craftmen.get().getProfileManager().getProfile(damager).getState();

        // Only allow PvP if both are IN_MATCH
        if (damagedState != PlayerState.IN_MATCH || damagerState != PlayerState.IN_MATCH) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageCheckDeath(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        PlayerState state = Craftmen.get().getProfileManager().getProfile(player).getState();
        if (state != PlayerState.IN_MATCH) return;

        // Check if damage would kill the player
        if (player.getHealth() - e.getFinalDamage() <= 0) {
            e.setCancelled(true); // prevent default death
            handleCustomDeath(player);
        }
    }

    private void handleCustomDeath(Player dead) {
        // suppress, remove drop ability
        dead.getInventory().clear();
        dead.setHealth(dead.getMaxHealth()); // reset health

        Match match = Craftmen.get().getMatchManager().getMatch(dead);
        if (match == null) {
            System.out.println("No match found for " + dead.getName() + ", this should NOT happen.");
            return;
        }

        Player winner = match.getP1() == dead ? match.getP2() : match.getP1();
        Craftmen.get().getMatchManager().endMatch(match, winner);
    }
}