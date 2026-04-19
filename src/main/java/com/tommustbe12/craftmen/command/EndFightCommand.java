package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.endfight.EndFightManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

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

        var party = Craftmen.get().getPartyManager().getParty(player);
        if (party != null) {
            if (!player.getUniqueId().equals(party.getLeader())) {
                player.sendMessage(ChatColor.RED + "Only the party leader can start End Fight for the party.");
                player.sendMessage(ChatColor.GRAY + "Party leader: use the Party Activities item in your hotbar.");
                return true;
            }

            // Leader command starts End Fight for the whole party.
            for (UUID memberId : party.getMembers()) {
                Player m = Bukkit.getPlayer(memberId);
                if (m != null) manager.join(m);
            }
            return true;
        }

        manager.join(player);
        return true;
    }
}
