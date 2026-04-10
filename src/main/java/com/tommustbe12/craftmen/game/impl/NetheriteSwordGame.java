package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NetheriteSwordGame extends Game {

    public NetheriteSwordGame() {
        super("Netherite Sword", new ItemStack(Material.NETHERITE_SWORD));
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
    public Kit createDefaultKit() {
        ItemStack[] contents = new ItemStack[36];
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            sword.setItemMeta(swordMeta);
        }
        contents[0] = sword;

        ItemStack[] armor = new ItemStack[4];
        armor[0] = createArmor(Material.NETHERITE_HELMET, 3);
        armor[1] = createArmor(Material.NETHERITE_CHESTPLATE, 3);
        armor[2] = createArmor(Material.NETHERITE_LEGGINGS, 4);
        armor[3] = createArmor(Material.NETHERITE_BOOTS, 4);

        return new Kit(contents, armor, null);
    }

    @Override
    protected void afterLoadoutApplied(Player player) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20f);
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
