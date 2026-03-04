package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DPotionGame extends Game {

    public DPotionGame() {
        super("Diamond Potion", Material.SPLASH_POTION);
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
    public void applyLoadout(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();

        // ===== ARMOR =====
        inv.setHelmet(enchItem(Material.DIAMOND_HELMET, new Object[][]{
                {"protection",4},{"aqua_affinity",1},{"unbreaking",3},{"mending",1}
        }));
        inv.setChestplate(enchItem(Material.DIAMOND_CHESTPLATE, new Object[][]{
                {"protection",4},{"unbreaking",3},{"mending",1}
        }));
        inv.setLeggings(enchItem(Material.DIAMOND_LEGGINGS, new Object[][]{
                {"protection",4},{"swift_sneak",3},{"unbreaking",3},{"mending",1}
        }));
        inv.setBoots(enchItem(Material.DIAMOND_BOOTS, new Object[][]{
                {"protection",4},{"feather_falling",4},{"depth_strider",3},{"unbreaking",3},{"mending",1}
        }));

        inv.setItemInOffHand(new ItemStack(Material.COOKED_BEEF, 5));

        // ===== POTIONS =====
        ItemStack heal = instantSplash(PotionEffectType.INSTANT_HEALTH, 1); // Health II
        ItemStack strength = splash(PotionEffectType.STRENGTH, 90, 1);
        ItemStack speed = splash(PotionEffectType.SPEED, 90, 1);
        ItemStack regen = splash(PotionEffectType.REGENERATION, 90, 0);

        // ===== HOTBAR =====
        inv.setItem(0, enchItem(Material.DIAMOND_SWORD, new Object[][]{
                {"sharpness",5},{"unbreaking",3},{"mending",1}
        }));

        for (int i = 1; i <= 8; i++)
            inv.setItem(i, regen.clone());

        // ===== INVENTORY (+8 offset) =====

        // 1-6 healing
        for(int i=1;i<=6;i++)
            inv.setItem(8 + i, heal.clone());

        // 7 strength
        inv.setItem(8 + 7, strength.clone());

        // 8 speed
        inv.setItem(8 + 8, speed.clone());

        // 9 regen
        inv.setItem(8 + 9, regen.clone());

        // 10-15 healing
        for(int i=10;i<=15;i++)
            inv.setItem(8 + i, heal.clone());

        // 16 strength
        inv.setItem(8 + 16, strength.clone());

        // 17 speed
        inv.setItem(8 + 17, speed.clone());

        // 18 regen
        inv.setItem(8 + 18, regen.clone());

        // 19-24 healing
        for(int i=19;i<=24;i++)
            inv.setItem(8 + i, heal.clone());

        // 25 strength
        inv.setItem(8 + 25, strength.clone());

        // 26 speed
        inv.setItem(8 + 26, speed.clone());

        // 27 regen
        inv.setItem(8 + 27, regen.clone());
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}