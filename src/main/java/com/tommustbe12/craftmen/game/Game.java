package com.tommustbe12.craftmen.game;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class Game {

    private final String name;
    private final ItemStack iconItemStack;

    public Game(String name, ItemStack iconItemStack) {
        this.name = name;
        this.iconItemStack = iconItemStack;
    }

    public List<Player> getPlayersInGame() {
        List<Player> players = new ArrayList<>();

        // Iterate through all matches related to this game
        for (Match match : Craftmen.get().getMatchManager().getMatchesByGame(this)) {
            Player p1 = match.getP1();
            Player p2 = match.getP2();

            // Add players to the list if they are not null
            if (p1 != null) players.add(p1);
            if (p2 != null) players.add(p2);
        }

        return players;
    }

    public String getName() { return name; }

    public ItemStack getIcon() {
        return iconItemStack;
    }

    public final void applyLoadout(Player player) {
        if (player == null) return;
        Kit kit = Craftmen.get().getKitManager().getEffectiveKit(player, this);
        kit.apply(player);
        Craftmen.get().getArmorTrimManager().apply(player);
        afterLoadoutApplied(player);
    }

    public abstract Kit createDefaultKit();

    protected void afterLoadoutApplied(Player player) {
        // Optional hook for games that need non-inventory setup (health, hunger, effects, etc.)
    }

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
