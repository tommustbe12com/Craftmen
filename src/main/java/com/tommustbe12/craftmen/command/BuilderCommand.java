package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BuilderCommand implements CommandExecutor {

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
        if (args.length != 2 || !(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            player.sendMessage(ChatColor.RED + "Usage: /builder add|remove <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        boolean add = args[0].equalsIgnoreCase("add");
        Craftmen.get().getBuildAllowManager().setPermanentAllowed(target, add);
        player.sendMessage((add ? ChatColor.GREEN + "Added " : ChatColor.YELLOW + "Removed ")
                + ChatColor.AQUA + target.getName()
                + ChatColor.GRAY + (add ? " as a permanent builder." : " from permanent builders."));
        return true;
    }
}

