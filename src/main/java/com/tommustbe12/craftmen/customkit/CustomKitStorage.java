package com.tommustbe12.craftmen.customkit;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.kit.Kit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class CustomKitStorage {

    private static final int SCHEMA_VERSION = 1;

    private final Craftmen plugin;
    private final File dir;

    public CustomKitStorage(Craftmen plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "player-kits");
        if (!dir.exists()) dir.mkdirs();
    }

    public Collection<CustomKit> loadAll() {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return List.of();

        List<CustomKit> out = new ArrayList<>();
        for (File f : files) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                CustomKit kit = read(cfg);
                if (kit != null) out.add(kit);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player kit " + f.getName() + ": " + e.getMessage());
            }
        }
        return out;
    }

    public void save(CustomKit kit) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("schemaVersion", SCHEMA_VERSION);
        cfg.set("id", kit.getId().toString());
        cfg.set("name", kit.getName());
        cfg.set("creator.uuid", kit.getCreatorUuid().toString());
        cfg.set("creator.name", kit.getCreatorName());
        cfg.set("createdAtMillis", kit.getCreatedAtMillis());
        cfg.set("lastUsedAtMillis", kit.getLastUsedAtMillis());
        cfg.set("totalPlays", kit.getTotalPlays());

        cfg.set("kit.contents", toList(kit.getKit().getContents()));
        cfg.set("kit.armor", toList(kit.getKit().getArmor()));
        cfg.set("kit.offhand", kit.getKit().getOffhand());

        try {
            cfg.save(file(kit.getId()));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player kit " + kit.getId() + ": " + e.getMessage());
        }
    }

    public void delete(UUID id) {
        File f = file(id);
        if (f.exists() && !f.delete()) {
            plugin.getLogger().warning("Failed to delete player kit file " + f.getName());
        }
    }

    private File file(UUID id) {
        return new File(dir, id + ".yml");
    }

    private static CustomKit read(YamlConfiguration cfg) {
        String idRaw = cfg.getString("id");
        String name = cfg.getString("name");
        String creatorUuidRaw = cfg.getString("creator.uuid");
        String creatorName = cfg.getString("creator.name");
        if (idRaw == null || name == null || creatorUuidRaw == null || creatorName == null) return null;

        UUID id = UUID.fromString(idRaw);
        UUID creatorUuid = UUID.fromString(creatorUuidRaw);

        long createdAt = cfg.getLong("createdAtMillis", System.currentTimeMillis());
        long lastUsedAt = cfg.getLong("lastUsedAtMillis", createdAt);
        long totalPlays = cfg.getLong("totalPlays", 0L);

        ItemStack[] contents = readItemArray(cfg, "kit.contents", 36);
        ItemStack[] armor = readItemArray(cfg, "kit.armor", 4);
        ItemStack offhand = cfg.getItemStack("kit.offhand");
        Kit kit = new Kit(contents, armor, offhand);

        return new CustomKit(id, name, creatorUuid, creatorName, createdAt, lastUsedAt, totalPlays, kit);
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

