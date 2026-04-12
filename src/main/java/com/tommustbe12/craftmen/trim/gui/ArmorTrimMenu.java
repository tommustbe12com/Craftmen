package com.tommustbe12.craftmen.trim.gui;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.trim.ArmorSlot;
import com.tommustbe12.craftmen.trim.ArmorTrimManager;
import com.tommustbe12.craftmen.trim.ArmorTrimSelection;
import com.tommustbe12.craftmen.cosmetics.CosmeticsShop;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;

public final class ArmorTrimMenu implements Listener {

    private static final String TITLE_MAIN = ChatColor.DARK_GRAY + "Armor Trims";
    private static final String TITLE_PATTERN_PREFIX = ChatColor.DARK_GRAY + "Trim Pattern: ";
    private static final String TITLE_MATERIAL_PREFIX = ChatColor.DARK_GRAY + "Trim Material: ";

    private static final int SIZE = 54;

    private static final int SLOT_HELMET = 20;
    private static final int SLOT_CHEST = 21;
    private static final int SLOT_LEGS = 23;
    private static final int SLOT_BOOTS = 24;

    private static final int SLOT_CLEAR = 40;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_BACK = 45;

    private static final int SLOT_PREV = 46;
    private static final int SLOT_NEXT = 52;

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.length;

    private final ArmorTrimManager trims;
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final org.bukkit.NamespacedKey keyPattern;
    private final org.bukkit.NamespacedKey keyMaterial;

    public ArmorTrimMenu(ArmorTrimManager trims) {
        this.trims = trims;
        this.keyPattern = new org.bukkit.NamespacedKey(Craftmen.get(), "trim_pattern");
        this.keyMaterial = new org.bukkit.NamespacedKey(Craftmen.get(), "trim_material");
    }

    public void openMain(Player player) {
        if (!canUse(player)) return;
        Inventory inv = Bukkit.createInventory(new TrimMenuHolder(TrimMenuHolder.Type.MAIN, player.getUniqueId(), null, null), SIZE, TITLE_MAIN);
        fillBorder(inv);

        inv.setItem(SLOT_HELMET, armorSlotItem(player, ArmorSlot.HELMET, Material.DIAMOND_HELMET));
        inv.setItem(SLOT_CHEST, armorSlotItem(player, ArmorSlot.CHESTPLATE, Material.DIAMOND_CHESTPLATE));
        inv.setItem(SLOT_LEGS, armorSlotItem(player, ArmorSlot.LEGGINGS, Material.DIAMOND_LEGGINGS));
        inv.setItem(SLOT_BOOTS, armorSlotItem(player, ArmorSlot.BOOTS, Material.DIAMOND_BOOTS));

        inv.setItem(SLOT_CLOSE, closeItem());
        player.openInventory(inv);
    }

    private ItemStack armorSlotItem(Player player, ArmorSlot slot, Material iconMat) {
        ItemStack item = new ItemStack(iconMat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + nice(slot));

        ArmorTrimSelection sel = trims.getSelections(player.getUniqueId()).get(slot);
        List<String> lore = new ArrayList<>();
        if (sel == null) {
            lore.add(ChatColor.GRAY + "Current: " + ChatColor.RED + "None");
        } else {
            lore.add(ChatColor.GRAY + "Current: " + ChatColor.GREEN + stripNamespace(sel.patternKey()) + ChatColor.DARK_GRAY + " / " + ChatColor.GREEN + stripNamespace(sel.materialKey()));
        }
        lore.add(ChatColor.YELLOW + "Click to set");
        meta.setLore(lore);
        item.setItemMeta(meta);
        if (sel != null) applyTrimPreview(item, sel);
        return item;
    }

    private void openPattern(Player player, ArmorSlot slot, int page) {
        if (!canUse(player)) return;
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        List<TrimPattern> patterns = new ArrayList<>();
        for (TrimPattern p : Registry.TRIM_PATTERN) patterns.add(p);
        patterns.sort(Comparator.comparing(p -> keyOf(p).toString()));

        int maxPage = (int) Math.max(0, Math.ceil(patterns.size() / (double) ITEMS_PER_PAGE) - 1);
        int clamped = Math.max(0, Math.min(page, maxPage));
        pages.put(player.getUniqueId(), clamped);

        Inventory inv = Bukkit.createInventory(new TrimMenuHolder(TrimMenuHolder.Type.PATTERN, player.getUniqueId(), slot, null), SIZE,
                TITLE_PATTERN_PREFIX + ChatColor.AQUA + nice(slot) + ChatColor.DARK_GRAY + " (Page " + (clamped + 1) + ")");
        fillBorder(inv);

        inv.setItem(SLOT_BACK, backItem());
        inv.setItem(SLOT_PREV, arrowItem(true, clamped > 0));
        inv.setItem(SLOT_NEXT, arrowItem(false, clamped < maxPage));
        inv.setItem(SLOT_CLOSE, closeItem());

        int start = clamped * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= patterns.size()) break;
            TrimPattern p = patterns.get(idx);
            inv.setItem(CONTENT_SLOTS[i], patternItemWithKey(p, profile));
        }

        player.openInventory(inv);
    }

    private void openMaterial(Player player, ArmorSlot slot, String patternKey, int page) {
        if (!canUse(player)) return;
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        List<TrimMaterial> materials = new ArrayList<>();
        for (TrimMaterial m : Registry.TRIM_MATERIAL) materials.add(m);
        materials.sort(Comparator.comparing(m -> keyOf(m).toString()));

        int maxPage = (int) Math.max(0, Math.ceil(materials.size() / (double) ITEMS_PER_PAGE) - 1);
        int clamped = Math.max(0, Math.min(page, maxPage));
        pages.put(player.getUniqueId(), clamped);

        Inventory inv = Bukkit.createInventory(new TrimMenuHolder(TrimMenuHolder.Type.MATERIAL, player.getUniqueId(), slot, patternKey), SIZE,
                TITLE_MATERIAL_PREFIX + ChatColor.AQUA + nice(slot) + ChatColor.DARK_GRAY + " (Page " + (clamped + 1) + ")");
        fillBorder(inv);

        inv.setItem(SLOT_BACK, backItem());
        inv.setItem(SLOT_PREV, arrowItem(true, clamped > 0));
        inv.setItem(SLOT_NEXT, arrowItem(false, clamped < maxPage));
        inv.setItem(SLOT_CLOSE, closeItem());
        inv.setItem(SLOT_CLEAR, clearItem());

        int start = clamped * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= materials.size()) break;
            TrimMaterial m = materials.get(idx);
            inv.setItem(CONTENT_SLOTS[i], materialItemWithKey(m, profile));
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof TrimMenuHolder holder)) return;
        if (!player.getUniqueId().equals(holder.getOwner())) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);
        if (!canUse(player)) {
            player.closeInventory();
            return;
        }

        int raw = e.getRawSlot();
        if (raw < 0 || raw >= SIZE) return;

        if (raw == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        if (holder.getType() == TrimMenuHolder.Type.MAIN) {
            ArmorSlot slot = switch (raw) {
                case SLOT_HELMET -> ArmorSlot.HELMET;
                case SLOT_CHEST -> ArmorSlot.CHESTPLATE;
                case SLOT_LEGS -> ArmorSlot.LEGGINGS;
                case SLOT_BOOTS -> ArmorSlot.BOOTS;
                default -> null;
            };
            if (slot == null) return;
            openPattern(player, slot, 0);
            return;
        }

        if (raw == SLOT_BACK) {
            if (holder.getType() == TrimMenuHolder.Type.PATTERN) {
                openMain(player);
            } else {
                openPattern(player, holder.getSlot(), 0);
            }
            return;
        }

        if (raw == SLOT_PREV) {
            int page = pages.getOrDefault(player.getUniqueId(), 0);
            page = Math.max(0, page - 1);
            pages.put(player.getUniqueId(), page);
            if (holder.getType() == TrimMenuHolder.Type.PATTERN) openPattern(player, holder.getSlot(), page);
            else openMaterial(player, holder.getSlot(), holder.getPatternKey(), page);
            return;
        }

        if (raw == SLOT_NEXT) {
            int page = pages.getOrDefault(player.getUniqueId(), 0);
            page = page + 1;
            pages.put(player.getUniqueId(), page);
            if (holder.getType() == TrimMenuHolder.Type.PATTERN) openPattern(player, holder.getSlot(), page);
            else openMaterial(player, holder.getSlot(), holder.getPatternKey(), page);
            return;
        }

        if (holder.getType() == TrimMenuHolder.Type.PATTERN) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String key = clicked.getItemMeta().getPersistentDataContainer().get(keyPattern, org.bukkit.persistence.PersistentDataType.STRING);
            if (key == null) return;
            // Buy pattern (10 gems) if not owned
            Profile profile = Craftmen.get().getProfileManager().getProfile(player);
            String cosmeticId = "trim.pattern." + key;
            if (!profile.hasCosmetic(cosmeticId)) {
                if (!CosmeticsShop.purchase(player, cosmeticId, 10)) return;
            }
            openMaterial(player, holder.getSlot(), key, 0);
            return;
        }

        if (holder.getType() == TrimMenuHolder.Type.MATERIAL) {
            if (raw == SLOT_CLEAR) {
                trims.clearSelection(player.getUniqueId(), holder.getSlot());
                player.sendMessage(ChatColor.YELLOW + "Cleared trim for " + nice(holder.getSlot()) + ".");
                openMain(player);
                return;
            }

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String matKey = clicked.getItemMeta().getPersistentDataContainer().get(keyMaterial, org.bukkit.persistence.PersistentDataType.STRING);
            if (matKey == null) return;

            String patternKey = holder.getPatternKey();
            // Buy material (10 gems) if not owned
            Profile profile = Craftmen.get().getProfileManager().getProfile(player);
            String cosmeticId = "trim.material." + matKey;
            if (!profile.hasCosmetic(cosmeticId)) {
                if (!CosmeticsShop.purchase(player, cosmeticId, 10)) return;
            }
            ArmorTrimSelection sel = new ArmorTrimSelection(patternKey, matKey);
            trims.setSelection(player.getUniqueId(), holder.getSlot(), sel);
            player.sendMessage(ChatColor.GREEN + "Saved trim for " + nice(holder.getSlot()) + ".");
            openMain(player);
        }
    }

    private boolean canUse(Player player) {
        return Craftmen.get().getProfileManager().getProfile(player).getState() == PlayerState.LOBBY;
    }

    private static String nice(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> "Helmet";
            case CHESTPLATE -> "Chestplate";
            case LEGGINGS -> "Leggings";
            case BOOTS -> "Boots";
        };
    }

    private static ItemStack patternItem(TrimPattern pattern) {
        ItemStack item = new ItemStack(patternTemplateMaterial(pattern));
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = keyOf(pattern);
        meta.setDisplayName(ChatColor.GREEN + stripNamespace(key.toString()));
        meta.setLore(List.of(ChatColor.GRAY + "Click to choose material"));
        // key set by caller (needs plugin instance)
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack materialItem(TrimMaterial material) {
        Material icon = Material.GOLD_INGOT;
        String key = keyOf(material).toString();
        if (key.endsWith("diamond")) icon = Material.DIAMOND;
        else if (key.endsWith("netherite")) icon = Material.NETHERITE_INGOT;
        else if (key.endsWith("emerald")) icon = Material.EMERALD;
        else if (key.endsWith("amethyst")) icon = Material.AMETHYST_SHARD;
        else if (key.endsWith("quartz")) icon = Material.QUARTZ;
        else if (key.endsWith("lapis")) icon = Material.LAPIS_LAZULI;
        else if (key.endsWith("redstone")) icon = Material.REDSTONE;
        else if (key.endsWith("copper")) icon = Material.COPPER_INGOT;
        else if (key.endsWith("iron")) icon = Material.IRON_INGOT;
        else if (key.endsWith("resin")) icon = Material.RESIN_BRICK;

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + stripNamespace(key));
        meta.setLore(List.of(ChatColor.GRAY + "Click to save"));
        // key set by caller (needs plugin instance)
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack pane = borderPane();
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, pane);
            inv.setItem(row * 9 + 8, pane);
        }
    }

    private static ItemStack borderPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack closeItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Close");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack backItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "◀ Back");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack clearItem() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Clear This Piece");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack arrowItem(boolean left, boolean enabled) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (enabled) meta.setDisplayName(left ? ChatColor.YELLOW + "◀ Previous" : ChatColor.YELLOW + "Next ▶");
        else meta.setDisplayName(ChatColor.GRAY + (left ? "Previous" : "Next"));
        item.setItemMeta(meta);
        return item;
    }

    private static NamespacedKey keyOf(TrimPattern pattern) {
        NamespacedKey k = Registry.TRIM_PATTERN.getKey(pattern);
        return k == null ? NamespacedKey.minecraft("unknown") : k;
    }

    private static NamespacedKey keyOf(TrimMaterial material) {
        NamespacedKey k = Registry.TRIM_MATERIAL.getKey(material);
        return k == null ? NamespacedKey.minecraft("unknown") : k;
    }

    private static String stripNamespace(String key) {
        if (key == null) return "unknown";
        int idx = key.indexOf(':');
        return idx == -1 ? key : key.substring(idx + 1);
    }

    private static Material patternTemplateMaterial(TrimPattern pattern) {
        NamespacedKey k = keyOf(pattern);
        String path = k.getKey(); // e.g. "wild"
        String matName = (path + "_armor_trim_smithing_template").toUpperCase(Locale.ROOT);
        try {
            return Material.valueOf(matName);
        } catch (IllegalArgumentException ignored) {
            return Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE;
        }
    }

    private static void applyTrimPreview(ItemStack armorItem, ArmorTrimSelection selection) {
        if (armorItem == null || selection == null) return;
        if (!(armorItem.getItemMeta() instanceof org.bukkit.inventory.meta.ArmorMeta meta)) return;

        NamespacedKey pKey = NamespacedKey.fromString(selection.patternKey());
        NamespacedKey mKey = NamespacedKey.fromString(selection.materialKey());
        if (pKey == null || mKey == null) return;

        TrimPattern pattern = Registry.TRIM_PATTERN.get(pKey);
        TrimMaterial material = Registry.TRIM_MATERIAL.get(mKey);
        if (pattern == null || material == null) return;

        meta.setTrim(new ArmorTrim(material, pattern));
        armorItem.setItemMeta(meta);
    }

    private ItemStack patternItemWithKey(TrimPattern pattern, Profile profile) {
        ItemStack item = patternItem(pattern);
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = keyOf(pattern);
        String cosmeticId = "trim.pattern." + key;
        boolean owned = profile != null && profile.hasCosmetic(cosmeticId);
        meta.setLore(trimLore(owned, true));
        meta.getPersistentDataContainer().set(keyPattern, org.bukkit.persistence.PersistentDataType.STRING, key.toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack materialItemWithKey(TrimMaterial material, Profile profile) {
        ItemStack item = materialItem(material);
        ItemMeta meta = item.getItemMeta();
        String key = keyOf(material).toString();
        String cosmeticId = "trim.material." + key;
        boolean owned = profile != null && profile.hasCosmetic(cosmeticId);
        meta.setLore(trimLore(owned, false));
        meta.getPersistentDataContainer().set(keyMaterial, org.bukkit.persistence.PersistentDataType.STRING, key);
        item.setItemMeta(meta);
        return item;
    }

    private static List<String> trimLore(boolean owned, boolean pattern) {
        if (owned) {
            return List.of(
                    "§a§lPURCHASED",
                    "§7Click to select"
            );
        }
        return List.of(
                "§c§lNOT PURCHASED",
                "§7Cost: §b10 gems",
                "§eClick to purchase " + (pattern ? "pattern" : "material")
        );
    }
}
