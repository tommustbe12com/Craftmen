package com.tommustbe12.craftmen.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CreditsCommand implements CommandExecutor {
    @Override
    public boolean onCommand( CommandSender Sender, Command cmd, String label, String[] args){

        Sender.sendMessage(ChatColor.DARK_GRAY + "§m----------------------------------------");
        Sender.sendMessage("§6§lCraftmen §f§lCredits");
        Sender.sendMessage("§8 ");
        Sender.sendMessage("§6TomMustBe12 §8- §eMain Dev");
        Sender.sendMessage("§bnytsom §8- §eDev");
        Sender.sendMessage("§eBionicleBlaster §8- §eManager");
        Sender.sendMessage("§aRysterio §8- §eBuilder");
        Sender.sendMessage("§dZembiii §8- §eStaff / Creative Team");
        Sender.sendMessage("§dmecringe §8- §eStaff / Creative Team");
        Sender.sendMessage("§dWarden_Charlie §8- §eCreative Team");
        Sender.sendMessage("§8 ");
        Sender.sendMessage("§7Thanks to all beta testers and contributors!");
        Sender.sendMessage(ChatColor.DARK_GRAY + "§m----------------------------------------");
        return true;
    }
}
