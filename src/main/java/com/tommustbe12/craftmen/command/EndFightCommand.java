package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.endfight.EndFightManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EndFightCommand implements CommandExecutor {

    private final EndFightManager manager;

    public EndFightCommand(EndFightManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;
        manager.join(player);
        return true;
    }
}