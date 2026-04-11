package com.tommustbe12.craftmen.party.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.party.Party;
import com.tommustbe12.craftmen.party.PartyManager;
import com.tommustbe12.craftmen.profile.PlayerState;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
            Party party = parties.getParty(player);
            player.sendMessage("§8§m-------------------------");
            if (party == null) {
                player.sendMessage("§cYou are not in a party.");
                player.sendMessage("§7Create one with §e/party create§7.");

                PartyManager.Invite invite = parties.peekInvite(player);
                if (invite != null) {
                    Player inviter = Bukkit.getPlayer(invite.inviterId());
                    String inviterName = inviter != null ? inviter.getName() : "Someone";

                    TextComponent base = new TextComponent("§eParty invite from §a" + inviterName + "§e. ");
                    TextComponent click = new TextComponent("§a§l[CLICK HERE]");
                    click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"));
                    click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("§aClick to accept").create()));
                    base.addExtra(click);
                    player.spigot().sendMessage(base);
                }

                player.sendMessage("§8§m-------------------------");
                return true;
            }

            Player leader = Bukkit.getPlayer(party.getLeader());
            String leaderName = leader != null ? leader.getName() : party.getLeader().toString();
            player.sendMessage("§6Party §7(" + party.getMembers().size() + "/10)");
            player.sendMessage("§7Leader: §e" + leaderName);
            player.sendMessage("§7Members:");
            for (var uuid : party.getMembers()) {
                Player p = Bukkit.getPlayer(uuid);
                String name = p != null ? p.getName() : uuid.toString();
                String prefix = uuid.equals(party.getLeader()) ? "§6★ " : "§7- ";
                String online = p != null ? "§a(online)" : "§c(offline)";
                player.sendMessage(prefix + "§f" + name + " " + online);
            }

            boolean isLeader = party.getLeader().equals(player.getUniqueId());
            player.sendMessage("");
            player.sendMessage("§eCommands: §7/party invite <player>, /party leave, /pc <msg>");
            if (isLeader) {
                player.sendMessage("§eLeader: §7/party ffa §8(open selector), §7/party endffa, /party disband");
            }
            player.sendMessage("§8§m-------------------------");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> {
                if (parties.isInParty(player)) {
                    player.sendMessage(ChatColor.RED + "You are already in a party. Use /party leave first.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                Party party = parties.createParty(player);
                player.sendMessage(ChatColor.GREEN + "Party created.");
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
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
                if (party == null) {
                    party = parties.createParty(player);
                    player.sendMessage(ChatColor.YELLOW + "Party created.");
                }
                if (!party.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can invite.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                if (party.getMembers().size() >= 10) {
                    player.sendMessage(ChatColor.RED + "Party is full (10).");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                if (parties.isInParty(target)) {
                    player.sendMessage(ChatColor.RED + "That player is already in a party.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }

                parties.invite(player, target);
                player.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + " to your party.");
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);

                TextComponent msg = new TextComponent("§e" + player.getName() + " invited you to a party. ");
                TextComponent click = new TextComponent("§a§l[CLICK HERE TO ACCEPT]");
                click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"));
                click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("§aJoin " + player.getName() + "'s party").create()));
                msg.addExtra(click);
                target.spigot().sendMessage(msg);
                target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.1f);
            }
            case "accept" -> {
                if (parties.isInParty(player)) {
                    player.sendMessage(ChatColor.RED + "You are already in a party. Use /party leave first.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                if (parties.accept(player)) {
                    Party party = parties.getParty(player);
                    player.sendMessage(ChatColor.GREEN + "Joined the party.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
                    if (party != null) parties.broadcastParty(party, ChatColor.GREEN + player.getName() + " joined the party.", org.bukkit.Sound.ENTITY_PLAYER_LEVELUP);
                } else {
                    player.sendMessage(ChatColor.RED + "You have no pending party invites.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
            case "leave" -> {
                if (parties.leave(player)) {
                    Craftmen.get().getPartyChatManager().disable(player.getUniqueId());
                    player.sendMessage(ChatColor.RED + "You left the party.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 0.9f);
                } else {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
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
                    Craftmen.get().getPartyChatManager().disable(target.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + " from the party.");
                    target.sendMessage(ChatColor.RED + "You were kicked from the party.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 1.0f);
                    target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.9f);
                } else {
                    player.sendMessage(ChatColor.RED + "Could not kick that player.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
            case "disband" -> {
                Party party = parties.getParty(player);
                if (party == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                if (!party.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can disband.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                for (var uuid : party.getMembers()) {
                    Craftmen.get().getPartyChatManager().disable(uuid);
                }
                parties.disbandParty(party);
                player.sendMessage(ChatColor.RED + "Party disbanded.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 0.6f, 1.3f);
            }
            case "ffa" -> {
                Party party = parties.getParty(player);
                if (party == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                if (!party.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can start a private FFA.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                if (Craftmen.get().getEndFightManager().isInGame(player)) {
                    player.sendMessage(ChatColor.RED + "You cannot start private FFA while in End Fight.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                Craftmen.get().getPartyFfaMenu().openGameSelect(player, party);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            }
            case "endffa" -> {
                Party party = parties.getParty(player);
                if (party == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                if (!party.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the party leader can end the private FFA.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                if (!Craftmen.get().getFfaManager().hasPrivatePartyFfa(party.getId())) {
                    player.sendMessage(ChatColor.RED + "Your party does not have a private FFA running.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
                Craftmen.get().getFfaManager().endPrivatePartyFfa(party.getId());
                player.sendMessage(ChatColor.RED + "Ended your party's private FFA.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.4f);
            }
            default -> player.sendMessage(ChatColor.RED + "Unknown subcommand.");
        }

        return true;
    }
}
