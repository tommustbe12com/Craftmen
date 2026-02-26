package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.ProfileManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ProfileManager profileManager = Craftmen.get().getProfileManager();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        profileManager.getProfile(e.getPlayer());
        e.getPlayer().sendMessage("§aWelcome to Craftmen!");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        profileManager.removeProfile(e.getPlayer());
        Craftmen.get().getQueueManager().removePlayer(e.getPlayer());
    }
}