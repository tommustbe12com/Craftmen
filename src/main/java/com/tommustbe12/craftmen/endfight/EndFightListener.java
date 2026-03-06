package com.tommustbe12.craftmen.endfight;

import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

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

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
        Player p = e.getPlayer();
        if (!manager.isInGame(p)) return;

        if (p.getInventory().contains(Material.DRAGON_EGG)) {
            manager.win(p);
        } else {
            e.setCancelled(true);
            p.teleport(manager.getSpawn());
            p.sendMessage("§cYou must escape with the Dragon Egg!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (!manager.isInGame(p)) return;
        manager.removePlayer(p);

        // Drop egg if they had it
        if (p.getInventory().contains(Material.DRAGON_EGG)) {
            p.getWorld().dropItemNaturally(p.getLocation(),
                    p.getInventory().getItem(p.getInventory().first(Material.DRAGON_EGG)));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!manager.isInGame(p)) return;

        e.setRespawnLocation(manager.getSpawn());

        // Re-give kit after respawn
        manager.giveStartItems(p);
    }
}