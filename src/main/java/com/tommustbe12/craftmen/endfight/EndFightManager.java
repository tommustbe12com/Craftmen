package com.tommustbe12.craftmen.endfight;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class EndFightManager {

    private final JavaPlugin plugin;

    private final List<Player> players = new ArrayList<>();
    private boolean running = false;
    private boolean eggPhase = false;

    private World world;
    private Location spawn;

    private int worldId = 0;

    public EndFightManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void join(Player player) {

        if (players.contains(player)) {
            player.sendMessage("§cYou are already in End Fight.");
            return;
        }

        players.add(player);

        if (!running) {
            player.sendMessage("§6Loading up End Fight...");
            startGame(player);
        } else {
            player.sendMessage("§aJoined End Fight! Waiting for others...");
            // teleport to spawn after world is ready
            if (spawn != null && world != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        teleportToSpawn(player);
                        giveStartItems(player);
                    }
                }.runTaskLater(plugin, 20L);
            }
        }
    }

    private void startGame(Player firstPlayer) {

        running = true;
        eggPhase = false;
        worldId++;

        String worldName = "endfight_" + worldId;

        WorldCreator wc = new WorldCreator(worldName);
        wc.environment(World.Environment.THE_END);
        wc.type(WorldType.NORMAL);

        // Generate world asynchronously to avoid server freeze
        new BukkitRunnable() {
            @Override
            public void run() {
                world = Bukkit.createWorld(wc);

                // Wait a tick to ensure world is fully loaded
                new BukkitRunnable() {
                    @Override
                    public void run() {

                        // Teleport all current players
                        for (Player p : players) {
                            // Set the spawn dynamically to the normal End spawn location
                            spawn = world.getSpawnLocation();
                            teleportToSpawn(p);
                            giveStartItems(p);
                        }

                        Bukkit.broadcastMessage("§5End Fight has started!");

                    }
                }.runTask(plugin);
            }
        }.runTask(plugin); // BukkitRunnable with plugin ensures safe sync
    }

    public void dragonKilled() {
        eggPhase = true;
        Bukkit.broadcastMessage("§dThe Ender Dragon has been defeated! Grab the egg!");
    }

    public void win(Player player) {
        Bukkit.broadcastMessage("§6" + player.getName() + " has escaped with the Dragon Egg!");
        endGame();
    }

    public void endGame() {

        for (Player p : new ArrayList<>(players)) {
            if (p.isOnline()) {
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        }

        players.clear();

        if (world != null) {
            String name = world.getName();
            Bukkit.unloadWorld(world, false);

            File folder = new File(name);
            deleteWorld(folder);
        }

        running = false;
        eggPhase = false;
        world = null;
        spawn = null;
    }

    private void deleteWorld(File file) {
        if (file.isDirectory()) {
            for (File f : Objects.requireNonNull(file.listFiles())) {
                deleteWorld(f);
            }
        }
        file.delete();
    }

    public boolean isInGame(Player player) {
        return players.contains(player);
    }

    public boolean isEggPhase() {
        return eggPhase;
    }

    public Location getSpawn() {
        return spawn;
    }

    public void removePlayer(Player player) {
        players.remove(player);

        if (players.isEmpty() && running) {
            Bukkit.broadcastMessage("§cEnd Fight ended because everyone left.");
            endGame();
        }
    }

    private void teleportToSpawn(Player player) {
        if (spawn == null) return;

        // Randomize spawn slightly around center to prevent stacking
        player.teleport(spawn.clone().add(
                new Random().nextInt(5) - 2,
                0,
                new Random().nextInt(5) - 2
        ));
    }

    public void giveStartItems(Player player) {
        player.getInventory().clear();

        // Armor
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));

        // Weapons & Tools
        player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        player.getInventory().setItem(1, new ItemStack(Material.BOW));
        player.getInventory().setItem(2, new ItemStack(Material.IRON_PICKAXE));

        // Blocks for building
        player.getInventory().setItem(3, new ItemStack(Material.OAK_PLANKS, 64));
        player.getInventory().setItem(4, new ItemStack(Material.OAK_PLANKS, 64));

        // Beds for dragon damage
        player.getInventory().setItem(5, new ItemStack(Material.WHITE_BED, 6));

        // Utility
        player.getInventory().setItem(6, new ItemStack(Material.WATER_BUCKET));
        player.getInventory().setItem(7, new ItemStack(Material.COOKED_BEEF, 32));
        player.getInventory().setItem(8, new ItemStack(Material.TORCH, 16));

        // Extra items in inventory
        player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 32));
    }
}