package com.tommustbe12.craftmen.hub;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.profile.PlayerState;
import com.tommustbe12.craftmen.queue.QueueManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;

public class HubManager implements Listener {

    private static final String GUI_TITLE_PREFIX = "§8Select a Kit";
    private static final String GUI_FFA_TITLE_PREFIX = "§8FFA: Select a Kit";
    private static final String GUI_PLAYER_KITS_NAME = "§bPlayer Kits";
    private static final String GUI_FFA_NAME = "§cFFA";

    private static final String HUB_ITEM_GAME_SELECTOR = ChatColor.GOLD + "Game Selector";
    private static final String HUB_ITEM_KIT_EDITOR = ChatColor.AQUA + "Kit Editor";
    private static final String HUB_ITEM_ARMOR_TRIMS = ChatColor.LIGHT_PURPLE + "Armor Trims";
    private static final String HUB_ITEM_PLAY_AGAIN = ChatColor.GOLD + "Play Again";
    private static final String HUB_ITEM_LEAVE_QUEUE = ChatColor.RED + "Leave Queue";

    private static final List<String> PAGE_ONE_KITS = Arrays.asList(
            "Sword", "Mace", "Axe", "Netherite Potion", "Diamond Potion", "SMP", "UHC"
    );

    private static final int INV_SIZE = 54;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.length; // 28

    private static final int SLOT_PREV = 46;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT = 52;
    private static final int SLOT_PLAYER_KITS = 50;
    private static final int SLOT_FFA = 48;

    private static final int CRYSTAL_SLOT = 22;

    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, Consumer<Game>> gameCallbacks = new HashMap<>();
    private final Map<UUID, Integer> ffaPage = new HashMap<>();

    // ── Hub item helpers ─────────────────────────────────────────────────────

    public void giveHubItems(Player player) {
        player.getInventory().clear();
        ItemStack selector = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = selector.getItemMeta();
        meta.setDisplayName(HUB_ITEM_GAME_SELECTOR);
        selector.setItemMeta(meta);
        player.getInventory().setItem(0, selector);

        ItemStack kitEditor = new ItemStack(Material.CHEST);
        ItemMeta kitMeta = kitEditor.getItemMeta();
        kitMeta.setDisplayName(HUB_ITEM_KIT_EDITOR);
        kitEditor.setItemMeta(kitMeta);
        player.getInventory().setItem(8, kitEditor);

        ItemStack trims = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta trimsMeta = trims.getItemMeta();
        trimsMeta.setDisplayName(HUB_ITEM_ARMOR_TRIMS);
        trims.setItemMeta(trimsMeta);
        player.getInventory().setItem(7, trims);

        if(Craftmen.get().getProfileManager().getProfile(player).getLastPlayedGame().equals("None")) return;
        ItemStack again = new ItemStack(Craftmen.get().getGameManager().getGame(Craftmen.get().getProfileManager().getProfile(player).getLastPlayedGame()).getIcon()); // complicated lol
        ItemMeta meta1 = again.getItemMeta();
        meta1.setDisplayName(HUB_ITEM_PLAY_AGAIN);
        again.setItemMeta(meta1);
        player.getInventory().setItem(1, again);
    }

    public void giveLeaveQueueItem(Player player) {
        player.getInventory().clear();
        ItemStack leave = new ItemStack(Material.RED_DYE);
        ItemMeta meta = leave.getItemMeta();
        meta.setDisplayName(HUB_ITEM_LEAVE_QUEUE);
        leave.setItemMeta(meta);
        player.getInventory().setItem(4, leave);
        player.updateInventory();
    }

    // ── Event handlers ───────────────────────────────────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        giveHubItems(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!Craftmen.get().getEndFightManager().isInGame(e.getPlayer())) {
            Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> giveHubItems(e.getPlayer()), 1L);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) return;

        String name = item.getItemMeta().getDisplayName();
        Player player = e.getPlayer();

        if (name.equals(HUB_ITEM_GAME_SELECTOR)) {
            e.setCancelled(true);
            openGameSelector(player, game -> {
                giveLeaveQueueItem(player);
                Craftmen.get().getQueueManager().addPlayer(player, game);
            });

        } else if (name.equals(HUB_ITEM_KIT_EDITOR)) {
            e.setCancelled(true);
            if (Craftmen.get().getProfileManager().getProfile(player).getState() != PlayerState.LOBBY) {
                player.sendMessage(ChatColor.RED + "You can only edit kits in the hub.");
                return;
            }
            Craftmen.get().getKitEditorMenu().openSelect(player);

        } else if (name.equals(HUB_ITEM_ARMOR_TRIMS)) {
            e.setCancelled(true);
            if (Craftmen.get().getProfileManager().getProfile(player).getState() != PlayerState.LOBBY) {
                player.sendMessage(ChatColor.RED + "You can only edit armor trims in the hub.");
                return;
            }
            Craftmen.get().getArmorTrimMenu().openMain(player);

        } else if (name.equals(HUB_ITEM_LEAVE_QUEUE)) {
            e.setCancelled(true);
            Craftmen.get().getQueueManager().removePlayer(player);
            giveHubItems(player);
        } else if (name.equals(HUB_ITEM_PLAY_AGAIN)) {
            e.setCancelled(true);
            Craftmen.get().getQueueManager().queueAgain(player);
            player.teleport(Craftmen.get().getHubLocation());
            giveLeaveQueueItem(player);
        }
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent e) {
        boolean isNormal = e.getView().getTitle().startsWith(GUI_TITLE_PREFIX);
        boolean isFfa = e.getView().getTitle().startsWith(GUI_FFA_TITLE_PREFIX);
        if (!isNormal && !isFfa) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return;

        int slot = e.getRawSlot();

        if (isNormal && slot == SLOT_FFA) {
            player.closeInventory();
            openFfaSelector(player, 0);
            return;
        }

        if (slot == SLOT_PLAYER_KITS) {
            player.closeInventory();
            playerPage.remove(player.getUniqueId());
            gameCallbacks.remove(player.getUniqueId());
            Craftmen.get().getCustomKitManager().openBrowser(player);
            return;
        }

        // Navigation buttons
        if (slot == SLOT_PREV) {
            int page = (isFfa ? ffaPage : playerPage).getOrDefault(player.getUniqueId(), 0);
            if (page > 0) {
                if (isFfa) openFfaSelector(player, page - 1);
                else openPage(player, page - 1);
            }
            return;
        }
        if (slot == SLOT_NEXT) {
            int page = (isFfa ? ffaPage : playerPage).getOrDefault(player.getUniqueId(), 0);
            if (isFfa) openFfaSelector(player, page + 1);
            else openPage(player, page + 1);
            return;
        }
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            playerPage.remove(player.getUniqueId());
            ffaPage.remove(player.getUniqueId());
            gameCallbacks.remove(player.getUniqueId());
            return;
        }

        // Content slot — find which game was clicked
        String rawName = clicked.getItemMeta().getDisplayName();
        // Strip colour codes
        String gameName = ChatColor.stripColor(rawName);
        if (isFfa && gameName != null) {
            // "FFA - Sword" -> "Sword"
            int dash = gameName.indexOf('-');
            if (dash >= 0 && dash + 1 < gameName.length()) {
                gameName = gameName.substring(dash + 1).trim();
            }
            if (gameName.toLowerCase(Locale.ROOT).startsWith("ffa")) {
                gameName = gameName.substring(3).trim();
            }
        }

        Game game = Craftmen.get().getGameManager().getGame(gameName);
        if (game == null) return;

        if (isFfa) {
            ffaPage.remove(player.getUniqueId());
            player.closeInventory();
            Craftmen.get().getFfaManager().join(player, game);
        } else {
            Consumer<Game> callback = gameCallbacks.remove(player.getUniqueId());
            playerPage.remove(player.getUniqueId());
            player.closeInventory();
            if (callback != null) callback.accept(game);
        }
    }

    // ── GUI construction ─────────────────────────────────────────────────────

    public void openGameSelector(Player player, Consumer<Game> onSelect) {
        gameCallbacks.put(player.getUniqueId(), onSelect);
        openPage(player, 0);
    }

    public void openFfaSelector(Player player, int page) {
        openFfaSelector(player, page, true);
    }

    private void openPage(Player player, int page) {
        Collection<Game> allGames = Craftmen.get().getGameManager().getGames();
        List<Game> page1Games = getPage1Games(allGames); // ordered, no Crystal
        Game crystalGame = getCrystalGame(allGames);
        List<Game> extraGames = getExtraGames(allGames); // everything not on page 1

        int totalPages = 1 + (int) Math.ceil(extraGames.size() / (double) ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPage.put(player.getUniqueId(), page);

        String title = GUI_TITLE_PREFIX + " §7(Page " + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, title);

        // Fill border with black stained glass
        fillBorder(inv);

        // Navigation buttons
        if (page > 0) inv.setItem(SLOT_PREV, makeArrow(true));
        if (page < totalPages - 1) inv.setItem(SLOT_NEXT, makeArrow(false));
        inv.setItem(SLOT_CLOSE, makeBarrier());
        inv.setItem(SLOT_PLAYER_KITS, makePlayerKitsButton());
        inv.setItem(SLOT_FFA, makeFfaButton());

        if (page == 0) {
            // Page 1: ordered kits in row 1 (slots 10-16), Crystal centered in row 2 (slot 22)
            int col = 0;
            for (Game g : page1Games) {
                if (col >= 7) break;
                inv.setItem(CONTENT_SLOTS[col], makeGameItem(g));
                col++;
            }
            if (crystalGame != null) {
                inv.setItem(CRYSTAL_SLOT, makeGameItem(crystalGame));
            }
        } else {
            // Extra pages: fill all 28 content slots
            int startIndex = (page - 1) * ITEMS_PER_PAGE;
            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                int gameIndex = startIndex + i;
                if (gameIndex >= extraGames.size()) break;
                inv.setItem(CONTENT_SLOTS[i], makeGameItem(extraGames.get(gameIndex)));
            }
        }

        player.openInventory(inv);
    }

    private void openFfaSelector(Player player, int page, boolean teleportToHub) {
        if (teleportToHub) player.teleport(Craftmen.get().getHubLocation());

        Collection<Game> allGames = Craftmen.get().getGameManager().getGames();
        List<Game> page1Games = getPage1Games(allGames);
        Game crystalGame = getCrystalGame(allGames);
        List<Game> extraGames = getExtraGames(allGames);

        int totalPages = 1 + (int) Math.ceil(extraGames.size() / (double) ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        ffaPage.put(player.getUniqueId(), page);

        String title = GUI_FFA_TITLE_PREFIX + " §7(Page " + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, title);
        fillBorder(inv);

        if (page > 0) inv.setItem(SLOT_PREV, makeArrow(true));
        if (page < totalPages - 1) inv.setItem(SLOT_NEXT, makeArrow(false));
        inv.setItem(SLOT_CLOSE, makeBarrier());

        if (page == 0) {
            int col = 0;
            for (Game g : page1Games) {
                if (col >= 7) break;
                inv.setItem(CONTENT_SLOTS[col], makeFfaGameItem(g));
                col++;
            }
            if (crystalGame != null) inv.setItem(CRYSTAL_SLOT, makeFfaGameItem(crystalGame));
        } else {
            int startIndex = (page - 1) * ITEMS_PER_PAGE;
            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                int gameIndex = startIndex + i;
                if (gameIndex >= extraGames.size()) break;
                inv.setItem(CONTENT_SLOTS[i], makeFfaGameItem(extraGames.get(gameIndex)));
            }
        }

        player.openInventory(inv);
    }

    private ItemStack makePlayerKitsButton() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(GUI_PLAYER_KITS_NAME);
        meta.setLore(Arrays.asList(
                "§7Browse & queue player-made kits.",
                "§7Hub-only. Kits expire after 7d inactivity."
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFfaButton() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(GUI_FFA_NAME);
        meta.setLore(Arrays.asList(
                "§7Free-for-all in a huge arena.",
                "§7Hub-only. Resets every 15 minutes."
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFfaGameItem(Game game) {
        ItemStack item = game.getIcon().clone();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cFFA §7- §a" + game.getName());

        int players = Craftmen.get().getFfaManager().getPlayersInFfa(game);
        item.setAmount(Math.max(1, Math.min(players, 99)));
        meta.setLore(Arrays.asList("§7Players: §a" + players, "§7Click to join"));
        item.setItemMeta(meta);
        return item;
    }

    // ── Sorting helpers ──────────────────────────────────────────────────────

    private List<Game> getPage1Games(Collection<Game> all) {
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

    private Game getCrystalGame(Collection<Game> all) {
        for (Game g : all) {
            if (g.getName().equalsIgnoreCase("Crystal")) return g;
        }
        return null;
    }

    private List<Game> getExtraGames(Collection<Game> all) {
        Set<String> reserved = new HashSet<>();
        for (String s : PAGE_ONE_KITS) reserved.add(s.toLowerCase());
        reserved.add("crystal");

        List<Game> result = new ArrayList<>();
        for (Game g : all) {
            if (!reserved.contains(g.getName().toLowerCase())) result.add(g);
        }
        return result;
    }

    // ── Item factories ───────────────────────────────────────────────────────

    private ItemStack makeBorderPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = makeBorderPane();
        // Top row (0-8) and bottom row (45-53)
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);
        // Left and right columns for rows 1-4
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, pane); // col 0
            inv.setItem(row * 9 + 8, pane); // col 8
        }
    }

    private ItemStack makeArrow(boolean left) {
        ItemStack arrow = new ItemStack(left ? Material.ARROW : Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.setDisplayName(left ? "§e◀ Previous" : "§eNext ▶");
        arrow.setItemMeta(meta);
        return arrow;
    }

    private ItemStack makeBarrier() {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        meta.setDisplayName("§cClose");
        barrier.setItemMeta(meta);
        return barrier;
    }

    private ItemStack makeGameItem(Game game) {
        ItemStack item = game.getIcon().clone();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a" + game.getName());

        int queuedPlayers = Craftmen.get().getQueueManager().getPlayersInQueue(game).size();

        int playingPlayers = game.getPlayersInGame().size();

        int totalPlayers = queuedPlayers + playingPlayers;

        int amount = Math.max(1, Math.min(totalPlayers, 99));
        item.setAmount(amount);

        List<String> lore = new ArrayList<>();
        lore.add("§7Players: §a" + totalPlayers);
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }
}
