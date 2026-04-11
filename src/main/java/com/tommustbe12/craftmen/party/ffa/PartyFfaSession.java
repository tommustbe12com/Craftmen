package com.tommustbe12.craftmen.party.ffa;

import com.tommustbe12.craftmen.game.Game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PartyFfaSession {

    public final UUID partyId;
    public final Game game;
    public final int totalRounds;

    public int currentRound = 0;
    public final Map<UUID, Integer> roundWins = new HashMap<>();

    public final Set<UUID> alive = new HashSet<>();
    public final Set<UUID> spectators = new HashSet<>();

    public boolean running = false;

    public PartyFfaSession(UUID partyId, Game game, int totalRounds) {
        this.partyId = partyId;
        this.game = game;
        this.totalRounds = Math.max(1, Math.min(10, totalRounds));
    }
}

