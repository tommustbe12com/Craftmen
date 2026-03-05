package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CrystalGame extends Game {

    public CrystalGame() {
        super("Crystal", Material.END_CRYSTAL);
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    private ItemStack createEnchanted(Material material, Object[][] enchants) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        for (Object[] e : enchants) {
            meta.addEnchant(ench((String) e[0]), (int) e[1], true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack splashPotion(PotionEffectType type, int duration, int amplifier) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
        potion.setItemMeta(meta);
        return potion;
    }

    @Override
    public void applyLoadout(Player player) {

        PlayerInventory inv = player.getInventory();
        inv.clear();

        // ===== ARMOR =====

        inv.setHelmet(createEnchanted(Material.NETHERITE_HELMET, new Object[][]{
                {"blast_protection",4},
                {"respiration",3},
                {"aqua_affinity",1},
                {"thorns",3},
                {"unbreaking",3},
                {"mending",1}
        }));

        inv.setChestplate(createEnchanted(Material.NETHERITE_CHESTPLATE, new Object[][]{
                {"protection",4},
                {"unbreaking",3},
                {"mending",1}
        }));

        inv.setLeggings(createEnchanted(Material.NETHERITE_LEGGINGS, new Object[][]{
                {"blast_protection",4},
                {"unbreaking",3},
                {"mending",1}
        }));

        inv.setBoots(createEnchanted(Material.NETHERITE_BOOTS, new Object[][]{
                {"protection",4},
                {"feather_falling",4},
                {"soul_speed",3},
                {"depth_strider",3},
                {"unbreaking",3},
                {"mending",1}
        }));

        // ===== OFFHAND =====

        inv.setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));

        // ===== POTIONS =====

        ItemStack speed = splashPotion(PotionEffectType.SPEED, 20 * 45, 1);
        ItemStack strength = splashPotion(PotionEffectType.STRENGTH, 20 * 45, 1);

        // ===== HOTBAR =====

        inv.setItem(0, createEnchanted(Material.NETHERITE_SWORD, new Object[][]{
                {"sharpness",5},
                {"sweeping_edge",3},
                {"knockback",1},
                {"unbreaking",3}
        }));

        inv.setItem(1, new ItemStack(Material.END_CRYSTAL, 64));
        inv.setItem(2, new ItemStack(Material.OBSIDIAN, 64));
        inv.setItem(3, new ItemStack(Material.RESPAWN_ANCHOR, 64));
        inv.setItem(4, new ItemStack(Material.GLOWSTONE, 64));
        inv.setItem(5, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 16));

        inv.setItem(6, createEnchanted(Material.NETHERITE_PICKAXE, new Object[][]{
                {"efficiency",5},
                {"fortune",3},
                {"unbreaking",3}
        }));

        inv.setItem(7, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        inv.setItem(8, new ItemStack(Material.ENDER_PEARL, 16));

        // ===== INVENTORY =====

        // Row 1
        inv.setItem(9, createEnchanted(Material.SHIELD, new Object[][]{
                {"unbreaking",3},
                {"mending",1}
        }));

        inv.setItem(10, createEnchanted(Material.NETHERITE_AXE, new Object[][]{
                {"sharpness",5},
                {"unbreaking",3}
        }));

        inv.setItem(11, createEnchanted(Material.CROSSBOW, new Object[][]{
                {"multishot",1},
                {"quick_charge",3},
                {"unbreaking",3},
                {"mending",1}
        }));

        inv.setItem(12, new ItemStack(Material.TOTEM_OF_UNDYING));
        inv.setItem(13, new ItemStack(Material.TOTEM_OF_UNDYING));

        inv.setItem(14, strength.clone());
        inv.setItem(15, speed.clone());

        inv.setItem(16, new ItemStack(Material.ENDER_PEARL, 16));
        inv.setItem(17, new ItemStack(Material.ENDER_PEARL, 16));

        // Row 2
        inv.setItem(18, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        inv.setItem(19, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        inv.setItem(20, new ItemStack(Material.ARROW, 64));
        inv.setItem(21, new ItemStack(Material.TOTEM_OF_UNDYING));
        inv.setItem(22, new ItemStack(Material.TOTEM_OF_UNDYING));
        inv.setItem(23, new ItemStack(Material.COBWEB, 64));
        inv.setItem(24, strength.clone());
        inv.setItem(25, new ItemStack(Material.ENDER_PEARL, 16));
        inv.setItem(26, new ItemStack(Material.ENDER_PEARL, 16));

        // Row 3
        inv.setItem(27, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        inv.setItem(28, new ItemStack(Material.END_CRYSTAL, 64));
        inv.setItem(29, new ItemStack(Material.OBSIDIAN, 64));
        inv.setItem(30, new ItemStack(Material.TOTEM_OF_UNDYING));
        inv.setItem(31, new ItemStack(Material.TOTEM_OF_UNDYING));
        inv.setItem(32, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 16));
        inv.setItem(33, speed.clone());
        inv.setItem(34, speed.clone());
        inv.setItem(35, new ItemStack(Material.ENDER_PEARL, 16));
    }

    @Override
    public void onStart(Match match) {}

    @Override
    public void onEnd(Match match) {}
}