package com.tommustbe12.craftmen.badge;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.badge.gui.BadgeHolder;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
    private final BadgeDisplay display;

    private final NamespacedKey badgeIdKey = new NamespacedKey("craftmen", "badge_id");

    private final Set<UUID> awaitingCreateName = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> draftName = new ConcurrentHashMap<>();
    private final Set<UUID> awaitingCreateIcon = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> draftIcon = new ConcurrentHashMap<>();
    private final Set<UUID> awaitingCreateReq = ConcurrentHashMap.newKeySet();
    private final Set<UUID> awaitingCreateColor = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> draftReq = new ConcurrentHashMap<>();

    public BadgeManager(Craftmen plugin) {
        this.plugin = plugin;
        this.storage = new BadgeStorage(plugin);
        this.display = new BadgeDisplay();
        reload();
        this.display.startAutoRefresh();
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

    public BadgeDisplay getDisplay() {
        return display;
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
        list.sort((a, b) -> {
            if (selected != null) {
                boolean aSel = selected.equals(a.getId());
                boolean bSel = selected.equals(b.getId());
                if (aSel != bSel) return aSel ? -1 : 1;
            }

            int aDiff = difficultyScore(a.getRequirement());
            int bDiff = difficultyScore(b.getRequirement());
            if (aDiff != bDiff) return Integer.compare(aDiff, bDiff);

            return String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName());
        });

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
            progress = req.map(r -> progressLinesFriendly(profile, r)).orElse(null);
        }

        // Locked badges should look disabled.
        ItemStack item = new ItemStack(available ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((available ? ChatColor.AQUA : ChatColor.DARK_GRAY) + badge.getIcon() + " " + badge.getName());
        List<String> lore = new ArrayList<>();
        Optional<BadgeRequirement> parsed = BadgeRequirement.parse(badge.getRequirement());
        if (parsed.isPresent()) {
            lore.add(ChatColor.GRAY + "How to get it:");
            for (String line : requirementLinesFriendly(parsed.get())) {
                lore.add(ChatColor.DARK_GRAY + "- " + ChatColor.WHITE + line);
            }
        } else {
            lore.add(ChatColor.GRAY + "Requirement: " + ChatColor.WHITE + badge.getRequirement());
        }
        if (progress != null) {
            lore.add(ChatColor.GRAY + "Progress:");
            for (String line : progress.split("\n")) lore.add(line);
        }
        lore.add(ChatColor.GRAY + "Color: " + renderColorPreview(badge));
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
                ChatColor.GRAY + "Color: " + ChatColor.WHITE + (badge.getColor() == null ? "&7" : badge.getColor()),
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
                ChatColor.GRAY + "Color example:",
                ChatColor.WHITE + "&b (brackets + icon color)",
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
        awaitingCreateColor.add(uuid);
        player.sendMessage(ChatColor.YELLOW + "Type badge color using & codes (e.g. &b). Type 'default' for &7.");
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return ChatColor.stripColor(s).replaceAll("[\\r\\n\\t]", " ").trim();
    }

    private void handleCreateColor(Player player, String msg) {
        UUID uuid = player.getUniqueId();
        if (msg.equalsIgnoreCase("cancel")) return;

        String name = draftName.get(uuid);
        String icon = draftIcon.get(uuid);
        String req = draftReq.get(uuid);
        if (name == null || icon == null || req == null) return;

        String color = msg.equalsIgnoreCase("default") ? "&7" : sanitize(msg);
        if (color.isBlank()) color = "&7";
        // Very loose validation: must contain '&' followed by a legacy color code somewhere, else default.
        if (!color.contains("&")) color = "&7";

        UUID id = UUID.randomUUID();
        BadgeDefinition badge = new BadgeDefinition(id, sanitize(name), sanitize(icon), req, color);
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

    private static String renderColorPreview(BadgeDefinition badge) {
        String colorRaw = badge.getColor() == null ? "&7" : badge.getColor();
        String translated = ChatColor.translateAlternateColorCodes('&', colorRaw);
        // Use a neutral glyph so weird emoji don't duplicate; still shows exact color.
        return translated + "⬤ " + ChatColor.DARK_GRAY + "(" + ChatColor.GRAY + colorRaw + ChatColor.DARK_GRAY + ")";
    }

    private static java.util.List<String> requirementLinesFriendly(BadgeRequirement requirement) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (BadgeRequirement.Clause c : requirement.getClauses()) {
            out.add(requirementClauseFriendly(c));
        }
        return out;
    }

    private static String requirementClauseFriendly(BadgeRequirement.Clause c) {
        String targetName = friendlyKeyName(c.key());
        String amount = String.valueOf(c.value());
        return switch (c.op()) {
            case GT -> "More than " + amount + " " + targetName;
            case GTE -> "At least " + amount + " " + targetName;
            case EQ -> "Exactly " + amount + " " + targetName;
            case LT -> "Less than " + amount + " " + targetName;
            case LTE -> "At most " + amount + " " + targetName;
        };
    }

    private static String friendlyKeyName(String rawKey) {
        if (rawKey == null) return "progress";
        String k = rawKey.trim();
        String lower = k.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("wins")) return "overall wins";
        if (lower.equals("losses")) return "overall losses";
        if (lower.equals("ffa_kills") || lower.equals("ffa_kill")) return "FFA kills";
        if (lower.equals("ffa_deaths") || lower.equals("ffa_death")) return "FFA deaths";
        if (lower.startsWith("game.")) {
            String rest = k.substring(5);
            String restLower = rest.toLowerCase(java.util.Locale.ROOT).trim();
            boolean wins = restLower.endsWith("wins");
            boolean losses = restLower.endsWith("losses");
            String gameKey = rest.substring(0, rest.length() - (wins ? 4 : (losses ? 6 : 0)));
            String gameName = gameKey.replace("_", " ").trim();
            if (wins) return gameName + " wins";
            if (losses) return gameName + " losses";
        }
        return k;
    }

    private static String progressLinesFriendly(Profile profile, BadgeRequirement requirement) {
        StringBuilder sb = new StringBuilder();
        for (BadgeRequirement.Clause c : requirement.getClauses()) {
            int actual = BadgeRequirement.resolveForDisplay(profile, c.key());
            String name = friendlyKeyName(c.key());
            String line = ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + actual + ChatColor.DARK_GRAY + "/" + ChatColor.WHITE + c.value()
                    + ChatColor.DARK_GRAY + " " + ChatColor.GRAY + name;
            if (sb.length() > 0) sb.append("\n");
            sb.append(line);
        }
        return sb.toString();
    }

    private static int difficultyScore(String requirementRaw) {
        Optional<BadgeRequirement> parsed = BadgeRequirement.parse(requirementRaw);
        if (parsed.isEmpty()) return Integer.MAX_VALUE;
        int score = 0;
        for (BadgeRequirement.Clause c : parsed.get().getClauses()) {
            score += clauseDifficultyScore(c);
        }
        return score;
    }

    private static int clauseDifficultyScore(BadgeRequirement.Clause c) {
        String key = c.key() == null ? "" : c.key().toLowerCase(java.util.Locale.ROOT).trim();
        int base = Math.max(1, c.value());

        int weight;
        if (key.startsWith("game.")) weight = 1;
        else if (key.equals("wins") || key.equals("losses")) weight = 1;
        else if (key.startsWith("ffa_")) weight = 1;
        else if (key.equals("endwins") || key.equals("end_wins")) weight = 2;
        else if (key.equals("killsinarow") || key.equals("kills_in_a_row")) weight = 2;
        else if (key.contains("mostkills")) weight = 50; // essentially "hard"
        else weight = 3;

        double opFactor = switch (c.op()) {
            case GT -> 1.1;
            case GTE -> 1.0;
            case EQ -> 1.0;
            case LT -> 0.9;
            case LTE -> 0.9;
        };

        long scaled = Math.round(base * weight * opFactor);
        if (scaled > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) scaled;
    }

    private static int gemsForBadge(String requirementRaw) {
        int diff = difficultyScore(requirementRaw);
        if (diff == Integer.MAX_VALUE) return 0;
        int gems = Math.max(5, Math.min(250, (int) Math.round(Math.sqrt(diff) * 1.5)));
        gems = (gems / 5) * 5;
        if (gems < 5) gems = 5;
        return gems;
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
        awaitingCreateColor.remove(u);
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
        if (awaitingCreateColor.remove(u)) {
            e.setCancelled(true);
            String msg = e.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> handleCreateColor(e.getPlayer(), msg));
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
                display.apply(player);
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
            display.apply(player);

            // Gems reward (one-time) for unlocking/selecting a badge.
            if (!profile.hasClaimedBadgeReward(id)) {
                int gems = gemsForBadge(badge.getRequirement());
                profile.addGems(gems);
                profile.claimBadgeReward(id);
                player.sendMessage(ChatColor.AQUA + "+" + gems + " Gems " + ChatColor.GRAY + "(badge reward)");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.3f);
                Craftmen.get().saveProfile(profile);
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
