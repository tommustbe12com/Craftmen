package com.tommustbe12.craftmen.hub;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.queue.QueueManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HubManager implements Listener {

    private final QueueManager queueManager = Craftmen.get().getQueueManager();

    // Give the player the iron sword that opens the GUI
    public void giveHubItems(Player player) {
        player.getInventory().clear();
        ItemStack hubItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = hubItem.getItemMeta();
        meta.setDisplayName("§6Game Selector");
        hubItem.setItemMeta(meta);
        player.getInventory().setItem(0, hubItem);
    }

    // Give hub items on join
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        giveHubItems(e.getPlayer());
    }

    // Give hub items on respawn
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> giveHubItems(e.getPlayer()), 1L);
    }

    // Right click iron sword to open GUI
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().equals("§6Game Selector")) return;

        e.setCancelled(true);
        openGameSelector(e.getPlayer());
    }

    // Opens the GUI listing all registered games
    private void openGameSelector(Player player) {
        int size = 9;
        int gameCount = Craftmen.get().getGameManager().getGames().size();
        while (size < gameCount) size += 9; // round up to nearest row

        Inventory gui = Bukkit.createInventory(null, size, "Select a Game");

        for (Game game : Craftmen.get().getGameManager().getGames()) {
            ItemStack item = game.getIcon();
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + game.getName());
            item.setItemMeta(meta);
            gui.addItem(item);
        }

        player.openInventory(gui);
    }

    // Handle clicks in the GUI
    @EventHandler
    public void onGUIClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (e.getView().getTitle().equals("Select a Game")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

            String gameName = e.getCurrentItem().getItemMeta().getDisplayName().replace("§a", "");
            Game game = Craftmen.get().getGameManager().getGame(gameName);
            if (game != null) {
                queueManager.addPlayer((Player) e.getWhoClicked(), game);
                e.getWhoClicked().closeInventory();
            }
        }
    }
}