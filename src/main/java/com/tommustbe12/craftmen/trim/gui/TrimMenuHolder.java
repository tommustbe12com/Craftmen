package com.tommustbe12.craftmen.trim.gui;

import com.tommustbe12.craftmen.trim.ArmorSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class TrimMenuHolder implements InventoryHolder {

    public enum Type { MAIN, PATTERN, MATERIAL }

    private final Type type;
    private final UUID owner;
    private final ArmorSlot slot;
    private final String patternKey;

    public TrimMenuHolder(Type type, UUID owner, ArmorSlot slot, String patternKey) {
        this.type = type;
        this.owner = owner;
        this.slot = slot;
        this.patternKey = patternKey;
    }

    public Type getType() { return type; }
    public UUID getOwner() { return owner; }
    public ArmorSlot getSlot() { return slot; }
    public String getPatternKey() { return patternKey; }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

