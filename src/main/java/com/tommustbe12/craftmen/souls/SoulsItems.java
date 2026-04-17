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

    public static final NamespacedKey SHARD_KEY = new NamespacedKey(Craftmen.get(), "soul_shard");

    private SoulsItems() {}

    public static ItemStack shardOfSoul() {
        ItemStack shard = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = shard.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d🔥");
            meta.setUnbreakable(true);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(SHARD_KEY, PersistentDataType.BYTE, (byte) 1);
            shard.setItemMeta(meta);
        }
        return shard;
    }

    public static boolean isShardOfSoul(ItemStack item) {
        if (item == null || item.getType() != Material.AMETHYST_SHARD) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte b = meta.getPersistentDataContainer().get(SHARD_KEY, PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }
}
