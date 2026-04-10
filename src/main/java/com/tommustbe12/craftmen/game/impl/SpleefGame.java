package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SpleefGame extends Game {

    public SpleefGame() {
        super("Spleef", new ItemStack(Material.NETHERITE_SHOVEL));
    }

    @Override
    public Kit createDefaultKit() {
        ItemStack pickaxe = new ItemStack(Material.NETHERITE_SHOVEL);
        ItemMeta meta = pickaxe.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "Dig!");
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.setUnbreakable(true);

        pickaxe.setItemMeta(meta);

        ItemStack[] contents = new ItemStack[36];
        contents[0] = pickaxe;
        return new Kit(contents, new ItemStack[4], null);
    }

    @Override
    public void onStart(Match match) {
        // nothing needed — all handled by global listeners
    }

    @Override
    public void onEnd(Match match) {
        // nothing needed — all handled by global listeners
    }
}
