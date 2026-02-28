package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

public class ComboGame extends Game {

    public ComboGame() {
        super("Combo", Material.BLAZE_ROD);
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    @Override
    public void applyLoadout(Player player) {
        player.getInventory().clear();

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(ench("sharpness"), 2);
        sword.addEnchantment(ench("knockback"), 2);

        player.getInventory().addItem(sword);
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 16));

        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}