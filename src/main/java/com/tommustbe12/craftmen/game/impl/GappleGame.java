package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

public class GappleGame extends Game {

    public GappleGame() {
        super("Gapple", Material.GOLDEN_APPLE);
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
    public void applyLoadout(Player player) {
        player.getInventory().clear();

        // Sword
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(ench("sharpness"), 3);
        sword.addEnchantment(ench("unbreaking"), 3);
        player.getInventory().addItem(sword);

        // Gapples
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));

        // Armor
        player.getInventory().setHelmet(createArmor(Material.DIAMOND_HELMET, 3));
        player.getInventory().setChestplate(createArmor(Material.DIAMOND_CHESTPLATE, 3));
        player.getInventory().setLeggings(createArmor(Material.DIAMOND_LEGGINGS, 4));
        player.getInventory().setBoots(createArmor(Material.DIAMOND_BOOTS, 4));
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}