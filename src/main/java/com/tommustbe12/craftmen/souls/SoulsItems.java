package com.tommustbe12.craftmen.souls;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class SoulsItems {

    public static final NamespacedKey SOUL_ITEM_KEY = new NamespacedKey(Craftmen.get(), "soul_item");
    public static final NamespacedKey SOUL_CHARACTER_KEY = new NamespacedKey(Craftmen.get(), "soul_character");

    private SoulsItems() {}

    public static ItemStack shardOfSoul() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§dSoul Ability");
            meta.setUnbreakable(true);
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
            case BOUNTY_HUNTER -> Material.NETHER_STAR;
            case COPYCAT -> materialOr("MUSIC_DISC_TEARS", Material.MUSIC_DISC_13);
            case BLOODY_MONARCH -> Material.GHAST_TEAR;
            case DARK_KNIGHT -> Material.NAUTILUS_SHELL;
            case RAILGUN -> Material.FISHING_ROD;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d" + character.getDisplayName());
            meta.setLore(soulLore(character));
            meta.setUnbreakable(true);
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

    private static List<String> soulLore(SoulCharacter c) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Left-click: " + ChatColor.GREEN + "[1]");
        lore.add(ChatColor.GRAY + "Right-click: " + ChatColor.AQUA + "[2]");
        lore.add(" ");

        switch (c) {
            case GOOP -> {
                lore.add(ChatColor.GREEN + "[1] Goop Bounce / Launch");
                lore.add(ChatColor.GRAY + "- Hit nearest enemy: knock them away.");
                lore.add(ChatColor.GRAY + "- Sneak: launch yourself instead.");
                lore.add(ChatColor.AQUA + "[2] Goop Freeze");
                lore.add(ChatColor.GRAY + "- Freeze nearest enemy for " + ChatColor.WHITE + "2s" + ChatColor.GRAY + ".");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + "Speed I" + ChatColor.GRAY + " always.");
            }
            case DEVILS_FROST -> {
                lore.add(ChatColor.GREEN + "[1] Frost Stun");
                lore.add(ChatColor.GRAY + "- Freeze + heavy slowness for " + ChatColor.WHITE + "3s" + ChatColor.GRAY + ".");
                lore.add(ChatColor.AQUA + "[2] Frost Hearts " + ChatColor.DARK_GRAY + "(Special)");
                lore.add(ChatColor.GRAY + "- Gain extra max health temporarily.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + "Frost Walker" + ChatColor.GRAY + " on boots.");
            }
            case VOICE_OF_THE_SEA -> {
                lore.add(ChatColor.GREEN + "[1] Riptide Boost");
                lore.add(ChatColor.GRAY + "- Dash forward with a strong burst.");
                lore.add(ChatColor.AQUA + "[2] Thunderstorm " + ChatColor.DARK_GRAY + "(Special)");
                lore.add(ChatColor.GRAY + "- Summon a storm and strike nearby enemies.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- During storms: " + ChatColor.WHITE + "Speed II" + ChatColor.GRAY + ".");
                lore.add(" ");
                lore.add(ChatColor.DARK_GRAY + "Loadout: gives a trident in slot 2.");
            }
            case MAGNET -> {
                lore.add(ChatColor.GREEN + "[1] Magnet Pull");
                lore.add(ChatColor.GRAY + "- Pull the nearest enemy to you.");
                lore.add(ChatColor.AQUA + "[2] Magnet Push");
                lore.add(ChatColor.GRAY + "- Launch you and the nearest enemy apart.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- Hitting enemies builds " + ChatColor.WHITE + "Speed" + ChatColor.GRAY + " stacks.");
            }
            case ARTIFICIAL_GENOCIDE -> {
                lore.add(ChatColor.GREEN + "[1] Code Shift");
                lore.add(ChatColor.GRAY + "- Teleport forward through the air.");
                lore.add(ChatColor.AQUA + "[2] Inventory Shuffle " + ChatColor.DARK_GRAY + "(Special)");
                lore.add(ChatColor.GRAY + "- Shuffles the nearest enemy's inventory.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- Random buff/debuff every " + ChatColor.WHITE + "60s" + ChatColor.GRAY + ".");
            }
            case COSMIC_DESTROYER -> {
                lore.add(ChatColor.GREEN + "[1] Cosmic Smash");
                lore.add(ChatColor.GRAY + "- Break ground and damage nearby enemies.");
                lore.add(ChatColor.AQUA + "[2] Blackhole " + ChatColor.DARK_GRAY + "(Special)");
                lore.add(ChatColor.GRAY + "- Pull enemies to a point and damage them.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + "10%" + ChatColor.GRAY + " less armor durability damage.");
            }
            case SORCERER -> {
                lore.add(ChatColor.GREEN + "[1] Blink");
                lore.add(ChatColor.GRAY + "- Teleport forward to your target block.");
                lore.add(ChatColor.AQUA + "[2] Random Teleport");
                lore.add(ChatColor.GRAY + "- Teleport to a random nearby location.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- Increased attack reach while in Souls.");
            }
            case KING_OF_HEAT -> {
                lore.add(ChatColor.GREEN + "[1] Flame Jump");
                lore.add(ChatColor.GRAY + "- Launch upward and ignite enemies below.");
                lore.add(ChatColor.AQUA + "[2] Flamethrower " + ChatColor.DARK_GRAY + "(Special)");
                lore.add(ChatColor.GRAY + "- Cone of fire that burns and damages enemies.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + "Fire Resistance" + ChatColor.GRAY + ".");
            }
            case ARCHANGEL -> {
                lore.add(ChatColor.GREEN + "[1] Levitate");
                lore.add(ChatColor.GRAY + "- Levitate the nearest enemy.");
                lore.add(ChatColor.AQUA + "[2] Immortality " + ChatColor.DARK_GRAY + "(Special)");
                lore.add(ChatColor.GRAY + "- Become invulnerable for " + ChatColor.WHITE + "10s" + ChatColor.GRAY + ".");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- Extra midair jumps.");
            }
            case BOUNTY_HUNTER -> {
                lore.add(ChatColor.GREEN + "[1] Smoke Bomb");
                lore.add(ChatColor.GRAY + "- Blind nearby enemies briefly.");
                lore.add(ChatColor.AQUA + "[2] Vanish " + ChatColor.DARK_GRAY + "(Special)");
                lore.add(ChatColor.GRAY + "- Turn invisible (armor hidden) for " + ChatColor.WHITE + "10s" + ChatColor.GRAY + ".");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + "Speed II" + ChatColor.GRAY + " always.");
            }
            case COPYCAT -> {
                lore.add(ChatColor.GREEN + "[1] Roll the Dice");
                lore.add(ChatColor.GRAY + "- Randomly gain Strength or get Weakness.");
                lore.add(ChatColor.AQUA + "[2] Copy Special");
                lore.add(ChatColor.GRAY + "- Copy the nearest enemy's special ability.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- Increased attack speed.");
            }
            case BLOODY_MONARCH -> {
                lore.add(ChatColor.GREEN + "[1] Blood Beam");
                lore.add(ChatColor.GRAY + "- Fire a thick crimson beam that damages.");
                lore.add(ChatColor.AQUA + "[2] Royal Beam " + ChatColor.DARK_GRAY + "(Special)");
                lore.add(ChatColor.GRAY + "- Massive beam + destroys terrain at impact.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- Lifesteal: heal " + ChatColor.WHITE + "25%" + ChatColor.GRAY + " of damage dealt.");
            }
            case DARK_KNIGHT -> {
                lore.add(ChatColor.GREEN + "[1] Shadow Step");
                lore.add(ChatColor.GRAY + "- First use: set a shadow marker (15s).");
                lore.add(ChatColor.GRAY + "- Use again: warp back to the marker.");
                lore.add(ChatColor.AQUA + "[2] Backstab");
                lore.add(ChatColor.GRAY + "- Teleport behind the nearest enemy.");
                lore.add(ChatColor.GRAY + "- Apply " + ChatColor.WHITE + "Darkness" + ChatColor.GRAY + " for " + ChatColor.WHITE + "2s" + ChatColor.GRAY + ".");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + "25%" + ChatColor.GRAY + " chance to blind on hit.");
            }
            case RAILGUN -> {
                lore.add(ChatColor.GREEN + "[1] Nuke Strike");
                lore.add(ChatColor.GRAY + "- Drop a " + ChatColor.WHITE + "5x5" + ChatColor.GRAY + " grid of TNT on the nearest enemy.");
                lore.add(ChatColor.GRAY + "- TNT damage is reduced.");
                lore.add(ChatColor.AQUA + "[2] Stab Shot " + ChatColor.DARK_GRAY + "(Special)");
                lore.add(ChatColor.GRAY + "- Orbital stab barrage on the nearest enemy.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Passive");
                lore.add(ChatColor.GRAY + "- Immune to TNT explosions.");
            }
        }

        lore.add(" ");
        lore.add(ChatColor.DARK_GRAY + "Cooldowns: [1] ~15s, [2] varies by soul.");
        return lore;
    }

    private static Material materialOr(String preferred, Material fallback) {
        Material m = Material.matchMaterial(preferred);
        return m != null ? m : fallback;
    }
}

