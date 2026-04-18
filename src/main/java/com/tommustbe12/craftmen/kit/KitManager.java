package com.tommustbe12.craftmen.kit;

import com.tommustbe12.craftmen.game.Game;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class KitManager {

    private final KitStorage storage;

    public KitManager(KitStorage storage) {
        this.storage = storage;
    }

    public String gameKey(Game game) {
        return game.getName()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "_");
    }

    public Optional<Kit> getCustomKit(UUID uuid, Game game) {
        return storage.loadCustomKit(uuid, gameKey(game));
    }

    public Kit getEffectiveKit(Player player, Game game) {
        if (!game.allowCustomKits()) return game.createDefaultKit();
        return getCustomKit(player.getUniqueId(), game).orElseGet(game::createDefaultKit);
    }

    public void setCustomKit(UUID uuid, Game game, Kit kit) {
        storage.saveCustomKit(uuid, gameKey(game), kit);
    }

    public void clearCustomKit(UUID uuid, Game game) {
        storage.clearCustomKit(uuid, gameKey(game));
    }

    public void flushAll() {
        storage.flushAll();
    }
}
