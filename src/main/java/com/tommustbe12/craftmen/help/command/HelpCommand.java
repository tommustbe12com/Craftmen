package com.tommustbe12.craftmen.help.command;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HelpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /help bukkit -> forward to Bukkit help
        if (args.length >= 1 && args[0].equalsIgnoreCase("bukkit")) {
            String rest = args.length == 1 ? "" : String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            if (rest.isBlank()) Bukkit.dispatchCommand(sender, "bukkit:help");
            else Bukkit.dispatchCommand(sender, "bukkit:help " + rest);
            return true;
        }

        if (!(sender instanceof Player player)) {
            Bukkit.dispatchCommand(sender, "bukkit:help");
            return true;
        }

        Craftmen.get().getHelpMenu().openMain(player);
        return true;
    }
}

