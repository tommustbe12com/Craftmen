package com.tommustbe12.craftmen.gems.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GemsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /gems <player>");
                return true;
            }
            Profile profile = Craftmen.get().getProfileManager().getProfile(player);
            player.sendMessage(ChatColor.AQUA + "Gems: " + ChatColor.WHITE + profile.getGems());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            return true;
        }

        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            Profile profile = Craftmen.get().getProfileManager().getProfile(target);
            sender.sendMessage(ChatColor.AQUA + target.getName() + "'s Gems: " + ChatColor.WHITE + profile.getGems());
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("add")) {
            if (!(sender instanceof Player p) || !p.isOp()) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }

            Player target;
            String amountRaw;
            if (args.length == 2) {
                // /gems add <amount> (adds to yourself)
                target = p;
                amountRaw = args[1];
            } else {
                // /gems add <player> <amount>
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                amountRaw = args[2];
            }

            int amount;
            try {
                amount = Integer.parseInt(amountRaw);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount.");
                return true;
            }
            if (amount <= 0) {
                sender.sendMessage(ChatColor.RED + "Amount must be > 0.");
                return true;
            }

            Profile profile = Craftmen.get().getProfileManager().getProfile(target);
            profile.addGems(amount);
            Craftmen.get().saveProfile(profile);

            sender.sendMessage(ChatColor.GREEN + "Added " + amount + " gems to " + target.getName() + ".");
            if (!target.equals(p)) {
                target.sendMessage(ChatColor.AQUA + "An admin gave you " + ChatColor.WHITE + amount + ChatColor.AQUA + " gems!");
            }
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.3f);
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /gems  OR  /gems <player>  OR  /gems add <player> <amount>");
        return true;
    }
}
