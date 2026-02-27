package com.tommustbe12.craftmen.queue;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.match.MatchManager;
import com.tommustbe12.craftmen.arena.Arena;
import com.tommustbe12.craftmen.arena.ArenaManager;
import com.tommustbe12.craftmen.profile.Profile;
import com.tommustbe12.craftmen.profile.ProfileManager;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class QueueManager {

    private final Map<String, Queue> queues = new HashMap<>();
    private final Map<Player, DuelRequest> duelRequests = new HashMap<>();

    public void addDuelRequest(Player sender, Player target, Game game) {
        duelRequests.put(target, new DuelRequest(sender, target, game));
    }

    public DuelRequest getDuelRequest(Player target) {
        return duelRequests.get(target);
    }

    public void removeDuelRequest(Player target) {
        duelRequests.remove(target);
    }

    public void addPlayer(Player player, Game game) {
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile.getState() != PlayerState.LOBBY) return;

        Queue queue = queues.computeIfAbsent(game.getName(), k -> new Queue(game));
        queue.addPlayer(player);
        profile.setState(PlayerState.QUEUED);
        player.sendMessage("§aYou joined the " + game.getName() + " queue!");

        checkQueue(queue);
    }

    private void checkQueue(Queue queue) {
        if (!queue.hasEnoughPlayers()) return;

        Player[] players = queue.pollPlayers();
        Arena arena = Craftmen.get().getArenaManager().getArenas().get(0); // pick first arena
        Match match = new Match(players[0], players[1], queue.getGame(), arena);
        Craftmen.get().getMatchManager().startMatch(match);
    }

    public void removePlayer(Player player) {
        queues.values().forEach(q -> q.removePlayer(player));
        duelRequests.remove(player);
        duelRequests.values().removeIf(v -> v == player);
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        profile.setState(PlayerState.LOBBY);
    }

    public static class DuelRequest {
        private final Player requester;
        private final Player challenged;
        private final Game game;

        public DuelRequest(Player requester, Player challenged, Game game) {
            this.requester = requester;
            this.challenged = challenged;
            this.game = game;
        }

        public Player getRequester() { return requester; }
        public Player getChallenged() { return challenged; }
        public Game getGame() { return game; }
    }
}