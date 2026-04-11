package com.tommustbe12.craftmen.trim;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.Optional;

public record ArmorTrimSelection(String patternKey, String materialKey) {

    public static Optional<ArmorTrimSelection> of(TrimPattern pattern, TrimMaterial material) {
        if (pattern == null || material == null) return Optional.empty();
        NamespacedKey p = Registry.TRIM_PATTERN.getKey(pattern);
        NamespacedKey m = Registry.TRIM_MATERIAL.getKey(material);
        if (p == null || m == null) return Optional.empty();
        return Optional.of(new ArmorTrimSelection(p.toString(), m.toString()));
    }
}

