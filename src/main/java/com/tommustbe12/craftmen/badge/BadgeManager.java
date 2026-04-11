package com.tommustbe12.craftmen.badge;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.badge.gui.BadgeHolder;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BadgeManager implements Listener {

    private static final String TITLE_PLAYER = ChatColor.DARK_GRAY + "Badges";
    private static final String TITLE_ADMIN = ChatColor.DARK_GRAY + "Badge Admin";

    private final Craftmen plugin;
    private final BadgeStorage storage;
    private final Map<UUID, BadgeDefinition> badges = new HashMap<>();

    private final NamespacedKey badgeIdKey = new NamespacedKey("craftmen", "badge_id");

    private final Set<UUID> awaitingCreateName = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> draftName = new ConcurrentHashMap<>();
    private final Set<UUID> awaitingCreateIcon = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> draftIcon = new ConcurrentHashMap<>();
    private final Set<UUID> awaitingCreateReq = ConcurrentHashMap.newKeySet();
    private final Set<UUID> awaitingCreateRank = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> draftReq = new ConcurrentHashMap<>();

    public BadgeManager(Craftmen plugin) {
        this.plugin = plugin;
        this.storage = new BadgeStorage(plugin);
        reload();
    }

    public void reload() {
        badges.clear();
        for (BadgeDefinition b : storage.loadAll()) {
            badges.put(b.getId(), b);
        }
    }

    public Collection<BadgeDefinition> getBadges() {
        return Collections.unmodifiableCollection(badges.values());
    }

    public void openPlayer(Player player) {
        if (player == null) return;
        player.openInventory(buildPlayerMenu(player));
    }

    public void openAdmin(Player player) {
        if (player == null) return;
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Ops only.");
            return;
        }
        player.openInventory(buildAdminMenu(player));
    }

    private Inventory buildPlayerMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new BadgeHolder(BadgeHolder.Type.PLAYER, player.getUniqueId()), 54, TITLE_PLAYER);
        fillBorder(inv);

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        UUID selected = profile == null ? null : profile.getSelectedBadgeId();

        List<BadgeDefinition> list = new ArrayList<>(badges.values());
        list.sort(Comparator.comparing(BadgeDefinition::getName, String.CASE_INSENSITIVE_ORDER));

        int[] slots = contentSlots();
        int i = 0;
        for (BadgeDefinition badge : list) {
            if (i >= slots.length) break;
            inv.setItem(slots[i++], playerBadgeItem(profile, badge, selected));
        }

        inv.setItem(49, closeItem());
        inv.setItem(45, clearItem());
        return inv;
    }

    private Inventory buildAdminMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new BadgeHolder(BadgeHolder.Type.ADMIN, player.getUniqueId()), 54, TITLE_ADMIN);
        fillBorder(inv);

        List<BadgeDefinition> list = new ArrayList<>(badges.values());
        list.sort(Comparator.comparing(BadgeDefinition::getName, String.CASE_INSENSITIVE_ORDER));

        int[] slots = contentSlots();
        int i = 0;
        for (BadgeDefinition badge : list) {
            if (i >= slots.length) break;
            inv.setItem(slots[i++], adminBadgeItem(badge));
        }

        inv.setItem(45, createItem());
        inv.setItem(49, closeItem());
        inv.setItem(53, reloadItem());
        inv.setItem(46, helpItem());
        return inv;
    }

    private ItemStack playerBadgeItem(Profile profile, BadgeDefinition badge, UUID selected) {
        boolean available = false;
        boolean parsedOk = false;
        String progress = null;
        if (profile != null) {
            Optional<BadgeRequirement> req = BadgeRequirement.parse(badge.getRequirement());
            parsedOk = req.isPresent();
            available = req.map(r -> r.matches(profile)).orElse(false);
            progress = req.map(r -> progressLines(profile, r)).orElse(null);
        }

        // Locked badges should look disabled.
        ItemStack item = new ItemStack(available ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((available ? ChatColor.AQUA : ChatColor.DARK_GRAY) + badge.getIcon() + " " + badge.getName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Requirement: " + ChatColor.WHITE + badge.getRequirement());
        if (progress != null) {
            lore.add(ChatColor.GRAY + "Progress:");
            for (String line : progress.split("\n")) lore.add(line);
        }
        if (badge.getRank() != null && !badge.getRank().isBlank()) {
            lore.add(ChatColor.GRAY + "Rank: " + ChatColor.WHITE + badge.getRank());
        }
        if (!parsedOk) lore.add(ChatColor.RED + "Invalid requirement string.");
        lore.add(available ? (ChatColor.GREEN + "Unlocked") : (ChatColor.RED + "Locked"));
        if (selected != null && selected.equals(badge.getId())) lore.add(ChatColor.YELLOW + "Selected");
        lore.add(available ? (ChatColor.GRAY + "Click to select") : (ChatColor.GRAY + "How to get it: meet the requirement"));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(badgeIdKey, PersistentDataType.STRING, badge.getId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack adminBadgeItem(BadgeDefinition badge) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + badge.getIcon() + " " + badge.getName());
        meta.setLore(List.of(
                ChatColor.GRAY + "Req: " + ChatColor.WHITE + badge.getRequirement(),
                ChatColor.GRAY + "Left-click: edit (recreate)",
                ChatColor.GRAY + "Shift-right: delete"
        ));
        meta.getPersistentDataContainer().set(badgeIdKey, PersistentDataType.STRING, badge.getId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, pane);
            inv.setItem(row * 9 + 8, pane);
        }
    }

    private int[] contentSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    private ItemStack closeItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Close");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack clearItem() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Clear Badge");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Create Badge");
        meta.setLore(List.of(ChatColor.GRAY + "Click, then answer in chat."));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack reloadItem() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Reload");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack helpItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Requirement Help");
        meta.setLore(List.of(
                ChatColor.GRAY + "Examples:",
                ChatColor.WHITE + "wins>=10&losses<5",
                ChatColor.WHITE + "ffa_kills>=20&ffa_deaths<10",
                ChatColor.WHITE + "game.crystalWins>=300",
                ChatColor.GRAY + "Ops: create via chat prompts."
        ));
        item.setItemMeta(meta);
        return item;
    }

    private UUID readBadgeId(ItemMeta meta) {
        if (meta == null) return null;
        String raw = meta.getPersistentDataContainer().get(badgeIdKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void beginCreate(Player player) {
        awaitingCreateName.add(player.getUniqueId());
        player.closeInventory();
        draftName.remove(player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "Create badge: type name (or 'cancel').");
    }

    private void handleCreateName(Player player, String msg) {
        UUID uuid = player.getUniqueId();
        if (msg.equalsIgnoreCase("cancel")) return;
        draftIcon.remove(uuid);
        awaitingCreateIcon.add(uuid);
        draftName.put(uuid, msg);
        player.sendMessage(ChatColor.YELLOW + "Type the badge icon character (unicode emoji), or 'cancel'.");
    }

    private void handleCreateIcon(Player player, String msg) {
        UUID uuid = player.getUniqueId();
        if (msg.equalsIgnoreCase("cancel")) return;
        draftIcon.put(uuid, msg);
        awaitingCreateReq.add(uuid);
        player.sendMessage(ChatColor.YELLOW + "Type requirement (e.g. wins>=10&losses<5), or 'cancel'.");
    }

    private void handleCreateReq(Player player, String msg) {
        UUID uuid = player.getUniqueId();
        if (msg.equalsIgnoreCase("cancel")) return;

        String name = draftName.get(uuid);
        String icon = draftIcon.get(uuid);
        if (name == null || icon == null) return;

        Optional<BadgeRequirement> parsed = BadgeRequirement.parse(msg);
        if (parsed.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Invalid requirement string. Try again.");
            awaitingCreateReq.add(uuid);
            return;
        }
        draftReq.put(uuid, msg.trim());
        awaitingCreateRank.add(uuid);
        player.sendMessage(ChatColor.YELLOW + "Type rank name to give on select (used for /rank give <player> <rank>). Type 'none' for no rank.");
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return ChatColor.stripColor(s).replaceAll("[\\r\\n\\t]", " ").trim();
    }

    private void handleCreateRank(Player player, String msg) {
        UUID uuid = player.getUniqueId();
        if (msg.equalsIgnoreCase("cancel")) return;

        String name = draftName.get(uuid);
        String icon = draftIcon.get(uuid);
        String req = draftReq.get(uuid);
        if (name == null || icon == null || req == null) return;

        String rank = msg.equalsIgnoreCase("none") ? "" : sanitize(msg);
        if (!rank.isBlank() && !rank.matches("[A-Za-z0-9_\\-]{1,32}")) {
            player.sendMessage(ChatColor.RED + "Rank must be 1-32 chars: letters/numbers/_/-. Try again or type 'none'.");
            awaitingCreateRank.add(uuid);
            return;
        }

        UUID id = UUID.randomUUID();
        BadgeDefinition badge = new BadgeDefinition(id, sanitize(name), sanitize(icon), req, rank);
        badges.put(id, badge);
        storage.saveAll(badges.values());

        player.sendMessage(ChatColor.GREEN + "Badge created: " + ChatColor.YELLOW + badge.getIcon() + " " + badge.getName());
        openAdmin(player);
    }

    private static String progressLines(Profile profile, BadgeRequirement requirement) {
        StringBuilder sb = new StringBuilder();
        for (BadgeRequirement.Clause c : requirement.getClauses()) {
            int actual = BadgeRequirement.resolveForDisplay(profile, c.key());
            String line = ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + c.key() + ChatColor.WHITE + " " + opString(c.op())
                    + " " + c.value() + ChatColor.DARK_GRAY + " (you: " + actual + ")";
            if (sb.length() > 0) sb.append("\n");
            sb.append(line);
        }
        return sb.toString();
    }

    private static String opString(BadgeRequirement.Op op) {
        return switch (op) {
            case EQ -> "=";
            case GT -> ">";
            case GTE -> ">=";
            case LT -> "<";
            case LTE -> "<=";
        };
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        awaitingCreateName.remove(u);
        awaitingCreateIcon.remove(u);
        awaitingCreateReq.remove(u);
        awaitingCreateRank.remove(u);
        draftName.remove(u);
        draftIcon.remove(u);
        draftReq.remove(u);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        if (awaitingCreateName.remove(u)) {
            e.setCancelled(true);
            String msg = e.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> handleCreateName(e.getPlayer(), msg));
            return;
        }
        if (awaitingCreateIcon.remove(u)) {
            e.setCancelled(true);
            String msg = e.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> handleCreateIcon(e.getPlayer(), msg));
            return;
        }
        if (awaitingCreateReq.remove(u)) {
            e.setCancelled(true);
            String msg = e.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> handleCreateReq(e.getPlayer(), msg));
            return;
        }
        if (awaitingCreateRank.remove(u)) {
            e.setCancelled(true);
            String msg = e.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> handleCreateRank(e.getPlayer(), msg));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof BadgeHolder holder)) return;

        boolean clickedTop = e.getClickedInventory() != null && e.getClickedInventory().equals(e.getView().getTopInventory());
        if (clickedTop) e.setCancelled(true);
        if (!clickedTop) return;
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        int slot = e.getRawSlot();
        ItemMeta meta = e.getCurrentItem().getItemMeta();

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (holder.getType() == BadgeHolder.Type.PLAYER) {
            Profile profile = Craftmen.get().getProfileManager().getProfile(player);
            if (profile == null) return;
            if (slot == 45) {
                profile.setSelectedBadgeId(null);
                player.sendMessage(ChatColor.RED + "Badge cleared.");
                player.openInventory(buildPlayerMenu(player));
                return;
            }
            UUID id = readBadgeId(meta);
            if (id == null) return;
            BadgeDefinition badge = badges.get(id);
            if (badge == null) return;
            Optional<BadgeRequirement> req = BadgeRequirement.parse(badge.getRequirement());
            boolean ok = req.map(r -> r.matches(profile)).orElse(false);
            if (!ok) {
                player.sendMessage(ChatColor.RED + "You haven't unlocked that badge.");
                return;
            }
            profile.setSelectedBadgeId(id);
            player.sendMessage(ChatColor.GREEN + "Selected badge: " + ChatColor.YELLOW + badge.getIcon() + " " + badge.getName());

            String rank = badge.getRank();
            if (rank != null && !rank.isBlank()) {
                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(console, "rank give " + player.getName() + " " + rank);
            }

            player.openInventory(buildPlayerMenu(player));
            return;
        }

        if (holder.getType() == BadgeHolder.Type.ADMIN) {
            if (!player.isOp()) return;
            if (slot == 45) {
                beginCreate(player);
                return;
            }
            if (slot == 53) {
                reload();
                player.openInventory(buildAdminMenu(player));
                player.sendMessage(ChatColor.GREEN + "Badges reloaded.");
                return;
            }
            UUID id = readBadgeId(meta);
            if (id == null) return;
            if (e.isShiftClick() && e.isRightClick()) {
                badges.remove(id);
                storage.saveAll(badges.values());
                player.sendMessage(ChatColor.RED + "Badge deleted.");
                player.openInventory(buildAdminMenu(player));
                return;
            }
            // Simple edit: delete + recreate flow (fast and safe)
            badges.remove(id);
            storage.saveAll(badges.values());
            beginCreate(player);
        }
    }
}
