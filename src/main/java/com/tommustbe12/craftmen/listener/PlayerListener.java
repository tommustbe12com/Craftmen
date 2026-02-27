package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import com.tommustbe12.craftmen.profile.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ProfileManager profileManager = Craftmen.get().getProfileManager();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent("§aWelcome to Craftmen!"));

        // buns sound cuz why not
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.7f, 1.2f);

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);

        // load stats from config if they exist
        if (Craftmen.get().getConfig().contains("stats." + player.getUniqueId())) {
            profile.setWins(Craftmen.get().getConfig().getInt("stats." + player.getUniqueId() + ".wins"));
            profile.setLosses(Craftmen.get().getConfig().getInt("stats." + player.getUniqueId() + ".losses"));
            profile.setDeaths(Craftmen.get().getConfig().getInt("stats." + player.getUniqueId() + ".deaths"));
        }
        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
            Craftmen.get().getScoreboardManager().create(player);
        }, 2L); // delay ig for scoreboard cuz buns simpleranks conflicts and stuff
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Craftmen.get().getQueueManager().removePlayer(e.getPlayer());

        Player player = e.getPlayer();
        Craftmen.get().saveProfile(Craftmen.get().getProfileManager().getProfile(player));
        Craftmen.get().getScoreboardManager().remove(player);
        Craftmen.get().getProfileManager().removeProfile(player);
    }
}