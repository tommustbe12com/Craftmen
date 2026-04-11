package com.tommustbe12.craftmen.kit.gui;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class KitEditorMenu implements Listener {

    private static final String TITLE_SELECT_PREFIX = ChatColor.DARK_GRAY + "Kit Editor";
    private static final String TITLE_EDIT_PREFIX = ChatColor.DARK_GRAY + "Edit Kit: ";

    private static final int EDIT_SIZE = 54;

    // Match Hub selector layout
    private static final List<String> PAGE_ONE_KITS = Arrays.asList(
            "Sword", "Mace", "Axe", "Netherite Potion", "Diamond Potion", "SMP", "UHC"
    );
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.length; // 28
    private static final int SLOT_PREV = 46;
    private static final int SLOT_NEXT = 52;
    private static final int CRYSTAL_SLOT = 22;

    private static final int SLOT_HELMET = 45;
    private static final int SLOT_CHEST = 46;
    private static final int SLOT_LEGS = 47;
    private static final int SLOT_BOOTS = 48;
    private static final int SLOT_OFFHAND = 49;

    private static final int SLOT_SAVE = 51;
    private static final int SLOT_RESET = 52;
    private static final int SLOT_CLOSE = 53;

    private final KitManager kitManager;
    private final Set<UUID> suppressCloseReopen = new HashSet<>();
    private final Map<UUID, Boolean> cursorFromEditor = new HashMap<>();
    private final Map<UUID, Integer> playerPage = new HashMap<>();

    public KitEditorMenu(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    public void openSelect(Player player) {
        List<Game> games = new ArrayList<>(Craftmen.get().getGameManager().getGames());
        games.sort(Comparator.comparing(Game::getName, String.CASE_INSENSITIVE_ORDER));

        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        openSelectPage(player, games, page);
    }

    private void openSelectPage(Player player, List<Game> games, int page) {
        Collection<Game> all = games;

        List<Game> page1Games = getPage1Games(all);
        Game crystalGame = getCrystalGame(all);
        List<Game> extraGames = getExtraGames(all);

        int extraPages = (int) Math.ceil(extraGames.size() / (double) ITEMS_PER_PAGE);
        int maxPage = extraPages; // page 0 is special
        int clamped = Math.max(0, Math.min(page, maxPage));
        playerPage.put(player.getUniqueId(), clamped);

        Inventory inv = Bukkit.createInventory(
                new KitEditorHolder(KitEditorHolder.Type.SELECT, player.getUniqueId(), null),
                54,
                TITLE_SELECT_PREFIX + ChatColor.DARK_GRAY + " (Page " + (clamped + 1) + ")"
        );

        fillBorder(inv);

        inv.setItem(SLOT_PREV, makeArrow(true, clamped > 0));
        inv.setItem(SLOT_NEXT, makeArrow(false, clamped < maxPage));
        inv.setItem(SLOT_CLOSE, closeItem());

        if (clamped == 0) {
            int col = 0;
            for (Game g : page1Games) {
                if (col >= 7) break;
                inv.setItem(CONTENT_SLOTS[col], makeGameIcon(g, player));
                col++;
            }
            if (crystalGame != null) {
                inv.setItem(CRYSTAL_SLOT, makeGameIcon(crystalGame, player));
            }
        } else {
            int startIndex = (clamped - 1) * ITEMS_PER_PAGE;
            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                int idx = startIndex + i;
                if (idx >= extraGames.size()) break;
                inv.setItem(CONTENT_SLOTS[i], makeGameIcon(extraGames.get(idx), player));
            }
        }

        player.openInventory(inv);
    }

    public void openEdit(Player player, Game game, boolean defaultIfNone) {
        Kit kit = defaultIfNone
                ? kitManager.getCustomKit(player.getUniqueId(), game).orElseGet(game::createDefaultKit)
                : kitManager.getEffectiveKit(player, game);

        cursorFromEditor.put(player.getUniqueId(), false);

        Inventory inv = Bukkit.createInventory(
                new KitEditorHolder(KitEditorHolder.Type.EDIT, player.getUniqueId(), game),
                EDIT_SIZE,
                TITLE_EDIT_PREFIX + ChatColor.GREEN + game.getName()
        );

        ItemStack pane = borderPane();
        for (int i = 36; i < 45; i++) inv.setItem(i, pane);

        ItemStack[] contents = kit.getContents();
        for (int kitIndex = 0; kitIndex < 36; kitIndex++) {
            int editorSlot = editorSlotForKitIndex(kitIndex);
            inv.setItem(editorSlot, contents[kitIndex]);
        }

        ItemStack[] armor = kit.getArmor();
        inv.setItem(SLOT_HELMET, armor[0]);
        inv.setItem(SLOT_CHEST, armor[1]);
        inv.setItem(SLOT_LEGS, armor[2]);
        inv.setItem(SLOT_BOOTS, armor[3]);
        inv.setItem(SLOT_OFFHAND, kit.getOffhand());

        inv.setItem(50, helpItem());
        inv.setItem(SLOT_SAVE, saveItem());
        inv.setItem(SLOT_RESET, resetItem());
        inv.setItem(SLOT_CLOSE, closeItem());

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof KitEditorHolder holder)) return;

        if (!player.getUniqueId().equals(holder.getOwner())) {
            e.setCancelled(true);
            return;
        }

        if (holder.getType() == KitEditorHolder.Type.SELECT) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            if (isClose(clicked)) {
                player.closeInventory();
                return;
            }

            int raw = e.getRawSlot();
            if (raw == SLOT_PREV) {
                int page = playerPage.getOrDefault(player.getUniqueId(), 0);
                playerPage.put(player.getUniqueId(), Math.max(0, page - 1));
                suppressCloseReopen.add(player.getUniqueId());
                Bukkit.getScheduler().runTask(Craftmen.get(), () -> openSelect(player));
                return;
            }
            if (raw == SLOT_NEXT) {
                int page = playerPage.getOrDefault(player.getUniqueId(), 0);
                playerPage.put(player.getUniqueId(), page + 1);
                suppressCloseReopen.add(player.getUniqueId());
                Bukkit.getScheduler().runTask(Craftmen.get(), () -> openSelect(player));
                return;
            }

            Game game = gameFromIcon(clicked);
            if (game == null) return;

            suppressCloseReopen.add(player.getUniqueId());
            Bukkit.getScheduler().runTask(Craftmen.get(), () -> openEdit(player, game, true));
            return;
        }

        // EDIT
        // Never allow pulling items from the player's own inventory into the editor (anti-cheat).
        // The editor is for rearranging the existing kit only.
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(e.getView().getBottomInventory())) {
            e.setCancelled(true);
            return;
        }

        if (e.isShiftClick() || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            e.setCancelled(true);
            return;
        }

        if (e.getAction() == InventoryAction.HOTBAR_SWAP
                || e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
                || e.getAction() == InventoryAction.SWAP_WITH_CURSOR
                || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            // These can introduce items from the player's inventory/hotbar.
            e.setCancelled(true);
            return;
        }

        int raw = e.getRawSlot();
        if (raw == SLOT_SAVE) {
            e.setCancelled(true);
            Game game = holder.getGame();
            Kit kit = readKitFromEditor(e.getInventory());
            kitManager.setCustomKit(player.getUniqueId(), game, kit);
            player.sendMessage(ChatColor.GREEN + "Saved custom kit for " + game.getName() + ".");
            player.closeInventory();
            return;
        }

        if (raw == SLOT_RESET) {
            e.setCancelled(true);
            Game game = holder.getGame();
            kitManager.clearCustomKit(player.getUniqueId(), game);
            player.sendMessage(ChatColor.YELLOW + "Reset kit for " + game.getName() + " back to default.");
            suppressCloseReopen.add(player.getUniqueId());
            Bukkit.getScheduler().runTask(Craftmen.get(), () -> openEdit(player, game, true));
            return;
        }

        if (raw == SLOT_CLOSE || raw == 50) {
            e.setCancelled(true);
            player.closeInventory();
            return;
        }

        if (raw >= 0 && raw < EDIT_SIZE) {
            if (!isEditableSlot(raw)) {
                e.setCancelled(true);
                return;
            }

            ItemStack cursor = e.getCursor();
            boolean cursorHasItem = cursor != null && cursor.getType() != Material.AIR;

            // If the cursor has an item that did NOT originate from the editor, don't allow placing it.
            boolean fromEditor = cursorFromEditor.getOrDefault(player.getUniqueId(), false);
            if (cursorHasItem && !fromEditor) {
                // Allow picking up from editor (cursor was empty).
                InventoryAction action = e.getAction();
                if (action == InventoryAction.PLACE_ALL
                        || action == InventoryAction.PLACE_ONE
                        || action == InventoryAction.PLACE_SOME) {
                    e.setCancelled(true);
                    return;
                }
            }

            // Mark cursor as coming from editor if we are picking up an item from an editable slot.
            ItemStack current = e.getCurrentItem();
            boolean currentHasItem = current != null && current.getType() != Material.AIR;
            if (!cursorHasItem && currentHasItem) {
                InventoryAction action = e.getAction();
                if (action == InventoryAction.PICKUP_ALL
                        || action == InventoryAction.PICKUP_HALF
                        || action == InventoryAction.PICKUP_ONE
                        || action == InventoryAction.PICKUP_SOME) {
                    cursorFromEditor.put(player.getUniqueId(), true);
                }
            }

            // If we just placed the last item (cursor becomes empty), clear the flag next tick.
            Bukkit.getScheduler().runTask(Craftmen.get(), () -> {
                ItemStack c = player.getItemOnCursor();
                boolean has = c != null && c.getType() != Material.AIR;
                if (!has) cursorFromEditor.put(player.getUniqueId(), false);
            });

            return; // allow rearranging inside editable slots
        }

        // Anything else (including outside clicks) is blocked while editing.
        e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof KitEditorHolder holder)) return;

        if (holder.getType() != KitEditorHolder.Type.EDIT) {
            e.setCancelled(true);
            return;
        }

        if (!player.getUniqueId().equals(holder.getOwner())) {
            e.setCancelled(true);
            return;
        }

        boolean fromEditor = cursorFromEditor.getOrDefault(player.getUniqueId(), false);
        ItemStack cursor = e.getOldCursor();
        boolean cursorHasItem = cursor != null && cursor.getType() != Material.AIR;

        for (int raw : e.getRawSlots()) {
            if (raw >= 0 && raw < EDIT_SIZE && !isEditableSlot(raw)) {
                e.setCancelled(true);
                return;
            }
            if (raw >= 0 && raw < EDIT_SIZE && isEditableSlot(raw) && cursorHasItem && !fromEditor) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof KitEditorHolder holder)) return;
        if (holder.getType() != KitEditorHolder.Type.EDIT) return;

        cursorFromEditor.remove(player.getUniqueId());

        // If we intentionally closed to reopen (reset/select), don't spam.
        if (suppressCloseReopen.remove(player.getUniqueId())) return;

        player.sendMessage(ChatColor.GRAY + "Tip: click " + ChatColor.GREEN + "Save" + ChatColor.GRAY + " to keep your changes.");
    }

    private static void sanitizeEditor(Inventory inv) {
        // Border row (36-44) must stay panes.
        for (int i = 36; i < 45; i++) inv.setItem(i, borderPane());
        // Buttons must stay buttons.
        inv.setItem(50, helpItem());
        inv.setItem(SLOT_SAVE, saveItem());
        inv.setItem(SLOT_RESET, resetItem());
        inv.setItem(SLOT_CLOSE, closeItem());
    }

    private static int firstEmptyEditable(Inventory inv) {
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) return i;
        }
        for (int i : new int[]{SLOT_HELMET, SLOT_CHEST, SLOT_LEGS, SLOT_BOOTS, SLOT_OFFHAND}) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) return i;
        }
        return -1;
    }

    private static boolean isEditableSlot(int raw) {
        if (raw >= 0 && raw < 36) return true;
        return raw == SLOT_HELMET || raw == SLOT_CHEST || raw == SLOT_LEGS || raw == SLOT_BOOTS || raw == SLOT_OFFHAND;
    }

    private Kit readKitFromEditor(Inventory inv) {
        ItemStack[] contents = new ItemStack[36];
        for (int kitIndex = 0; kitIndex < 36; kitIndex++) {
            int editorSlot = editorSlotForKitIndex(kitIndex);
            ItemStack item = inv.getItem(editorSlot);
            contents[kitIndex] = item == null ? null : item.clone();
        }

        ItemStack[] armor = new ItemStack[4];
        armor[0] = cloneOrNull(inv.getItem(SLOT_HELMET));
        armor[1] = cloneOrNull(inv.getItem(SLOT_CHEST));
        armor[2] = cloneOrNull(inv.getItem(SLOT_LEGS));
        armor[3] = cloneOrNull(inv.getItem(SLOT_BOOTS));

        ItemStack offhand = cloneOrNull(inv.getItem(SLOT_OFFHAND));
        return new Kit(contents, armor, offhand);
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        return item.clone();
    }

    private ItemStack makeGameIcon(Game game, Player player) {
        ItemStack icon = game.getIcon().clone();
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + game.getName());
        List<String> lore = new ArrayList<>();
        if (kitManager.getCustomKit(player.getUniqueId(), game).isPresent()) {
            lore.add(ChatColor.AQUA + "Custom kit: " + ChatColor.GREEN + "Yes");
        } else {
            lore.add(ChatColor.AQUA + "Custom kit: " + ChatColor.RED + "No");
        }
        lore.add(ChatColor.GRAY + "Click to edit");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private static Game gameFromIcon(ItemStack stack) {
        if (!stack.hasItemMeta()) return null;
        String name = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
        if (name == null) return null;
        return Craftmen.get().getGameManager().getGame(name);
    }

    private static ItemStack borderPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack helpItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "How to edit");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Drag items around to reorder.",
                ChatColor.GRAY + "You cannot add new items here.",
                ChatColor.GRAY + "Armor slots are at the bottom-left.",
                ChatColor.GRAY + "Offhand slot is next to armor.",
                ChatColor.GRAY + "Click Save when done."
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack saveItem() {
        ItemStack item = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Save");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack resetItem() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Reset to Default");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack closeItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Close");
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isClose(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return name != null && name.equalsIgnoreCase("Close");
    }

    // Editor layout: show main inventory (9-35) first (top 3 rows), then hotbar (0-8) on the bottom row.
    private static int editorSlotForKitIndex(int kitIndex) {
        if (kitIndex >= 9) return kitIndex - 9; // 9..35 -> 0..26
        return 27 + kitIndex; // 0..8 -> 27..35
    }

    private static List<Game> getPage1Games(Collection<Game> all) {
        List<Game> result = new ArrayList<>();
        for (String name : PAGE_ONE_KITS) {
            for (Game g : all) {
                if (g.getName().equalsIgnoreCase(name)) {
                    result.add(g);
                    break;
                }
            }
        }
        return result;
    }

    private static Game getCrystalGame(Collection<Game> all) {
        for (Game g : all) {
            if (g.getName().equalsIgnoreCase("Crystal")) return g;
        }
        return null;
    }

    private static List<Game> getExtraGames(Collection<Game> all) {
        Set<String> reserved = new HashSet<>();
        for (String s : PAGE_ONE_KITS) reserved.add(s.toLowerCase(Locale.ROOT));
        reserved.add("crystal");

        List<Game> result = new ArrayList<>();
        for (Game g : all) {
            if (!reserved.contains(g.getName().toLowerCase(Locale.ROOT))) result.add(g);
        }
        return result;
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

    private static ItemStack makeArrow(boolean left, boolean enabled) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        if (enabled) {
            meta.setDisplayName((left ? ChatColor.YELLOW + "◀ Previous" : ChatColor.YELLOW + "Next ▶"));
        } else {
            meta.setDisplayName(ChatColor.GRAY + (left ? "Previous" : "Next"));
        }
        arrow.setItemMeta(meta);
        return arrow;
    }
}
