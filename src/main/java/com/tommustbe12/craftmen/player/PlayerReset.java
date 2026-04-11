package com.tommustbe12.craftmen.player;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public final class PlayerReset {

    private PlayerReset() {}

    /**
     * Clears any transient combat/match metadata that can leak between modes
     * (invulnerability, flight, potion effects, etc). Does not teleport.
     */
    public static void clearTransientState(Player player) {
        if (player == null || !player.isOnline()) return;

        for (PotionEffectType type : PotionEffectType.values()) {
            if (type != null) player.removePotionEffect(type);
        }

        player.setFireTicks(0);
        player.setFallDistance(0);

        player.setInvulnerable(false);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setCollidable(true);
        player.setGlowing(false);

        // restore defaults in case a mode changed them
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    /**
     * Full reset to hub baseline: clears transient state, restores health/food,
     * clears inventory and gives hub items, and teleports to hub.
     */
    public static void resetToHub(Player player) {
        if (player == null || !player.isOnline()) return;

        clearTransientState(player);

        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);

        player.getInventory().clear();
        player.teleport(Craftmen.get().getHubLocation());
        Craftmen.get().getHubManager().giveHubItems(player);
        player.updateInventory();
    }
}

