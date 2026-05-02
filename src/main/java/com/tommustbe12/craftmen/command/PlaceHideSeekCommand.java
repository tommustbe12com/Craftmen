package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlaceHideSeekCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        boolean ok = Craftmen.get().getHideSeekManager().placeArena(player);
        if (!ok) {
            player.sendMessage(ChatColor.RED + "Failed to place Hide & Seek arena (check console).");
        }
        return true;
    }
}

