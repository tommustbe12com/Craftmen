package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.match.MatchManager;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import com.tommustbe12.craftmen.profile.ProfileManager;
import com.tommustbe12.craftmen.queue.Queue;
import com.tommustbe12.craftmen.queue.QueueManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class QueueCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        var party = Craftmen.get().getPartyManager().getParty(player);
        if (party != null) {
            player.sendMessage("Â§cYou cannot queue normally while in a party.");
            player.sendMessage("Â§7Party leader: use the Party Activities item in your hotbar.");
            return true;
        }

        // is player in a match
        MatchManager matchManager = Craftmen.get().getMatchManager();
        Match match = matchManager.getMatch(player);

        if (match != null) {
            player.sendMessage("§cYou cannot use /queue while in a match!");
            return true;
        }

        ProfileManager profileManager = Craftmen.get().getProfileManager();
        PlayerState profileState = profileManager.getProfile(player).getState();

        if (profileState.toString().equals("QUEUED")) {
            player.sendMessage("§cYou are already in a queue!");
            return true;
        }

        Craftmen.get().getHubManager().openGameSelector(player, game -> {
            Craftmen.get().getHubManager().giveLeaveQueueItem(player);
            Craftmen.get().getQueueManager().addPlayer(player, game);
        });
        return true;
    }
}
