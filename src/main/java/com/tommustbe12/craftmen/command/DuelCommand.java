package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import com.tommustbe12.craftmen.queue.QueueManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can duel!");
            return true;
        }

        if (args.length != 1) {
            p.sendMessage("§cUsage: /duel <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            p.sendMessage("§cPlayer not found or offline!");
            return true;
        }
        if (target == p) {
            p.sendMessage("§cYou cannot duel yourself!");
            return true;
        }

        Profile pProfile = Craftmen.get().getProfileManager().getProfile(p);
        Profile tProfile = Craftmen.get().getProfileManager().getProfile(target);

        if (pProfile.getState() != PlayerState.LOBBY) {
            p.sendMessage("§cYou are in a queue or in a match!");
            return true;
        }
        if (tProfile.getState() != PlayerState.LOBBY) {
            p.sendMessage("§cThat player is in queue or in a match!");
            return true;
        }

        // open game selector for sender
        Craftmen.get().getHubManager().openGameSelector(p, (game) -> {
            // send duel request after selecting a game
            QueueManager queue = Craftmen.get().getQueueManager();
            queue.addDuelRequest(p, target, game);

            p.sendMessage("§aDuel request sent to §e" + target.getName());
            p.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);

            TextComponent msg = new TextComponent("§e" + p.getName() + " has challenged you to a game of " + game.getName() + "!");
            TextComponent clickHere = new TextComponent("§f[§aCLICK TO ACCEPT§f]");
            clickHere.setBold(true);
            clickHere.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accept " + p.getName()));
            msg.addExtra(clickHere);

            target.spigot().sendMessage(msg);
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        });

        return true;
    }
}