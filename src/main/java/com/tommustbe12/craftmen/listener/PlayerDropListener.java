package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropListener implements Listener {

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Profile profile = Craftmen.get().getProfileManager().getProfile(event.getPlayer());

        if (profile == null) return;

        // Spawn builder bypass
        if (Craftmen.get().getBuildAllowManager() != null && Craftmen.get().getBuildAllowManager().canBuild(event.getPlayer())) {
            return;
        }

        if (Craftmen.get().getEndFightManager().isInGame(event.getPlayer())) {
            return;
        }

        if (profile.getState() != PlayerState.IN_MATCH
                && profile.getState() != PlayerState.CUSTOM_KIT_PLAYING
                && profile.getState() != PlayerState.FFA_FIGHTING) {
            event.setCancelled(true);
        }
    }
}
