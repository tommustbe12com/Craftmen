package com.tommustbe12.craftmen.endfight;

import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class EndFightListener implements Listener {

    private final EndFightManager manager;

    public EndFightListener(EndFightManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof EnderDragon)) return;
        manager.dragonKilled();
    }

    // Advance the kit tier when they die
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!manager.isInGame(p)) return;
        manager.onPlayerDeath(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!manager.isInGame(p)) return;

        e.setRespawnLocation(manager.getSpawn());

        manager.onPlayerRespawn(p);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        if (!manager.isInGame(p)) return;

        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        org.bukkit.Location loc = p.getLocation();

        if (loc.getWorld().getEnvironment() != org.bukkit.World.Environment.THE_END) return;

        int px = loc.getBlockX();
        int pz = loc.getBlockZ();

        // All XZ coords of the end portal exit area
        int[][] portalCoords = {
                {1,2},{0,2},{-1,2},{-2,1},{-2,0},{-2,-1},
                {-1,-2},{0,-2},{1,-2},{2,-1},{2,0},{2,1},
                {1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1},{1,0}
        };

        boolean inPortalArea = false;
        for (int[] coord : portalCoords) {
            if (px == coord[0] && pz == coord[1]) {
                inPortalArea = true;
                break;
            }
        }

        if (!inPortalArea) return;

        // Get highest Y at 0,0 (center of portal) as the reference
        int portalY = loc.getWorld().getHighestBlockYAt(0, 0);

        if (loc.getY() < portalY || loc.getY() > portalY + 3) return;

        if (p.getInventory().contains(Material.DRAGON_EGG)) {
            manager.win(p);
        } else {
            p.teleport(manager.getSpawn());
            p.sendMessage("§cYou must escape with the Dragon Egg!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (!manager.isInGame(p)) return;

        // Drop egg if they had it
        if (p.getInventory().contains(Material.DRAGON_EGG)) {
            p.getWorld().dropItemNaturally(p.getLocation(),
                    p.getInventory().getItem(p.getInventory().first(Material.DRAGON_EGG)));
        }

        manager.removePlayer(p);
    }
}