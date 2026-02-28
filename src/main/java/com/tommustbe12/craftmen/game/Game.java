package com.tommustbe12.craftmen.game;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

public abstract class Game {

    private final String name;
    private final Material iconMaterial;

    public Game(String name, Material iconMaterial) {
        this.name = name;
        this.iconMaterial = iconMaterial;
    }

    public String getName() { return name; }

    public ItemStack getIcon() {
        return new ItemStack(iconMaterial);
    }

    public abstract void applyLoadout(Player player);

    public abstract void onStart(Match match);

    // match end
    public abstract void onEnd(Match match);

    public void startCountdown(Match match) {
        Player p1 = match.getP1();
        Player p2 = match.getP2();

        // give loadouts
        applyLoadout(p1);
        applyLoadout(p2);

        // this is buns but whatever
        for (int i = 3; i >= 1; i--) {
            final int count = i;
            Craftmen.get().getServer().getScheduler().runTaskLater(Craftmen.get(), () -> {

                String color;
                switch (count) {
                    case 3 -> color = "§a";
                    case 2 -> color = "§e";
                    case 1 -> color = "§c";
                    default -> color = "§f";
                }

                p1.sendTitle(color + count, "", 0, 20, 0);
                p2.sendTitle(color + count, "", 0, 20, 0);

                p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
                p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);

            }, (4 - i) * 20L); // schedule 1 second
        }

        Craftmen.get().getServer().getScheduler().runTaskLater(Craftmen.get(), () -> {
            // pvp, movement
            Craftmen.get().getProfileManager().getProfile(p1).setState(PlayerState.IN_MATCH);
            Craftmen.get().getProfileManager().getProfile(p2).setState(PlayerState.IN_MATCH);

            // fight title
            p1.sendTitle("§a§lFIGHT!", "", 5, 40, 5);
            p2.sendTitle("§a§lFIGHT!", "", 5, 40, 5);

            p1.playSound(p1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            p2.playSound(p2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

            onStart(match);
        }, 4 * 20L);
    }
}