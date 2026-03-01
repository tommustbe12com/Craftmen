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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class HubManager implements Listener {

    private final QueueManager queueManager = Craftmen.get().getQueueManager();

    private final Map<UUID, Consumer<Game>> selectedGameCallbacks = new HashMap<>();

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

    // right click item detector interact
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!(e.getAction() == Action.RIGHT_CLICK_AIR ||
                e.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) return;

        String name = item.getItemMeta().getDisplayName();
        Player player = e.getPlayer();

        // game select
        if (name.equals("§6Game Selector")) {
            e.setCancelled(true);

            openGameSelector(player, game -> {
                giveLeaveQueueItem(player);
                Craftmen.get().getQueueManager().addPlayer(player, game);
            });
            return;
        }

        // left queue
        if (name.equals("§cLeave Queue")) {
            e.setCancelled(true);

            Craftmen.get().getQueueManager().removePlayer(player);
            giveHubItems(player);
        }
    }

    // Opens the GUI listing all registered games
    public void openGameSelector(Player player, java.util.function.Consumer<Game> onSelect) {
        int size = 9;
        int gameCount = Craftmen.get().getGameManager().getGames().size();
        while (size < gameCount) size += 9;

        Inventory gui = Bukkit.createInventory(null, size, "Select a Game");

        for (Game game : Craftmen.get().getGameManager().getGames()) {
            ItemStack item = game.getIcon();
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + game.getName());
            item.setItemMeta(meta);
            gui.addItem(item);
        }

        player.openInventory(gui);

        // Store the callback somewhere temporarily so onGUIClick can use it
        selectedGameCallbacks.put(player.getUniqueId(), onSelect);
    }

    // Handle clicks in the GUI
    @EventHandler
    public void onGUIClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (e.getView().getTitle().equals("Select a Game")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

            Player clicker = (Player) e.getWhoClicked();
            java.util.function.Consumer<Game> callback = selectedGameCallbacks.remove(clicker.getUniqueId());
            if (callback == null) return; // not a duel selection

            String gameName = e.getCurrentItem().getItemMeta().getDisplayName().replace("§a", "");
            Game game = Craftmen.get().getGameManager().getGame(gameName);
            if (game != null) callback.accept(game);

            clicker.closeInventory();
        }
    }

    private void giveLeaveQueueItem(Player player) {
        player.getInventory().clear();

        ItemStack leave = new ItemStack(Material.RED_DYE);
        ItemMeta meta = leave.getItemMeta();
        meta.setDisplayName("§cLeave Queue");
        leave.setItemMeta(meta);

        // Slot 4 = middle of hotbar (0-8)
        player.getInventory().setItem(4, leave);

        player.updateInventory();
    }
}