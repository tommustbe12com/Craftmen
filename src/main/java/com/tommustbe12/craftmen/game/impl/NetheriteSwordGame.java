package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NetheriteSwordGame extends Game {

    public NetheriteSwordGame() {
        super("Netherite Sword", Material.NETHERITE_SWORD);
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    private ItemStack createArmor(Material material, int protLevel) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(ench("protection"), protLevel, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void applyLoadout(Player player) {
        player.getInventory().clear();

        // Reset health/food properly
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);

        // Netherite Sword
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            sword.setItemMeta(swordMeta);
        }

        player.getInventory().setItem(0, sword);

        // Armor
        player.getInventory().setHelmet(createArmor(Material.NETHERITE_HELMET, 3));
        player.getInventory().setChestplate(createArmor(Material.NETHERITE_CHESTPLATE, 3));
        player.getInventory().setLeggings(createArmor(Material.NETHERITE_LEGGINGS, 4));
        player.getInventory().setBoots(createArmor(Material.NETHERITE_BOOTS, 4));
    }

    @Override
    public void onStart(Match match) {
        // nothing special
    }

    @Override
    public void onEnd(Match match) {
        // nothing special
    }
}