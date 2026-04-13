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
import org.bukkit.potion.PotionType;

public class DPotionGame extends Game {

    public DPotionGame() {
        super("Diamond Potion", createInstantHealthPotion());
    }

    private static ItemStack createInstantHealthPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);

        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

        if (potionMeta != null) {
            potionMeta.setBasePotionType(PotionType.HEALING);

            potion.setItemMeta(potionMeta);
        }

        return potion;
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

    private ItemStack instantSplash(PotionEffectType type, int amplifier) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, 1, amplifier), true);
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

        ItemStack offhand = new ItemStack(Material.COOKED_BEEF, 5);

        // ===== POTIONS =====
        ItemStack heal = instantSplash(PotionEffectType.INSTANT_HEALTH, 1); // Health II
        ItemStack strength = splash(PotionEffectType.STRENGTH, 90, 1);
        ItemStack speed = splash(PotionEffectType.SPEED, 90, 1);
        ItemStack regen = splash(PotionEffectType.REGENERATION, 90, 0);

        // ===== HOTBAR =====
        contents[0] = enchItem(Material.DIAMOND_SWORD, new Object[][]{
                {"sharpness",5},{"unbreaking",3},{"mending",1}
        });

        for (int i = 1; i <= 8; i++)
            contents[i] = heal.clone();

        // ===== INVENTORY (+8 offset) =====

        // 1-6 healing
        for(int i=1;i<=6;i++)
            contents[8 + i] = heal.clone();

        // 7 strength
        contents[8 + 7] = strength.clone();

        // 8 speed
        contents[8 + 8] = speed.clone();

        // 9 regen
        contents[8 + 9] = regen.clone();

        // 10-15 healing
        for(int i=10;i<=15;i++)
            contents[8 + i] = heal.clone();

        // 16 strength
        contents[8 + 16] = strength.clone();

        // 17 speed
        contents[8 + 17] = speed.clone();

        // 18 regen
        contents[8 + 18] = regen.clone();

        // 19-24 healing
        for(int i=19;i<=24;i++)
            contents[8 + i] = heal.clone();

        // 25 strength
        contents[8 + 25] = strength.clone();

        // 26 speed
        contents[8 + 26] = speed.clone();

        // 27 regen
        contents[8 + 27] = regen.clone();

        return new Kit(contents, armor, offhand);
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}
