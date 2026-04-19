package com.tommustbe12.craftmen.party.ffa;

import com.tommustbe12.craftmen.Craftmen;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PartyTeamsMenu implements Listener {

    private static final String TITLE = "§8Party Teams";

    // Right column helper slots (visible at a glance).
    private static final int SLOT_HELP = 8;
    private static final int SLOT_TOGGLE_TEAM_MODE = 17;
    private static final int SLOT_SAVE = 26;
    private static final int SLOT_RESET = 35;
    private static final int SLOT_CLOSE = 53;

    private final Map<UUID, UUID> editingPartyByLeader = new HashMap<>();
    private final Map<UUID, Map<UUID, Integer>> draftTeamsByLeader = new HashMap<>();
    private final Map<UUID, Boolean> draftEnabledByLeader = new HashMap<>();

    public void open(Player leader, Party party) {
        if (leader == null || party == null) return;
        if (!party.getLeader().equals(leader.getUniqueId())) return;

        editingPartyByLeader.put(leader.getUniqueId(), party.getId());

        Map<UUID, Integer> draft = draftTeamsByLeader.computeIfAbsent(leader.getUniqueId(), k -> new HashMap<>());
        for (UUID member : party.getMembers()) {
            draft.putIfAbsent(member, 1);
        }
        draftEnabledByLeader.putIfAbsent(leader.getUniqueId(), false);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv);

        boolean enabled = draftEnabledByLeader.getOrDefault(leader.getUniqueId(), false);
        inv.setItem(SLOT_HELP, item(Material.BOOK,
                "§eHow to use",
                List.of(
                        "§7Click a player head to change their team.",
                        "§8Left-click: team +1",
                        "§8Right-click: team -1"
                )));
        inv.setItem(SLOT_TOGGLE_TEAM_MODE, item(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "§eTeam Mode: " + (enabled ? "§aON" : "§cOFF"),
                List.of("§7If ON: teammates can't hit each other.", "§8Click to toggle")));
        inv.setItem(SLOT_SAVE, item(Material.EMERALD_BLOCK, "§aSave", List.of("§7Save teams for party FFA")));
        inv.setItem(SLOT_RESET, item(Material.BARRIER, "§cReset teams", List.of("§7Set everyone to Team 1")));
        inv.setItem(SLOT_CLOSE, item(Material.OAK_DOOR, "§cClose", List.of("§7Close")));

        // Members list (slots 10..43 typical)
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
        int idx = 0;
        for (UUID member : party.getMembers()) {
            if (idx >= slots.length) break;
            Player p = Bukkit.getPlayer(member);
            String name = p != null ? p.getName() : member.toString();
            int team = draft.getOrDefault(member, 1);
            inv.setItem(slots[idx++], item(Material.PLAYER_HEAD,
                    "§f" + name,
                    List.of(
                            "§7Team: §b" + team,
                            "§8Left-click: team +1",
                            "§8Right-click: team -1"
                    )));
        }

        leader.openInventory(inv);
        leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player leader)) return;
        if (e.getView() == null) return;
        String stripped = ChatColor.stripColor(e.getView().getTitle());
        if (stripped == null || !stripped.equals(ChatColor.stripColor(TITLE))) return;
        e.setCancelled(true);

        // Non-interactive helper slots.
        int slot = e.getRawSlot();
        if (slot == SLOT_HELP) return;

        UUID partyId = editingPartyByLeader.get(leader.getUniqueId());
        if (partyId == null) return;
        Party party = Craftmen.get().getPartyManager().getPartyById(partyId);
        if (party == null) return;
        if (!party.getLeader().equals(leader.getUniqueId())) return;

        if (slot == SLOT_CLOSE) {
            leader.closeInventory();
            return;
        }
        if (slot == SLOT_TOGGLE_TEAM_MODE) {
            boolean cur = draftEnabledByLeader.getOrDefault(leader.getUniqueId(), false);
            draftEnabledByLeader.put(leader.getUniqueId(), !cur);
            open(leader, party);
            return;
        }
        if (slot == SLOT_SAVE) {
            boolean enabled = draftEnabledByLeader.getOrDefault(leader.getUniqueId(), false);
            Map<UUID, Integer> teams = new HashMap<>(draftTeamsByLeader.getOrDefault(leader.getUniqueId(), Map.of()));
            Craftmen.get().getFfaManager().setPendingPartyTeamSettings(party.getId(), enabled, teams);
            leader.sendMessage("§aSaved party teams.");
            leader.playSound(leader.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
            leader.closeInventory();
            return;
        }
        if (slot == SLOT_RESET) {
            Map<UUID, Integer> draft = draftTeamsByLeader.computeIfAbsent(leader.getUniqueId(), k -> new HashMap<>());
            for (UUID member : party.getMembers()) {
                draft.put(member, 1);
            }
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            open(leader, party);
            return;
        }

        // member slots: resolve by display name match (simple + sufficient)
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String disp = clicked.getItemMeta().getDisplayName();
        if (disp == null) return;
        String name = ChatColor.stripColor(disp);
        if (name == null || name.isBlank()) return;

        Player target = Bukkit.getPlayerExact(name);
        if (target == null) return;
        if (!party.getMembers().contains(target.getUniqueId())) return;

        Map<UUID, Integer> draft = draftTeamsByLeader.computeIfAbsent(leader.getUniqueId(), k -> new HashMap<>());
        int cur = draft.getOrDefault(target.getUniqueId(), 1);
        if (e.isLeftClick()) cur++;
        else if (e.isRightClick()) cur = Math.max(1, cur - 1);
        draft.put(target.getUniqueId(), cur);
        leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        open(leader, party);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView() == null) return;
        String stripped = ChatColor.stripColor(e.getView().getTitle());
        if (stripped == null || !stripped.equals(ChatColor.stripColor(TITLE))) return;
        e.setCancelled(true);
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void fill(Inventory inv) {
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
}
