package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.match.MatchManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class HubCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        // is player in a match
        MatchManager matchManager = Craftmen.get().getMatchManager();
        Match match = matchManager.getMatch(player);

        if (match != null) {
            player.sendMessage("§cYou cannot use /hub while in a match!");
            return true;
        }

        Location hubLocation = Craftmen.get().getHubLocation();

        if (hubLocation == null) {
            player.sendMessage("§cHub location is not set.");
            return false;
        }

        player.teleport(hubLocation);
        return true;
    }
}