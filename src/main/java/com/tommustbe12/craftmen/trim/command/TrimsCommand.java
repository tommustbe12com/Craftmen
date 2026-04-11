package com.tommustbe12.craftmen.trim.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TrimsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (Craftmen.get().getProfileManager().getProfile(player).getState() != PlayerState.LOBBY) {
            player.sendMessage(ChatColor.RED + "You can only edit armor trims in the hub.");
            return true;
        }

        Craftmen.get().getArmorTrimMenu().openMain(player);
        return true;
    }
}

