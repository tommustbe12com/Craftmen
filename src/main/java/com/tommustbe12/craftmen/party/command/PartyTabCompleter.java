package com.tommustbe12.craftmen.party.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class PartyTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("create", "invite", "accept", "leave", "kick", "disband", "ffa", "endffa"));
            return filter(subs, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            return filter(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("kick")) {
            Party party = Craftmen.get().getPartyManager().getParty(player);
            if (party == null) return Collections.emptyList();
            return filter(party.getMembers().stream()
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null)
                    .map(Player::getName)
                    .collect(Collectors.toList()), args[1]);
        }

        return Collections.emptyList();
    }

    private static List<String> filter(List<String> candidates, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase();
        return candidates.stream()
                .filter(s -> s.toLowerCase().startsWith(p))
                .sorted()
                .toList();
    }
}

