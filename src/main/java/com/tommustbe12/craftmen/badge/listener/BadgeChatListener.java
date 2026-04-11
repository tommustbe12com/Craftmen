package com.tommustbe12.craftmen.badge.listener;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class BadgeChatListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent e) {
        String badgePrefix = Craftmen.get().getBadgeManager().getDisplay().getBadgePrefix(e.getPlayer());
        if (badgePrefix == null || badgePrefix.isEmpty()) return;

        String format = e.getFormat();
        if (format == null) return;

        // Put badge at the very start so it becomes:
        // [BADGE] [RANK] username: message
        // This is more compatible with rank/chat plugins that build a full prefix in the format.
        if (startsWithNormalized(format, badgePrefix)) return;

        e.setFormat(badgePrefix + format);
    }

    private static boolean startsWithNormalized(String value, String prefix) {
        if (value == null || prefix == null) return false;
        return removeVariationSelectors(value).startsWith(removeVariationSelectors(prefix));
    }

    private static String removeVariationSelectors(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '\uFE00' && c <= '\uFE0F') continue;
            out.append(c);
        }
        return out.toString();
    }
}
