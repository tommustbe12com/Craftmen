package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.match.MatchManager;
import com.tommustbe12.craftmen.profile.Profile;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final MatchManager matchManager = Craftmen.get().getMatchManager();

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player damaged = (Player) e.getEntity();
        Player damager = null;
        if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();

        Profile damagedProfile = Craftmen.get().getProfileManager().getProfile(damaged);
        if (damager != null) {
            Profile damagerProfile = Craftmen.get().getProfileManager().getProfile(damager);
            if (damagedProfile.getState() != PlayerState.IN_MATCH || damagerProfile.getState() != PlayerState.IN_MATCH) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent e) {
        Player dead = e.getEntity();
        for (Match match : matchManager.getMatches()) {
            if (match.getP1() == dead) match.end(match.getP2());
            else if (match.getP2() == dead) match.end(match.getP1());
        }
    }
}