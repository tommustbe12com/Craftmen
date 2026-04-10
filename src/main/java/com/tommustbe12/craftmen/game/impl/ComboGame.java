package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

public class ComboGame extends Game {

    public ComboGame() {
        super("Combo", new ItemStack(Material.BLAZE_ROD));
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    @Override
    public Kit createDefaultKit() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(ench("sharpness"), 2);
        sword.addEnchantment(ench("knockback"), 2);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = sword;
        contents[1] = new ItemStack(Material.GOLDEN_APPLE, 16);

        ItemStack[] armor = new ItemStack[4];
        armor[0] = new ItemStack(Material.DIAMOND_HELMET);
        armor[1] = new ItemStack(Material.DIAMOND_CHESTPLATE);
        armor[2] = new ItemStack(Material.DIAMOND_LEGGINGS);
        armor[3] = new ItemStack(Material.DIAMOND_BOOTS);

        return new Kit(contents, armor, null);
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}
