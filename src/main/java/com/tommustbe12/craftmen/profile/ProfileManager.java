package com.tommustbe12.craftmen.profile;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileManager {

    private final Map<UUID, Profile> profiles = new HashMap<>();

    public Profile getProfile(Player player) {
        return profiles.computeIfAbsent(player.getUniqueId(), uuid -> new Profile(player));
    }

    public void removeProfile(Player player) {
        profiles.remove(player.getUniqueId());
    }
}