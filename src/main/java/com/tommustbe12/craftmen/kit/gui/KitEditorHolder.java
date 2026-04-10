package com.tommustbe12.craftmen.kit.gui;

import com.tommustbe12.craftmen.game.Game;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class KitEditorHolder implements InventoryHolder {

    public enum Type { SELECT, EDIT }

    private final Type type;
    private final UUID owner;
    private final Game game;

    public KitEditorHolder(Type type, UUID owner, Game game) {
        this.type = type;
        this.owner = owner;
        this.game = game;
    }

    public Type getType() {
        return type;
    }

    public UUID getOwner() {
        return owner;
    }

    public Game getGame() {
        return game;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

