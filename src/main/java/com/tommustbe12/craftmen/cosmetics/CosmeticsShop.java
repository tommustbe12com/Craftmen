package com.tommustbe12.craftmen.cosmetics;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class CosmeticsShop {

    public static boolean purchase(Player player, String cosmeticId, int cost) {
        if (player == null || cosmeticId == null) return false;
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null) return false;

        if (profile.hasCosmetic(cosmeticId)) {
            player.sendMessage(ChatColor.YELLOW + "You already own that.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        if (profile.getGems() < cost) {
            player.sendMessage(ChatColor.RED + "Not enough gems. You need " + cost + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        profile.setGems(profile.getGems() - cost);
        profile.addCosmetic(cosmeticId);
        Craftmen.get().saveProfile(profile);

        player.sendMessage(ChatColor.AQUA + "Purchased for " + ChatColor.WHITE + cost + ChatColor.AQUA + " gems.");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        return true;
    }
}

