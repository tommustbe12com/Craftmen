package com.tommustbe12.craftmen.gems;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.ChatColor;
import org.bukkit.Sound;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GemManager {

    // "limited though": implement as one-time-per-threshold rewards per player.
    private static final Map<Integer, Integer> WIN_STREAK_REWARDS = new LinkedHashMap<>();
    static {
        WIN_STREAK_REWARDS.put(3, 10);
        WIN_STREAK_REWARDS.put(5, 20);
        WIN_STREAK_REWARDS.put(10, 50);
        WIN_STREAK_REWARDS.put(20, 100);
    }

    public void handleWinStreakRewards(Profile profile) {
        if (profile == null) return;
        int streak = profile.getKillsInARow();

        for (var entry : WIN_STREAK_REWARDS.entrySet()) {
            int threshold = entry.getKey();
            int gems = entry.getValue();
            if (streak < threshold) continue;
            if (profile.hasClaimedWinStreakReward(threshold)) continue;

            profile.addGems(gems);
            profile.claimWinStreakReward(threshold);
            var p = profile.getPlayer();
            if (p != null && p.isOnline()) {
                p.sendMessage(ChatColor.AQUA + "+" + gems + " Gems " + ChatColor.GRAY + "(win streak: " + threshold + ")");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
            }
            Craftmen.get().saveProfile(profile);
        }
    }

    public int getKillStreakBonus(int streak) {
        int s = Math.min(100, Math.max(0, streak));
        if (s >= 100) return 25;
        if (s >= 50) return 10;
        if (s >= 20) return 5;
        if (s >= 10) return 3;
        if (s >= 5) return 2;
        return 0;
    }

    public void awardWinGems(Profile winnerProfile) {
        if (winnerProfile == null) return;
        winnerProfile.addGems(1);
        Craftmen.get().saveProfile(winnerProfile);
        var p = winnerProfile.getPlayer();
        if (p != null && p.isOnline()) {
            p.sendMessage(ChatColor.AQUA + "+1 Gems " + ChatColor.GRAY + "(win)");
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
        }
    }
}
