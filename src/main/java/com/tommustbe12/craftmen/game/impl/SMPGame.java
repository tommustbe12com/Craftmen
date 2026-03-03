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

public class SMPGame extends Game {

    public SMPGame() {
        super("SMP", Material.NETHERITE_SWORD);
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

    private ItemStack splashPotion(PotionEffectType type, int durationTicks, int amplifier) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, durationTicks, amplifier), true);
        potion.setItemMeta(meta);
        return potion;
    }

    @Override
    public void applyLoadout(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();

        // ===== ARMOR =====
        inv.setHelmet(createEnchanted(Material.NETHERITE_HELMET, new Object[][]{
                {"protection", 4},
                {"aqua_affinity", 1},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        inv.setChestplate(createEnchanted(Material.NETHERITE_CHESTPLATE, new Object[][]{
                {"protection", 4},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        inv.setLeggings(createEnchanted(Material.NETHERITE_LEGGINGS, new Object[][]{
                {"protection", 4},
                {"swift_sneak", 3},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        inv.setBoots(createEnchanted(Material.NETHERITE_BOOTS, new Object[][]{
                {"protection", 4},
                {"feather_falling", 4},
                {"depth_strider", 3},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        // Offhand
        inv.setItemInOffHand(createEnchanted(Material.SHIELD, new Object[][]{
                {"unbreaking", 3},
                {"mending", 1}
        }));

        // ===== POTIONS =====
        ItemStack str = splashPotion(PotionEffectType.STRENGTH, 20 * 90, 1); // 1:30 Strength II
        ItemStack speed = splashPotion(PotionEffectType.SPEED, 20 * 90, 1);   // 1:30 Speed II
        ItemStack fire = splashPotion(PotionEffectType.FIRE_RESISTANCE, 20 * 480, 0); // 8:00 Fire Res

        // ===== HOTBAR =====
        inv.setItem(0, createEnchanted(Material.NETHERITE_SWORD, new Object[][]{
                {"sharpness", 5},
                {"fire_aspect", 2},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        inv.setItem(1, new ItemStack(Material.GOLDEN_APPLE, 64));
        inv.setItem(2, new ItemStack(Material.ENDER_PEARL, 16));

        inv.setItem(3, createEnchanted(Material.NETHERITE_AXE, new Object[][]{
                {"sharpness", 5},
                {"efficiency", 5},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        inv.setItem(4, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        inv.setItem(5, str.clone());
        inv.setItem(6, speed.clone());
        inv.setItem(7, fire.clone());
        inv.setItem(8, new ItemStack(Material.TOTEM_OF_UNDYING));

        // ===== INVENTORY (slot +8 offset) =====

        // Slots 1-9 Strength
        for (int i = 1; i <= 9; i++) {
            inv.setItem(8 + i, str.clone());
        }

        // Slots 10-18 Speed
        for (int i = 10; i <= 18; i++) {
            inv.setItem(8 + i, speed.clone());
        }

        // Slot 19 Sword
        inv.setItem(8 + 19, createEnchanted(Material.NETHERITE_SWORD, new Object[][]{
                {"sharpness", 5},
                {"fire_aspect", 2},
                {"knockback", 1},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        // Slot 20 Gapples
        inv.setItem(8 + 20, new ItemStack(Material.GOLDEN_APPLE, 64));

        // Slot 21 Pearls
        inv.setItem(8 + 21, new ItemStack(Material.ENDER_PEARL, 16));

        // 22-23 Strength
        inv.setItem(8 + 22, str.clone());
        inv.setItem(8 + 23, str.clone());

        // 24-25 Speed
        inv.setItem(8 + 24, speed.clone());
        inv.setItem(8 + 25, speed.clone());

        // 26-27 Fire Res
        inv.setItem(8 + 26, fire.clone());
        inv.setItem(8 + 27, fire.clone());
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}