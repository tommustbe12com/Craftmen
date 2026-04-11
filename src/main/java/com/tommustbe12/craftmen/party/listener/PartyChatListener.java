package com.tommustbe12.craftmen.party.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public final class PartyChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player sender = e.getPlayer();
        if (!Craftmen.get().getPartyChatManager().isEnabled(sender.getUniqueId())) return;

        Party party = Craftmen.get().getPartyManager().getParty(sender);
        if (party == null) return;

        e.setCancelled(true);

        String msg = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "PC" + ChatColor.DARK_GRAY + "] "
                + ChatColor.YELLOW + sender.getName() + ChatColor.GRAY + ": "
                + ChatColor.WHITE + e.getMessage();

        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }
}

