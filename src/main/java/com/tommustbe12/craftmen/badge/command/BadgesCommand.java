package com.tommustbe12.craftmen.badge.command;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BadgesCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Craftmen.get().getBadgeManager().openPlayer(player);
        return true;
    }
}

