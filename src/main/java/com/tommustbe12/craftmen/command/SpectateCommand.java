package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.match.MatchManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class SpectateCommand implements CommandExecutor {

    private final MatchManager matchManager;

    // spectatorUUID -> match
    private static final HashMap<UUID, Match> spectators = new HashMap<>();

    public SpectateCommand() {
        this.matchManager = Craftmen.get().getMatchManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        // /spectate (leave spectate)
        if (args.length == 0) {

            if (!spectators.containsKey(player.getUniqueId())) {
                player.sendMessage("§cYou are not spectating.");
                return true;
            }

            stopSpectating(player, false);
            player.sendMessage("§aYou stopped spectating.");
            return true;
        }

        // /spectate <player>
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            player.sendMessage("§cThat player is not online.");
            return true;
        }

        if (player.equals(target)) {
            player.sendMessage("§cYou cannot spectate yourself.");
            return true;
        }

        // If they are in a match
        Match match = matchManager.getMatch(target);

        if (match == null) {
            player.sendMessage("§cThat player is not in a match.");
            return true;
        }

        // Spectator cannot be in a match
        if (matchManager.getMatch(player) != null) {
            player.sendMessage("§cYou cannot spectate while in a match.");
            return true;
        }

        // If already spectating, remove first
        if (spectators.containsKey(player.getUniqueId())) {
            stopSpectating(player, false);
        }

        spectators.put(player.getUniqueId(), match);

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(target.getLocation());

        player.sendMessage("§aNow spectating §f" + target.getName());

        match.getP1().sendMessage(player.getName() + " is now spectating.");
        match.getP2().sendMessage(player.getName() + " is now spectating.");

        return true;
    }

    public static void stopSpectating(Player player, boolean matchEnded) {

        spectators.remove(player.getUniqueId());

        player.setGameMode(GameMode.SURVIVAL);

        World world = Bukkit.getWorld("world");
        if (world != null) {
            player.teleport(world.getSpawnLocation());
        }

        if (matchEnded) {
            player.sendMessage("§cMatch ended.");
        }
    }

    public static void stopAllSpectatorsOfMatch(Match match) {

        spectators.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(match)) {
                Player spectator = Bukkit.getPlayer(entry.getKey());
                if (spectator != null && spectator.isOnline()) {
                    stopSpectating(spectator, true);
                }
                return true;
            }
            return false;
        });
    }
}