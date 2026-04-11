package com.tommustbe12.craftmen.badge.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class BadgeHolder implements InventoryHolder {

    public enum Type { PLAYER, ADMIN }

    private final Type type;
    private final UUID player;

    public BadgeHolder(Type type, UUID player) {
        this.type = type;
        this.player = player;
    }

    public Type getType() {
        return type;
    }

    public UUID getPlayer() {
        return player;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

