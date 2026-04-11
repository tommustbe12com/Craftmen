package com.tommustbe12.craftmen.party.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.party.Party;
import com.tommustbe12.craftmen.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PartyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        PartyManager parties = Craftmen.get().getPartyManager();
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/party create|invite <player>|accept|leave|kick <player>|disband|ffa <game>");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> {
                Party party = parties.createParty(player);
                player.sendMessage(ChatColor.GREEN + "Party created. ID: " + party.getId());
            }
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /party invite <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                Party party = parties.getParty(player);
                if (party == null) party = parties.createParty(player);
                if (!party.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can invite.");
                    return true;
                }
                if (party.getMembers().size() >= 10) {
                    player.sendMessage(ChatColor.RED + "Party is full (10).");
                    return true;
                }
                if (parties.isInParty(target)) {
                    player.sendMessage(ChatColor.RED + "That player is already in a party.");
                    return true;
                }

                parties.invite(player, target);
                player.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + " to your party.");
                target.sendMessage(ChatColor.YELLOW + player.getName() + " invited you to a party. Type /party accept");
            }
            case "accept" -> {
                if (parties.accept(player)) {
                    Party party = parties.getParty(player);
                    player.sendMessage(ChatColor.GREEN + "Joined the party.");
                    if (party != null) {
                        Player leader = Bukkit.getPlayer(party.getLeader());
                        if (leader != null) leader.sendMessage(ChatColor.GREEN + player.getName() + " joined your party.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You have no pending party invites.");
                }
            }
            case "leave" -> {
                if (parties.leave(player)) {
                    player.sendMessage(ChatColor.RED + "You left the party.");
                } else {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                }
            }
            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /party kick <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                if (parties.kick(player, target)) {
                    player.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + " from the party.");
                    target.sendMessage(ChatColor.RED + "You were kicked from the party.");
                } else {
                    player.sendMessage(ChatColor.RED + "Could not kick that player.");
                }
            }
            case "disband" -> {
                Party party = parties.getParty(player);
                if (party == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    return true;
                }
                if (!party.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can disband.");
                    return true;
                }
                parties.disbandParty(party);
                player.sendMessage(ChatColor.RED + "Party disbanded.");
            }
            case "ffa" -> {
                Party party = parties.getParty(player);
                if (party == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    return true;
                }
                if (!party.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can start a private FFA.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /party ffa <game>");
                    return true;
                }
                Game game = Craftmen.get().getGameManager().getGame(args[1]);
                if (game == null) {
                    player.sendMessage(ChatColor.RED + "Unknown game: " + args[1]);
                    return true;
                }
                Craftmen.get().getFfaManager().joinPrivateParty(party.getId(), party.getMembers(), game);
                player.sendMessage(ChatColor.YELLOW + "Starting private FFA for your party...");
            }
            default -> player.sendMessage(ChatColor.RED + "Unknown subcommand.");
        }

        return true;
    }
}

