package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MaceGame extends Game {

    public MaceGame() {
        super("Mace", new ItemStack(Material.MACE));
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    private ItemStack enchItem(Material mat, Object[][] enchants) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        for (Object[] e : enchants) {
            meta.addEnchant(ench((String) e[0]), (int) e[1], true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack splash(PotionEffectType type, int seconds, int amplifier) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, 20 * seconds, amplifier), true);
        potion.setItemMeta(meta);
        return potion;
    }

    private ItemStack createShulker(ItemStack speed, ItemStack strength) {
        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) shulker.getItemMeta();
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        Inventory inv = box.getInventory();

        // 1-4 Speed
        for (int i = 1; i <= 4; i++)
            inv.setItem(i - 1, speed.clone());

        // 5-9 Strength
        for (int i = 5; i <= 9; i++)
            inv.setItem(i - 1, strength.clone());

        // 10-18 Speed
        for (int i = 10; i <= 18; i++)
            inv.setItem(i - 1, speed.clone());

        // 19-27 Strength
        for (int i = 19; i <= 27; i++)
            inv.setItem(i - 1, strength.clone());

        meta.setBlockState(box);
        shulker.setItemMeta(meta);
        return shulker;
    }

    @Override
    public Kit createDefaultKit() {
        ItemStack[] contents = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];

        armor[0] = enchItem(Material.NETHERITE_HELMET, new Object[][]{
                {"protection",4},{"aqua_affinity",1},{"unbreaking",3},{"mending",1}
        });
        armor[1] = enchItem(Material.NETHERITE_CHESTPLATE, new Object[][]{
                {"protection",4},{"unbreaking",3},{"mending",1}
        });
        armor[2] = enchItem(Material.NETHERITE_LEGGINGS, new Object[][]{
                {"protection",4},{"swift_sneak",3},{"unbreaking",3},{"mending",1}
        });
        armor[3] = enchItem(Material.NETHERITE_BOOTS, new Object[][]{
                {"protection",4},{"feather_falling",4},{"depth_strider",3},{"unbreaking",3},{"mending",1}
        });

        ItemStack strength = splash(PotionEffectType.STRENGTH, 90, 1);
        ItemStack speed = splash(PotionEffectType.SPEED, 90, 1);

        contents[0] = enchItem(Material.NETHERITE_SWORD, new Object[][]{
                {"sharpness",5},{"unbreaking",3},{"mending",1}
        });
        contents[1] = enchItem(Material.NETHERITE_AXE, new Object[][]{
                {"sharpness",5},{"efficiency",5},{"unbreaking",3},{"mending",1}
        });
        contents[2] = new ItemStack(Material.ENDER_PEARL,16);
        contents[3] = new ItemStack(Material.GOLDEN_APPLE,64);
        contents[4] = new ItemStack(Material.ELYTRA);
        contents[5] = new ItemStack(Material.WIND_CHARGE,64);
        contents[6] = enchItem(Material.MACE, new Object[][]{
                {"wind_burst",3},{"density",5},{"unbreaking",3},{"mending",1}
        });
        contents[7] = enchItem(Material.MACE, new Object[][]{
                {"breach",4},{"unbreaking",3},{"mending",1}
        });
        contents[8] = enchItem(Material.SHIELD, new Object[][]{
                {"unbreaking",3},{"mending",1}
        });

        // ===== INVENTORY (+8 offset) =====

        // Slot 1 pearls
        contents[8 + 1] = new ItemStack(Material.ENDER_PEARL,16);

        // 2-8 strength
        for(int i=2;i<=8;i++) contents[8 + i] = strength.clone();

        // 9 shulker
        contents[8 + 9] = createShulker(speed, strength);

        // 10 pearls
        contents[8 + 10] = new ItemStack(Material.ENDER_PEARL,16);

        // 11-17 speed
        for(int i=11;i<=17;i++) contents[8 + i] = speed.clone();

        // 18 gapples
        contents[8 + 18] = new ItemStack(Material.GOLDEN_APPLE,64);

        // 19 pearls
        contents[8 + 19] = new ItemStack(Material.ENDER_PEARL,16);

        // 20-22 strength
        for(int i=20;i<=22;i++) contents[8 + i] = strength.clone();

        // 23-25 speed
        for(int i=23;i<=25;i++) contents[8 + i] = speed.clone();

        // 26 wind charges
        contents[8 + 26] = new ItemStack(Material.WIND_CHARGE,64);

        // 27 totem
        contents[8 + 27] = new ItemStack(Material.TOTEM_OF_UNDYING);

        return new Kit(contents, armor, new ItemStack(Material.TOTEM_OF_UNDYING));
    }

    @Override public void onStart(Match match) {}
    @Override public void onEnd(Match match) {}
}
