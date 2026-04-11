package com.tommustbe12.craftmen.party.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.party.Party;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PartyPlayerListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        Party party = Craftmen.get().getPartyManager().getParty(player);
        if (party == null) return;

        Craftmen.get().getPartyChatManager().disable(player.getUniqueId());
        Craftmen.get().getPartyManager().leave(
                player,
                ChatColor.RED + player.getName() + " left the party (disconnected).",
                Sound.ENTITY_ENDERMAN_TELEPORT
        );
    }
}
