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

public class PotionGame extends Game {

    public PotionGame() {
        super("Netherite Potion", new ItemStack(Material.NETHERITE_HELMET));
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
        inv.setHelmet(enchItem(Material.NETHERITE_HELMET, new Object[][]{
                {"protection",4},{"aqua_affinity",1},{"unbreaking",3},{"mending",1}
        }));
        inv.setChestplate(enchItem(Material.NETHERITE_CHESTPLATE, new Object[][]{
                {"protection",4},{"unbreaking",3},{"mending",1}
        }));
        inv.setLeggings(enchItem(Material.NETHERITE_LEGGINGS, new Object[][]{
                {"protection",4},{"swift_sneak",3},{"unbreaking",3},{"mending",1}
        }));
        inv.setBoots(enchItem(Material.NETHERITE_BOOTS, new Object[][]{
                {"protection",4},{"feather_falling",4},{"depth_strider",3},{"unbreaking",3},{"mending",1}
        }));

        inv.setItemInOffHand(new ItemStack(Material.GOLDEN_APPLE, 64));

        // ===== POTIONS =====
        ItemStack heal = instantSplash(PotionEffectType.INSTANT_HEALTH, 1);
        ItemStack speed = splash(PotionEffectType.SPEED, 90, 1);
        ItemStack strength = splash(PotionEffectType.STRENGTH, 90, 1);

        // ===== HOTBAR =====
        inv.setItem(0, enchItem(Material.NETHERITE_SWORD, new Object[][]{
                {"sharpness",5},{"unbreaking",3},{"mending",1}
        }));

        for(int i=1;i<=5;i++)
            inv.setItem(i, heal.clone());

        inv.setItem(6, strength.clone());
        inv.setItem(7, speed.clone());
        inv.setItem(8, new ItemStack(Material.TOTEM_OF_UNDYING));

        // ===== INVENTORY (+8 offset) =====

        // 1-9 healing
        for(int i=1;i<=9;i++)
            inv.setItem(8 + i, heal.clone());

        // 10-13 speed
        for(int i=10;i<=13;i++)
            inv.setItem(8 + i, speed.clone());

        // 14-17 healing
        for(int i=14;i<=17;i++)
            inv.setItem(8 + i, heal.clone());

        // 18 XP bottles
        inv.setItem(8 + 18, new ItemStack(Material.EXPERIENCE_BOTTLE,64));

        // 19-22 strength
        for(int i=19;i<=22;i++)
            inv.setItem(8 + i, strength.clone());

        // 23-26 healing
        for(int i=23;i<=26;i++)
            inv.setItem(8 + i, heal.clone());

        // 27 totem
        inv.setItem(8 + 27, new ItemStack(Material.TOTEM_OF_UNDYING));
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}