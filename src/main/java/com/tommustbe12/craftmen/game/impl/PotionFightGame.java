package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class PotionFightGame extends Game {

    public PotionFightGame() {
        super("Potion Fight", createHarmingPotion());
    }

    private static ItemStack createHarmingPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);

        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

        if (potionMeta != null) {
            potionMeta.setBasePotionType(PotionType.HARMING);

            potion.setItemMeta(potionMeta);
        }

        return potion;
    }

    @Override
    public Kit createDefaultKit() {
        ItemStack[] contents = new ItemStack[36];
        contents[0] = new ItemStack(Material.STONE_SWORD);
        contents[1] = createPotion(PotionType.HARMING, true);
        contents[2] = createPotion(PotionType.POISON, true);
        contents[3] = createPotion(PotionType.HEALING, true);
        contents[4] = createPotion(PotionType.SWIFTNESS, true);
        contents[5] = createPotion(PotionType.SLOWNESS, true);
        contents[6] = createPotion(PotionType.HARMING, true);
        contents[7] = new ItemStack(Material.BOW);
        contents[8] = new ItemStack(Material.ARROW, 16);

        for (int slot = 9; slot < 36; slot++) {
            PotionType type;
            switch (slot % 5) {
                case 0 -> type = PotionType.HARMING;
                case 1 -> type = PotionType.HEALING;
                case 2 -> type = PotionType.POISON;
                case 3 -> type = PotionType.SWIFTNESS;
                default -> type = PotionType.SLOWNESS;
            }
            contents[slot] = createPotion(type, true);
        }

        ItemStack[] armor = new ItemStack[4];
        armor[0] = new ItemStack(Material.IRON_HELMET);
        armor[1] = new ItemStack(Material.IRON_CHESTPLATE);
        armor[2] = new ItemStack(Material.IRON_LEGGINGS);
        armor[3] = new ItemStack(Material.IRON_BOOTS);

        return new Kit(contents, armor, null);
    }

    @Override
    protected void afterLoadoutApplied(Player player) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    @Override
    public void onStart(Match match) {}

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
