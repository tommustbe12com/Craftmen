package com.tommustbe12.craftmen.party;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PartyChatManager {

    private final Set<UUID> enabled = new HashSet<>();

    public boolean isEnabled(UUID playerId) {
        return enabled.contains(playerId);
    }

    public boolean toggle(UUID playerId) {
        if (enabled.contains(playerId)) {
            enabled.remove(playerId);
            return false;
        }
        enabled.add(playerId);
        return true;
    }

    public void disable(UUID playerId) {
        enabled.remove(playerId);
    }
}

