package com.tommustbe12.craftmen.match;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.arena.Arena;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class Match {

    private final Player p1;
    private final Player p2;
    private final Game game;
    private final Arena arena;
    private boolean ended = false;

    public Match(Player p1, Player p2, Game game, Arena arena) {
        this.p1 = p1;
        this.p2 = p2;
        this.game = game;
        this.arena = arena;
    }

    public void start() {
        ended = false; // FIX to fix the stupid thingy not letting me do more than 1 match

        p1.setGameMode(GameMode.SURVIVAL); // just in case lol and for admins test
        p2.setGameMode(GameMode.SURVIVAL);

        p1.teleport(arena.getSpawn1()); // tp
        p2.teleport(arena.getSpawn2());

        // freeze players during countdown
        Craftmen.get().getProfileManager().getProfile(p1).setState(PlayerState.COUNTDOWN);
        Craftmen.get().getProfileManager().getProfile(p2).setState(PlayerState.COUNTDOWN);

        // start countdown INSIDE Game CLASS
        game.startCountdown(this);
    }

    // after match end
    public void end(Player winner) {
        if (ended) {
            System.out.println("Match was already ended.");
            return;
        }
        ended = true;

        game.onEnd(this);

        Player p1 = getP1();
        Player p2 = getP2();
        Player loser = winner == p1 ? p2 : p1;

        // spectator mode for loser
        loser.setGameMode(org.bukkit.GameMode.SPECTATOR);

        // winner in survival.
        winner.setGameMode(org.bukkit.GameMode.SURVIVAL);

        // START COOL ENDING
        // winner
        winner.sendTitle(
                net.md_5.bungee.api.ChatColor.GOLD + "" + net.md_5.bungee.api.ChatColor.BOLD + "VICTORY!",
                net.md_5.bungee.api.ChatColor.YELLOW + "Defeated " + loser.getName(),
                10, 70, 20
        );
        winner.playSound(winner.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);

        // animate
        new org.bukkit.scheduler.BukkitRunnable() {
            int step = 0;
            final int total = 20; // duration frames
            @Override
            public void run() {
                if (step++ > total) cancel();
                String ripple = net.md_5.bungee.api.ChatColor.GOLD + "" + net.md_5.bungee.api.ChatColor.BOLD +
                        "V" + net.md_5.bungee.api.ChatColor.YELLOW + "I" + net.md_5.bungee.api.ChatColor.GOLD + "C" +
                        net.md_5.bungee.api.ChatColor.YELLOW + "T" + net.md_5.bungee.api.ChatColor.GOLD + "O" +
                        net.md_5.bungee.api.ChatColor.YELLOW + "R" + net.md_5.bungee.api.ChatColor.GOLD + "Y!";
                winner.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ripple));
            }
        }.runTaskTimer(Craftmen.get(), 0, 2);

        // loser
        loser.sendTitle(
                net.md_5.bungee.api.ChatColor.RED + "" + net.md_5.bungee.api.ChatColor.BOLD + "GAME OVER",
                net.md_5.bungee.api.ChatColor.DARK_RED + "Defeated by " + winner.getName(),
                10, 70, 20
        );
        loser.playSound(loser.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 1.0f, 0.8f);

        // Animated action bar ripple for loser
        new org.bukkit.scheduler.BukkitRunnable() {
            int step = 0;
            final int total = 20;
            @Override
            public void run() {
                if (step++ > total) cancel();
                String ripple = net.md_5.bungee.api.ChatColor.DARK_RED + "G" +
                        net.md_5.bungee.api.ChatColor.RED + "A" +
                        net.md_5.bungee.api.ChatColor.DARK_RED + "M" +
                        net.md_5.bungee.api.ChatColor.RED + "E " +
                        net.md_5.bungee.api.ChatColor.DARK_RED + "O" +
                        net.md_5.bungee.api.ChatColor.RED + "V" +
                        net.md_5.bungee.api.ChatColor.DARK_RED + "E" +
                        net.md_5.bungee.api.ChatColor.RED + "R";
                loser.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ripple));
            }
        }.runTaskTimer(Craftmen.get(), 0, 2);
        // END COOL ENDING

        Profile winnerProfile = Craftmen.get().getProfileManager().getProfile(winner);
        winnerProfile.addWin();

        Profile loserProfile = Craftmen.get().getProfileManager().getProfile(loser);
        loserProfile.addLoss();
        loserProfile.addDeath();

        // After 5 seconds, send both to hub
        Craftmen.get().getServer().getScheduler().runTaskLater(Craftmen.get(), () -> {

            // Teleport both to hub
            p1.teleport(Craftmen.get().getHubLocation());
            p2.teleport(Craftmen.get().getHubLocation());

            // Reset inventory and state
            p1.getInventory().clear();
            p2.getInventory().clear();

            Craftmen.get().getProfileManager().getProfile(p1).setState(PlayerState.LOBBY);
            Craftmen.get().getProfileManager().getProfile(p2).setState(PlayerState.LOBBY);

            // Give hub/game selector sword to winner
            giveHubSword(p1);
            giveHubSword(p2);

            // Reset game mode in case they were spectating
            p1.setGameMode(org.bukkit.GameMode.SURVIVAL);
            p2.setGameMode(org.bukkit.GameMode.SURVIVAL);

        }, 5 * 20L); // 5 seconds = 100 ticks
    }

    private void giveHubSword(Player player) {
        player.getInventory().clear(); // optional, make sure they only get this
        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD);
        org.bukkit.inventory.meta.ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Game Selector");
            sword.setItemMeta(meta);
        }
        player.getInventory().setItem(0, sword); // put in first hotbar slot
    }

    public Player getP1() { return p1; }
    public Player getP2() { return p2; }
    public Game getGame() { return game; }
    public Arena getArena() { return arena; }
}