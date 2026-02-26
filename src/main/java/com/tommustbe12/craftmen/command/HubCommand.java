package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class HubCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // player send
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        Location hubLocation = Craftmen.get().getHubLocation(); // grab

        if (hubLocation == null) {
            player.sendMessage("§cHub location is not set.");
            return false;
        }

        player.teleport(hubLocation);

        return true;  // /hub command
    }
}