package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * Internal-only pseudo-games so End Fight kits can be edited in the kit editor.
 * Not shown in normal selectors.
 */
public final class EndFightKitGame extends Game {

    public enum Variant { NETHERITE, DIAMOND, IRON }

    private final Variant variant;

    public EndFightKitGame(Variant variant) {
        super(nameFor(variant), new ItemStack(Material.END_PORTAL_FRAME));
        this.variant = variant;
    }

    @Override
    public boolean listInSelectors() {
        return false;
    }

    @Override
    public Kit createDefaultKit() {
        return switch (variant) {
            case NETHERITE -> netherite();
            case DIAMOND -> diamond();
            case IRON -> iron();
        };
    }

    @Override
    public void onStart(Match match) {}

    @Override
    public void onEnd(Match match) {}

    private static String nameFor(Variant v) {
        return switch (v) {
            case NETHERITE -> "End Fight (Netherite)";
            case DIAMOND -> "End Fight (Diamond)";
            case IRON -> "End Fight (Iron)";
        };
    }

    private static ItemStack enchant(ItemStack item, Map<Enchantment, Integer> enchants) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            for (var e : enchants.entrySet()) {
                meta.addEnchant(e.getKey(), e.getValue(), true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack splash(PotionEffectType type, int durationTicks, int amp) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.addCustomEffect(new PotionEffect(type, durationTicks, amp), true);
            potion.setItemMeta(meta);
        }
        return potion;
    }

    private static Kit netherite() {
        ItemStack[] contents = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];

        armor[0] = enchant(new ItemStack(Material.NETHERITE_HELMET), Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1, Enchantment.AQUA_AFFINITY, 1));
        armor[1] = enchant(new ItemStack(Material.NETHERITE_CHESTPLATE), Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        armor[2] = enchant(new ItemStack(Material.NETHERITE_LEGGINGS), Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1, Enchantment.SWIFT_SNEAK, 3));
        armor[3] = enchant(new ItemStack(Material.NETHERITE_BOOTS), Map.of(Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1, Enchantment.DEPTH_STRIDER, 3));

        ItemStack offhand = new ItemStack(Material.TOTEM_OF_UNDYING);

        ItemStack fireRes = splash(PotionEffectType.FIRE_RESISTANCE, 9600, 0);
        ItemStack strength = splash(PotionEffectType.STRENGTH, 1800, 1);
        ItemStack speed = splash(PotionEffectType.SPEED, 9600, 0);

        contents[0] = enchant(new ItemStack(Material.NETHERITE_SWORD), Map.of(Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        contents[1] = enchant(new ItemStack(Material.NETHERITE_AXE), Map.of(Enchantment.SHARPNESS, 5, Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        contents[2] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[3] = new ItemStack(Material.GOLDEN_APPLE, 64);
        contents[4] = new ItemStack(Material.WATER_BUCKET);
        contents[5] = enchant(new ItemStack(Material.NETHERITE_SPEAR), Map.of(Enchantment.LUNGE, 3, Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3));
        contents[6] = new ItemStack(Material.WIND_CHARGE, 64);
        contents[7] = new ItemStack(Material.MACE);
        contents[8] = enchant(new ItemStack(Material.SHIELD), Map.of(Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));

        contents[9] = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);
        contents[10] = fireRes.clone();
        contents[11] = fireRes.clone();
        contents[12] = fireRes.clone();
        contents[13] = strength.clone();
        contents[14] = strength.clone();
        contents[15] = strength.clone();
        contents[16] = strength.clone();
        contents[17] = strength.clone();
        contents[18] = new ItemStack(Material.OAK_LOG, 64);
        contents[19] = speed.clone();
        contents[20] = speed.clone();
        contents[21] = speed.clone();
        contents[22] = strength.clone();
        contents[23] = strength.clone();
        contents[24] = strength.clone();
        contents[25] = strength.clone();
        contents[26] = strength.clone();
        contents[27] = new ItemStack(Material.COBWEB, 64);
        contents[28] = enchant(new ItemStack(Material.NETHERITE_PICKAXE), Map.of(Enchantment.SILK_TOUCH, 1, Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        contents[29] = enchant(new ItemStack(Material.BOW), Map.of(Enchantment.POWER, 5, Enchantment.UNBREAKING, 3, Enchantment.INFINITY, 1));
        contents[30] = new ItemStack(Material.GOLDEN_APPLE, 64);
        contents[31] = new ItemStack(Material.WATER_BUCKET);
        contents[32] = new ItemStack(Material.ARROW, 1);
        contents[33] = strength.clone();
        contents[34] = strength.clone();
        contents[35] = strength.clone();

        return new Kit(contents, armor, offhand);
    }

    private static Kit diamond() {
        ItemStack[] contents = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];

        armor[0] = enchant(new ItemStack(Material.DIAMOND_HELMET), Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1, Enchantment.AQUA_AFFINITY, 1));
        armor[1] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Map.of(Enchantment.PROTECTION, 4, Enchantment.MENDING, 1, Enchantment.UNBREAKING, 3));
        armor[2] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1, Enchantment.SWIFT_SNEAK, 3));
        armor[3] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Map.of(Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1, Enchantment.DEPTH_STRIDER, 3));

        ItemStack offhand = enchant(new ItemStack(Material.SHIELD), Map.of(Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));

        ItemStack speed = splash(PotionEffectType.SPEED, 9600, 0);
        ItemStack strength = splash(PotionEffectType.STRENGTH, 1800, 1);
        ItemStack fireRes = splash(PotionEffectType.FIRE_RESISTANCE, 9600, 0);

        // Hotbar (approx)
        contents[0] = enchant(new ItemStack(Material.DIAMOND_SWORD), Map.of(Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        contents[1] = enchant(new ItemStack(Material.DIAMOND_AXE), Map.of(Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1));
        contents[2] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[3] = new ItemStack(Material.GOLDEN_CARROT, 64);
        contents[4] = new ItemStack(Material.WATER_BUCKET);
        contents[5] = new ItemStack(Material.WIND_CHARGE, 64);
        contents[6] = new ItemStack(Material.MACE);
        contents[7] = new ItemStack(Material.COBWEB, 64);
        contents[8] = new ItemStack(Material.EXPERIENCE_BOTTLE, 32);

        contents[9] = speed.clone();
        contents[10] = speed.clone();
        contents[11] = speed.clone();
        contents[12] = strength.clone();
        contents[13] = strength.clone();
        contents[14] = strength.clone();
        contents[15] = strength.clone();
        contents[16] = strength.clone();
        contents[17] = strength.clone();
        contents[18] = fireRes.clone();
        contents[19] = fireRes.clone();
        contents[20] = fireRes.clone();
        contents[21] = strength.clone();
        contents[22] = strength.clone();
        contents[23] = strength.clone();
        contents[24] = strength.clone();
        contents[25] = strength.clone();
        contents[26] = strength.clone();
        contents[27] = new ItemStack(Material.EXPERIENCE_BOTTLE, 32);
        contents[28] = new ItemStack(Material.ARROW, 32);
        contents[29] = enchant(new ItemStack(Material.BOW), Map.of(Enchantment.PUNCH, 1, Enchantment.UNBREAKING, 3));
        contents[30] = new ItemStack(Material.GOLDEN_CARROT, 64);
        contents[31] = new ItemStack(Material.WATER_BUCKET);
        contents[32] = new ItemStack(Material.SPRUCE_LOG, 64);
        contents[33] = strength.clone();
        contents[34] = strength.clone();
        contents[35] = strength.clone();

        return new Kit(contents, armor, offhand);
    }

    private static Kit iron() {
        // Keep a simpler default; players can customize via kit editor.
        ItemStack[] contents = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];

        armor[0] = enchant(new ItemStack(Material.IRON_HELMET), Map.of(Enchantment.PROTECTION, 3, Enchantment.UNBREAKING, 3));
        armor[1] = enchant(new ItemStack(Material.IRON_CHESTPLATE), Map.of(Enchantment.PROTECTION, 3, Enchantment.UNBREAKING, 3));
        armor[2] = enchant(new ItemStack(Material.IRON_LEGGINGS), Map.of(Enchantment.PROTECTION, 3, Enchantment.UNBREAKING, 3));
        armor[3] = enchant(new ItemStack(Material.IRON_BOOTS), Map.of(Enchantment.PROTECTION, 3, Enchantment.FEATHER_FALLING, 3, Enchantment.UNBREAKING, 3));

        ItemStack offhand = enchant(new ItemStack(Material.SHIELD), Map.of(Enchantment.UNBREAKING, 3));

        ItemStack speed = splash(PotionEffectType.SPEED, 20 * 90, 0);
        ItemStack strength = splash(PotionEffectType.STRENGTH, 20 * 30, 0);

        contents[0] = enchant(new ItemStack(Material.IRON_SWORD), Map.of(Enchantment.SHARPNESS, 3, Enchantment.UNBREAKING, 3));
        contents[1] = enchant(new ItemStack(Material.IRON_AXE), Map.of(Enchantment.SHARPNESS, 3, Enchantment.UNBREAKING, 3));
        contents[2] = new ItemStack(Material.ENDER_PEARL, 8);
        contents[3] = new ItemStack(Material.GOLDEN_APPLE, 16);
        contents[4] = new ItemStack(Material.WATER_BUCKET);
        contents[5] = new ItemStack(Material.WIND_CHARGE, 32);
        contents[6] = new ItemStack(Material.MACE);
        contents[7] = new ItemStack(Material.COBWEB, 32);
        contents[8] = new ItemStack(Material.EXPERIENCE_BOTTLE, 16);

        contents[9] = speed.clone();
        contents[10] = speed.clone();
        contents[11] = strength.clone();
        contents[12] = strength.clone();
        contents[13] = new ItemStack(Material.ARROW, 16);
        contents[14] = new ItemStack(Material.BOW);
        contents[15] = new ItemStack(Material.OAK_LOG, 32);

        return new Kit(contents, armor, offhand);
    }
}

