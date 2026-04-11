package com.tommustbe12.craftmen.queue;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.arena.Arena;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.player.PlayerReset;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.entity.Player;

import java.util.*;

public class QueueManager {

    private final Map<String, Queue> queues = new HashMap<>();
    private final Map<Player, DuelRequest> duelRequests = new HashMap<>();

    public void addDuelRequest(Player sender, Player target, Game game) {
        duelRequests.put(target, new DuelRequest(sender, target, game));
    }

    public void queueAgain(Player player) {
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null) return;
        addPlayer(player, Craftmen.get().getGameManager().getGame(profile.getLastPlayedGame()));
    }

    public DuelRequest getDuelRequest(Player target) {
        return duelRequests.get(target);
    }

    public void removeDuelRequest(Player target) {
        duelRequests.remove(target);
    }

    public void addPlayer(Player player, Game game) {
        if (player == null || game == null) return;

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null || profile.getState() != PlayerState.LOBBY) return;

        // Match end gives temporary flight/invulnerability; ensure it never leaks into the next queue.
        PlayerReset.clearTransientState(player);

        Queue queue = queues.computeIfAbsent(game.getName(), k -> new Queue(game));
        queue.addPlayer(player);
        profile.setState(PlayerState.QUEUED);
        player.sendMessage("§aYou joined the " + game.getName() + " queue!");

        checkQueue(queue);
    }

    private void checkQueue(Queue queue) {
        if (!queue.hasEnoughPlayers()) return;

        Player[] players = queue.pollPlayers();

        String category = queue.getGame().getName();
        List<Arena> arenas = Craftmen.get().getArenaManager().getArenas(category);

        if (arenas.isEmpty()) {
            for (Player p : players) {
                p.sendMessage("§cNo arena available for this game!");
            }
            return;
        }

        Arena arena = arenas.get(new Random().nextInt(arenas.size()));

        Match match = new Match(players[0], players[1], queue.getGame(), arena);
        Craftmen.get().getMatchManager().startMatch(match);
    }

    public void removePlayer(Player player) {
        queues.values().forEach(q -> q.removePlayer(player));
        duelRequests.remove(player);
        duelRequests.values().removeIf(v -> v == player);

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null) profile.setState(PlayerState.LOBBY);

        PlayerReset.resetToHub(player);
    }

    public List<Player> getPlayersInQueue(Game game) {
        List<Player> playersInQueue = new ArrayList<>();
        Queue queue = queues.get(game.getName());

        if (queue != null) {
            playersInQueue.addAll(queue.getPlayers());
        }

        return playersInQueue;
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
