package com.tommustbe12.craftmen.party;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {

    private final UUID id;
    private UUID leader;
    private final Set<UUID> members = new HashSet<>();

    Party(UUID leader) {
        this.id = UUID.randomUUID();
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeader() {
        return leader;
    }

    void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    void addMember(UUID uuid) {
        members.add(uuid);
    }

    void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    int size() {
        return members.size();
    }
}

