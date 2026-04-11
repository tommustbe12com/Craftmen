package com.tommustbe12.craftmen.customkit;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.customkit.gui.CustomKitHolder;
import com.tommustbe12.craftmen.customkit.gui.CustomKitMenus;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomKitManager implements Listener {

    private static final long INACTIVITY_DELETE_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private static final String LEAVE_ITEM_NAME = ChatColor.RED + "Leave Player Kit";
    private static final String LEAVE_QUEUE_ITEM_NAME = ChatColor.RED + "Leave Player Kit Queue";

    private final Craftmen plugin;
    private final CustomKitStorage storage;

    private final Map<UUID, CustomKit> kits = new HashMap<>();
    private final Map<UUID, UUID> activeKitByPlayer = new HashMap<>(); // player -> kitId
    private final Map<UUID, Set<UUID>> activePlayersByKit = new HashMap<>(); // kitId -> players

    private final Map<UUID, Deque<UUID>> queuedPlayersByKit = new HashMap<>(); // kitId -> player queue
    private final Map<UUID, UUID> queuedKitByPlayer = new HashMap<>(); // player -> kitId
    private final Map<UUID, UUID> duelOpponent = new HashMap<>(); // player -> opponent
    private final Map<UUID, UUID> duelKit = new HashMap<>(); // player -> kitId

    private final Map<UUID, CustomKitDraft> drafts = new HashMap<>();

    private final Set<UUID> awaitingKitName = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> awaitingSearch = new ConcurrentHashMap<>();
    private final Map<UUID, String> itemPickerSearch = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> itemPickerShowAll = new ConcurrentHashMap<>();

    private final List<Material> allMaterialsSorted;
    private final List<Material> pvpMaterialsSorted;

    public CustomKitManager(Craftmen plugin) {
        this.plugin = plugin;
        this.storage = new CustomKitStorage(plugin);

        for (CustomKit kit : storage.loadAll()) {
            kits.put(kit.getId(), kit);
        }

        this.allMaterialsSorted = Arrays.stream(Material.values())
                .filter(Material::isItem)
                .sorted(Comparator.comparing(Material::name))
                .toList();

        this.pvpMaterialsSorted = buildPvpMaterialList(allMaterialsSorted);

        startCleanupTask();
    }

    public Collection<CustomKit> getAllKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public int getActivePlayers(UUID kitId) {
        return activePlayersByKit.getOrDefault(kitId, Set.of()).size();
    }

    public boolean isInPlayerKit(Player player) {
        return activeKitByPlayer.containsKey(player.getUniqueId());
    }

    public void openBrowser(Player player) {
        if (!ensureHub(player)) return;
        openBrowserPage(player, 0);
    }

    private void openBrowserPage(Player player, int page) {
        List<CustomKit> list = new ArrayList<>(kits.values());
        list.sort(Comparator.comparing(CustomKit::getName, String.CASE_INSENSITIVE_ORDER));
        int perPage = 28;
        int maxPage = Math.max(0, (int) Math.ceil(list.size() / (double) perPage) - 1);
        int clamped = Math.max(0, Math.min(page, maxPage));

        Map<UUID, Integer> counts = new HashMap<>();
        for (CustomKit kit : list) counts.put(kit.getId(), getActivePlayers(kit.getId()));

        Inventory inv = CustomKitMenus.createBrowser(player, clamped, maxPage, list, counts);
        player.openInventory(inv);
    }

    private void startCreateFlow(Player player) {
        if (!ensureHub(player)) return;
        if (drafts.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You're already creating a kit.");
            return;
        }
        awaitingKitName.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Type your new kit name in chat (or type 'cancel').");
    }

    private void startItemSearch(Player player) {
        awaitingSearch.put(player.getUniqueId(), "itemPicker");
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Type an item search in chat (or type 'clear' / 'cancel').");
    }

    private void beginDraft(Player player, String kitName) {
        kitName = sanitizeName(kitName);
        if (kitName.isBlank()) {
            player.sendMessage(ChatColor.RED + "That kit name is invalid.");
            return;
        }

        UUID kitId = UUID.randomUUID();
        Kit restore = Kit.fromPlayer(player);
        CustomKitDraft draft = new CustomKitDraft(kitId, player.getUniqueId(), player.getName(), kitName, restore);
        drafts.put(player.getUniqueId(), draft);

        player.getInventory().clear();
        player.getInventory().setItem(0, CustomKitMenus.itemBrowserTool());
        player.getInventory().setItem(1, CustomKitMenus.enchantTool());
        player.getInventory().setItem(4, leaveQueueItem()); // reserve common slot
        player.getInventory().setItem(7, CustomKitMenus.cancelTool());
        player.getInventory().setItem(8, CustomKitMenus.saveTool());
        player.updateInventory();

        player.sendMessage(ChatColor.AQUA + "Custom kit builder started: " + ChatColor.YELLOW + kitName);
        player.sendMessage(ChatColor.GRAY + "Use Item Browser + Enchant Tool, arrange your inventory, then Save.");
    }

    private void cancelDraft(Player player) {
        CustomKitDraft draft = drafts.remove(player.getUniqueId());
        if (draft == null) return;

        draft.restoreInventory.apply(player);
        Craftmen.get().getHubManager().giveHubItems(player);
        player.sendMessage(ChatColor.RED + "Cancelled kit creation.");
    }

    private void saveDraft(Player player) {
        CustomKitDraft draft = drafts.remove(player.getUniqueId());
        if (draft == null) return;

        Kit kit = Kit.fromPlayer(player);
        long now = System.currentTimeMillis();
        CustomKit customKit = new CustomKit(
                draft.id,
                draft.name,
                draft.creatorUuid,
                draft.creatorName,
                now,
                now,
                0L,
                kit
        );

        kits.put(customKit.getId(), customKit);
        storage.save(customKit);

        draft.restoreInventory.apply(player);
        Craftmen.get().getHubManager().giveHubItems(player);
        player.sendMessage(ChatColor.GREEN + "Published kit: " + ChatColor.AQUA + customKit.getName());
    }

    public void joinKit(Player player, UUID kitId) {
        if (!ensureHub(player)) return;
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile.getState() != PlayerState.LOBBY) {
            player.sendMessage(ChatColor.RED + "You can only play player kits in the hub.");
            return;
        }
        if (drafts.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Finish or cancel kit creation first.");
            return;
        }

        CustomKit kit = kits.get(kitId);
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "That kit no longer exists.");
            return;
        }

        // Queue-like behavior: click -> join kit queue, then pair into a hub-only duel.
        leaveKit(player, false);
        leaveQueue(player, false);

        queuedKitByPlayer.put(player.getUniqueId(), kitId);
        queuedPlayersByKit.computeIfAbsent(kitId, k -> new ArrayDeque<>()).addLast(player.getUniqueId());
        profile.setState(PlayerState.CUSTOM_KIT_QUEUED);
        giveQueuedItem(player);

        int queuedCount = queuedPlayersByKit.getOrDefault(kitId, new ArrayDeque<>()).size();
        player.sendMessage(ChatColor.GREEN + "Queued for " + ChatColor.AQUA + kit.getName() + ChatColor.GREEN + " (" + queuedCount + " queued)");

        tryStartDuel(kitId);
    }

    public void leaveKit(Player player, boolean message) {
        UUID kitId = activeKitByPlayer.remove(player.getUniqueId());
        if (kitId == null) return;

        Set<UUID> actives = activePlayersByKit.get(kitId);
        if (actives != null) {
            actives.remove(player.getUniqueId());
            if (actives.isEmpty()) activePlayersByKit.remove(kitId);
        }

        duelKit.remove(player.getUniqueId());
        UUID opp = duelOpponent.remove(player.getUniqueId());
        if (opp != null) duelOpponent.remove(opp);

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null) profile.setState(PlayerState.LOBBY);
        Craftmen.get().getHubManager().giveHubItems(player);
        if (message) player.sendMessage(ChatColor.RED + "Left player kit.");
    }

    public void leaveQueue(Player player, boolean message) {
        UUID kitId = queuedKitByPlayer.remove(player.getUniqueId());
        if (kitId == null) return;
        Deque<UUID> q = queuedPlayersByKit.get(kitId);
        if (q != null) {
            q.remove(player.getUniqueId());
            if (q.isEmpty()) queuedPlayersByKit.remove(kitId);
        }
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile != null && profile.getState() == PlayerState.CUSTOM_KIT_QUEUED) profile.setState(PlayerState.LOBBY);
        Craftmen.get().getHubManager().giveHubItems(player);
        if (message) player.sendMessage(ChatColor.RED + "Left player kit queue.");
    }

    private ItemStack leaveItem() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(LEAVE_ITEM_NAME);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack leaveQueueItem() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(LEAVE_QUEUE_ITEM_NAME);
        item.setItemMeta(meta);
        return item;
    }

    private void giveQueuedItem(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(4, leaveQueueItem());
        player.updateInventory();
    }

    private void openItemPicker(Player player, int page) {
        String search = itemPickerSearch.getOrDefault(player.getUniqueId(), "");
        boolean showAll = itemPickerShowAll.getOrDefault(player.getUniqueId(), false);
        List<Material> filtered = filterMaterials(search, showAll);
        int perPage = 28;
        int maxPage = Math.max(0, (int) Math.ceil(filtered.size() / (double) perPage) - 1);
        int clamped = Math.max(0, Math.min(page, maxPage));

        Inventory inv = CustomKitMenus.createItemPicker(player, clamped, maxPage, filtered, search);
        inv.setItem(53, CustomKitMenus.showAllToggle(showAll));
        player.openInventory(inv);
    }

    private List<Material> filterMaterials(String search, boolean showAll) {
        List<Material> base = showAll ? allMaterialsSorted : pvpMaterialsSorted;
        if (search == null || search.isBlank()) return base;
        String s = search.toLowerCase(Locale.ROOT).trim();
        List<Material> out = new ArrayList<>();
        for (Material m : base) {
            if (m.name().toLowerCase(Locale.ROOT).contains(s)) out.add(m);
        }
        return out;
    }

    private void openEnchantMenu(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold an item in your main hand first.");
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "That item can't be enchanted.");
            return;
        }

        List<Enchantment> applicable = new ArrayList<>();
        for (Enchantment e : Enchantment.values()) {
            if (e == null) continue;
            if (meta instanceof EnchantmentStorageMeta) {
                // Enchanted books can store any vanilla enchantment.
                applicable.add(e);
            } else {
                if (e.canEnchantItem(item)) applicable.add(e);
            }
        }

        applicable.sort(Comparator.comparing(a -> a.getKey().toString()));
        Inventory inv = CustomKitMenus.createEnchantMenu(player, item, applicable);
        player.openInventory(inv);
        player.sendMessage(ChatColor.GRAY + "Enchanting: left-click (+1), right-click (-1), shift-click (remove).");
    }

    private void adjustEnchant(Player player, Enchantment ench, int delta, boolean remove) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (!(meta instanceof EnchantmentStorageMeta) && !ench.canEnchantItem(item)) return;

        Map<Enchantment, Integer> current = meta instanceof EnchantmentStorageMeta esm
                ? new HashMap<>(esm.getStoredEnchants())
                : new HashMap<>(item.getEnchantments());

        if (remove) {
            current.remove(ench);
        } else {
            int old = current.getOrDefault(ench, 0);
            int next = Math.max(0, Math.min(ench.getMaxLevel(), old + delta));
            if (next == 0) current.remove(ench);
            else current.put(ench, next);
        }

        // Enforce vanilla conflicts (no impossible combos)
        for (Enchantment a : new ArrayList<>(current.keySet())) {
            for (Enchantment b : new ArrayList<>(current.keySet())) {
                if (a == b) continue;
                if (a.conflictsWith(b) || b.conflictsWith(a)) {
                    // keep the higher level enchant, otherwise keep 'a'
                    int la = current.getOrDefault(a, 0);
                    int lb = current.getOrDefault(b, 0);
                    if (lb > la) current.remove(a);
                    else current.remove(b);
                }
            }
        }

        if (meta instanceof EnchantmentStorageMeta esm) {
            for (Enchantment e : new ArrayList<>(esm.getStoredEnchants().keySet())) esm.removeStoredEnchant(e);
            for (Map.Entry<Enchantment, Integer> entry : current.entrySet()) {
                esm.addStoredEnchant(entry.getKey(), entry.getValue(), true);
            }
            item.setItemMeta(esm);
        } else {
            for (Enchantment e : new ArrayList<>(item.getEnchantments().keySet())) item.removeEnchantment(e);
            for (Map.Entry<Enchantment, Integer> entry : current.entrySet()) {
                item.addEnchantment(entry.getKey(), entry.getValue());
            }
        }

        player.getInventory().setItemInMainHand(item);
        player.updateInventory();
        openEnchantMenu(player);
    }

    private boolean ensureHub(Player player) {
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null) return false;
        if (profile.getState() != PlayerState.LOBBY) return false;
        return true;
    }

    public boolean allowDamage(Player damager, Player damaged) {
        UUID d = damager.getUniqueId();
        UUID t = damaged.getUniqueId();
        UUID opp = duelOpponent.get(d);
        return opp != null && opp.equals(t);
    }

    private void tryStartDuel(UUID kitId) {
        Deque<UUID> queue = queuedPlayersByKit.get(kitId);
        if (queue == null) return;
        while (queue.size() >= 2) {
            UUID p1 = queue.pollFirst();
            UUID p2 = queue.pollFirst();
            if (p1 == null || p2 == null) return;

            Player a = Bukkit.getPlayer(p1);
            Player b = Bukkit.getPlayer(p2);
            if (a == null || b == null) continue;

            // Both must still be queued for this kit
            if (!kitId.equals(queuedKitByPlayer.get(p1)) || !kitId.equals(queuedKitByPlayer.get(p2))) continue;

            queuedKitByPlayer.remove(p1);
            queuedKitByPlayer.remove(p2);

            startDuel(a, b, kitId);
        }
        if (queue.isEmpty()) queuedPlayersByKit.remove(kitId);
    }

    private void startDuel(Player a, Player b, UUID kitId) {
        CustomKit kit = kits.get(kitId);
        if (kit == null) return;

        long now = System.currentTimeMillis();
        CustomKit updated = kit.withUsage(now);
        kits.put(updated.getId(), updated);
        storage.save(updated);

        Profile pa = Craftmen.get().getProfileManager().getProfile(a);
        Profile pb = Craftmen.get().getProfileManager().getProfile(b);
        if (pa != null) pa.setState(PlayerState.CUSTOM_KIT_PLAYING);
        if (pb != null) pb.setState(PlayerState.CUSTOM_KIT_PLAYING);

        duelOpponent.put(a.getUniqueId(), b.getUniqueId());
        duelOpponent.put(b.getUniqueId(), a.getUniqueId());
        duelKit.put(a.getUniqueId(), kitId);
        duelKit.put(b.getUniqueId(), kitId);

        activeKitByPlayer.put(a.getUniqueId(), kitId);
        activeKitByPlayer.put(b.getUniqueId(), kitId);
        activePlayersByKit.computeIfAbsent(kitId, k -> new HashSet<>()).add(a.getUniqueId());
        activePlayersByKit.computeIfAbsent(kitId, k -> new HashSet<>()).add(b.getUniqueId());

        updated.getKit().apply(a);
        updated.getKit().apply(b);
        a.getInventory().setItem(8, leaveItem());
        b.getInventory().setItem(8, leaveItem());
        a.updateInventory();
        b.updateInventory();

        a.sendMessage(ChatColor.GREEN + "Duel started: " + ChatColor.AQUA + updated.getName() + ChatColor.GRAY + " vs " + ChatColor.YELLOW + b.getName());
        b.sendMessage(ChatColor.GREEN + "Duel started: " + ChatColor.AQUA + updated.getName() + ChatColor.GRAY + " vs " + ChatColor.YELLOW + a.getName());
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpired();
            }
        }.runTaskTimer(plugin, 20L * 60L, 20L * 60L * 60L); // 1 min delay, hourly
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        List<UUID> toDelete = new ArrayList<>();
        for (CustomKit kit : kits.values()) {
            if (getActivePlayers(kit.getId()) > 0) continue;
            long last = Math.max(kit.getLastUsedAtMillis(), kit.getCreatedAtMillis());
            if (now - last >= INACTIVITY_DELETE_MILLIS) toDelete.add(kit.getId());
        }

        for (UUID id : toDelete) {
            kits.remove(id);
            storage.delete(id);
        }
    }

    private static String sanitizeName(String input) {
        if (input == null) return "";
        String s = ChatColor.stripColor(input).trim();
        if (s.length() > 32) s = s.substring(0, 32);
        return s.replaceAll("[\\r\\n\\t]", " ").trim();
    }

    // --------------------
    // Events
    // --------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        awaitingKitName.remove(player.getUniqueId());
        awaitingSearch.remove(player.getUniqueId());
        itemPickerSearch.remove(player.getUniqueId());
        cancelDraft(player);
        leaveQueue(player, false);
        leaveKit(player, false);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();

        if (awaitingKitName.remove(uuid)) {
            e.setCancelled(true);
            String msg = e.getMessage();
            if (msg.equalsIgnoreCase("cancel")) {
                Bukkit.getScheduler().runTask(plugin, () -> e.getPlayer().sendMessage(ChatColor.RED + "Cancelled."));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> beginDraft(e.getPlayer(), msg));
            return;
        }

        String mode = awaitingSearch.remove(uuid);
        if (mode != null && mode.equals("itemPicker")) {
            e.setCancelled(true);
            String msg = e.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (msg.equalsIgnoreCase("cancel")) return;
                if (msg.equalsIgnoreCase("clear")) itemPickerSearch.remove(uuid);
                else itemPickerSearch.put(uuid, msg);
                openItemPicker(e.getPlayer(), 0);
            });
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) return;
        String name = item.getItemMeta().getDisplayName();

        if (name.equals(CustomKitMenus.TOOL_ITEM_BROWSER_NAME)) {
            if (!drafts.containsKey(player.getUniqueId())) return;
            e.setCancelled(true);
            openItemPicker(player, 0);
            return;
        }

        if (name.equals(CustomKitMenus.TOOL_ENCHANT_NAME)) {
            if (!drafts.containsKey(player.getUniqueId())) return;
            e.setCancelled(true);
            openEnchantMenu(player);
            return;
        }

        if (name.equals(CustomKitMenus.TOOL_SAVE_NAME)) {
            if (!drafts.containsKey(player.getUniqueId())) return;
            e.setCancelled(true);
            saveDraft(player);
            return;
        }

        if (name.equals(CustomKitMenus.TOOL_CANCEL_NAME)) {
            if (!drafts.containsKey(player.getUniqueId())) return;
            e.setCancelled(true);
            cancelDraft(player);
            return;
        }

        if (name.equals(LEAVE_ITEM_NAME)) {
            e.setCancelled(true);
            leaveKit(player, true);
        }

        if (name.equals(LEAVE_QUEUE_ITEM_NAME)) {
            e.setCancelled(true);
            leaveQueue(player, true);
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomKitHolder holder)) return;
        boolean clickedTop = e.getClickedInventory() != null && e.getClickedInventory().equals(e.getView().getTopInventory());
        if (clickedTop) e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        int slot = e.getRawSlot();

        if (holder.getType() == CustomKitHolder.Type.BROWSER) {
            if (slot == 45) {
                startCreateFlow(player);
                return;
            }
            if (slot == 46) {
                openBrowserPage(player, holder.getPage() - 1);
                return;
            }
            if (slot == 52) {
                openBrowserPage(player, holder.getPage() + 1);
                return;
            }
            if (slot == 49) {
                player.closeInventory();
                return;
            }

            ItemStack clicked = e.getCurrentItem();
            ItemMeta meta = clicked.getItemMeta();
            UUID kitId = CustomKitMenus.readKitId(meta);
            if (kitId == null) return;
            player.closeInventory();
            joinKit(player, kitId);
            return;
        }

        if (holder.getType() == CustomKitHolder.Type.ITEM_PICKER) {
            if (!drafts.containsKey(player.getUniqueId())) {
                player.closeInventory();
                return;
            }
            if (!clickedTop) return; // allow moving items in own inventory while picker is open
            if (slot == 45) {
                startItemSearch(player);
                return;
            }
            if (slot == 53) {
                boolean current = itemPickerShowAll.getOrDefault(player.getUniqueId(), false);
                itemPickerShowAll.put(player.getUniqueId(), !current);
                openItemPicker(player, 0);
                return;
            }
            if (slot == 46) {
                openItemPicker(player, holder.getPage() - 1);
                return;
            }
            if (slot == 52) {
                openItemPicker(player, holder.getPage() + 1);
                return;
            }
            if (slot == 49) {
                player.closeInventory();
                return;
            }

            ItemStack clicked = e.getCurrentItem();
            Material mat = clicked.getType();
            int amount = e.isShiftClick() ? 64 : 1;
            ItemStack give = new ItemStack(mat, amount);
            player.setItemOnCursor(give);
            player.closeInventory();
            player.sendMessage(ChatColor.GRAY + "Item picked. Click in your inventory to place it, then reopen Item Browser.");
            return;
        }

        if (holder.getType() == CustomKitHolder.Type.ENCHANT) {
            if (!drafts.containsKey(player.getUniqueId())) {
                player.closeInventory();
                return;
            }
            if (!clickedTop) return;
            if (slot == 49) {
                player.closeInventory();
                return;
            }

            ItemStack clicked = e.getCurrentItem();
            ItemMeta meta = clicked.getItemMeta();
            String key = CustomKitMenus.readEnchantKey(meta);
            if (key == null) return;

            Enchantment ench = null;
            for (Enchantment en : Enchantment.values()) {
                if (en == null) continue;
                if (en.getKey().toString().equals(key)) {
                    ench = en;
                    break;
                }
            }
            if (ench == null) return;

            boolean remove = e.isShiftClick();
            int delta = e.isRightClick() ? -1 : 1;
            adjustEnchant(player, ench, delta, remove);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        if (!activeKitByPlayer.containsKey(dead.getUniqueId())) return;
        UUID oppId = duelOpponent.get(dead.getUniqueId());
        Player opp = oppId == null ? null : Bukkit.getPlayer(oppId);
        e.setDeathMessage(null);
        e.getDrops().clear();
        e.setDroppedExp(0);

        // End duel for both players.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (opp != null) {
                opp.sendMessage(ChatColor.GREEN + "You won the player kit duel vs " + ChatColor.YELLOW + dead.getName());
                leaveKit(opp, false);
            }
            leaveKit(dead, false);
        });
    }

    private static List<Material> buildPvpMaterialList(List<Material> all) {
        Set<Material> out = new HashSet<>();
        for (Material m : all) {
            String n = m.name();

            // Weapons/tools
            if (n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_HOE")) out.add(m);
            if (n.equals("MACE") || n.equals("TRIDENT") || n.equals("BOW") || n.equals("CROSSBOW")) out.add(m);

            // Armor
            if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")) out.add(m);
            if (n.equals("ELYTRA") || n.equals("SHIELD")) out.add(m);

            // Combat consumables + utilities
            if (n.equals("GOLDEN_APPLE") || n.equals("ENCHANTED_GOLDEN_APPLE")) out.add(m);
            if (n.equals("ENDER_PEARL") || n.equals("SNOWBALL") || n.equals("EGG")) out.add(m);
            if (n.equals("TOTEM_OF_UNDYING")) out.add(m);
            if (n.equals("FISHING_ROD")) out.add(m);
            if (n.equals("LAVA_BUCKET") || n.equals("WATER_BUCKET")) out.add(m);
            if (n.equals("COBWEB")) out.add(m);

            // Potions / arrows
            if (n.equals("POTION") || n.equals("SPLASH_POTION") || n.equals("LINGERING_POTION")) out.add(m);
            if (n.equals("ARROW") || n.equals("TIPPED_ARROW") || n.equals("SPECTRAL_ARROW")) out.add(m);

            // Basics
            if (n.equals("ENCHANTED_BOOK")) out.add(m);
        }
        List<Material> list = new ArrayList<>(out);
        list.sort(Comparator.comparing(Material::name));
        return list;
    }
}
