package com.tommustbe12.craftmen.party.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PartyChatCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Party party = Craftmen.get().getPartyManager().getParty(player);
        if (party == null) {
            player.sendMessage(ChatColor.RED + "You are not in a party.");
            return true;
        }

        if (args.length == 0) {
            boolean enabled = Craftmen.get().getPartyChatManager().toggle(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Party chat: " + (enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
            return true;
        }

        String message = String.join(" ", args);
        String formatted = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "PC" + ChatColor.DARK_GRAY + "] "
                + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + ": "
                + ChatColor.WHITE + message;

        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(formatted);
        }

        return true;
    }
}

