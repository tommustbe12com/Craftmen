package com.tommustbe12.craftmen.trim;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ArmorTrimStorage {

    private final Craftmen plugin;
    private final File trimDir;
    private final Map<UUID, YamlConfiguration> cache = new HashMap<>();

    public ArmorTrimStorage(Craftmen plugin) {
        this.plugin = plugin;
        // Reuse the same folder as kits to keep player customization together.
        this.trimDir = new File(plugin.getDataFolder(), "kits");
        if (!trimDir.exists()) trimDir.mkdirs();
    }

    public Map<ArmorSlot, ArmorTrimSelection> load(UUID uuid) {
        YamlConfiguration cfg = getConfig(uuid);
        Map<ArmorSlot, ArmorTrimSelection> out = new HashMap<>();
        for (ArmorSlot slot : ArmorSlot.values()) {
            String base = "trims." + slot.name().toLowerCase();
            String pattern = cfg.getString(base + ".pattern");
            String material = cfg.getString(base + ".material");
            if (pattern == null || material == null) continue;
            out.put(slot, new ArmorTrimSelection(pattern, material));
        }
        return out;
    }

    public void save(UUID uuid, ArmorSlot slot, ArmorTrimSelection selection) {
        YamlConfiguration cfg = getConfig(uuid);
        String base = "trims." + slot.name().toLowerCase();
        if (selection == null) {
            cfg.set(base, null);
        } else {
            cfg.set(base + ".pattern", selection.patternKey());
            cfg.set(base + ".material", selection.materialKey());
        }
        saveNow(uuid, cfg);
    }

    public void flushAll() {
        for (Map.Entry<UUID, YamlConfiguration> entry : new HashMap<>(cache).entrySet()) {
            saveNow(entry.getKey(), entry.getValue());
        }
    }

    private YamlConfiguration getConfig(UUID uuid) {
        YamlConfiguration cfg = cache.get(uuid);
        if (cfg != null) return cfg;
        File f = file(uuid);
        cfg = YamlConfiguration.loadConfiguration(f);
        cache.put(uuid, cfg);
        return cfg;
    }

    private void saveNow(UUID uuid, YamlConfiguration cfg) {
        try {
            cfg.save(file(uuid));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save trim file for " + uuid + ": " + e.getMessage());
        }
    }

    private File file(UUID uuid) {
        return new File(trimDir, uuid + ".yml");
    }
}

