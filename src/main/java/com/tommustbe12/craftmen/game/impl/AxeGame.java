package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

public class AxeGame extends Game {

    public AxeGame() {
        super("Axe", Material.DIAMOND_AXE);
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    @Override
    public void applyLoadout(Player player) {
        player.getInventory().clear();

        ItemStack axe = new ItemStack(Material.DIAMOND_AXE);

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(ench("sharpness"), 1);

        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(ench("power"), 1);

        player.getInventory().addItem(axe);
        player.getInventory().addItem(sword);
        player.getInventory().addItem(bow);
        player.getInventory().addItem(new ItemStack(Material.ARROW, 8));

        player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));

        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}