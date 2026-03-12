package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.impl.SpleefGame;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class BlockListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();

        // op bypass
        if (player.isOp()) return;

        if(Craftmen.get().getEndFightManager().isInGame(e.getPlayer())) return;

        PlayerState state = Craftmen.get()
                .getProfileManager()
                .getProfile(player)
                .getState();

        if (state != PlayerState.IN_MATCH) {
            e.setCancelled(true);
            player.sendMessage("§cYou can't break this here!");
            return;
        }

        Match match = Craftmen.get().getMatchManager().getMatch(player);

        if (match == null) return;
        if (!(match.getGame() instanceof SpleefGame)) return;

        Material type = e.getBlock().getType();

        if (type != Material.SNOW_BLOCK && type != Material.SNOW) return;

        e.setDropItems(false);

        // 25% chance to get a snowball
        if (ThreadLocalRandom.current().nextDouble() < 0.25) {
            player.getInventory().addItem(new ItemStack(Material.SNOWBALL));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();

        // op bypass
        if (player.isOp()) return;

        if(Craftmen.get().getEndFightManager().isInGame(e.getPlayer())) return;

        PlayerState state = Craftmen.get()
                .getProfileManager()
                .getProfile(player)
                .getState();

        if (state != PlayerState.IN_MATCH) {
            e.setCancelled(true);
            player.sendMessage("§cYou can't place this here!");
        }
    }
}