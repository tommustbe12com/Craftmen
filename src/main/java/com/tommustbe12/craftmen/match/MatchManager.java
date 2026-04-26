package com.tommustbe12.craftmen.match;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.arena.Arena;
import com.tommustbe12.craftmen.command.SpectateCommand;
import com.tommustbe12.craftmen.game.Game;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MatchManager {

    private final List<Match> matches = new ArrayList<>();

    public void startMatch(Match match) {
        if (match == null) return;
        // Prevent stale duel requests from being accepted mid-match.
        Craftmen.get().getQueueManager().cancelDuelRequestsFor(match.getP1());
        Craftmen.get().getQueueManager().cancelDuelRequestsFor(match.getP2());

        matches.add(match);
        SpectateCommand.stopAllSpectatorsOfMatch(match);
        match.start();

        preparePlayer(match.getP1());
        preparePlayer(match.getP2());
    }

    public void abortMatch(Match match, String reason) {
        if (match == null) return;
        matches.remove(match);

        Player p1 = match.getP1();
        Player p2 = match.getP2();

        if (p1 != null && p1.isOnline()) {
            var prof = Craftmen.get().getProfileManager().getProfile(p1);
            if (prof != null) prof.setState(com.tommustbe12.craftmen.profile.PlayerState.LOBBY);
            com.tommustbe12.craftmen.player.PlayerReset.resetToHub(p1);
            if (reason != null && !reason.isBlank()) p1.sendMessage("§cMatch failed to start: " + reason);
        }
        if (p2 != null && p2.isOnline()) {
            var prof = Craftmen.get().getProfileManager().getProfile(p2);
            if (prof != null) prof.setState(com.tommustbe12.craftmen.profile.PlayerState.LOBBY);
            com.tommustbe12.craftmen.player.PlayerReset.resetToHub(p2);
            if (reason != null && !reason.isBlank()) p2.sendMessage("§cMatch failed to start: " + reason);
        }
    }

    public void startDuel(Player p1, Player p2, Game game) {
        // game name for category
        String category = game.getName();
        List<Arena> arenas = Craftmen.get().getArenaManager().getArenas(category);

        if (arenas.isEmpty()) {
            p1.sendMessage("§cNo arena available for this game!");
            p2.sendMessage("§cNo arena available for this game!");
            return;
        }

        // Pick a random arena in the category
        Arena arena = arenas.get(new Random().nextInt(arenas.size()));

        Match match = new Match(p1, p2, game, arena);
        startMatch(match);
    }

    public void endMatch(Match match, org.bukkit.entity.Player winner) {
        match.end(winner);
        matches.remove(match);
    }

    public Match getMatch(Player player) {
        for (Match match : matches) {
            if (match.getP1().equals(player) || match.getP2().equals(player)) {
                return match;
            }
        }
        return null;
    }

    public List<Match> getMatchesByGame(Game game) {
        List<Match> gameMatches = new ArrayList<>();
        for (Match match : matches) {
            if (match.getGame().equals(game)) {  // Check if match is associated with the game
                gameMatches.add(match);
            }
        }
        return gameMatches;
    }

    private void preparePlayer(Player player) {
        for (PotionEffectType type : PotionEffectType.values()) {
            if (type != null) player.removePotionEffect(type);
        }

        player.setHealth(20.0);
        player.setSaturation(20f);
        player.setFoodLevel(20);

        player.setFireTicks(0);
        player.setFallDistance(0);

        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setCollidable(true);

        player.setGameMode(GameMode.SURVIVAL);
    }

    public List<Match> getMatches() { return matches; }
}
