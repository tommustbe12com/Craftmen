package com.tommustbe12.craftmen.ffa.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FfaCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null) return true;

        if (Craftmen.get().getFfaManager().isInFfa(player)) {
            Craftmen.get().getFfaManager().leave(player, true);
            return true;
        }

        if (profile.getState() != PlayerState.LOBBY) {
            player.sendMessage(ChatColor.RED + "You can only join FFA from the hub.");
            return true;
        }

        Craftmen.get().getHubManager().openFfaSelector(player, 0);
        return true;
    }
}

