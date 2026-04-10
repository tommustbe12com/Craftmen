package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DSMPGame extends Game {

    public DSMPGame() {
        super("Diamond SMP", new ItemStack(Material.CHORUS_FRUIT));
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    private ItemStack enchItem(Material mat, Object[][] enchants) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        for (Object[] e : enchants) {
            meta.addEnchant(ench((String) e[0]), (int) e[1], true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack splash(PotionEffectType type, int seconds, int amplifier) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, 20 * seconds, amplifier), true);
        potion.setItemMeta(meta);
        return potion;
    }

    @Override
    public Kit createDefaultKit() {
        ItemStack[] contents = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];

        armor[0] = enchItem(Material.DIAMOND_HELMET, new Object[][]{
                {"protection",4},{"aqua_affinity",1},{"unbreaking",3},{"mending",1}
        });
        armor[1] = enchItem(Material.DIAMOND_CHESTPLATE, new Object[][]{
                {"protection",4},{"unbreaking",3},{"mending",1}
        });
        armor[2] = enchItem(Material.DIAMOND_LEGGINGS, new Object[][]{
                {"protection",4},{"swift_sneak",3},{"unbreaking",3},{"mending",1}
        });
        armor[3] = enchItem(Material.DIAMOND_BOOTS, new Object[][]{
                {"protection",4},{"feather_falling",4},{"depth_strider",3},{"unbreaking",3},{"mending",1}
        });

        ItemStack offhand = enchItem(Material.SHIELD, new Object[][]{
                {"unbreaking",3},{"mending",1}
        });

        // ===== POTIONS =====
        ItemStack strength = splash(PotionEffectType.STRENGTH, 90, 1);
        ItemStack speed = splash(PotionEffectType.SPEED, 480, 0);
        ItemStack fire = splash(PotionEffectType.FIRE_RESISTANCE, 480, 0);

        // ===== HOTBAR =====
        contents[0] = enchItem(Material.DIAMOND_SWORD, new Object[][]{
                {"sharpness",5},{"fire_aspect",2},{"unbreaking",3},{"mending",1}
        });
        contents[1] = enchItem(Material.DIAMOND_AXE, new Object[][]{
                {"sharpness",5},{"efficiency",5},{"unbreaking",3},{"mending",1}
        });
        contents[2] = new ItemStack(Material.ENDER_PEARL,16);
        contents[3] = new ItemStack(Material.GOLDEN_APPLE,64);
        contents[4] = new ItemStack(Material.WATER_BUCKET);
        contents[5] = strength.clone();
        contents[6] = strength.clone();
        contents[7] = new ItemStack(Material.COBWEB,64);
        contents[8] = new ItemStack(Material.TOTEM_OF_UNDYING);

        // ===== INVENTORY (+8 offset) =====

        // 1-9 strength
        for(int i=1;i<=9;i++)
            contents[8 + i] = strength.clone();

        // 10 spruce logs
        contents[8 + 10] = new ItemStack(Material.SPRUCE_LOG,64);

        // 11 water bucket
        contents[8 + 11] = new ItemStack(Material.WATER_BUCKET);

        // 12-16 strength
        for(int i=12;i<=16;i++)
            contents[8 + i] = strength.clone();

        // 17 XP bottles
        contents[8 + 17] = new ItemStack(Material.EXPERIENCE_BOTTLE,64);

        // 18 chorus fruit
        contents[8 + 18] = new ItemStack(Material.CHORUS_FRUIT,64);

        // 19 gapples
        contents[8 + 19] = new ItemStack(Material.GOLDEN_APPLE,64);

        // 20 water
        contents[8 + 20] = new ItemStack(Material.WATER_BUCKET);

        // 21 strength
        contents[8 + 21] = strength.clone();

        // 22-24 speed
        for(int i=22;i<=24;i++)
            contents[8 + i] = speed.clone();

        // 25-27 fire res
        for(int i=25;i<=27;i++)
            contents[8 + i] = fire.clone();

        return new Kit(contents, armor, offhand);
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}
