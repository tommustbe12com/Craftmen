package com.tommustbe12.craftmen.party.ffa;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PartyFfaMenu implements Listener {

    private static final String SETTINGS_TITLE = ChatColor.DARK_GRAY + "Party FFA Settings";
    private static final String TEAMS_TITLE = ChatColor.DARK_GRAY + "Party FFA Teams";
    private static final String PLAY_AGAIN_NAME = ChatColor.GOLD + "" + ChatColor.BOLD + "Play Party FFA Again";

    private static final int MAX_TEAMS = 4;
    private static final int TEAM_AREA_SIZE = 36; // 4 rows * 9 columns

    private final Map<UUID, UUID> pendingPartyByLeader = new HashMap<>();
    private final Map<UUID, Game> pendingGameByLeader = new HashMap<>();
    private final Map<UUID, Integer> pendingRoundsByLeader = new HashMap<>();
    private final Map<UUID, Boolean> pendingTeamsEnabledByLeader = new HashMap<>();
    private final Map<UUID, Map<UUID, Integer>> pendingTeamsByLeader = new HashMap<>();
    private final Map<UUID, Boolean> pendingTeamsCustomizeByLeader = new HashMap<>();
    private final Map<UUID, Map<Integer, Game>> pendingKitByTeamByLeader = new HashMap<>();

    private static final String TEAM_KITS_TITLE = ChatColor.DARK_GRAY + "Party FFA Team Kits";

    public void openGameSelect(Player leader, Party party) {
        if (leader == null || party == null) return;
        pendingPartyByLeader.put(leader.getUniqueId(), party.getId());

        Craftmen.get().getHubManager().openGameSelector(leader, (Game game) -> {
            if (game == null) return;
            pendingGameByLeader.put(leader.getUniqueId(), game);
            pendingRoundsByLeader.putIfAbsent(leader.getUniqueId(), 1);
            openSettings(leader);
        });
    }

    public void givePlayAgain(Player leader) {
        if (leader == null || !leader.isOnline()) return;
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PLAY_AGAIN_NAME);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        leader.getInventory().setItem(1, item);
        leader.updateInventory();
    }

    private void openSettings(Player leader) {
        Inventory inv = Bukkit.createInventory(null, 27, SETTINGS_TITLE);
        fill(inv);

        Integer rounds = pendingRoundsByLeader.getOrDefault(leader.getUniqueId(), 1);
        Game game = pendingGameByLeader.get(leader.getUniqueId());
        boolean teamsEnabled = pendingTeamsEnabledByLeader.getOrDefault(leader.getUniqueId(), false);

        inv.setItem(11, make(Material.NETHER_STAR, ChatColor.AQUA + "" + ChatColor.BOLD + "Game", game == null ? (ChatColor.GRAY + "Not selected") : (ChatColor.WHITE + game.getName()), Sound.UI_BUTTON_CLICK));
        inv.setItem(13, make(Material.COMPARATOR, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Rounds", ChatColor.WHITE + "" + rounds + ChatColor.GRAY + " (1-10)", Sound.UI_BUTTON_CLICK));
        inv.setItem(15, make(Material.EMERALD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "Start", ChatColor.GRAY + "Start party FFA", Sound.UI_TOAST_CHALLENGE_COMPLETE));
        inv.setItem(17, make(teamsEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Teams",
                teamsEnabled ? (ChatColor.GREEN + "Enabled") : (ChatColor.GRAY + "Disabled"),
                Sound.UI_BUTTON_CLICK));
        if (teamsEnabled) {
            inv.setItem(26, make(Material.CHEST, ChatColor.AQUA + "" + ChatColor.BOLD + "Team Kits", ChatColor.GRAY + "Choose a kit per team", Sound.UI_BUTTON_CLICK));
        }

        // Quick round buttons
        for (int i = 1; i <= 10; i++) {
            Material mat = (i == rounds) ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            inv.setItem(i - 1, make(mat, ChatColor.YELLOW + "" + i, (i == rounds) ? (ChatColor.GREEN + "Selected") : (ChatColor.GRAY + "Click to set"), Sound.UI_BUTTON_CLICK));
        }

        leader.openInventory(inv);
    }

    private void fill(Inventory inv) {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }

    private ItemStack make(Material mat, String name, String lore, Sound sound) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.List.of(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null) return;
        String title = e.getView().getTitle();
        if (!SETTINGS_TITLE.equals(title) && !TEAMS_TITLE.equals(title)) return;
        if (!(e.getWhoClicked() instanceof Player leader)) return;

        e.setCancelled(true); // we manually manage all movement in these menus

        int slot = e.getRawSlot();
        if (slot < 0) return;

        UUID leaderId = leader.getUniqueId();

        if (TEAMS_TITLE.equals(title)) {
            handleTeamsClick(e, leader, slot);
            return;
        }

        if (slot < 0 || slot >= e.getInventory().getSize()) return;

        // round buttons (0..9)
        if (slot >= 0 && slot <= 9) {
            int rounds = slot + 1;
            pendingRoundsByLeader.put(leaderId, rounds);
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            openSettings(leader);
            return;
        }

        // teams button
        if (slot == 17) {
            boolean enabled = pendingTeamsEnabledByLeader.getOrDefault(leaderId, false);
            pendingTeamsEnabledByLeader.put(leaderId, !enabled);
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            if (!enabled) {
                initTeamsIfMissing(leader);
                pendingTeamsCustomizeByLeader.put(leaderId, false);
                openTeams(leader);
            } else {
                openSettings(leader);
            }
            return;
        }

        if (slot == 26) {
            boolean teamsEnabled = pendingTeamsEnabledByLeader.getOrDefault(leaderId, false);
            if (!teamsEnabled) return;
            openTeamKits(leader);
            return;
        }

        // start button
        if (slot == 15) {
            UUID partyId = pendingPartyByLeader.get(leaderId);
            if (partyId == null) {
                leader.sendMessage(ChatColor.RED + "Party not found.");
                leader.closeInventory();
                return;
            }
            Party party = Craftmen.get().getPartyManager().getPartyById(partyId);
            if (party == null) {
                leader.sendMessage(ChatColor.RED + "Party not found.");
                leader.closeInventory();
                return;
            }
            if (!party.getLeader().equals(leaderId)) {
                leader.sendMessage(ChatColor.RED + "Only the party leader can start.");
                leader.playSound(leader.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                leader.closeInventory();
                return;
            }

            Game game = pendingGameByLeader.get(leaderId);
            if (game == null) {
                leader.sendMessage(ChatColor.RED + "Pick a game first.");
                leader.playSound(leader.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            int rounds = pendingRoundsByLeader.getOrDefault(leaderId, 1);
            boolean teamsEnabled = pendingTeamsEnabledByLeader.getOrDefault(leaderId, false);
            Map<UUID, Integer> teamByPlayer = pendingTeamsByLeader.getOrDefault(leaderId, java.util.Collections.emptyMap());
            Map<Integer, Game> kitByTeam = pendingKitByTeamByLeader.getOrDefault(leaderId, java.util.Collections.emptyMap());
            leader.closeInventory();
            leader.playSound(leader.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            Craftmen.get().getPartyManager().broadcastParty(party,
                    ChatColor.GOLD + "Party FFA starting: " + ChatColor.YELLOW + game.getName() + ChatColor.GRAY + " (" + rounds + " rounds)",
                    Sound.ENTITY_ENDER_DRAGON_GROWL);

            Craftmen.get().getFfaManager().joinPrivateParty(party.getId(), party.getMembers(), game, rounds, teamsEnabled, teamByPlayer, kitByTeam);
        }
    }

    private void openTeamKits(Player leader) {
        UUID leaderId = leader.getUniqueId();
        UUID partyId = pendingPartyByLeader.get(leaderId);
        Party party = partyId == null ? null : Craftmen.get().getPartyManager().getPartyById(partyId);
        if (party == null) {
            leader.sendMessage(ChatColor.RED + "Party not found.");
            leader.closeInventory();
            return;
        }

        Map<Integer, Game> map = pendingKitByTeamByLeader.computeIfAbsent(leaderId, k -> new HashMap<>());
        Game fallback = pendingGameByLeader.get(leaderId);
        for (int t = 1; t <= MAX_TEAMS; t++) {
            if (!map.containsKey(t) && fallback != null) map.put(t, fallback);
        }

        Inventory inv = Bukkit.createInventory(null, 27, TEAM_KITS_TITLE);
        fill(inv);

        inv.setItem(18, make(Material.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to settings", Sound.UI_BUTTON_CLICK));
        inv.setItem(26, make(Material.EMERALD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "Done", ChatColor.GRAY + "Save team kits", Sound.UI_TOAST_CHALLENGE_COMPLETE));

        inv.setItem(10, teamKitItem(map.get(1), 1));
        inv.setItem(12, teamKitItem(map.get(2), 2));
        inv.setItem(14, teamKitItem(map.get(3), 3));
        inv.setItem(16, teamKitItem(map.get(4), 4));

        leader.openInventory(inv);
        leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    private ItemStack teamKitItem(Game game, int team) {
        Material mat = switch (team) {
            case 1 -> Material.RED_CONCRETE;
            case 2 -> Material.BLUE_CONCRETE;
            case 3 -> Material.GREEN_CONCRETE;
            default -> Material.YELLOW_CONCRETE;
        };
        String kit = game == null ? "None" : game.getName();
        return make(mat,
                ChatColor.GOLD + "Team " + team,
                ChatColor.GRAY + "Kit: " + ChatColor.AQUA + kit + ChatColor.GRAY + " (click to change)",
                Sound.UI_BUTTON_CLICK);
    }

    @EventHandler
    public void onTeamKitsClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player leader)) return;
        if (e.getView() == null) return;
        String title = e.getView().getTitle();
        if (!TEAM_KITS_TITLE.equals(title)) return;
        e.setCancelled(true);

        UUID leaderId = leader.getUniqueId();
        UUID partyId = pendingPartyByLeader.get(leaderId);
        Party party = partyId == null ? null : Craftmen.get().getPartyManager().getPartyById(partyId);
        if (party == null) return;
        if (!party.getLeader().equals(leaderId)) return;

        int slot = e.getRawSlot();
        if (slot == 18) {
            openSettings(leader);
            return;
        }
        if (slot == 26) {
            openSettings(leader);
            return;
        }

        int team = switch (slot) {
            case 10 -> 1;
            case 12 -> 2;
            case 14 -> 3;
            case 16 -> 4;
            default -> -1;
        };
        if (team == -1) return;

        // Reuse the hub game selector to pick a kit, then store it for this team.
        Craftmen.get().getHubManager().openGameSelector(leader, picked -> {
            if (picked == null) return;
            pendingKitByTeamByLeader.computeIfAbsent(leaderId, k -> new HashMap<>()).put(team, picked);
            Bukkit.getScheduler().runTask(Craftmen.get(), () -> openTeamKits(leader));
        });
    }

    @EventHandler
    public void onPlayAgainUse(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null) return;
        if (item.getType() != Material.PAPER) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        if (!PLAY_AGAIN_NAME.equals(meta.getDisplayName())) return;

        Party party = Craftmen.get().getPartyManager().getParty(player);
        if (party == null) {
            player.sendMessage(ChatColor.RED + "You are not in a party.");
            return;
        }
        if (!party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the party leader can start.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        e.setCancelled(true);
        openGameSelect(player, party);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    private void initTeamsIfMissing(Player leader) {
        UUID leaderId = leader.getUniqueId();
        Map<UUID, Integer> map = pendingTeamsByLeader.computeIfAbsent(leaderId, k -> new HashMap<>());
        UUID partyId = pendingPartyByLeader.get(leaderId);
        Party party = partyId == null ? null : Craftmen.get().getPartyManager().getPartyById(partyId);
        if (party == null) return;

        // Default: spread across up to 4 teams (round-robin).
        List<UUID> members = new java.util.ArrayList<>(party.getMembers());
        for (int i = 0; i < members.size(); i++) {
            map.putIfAbsent(members.get(i), (i % MAX_TEAMS) + 1);
        }
    }

    private void openTeams(Player leader) {
        UUID leaderId = leader.getUniqueId();
        UUID partyId = pendingPartyByLeader.get(leaderId);
        Party party = partyId == null ? null : Craftmen.get().getPartyManager().getPartyById(partyId);
        if (party == null) {
            leader.sendMessage(ChatColor.RED + "Party not found.");
            leader.closeInventory();
            return;
        }

        initTeamsIfMissing(leader);
        Map<UUID, Integer> teams = pendingTeamsByLeader.get(leaderId);
        boolean customize = pendingTeamsCustomizeByLeader.getOrDefault(leaderId, false);

        Inventory inv = Bukkit.createInventory(null, 54, TEAMS_TITLE);
        fill(inv);

        inv.setItem(45, make(Material.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to settings", Sound.UI_BUTTON_CLICK));
        inv.setItem(47, make(customize ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Customize Teams",
                customize ? (ChatColor.GREEN + "ON") : (ChatColor.GRAY + "OFF"),
                Sound.UI_BUTTON_CLICK));
        inv.setItem(49, make(Material.NETHER_STAR, ChatColor.AQUA + "Randomize", ChatColor.GRAY + "Shuffle into 4 teams", Sound.UI_BUTTON_CLICK));
        inv.setItem(51, make(Material.EMERALD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm", ChatColor.GRAY + "Save & lock in teams", Sound.UI_TOAST_CHALLENGE_COMPLETE));
        inv.setItem(53, make(Material.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close", Sound.UI_BUTTON_CLICK));

        // 4 rows (teams 1-4): slots 0..35. Heads can only be moved within this area when customize is ON.
        // Row labels (right side of each row) to make it clearer which row is which team.
        inv.setItem(8, make(Material.RED_CONCRETE, ChatColor.RED + "Team 1", ChatColor.GRAY + "Top row", Sound.UI_BUTTON_CLICK));
        inv.setItem(17, make(Material.BLUE_CONCRETE, ChatColor.BLUE + "Team 2", ChatColor.GRAY + "2nd row", Sound.UI_BUTTON_CLICK));
        inv.setItem(26, make(Material.GREEN_CONCRETE, ChatColor.GREEN + "Team 3", ChatColor.GRAY + "3rd row", Sound.UI_BUTTON_CLICK));
        inv.setItem(35, make(Material.YELLOW_CONCRETE, ChatColor.YELLOW + "Team 4", ChatColor.GRAY + "Bottom row", Sound.UI_BUTTON_CLICK));

        Set<UUID> placed = new HashSet<>();
        for (int team = 1; team <= MAX_TEAMS; team++) {
            int rowStart = (team - 1) * 9;
            int col = 0;
            for (UUID memberId : party.getMembers()) {
                if (placed.contains(memberId)) continue;
                if (teams.getOrDefault(memberId, 1) != team) continue;
                if (col >= 9) break;

                inv.setItem(rowStart + col, makeMemberHead(memberId, teams.getOrDefault(memberId, 1), customize));
                placed.add(memberId);
                col++;
            }
        }
        // Any missing/unassigned members: place them into the first available slot and assign a team based on row.
        for (UUID memberId : party.getMembers()) {
            if (placed.contains(memberId)) continue;
            int firstEmpty = firstEmptyTeamSlot(inv);
            if (firstEmpty < 0) break;
            int team = (firstEmpty / 9) + 1;
            teams.put(memberId, team);
            inv.setItem(firstEmpty, makeMemberHead(memberId, team, customize));
            placed.add(memberId);
        }

        leader.openInventory(inv);
    }

    private void handleTeamsClick(InventoryClickEvent e, Player leader, int rawSlot) {
        UUID leaderId = leader.getUniqueId();

        // Only allow interacting with the top inventory; prevent shift-clicking into/from player inventory.
        int topSize = leader.getOpenInventory().getTopInventory().getSize();
        if (rawSlot >= topSize) return;

        if (rawSlot == 45) {
            clearCursorIfMemberHead(e);
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            openSettings(leader);
            return;
        }
        if (rawSlot == 47) {
            boolean cur = pendingTeamsCustomizeByLeader.getOrDefault(leaderId, false);
            pendingTeamsCustomizeByLeader.put(leaderId, !cur);
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            openTeams(leader);
            return;
        }
        if (rawSlot == 49) {
            randomizeTeams(leader);
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            openTeams(leader);
            return;
        }
        if (rawSlot == 51) {
            clearCursorIfMemberHead(e);
            if (!validateTeams(leader)) {
                leader.playSound(leader.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                openTeams(leader);
                return;
            }
            // Teams are stored in pendingTeamsByLeader already; confirming just returns to settings.
            leader.playSound(leader.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            openSettings(leader);
            return;
        }
        if (rawSlot == 53) {
            clearCursorIfMemberHead(e);
            leader.closeInventory();
            return;
        }

        boolean customize = pendingTeamsCustomizeByLeader.getOrDefault(leaderId, false);
        if (!customize) return;

        // Team area: rows 0..3 (slots 0..35)
        if (rawSlot < 0 || rawSlot >= TEAM_AREA_SIZE) return;

        ItemStack cursor = e.getCursor();
        ItemStack clicked = e.getCurrentItem();

        boolean cursorHead = isMemberHead(cursor);
        boolean clickedHead = isMemberHead(clicked);

        // Disallow placing arbitrary items into the menu.
        if (cursor != null && cursor.getType() != Material.AIR && !cursorHead) return;
        if (clicked != null && clicked.getType() != Material.AIR && !clickedHead) return;

        // Pick up
        if (!cursorHead && clickedHead) {
            e.setCursor(clicked);
            e.setCurrentItem(null);
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            return;
        }

        // Place / swap
        if (cursorHead) {
            e.setCursor(clickedHead ? clicked : null);
            e.setCurrentItem(cursor);
            syncTeamAssignmentFromSlot(leader, rawSlot, cursor);
            if (clickedHead) {
                // the previous occupant moved to cursor; no team sync until they are placed
            }
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        }
    }

    private void randomizeTeams(Player leader) {
        UUID leaderId = leader.getUniqueId();
        UUID partyId = pendingPartyByLeader.get(leaderId);
        Party party = partyId == null ? null : Craftmen.get().getPartyManager().getPartyById(partyId);
        if (party == null) return;

        Map<UUID, Integer> teams = pendingTeamsByLeader.computeIfAbsent(leaderId, k -> new HashMap<>());
        java.util.List<UUID> members = new java.util.ArrayList<>(party.getMembers());
        java.util.Collections.shuffle(members);

        for (int i = 0; i < members.size(); i++) {
            teams.put(members.get(i), (i % MAX_TEAMS) + 1);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView() == null) return;
        String title = e.getView().getTitle();
        if (!TEAMS_TITLE.equals(title) && !SETTINGS_TITLE.equals(title)) return;
        e.setCancelled(true);
    }

    private static boolean isMemberHead(ItemStack item) {
        return item != null && item.getType() == Material.PLAYER_HEAD && item.hasItemMeta();
    }

    private static void clearCursorIfMemberHead(InventoryClickEvent e) {
        if (e == null) return;
        ItemStack cursor = e.getCursor();
        if (isMemberHead(cursor)) e.setCursor(null);
    }

    private ItemStack makeMemberHead(UUID memberId, int team, boolean customize) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta sm) {
            sm.setOwningPlayer(Bukkit.getOfflinePlayer(memberId));
            String name = Bukkit.getOfflinePlayer(memberId).getName();
            sm.setDisplayName(ChatColor.WHITE + (name == null ? memberId.toString() : name));
            sm.setLore(List.of(
                    ChatColor.GRAY + "Team: " + ChatColor.GREEN + team,
                    customize ? (ChatColor.YELLOW + "Drag between rows to change teams") : (ChatColor.YELLOW + "Enable Customize to move")
            ));
            head.setItemMeta(sm);
        }
        return head;
    }

    private static int firstEmptyTeamSlot(Inventory inv) {
        for (int i = 0; i < TEAM_AREA_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() == Material.AIR) return i;
        }
        return -1;
    }

    private void syncTeamAssignmentFromSlot(Player leader, int rawSlot, ItemStack head) {
        if (leader == null || head == null) return;
        if (!(head.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta sm)) return;
        if (sm.getOwningPlayer() == null) return;

        UUID leaderId = leader.getUniqueId();
        UUID memberId = sm.getOwningPlayer().getUniqueId();
        int team = (rawSlot / 9) + 1;
        if (team < 1 || team > MAX_TEAMS) return;

        Map<UUID, Integer> teams = pendingTeamsByLeader.computeIfAbsent(leaderId, k -> new HashMap<>());
        teams.put(memberId, team);

        // Refresh lore (team number) on the moved head.
        head.setItemMeta(makeMemberHead(memberId, team, true).getItemMeta());
    }

    private boolean validateTeams(Player leader) {
        UUID leaderId = leader.getUniqueId();
        UUID partyId = pendingPartyByLeader.get(leaderId);
        Party party = partyId == null ? null : Craftmen.get().getPartyManager().getPartyById(partyId);
        if (party == null) {
            leader.sendMessage(ChatColor.RED + "Party not found.");
            return false;
        }

        Map<UUID, Integer> teams = pendingTeamsByLeader.getOrDefault(leaderId, Map.of());
        Set<Integer> used = new HashSet<>();
        for (UUID u : party.getMembers()) {
            Integer t = teams.get(u);
            if (t == null) continue;
            used.add(t);
        }

        // Must have at least 2 opposing teams.
        if (used.size() < 2) {
            leader.sendMessage(ChatColor.RED + "You must have at least 2 teams.");
            leader.sendMessage(ChatColor.GRAY + "Move at least one player to a different team.");
            return false;
        }
        return true;
    }
}
