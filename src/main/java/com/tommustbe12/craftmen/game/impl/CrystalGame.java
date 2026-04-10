package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class CrystalGame extends Game {

    public CrystalGame() {
        super("Crystal", new ItemStack(Material.END_CRYSTAL));
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
    public Kit createDefaultKit() {
        ItemStack[] contents = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];

        armor[0] = createEnchanted(Material.NETHERITE_HELMET, new Object[][]{
                {"blast_protection",4},
                {"respiration",3},
                {"aqua_affinity",1},
                {"thorns",3},
                {"unbreaking",3},
                {"mending",1}
        });

        armor[1] = createEnchanted(Material.NETHERITE_CHESTPLATE, new Object[][]{
                {"protection",4},
                {"unbreaking",3},
                {"mending",1}
        });

        armor[2] = createEnchanted(Material.NETHERITE_LEGGINGS, new Object[][]{
                {"blast_protection",4},
                {"unbreaking",3},
                {"mending",1}
        });

        armor[3] = createEnchanted(Material.NETHERITE_BOOTS, new Object[][]{
                {"protection",4},
                {"feather_falling",4},
                {"soul_speed",3},
                {"depth_strider",3},
                {"unbreaking",3},
                {"mending",1}
        });

        ItemStack offhand = new ItemStack(Material.TOTEM_OF_UNDYING);

        // ===== POTIONS =====

        ItemStack speed = splashPotion(PotionEffectType.SPEED, 20 * 45, 1);
        ItemStack strength = splashPotion(PotionEffectType.STRENGTH, 20 * 45, 1);

        // ===== HOTBAR =====

        contents[0] = createEnchanted(Material.NETHERITE_SWORD, new Object[][]{
                {"sharpness",5},
                {"sweeping_edge",3},
                {"knockback",1},
                {"unbreaking",3}
        });

        contents[1] = new ItemStack(Material.END_CRYSTAL, 64);
        contents[2] = new ItemStack(Material.OBSIDIAN, 64);
        contents[3] = new ItemStack(Material.RESPAWN_ANCHOR, 64);
        contents[4] = new ItemStack(Material.GLOWSTONE, 64);
        contents[5] = new ItemStack(Material.GOLDEN_APPLE, 64);

        contents[6] = createEnchanted(Material.NETHERITE_PICKAXE, new Object[][]{
                {"efficiency",5},
                {"fortune",3},
                {"unbreaking",3}
        });

        contents[7] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[8] = new ItemStack(Material.ENDER_PEARL, 16);

        // ===== INVENTORY =====

        // Row 1
        contents[9] = createEnchanted(Material.SHIELD, new Object[][]{
                {"unbreaking",3},
                {"mending",1}
        });

        contents[10] = createEnchanted(Material.NETHERITE_AXE, new Object[][]{
                {"sharpness",5},
                {"unbreaking",3}
        });

        ItemStack crossbow = createEnchanted(Material.CROSSBOW, new Object[][]{
                {"multishot",1},
                {"quick_charge",3},
                {"unbreaking",3},
                {"mending",1}
        });

        ItemStack arrow = new ItemStack(Material.TIPPED_ARROW);
        PotionMeta arrowMeta = (PotionMeta) arrow.getItemMeta();
        if (arrowMeta != null) {
            arrowMeta.setBasePotionType(PotionType.SLOW_FALLING);
            arrow.setItemMeta(arrowMeta);
        }

        CrossbowMeta meta = (CrossbowMeta) crossbow.getItemMeta();
        if (meta != null) {
            meta.addChargedProjectile(arrow);
            crossbow.setItemMeta(meta);
        }

        contents[11] = crossbow;

        contents[12] = new ItemStack(Material.TOTEM_OF_UNDYING);
        contents[13] = new ItemStack(Material.TOTEM_OF_UNDYING);

        contents[14] = strength.clone();
        contents[15] = speed.clone();

        contents[16] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[17] = new ItemStack(Material.ENDER_PEARL, 16);

        // Row 2
        contents[18] = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);
        contents[19] = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);
        ItemStack arroww = new ItemStack(Material.TIPPED_ARROW, 63);

        PotionMeta metaw = (PotionMeta) arroww.getItemMeta();
        if (metaw != null) {
            metaw.setBasePotionType(PotionType.SLOW_FALLING);
            arroww.setItemMeta(metaw);
        }

        contents[20] = arroww;
        contents[21] = new ItemStack(Material.TOTEM_OF_UNDYING);
        contents[22] = new ItemStack(Material.TOTEM_OF_UNDYING);
        contents[23] = new ItemStack(Material.COBWEB, 64);
        contents[24] = strength.clone();
        contents[25] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[26] = new ItemStack(Material.ENDER_PEARL, 16);

        // Row 3
        contents[27] = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);
        contents[28] = new ItemStack(Material.END_CRYSTAL, 64);
        contents[29] = new ItemStack(Material.OBSIDIAN, 64);
        contents[30] = new ItemStack(Material.TOTEM_OF_UNDYING);
        contents[31] = new ItemStack(Material.TOTEM_OF_UNDYING);
        contents[32] = new ItemStack(Material.GOLDEN_APPLE, 64);
        contents[33] = speed.clone();
        contents[34] = speed.clone();
        contents[35] = new ItemStack(Material.ENDER_PEARL, 16);

        return new Kit(contents, armor, offhand);
    }

    @Override
    public void onStart(Match match) {}

    @Override
    public void onEnd(Match match) {}
}
