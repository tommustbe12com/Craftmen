package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.match.MatchManager;
import com.tommustbe12.craftmen.profile.Profile;
import com.tommustbe12.craftmen.profile.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.7f, 1.2f);

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        Craftmen.get().loadProfile(profile);
        validateSelectedBadge(profile);

        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
            if (player.isOnline()) {
                Craftmen.get().getScoreboardManager().create(player);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Craftmen.get().getScoreboardManager().update(p);
                }
            }
        }, 2L);

        player.setGameMode(GameMode.SURVIVAL);
        player.setSaturation(20f);
        player.setHealth(20);
        player.teleport(Craftmen.get().getHubLocation());

        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
            if (!player.isOnline()) return;
            Craftmen.get().getBadgeManager().getDisplay().apply(player);
            Craftmen.get().getHubManager().giveHubItems(player);
            Craftmen.get().getScoreboardManager().update(player);
        }, 3L);
    }

    private void validateSelectedBadge(Profile profile) {
        if (profile == null) return;
        if (profile.getSelectedBadgeId() == null) return;

        var badge = Craftmen.get().getBadgeManager().getBadges().stream()
                .filter(b -> b.getId().equals(profile.getSelectedBadgeId()))
                .findFirst()
                .orElse(null);
        if (badge == null) {
            profile.setSelectedBadgeId(null);
            Craftmen.get().saveProfile(profile);
            return;
        }

        var req = com.tommustbe12.craftmen.badge.BadgeRequirement.parse(badge.getRequirement());
        boolean ok = req.map(r -> r.matches(profile)).orElse(false);
        if (!ok) {
            profile.setSelectedBadgeId(null);
            Craftmen.get().saveProfile(profile);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {

        Player player = e.getPlayer();

        MatchManager matchManager = Craftmen.get().getMatchManager();
        Match match = matchManager.getMatch(player);

        // are they in a match. if so, end it
        if (match != null) {
            Player opponent;

            if (match.getP1().equals(player)) {
                opponent = match.getP2();
            } else {
                opponent = match.getP1();
            }

            // End match, opponent is winner
            matchManager.endMatch(match, opponent);

            if (opponent != null && opponent.isOnline()) {
                opponent.sendMessage("§aYour opponent left the match. You win!");
            }
        }

        // remove from q if they are in it
        Craftmen.get().getQueueManager().removePlayer(player);

        // save cleanup
        Craftmen.get().saveProfile(Craftmen.get().getProfileManager().getProfile(player));
        Craftmen.get().getScoreboardManager().remove(player);
        Craftmen.get().getProfileManager().removeProfile(player);
    }
}
