package com.tommustbe12.craftmen.stats.command;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TopCommand implements CommandExecutor, Listener {

    private static final int INV_SIZE = 54;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int SLOT_PREV = 45;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_CLOSE = 49;

    private static final String TITLE_PREFIX = ChatColor.DARK_GRAY + "Top: ";

    private enum Metric {
        WINS("Wins", Material.DIAMOND_SWORD),
        LOSSES("Losses", Material.SKELETON_SKULL),
        GEMS("Gems", Material.EMERALD);

        final String label;
        final Material icon;
        Metric(String label, Material icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    private record Entry(UUID uuid, int value) {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Metric metric = Metric.WINS;
        int page = 0;

        if (args.length >= 1) {
            String raw = args[0].toLowerCase(Locale.ROOT);
            if (raw.startsWith("win")) metric = Metric.WINS;
            else if (raw.startsWith("loss")) metric = Metric.LOSSES;
            else if (raw.startsWith("gem")) metric = Metric.GEMS;
        }
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1])) - 1;
            } catch (NumberFormatException ignored) {}
        }

        open(player, metric, page);
        return true;
    }

    private void open(Player player, Metric metric, int page) {
        List<Entry> entries = loadEntries(metric);
        entries.sort(Comparator.<Entry>comparingInt(e -> e.value).reversed());

        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) CONTENT_SLOTS.length));
        int clamped = Math.max(0, Math.min(page, totalPages - 1));

        String title = TITLE_PREFIX + ChatColor.GOLD + metric.label + ChatColor.GRAY + " (Page " + (clamped + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, title);
        fillBorder(inv);

        if (clamped > 0) inv.setItem(SLOT_PREV, navItem(Material.ARROW, ChatColor.YELLOW + "Prev"));
        if (clamped < totalPages - 1) inv.setItem(SLOT_NEXT, navItem(Material.ARROW, ChatColor.YELLOW + "Next"));
        inv.setItem(SLOT_CLOSE, navItem(Material.BARRIER, ChatColor.RED + "Close"));

        int start = clamped * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            int idx = start + i;
            if (idx >= entries.size()) break;
            Entry entry = entries.get(idx);
            inv.setItem(CONTENT_SLOTS[i], entryItem(metric, idx + 1, entry));
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    private static ItemStack entryItem(Metric metric, int rank, Entry entry) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(entry.uuid);
        String name = off.getName() == null ? entry.uuid.toString() : off.getName();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta sm) {
            sm.setOwningPlayer(off);
            sm.setDisplayName(ChatColor.GOLD + "#" + rank + ChatColor.WHITE + " " + name);
            sm.setLore(List.of(
                    ChatColor.GRAY + metric.label + ": " + ChatColor.GREEN + entry.value
            ));
            sm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            head.setItemMeta(sm);
        }
        return head;
    }

    private List<Entry> loadEntries(Metric metric) {
        List<Entry> out = new ArrayList<>();
        var cfg = Craftmen.get().getConfig();
        if (!cfg.contains("stats")) return out;
        var section = cfg.getConfigurationSection("stats");
        if (section == null) return out;

        for (String uuidString : section.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidString); }
            catch (IllegalArgumentException ignored) { continue; }

            String path = "stats." + uuidString + ".";
            int value = switch (metric) {
                case WINS -> cfg.getInt(path + "wins", 0);
                case LOSSES -> cfg.getInt(path + "losses", 0);
                case GEMS -> cfg.getInt(path + "gems", 0);
            };
            out.add(new Entry(uuid, value));
        }
        return out;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getView() == null || e.getView().getTitle() == null) return;
        String title = e.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) return;
        e.setCancelled(true);

        int raw = e.getRawSlot();
        if (raw == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (raw != SLOT_PREV && raw != SLOT_NEXT) return;

        Metric metric = Metric.WINS;
        String stripped = ChatColor.stripColor(title);
        if (stripped != null) {
            if (stripped.toLowerCase(Locale.ROOT).contains("loss")) metric = Metric.LOSSES;
            else if (stripped.toLowerCase(Locale.ROOT).contains("gem")) metric = Metric.GEMS;
        }

        int curPage = parsePage(title) - 1;
        int next = raw == SLOT_PREV ? Math.max(0, curPage - 1) : (curPage + 1);
        open(player, metric, next);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().startsWith(TITLE_PREFIX)) return;
        e.setCancelled(true);
    }

    private static int parsePage(String title) {
        // "... (Page X/Y)"
        String stripped = ChatColor.stripColor(title);
        if (stripped == null) return 1;
        int idx = stripped.toLowerCase(Locale.ROOT).lastIndexOf("page");
        if (idx < 0) return 1;
        int open = stripped.indexOf('(', idx);
        int slash = stripped.indexOf('/', idx);
        if (slash < 0) return 1;
        int start = stripped.lastIndexOf(' ', slash);
        if (start < 0) return 1;
        try {
            return Integer.parseInt(stripped.substring(start + 1, slash).trim());
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static ItemStack navItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }
    }
}

