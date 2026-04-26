package com.tommustbe12.craftmen.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DiscordCommand implements CommandExecutor {
    @Override
    public boolean onCommand( CommandSender Sender, Command cmd, String label, String[] args){

        Sender.sendMessage(ChatColor.DARK_GRAY + "§m----------------------------------------");
        Sender.sendMessage("§5§lJoin the Craftmen Discord");
        Sender.sendMessage("§8 ");
        Sender.sendMessage("§7Get updates, report bugs, find parties,");
        Sender.sendMessage("§7and meet the community.");
        Sender.sendMessage("§8 ");
        Sender.sendMessage("§d§l➜ §bhttps://discord.gg/Re578UGXur");
        Sender.sendMessage(ChatColor.DARK_GRAY + "§m----------------------------------------");
        return true;
    }
}
