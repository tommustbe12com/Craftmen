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
    private final Map<UUID, String> lastInjectedTeamPrefix = new HashMap<>();
    private final Map<UUID, String> lastInjectedDisplayPrefix = new HashMap<>();

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
        String injected = lastInjectedDisplayPrefix.getOrDefault(player.getUniqueId(), "");

        // display name (often used by chat plugins)
        Component currentDisplay = player.displayName();
        String currentLegacy = legacy.serialize(currentDisplay);
        String baseLegacy = stripLeading(currentLegacy, injected);
        String nextLegacy = badgePrefix + baseLegacy;
        player.displayName(legacy.deserialize(nextLegacy));
        lastInjectedDisplayPrefix.put(player.getUniqueId(), badgePrefix);

        // tab list name
        Component currentTab = player.playerListName();
        String currentTabLegacy = legacy.serialize(currentTab);
        String baseTabLegacy = stripLeading(currentTabLegacy, injected);
        String nextTabLegacy = badgePrefix + baseTabLegacy;
        player.playerListName(legacy.deserialize(nextTabLegacy));
    }

    private void applyTeamPrefix(Player player, String badgePrefix) {
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team == null) return;

        String injected = lastInjectedTeamPrefix.getOrDefault(player.getUniqueId(), "");
        String currentPrefix = team.getPrefix();
        String basePrefix = stripLeading(currentPrefix, injected);
        String nextPrefix = badgePrefix + basePrefix;

        // Avoid extra writes
        if (!nextPrefix.equals(currentPrefix)) {
            team.setPrefix(nextPrefix);
        }
        lastInjectedTeamPrefix.put(player.getUniqueId(), badgePrefix);
    }

    private static String stripLeading(String value, String leading) {
        if (value == null) return "";
        if (leading == null || leading.isEmpty()) return value;
        return value.startsWith(leading) ? value.substring(leading.length()) : value;
    }

    private static String renderBadgePrefix(BadgeDefinition badge) {
        String color = badge.getColor() == null ? "&7" : badge.getColor();
        String translated = ChatColor.translateAlternateColorCodes('&', color);
        // brackets and icon same color
        return translated + "[" + badge.getIcon() + "] ";
    }

    public void startAutoRefresh() {
        Bukkit.getScheduler().runTaskTimer(Craftmen.get(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                apply(p);
            }
        }, 20L, 20L);
    }
}

