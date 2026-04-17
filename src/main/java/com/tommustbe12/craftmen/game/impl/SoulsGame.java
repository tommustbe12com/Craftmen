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

        ItemStack sword = enchItem(Material.DIAMOND_SWORD, new Object[][]{
                {"sharpness", 5}, {"fire_aspect", 2}, {"unbreaking", 3}, {"mending", 1}
        });
        ItemStack axe = enchItem(Material.DIAMOND_AXE, new Object[][]{
                {"sharpness", 5}, {"efficiency", 5}, {"unbreaking", 3}, {"mending", 1}
        });
        ItemStack crossbow = enchItem(Material.CROSSBOW, new Object[][]{
                {"piercing", 4}, {"quick_charge", 3}, {"unbreaking", 3}, {"mending", 1}
        });

        Material maceMat = Material.matchMaterial("MACE");
        ItemStack mace = enchItem(materialOr(maceMat, Material.DIAMOND_AXE), new Object[][]{
                {"wind_burst", 1}, {"density", 1}
        });

        // "Spear" = trident by default; support "lunge" enchant if present, else fall back to riptide.
        ItemStack spear = new ItemStack(Material.TRIDENT);
        ItemMeta spearMeta = spear.getItemMeta();
        if (spearMeta != null) {
            Enchantment lunge = enchOrNull("lunge");
            if (lunge != null) spearMeta.addEnchant(lunge, 3, true);
            else {
                Enchantment riptide = enchOrNull("riptide");
                if (riptide != null) spearMeta.addEnchant(riptide, 3, true);
            }
            Enchantment unbreaking = enchOrNull("unbreaking");
            if (unbreaking != null) spearMeta.addEnchant(unbreaking, 3, true);
            Enchantment mending = enchOrNull("mending");
            if (mending != null) spearMeta.addEnchant(mending, 1, true);
            spear.setItemMeta(spearMeta);
        }

        ItemStack shard = SoulsItems.shardOfSoul();

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

        contents[0] = sword;
        contents[1] = axe;
        contents[2] = spear;
        contents[3] = mace;
        contents[4] = crossbow;
        contents[5] = shard;
        contents[6] = bundle;

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

