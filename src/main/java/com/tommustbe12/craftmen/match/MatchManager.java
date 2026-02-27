package com.tommustbe12.craftmen.match;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.arena.Arena;
import com.tommustbe12.craftmen.game.Game;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MatchManager {

    private final List<Match> matches = new ArrayList<>();

    public void startMatch(Match match) {
        matches.add(match);
        match.start();
    }

    public void startDuel(Player p1, Player p2, Game game) {
        // 1st
        Arena arena = Craftmen.get().getArenaManager().getArenas().get(0);

        if (arena == null) {
            p1.sendMessage("§cNo arena available!");
            p2.sendMessage("§cNo arena available!");
            return;
        }

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

    public List<Match> getMatches() { return matches; }
}