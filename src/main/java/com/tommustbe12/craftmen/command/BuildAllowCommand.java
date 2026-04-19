package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BuildAllowCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.isOp() && !player.hasPermission("craftmen.admin")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /buildallow <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or offline.");
            return true;
        }

        var mgr = Craftmen.get().getBuildAllowManager();
        mgr.setTempAllowed(target, true);
        player.sendMessage(ChatColor.GREEN + "Temporarily allowed " + ChatColor.YELLOW + target.getName()
                + ChatColor.GREEN + " to build in spawn (until they rejoin).");
        return true;
    }
}
