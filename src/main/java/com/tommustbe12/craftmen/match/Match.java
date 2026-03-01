package com.tommustbe12.craftmen.match;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.arena.Arena;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class Match {

    private final Player p1;
    private final Player p2;
    private final Game game;
    private final Arena arena;
    private boolean ended = false;

    private Location pasteMinLocation;
    private Location pasteMaxLocation;

    public Match(Player p1, Player p2, Game game, Arena arena) {
        this.p1 = p1;
        this.p2 = p2;
        this.game = game;
        this.arena = arena;
    }

    public void start() {
        ended = false;

        // set survival just in case
        p1.setGameMode(GameMode.SURVIVAL);
        p2.setGameMode(GameMode.SURVIVAL);

        World world = Bukkit.getWorld("world");
        int width = Craftmen.get().getArenaManager().getSchematicWidth(arena.getCategory(), arena.getName());
        int length = Craftmen.get().getArenaManager().getSchematicLength(arena.getCategory(), arena.getName());
        Random rand = new Random();

        // find a random empty location
        Location pasteLoc;
        boolean found = false;
        do {
            int x = rand.nextInt(500) + 500;
            int z = rand.nextInt(500) + 500;
            int y = 64; // default ground level
            pasteLoc = new Location(world, x, y, z);
            found = Craftmen.get().getArenaManager().isAreaEmpty(pasteLoc, width, length, world);
        } while (!found);

        // paste arena
        Craftmen.get().getArenaManager().pasteArena(arena.getCategory(), arena.getName(), pasteLoc, this);

        int height = Craftmen.get().getArenaManager()
                .getSchematicHeight(arena.getCategory(), arena.getName());

        // Wait 2 ticks to ensure paste is finished
        Location finalPasteLoc = pasteLoc;
        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {

            java.util.List<Location> spawns = findIronSpawns(finalPasteLoc, world);

            if (spawns.size() != 2) {
                Bukkit.getLogger().severe("Arena must contain exactly 2 IRON_BLOCK spawn markers! found: " + spawns.size());

                if (pasteMinLocation != null && pasteMaxLocation != null) {
                    Craftmen.get().getArenaManager().removeArenaAtLocation(
                            arena.getName(),
                            pasteMinLocation,
                            pasteMaxLocation
                    );
                }
                return;
            }

            // initial
            p1.teleport(spawns.get(0));

            //spawn looking at player
            p2.teleport(spawns.get(1).setDirection(p1.getLocation().subtract(p2.getLocation()).toVector()));
            p1.teleport(p1.getLocation().setDirection(p2.getLocation().subtract(p1.getLocation()).toVector()));

            // freeze players
            Craftmen.get().getProfileManager().getProfile(p1).setState(PlayerState.COUNTDOWN);
            Craftmen.get().getProfileManager().getProfile(p2).setState(PlayerState.COUNTDOWN);

            game.startCountdown(this);

        }, 2L);
    }

    public void end(Player winner) {
        if (ended) return;
        ended = true;

        game.onEnd(this);

        Player p1 = getP1();
        Player p2 = getP2();
        Player loser = winner == p1 ? p2 : p1;

        // set game modes
        loser.setGameMode(GameMode.SPECTATOR);
        winner.setGameMode(GameMode.SURVIVAL);

        // victory messages
        winner.sendTitle("§6§lVICTORY!", "§eDefeated " + loser.getName(), 10, 70, 20);
        winner.playSound(winner.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);

        loser.sendTitle("§c§lGAME OVER", "§4Defeated by " + winner.getName(), 10, 70, 20);
        loser.playSound(loser.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 1.0f, 0.8f);

        // update profiles
        Profile winnerProfile = Craftmen.get().getProfileManager().getProfile(winner);
        winnerProfile.addWin();
        Profile loserProfile = Craftmen.get().getProfileManager().getProfile(loser);
        loserProfile.addLoss();
        loserProfile.addDeath();

        // after 5 seconds, reset players and remove arena
        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {

            // full health
            p1.setHealth(20.0);
            p2.setHealth(20.0);

            // teleport to hub
            p1.teleport(Craftmen.get().getHubLocation());
            p2.teleport(Craftmen.get().getHubLocation());

            // reset inventories
            p1.getInventory().clear();
            p2.getInventory().clear();

            // reset player states
            Craftmen.get().getProfileManager().getProfile(p1).setState(PlayerState.LOBBY);
            Craftmen.get().getProfileManager().getProfile(p2).setState(PlayerState.LOBBY);

            // give hub swords
            giveHubSword(p1);
            giveHubSword(p2);

            // reset gamemode
            p1.setGameMode(GameMode.SURVIVAL);
            p2.setGameMode(GameMode.SURVIVAL);

            // remove ONLY this match’s arena
            if (pasteMinLocation != null && pasteMaxLocation != null) {
                Craftmen.get().getArenaManager().removeArenaAtLocation(
                        arena.getName(),
                        pasteMinLocation,
                        pasteMaxLocation
                );
            }

        }, 5 * 20L);
    }

    private void giveHubSword(Player player) {
        player.getInventory().clear();
        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD);
        org.bukkit.inventory.meta.ItemMeta meta = sword.getItemMeta();
        if (meta != null) meta.setDisplayName("§6Game Selector");
        sword.setItemMeta(meta);
        player.getInventory().setItem(0, sword);
    }

    private java.util.List<Location> findIronSpawns(Location center, World world) {

        java.util.List<Location> spawns = new java.util.ArrayList<>();

        int radius = 100; // large safe radius

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    Location check = center.clone().add(x, y, z);

                    if (world.getBlockAt(check).getType() == Material.IRON_BLOCK) {
                        spawns.add(check.clone().add(0.5, 1, 0.5));
                    }
                }
            }
        }

        return spawns;
    }

    public Player getP1() { return p1; }
    public Player getP2() { return p2; }
    public Game getGame() { return game; }
    public Arena getArena() { return arena; }
    public Location getPasteMinLocation() { return pasteMinLocation; }
    public Location getPasteMaxLocation() { return pasteMaxLocation; }
    public void setPasteMinLocation(Location loc) { this.pasteMinLocation = loc; }
    public void setPasteMaxLocation(Location loc) { this.pasteMaxLocation = loc; }
}