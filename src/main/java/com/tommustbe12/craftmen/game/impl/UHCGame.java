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
import org.bukkit.enchantments.Enchantment;

public class UHCGame extends Game {

    public UHCGame() {
        super("UHC", Material.GOLDEN_APPLE);
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    private ItemStack createItem(Material material) {
        return new ItemStack(material);
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

    @Override
    public void applyLoadout(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();

        // ===== ARMOR =====
        inv.setHelmet(createEnchanted(Material.DIAMOND_HELMET, new Object[][]{
                {"protection", 3},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        inv.setChestplate(createEnchanted(Material.DIAMOND_CHESTPLATE, new Object[][]{
                {"protection", 3},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        inv.setLeggings(createEnchanted(Material.DIAMOND_LEGGINGS, new Object[][]{
                {"protection", 2},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        inv.setBoots(createEnchanted(Material.DIAMOND_BOOTS, new Object[][]{
                {"protection", 3},
                {"feather_falling", 4},
                {"unbreaking", 3},
                {"mending", 1}
        }));

        // ===== OFFHAND =====
        inv.setItemInOffHand(createEnchanted(Material.SHIELD, new Object[][]{
                {"unbreaking", 3},
                {"mending", 1}
        }));

        // ===== HOTBAR =====
        inv.setItem(0, createEnchanted(Material.DIAMOND_SWORD, new Object[][]{
                {"sharpness", 4},
                {"unbreaking", 3}
        }));

        inv.setItem(1, createEnchanted(Material.DIAMOND_AXE, new Object[][]{
                {"sharpness", 1},
                {"unbreaking", 3}
        }));

        inv.setItem(2, new ItemStack(Material.GOLDEN_APPLE, 12));
        inv.setItem(3, createItem(Material.WATER_BUCKET));
        inv.setItem(4, createItem(Material.LAVA_BUCKET));
        inv.setItem(5, new ItemStack(Material.COBBLESTONE, 64));
        inv.setItem(6, new ItemStack(Material.COBWEB, 8));
        inv.setItem(7, createEnchanted(Material.BOW, new Object[][]{
                {"power", 1}
        }));
        inv.setItem(8, createEnchanted(Material.CROSSBOW, new Object[][]{
                {"piercing", 1}
        }));

        // ===== INVENTORY (not hotbar) =====
        inv.setItem(9, new ItemStack(Material.ARROW, 16)); // first inventory slot
        inv.setItem(12, createItem(Material.WATER_BUCKET)); // 4th inventory slot
        inv.setItem(14, createEnchanted(Material.DIAMOND_PICKAXE, new Object[][]{
                {"efficiency", 3},
                {"unbreaking", 3}
        }));
        inv.setItem(21, createItem(Material.WATER_BUCKET));
        inv.setItem(30, createItem(Material.WATER_BUCKET));
        inv.setItem(31, createItem(Material.LAVA_BUCKET));
        inv.setItem(32, new ItemStack(Material.COBBLESTONE, 64));
        inv.setItem(33, new ItemStack(Material.OAK_PLANKS, 64));
        inv.setItem(34, new ItemStack(Material.OAK_PLANKS, 64));
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}