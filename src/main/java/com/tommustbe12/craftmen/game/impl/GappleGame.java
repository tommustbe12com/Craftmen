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

public class GappleGame extends Game {

    public GappleGame() {
        super("Gapple", new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    private ItemStack createArmor(Material material, int protLevel) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(ench("protection"), protLevel, true);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Kit createDefaultKit() {
        ItemStack[] contents = new ItemStack[36];
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(ench("sharpness"), 3);
        sword.addEnchantment(ench("unbreaking"), 3);
        contents[0] = sword;

        contents[1] = new ItemStack(Material.GOLDEN_APPLE, 64);

        ItemStack[] armor = new ItemStack[4];
        armor[0] = createArmor(Material.DIAMOND_HELMET, 3);
        armor[1] = createArmor(Material.DIAMOND_CHESTPLATE, 3);
        armor[2] = createArmor(Material.DIAMOND_LEGGINGS, 4);
        armor[3] = createArmor(Material.DIAMOND_BOOTS, 4);

        return new Kit(contents, armor, null);
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}
