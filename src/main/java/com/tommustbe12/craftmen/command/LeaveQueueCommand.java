package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import com.tommustbe12.craftmen.queue.QueueManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveQueueCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);

        if (profile.getState() != PlayerState.QUEUED) {
            player.sendMessage("§cYou are not in a queue!");
            return true;
        }

        QueueManager queueManager = Craftmen.get().getQueueManager();
        queueManager.removePlayer(player);

        profile.setState(PlayerState.LOBBY);

        player.sendMessage("§cYou have left the queue.");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        return true;
    }
}