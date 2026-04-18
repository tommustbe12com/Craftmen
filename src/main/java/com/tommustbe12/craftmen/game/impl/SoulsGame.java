package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.souls.SoulsItems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class SoulsGame extends Game {

    public SoulsGame() {
        super("Souls", new ItemStack(Material.AMETHYST_SHARD));
    }

    private Enchantment enchOrNull(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    private ItemStack enchItem(Material mat, Object[][] enchants) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            for (Object[] e : enchants) {
                Enchantment ench = enchOrNull((String) e[0]);
                if (ench == null) continue;
                meta.addEnchant(ench, (int) e[1], true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Material materialOr(Material preferred, Material fallback) {
        return preferred != null ? preferred : fallback;
    }

    private ItemStack splash(PotionType type) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        ItemMeta meta = potion.getItemMeta();
        if (meta instanceof PotionMeta pm) {
            pm.setBasePotionType(type);
            potion.setItemMeta(pm);
        }
        return potion;
    }

    private ItemStack turtleMaster20s() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        ItemMeta meta = potion.getItemMeta();
        if (meta instanceof PotionMeta pm) {
            // Exact effects requested: Slowness VI (20s) + Resistance IV (20s)
            pm.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 20, 5), true);
            pm.addCustomEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 20, 3), true);
            potion.setItemMeta(pm);
        }
        return potion;
    }

    @Override
    public Kit createDefaultKit() {
        ItemStack[] contents = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];

        // Armor array order used in this codebase: [helmet, chest, legs, boots]
        armor[0] = enchItem(Material.DIAMOND_HELMET, new Object[][]{
                {"protection", 3}, {"unbreaking", 3}, {"mending", 1}
        });
        armor[1] = enchItem(Material.DIAMOND_CHESTPLATE, new Object[][]{
                {"protection", 4}, {"unbreaking", 3}, {"mending", 1}
        });
        armor[2] = enchItem(Material.DIAMOND_LEGGINGS, new Object[][]{
                {"protection", 3}, {"unbreaking", 3}, {"mending", 1}
        });
        armor[3] = enchItem(Material.DIAMOND_BOOTS, new Object[][]{
                {"protection", 3}, {"feather_falling", 4}, {"unbreaking", 3}, {"mending", 1}
        });

        ItemStack offhand = enchItem(Material.SHIELD, new Object[][]{
                {"unbreaking", 3}, {"mending", 1}
        });

        // Hotbar
        ItemStack sword = enchItem(Material.DIAMOND_SWORD, new Object[][]{
                {"sharpness", 5}, {"fire_aspect", 2}, {"unbreaking", 3}, {"mending", 1}
        });
        ItemStack axe = enchItem(Material.DIAMOND_AXE, new Object[][]{
                {"sharpness", 5}, {"efficiency", 5}, {"unbreaking", 3}, {"mending", 1}
        });
        ItemStack chorus = new ItemStack(Material.CHORUS_FRUIT, 32);
        ItemStack gapples = new ItemStack(Material.GOLDEN_APPLE, 64);
        ItemStack water = new ItemStack(Material.WATER_BUCKET);
        ItemStack turtle = turtleMaster20s();
        ItemStack wind = new ItemStack(Material.WIND_CHARGE, 32);
        ItemStack webs = new ItemStack(Material.COBWEB, 32);

        // Inventory items
        ItemStack fireRes = splash(PotionType.LONG_FIRE_RESISTANCE);
        ItemStack strength = splash(PotionType.STRONG_STRENGTH);
        ItemStack speed = splash(PotionType.LONG_SWIFTNESS);
        ItemStack xp = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);

        ItemStack crossbow = enchItem(Material.CROSSBOW, new Object[][]{
                {"piercing", 4}, {"quick_charge", 3}, {"unbreaking", 3}, {"mending", 1}
        });

        Material maceMat = Material.matchMaterial("MACE");
        ItemStack mace = enchItem(materialOr(maceMat, Material.DIAMOND_AXE), new Object[][]{
                {"wind_burst", 1}, {"density", 1}
        });

        Material spearMat = Material.matchMaterial("DIAMOND_SPEAR");
        ItemStack spear = enchItem(materialOr(spearMat, Material.TRIDENT), new Object[][]{
                {"lunge", 3}
        });

        ItemStack shard = SoulsItems.shardOfSoul(); // hotbar slot 9

        Material paleOakLog = Material.matchMaterial("PALE_OAK_LOG");
        ItemStack logs = new ItemStack(materialOr(paleOakLog, Material.OAK_LOG), 48);

        ItemStack dmgArrow = new ItemStack(Material.TIPPED_ARROW, 16);
        ItemMeta arrowMeta = dmgArrow.getItemMeta();
        if (arrowMeta instanceof PotionMeta pm) {
            pm.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1), true);
            dmgArrow.setItemMeta(pm);
        }

        ItemStack bundle = new ItemStack(Material.BUNDLE);
        ItemMeta bundleMeta = bundle.getItemMeta();
        if (bundleMeta instanceof BundleMeta bm) {
            bm.addItem(logs);
            bm.addItem(dmgArrow);
            bundle.setItemMeta(bm);
        }

        // Hotbar slots 1-9
        contents[0] = sword;
        contents[1] = axe;
        contents[2] = chorus;
        contents[3] = gapples;
        contents[4] = water;
        contents[5] = turtle;
        contents[6] = wind;
        contents[7] = webs;
        contents[8] = shard;

        // Inventory (slots 1-27 => contents[8+slot])
        // 1-3 fire res
        contents[8 + 1] = fireRes.clone();
        contents[8 + 2] = fireRes.clone();
        contents[8 + 3] = fireRes.clone();
        // 4-9 strength
        for (int i = 4; i <= 9; i++) contents[8 + i] = strength.clone();
        // 10-12 speed
        for (int i = 10; i <= 12; i++) contents[8 + i] = speed.clone();
        // 13-18 strength
        for (int i = 13; i <= 18; i++) contents[8 + i] = strength.clone();
        // 19 xp
        contents[8 + 19] = xp;
        // 20 crossbow
        contents[8 + 20] = crossbow;
        // 21 mace
        contents[8 + 21] = mace;
        // 22 gapples
        contents[8 + 22] = new ItemStack(Material.GOLDEN_APPLE, 64);
        // 23 water
        contents[8 + 23] = new ItemStack(Material.WATER_BUCKET);
        // 24 turtle master
        contents[8 + 24] = turtleMaster20s();
        // 25 strength
        contents[8 + 25] = strength.clone();
        // 26 diamond spear (lunge 3)
        contents[8 + 26] = spear;
        // 27 bundle (logs first, then arrows)
        contents[8 + 27] = bundle;

        return new Kit(contents, armor, offhand);
    }

    @Override
    public void onStart(Match match) {
        if (match == null) return;
        Craftmen.get().getSoulsManager().onRoundStart(match.getP1());
        Craftmen.get().getSoulsManager().onRoundStart(match.getP2());
    }

    @Override
    public void onEnd(Match match) {
    }
}
