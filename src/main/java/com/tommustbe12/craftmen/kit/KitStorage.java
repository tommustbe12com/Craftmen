package com.tommustbe12.craftmen.kit;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class KitStorage {

    private static final int SCHEMA_VERSION = 1;

    private final Craftmen plugin;
    private final File kitDir;

    private final Map<UUID, YamlConfiguration> cache = new HashMap<>();

    public KitStorage(Craftmen plugin) {
        this.plugin = plugin;
        this.kitDir = new File(plugin.getDataFolder(), "kits");
        if (!kitDir.exists()) kitDir.mkdirs();
    }

    public Optional<Kit> loadCustomKit(UUID uuid, String gameKey) {
        YamlConfiguration cfg = getConfig(uuid);
        String base = "games." + gameKey;
        if (!cfg.contains(base + ".contents") && !cfg.contains(base + ".armor") && !cfg.contains(base + ".offhand")) {
            return Optional.empty();
        }

        ItemStack[] contents = readItemArray(cfg, base + ".contents", 36);
        ItemStack[] armor = readItemArray(cfg, base + ".armor", 4);
        ItemStack offhand = cfg.getItemStack(base + ".offhand");
        return Optional.of(new Kit(contents, armor, offhand));
    }

    public void saveCustomKit(UUID uuid, String gameKey, Kit kit) {
        YamlConfiguration cfg = getConfig(uuid);
        cfg.set("schemaVersion", SCHEMA_VERSION);

        String base = "games." + gameKey;
        cfg.set(base + ".contents", toList(kit.getContents()));
        cfg.set(base + ".armor", toList(kit.getArmor()));
        cfg.set(base + ".offhand", kit.getOffhand());
        saveNow(uuid, cfg);
    }

    public void clearCustomKit(UUID uuid, String gameKey) {
        YamlConfiguration cfg = getConfig(uuid);
        cfg.set("games." + gameKey, null);
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
            plugin.getLogger().warning("Failed to save kit file for " + uuid + ": " + e.getMessage());
        }
    }

    private File file(UUID uuid) {
        return new File(kitDir, uuid + ".yml");
    }

    private static List<ItemStack> toList(ItemStack[] arr) {
        List<ItemStack> list = new ArrayList<>(arr.length);
        for (ItemStack item : arr) list.add(item == null ? null : item.clone());
        return list;
    }

    @SuppressWarnings("unchecked")
    private static ItemStack[] readItemArray(YamlConfiguration cfg, String path, int size) {
        List<?> raw = cfg.getList(path);
        ItemStack[] out = new ItemStack[size];
        if (raw == null) return out;
        for (int i = 0; i < Math.min(raw.size(), size); i++) {
            Object o = raw.get(i);
            if (o instanceof ItemStack stack) out[i] = stack;
        }
        return out;
    }
}

