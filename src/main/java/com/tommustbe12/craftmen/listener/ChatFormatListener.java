package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatFormatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        // Message colors: apply selected chat color + allow & codes.
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        String msg = e.getMessage();
        msg = ChatColor.translateAlternateColorCodes('&', msg);

        if (profile != null) {
            String code = profile.getSelectedChatColor();
            if (code != null && !code.isBlank()) {
                String color = ChatColor.translateAlternateColorCodes('&', code);
                msg = color + msg + ChatColor.RESET;
            }
        }
        e.setMessage(msg);

        String badgePrefix = Craftmen.get().getBadgeManager().getDisplay().getBadgePrefix(player);
        if (badgePrefix == null) badgePrefix = "";
        badgePrefix = badgePrefix.trim();

        String rankPrefix = rankPrefix(player);
        rankPrefix = rankPrefix == null ? "" : rankPrefix.trim();

        // Some rank/team prefixes may already include the badge token; prevent duplication.
        if (!badgePrefix.isEmpty() && rankPrefix.startsWith(badgePrefix)) {
            rankPrefix = rankPrefix.substring(badgePrefix.length()).trim();
        }

        StringBuilder prefix = new StringBuilder();
        if (!badgePrefix.isEmpty()) {
            prefix.append(badgePrefix).append(" ");
        }
        if (!rankPrefix.isEmpty()) {
            prefix.append(rankPrefix).append(" ");
        }

        String styledName = styledName(player, profile);

        // Exact format: [Badge] [rank] Name: message
        // We render the name ourselves so it's white by default (unless customized).
        e.setFormat(prefix.toString() + styledName + ChatColor.GRAY + ": " + "%2$s");
    }

    private static String styledName(Player player, Profile profile) {
        String name = player == null ? "" : player.getName();
        String colorCode = (profile == null || profile.getSelectedNameColor() == null || profile.getSelectedNameColor().isBlank())
                ? "&f"
                : profile.getSelectedNameColor();

        String styles = (profile == null) ? null : profile.getSelectedNameStyles();
        if (styles == null) styles = "";

        // Safety: never allow obfuscated (&k).
        styles = styles.replace("&k", "").replace("&K", "");

        String raw = colorCode + styles + name + "&r";
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    private static String rankPrefix(Player player) {
        if (player == null) return "";

        // Prefer main scoreboard so permission/rank plugins are reflected.
        var manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Team t = manager.getMainScoreboard().getEntryTeam(player.getName());
            if (t != null && t.getPrefix() != null && !t.getPrefix().isBlank()) return t.getPrefix();
        }

        // Fallback: player's current scoreboard.
        if (player.getScoreboard() != null) {
            Team t2 = player.getScoreboard().getEntryTeam(player.getName());
            if (t2 != null && t2.getPrefix() != null && !t2.getPrefix().isBlank()) return t2.getPrefix();
        }
        return "";
    }
}
