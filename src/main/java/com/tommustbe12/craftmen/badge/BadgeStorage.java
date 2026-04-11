package com.tommustbe12.craftmen.badge;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class BadgeStorage {

    private final Craftmen plugin;
    private final File file;

    public BadgeStorage(Craftmen plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "badges.yml");
    }

    public List<BadgeDefinition> loadAll() {
        if (!file.exists()) return new ArrayList<>();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<BadgeDefinition> out = new ArrayList<>();

        if (!cfg.contains("badges")) return out;
        for (String key : Objects.requireNonNull(cfg.getConfigurationSection("badges")).getKeys(false)) {
            String base = "badges." + key;
            String name = cfg.getString(base + ".name");
            String icon = cfg.getString(base + ".icon");
            String req = cfg.getString(base + ".requirement");
            String color = cfg.getString(base + ".color", null);
            if (color == null) {
                // Backwards-compat: if older badges used "rank" to store the badge token/icon.
                color = cfg.getString(base + ".rank", "&7");
            }
            if (name == null || icon == null || req == null) continue;
            try {
                UUID id = UUID.fromString(key);
                out.add(new BadgeDefinition(id, name, icon, req, color == null ? "&7" : color));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    public void saveAll(Collection<BadgeDefinition> badges) {
        YamlConfiguration cfg = new YamlConfiguration();
        for (BadgeDefinition badge : badges) {
            String base = "badges." + badge.getId();
            cfg.set(base + ".name", badge.getName());
            cfg.set(base + ".icon", badge.getIcon());
            cfg.set(base + ".requirement", badge.getRequirement());
            cfg.set(base + ".color", badge.getColor());
        }
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save badges.yml: " + e.getMessage());
        }
    }
}
