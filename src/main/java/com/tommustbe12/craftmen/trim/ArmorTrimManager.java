package com.tommustbe12.craftmen.trim;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.Map;
import java.util.UUID;

public final class ArmorTrimManager {

    private final ArmorTrimStorage storage;

    private final Map<UUID, Map<ArmorSlot, ArmorTrimSelection>> cache = new java.util.HashMap<>();

    public ArmorTrimManager(ArmorTrimStorage storage) {
        this.storage = storage;
    }

    public Map<ArmorSlot, ArmorTrimSelection> getSelections(UUID uuid) {
        return cache.computeIfAbsent(uuid, storage::load);
    }

    public void setSelection(UUID uuid, ArmorSlot slot, ArmorTrimSelection selection) {
        getSelections(uuid).put(slot, selection);
        storage.save(uuid, slot, selection);
    }

    public void clearSelection(UUID uuid, ArmorSlot slot) {
        getSelections(uuid).remove(slot);
        storage.save(uuid, slot, null);
    }

    public void apply(Player player) {
        if (player == null) return;
        apply(player.getUniqueId(), player.getInventory());
    }

    public void apply(UUID uuid, PlayerInventory inv) {
        if (inv == null) return;
        Map<ArmorSlot, ArmorTrimSelection> selections = getSelections(uuid);
        applyOne(inv.getHelmet(), selections.get(ArmorSlot.HELMET));
        applyOne(inv.getChestplate(), selections.get(ArmorSlot.CHESTPLATE));
        applyOne(inv.getLeggings(), selections.get(ArmorSlot.LEGGINGS));
        applyOne(inv.getBoots(), selections.get(ArmorSlot.BOOTS));
    }

    public void flushAll() {
        storage.flushAll();
    }

    private static void applyOne(ItemStack item, ArmorTrimSelection selection) {
        if (item == null) return;
        if (selection == null) return;
        if (!(item.getItemMeta() instanceof ArmorMeta meta)) return;

        TrimPattern pattern = resolvePattern(selection.patternKey());
        TrimMaterial material = resolveMaterial(selection.materialKey());
        if (pattern == null || material == null) return;

        meta.setTrim(new ArmorTrim(material, pattern));
        item.setItemMeta(meta);
    }

    private static TrimPattern resolvePattern(String key) {
        if (key == null) return null;
        NamespacedKey ns = NamespacedKey.fromString(key);
        if (ns == null) return null;
        return Registry.TRIM_PATTERN.get(ns);
    }

    private static TrimMaterial resolveMaterial(String key) {
        if (key == null) return null;
        NamespacedKey ns = NamespacedKey.fromString(key);
        if (ns == null) return null;
        return Registry.TRIM_MATERIAL.get(ns);
    }
}
