package com.tommustbe12.craftmen.command;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StatsCommand implements CommandExecutor, Listener, TabCompleter {

    private static final String MAIN_TITLE = "§8Stats";
    private static final String GAME_TITLE = "§8Per-Game Stats";

    private final Map<UUID, UUID> viewing = new HashMap<>();

    // =========================
    // COMMAND
    // =========================
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        Player target = player;

        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found or not online.");
                return true;
            }
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(target);

        if (profile == null) {
            player.sendMessage(ChatColor.RED + "That player has no stats.");
            return true;
        }

        viewing.put(player.getUniqueId(), target.getUniqueId());
        openMain(player, target, profile);

        return true;
    }

    // =========================
    // MAIN MENU
    // =========================
    private void openMain(Player viewer, Player target, Profile profile) {
        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE);

        fill(inv);

        inv.setItem(20, item(Material.DIAMOND_SWORD, "§bCombat Stats",
                "§7Wins: §f" + profile.getWins(),
                "§7Losses: §f" + profile.getLosses(),
                "§7Win Streak: §f" + profile.getKillsInARow(),
                "§7Loss Streak: §f" + profile.getLossesInARow()
        ));

        inv.setItem(22, item(Material.EMERALD, "§aGems",
                "§7Amount: §f" + profile.getGems()
        ));

        inv.setItem(24, item(Material.BOW, "§dFFA Stats",
                "§7Kills: §f" + profile.getFfaKills(),
                "§7Deaths: §f" + profile.getFfaDeaths()
        ));

        inv.setItem(31, item(Material.NETHER_STAR, "§eOther Stats",
                "§7End Wins: §f" + profile.getEndWins(),
                "§7Last Game: §f" + (profile.getLastPlayedGame() == null ? "None" : profile.getLastPlayedGame())
        ));

        inv.setItem(49, item(Material.CHEST, "§6Per-Game Stats", "§7Click to view"));

        viewer.openInventory(inv);
    }

    // =========================
    // PER GAME MENU
    // =========================
    private void openGames(Player viewer, Player target, Profile profile) {
        Inventory inv = Bukkit.createInventory(null, 54, GAME_TITLE);

        fill(inv);

        int slot = 10;

        for (Map.Entry<String, Integer> entry : profile.getGameWins().entrySet()) {

            String game = entry.getKey();
            int wins = entry.getValue();
            int losses = profile.getGameLosses().getOrDefault(game, 0);

            inv.setItem(slot, item(Material.PAPER, "§b" + game,
                    "§7Wins: §f" + wins,
                    "§7Losses: §f" + losses
            ));

            slot++;

            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
        }

        inv.setItem(49, item(Material.ARROW, "§cBack", "§7Return"));

        viewer.openInventory(inv);
    }

    // =========================
    // CLICK HANDLER (SHOP STYLE FIX)
    // =========================
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getView() == null || e.getView().getTitle() == null) return;

        String title = ChatColor.stripColor(e.getView().getTitle());
        if (title == null) return;

        boolean isMain = title.equalsIgnoreCase("Stats");
        boolean isGame = title.equalsIgnoreCase("Per-Game Stats");

        if (!isMain && !isGame) return;

        // 🔥 HARD CANCEL (like your working GUI)
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = e.getRawSlot();

        UUID targetUUID = viewing.get(player.getUniqueId());
        if (targetUUID == null) return;

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Player no longer online.");
            return;
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(target);

        if (isMain) {
            if (slot == 49) {
                openGames(player, target, profile);
            }
        }

        if (isGame) {
            if (slot == 49) {
                openMain(player, target, profile);
            }
        }
    }

    // =========================
    // DRAG BLOCK
    // =========================
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;

        String title = ChatColor.stripColor(e.getView().getTitle());
        if (title == null) return;

        if (title.equalsIgnoreCase("Stats") || title.equalsIgnoreCase("Per-Game Stats")) {
            e.setCancelled(true);
        }
    }

    // =========================
    // ITEM UTIL
    // =========================
    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) list.add(p.getName());
            return list;
        }
        return Collections.emptyList();
    }
}