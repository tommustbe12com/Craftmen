package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.game.impl.SpleefGame;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class SnowballHitListener implements Listener {

    private static boolean isSpleef(Player player) {
        if (player == null) return false;

        Match match = Craftmen.get().getMatchManager().getMatch(player);
        if (match != null && match.getGame() != null && "Spleef".equalsIgnoreCase(match.getGame().getName())) return true;

        var ffaGame = Craftmen.get().getFfaManager().getGame(player);
        return ffaGame != null && "Spleef".equalsIgnoreCase(ffaGame.getName());
    }

    @EventHandler
    public void onSnowballHit(ProjectileHitEvent e) {
        Projectile projectile = e.getEntity();

        if (!(projectile instanceof Snowball)) return;

        if (!(projectile.getShooter() instanceof Player shooter)) return;

        if (e.getHitEntity() instanceof Player target) {
            if (!isSpleef(shooter) || !isSpleef(target)) return;

            target.setNoDamageTicks(1);

            Vector direction = target.getLocation().toVector().subtract(shooter.getLocation().toVector()).normalize();
            direction.setY(0.4); // vertical push
            target.setVelocity(direction.multiply(1.2));
        }

        Block hitBlock = e.getHitBlock();
        if (hitBlock == null) return;

        if (!isSpleef(shooter)) return;

        if (hitBlock.getType() == Material.SNOW_BLOCK ||
                hitBlock.getType() == Material.SNOW ||
                hitBlock.getType() == Material.POWDER_SNOW) {
            hitBlock.setType(Material.AIR);

            // 10% chance to refund 4 snowballs when breaking snow via projectile (most common in Spleef).
            if (ThreadLocalRandom.current().nextDouble() < 0.10) {
                shooter.getInventory().addItem(new ItemStack(Material.SNOWBALL, 4));
                shooter.updateInventory();
            }
        }
    }
}
