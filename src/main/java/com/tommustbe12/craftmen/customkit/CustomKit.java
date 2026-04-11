package com.tommustbe12.craftmen.customkit;

import com.tommustbe12.craftmen.kit.Kit;

import java.util.UUID;

public final class CustomKit {

    private final UUID id;
    private final String name;
    private final UUID creatorUuid;
    private final String creatorName;
    private final long createdAtMillis;
    private final long lastUsedAtMillis;
    private final long totalPlays;
    private final Kit kit;

    public CustomKit(
            UUID id,
            String name,
            UUID creatorUuid,
            String creatorName,
            long createdAtMillis,
            long lastUsedAtMillis,
            long totalPlays,
            Kit kit
    ) {
        this.id = id;
        this.name = name;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.createdAtMillis = createdAtMillis;
        this.lastUsedAtMillis = lastUsedAtMillis;
        this.totalPlays = totalPlays;
        this.kit = kit;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getLastUsedAtMillis() {
        return lastUsedAtMillis;
    }

    public long getTotalPlays() {
        return totalPlays;
    }

    public Kit getKit() {
        return kit;
    }

    public CustomKit withUsage(long usedAtMillis) {
        return new CustomKit(
                id,
                name,
                creatorUuid,
                creatorName,
                createdAtMillis,
                usedAtMillis,
                totalPlays + 1,
                kit
        );
    }
}

