package com.tommustbe12.craftmen.endfight;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class EndFightManager {

    private final JavaPlugin plugin;

    private final List<Player> players = new ArrayList<>();

    private final Map<UUID, Integer> kitIndex = new HashMap<>();

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
        kitIndex.put(player.getUniqueId(), 0);

        if (!running) {
            player.sendMessage("§6Loading up End Fight...");
            startGame(player);
        } else {
            player.sendMessage("§aJoined End Fight! Waiting for others...");
            if (spawn != null && world != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        teleportToSpawn(player);
                        giveKit(player);
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

        new BukkitRunnable() {
            @Override
            public void run() {
                world = Bukkit.createWorld(wc);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        spawn = new Location(world, 100.5, 50, 0.5);

                        for (int x = -2; x <= 2; x++) {
                            for (int z = -2; z <= 2; z++) {
                                world.getBlockAt(100 + x, 49, z).setType(Material.OBSIDIAN);
                            }
                        }

                        for (Player p : players) {
                            teleportToSpawn(p);
                            giveKit(p);
                        }

                        Bukkit.broadcastMessage("§5End Fight has started!");
                    }
                }.runTask(plugin);
            }
        }.runTask(plugin);
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
        kitIndex.clear();

        if (world != null) {
            String name = world.getName();
            Bukkit.unloadWorld(world, false);

            new BukkitRunnable() {
                @Override
                public void run() {
                    File folder = new File(Bukkit.getWorldContainer(), name);
                    if (name.startsWith("endfight_")) {
                        deleteWorld(folder);
                    }
                }
            }.runTaskLater(plugin, 100L);
        }

        running = false;
        eggPhase = false;
        world = null;
        spawn = null;
    }

    public void onPlayerDeath(Player player) {
        if (!isInGame(player)) return;
        UUID id = player.getUniqueId();
        int current = kitIndex.getOrDefault(id, 0);
        // Advance kit: 0->1 (Netherite->Diamond), 1->2 (Diamond->Iron), 2+ stays Iron
        kitIndex.put(id, Math.min(current + 1, 2));
    }

    public void onPlayerRespawn(Player player) {
        if (!isInGame(player)) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                teleportToSpawn(player);
                giveKit(player);
                String kitName = getKitName(player);
                player.sendMessage("§eYou respawned with the §6" + kitName + " §ekit.");
            }
        }.runTaskLater(plugin, 1L);
    }

    private String getKitName(Player player) {
        int idx = kitIndex.getOrDefault(player.getUniqueId(), 0);
        if (idx == 0) return "Netherite";
        if (idx == 1) return "Diamond";
        return "Iron";
    }

    public void giveKit(Player player) {
        int idx = kitIndex.getOrDefault(player.getUniqueId(), 0);
        player.getInventory().clear();
        if (idx == 0) {
            giveNetheriteKit(player);
        } else if (idx == 1) {
            giveDiamondKit(player);
        } else {
            giveIronKit(player);
        }
    }

    private void giveNetheriteKit(Player player) {
        ItemStack helmet = enchant(new ItemStack(Material.NETHERITE_HELMET),
                Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3,
                        Enchantment.MENDING, 1, Enchantment.AQUA_AFFINITY, 1));

        ItemStack chestplate = enchant(new ItemStack(Material.NETHERITE_CHESTPLATE),
                Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3,
                        Enchantment.MENDING, 1));

        ItemStack leggings = enchant(new ItemStack(Material.NETHERITE_LEGGINGS),
                Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3,
                        Enchantment.MENDING, 1, Enchantment.SWIFT_SNEAK, 3));

        ItemStack boots = enchant(new ItemStack(Material.NETHERITE_BOOTS),
                Map.of(Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 4,
                        Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1,
                        Enchantment.DEPTH_STRIDER, 3));

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        player.getInventory().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));

        player.getInventory().setItem(9, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));

        ItemStack fireRes = makeSplashPotion(Material.POTION, PotionEffectType.FIRE_RESISTANCE, 9600, 0);
        for (int i = 10; i <= 12; i++) player.getInventory().setItem(i, fireRes.clone());

        ItemStack strength = makeSplashPotion(Material.POTION, PotionEffectType.STRENGTH, 1800, 1);
        for (int i = 13; i <= 17; i++) player.getInventory().setItem(i, strength.clone());

        player.getInventory().setItem(18, new ItemStack(Material.OAK_LOG, 64));

        ItemStack speed = makeSplashPotion(Material.POTION, PotionEffectType.SPEED, 9600, 0);
        for (int i = 19; i <= 21; i++) player.getInventory().setItem(i, speed.clone());

        for (int i = 22; i <= 26; i++) player.getInventory().setItem(i, strength.clone());

        player.getInventory().setItem(27, new ItemStack(Material.COBWEB, 64));

        ItemStack pick = enchant(new ItemStack(Material.NETHERITE_PICKAXE),
                Map.of(Enchantment.SILK_TOUCH, 1, Enchantment.EFFICIENCY, 5,
                        Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        player.getInventory().setItem(28, pick);

        ItemStack bow = enchant(new ItemStack(Material.BOW),
                Map.of(Enchantment.POWER, 5, Enchantment.UNBREAKING, 3,
                        Enchantment.INFINITY, 1));
        player.getInventory().setItem(29, bow);

        player.getInventory().setItem(30, new ItemStack(Material.GOLDEN_APPLE, 64));

        player.getInventory().setItem(31, new ItemStack(Material.WATER_BUCKET));

        player.getInventory().setItem(32, new ItemStack(Material.ARROW, 1));

        for (int i = 33; i <= 35; i++) player.getInventory().setItem(i, strength.clone());

        ItemStack sword = enchant(new ItemStack(Material.NETHERITE_SWORD),
                Map.of(Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2,
                        Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        player.getInventory().setItem(0, sword);

        ItemStack axe = enchant(new ItemStack(Material.NETHERITE_AXE),
                Map.of(Enchantment.SHARPNESS, 5, Enchantment.EFFICIENCY, 5,
                        Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        player.getInventory().setItem(1, axe);

        player.getInventory().setItem(2, new ItemStack(Material.ENDER_PEARL, 16));

        player.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 64));

        player.getInventory().setItem(4, new ItemStack(Material.WATER_BUCKET));

        ItemStack spear = enchant(new ItemStack(Material.TRIDENT),
                Map.of(Enchantment.RIPTIDE, 3, Enchantment.SHARPNESS, 5,
                        Enchantment.UNBREAKING, 3));
        player.getInventory().setItem(5, spear);

        player.getInventory().setItem(6, new ItemStack(Material.WIND_CHARGE, 64));

        player.getInventory().setItem(7, new ItemStack(Material.MACE));

        ItemStack shield = enchant(new ItemStack(Material.SHIELD),
                Map.of(Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        player.getInventory().setItem(8, shield);
    }

    private void giveDiamondKit(Player player) {
        ItemStack helmet = enchant(new ItemStack(Material.DIAMOND_HELMET),
                Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3,
                        Enchantment.MENDING, 1, Enchantment.AQUA_AFFINITY, 1));

        ItemStack chestplate = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE),
                Map.of(Enchantment.PROTECTION, 4, Enchantment.MENDING, 1,
                        Enchantment.UNBREAKING, 3));

        ItemStack leggings = enchant(new ItemStack(Material.DIAMOND_LEGGINGS),
                Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3,
                        Enchantment.MENDING, 1, Enchantment.SWIFT_SNEAK, 3));

        ItemStack boots = enchant(new ItemStack(Material.DIAMOND_BOOTS),
                Map.of(Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 4,
                        Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1,
                        Enchantment.DEPTH_STRIDER, 3));

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        ItemStack offShield = enchant(new ItemStack(Material.SHIELD),
                Map.of(Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        player.getInventory().setItemInOffHand(offShield);

        ItemStack speed = makeSplashPotion(Material.POTION, PotionEffectType.SPEED, 9600, 0);
        ItemStack strength = makeSplashPotion(Material.POTION, PotionEffectType.STRENGTH, 1800, 1);
        ItemStack fireRes = makeSplashPotion(Material.POTION, PotionEffectType.FIRE_RESISTANCE, 9600, 0);

        for (int i = 9; i <= 11; i++) player.getInventory().setItem(i, speed.clone());

        for (int i = 12; i <= 17; i++) player.getInventory().setItem(i, strength.clone());

        for (int i = 18; i <= 20; i++) player.getInventory().setItem(i, fireRes.clone());

        for (int i = 21; i <= 26; i++) player.getInventory().setItem(i, strength.clone());

        player.getInventory().setItem(27, new ItemStack(Material.EXPERIENCE_BOTTLE, 32));

        player.getInventory().setItem(28, new ItemStack(Material.ARROW, 32));

        ItemStack bow = enchant(new ItemStack(Material.BOW),
                Map.of(Enchantment.PUNCH, 1, Enchantment.UNBREAKING, 3));
        player.getInventory().setItem(29, bow);

        player.getInventory().setItem(30, new ItemStack(Material.GOLDEN_CARROT, 64));

        player.getInventory().setItem(31, new ItemStack(Material.WATER_BUCKET));

        player.getInventory().setItem(32, new ItemStack(Material.SPRUCE_LOG, 64));

        for (int i = 33; i <= 35; i++) player.getInventory().setItem(i, strength.clone());

        ItemStack sword = enchant(new ItemStack(Material.DIAMOND_SWORD),
                Map.of(Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2,
                        Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        player.getInventory().setItem(0, sword);

        ItemStack axe = enchant(new ItemStack(Material.DIAMOND_AXE),
                Map.of(Enchantment.SHARPNESS, 5, Enchantment.EFFICIENCY, 5,
                        Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        player.getInventory().setItem(1, axe);

        player.getInventory().setItem(2, new ItemStack(Material.ENDER_PEARL, 8));

        player.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 32));

        player.getInventory().setItem(4, new ItemStack(Material.WATER_BUCKET));

        ItemStack pick = enchant(new ItemStack(Material.DIAMOND_PICKAXE),
                Map.of(Enchantment.EFFICIENCY, 3, Enchantment.UNBREAKING, 3));
        player.getInventory().setItem(5, pick);

        player.getInventory().setItem(6, new ItemStack(Material.WIND_CHARGE, 32));

        player.getInventory().setItem(7, new ItemStack(Material.COBWEB, 32));

        ItemStack spear = enchant(new ItemStack(Material.TRIDENT),
                Map.of(Enchantment.RIPTIDE, 2, Enchantment.SHARPNESS, 5,
                        Enchantment.UNBREAKING, 3));
        player.getInventory().setItem(8, spear);
    }

    private void giveIronKit(Player player) {
        ItemStack helmet = enchant(new ItemStack(Material.IRON_HELMET),
                Map.of(Enchantment.PROTECTION, 4));
        ItemStack chestplate = enchant(new ItemStack(Material.IRON_CHESTPLATE),
                Map.of(Enchantment.PROTECTION, 4));
        ItemStack leggings = enchant(new ItemStack(Material.IRON_LEGGINGS),
                Map.of(Enchantment.PROTECTION, 4));
        ItemStack boots = enchant(new ItemStack(Material.IRON_BOOTS),
                Map.of(Enchantment.PROTECTION, 4));

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));

        ItemStack sword = enchant(new ItemStack(Material.IRON_SWORD),
                Map.of(Enchantment.SHARPNESS, 5));
        player.getInventory().setItem(0, sword);

        ItemStack axe = enchant(new ItemStack(Material.IRON_AXE),
                Map.of(Enchantment.SHARPNESS, 5, Enchantment.EFFICIENCY, 5));
        player.getInventory().setItem(1, axe);

        ItemStack pick = enchant(new ItemStack(Material.IRON_PICKAXE),
                Map.of(Enchantment.EFFICIENCY, 5));
        player.getInventory().setItem(2, pick);

        player.getInventory().setItem(3, new ItemStack(Material.BOW));

        player.getInventory().setItem(4, new ItemStack(Material.ARROW, 16));

        player.getInventory().setItem(5, new ItemStack(Material.WHITE_BED, 6));

        player.getInventory().setItem(6, new ItemStack(Material.OBSIDIAN, 4));

        player.getInventory().setItem(7, new ItemStack(Material.BREAD, 16));

        player.getInventory().setItem(8, new ItemStack(Material.SPRUCE_LOG, 64));
    }

    private ItemStack enchant(ItemStack item, Map<Enchantment, Integer> enchantments) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeSplashPotion(Material material, PotionEffectType type, int durationTicks, int amplifier) {
        ItemStack potion = new ItemStack(material);
        org.bukkit.inventory.meta.PotionMeta meta =
                (org.bukkit.inventory.meta.PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.addCustomEffect(new PotionEffect(type, durationTicks, amplifier), true);
            potion.setItemMeta(meta);
        }
        return potion;
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
        kitIndex.remove(player.getUniqueId());

        if (players.isEmpty() && running) {
            Bukkit.broadcastMessage("§cEnd Fight ended because everyone left.");
            endGame();
        }
    }

    private void teleportToSpawn(Player player) {
        if (spawn == null) return;
        player.teleport(spawn.clone().add(
                new Random().nextInt(5) - 2,
                0,
                new Random().nextInt(5) - 2
        ));
    }
}