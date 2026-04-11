package com.tommustbe12.craftmen.customkit.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class CustomKitHolder implements InventoryHolder {

    public enum Type {
        BROWSER,
        ITEM_PICKER,
        ENCHANT
    }

    private final Type type;
    private final UUID player;
    private final int page;

    public CustomKitHolder(Type type, UUID player, int page) {
        this.type = type;
        this.player = player;
        this.page = page;
    }

    public Type getType() {
        return type;
    }

    public UUID getPlayer() {
        return player;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

