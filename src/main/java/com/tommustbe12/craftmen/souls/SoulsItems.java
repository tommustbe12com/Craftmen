package com.tommustbe12.craftmen.souls;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class SoulsItems {

    public static final NamespacedKey SOUL_ITEM_KEY = new NamespacedKey(Craftmen.get(), "soul_item");
    public static final NamespacedKey SOUL_CHARACTER_KEY = new NamespacedKey(Craftmen.get(), "soul_character");

    private SoulsItems() {}

    public static ItemStack shardOfSoul() {
        // Placeholder item that the kit editor can move/save.
        // At runtime this is replaced with the selected character's icon.
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§dSoul Ability");
            meta.setUnbreakable(true);
            // Glint-only – hidden.
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(SOUL_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack soulItem(SoulCharacter character) {
        if (character == null) character = SoulCharacter.GOOP;

        Material mat = switch (character) {
            case DEVILS_FROST -> Material.SNOWBALL;
            case GOOP -> Material.SLIME_BALL;
            case VOICE_OF_THE_SEA -> Material.TRIDENT;
            case MAGNET -> materialOr("HEAVY_CORE", Material.IRON_BLOCK);
            case ARTIFICIAL_GENOCIDE -> Material.REPEATING_COMMAND_BLOCK;
            case COSMIC_DESTROYER -> Material.ECHO_SHARD;
            case SORCERER -> Material.AMETHYST_SHARD;
            case KING_OF_HEAT -> Material.BLAZE_POWDER;
            case ARCHANGEL -> Material.FEATHER;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d" + character.getDisplayName());
            meta.setUnbreakable(true);
            // Glint-only – hidden.
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(SOUL_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(SOUL_CHARACTER_KEY, PersistentDataType.STRING, character.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isShardOfSoul(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte b = meta.getPersistentDataContainer().get(SOUL_ITEM_KEY, PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    public static SoulCharacter getSoulCharacter(ItemStack item) {
        if (!isShardOfSoul(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String name = meta.getPersistentDataContainer().get(SOUL_CHARACTER_KEY, PersistentDataType.STRING);
        if (name == null) return null;
        try {
            return SoulCharacter.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Material materialOr(String preferred, Material fallback) {
        Material m = Material.matchMaterial(preferred);
        return m != null ? m : fallback;
    }
}
