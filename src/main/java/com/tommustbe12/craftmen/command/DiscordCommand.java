package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DiscordCommand implements CommandExecutor {
    @Override
    public boolean onCommand( CommandSender Sender, Command cmd, String label, String[] args){

        Sender.sendMessage("§b https://discord.gg/Re578UGXur");
        return true;
    }
}
