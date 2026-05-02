package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.match.MatchManager;
import com.tommustbe12.craftmen.player.PlayerReset;
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

        if (Craftmen.get().getEndFightManager().isInGame(player)) {
            Craftmen.get().getEndFightManager().removePlayer(player);
        }

        if (Craftmen.get().getHideSeekManager().isInGame(player)) {
            Craftmen.get().getHideSeekManager().remove(player, true);
        }

        // Leaving FFA should always clear inventory/state first.
        if (Craftmen.get().getFfaManager().isInFfa(player)) {
            Craftmen.get().getFfaManager().leave(player, true);
            return true;
        }

        Location hubLocation = Craftmen.get().getHubLocation();

        if (hubLocation == null) {
            player.sendMessage("§cHub location is not set.");
            return false;
        }



        PlayerReset.resetToHub(player);
        return true;
    }
}
