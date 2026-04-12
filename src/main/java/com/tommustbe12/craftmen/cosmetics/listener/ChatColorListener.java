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
        // Most chat plugins replace the message later; the most compatible approach is to color the message itself.
        String msg = e.getMessage();
        if (msg == null) return;
        if (msg.startsWith(color)) return;
        e.setMessage(color + msg + ChatColor.RESET);
    }
}
