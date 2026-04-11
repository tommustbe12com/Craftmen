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

    private static final String SETTINGS_TITLE = "§8Party FFA Settings";
    private static final String PLAY_AGAIN_NAME = "§6§lPlay Party FFA Again";

    private final Map<UUID, UUID> pendingPartyByLeader = new HashMap<>();
    private final Map<UUID, Game> pendingGameByLeader = new HashMap<>();
    private final Map<UUID, Integer> pendingRoundsByLeader = new HashMap<>();

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

        inv.setItem(11, make(Material.NETHER_STAR, "§b§lGame", game == null ? "§7Not selected" : "§f" + game.getName(), Sound.UI_BUTTON_CLICK));
        inv.setItem(13, make(Material.COMPARATOR, "§d§lRounds", "§f" + rounds + " §7(1-10)", Sound.UI_BUTTON_CLICK));
        inv.setItem(15, make(Material.EMERALD_BLOCK, "§a§lStart", "§7Start party FFA", Sound.UI_TOAST_CHALLENGE_COMPLETE));

        // Quick round buttons
        for (int i = 1; i <= 10; i++) {
            Material mat = (i == rounds) ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            inv.setItem(i - 1, make(mat, "§e" + i, (i == rounds) ? "§aSelected" : "§7Click to set", Sound.UI_BUTTON_CLICK));
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
        if (!SETTINGS_TITLE.equals(e.getView().getTitle())) return;
        if (!(e.getWhoClicked() instanceof Player leader)) return;

        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) return;

        UUID leaderId = leader.getUniqueId();

        // round buttons (0..9)
        if (slot >= 0 && slot <= 9) {
            int rounds = slot + 1;
            pendingRoundsByLeader.put(leaderId, rounds);
            leader.playSound(leader.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            openSettings(leader);
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
            leader.closeInventory();
            leader.playSound(leader.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            Craftmen.get().getPartyManager().broadcastParty(party,
                    "§6Party FFA starting: §e" + game.getName() + " §7(" + rounds + " rounds)",
                    Sound.ENTITY_ENDER_DRAGON_GROWL);

            Craftmen.get().getFfaManager().joinPrivateParty(party.getId(), party.getMembers(), game, rounds);
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
}

