package com.tommustbe12.craftmen.cosmetics.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatColorListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent e) {
        Profile profile = Craftmen.get().getProfileManager().getProfile(e.getPlayer());
        if (profile == null) return;

        String code = profile.getSelectedChatColor();
        if (code == null || code.isBlank()) return;

        String color = ChatColor.translateAlternateColorCodes('&', code);

        String format = e.getFormat();
        if (format == null) return;

        if (format.contains("%2$s")) {
            e.setFormat(format.replace("%2$s", color + "%2$s" + ChatColor.RESET));
        }
    }
}