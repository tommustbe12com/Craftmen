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
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        profile.setState(PlayerState.LOBBY);
    }
}