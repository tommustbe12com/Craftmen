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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class InvisGame extends Game {

    public InvisGame() {
        super("Invis", createInvisPotion());
    }

    private static ItemStack createInvisPotion() {
        ItemStack potion = new ItemStack(Material.POTION);

        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

        if (potionMeta != null) {
            potionMeta.setBasePotionType(PotionType.INVISIBILITY);

            potion.setItemMeta(potionMeta);
        }

        return potion;
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
        contents[0] = new ItemStack(Material.DIAMOND_SWORD);
        return new Kit(contents, new ItemStack[4], null);
    }

    @Override
    protected void afterLoadoutApplied(Player player) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20f);
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
}
