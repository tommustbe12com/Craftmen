package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class CombatListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {

        if (!(e.getEntity() instanceof Player damaged)) return;
        if (!(e.getDamager() instanceof Player damager)) return;

        // Allow damage in End Fight
        if (Craftmen.get().getEndFightManager().isInGame(damaged)
                && Craftmen.get().getEndFightManager().isInGame(damager)) {
            return;
        }

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
            e.setDamage(0.0);
            player.setNoDamageTicks(0);
        } else {
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

        if (match.getGame().getName().equals("Sumo")) return;

        double finalHealth = player.getHealth() - e.getFinalDamage();

        if (finalHealth <= 0) {

            if (hasTotem(player)) {
                return;
            }

            e.setCancelled(true);
            handleCustomDeath(player);
        }
    }

    @EventHandler
    public void onTotemPop(EntityResurrectEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        Match match = Craftmen.get().getMatchManager().getMatch(player);
        if (match == null) return;

        if (e.isCancelled()) return;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();

        Match match = Craftmen.get().getMatchManager().getMatch(dead);
        if (match == null) return;

        e.setDeathMessage(null);
        e.getDrops().clear();
        e.setDroppedExp(0);

        Player winner = match.getP1().equals(dead) ? match.getP2() : match.getP1();
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

        Player winner = match.getP1().equals(dead) ? match.getP2() : match.getP1();
        Craftmen.get().getMatchManager().endMatch(match, winner);
    }

    private boolean hasTotem(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        return (mainHand != null && mainHand.getType() == Material.TOTEM_OF_UNDYING)
                || (offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING);
    }
}