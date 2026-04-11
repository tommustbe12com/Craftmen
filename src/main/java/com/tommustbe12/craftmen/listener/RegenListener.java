package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.Set;

public class RegenListener implements Listener {
    private static final Set<String> NO_NATURAL_REGEN = Set.of(
            "Sword",
            "Axe",
            "UHC",
            "Invis",
            "Sumo",
            "Netherite Sword",
            "Spleef"
    );

    @EventHandler(priority = EventPriority.HIGH)
    public void onRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Match match = Craftmen.get().getMatchManager().getMatch(player);
        String gameName = match == null ? null : match.getGame().getName();
        if (match == null && Craftmen.get().getFfaManager().isInFfa(player)) {
            Game g = Craftmen.get().getFfaManager().getGame(player);
            gameName = g == null ? null : g.getName();
        }
        if (gameName == null) return;

        // Only cancel natural regen
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            if (NO_NATURAL_REGEN.contains(gameName)) {
                event.setCancelled(true);
            }
        }
    }
}
