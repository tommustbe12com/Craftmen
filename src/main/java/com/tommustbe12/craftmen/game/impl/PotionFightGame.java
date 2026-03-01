package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class PotionFightGame extends Game {

    public PotionFightGame() {
        super("Potion Fight", Material.SPLASH_POTION);
    }

    @Override
    public void applyLoadout(Player player) {
        player.getInventory().clear();

        // reset stats
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);

        // 0-8 hotbar
        player.getInventory().setItem(0, new ItemStack(Material.STONE_SWORD));
        player.getInventory().setItem(1, createPotion(PotionType.STRONG_HARMING, true));
        player.getInventory().setItem(2, createPotion(PotionType.POISON, true));
        player.getInventory().setItem(3, createPotion(PotionType.INSTANT_HEAL, true));
        player.getInventory().setItem(4, createPotion(PotionType.SPEED, true));
        player.getInventory().setItem(5, createPotion(PotionType.SLOWNESS, true));
        player.getInventory().setItem(6, createPotion(PotionType.STRONG_HARMING, true));
        player.getInventory().setItem(7, new ItemStack(Material.BOW));
        player.getInventory().setItem(8, new ItemStack(Material.ARROW, 16));

        // more pots
        for (int slot = 9; slot < 36; slot++) {
            PotionType type;
            switch (slot % 5) {
                case 0 -> type = PotionType.STRONG_HARMING;
                case 1 -> type = PotionType.INSTANT_HEAL;
                case 2 -> type = PotionType.POISON;
                case 3 -> type = PotionType.SPEED;
                default -> type = PotionType.SLOWNESS;
            }
            player.getInventory().setItem(slot, createPotion(type, true));
        }

        // Light armor
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    @Override
    public void onStart(Match match) {
        applyLoadout(match.getP1());
        applyLoadout(match.getP2());
    }

    @Override
    public void onEnd(Match match) {
        // Nothing special
    }

    private ItemStack createPotion(PotionType type, boolean splash) {
        Material mat = splash ? Material.SPLASH_POTION : Material.POTION;
        ItemStack potion = new ItemStack(mat);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionData(new PotionData(type));
            potion.setItemMeta(meta);
        }
        return potion;
    }
}