package com.tommustbe12.craftmen.customkit.gui;

import com.tommustbe12.craftmen.customkit.CustomKit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;

import java.util.*;

public final class CustomKitMenus {

    public static final String TITLE_BROWSER_PREFIX = ChatColor.DARK_GRAY + "Player Kits";
    public static final String TITLE_ITEM_PICKER_PREFIX = ChatColor.DARK_GRAY + "Pick Items";
    public static final String TITLE_ENCHANT_PREFIX = ChatColor.DARK_GRAY + "Enchant Item";

    public static final String TOOL_ITEM_BROWSER_NAME = ChatColor.GOLD + "Item Browser";
    public static final String TOOL_ENCHANT_NAME = ChatColor.LIGHT_PURPLE + "Enchant Tool";
    public static final String TOOL_SAVE_NAME = ChatColor.GREEN + "Save & Publish";
    public static final String TOOL_CANCEL_NAME = ChatColor.RED + "Cancel";

    private static final NamespacedKey KIT_ID_KEY = new NamespacedKey("craftmen", "custom_kit_id");
    private static final NamespacedKey ENCHANT_KEY = new NamespacedKey("craftmen", "custom_enchant_key");

    private CustomKitMenus() {}

    public static ItemStack borderPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    public static void fillBorder(Inventory inv) {
        ItemStack pane = borderPane();
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, pane);
            inv.setItem(row * 9 + 8, pane);
        }
    }

    public static ItemStack arrow(boolean left, boolean enabled) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.setDisplayName(enabled
                ? (left ? ChatColor.YELLOW + "◀ Previous" : ChatColor.YELLOW + "Next ▶")
                : ChatColor.GRAY + (left ? "Previous" : "Next"));
        arrow.setItemMeta(meta);
        return arrow;
    }

    public static ItemStack close() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Close");
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createNew() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Create New Kit");
        meta.setLore(List.of(
                ChatColor.GRAY + "Make a kit from scratch:",
                ChatColor.GRAY + "pick any items, add enchants,",
                ChatColor.GRAY + "arrange your inventory, then save."
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack kitIcon(CustomKit kit, int activePlayers) {
        ItemStack icon = new ItemStack(Material.CHEST);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + kit.getName());
        meta.setLore(List.of(
                ChatColor.GRAY + "By: " + ChatColor.YELLOW + kit.getCreatorName(),
                ChatColor.GRAY + "Playing: " + ChatColor.GREEN + activePlayers,
                ChatColor.GRAY + "Total plays: " + ChatColor.GREEN + kit.getTotalPlays(),
                ChatColor.GRAY + "Click to queue"
        ));
        meta.getPersistentDataContainer().set(KIT_ID_KEY, PersistentDataType.STRING, kit.getId().toString());
        icon.setItemMeta(meta);
        icon.setAmount(Math.max(1, Math.min(activePlayers, 99)));
        return icon;
    }

    public static ItemStack search() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Search");
        meta.setLore(List.of(ChatColor.GRAY + "Click then type in chat."));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack showAllToggle(boolean showAll) {
        ItemStack item = new ItemStack(showAll ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Show All Items: " + (showAll ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        meta.setLore(List.of(
                ChatColor.GRAY + "OFF = PvP-focused list",
                ChatColor.GRAY + "ON = every item"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack itemBrowserTool() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(TOOL_ITEM_BROWSER_NAME);
        meta.setLore(List.of(ChatColor.GRAY + "Right-click to pick any item."));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack enchantTool() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(TOOL_ENCHANT_NAME);
        meta.setLore(List.of(ChatColor.GRAY + "Right-click to enchant held item."));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.LUCK_OF_THE_SEA, 1);
        return item;
    }

    public static ItemStack saveTool() {
        ItemStack item = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(TOOL_SAVE_NAME);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack cancelTool() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(TOOL_CANCEL_NAME);
        item.setItemMeta(meta);
        return item;
    }

    public static Inventory createBrowser(Player player, int page, int maxPage, List<CustomKit> kits, Map<UUID, Integer> activeCounts) {
        Inventory inv = Bukkit.createInventory(new CustomKitHolder(CustomKitHolder.Type.BROWSER, player.getUniqueId(), page), 54,
                TITLE_BROWSER_PREFIX + ChatColor.DARK_GRAY + " (Page " + (page + 1) + "/" + (maxPage + 1) + ")");
        fillBorder(inv);

        inv.setItem(45, createNew());
        inv.setItem(46, arrow(true, page > 0));
        inv.setItem(49, close());
        inv.setItem(52, arrow(false, page < maxPage));

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int start = page * slots.length;
        for (int i = 0; i < slots.length; i++) {
            int idx = start + i;
            if (idx >= kits.size()) break;
            CustomKit kit = kits.get(idx);
            int active = activeCounts.getOrDefault(kit.getId(), 0);
            inv.setItem(slots[i], kitIcon(kit, active));
        }

        return inv;
    }

    public static Inventory createItemPicker(Player player, int page, int maxPage, List<Material> materials, String search) {
        String title = TITLE_ITEM_PICKER_PREFIX + ChatColor.DARK_GRAY + " (Page " + (page + 1) + "/" + (maxPage + 1) + ")";
        if (search != null && !search.isBlank()) title += ChatColor.GRAY + " [" + search + "]";

        Inventory inv = Bukkit.createInventory(new CustomKitHolder(CustomKitHolder.Type.ITEM_PICKER, player.getUniqueId(), page), 54, title);
        fillBorder(inv);

        inv.setItem(45, search());
        inv.setItem(46, arrow(true, page > 0));
        inv.setItem(49, close());
        inv.setItem(52, arrow(false, page < maxPage));
        // slot 53 reserved for toggle button (set by manager based on per-player state)

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int start = page * slots.length;
        for (int i = 0; i < slots.length; i++) {
            int idx = start + i;
            if (idx >= materials.size()) break;
            Material mat = materials.get(idx);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = Bukkit.getItemFactory().getItemMeta(mat);
            if (meta == null) {
                // Some materials may not have item meta on certain server versions.
                continue;
            }
            meta.setDisplayName(ChatColor.GREEN + prettify(mat.name()));
            meta.setLore(List.of(ChatColor.GRAY + "Click to add", ChatColor.DARK_GRAY + mat.name()));
            item.setItemMeta(meta);
            inv.setItem(slots[i], item);
        }

        return inv;
    }

    public static Inventory createEnchantMenu(Player player, ItemStack item, List<Enchantment> applicable) {
        Inventory inv = Bukkit.createInventory(new CustomKitHolder(CustomKitHolder.Type.ENCHANT, player.getUniqueId(), 0), 54, TITLE_ENCHANT_PREFIX);
        fillBorder(inv);
        inv.setItem(49, close());
        inv.setItem(45, enchantHelp());

        ItemStack preview = item == null ? new ItemStack(Material.BARRIER) : item.clone();
        ItemMeta pMeta = preview.getItemMeta();
        if (pMeta != null) {
            pMeta.setLore(List.of(
                    ChatColor.GRAY + "Left-click: +1 level",
                    ChatColor.GRAY + "Right-click: -1 level",
                    ChatColor.GRAY + "Shift-click: remove"
            ));
            preview.setItemMeta(pMeta);
        }
        inv.setItem(4, preview);

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        Map<Enchantment, Integer> current = currentEnchants(item);
        for (int i = 0; i < slots.length; i++) {
            if (i >= applicable.size()) break;
            Enchantment ench = applicable.get(i);
            int level = current.getOrDefault(ench, 0);
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = book.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + prettify(ench.getKey().getKey()) + ChatColor.GRAY + " ("
                    + (level == 0 ? "none" : ("lvl " + level)) + ")");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Max: " + ench.getMaxLevel(),
                    ChatColor.GRAY + "Click to change"
            ));
            meta.getPersistentDataContainer().set(ENCHANT_KEY, PersistentDataType.STRING, ench.getKey().toString());
            book.setItemMeta(meta);
            inv.setItem(slots[i], book);
        }

        return inv;
    }

    public static UUID readKitId(ItemMeta meta) {
        if (meta == null) return null;
        String raw = meta.getPersistentDataContainer().get(KIT_ID_KEY, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String readEnchantKey(ItemMeta meta) {
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(ENCHANT_KEY, PersistentDataType.STRING);
    }

    private static ItemStack enchantHelp() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "How to enchant");
        meta.setLore(List.of(
                ChatColor.GRAY + "Hold the item you want to enchant.",
                ChatColor.GRAY + "Left-click enchant: +1 level",
                ChatColor.GRAY + "Right-click enchant: -1 level",
                ChatColor.GRAY + "Shift-click enchant: remove",
                ChatColor.DARK_GRAY + "Conflicts are blocked (Sharpness vs Smite etc.)"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static Map<Enchantment, Integer> currentEnchants(ItemStack item) {
        if (item == null) return Map.of();
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta esm) {
            return new HashMap<>(esm.getStoredEnchants());
        }
        return new HashMap<>(item.getEnchantments());
    }

    private static String prettify(String raw) {
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
