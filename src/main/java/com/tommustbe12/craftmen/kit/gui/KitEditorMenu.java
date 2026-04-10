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

    private static final String TITLE_SELECT = ChatColor.DARK_GRAY + "Kit Editor";
    private static final String TITLE_EDIT_PREFIX = ChatColor.DARK_GRAY + "Edit Kit: ";

    private static final int EDIT_SIZE = 54;

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

    public KitEditorMenu(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    public void openSelect(Player player) {
        List<Game> games = new ArrayList<>(Craftmen.get().getGameManager().getGames());
        games.sort(Comparator.comparing(Game::getName, String.CASE_INSENSITIVE_ORDER));

        Inventory inv = Bukkit.createInventory(
                new KitEditorHolder(KitEditorHolder.Type.SELECT, player.getUniqueId(), null),
                54,
                TITLE_SELECT
        );

        ItemStack pane = borderPane();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);

        int slot = 10;
        int col = 0;
        int row = 1;
        for (Game game : games) {
            int s = row * 9 + col + 1;
            inv.setItem(s, makeGameIcon(game, player));
            col++;
            if (col >= 7) {
                col = 0;
                row++;
                if (row >= 5) break;
            }
            slot++;
        }

        inv.setItem(49, closeItem());
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
        for (int i = 0; i < 36; i++) inv.setItem(i, contents[i]);

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
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            contents[i] = item == null ? null : item.clone();
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
                ChatColor.GRAY + "Use your inventory to add items.",
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
}
