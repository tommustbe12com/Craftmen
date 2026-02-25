package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.arena.Arena;
import com.tommustbe12.craftmen.arena.ArenaManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetupArenaCommand implements CommandExecutor {

    private final ArenaManager arenaManager = Craftmen.get().getArenaManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("§cUsage: /setuparena <name>");
            return true;
        }

        String arenaName = args[0];
        Location loc = player.getLocation();

        // For simplicity, save pos1=player location, pos2 slightly offset
        Arena arena = new Arena(arenaName, loc, loc.clone().add(5,0,0));
        arenaManager.getArenas().add(arena);

        // Save to config
        Craftmen.get().getConfig().set("arenas." + arenaName + ".pos1.x", loc.getX());
        Craftmen.get().getConfig().set("arenas." + arenaName + ".pos1.y", loc.getY());
        Craftmen.get().getConfig().set("arenas." + arenaName + ".pos1.z", loc.getZ());
        Craftmen.get().getConfig().set("arenas." + arenaName + ".pos2.x", loc.getX()+5);
        Craftmen.get().getConfig().set("arenas." + arenaName + ".pos2.y", loc.getY());
        Craftmen.get().getConfig().set("arenas." + arenaName + ".pos2.z", loc.getZ());

        Craftmen.get().saveConfig();

        player.sendMessage("§aArena " + arenaName + " created!");
        return true;
    }
}