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
    private final Map<String, Long> duelRequestCooldowns = new HashMap<>(); // requester|target -> lastSentMillis
    private static final long DUEL_REQUEST_COOLDOWN_MILLIS = 15_000L;

    public boolean addDuelRequest(Player sender, Player target, Game game) {
        if (sender == null || target == null || game == null) return false;

        String key = sender.getUniqueId() + "|" + target.getUniqueId();
        long now = System.currentTimeMillis();
        long last = duelRequestCooldowns.getOrDefault(key, 0L);
        long remaining = (last + DUEL_REQUEST_COOLDOWN_MILLIS) - now;
        if (remaining > 0) {
            return false;
        }

        duelRequestCooldowns.put(key, now);
        duelRequests.put(target, new DuelRequest(sender, target, game, now));
        return true;
    }

    public void queueAgain(Player player) {
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null) return;
        addPlayer(player, Craftmen.get().getGameManager().getGame(profile.getLastPlayedGame()));
    }

    public DuelRequest getDuelRequest(Player target) {
        DuelRequest req = duelRequests.get(target);
        if (req == null) return null;
        if (isExpired(req)) {
            duelRequests.remove(target);
            return null;
        }
        return req;
    }

    public void removeDuelRequest(Player target) {
        duelRequests.remove(target);
    }

    public void cancelDuelRequestsFor(Player player) {
        if (player == null) return;
        duelRequests.entrySet().removeIf(e -> {
            DuelRequest req = e.getValue();
            if (req == null) return true;
            return e.getKey().equals(player) || req.requester.equals(player) || req.challenged.equals(player);
        });
    }

    public boolean isExpired(DuelRequest request) {
        if (request == null) return true;
        return (System.currentTimeMillis() - request.createdAtMillis) > DUEL_REQUEST_COOLDOWN_MILLIS;
    }

    public void addPlayer(Player player, Game game) {
        if (player == null || game == null) return;

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null || profile.getState() != PlayerState.LOBBY) return;

        // Parties can only queue for party-capable activities (Party FFA / Public FFA as a party / End Fight).
        // Disallow normal 1v1 queuing when the player is in a party.
        var party = Craftmen.get().getPartyManager().getParty(player);
        if (party != null) {
            player.sendMessage("§cYou cannot queue normally while in a party.");
            player.sendMessage("§7Have the party leader start Party FFA / Public FFA / End Fight.");
            return;
        }

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
        cancelDuelRequestsFor(player);

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
        private final long createdAtMillis;

        public DuelRequest(Player requester, Player challenged, Game game, long createdAtMillis) {
            this.requester = requester;
            this.challenged = challenged;
            this.game = game;
            this.createdAtMillis = createdAtMillis;
        }

        public Player getRequester() { return requester; }
        public Player getChallenged() { return challenged; }
        public Game getGame() { return game; }
        public long getCreatedAtMillis() { return createdAtMillis; }
    }
}
