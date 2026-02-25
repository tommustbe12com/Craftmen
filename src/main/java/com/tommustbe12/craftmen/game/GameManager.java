package com.tommustbe12.craftmen.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GameManager {

    private final Map<String, Game> games = new HashMap<>();

    public void registerGame(Game game) {
        games.put(game.getName(), game);
    }

    public Game getGame(String name) {
        return games.get(name);
    }

    public Collection<Game> getGames() {
        return games.values();
    }
}