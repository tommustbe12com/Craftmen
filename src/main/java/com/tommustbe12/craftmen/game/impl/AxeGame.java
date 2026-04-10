package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

public class AxeGame extends Game {

    public AxeGame() {
        super("Axe", new ItemStack(Material.DIAMOND_AXE));
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    @Override
    public Kit createDefaultKit() {
        ItemStack axe = new ItemStack(Material.DIAMOND_AXE);

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(ench("sharpness"), 1);

        ItemStack bow = new ItemStack(Material.BOW);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = axe;
        contents[1] = sword;
        contents[2] = new ItemStack(Material.CROSSBOW);
        contents[3] = bow;
        contents[8] = new ItemStack(Material.ARROW, 5);

        ItemStack[] armor = new ItemStack[4];
        armor[0] = new ItemStack(Material.DIAMOND_HELMET);
        armor[1] = new ItemStack(Material.DIAMOND_CHESTPLATE);
        armor[2] = new ItemStack(Material.DIAMOND_LEGGINGS);
        armor[3] = new ItemStack(Material.DIAMOND_BOOTS);

        return new Kit(contents, armor, new ItemStack(Material.SHIELD));
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}
