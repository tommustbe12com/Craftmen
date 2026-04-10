package com.tommustbe12.craftmen.kit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;

public final class Kit {

    private final ItemStack[] contents; // 0-35
    private final ItemStack[] armor;    // 0-3: helmet, chestplate, leggings, boots
    private final ItemStack offhand;

    public Kit(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        this.contents = normalize(contents, 36);
        this.armor = normalize(armor, 4);
        this.offhand = offhand == null ? null : offhand.clone();
    }

    public static Kit empty() {
        return new Kit(new ItemStack[36], new ItemStack[4], null);
    }

    public static Kit fromPlayer(Player player) {
        PlayerInventory inv = player.getInventory();

        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            contents[i] = item == null ? null : item.clone();
        }

        ItemStack[] armor = new ItemStack[4];
        armor[0] = inv.getHelmet() == null ? null : inv.getHelmet().clone();
        armor[1] = inv.getChestplate() == null ? null : inv.getChestplate().clone();
        armor[2] = inv.getLeggings() == null ? null : inv.getLeggings().clone();
        armor[3] = inv.getBoots() == null ? null : inv.getBoots().clone();

        ItemStack offhand = inv.getItemInOffHand();
        return new Kit(contents, armor, offhand == null ? null : offhand.clone());
    }

    public ItemStack[] getContents() {
        return clone(contents);
    }

    public ItemStack[] getArmor() {
        return clone(armor);
    }

    public ItemStack getOffhand() {
        return offhand == null ? null : offhand.clone();
    }

    public void apply(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();

        for (int i = 0; i < 36; i++) {
            inv.setItem(i, contents[i] == null ? null : contents[i].clone());
        }

        inv.setHelmet(armor[0] == null ? null : armor[0].clone());
        inv.setChestplate(armor[1] == null ? null : armor[1].clone());
        inv.setLeggings(armor[2] == null ? null : armor[2].clone());
        inv.setBoots(armor[3] == null ? null : armor[3].clone());

        inv.setItemInOffHand(offhand == null ? null : offhand.clone());
        player.updateInventory();
    }

    private static ItemStack[] normalize(ItemStack[] src, int size) {
        ItemStack[] out = new ItemStack[size];
        if (src == null) return out;
        for (int i = 0; i < Math.min(src.length, size); i++) {
            out[i] = src[i] == null ? null : src[i].clone();
        }
        return out;
    }

    private static ItemStack[] clone(ItemStack[] src) {
        ItemStack[] out = Arrays.copyOf(src, src.length);
        for (int i = 0; i < out.length; i++) {
            out[i] = out[i] == null ? null : out[i].clone();
        }
        return out;
    }
}

