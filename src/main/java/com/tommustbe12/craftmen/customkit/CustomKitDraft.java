package com.tommustbe12.craftmen.customkit;

import com.tommustbe12.craftmen.kit.Kit;

import java.util.UUID;

final class CustomKitDraft {
    final UUID id;
    final UUID creatorUuid;
    final String creatorName;
    final String name;
    final Kit restoreInventory;

    CustomKitDraft(UUID id, UUID creatorUuid, String creatorName, String name, Kit restoreInventory) {
        this.id = id;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.name = name;
        this.restoreInventory = restoreInventory;
    }
}

