package com.tommustbe12.craftmen.ffa;

import com.tommustbe12.craftmen.game.Game;
import org.bukkit.Location;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class FfaInstance {
    final UUID id;
    final Game game;
    final File schematicFile;
    final Location pasteOrigin;
    Location minCorner;
    Location maxCorner;
    long lastResetAtMillis;
    final int width;
    final int height;
    final int length;
    final Set<UUID> players = new HashSet<>();

    FfaInstance(UUID id, Game game, File schematicFile, Location pasteOrigin, int width, int height, int length) {
        this.id = id;
        this.game = game;
        this.schematicFile = schematicFile;
        this.pasteOrigin = pasteOrigin;
        this.width = width;
        this.height = height;
        this.length = length;
    }
}
