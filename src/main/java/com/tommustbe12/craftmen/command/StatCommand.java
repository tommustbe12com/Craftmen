package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatCommand implements CommandExecutor, TabCompleter {

    private final List<String> stats = List.of("wins", "losses", "deaths");
    private final List<String> actions = List.of("add", "sub", "reset");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("craftmen.stats")) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§eUsage: /stat <player> <wins|losses|deaths> <add|sub|reset> [amount]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(target);
        String stat = args[1].toLowerCase();
        String action = args[2].toLowerCase();
        int amount = 1;

        if ((action.equals("add") || action.equals("sub")) && args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cAmount must be a number!");
                return true;
            }
        }

        switch (stat) {
            case "wins", "losses", "deaths" -> handleStat(profile, stat, action, amount, sender);
            default -> sender.sendMessage("§cInvalid stat! Use wins, losses, or deaths.");
        }

        return true;
    }

    private void handleStat(Profile profile, String stat, String action, int amount, CommandSender sender) {
        switch (action) {
            case "add" -> {
                profile.addStat(stat, amount);
                sender.sendMessage("§aAdded §f" + amount + " §ato " + stat + " for " + profile.getPlayer().getName());
            }
            case "sub" -> {
                profile.addStat(stat, -amount);
                sender.sendMessage("§aSubtracted §f" + amount + " §afrom " + stat + " for " + profile.getPlayer().getName());
            }
            case "reset" -> {
                profile.setStat(stat, 0);
                sender.sendMessage("§aReset §f" + stat + " §afor " + profile.getPlayer().getName());
            }
            default -> sender.sendMessage("§cInvalid action! Use add, sub, or reset.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("craftmen.stats")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 1 -> { // Player names
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
            case 2 -> { // Stat names
                for (String s : stats) {
                    if (s.startsWith(args[1].toLowerCase())) completions.add(s);
                }
            }
            case 3 -> { // Actions
                for (String a : actions) {
                    if (a.startsWith(args[2].toLowerCase())) completions.add(a);
                }
            }
            default -> completions = Collections.emptyList();
        }

        return completions;
    }
}