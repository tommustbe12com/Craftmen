package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomKitGame extends Game {

    private final Random random = new Random();

    public RandomKitGame() {
        super("Random Kit", new ItemStack(Material.CHEST));
    }

    @Override
    public Kit createDefaultKit() {
        ItemStack[] contents = new ItemStack[36];

        ItemStack[] armor = new ItemStack[4];
        armor[0] = randomArmorPiece("helmet");
        armor[1] = randomArmorPiece("chestplate");
        armor[2] = randomArmorPiece("leggings");
        armor[3] = randomArmorPiece("boots");

        List<ItemStack> hotbar = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            hotbar.add(randomItem());
        }
        Collections.shuffle(hotbar);

        for (int i = 0; i < hotbar.size(); i++) {
            contents[i] = hotbar.get(i);
        }

        return new Kit(contents, armor, null);
    }

    @Override
    public void onStart(Match match) {}

    @Override
    public void onEnd(Match match) {
        // Optional: nothing special needed
    }

    private ItemStack randomArmorPiece(String type) {
        Material[] helmet = {
                Material.LEATHER_HELMET,
                Material.CHAINMAIL_HELMET,
                Material.IRON_HELMET,
                Material.DIAMOND_HELMET,
                Material.NETHERITE_HELMET
        };
        Material[] chestplate = {
                Material.LEATHER_CHESTPLATE,
                Material.CHAINMAIL_CHESTPLATE,
                Material.IRON_CHESTPLATE,
                Material.DIAMOND_CHESTPLATE,
                Material.NETHERITE_CHESTPLATE
        };
        Material[] leggings = {
                Material.LEATHER_LEGGINGS,
                Material.CHAINMAIL_LEGGINGS,
                Material.IRON_LEGGINGS,
                Material.DIAMOND_LEGGINGS,
                Material.NETHERITE_LEGGINGS
        };
        Material[] boots = {
                Material.LEATHER_BOOTS,
                Material.CHAINMAIL_BOOTS,
                Material.IRON_BOOTS,
                Material.DIAMOND_BOOTS,
                Material.NETHERITE_BOOTS
        };

        Material mat = null;

        switch (type.toLowerCase()) {
            case "helmet":
                mat = helmet[random.nextInt(helmet.length)];
                break;
            case "chestplate":
                mat = chestplate[random.nextInt(chestplate.length)];
                break;
            case "leggings":
                mat = leggings[random.nextInt(leggings.length)];
                break;
            case "boots":
                mat = boots[random.nextInt(boots.length)];
                break;
        }

        return mat != null ? new ItemStack(mat) : null;
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
