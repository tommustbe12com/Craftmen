package com.tommustbe12.craftmen.help;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public final class HelpMenu implements Listener {

    private static final String TITLE_MAIN = "§8Craftmen Help";
    private static final String TITLE_COMMANDS = "§8Commands";
    private static final String TITLE_CREDITS = "§8Credits";

    private static final int SIZE = 54;

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE_MAIN);
        fillBorder(inv);

        // Center content (row 3-ish)
        inv.setItem(22, item(Material.BOOK,
                "§b§lCraftmen",
                List.of(
                        "§7A PvP match-making server",
                        "§7designed for ease-of-use",
                        "§7and player experience."
                )));

        inv.setItem(20, item(Material.LANTERN,
                "§e§lCommands",
                List.of("§7Click to view commands", "§8(with aliases)")));

        inv.setItem(24, item(Material.PLAYER_HEAD,
                "§d§lCredits",
                List.of("§7Click to view the team", "§7and contributors")));

        inv.setItem(30, item(Material.LEAD,
                "§6§lParties",
                List.of(
                        "§7/party create, invite, accept",
                        "§7/party ffa (leader) starts",
                        "§7a private party FFA",
                        "§7/pc toggles party chat"
                )));

        inv.setItem(31, item(Material.CHEST,
                "§a§lCustom Kits",
                List.of(
                        "§7Create and edit kits in hub",
                        "§7Then queue & fight with them",
                        "§7(see Player Kits menu)"
                )));

        inv.setItem(32, item(Material.NAME_TAG,
                "§b§lBadges",
                List.of(
                        "§7Unlock badges by progress",
                        "§7Badges are cosmetic",
                        "§7and show off skill"
                )));

        inv.setItem(33, item(Material.EMERALD,
                "§b§lGems",
                List.of(
                        "§7Earn gems from badges",
                        "§7and win streak milestones",
                        "§7Use gems in /shop"
                )));

        inv.setItem(49, item(Material.BARRIER, "§cClose", List.of("§7Close this menu")));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    private void openCommands(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE_COMMANDS);
        fillBorder(inv);

        inv.setItem(49, item(Material.ARROW, "§eBack", List.of("§7Return to help")));

        // A clean list of common commands
        inv.setItem(20, item(Material.IRON_SWORD, "§6Queue / Duel",
                List.of(
                        "§7/queue §8(aliases: /q)",
                        "§7/duel <player>",
                        "§7/accept <player>",
                        "§7/forfeit §8(aliases: /f)"
                )));

        inv.setItem(21, item(Material.TNT, "§cFFA",
                List.of(
                        "§7/ffa",
                        "§7/hub §8(leaves FFA too)"
                )));

        inv.setItem(22, item(Material.LEAD, "§6Parties",
                List.of(
                        "§7/party §8(aliases: /p)",
                        "§7/pc [msg] §8(party chat)",
                        "§7/party ffa §8(leader)"
                )));

        inv.setItem(23, item(Material.NAME_TAG, "§bBadges",
                List.of(
                        "§7/badges",
                        "§7/badgeadmin §8(op only)"
                )));

        inv.setItem(24, item(Material.EMERALD, "§bGems / Shop",
                List.of(
                        "§7/gems",
                        "§7/shop",
                        "§7/gems add|sub|set ... §8(op only)"
                )));

        inv.setItem(25, item(Material.END_PORTAL_FRAME, "§5End Fight",
                List.of("§7/endfight §8(aliases: /ef, /end)")));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    private void openCredits(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE_CREDITS);
        fillBorder(inv);
        inv.setItem(49, item(Material.ARROW, "§eBack", List.of("§7Return to help")));

        // Ordered list
        inv.setItem(10, head("TomMustBe12", "§6TomMustBe12", List.of("§7Owner / Developer")));
        inv.setItem(12, head("BionicleBlaster", "§eBionicleBlaster", List.of("§7Manager")));
        inv.setItem(14, head("Zembiii", "§dZembiii", List.of("§7Staff / Creative Team")));
        inv.setItem(16, head("mecringe", "§dmecringe", List.of("§7Staff / Creative Team")));
        inv.setItem(28, head("nytsom", "§bnytsom", List.of("§7Staff")));
        inv.setItem(30, head("Rysterio", "§aRysterio", List.of("§7Map Builder")));

        inv.setItem(32, item(Material.PAPER, "§f§lNote",
                List.of("§7Thank you to all beta testers!", "§8We appreciate you.")));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getView() == null || e.getView().getTitle() == null) return;

        String stripped = ChatColor.stripColor(e.getView().getTitle());
        if (stripped == null) return;

        boolean main = stripped.equals(ChatColor.stripColor(TITLE_MAIN));
        boolean cmds = stripped.equals(ChatColor.stripColor(TITLE_COMMANDS));
        boolean credits = stripped.equals(ChatColor.stripColor(TITLE_CREDITS));
        if (!main && !cmds && !credits) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = e.getRawSlot();

        if (main) {
            if (slot == 20) {
                openCommands(player);
                return;
            }
            if (slot == 24) {
                openCredits(player);
                return;
            }
            if (slot == 49) {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                return;
            }
        }

        if ((cmds || credits) && slot == 49) {
            openMain(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        String stripped = ChatColor.stripColor(e.getView().getTitle());
        if (stripped == null) return;
        if (stripped.equals(ChatColor.stripColor(TITLE_MAIN))
                || stripped.equals(ChatColor.stripColor(TITLE_COMMANDS))
                || stripped.equals(ChatColor.stripColor(TITLE_CREDITS))) {
            e.setCancelled(true);
        }
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }

        // top + bottom
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);
        // left + right
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, pane);
            inv.setItem(row * 9 + 8, pane);
        }
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
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

    private ItemStack head(String username, String name, List<String> lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta0 = skull.getItemMeta();
        if (!(meta0 instanceof SkullMeta meta)) return item(Material.PLAYER_HEAD, name, lore);

        OfflinePlayer off = Bukkit.getOfflinePlayer(username);
        meta.setOwningPlayer(off);
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        skull.setItemMeta(meta);
        return skull;
    }
}

