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
import org.bukkit.util.Vector;

public class SnowballHitListener implements Listener {

    @EventHandler
    public void onSnowballHit(ProjectileHitEvent e) {
        Projectile projectile = e.getEntity();

        if (!(projectile instanceof Snowball)) return;

        if (!(projectile.getShooter() instanceof Player shooter)) return;

        if (e.getHitEntity() instanceof Player target) {
            Match match = Craftmen.get().getMatchManager().getMatch(target);
            if (match == null || !(match.getGame() instanceof SpleefGame)) return;

            target.setNoDamageTicks(1);

            Vector direction = target.getLocation().toVector().subtract(shooter.getLocation().toVector()).normalize();
            direction.setY(0.4); // vertical push
            target.setVelocity(direction.multiply(1.2));
        }

        Block hitBlock = e.getHitBlock();
        if (hitBlock == null) return;

        Match match = Craftmen.get().getMatchManager().getMatch(shooter);
        if (match == null || !(match.getGame() instanceof SpleefGame)) return;

        if (hitBlock.getType() == Material.SNOW_BLOCK ||
                hitBlock.getType() == Material.SNOW ||
                hitBlock.getType() == Material.POWDER_SNOW) {
            hitBlock.setType(Material.AIR);
        }
    }
}