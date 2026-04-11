package com.tommustbe12.craftmen.badge;

import java.util.UUID;

public final class BadgeDefinition {
    private final UUID id;
    private final String name;
    private final String icon; // unicode emoji / character
    private final String requirement; // expression string
    private final String color; // &-codes allowed, e.g. "&b"

    public BadgeDefinition(UUID id, String name, String icon, String requirement, String color) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.requirement = requirement;
        this.color = color;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getRequirement() {
        return requirement;
    }

    public String getColor() {
        return color;
    }
}
