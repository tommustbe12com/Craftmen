package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

        if (damagedState != PlayerState.IN_MATCH || damagerState != PlayerState.IN_MATCH) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSumoDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        Match match = Craftmen.get().getMatchManager().getMatch(player);
        if (match == null) return;
        if (!match.getGame().getName().equals("Sumo")) return;

        if (e instanceof EntityDamageByEntityEvent) {
            // knockback only, no health loss
            e.setDamage(0.0);
            player.setNoDamageTicks(0);
        } else {
            // cancel all environmental damage in Sumo
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageCheckDeath(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        PlayerState state = Craftmen.get().getProfileManager().getProfile(player).getState();
        if (state != PlayerState.IN_MATCH) return;

        Match match = Craftmen.get().getMatchManager().getMatch(player);
        if (match == null) return;

        // Sumo has no health-based death
        if (match.getGame().getName().equals("Sumo")) return;

        if (player.getHealth() - e.getFinalDamage() <= 0) {
            e.setCancelled(true);
            handleCustomDeath(player);
        }
    }

    // Safety net in case damage cancellation slips through
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();

        Match match = Craftmen.get().getMatchManager().getMatch(dead);
        if (match == null) return;

        e.setDeathMessage(null);
        e.getDrops().clear();
        e.setDroppedExp(0);

        Player winner = match.getP1() == dead ? match.getP2() : match.getP1();
        Craftmen.get().getMatchManager().endMatch(match, winner);
    }

    private void handleCustomDeath(Player dead) {
        dead.getInventory().clear();
        dead.setHealth(dead.getMaxHealth());

        Match match = Craftmen.get().getMatchManager().getMatch(dead);
        if (match == null) {
            System.out.println("No match found for " + dead.getName() + ", this should NOT happen.");
            return;
        }

        Player winner = match.getP1() == dead ? match.getP2() : match.getP1();
        Craftmen.get().getMatchManager().endMatch(match, winner);
    }
}