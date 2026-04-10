package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

public class UHCGame extends Game {

    public UHCGame() {
        super("UHC", new ItemStack(Material.GOLDEN_APPLE));
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
    public Kit createDefaultKit() {
        ItemStack[] contents = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];

        armor[0] = createEnchanted(Material.DIAMOND_HELMET, new Object[][]{
                {"protection", 3},
                {"unbreaking", 3},
                {"mending", 1}
        });

        armor[1] = createEnchanted(Material.DIAMOND_CHESTPLATE, new Object[][]{
                {"protection", 3},
                {"unbreaking", 3},
                {"mending", 1}
        });

        armor[2] = createEnchanted(Material.DIAMOND_LEGGINGS, new Object[][]{
                {"protection", 2},
                {"unbreaking", 3},
                {"mending", 1}
        });

        armor[3] = createEnchanted(Material.DIAMOND_BOOTS, new Object[][]{
                {"protection", 3},
                {"feather_falling", 4},
                {"unbreaking", 3},
                {"mending", 1}
        });

        ItemStack offhand = createEnchanted(Material.SHIELD, new Object[][]{
                {"unbreaking", 3},
                {"mending", 1}
        });

        // ===== HOTBAR =====
        contents[0] = createEnchanted(Material.DIAMOND_SWORD, new Object[][]{
                {"sharpness", 4},
                {"unbreaking", 3}
        });

        contents[1] = createEnchanted(Material.DIAMOND_AXE, new Object[][]{
                {"sharpness", 1},
                {"unbreaking", 3}
        });

        contents[2] = new ItemStack(Material.GOLDEN_APPLE, 12);
        contents[3] = createItem(Material.WATER_BUCKET);
        contents[4] = createItem(Material.LAVA_BUCKET);
        contents[5] = new ItemStack(Material.COBBLESTONE, 64);
        contents[6] = new ItemStack(Material.COBWEB, 8);
        contents[7] = createEnchanted(Material.BOW, new Object[][]{
                {"power", 1}
        });
        contents[8] = createEnchanted(Material.CROSSBOW, new Object[][]{
                {"piercing", 1}
        });

        // ===== INVENTORY (not hotbar) =====
        contents[9] = new ItemStack(Material.ARROW, 16); // first inventory slot
        contents[12] = createItem(Material.WATER_BUCKET); // 4th inventory slot
        contents[14] = createEnchanted(Material.DIAMOND_PICKAXE, new Object[][]{
                {"efficiency", 3},
                {"unbreaking", 3}
        });
        contents[21] = createItem(Material.WATER_BUCKET);
        contents[30] = createItem(Material.WATER_BUCKET);
        contents[31] = createItem(Material.LAVA_BUCKET);
        contents[32] = new ItemStack(Material.COBBLESTONE, 64);
        contents[33] = new ItemStack(Material.OAK_PLANKS, 64);
        contents[34] = new ItemStack(Material.OAK_PLANKS, 64);

        return new Kit(contents, armor, offhand);
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}
