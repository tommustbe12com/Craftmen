package com.tommustbe12.craftmen.party;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public final class PartyManager {

    private final Map<UUID, Party> partiesByMember = new HashMap<>(); // member -> party
    private final Map<UUID, Party> partiesById = new HashMap<>(); // partyId -> party
    private final Map<UUID, Invite> invitesByTarget = new HashMap<>(); // target -> invite

    public Party getParty(Player player) {
        if (player == null) return null;
        return partiesByMember.get(player.getUniqueId());
    }

    public Party getPartyById(UUID id) {
        return partiesById.get(id);
    }

    public boolean isInParty(Player player) {
        return getParty(player) != null;
    }

    public Party createParty(Player leader) {
        if (leader == null) return null;
        if (isInParty(leader)) return getParty(leader);

        Party party = new Party(leader.getUniqueId());
        partiesById.put(party.getId(), party);
        for (UUID member : party.getMembers()) {
            partiesByMember.put(member, party);
        }
        return party;
    }

    public void disbandParty(Party party) {
        if (party == null) return;
        broadcastParty(party, ChatColor.RED + "Your party was disbanded.", org.bukkit.Sound.ENTITY_WITHER_DEATH);
        partiesById.remove(party.getId());
        for (UUID uuid : new HashSet<>(party.getMembers())) {
            partiesByMember.remove(uuid);
        }
        invitesByTarget.values().removeIf(inv -> inv.partyId.equals(party.getId()));
    }

    public boolean invite(Player inviter, Player target) {
        Party party = getParty(inviter);
        if (party == null) party = createParty(inviter);
        if (party == null) return false;
        if (!party.getLeader().equals(inviter.getUniqueId())) return false;

        if (target == null) return false;
        if (isInParty(target)) return false;

        invitesByTarget.put(target.getUniqueId(), new Invite(party.getId(), inviter.getUniqueId(), System.currentTimeMillis()));
        return true;
    }

    public boolean accept(Player target) {
        if (target == null) return false;
        if (isInParty(target)) return false;

        Invite invite = invitesByTarget.remove(target.getUniqueId());
        if (invite == null) return false;

        Party party = partiesById.get(invite.partyId);
        if (party == null) return false;

        party.addMember(target.getUniqueId());
        partiesByMember.put(target.getUniqueId(), party);
        return true;
    }

    public boolean leave(Player player) {
        return leave(player, null, null);
    }

    public boolean leave(Player player, String customMessage, org.bukkit.Sound customSound) {
        Party party = getParty(player);
        if (party == null) return false;

        UUID uuid = player.getUniqueId();
        String msg = customMessage != null ? customMessage : (ChatColor.RED + player.getName() + " left the party.");
        org.bukkit.Sound snd = customSound != null ? customSound : org.bukkit.Sound.UI_BUTTON_CLICK;
        broadcastParty(party, msg, snd);
        party.removeMember(uuid);
        partiesByMember.remove(uuid);

        if (party.size() <= 0) {
            disbandParty(party);
            return true;
        }

        if (party.getLeader().equals(uuid)) {
            UUID newLeader = party.getMembers().iterator().next();
            party.setLeader(newLeader);
            Player nl = Bukkit.getPlayer(newLeader);
            if (nl != null) nl.sendMessage(ChatColor.YELLOW + "You are now the party leader.");
            broadcastParty(party, ChatColor.YELLOW + "New leader: " + (nl != null ? nl.getName() : newLeader.toString()), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        }

        if (party.size() == 1) {
            // auto-disband singletons
            UUID last = party.getMembers().iterator().next();
            partiesByMember.remove(last);
            partiesById.remove(party.getId());
        }

        return true;
    }

    public boolean kick(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null) return false;
        if (!party.getLeader().equals(leader.getUniqueId())) return false;
        if (target == null) return false;
        if (!party.isMember(target.getUniqueId())) return false;
        if (party.getLeader().equals(target.getUniqueId())) return false;

        broadcastParty(party, ChatColor.RED + target.getName() + " was kicked from the party.", org.bukkit.Sound.ENTITY_IRON_GOLEM_ATTACK);
        party.removeMember(target.getUniqueId());
        partiesByMember.remove(target.getUniqueId());
        return true;
    }

    public Invite peekInvite(Player target) {
        if (target == null) return null;
        return invitesByTarget.get(target.getUniqueId());
    }

    public record Invite(UUID partyId, UUID inviterId, long createdAtMillis) {}

    public void broadcastParty(Party party, String message, org.bukkit.Sound sound) {
        if (party == null) return;
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (message != null) p.sendMessage(message);
            if (sound != null) p.playSound(p.getLocation(), sound, 1.0f, 1.2f);
        }
    }
}
