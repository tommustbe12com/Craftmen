package com.tommustbe12.craftmen.souls;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class SoulsCharacterMenu implements Listener {

    public static final String TITLE_PREFIX = "§8Select a Soul";

    private static final int INV_SIZE = 27;
    private static final int[] CHARACTER_SLOTS = {11, 13, 15};
    private static final int SLOT_PREV = 18;
    private static final int SLOT_CLOSE = 22;
    private static final int SLOT_NEXT = 26;
    private static final int SLOT_PAGE = 4;

    private static final List<SoulCharacter> ORDER = List.of(
            SoulCharacter.GOOP,
            SoulCharacter.DEVILS_FROST,
            SoulCharacter.VOICE_OF_THE_SEA,
            SoulCharacter.MAGNET,
            SoulCharacter.ARTIFICIAL_GENOCIDE,
            SoulCharacter.COSMIC_DESTROYER,
            SoulCharacter.SORCERER,
            SoulCharacter.KING_OF_HEAT,
            SoulCharacter.ARCHANGEL
    );

    private final Map<UUID, Consumer<SoulCharacter>> callbacks = new HashMap<>();
    private final Map<UUID, Integer> pageByPlayer = new HashMap<>();

    public void open(Player player, Consumer<SoulCharacter> onPick) {
        open(player, 0, onPick);
    }

    private void open(Player player, int page, Consumer<SoulCharacter> onPick) {
        if (player == null) return;
        callbacks.put(player.getUniqueId(), onPick);

        int totalPages = (int) Math.ceil(ORDER.size() / 3.0);
        page = Math.max(0, Math.min(page, totalPages - 1));
        pageByPlayer.put(player.getUniqueId(), page);

        String title = TITLE_PREFIX + " §7(" + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, title);
        fill(inv);

        // Nav + close
        if (page > 0) inv.setItem(SLOT_PREV, arrow(true));
        if (page < totalPages - 1) inv.setItem(SLOT_NEXT, arrow(false));
        inv.setItem(SLOT_CLOSE, button(Material.BARRIER, "§cClose", List.of("§7Close")));
        inv.setItem(SLOT_PAGE, button(Material.PAPER, "§ePage " + (page + 1), List.of("§7Pick a soul character")));

        int start = page * 3;
        for (int i = 0; i < 3; i++) {
            int idx = start + i;
            if (idx >= ORDER.size()) break;
            SoulCharacter c = ORDER.get(idx);
            inv.setItem(CHARACTER_SLOTS[i], characterButton(c));
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().startsWith(TITLE_PREFIX)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;
        int slot = e.getRawSlot();

        if (slot == SLOT_CLOSE) {
            callbacks.remove(player.getUniqueId());
            pageByPlayer.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }

        if (slot == SLOT_PREV || slot == SLOT_NEXT) {
            int cur = pageByPlayer.getOrDefault(player.getUniqueId(), 0);
            int next = (slot == SLOT_PREV) ? (cur - 1) : (cur + 1);
            open(player, next, callbacks.get(player.getUniqueId()));
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String id = meta.getPersistentDataContainer().get(SoulsItems.SOUL_CHARACTER_KEY, PersistentDataType.STRING);
        if (id == null) return;

        SoulCharacter picked;
        try {
            picked = SoulCharacter.valueOf(id);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        Consumer<SoulCharacter> cb = callbacks.remove(player.getUniqueId());
        pageByPlayer.remove(player.getUniqueId());
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.4f);
        if (cb != null) cb.accept(picked);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().startsWith(TITLE_PREFIX)) return;
        e.setCancelled(true);
    }

    private ItemStack characterButton(SoulCharacter c) {
        // Menu icons match the in-game soul item icons (glint-only; enchants hidden).
        ItemStack item = SoulsItems.soulItem(c);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(SoulsItems.SOUL_CHARACTER_KEY, PersistentDataType.STRING, c.name());

            List<String> lore = switch (c) {
                case GOOP -> List.of(
                        "§7Base: Bounce a player back",
                        "§7Passive: Speed I",
                        "§7Base 2: Freeze for 2s"
                );
                case DEVILS_FROST -> List.of(
                        "§7Base: Stun for 3s",
                        "§7Passive: Frost Walker",
                        "§7Special: +5 hearts (30s)"
                );
                case VOICE_OF_THE_SEA -> List.of(
                        "§7Base: Dash forward",
                        "§7Passive: Speed II in rain",
                        "§7Special: Thunderstorm"
                );
                case MAGNET -> List.of(
                        "§7Base: Pull closest enemy",
                        "§7Passive: Speed on hit",
                        "§7Base 2: Push closest enemy"
                );
                case ARTIFICIAL_GENOCIDE -> List.of(
                        "§7Base: TP 10 blocks forward",
                        "§7Passive: Random effect (1m)",
                        "§7Special: Shuffle enemy inventory"
                );
                case COSMIC_DESTROYER -> List.of(
                        "§7Base: Break blocks + 2❤ AOE",
                        "§7Passive: -10% armor damage",
                        "§7Special: Blackhole (pull + armor dmg)"
                );
                case SORCERER -> List.of(
                        "§7Base: Push enemy (4s)",
                        "§7Passive: +0.5 reach",
                        "§7Base 2: Random TP"
                );
                case KING_OF_HEAT -> List.of(
                        "§7Base: Flame Jump",
                        "§7Passive: Fire Res",
                        "§7Special: Flamethrower"
                );
                case ARCHANGEL -> List.of(
                        "§7Base: Levitation III (3s)",
                        "§7Passive: Double Jump",
                        "§7Special: Invulnerable (10s)"
                );
            };

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
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

    private ItemStack arrow(boolean left) {
        return button(Material.ARROW, left ? "§ePrevious" : "§eNext", List.of("§7Turn the page"));
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
