package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
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

    private final List<String> stats = List.of("wins", "losses");
    private final List<String> actions = List.of("add", "sub", "reset");

    // Usage:
    // /stat <player> <wins|losses> <add|sub|reset> [amount]             — overall
    // /stat <player> <wins|losses> <add|sub|reset> [amount] <gameName>  — per-game

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("craftmen.stats")) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§eUsage: /stat <player> <wins|losses> <add|sub|reset> [amount] [gameName]");
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

        if (!stats.contains(stat)) {
            sender.sendMessage("§cInvalid stat! Use wins or losses.");
            return true;
        }

        int amount = 1;
        String gameName = null;

        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (args.length >= 5) {
                    gameName = args[4];
                }
            } catch (NumberFormatException e) {
                gameName = args[3];
            }
        }

        // convert underscore to space for internal game name lookup
        if (gameName != null) {
            gameName = gameName.replace("_", " ");
        }

        handleStat(profile, stat, action, amount, gameName, sender);
        return true;
    }

    private void handleStat(Profile profile, String stat, String action, int amount, String gameName, CommandSender sender) {
        String targetDesc = gameName != null
                ? "§f" + stat + " §a(game: §f" + gameName + "§a) for §f" + profile.getPlayer().getName()
                : "§f" + stat + " §afor §f" + profile.getPlayer().getName();

        switch (action) {
            case "add" -> {
                if (gameName != null) {
                    // add to game-specific AND overall
                    if (stat.equals("wins")) {
                        profile.setGameWins(gameName, profile.getGameWins(gameName) + amount);
                    } else {
                        profile.setGameLosses(gameName, profile.getGameLosses(gameName) + amount);
                    }
                }
                profile.addStat(stat, amount);
                sender.sendMessage("§aAdded §f" + amount + " §ato " + targetDesc);
            }
            case "sub" -> {
                if (gameName != null) {
                    if (stat.equals("wins")) {
                        profile.setGameWins(gameName, Math.max(0, profile.getGameWins(gameName) - amount));
                    } else {
                        profile.setGameLosses(gameName, Math.max(0, profile.getGameLosses(gameName) - amount));
                    }
                }
                profile.addStat(stat, -amount);
                sender.sendMessage("§aSubtracted §f" + amount + " §afrom " + targetDesc);
            }
            case "reset" -> {
                if (gameName != null) {
                    // reset only that game
                    if (stat.equals("wins")) {
                        profile.setGameWins(gameName, 0);
                    } else {
                        profile.setGameLosses(gameName, 0);
                    }
                    sender.sendMessage("§aReset " + targetDesc);
                } else {
                    // reset overall + all game entries for that stat
                    profile.setStat(stat, 0);
                    if (stat.equals("wins")) {
                        profile.getGameWins().clear();
                    } else {
                        profile.getGameLosses().clear();
                    }
                    sender.sendMessage("§aReset all " + targetDesc);
                }
            }
            default -> sender.sendMessage("§cInvalid action! Use add, sub, or reset.");
        }

        // auto-save
        Craftmen.get().saveProfile(profile);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("craftmen.stats")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 1 -> { // player names
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
            case 2 -> { // stat names
                for (String s : stats) {
                    if (s.startsWith(args[1].toLowerCase())) completions.add(s);
                }
            }
            case 3 -> { // actions
                for (String a : actions) {
                    if (a.startsWith(args[2].toLowerCase())) completions.add(a);
                }
            }
            case 4 -> { // amount hint
                completions.add("1");
            }
            case 5 -> {
                for (Game game : Craftmen.get().getGameManager().getGames()) {
                    String displayName = game.getName().replace(" ", "_");
                    if (displayName.toLowerCase().startsWith(args[4].toLowerCase())) {
                        completions.add(displayName);
                    }
                }
            }
        }

        return completions;
    }
}