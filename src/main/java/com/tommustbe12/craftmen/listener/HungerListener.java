package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import java.util.Set;

public class HungerListener implements Listener {

    private static final Set<String> NO_HUNGER_LOSS = Set.of(
            "Sword",
            "Boxing",
            "Axe",
            "Combo",
            "Gapple"
    );

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Match match = Craftmen.get().getMatchManager().getMatch(player);

        // always cancel in hub
        if (match == null) {
            event.setCancelled(true);
            return;
        }

        // only cancel for certain gms
        if (NO_HUNGER_LOSS.contains(match.getGame().getName())) {
            event.setCancelled(true);
        }
    }
}