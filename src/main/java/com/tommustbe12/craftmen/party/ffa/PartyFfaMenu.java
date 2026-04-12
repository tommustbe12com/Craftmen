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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PartyFfaMenu implements Listener {

    private static final String SETTINGS_TITLE = ChatColor.DARK_GRAY + "Party FFA Settings";
    private static final String TEAMS_TITLE = ChatColor.DARK_GRAY + "Party FFA Teams";
    private static final String PLAY_AGAIN_NAME = ChatColor.GOLD + "" + ChatColor.BOLD + "Play Party FFA Again";

    private final Map<UUID, UUID> pendingPartyByLeader = new HashMap<>();
    private final Map<UUID, Game> pendingGameByLeader = new HashMap<>();
    private final Map<UUID, Integer> pendingRoundsByLeader = new HashMap<>();
    private final Map<UUID, Boolean> pendingTeamsEnabledByLeader = new HashMap<>();
    private final Map<UUID, Map<UUID, Integer>> pendingTeamsByLeader = new HashMap<>();

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

        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) return;

        UUID leaderId = leader.getUniqueId();

        if (TEAMS_TITLE.equals(title)) {
            handleTeamsClick(leader, slot);
            return;
        }

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
                openTeams(leader);
            } else {
                openSettings(leader);
            }
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
            leader.closeInventory();
            leader.playSound(leader.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            Craftmen.get().getPartyManager().broadcastParty(party,
                    ChatColor.GOLD + "Party FFA starting: " + ChatColor.YELLOW + game.getName() + ChatColor.GRAY + " (" + rounds + " rounds)",
                    Sound.ENTITY_ENDER_DRAGON_GROWL);

            Craftmen.get().getFfaManager().joinPrivateParty(party.getId(), party.getMembers(), game, rounds, teamsEnabled, teamByPlayer);
        }
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

        int team = 1;
        for (UUID u : party.getMembers()) {
            map.putIfAbsent(u, team);
            team++;
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

        Inventory inv = Bukkit.createInventory(null, 54, TEAMS_TITLE);
        fill(inv);

        inv.setItem(45, make(Material.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to settings", Sound.UI_BUTTON_CLICK));
        inv.setItem(49, make(Material.NETHER_STAR, ChatColor.AQUA + "Randomize", ChatColor.GRAY + "Random teams", Sound.UI_BUTTON_CLICK));
        inv.setItem(53, make(Material.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close", Sound.UI_BUTTON_CLICK));

        int slot = 10;
        for (UUID u : party.getMembers()) {
            Player p = Bukkit.getPlayer(u);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta sm) {
                if (p != null) sm.setOwningPlayer(p);
                int team = teams.getOrDefault(u, 1);
                String name = (p != null ? p.getName() : u.toString());
                sm.setDisplayName(ChatColor.WHITE + name);
                sm.setLore(java.util.List.of(
                        ChatColor.GRAY + "Team: " + ChatColor.GREEN + team,
                        ChatColor.YELLOW + "Click to change"
                ));
                head.setItemMeta(sm);
            }

            inv.setItem(slot, head);
            slot++;
            if (slot % 9 == 8) slot += 2; // skip right border
            if (slot >= 44) break;
        }

        leader.openInventory(inv);
    }

    private void handleTeamsClick(Player leader, int rawSlot) {
        UUID leaderId = leader.getUniqueId();

        if (rawSlot == 45) {
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            openSettings(leader);
            return;
        }
        if (rawSlot == 49) {
            randomizeTeams(leader);
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            openTeams(leader);
            return;
        }
        if (rawSlot == 53) {
            leader.closeInventory();
            return;
        }

        ItemStack clicked = leader.getOpenInventory().getTopInventory().getItem(rawSlot);
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        UUID partyId = pendingPartyByLeader.get(leaderId);
        Party party = partyId == null ? null : Craftmen.get().getPartyManager().getPartyById(partyId);
        if (party == null) return;

        String clickedName = ChatColor.stripColor(meta.getDisplayName());
        UUID memberId = null;
        for (UUID u : party.getMembers()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.getName().equalsIgnoreCase(clickedName)) {
                memberId = u;
                break;
            }
        }
        if (memberId == null) return;

        Map<UUID, Integer> teams = pendingTeamsByLeader.computeIfAbsent(leaderId, k -> new HashMap<>());
        int maxTeams = Math.max(2, party.size());
        int current = teams.getOrDefault(memberId, 1);
        int next = current + 1;
        if (next > maxTeams) next = 1;
        teams.put(memberId, next);
        leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        openTeams(leader);
    }

    private void randomizeTeams(Player leader) {
        UUID leaderId = leader.getUniqueId();
        UUID partyId = pendingPartyByLeader.get(leaderId);
        Party party = partyId == null ? null : Craftmen.get().getPartyManager().getPartyById(partyId);
        if (party == null) return;

        Map<UUID, Integer> teams = pendingTeamsByLeader.computeIfAbsent(leaderId, k -> new HashMap<>());
        java.util.List<UUID> members = new java.util.ArrayList<>(party.getMembers());
        java.util.Collections.shuffle(members);

        int half = Math.max(1, members.size() / 2);
        for (int i = 0; i < members.size(); i++) {
            teams.put(members.get(i), (i < half) ? 1 : 2);
        }
    }
}
