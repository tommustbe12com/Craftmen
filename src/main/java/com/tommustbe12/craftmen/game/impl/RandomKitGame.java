package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomKitGame extends Game {

    private final Random random = new Random();

    public RandomKitGame() {
        super("Random Kit", Material.CHEST);
    }

    @Override
    public void applyLoadout(Player player) {
        player.getInventory().clear();

        // Random armor
        player.getInventory().setHelmet(randomArmorPiece());
        player.getInventory().setChestplate(randomArmorPiece());
        player.getInventory().setLeggings(randomArmorPiece());
        player.getInventory().setBoots(randomArmorPiece());

        // Random 9-slot hotbar
        List<ItemStack> hotbar = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            hotbar.add(randomItem());
        }
        Collections.shuffle(hotbar);

        for (int i = 0; i < hotbar.size(); i++) {
            player.getInventory().setItem(i, hotbar.get(i));
        }
    }

    @Override
    public void onStart(Match match) {
        applyLoadout(match.getP1());
        applyLoadout(match.getP2());
    }

    @Override
    public void onEnd(Match match) {
        // Optional: nothing special needed
    }

    private ItemStack randomArmorPiece() {
        Material[] armor = {
                Material.LEATHER_HELMET,
                Material.CHAINMAIL_HELMET,
                Material.IRON_HELMET,
                Material.DIAMOND_HELMET,
                Material.NETHERITE_HELMET
        };
        Material mat = armor[random.nextInt(armor.length)];
        return new ItemStack(mat);
    }

    private ItemStack randomItem() {
        Material[] items = {
                Material.DIAMOND_SWORD,
                Material.IRON_SWORD,
                Material.BOW,
                Material.ARROW,
                Material.FISHING_ROD,
                Material.STONE_AXE,
                Material.DIAMOND_PICKAXE,
                Material.SHIELD,
                Material.SNOWBALL,
                Material.SPLASH_POTION
        };
        Material mat = items[random.nextInt(items.length)];
        ItemStack item = new ItemStack(mat);

        // Random simple meta if needed
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Random " + mat.name());
            item.setItemMeta(meta);
        }

        return item;
    }
}