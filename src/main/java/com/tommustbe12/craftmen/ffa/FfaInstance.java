package com.tommustbe12.craftmen.ffa;

import com.tommustbe12.craftmen.game.Game;
import org.bukkit.Location;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class FfaInstance {
    final Game game;
    final File schematicFile;
    final Location pasteOrigin;
    Location minCorner;
    Location maxCorner;
    long lastResetAtMillis;
    final Set<UUID> players = new HashSet<>();

    FfaInstance(Game game, File schematicFile, Location pasteOrigin) {
        this.game = game;
        this.schematicFile = schematicFile;
        this.pasteOrigin = pasteOrigin;
    }
}

