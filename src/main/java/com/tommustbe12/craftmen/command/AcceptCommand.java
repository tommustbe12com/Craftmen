package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.MatchManager;
import com.tommustbe12.craftmen.queue.QueueManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AcceptCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can accept duels!");
            return true;
        }

        if (args.length != 1) {
            p.sendMessage("§cUsage: /accept <player>");
            return true;
        }

        Player requester = Bukkit.getPlayer(args[0]);
        if (requester == null || !requester.isOnline()) {
            p.sendMessage("§cPlayer not found or offline!");
            return true;
        }

        QueueManager queue = Craftmen.get().getQueueManager();
        QueueManager.DuelRequest duelRequest = queue.getDuelRequest(p);
        if (duelRequest == null || duelRequest.getRequester() != requester) {
            p.sendMessage("§cNo duel request from this player!");
            return true;
        }

        // Remove the request
        queue.removeDuelRequest(p);

        // Start a match with the selected game
        Craftmen.get().getMatchManager().startDuel(duelRequest.getRequester(), p, duelRequest.getGame());

        return true;
    }
}