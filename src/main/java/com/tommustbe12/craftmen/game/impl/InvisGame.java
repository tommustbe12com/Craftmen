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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class InvisGame extends Game {

    public InvisGame() {
        super("Invis", Material.POTION);
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

        // Reset stats
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);

        // Sword
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));

        // Armor
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
    }

    @Override
    public void onStart(Match match) {
        applyInvis(match.getP1());
        applyInvis(match.getP2());
    }

    @Override
    public void onEnd(Match match) {
        removeInvis(match.getP1());
        removeInvis(match.getP2());
    }

    private void applyInvis(Player player) {
        if (player == null) return;

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.INVISIBILITY,
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false
                )
        );
    }

    private void removeInvis(Player player) {
        if (player == null) return;
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private static ItemStack createInvisPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        if (meta != null) {
            meta.setBasePotionType(PotionType.INVISIBILITY);
            meta.setDisplayName("§7Invisibility");
            potion.setItemMeta(meta);
        }

        return potion;
    }
}