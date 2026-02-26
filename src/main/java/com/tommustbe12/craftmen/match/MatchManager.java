package com.tommustbe12.craftmen.match;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MatchManager {

    private final List<Match> matches = new ArrayList<>();

    public void startMatch(Match match) {
        matches.add(match);
        match.start();
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