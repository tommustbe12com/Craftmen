package com.tommustbe12.craftmen.souls;

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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class SoulsCharacterMenu implements Listener {

    public static final String TITLE = "§8Select a Soul";

    private final Map<UUID, Consumer<SoulCharacter>> callbacks = new java.util.HashMap<>();
    private final Map<SoulCharacter, ItemStack> buttonByCharacter = new EnumMap<>(SoulCharacter.class);

    public SoulsCharacterMenu() {
        buttonByCharacter.put(SoulCharacter.GOOP,
                button(Material.SLIME_BALL, "§aGoop", List.of(
                        "§7Base: Bounce a player back",
                        "§7Passive: No fall damage",
                        "§7Base 2: Freeze for 7s"
                )));
        buttonByCharacter.put(SoulCharacter.DEVILS_FROST,
                button(Material.PACKED_ICE, "§bThe Devil's Frost", List.of(
                        "§7Base: Stun for 3s",
                        "§7Passive: Frost Walker",
                        "§7Special: +5 hearts (30s)"
                )));
        buttonByCharacter.put(SoulCharacter.VOICE_OF_THE_SEA,
                button(Material.TRIDENT, "§9Voice of the Sea", List.of(
                        "§7Base: Riptide III trident",
                        "§7Passive: Speed II in rain",
                        "§7Special: Thunderstorm"
                )));
    }

    public void open(Player player, Consumer<SoulCharacter> onPick) {
        if (player == null) return;
        callbacks.put(player.getUniqueId(), onPick);

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        fill(inv);

        inv.setItem(11, buttonByCharacter.get(SoulCharacter.GOOP));
        inv.setItem(13, buttonByCharacter.get(SoulCharacter.DEVILS_FROST));
        inv.setItem(15, buttonByCharacter.get(SoulCharacter.VOICE_OF_THE_SEA));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (name == null) return;

        SoulCharacter picked = null;
        if (name.equalsIgnoreCase("Goop")) picked = SoulCharacter.GOOP;
        else if (name.equalsIgnoreCase("The Devil's Frost")) picked = SoulCharacter.DEVILS_FROST;
        else if (name.equalsIgnoreCase("Voice of the Sea")) picked = SoulCharacter.VOICE_OF_THE_SEA;

        if (picked == null) return;

        Consumer<SoulCharacter> cb = callbacks.remove(player.getUniqueId());
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.4f);
        if (cb != null) cb.accept(picked);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
    }

    private ItemStack button(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
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
}

