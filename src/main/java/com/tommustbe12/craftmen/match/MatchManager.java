package com.tommustbe12.craftmen.match;

import java.util.ArrayList;
import java.util.List;

public class MatchManager {

    private final List<Match> matches = new ArrayList<>();

    public void startMatch(Match match) {
        matches.add(match);
        match.start();
    }

    public void endMatch(Match match, org.bukkit.entity.Player winner) {
        matches.remove(match);
        match.end(winner);
    }

    public List<Match> getMatches() { return matches; }
}