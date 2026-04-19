package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
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
        if (queue.isExpired(duelRequest)) {
            queue.removeDuelRequest(p);
            p.sendMessage("§cThat duel request expired.");
            return true;
        }

        // Reject duel accepts if either player is already in a match/queue (prevents dual-match glitches).
        Profile pProfile = Craftmen.get().getProfileManager().getProfile(p);
        Profile rProfile = Craftmen.get().getProfileManager().getProfile(requester);
        boolean pBusy = pProfile == null
                || pProfile.getState() != PlayerState.LOBBY
                || Craftmen.get().getMatchManager().getMatch(p) != null;
        boolean rBusy = rProfile == null
                || rProfile.getState() != PlayerState.LOBBY
                || Craftmen.get().getMatchManager().getMatch(requester) != null;
        if (pBusy || rBusy) {
            queue.removeDuelRequest(p);
            if (pBusy) p.sendMessage("§cYou can't accept a duel while in a queue/match.");
            else p.sendMessage("§cThat player is busy (queue/match).");
            return true;
        }

        // Remove the request and start a duel match.
        queue.removeDuelRequest(p);
        Craftmen.get().getMatchManager().startDuel(duelRequest.getRequester(), p, duelRequest.getGame());

        return true;
    }
}

