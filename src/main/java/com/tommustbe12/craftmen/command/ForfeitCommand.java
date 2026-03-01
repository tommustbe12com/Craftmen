package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.match.MatchManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForfeitCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        MatchManager matchManager = Craftmen.get().getMatchManager();
        Match match = matchManager.getMatch(player);

        if (match == null) {
            player.sendMessage("§cYou are not in a match.");
            return true;
        }

        Player opponent;

        // get opponent
        if (match.getP1().equals(player)) {
            opponent = match.getP2();
        } else {
            opponent = match.getP1();
        }

        player.sendMessage("§cYou forfeited the match!");
        opponent.sendMessage("§a" + player.getName() + " has forfeited. You win!");

        // end match (opponent winner)
        Craftmen.get().getMatchManager().endMatch(match, opponent);

        return true;
    }
}