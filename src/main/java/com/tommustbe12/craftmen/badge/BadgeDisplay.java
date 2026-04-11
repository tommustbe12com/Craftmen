package com.tommustbe12.craftmen.badge;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BadgeDisplay {

    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    // Track what we injected so we can preserve other plugins' prefixes.
    // viewer|target -> injected prefix
    private final Map<String, String> lastInjectedTeamPrefix = new HashMap<>();
    private final Map<UUID, String> lastInjectedDisplayPrefix = new HashMap<>();
    private final Map<UUID, String> lastInjectedTabPrefix = new HashMap<>();

    public void apply(Player player) {
        if (player == null) return;
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null) return;

        BadgeDefinition badge = null;
        UUID badgeId = profile.getSelectedBadgeId();
        if (badgeId != null) badge = Craftmen.get().getBadgeManager().getBadges().stream()
                .filter(b -> b.getId().equals(badgeId))
                .findFirst()
                .orElse(null);

        String badgePrefix = badge == null ? "" : renderBadgePrefix(badge);

        applyTeamPrefix(player, badgePrefix);
        applyDisplayNames(player, badgePrefix);
    }

    public void clear(Player player) {
        if (player == null) return;
        applyTeamPrefix(player, "");
        applyDisplayNames(player, "");
    }

    private void applyDisplayNames(Player player, String badgePrefix) {
        // display name (often used by chat plugins)
        Component currentDisplay = player.displayName();
        String currentLegacy = legacy.serialize(currentDisplay);
        // Strip our previous badge injection even if the stored injection doesn't match 1:1
        // due to variation selectors (VS16) or font quirks.
        String injectedDisplay = lastInjectedDisplayPrefix.getOrDefault(player.getUniqueId(), "");
        String baseLegacy = stripLeadingInjectedBadge(currentLegacy, injectedDisplay);
        String nextLegacy = badgePrefix + baseLegacy;
        player.displayName(legacy.deserialize(nextLegacy));
        // Also set legacy display name for plugins/events that still use getDisplayName().
        player.setDisplayName(nextLegacy);
        lastInjectedDisplayPrefix.put(player.getUniqueId(), badgePrefix);

        // tab list name
        Component currentTab = player.playerListName();
        String currentTabLegacy = legacy.serialize(currentTab);
        String injectedTab = lastInjectedTabPrefix.getOrDefault(player.getUniqueId(), "");
        String baseTabLegacy = stripLeadingInjectedBadge(currentTabLegacy, injectedTab);
        String nextTabLegacy = badgePrefix + baseTabLegacy;
        player.playerListName(legacy.deserialize(nextTabLegacy));
        // Also set legacy tab name for plugins/events that still use getPlayerListName().
        player.setPlayerListName(nextTabLegacy);
        lastInjectedTabPrefix.put(player.getUniqueId(), badgePrefix);
    }

    private void applyTeamPrefix(Player player, String badgePrefix) {
        // Nametag visibility is based on the VIEWER's scoreboard, not the target player's scoreboard.
        // Inject into whatever team other plugins already assigned the target to, for every online viewer.
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            var board = viewer.getScoreboard();
            if (board == null) continue;

            Team team = board.getEntryTeam(player.getName());
            if (team == null) continue;

            String key = viewer.getUniqueId() + "|" + player.getUniqueId();
            String injected = lastInjectedTeamPrefix.getOrDefault(key, "");

            String currentPrefix = team.getPrefix();
            String basePrefix = stripLeading(currentPrefix, injected);
            String nextPrefix = badgePrefix + basePrefix;

            if (!nextPrefix.equals(currentPrefix)) {
                team.setPrefix(nextPrefix);
            }

            lastInjectedTeamPrefix.put(key, badgePrefix);
        }
    }

    private static String stripLeading(String value, String leading) {
        if (value == null) return "";
        if (leading == null || leading.isEmpty()) return value;
        return value.startsWith(leading) ? value.substring(leading.length()) : value;
    }

    private static String stripLeadingInjectedBadge(String value, String previouslyInjectedBadgePrefix) {
        if (value == null) return "";
        if (previouslyInjectedBadgePrefix == null || previouslyInjectedBadgePrefix.isEmpty()) return value;

        String normValue = removeVariationSelectors(value);
        String normLeading = removeVariationSelectors(previouslyInjectedBadgePrefix);

        if (!normValue.startsWith(normLeading)) {
            return value;
        }

        // Remove the *actual* leading badge prefix from the original string by scanning its structure:
        // (color codes)* "[" ... "]" (space)?
        int i = 0;
        while (i + 1 < value.length() && value.charAt(i) == '§') {
            i += 2; // skip color code
        }
        if (i >= value.length() || value.charAt(i) != '[') {
            // fallback: best-effort strip by length
            return value.substring(Math.min(value.length(), previouslyInjectedBadgePrefix.length()));
        }
        int close = value.indexOf(']', i + 1);
        if (close == -1) {
            return value.substring(Math.min(value.length(), previouslyInjectedBadgePrefix.length()));
        }
        int end = close + 1;
        if (end < value.length() && value.charAt(end) == ' ') end++;
        return value.substring(end);
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

    public String getBadgePrefix(Player player) {
        if (player == null) return "";
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null) return "";
        UUID badgeId = profile.getSelectedBadgeId();
        if (badgeId == null) return "";

        BadgeDefinition badge = Craftmen.get().getBadgeManager().getBadges().stream()
                .filter(b -> b.getId().equals(badgeId))
                .findFirst()
                .orElse(null);
        if (badge == null) return "";
        return renderBadgePrefix(badge);
    }

    private static String renderBadgePrefix(BadgeDefinition badge) {
        String color = badge.getColor() == null ? "&7" : badge.getColor();
        String translated = ChatColor.translateAlternateColorCodes('&', color);
        String icon = sanitizeIcon(badge.getIcon());
        // brackets and icon same color
        return translated + "[" + icon + "] ";
    }

    private static String sanitizeIcon(String icon) {
        if (icon == null) return "";
        String trimmed = icon.trim();
        // If someone pasted "[x]" as icon, don't double-wrap.
        if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        StringBuilder out = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            // Strip variation selectors (commonly causes weird "VS16" artifacts on some clients/fonts)
            if (c >= '\uFE00' && c <= '\uFE0F') continue;

            // Strip formatting control chars
            if (c <= 0x1F || c == 0x7F) continue;

            out.append(c);
        }
        return out.toString();
    }

    public void startAutoRefresh() {
        Bukkit.getScheduler().runTaskTimer(Craftmen.get(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                apply(p);
            }
        }, 20L, 20L);
    }
}
