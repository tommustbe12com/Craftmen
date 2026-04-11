package com.tommustbe12.craftmen.badge.listener;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class BadgeChatListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        String badgePrefix = Craftmen.get().getBadgeManager().getDisplay().getBadgePrefix(e.getPlayer());
        if (badgePrefix == null || badgePrefix.isEmpty()) return;

        String format = e.getFormat();
        if (format == null || !format.contains("%1$s")) return;

        // Don't double-inject.
        if (format.contains(badgePrefix + "%1$s")) return;

        e.setFormat(format.replace("%1$s", badgePrefix + "%1$s"));
    }
}

