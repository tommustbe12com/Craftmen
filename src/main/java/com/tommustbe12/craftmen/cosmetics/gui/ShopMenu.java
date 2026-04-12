package com.tommustbe12.craftmen.cosmetics.gui;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.cosmetics.CosmeticsShop;
import com.tommustbe12.craftmen.profile.Profile;
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

import java.util.List;

public final class ShopMenu implements Listener {

    private static final String TITLE = "§8Cosmetics Shop";
    private static final String TITLE_CHAT = "§8Chat Colors";
    private static final String TITLE_GADGETS = "§8Spawn Gadgets";
    private static final String TITLE_KD = "§8Kill/Death Cosmetics";

    public void open(Player player) {
        if (player == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        fill(inv);
        inv.setItem(11, item(Material.NAME_TAG, "§b§lChat Color", List.of("§7Purchase and select a chat color.", "§8Click to view")));
        inv.setItem(13, item(Material.FIREWORK_ROCKET, "§d§lKill/Death Effects", List.of("§7Kill effects + sounds.", "§8Click to view")));
        inv.setItem(15, item(Material.ELYTRA, "§a§lSpawn Gadgets", List.of("§7Permanent hub gadgets.", "§8Click to view")));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    private void openKillDeath(Player player) {
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_KD);
        fill(inv);
        inv.setItem(49, item(Material.BARRIER, "§cBack", List.of("§7Return to shop")));

        inv.setItem(10, cosmeticSelectItem("kill.lightning", Material.TRIDENT, "§eKill Effect: Lightning", 125, profile,
                profile != null && "kill.lightning".equals(profile.getSelectedKillEffect())));
        inv.setItem(11, cosmeticSelectItem("kill.firework", Material.FIREWORK_ROCKET, "§eKill Effect: Firework", 100, profile,
                profile != null && "kill.firework".equals(profile.getSelectedKillEffect())));

        inv.setItem(13, cosmeticSelectItem("sound.kill.levelup", Material.NOTE_BLOCK, "§bKill Sound: Level Up", 75, profile,
                profile != null && "ENTITY_PLAYER_LEVELUP".equals(profile.getSelectedKillSound())));

        inv.setItem(15, cosmeticSelectItem("death.lightning", Material.LIGHTNING_ROD, "§cDeath Effect: Lightning", 125, profile,
                profile != null && "death.lightning".equals(profile.getSelectedDeathEffect())));
        inv.setItem(16, cosmeticSelectItem("death.firework", Material.FIREWORK_STAR, "§cDeath Effect: Firework", 100, profile,
                profile != null && "death.firework".equals(profile.getSelectedDeathEffect())));

        inv.setItem(22, cosmeticSelectItem("sound.death.wither", Material.WITHER_SKELETON_SKULL, "§dDeath Sound: Wither", 75, profile,
                profile != null && "ENTITY_WITHER_DEATH".equals(profile.getSelectedDeathSound())));

        player.openInventory(inv);
    }

    private void openChat(Player player) {
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_CHAT);
        fill(inv);
        inv.setItem(49, item(Material.BARRIER, "§cBack", List.of("§7Return to shop")));

        inv.setItem(10, chatColorItem("&b", "§bAqua", 75, profile));
        inv.setItem(11, chatColorItem("&d", "§dPink", 75, profile));
        inv.setItem(12, chatColorItem("&e", "§eYellow", 75, profile));
        inv.setItem(13, chatColorItem("&a", "§aGreen", 75, profile));
        inv.setItem(14, chatColorItem("&c", "§cRed", 75, profile));
        inv.setItem(15, chatColorItem("&6", "§6Gold", 75, profile));
        inv.setItem(16, chatColorItem("&f", "§fWhite", 50, profile));

        player.openInventory(inv);
    }

    private void openGadgets(Player player) {
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_GADGETS);
        fill(inv);
        inv.setItem(49, item(Material.BARRIER, "§cBack", List.of("§7Return to shop")));

        inv.setItem(20, gadgetItem("gadget.elytra", Material.ELYTRA, "§aPermanent Elytra", 150, profile,
                List.of("§7Get an Elytra gadget in spawn.")));
        inv.setItem(24, gadgetItem("gadget.windcharge", Material.WIND_CHARGE, "§aPermanent Wind Charges", 150, profile,
                List.of("§7Get Wind Charges gadget in spawn.")));

        inv.setItem(22, gadgetItem("gadget.launchfeather", Material.FEATHER, "§aLaunch Feather", 25, profile,
                List.of("§7Harmless hub launch gadget.", "§8Cooldown based.")));
        inv.setItem(23, gadgetItem("gadget.funfirework", Material.FIREWORK_ROCKET, "§aFun Firework", 25, profile,
                List.of("§7Harmless hub fireworks.", "§8Cooldown based.")));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getView() == null || e.getView().getTitle() == null) return;

        String title = e.getView().getTitle();
        String stripped = ChatColor.stripColor(title);
        if (stripped == null) return;
        boolean isShop = stripped.equals(ChatColor.stripColor(TITLE));
        boolean isChat = stripped.equals(ChatColor.stripColor(TITLE_CHAT));
        boolean isGadgets = stripped.equals(ChatColor.stripColor(TITLE_GADGETS));
        boolean isKD = stripped.equals(ChatColor.stripColor(TITLE_KD));
        if (!isShop && !isChat && !isGadgets && !isKD) return;

        // Always cancel interactions in our menus (including shift-clicking from bottom inventory).
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = e.getRawSlot();
        if (isShop) {
            if (slot == 11) openChat(player);
            else if (slot == 13) openKillDeath(player);
            else if (slot == 15) openGadgets(player);
            else player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            return;
        }

        if (slot == 49) {
            open(player);
            return;
        }

        if (isChat) {
            handleChatColorClick(player, clicked);
            return;
        }

        if (isGadgets) {
            handleGadgetClick(player, clicked);
            return;
        }

        if (isKD) {
            handleKillDeathClick(player, clicked);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        String stripped = ChatColor.stripColor(e.getView().getTitle());
        if (stripped == null) return;
        if (stripped.equals(ChatColor.stripColor(TITLE))
                || stripped.equals(ChatColor.stripColor(TITLE_CHAT))
                || stripped.equals(ChatColor.stripColor(TITLE_GADGETS))
                || stripped.equals(ChatColor.stripColor(TITLE_KD))) {
            e.setCancelled(true);
        }
    }

    private void handleChatColorClick(Player player, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getLore() == null) return;
        String lore0 = ChatColor.stripColor(meta.getLore().get(0));
        if (lore0 == null) return;

        // Lore format: "Color: &b"
        String code = lore0.replace("Color: ", "").trim();
        int cost = parseCost(meta.getLore());
        String cosmeticId = "chatcolor." + code;

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (!profile.hasCosmetic(cosmeticId)) {
            if (!CosmeticsShop.purchase(player, cosmeticId, cost)) return;
        }

        profile.setSelectedChatColor(code);
        Craftmen.get().saveProfile(profile);
        player.sendMessage(ChatColor.AQUA + "Selected chat color: " + ChatColor.translateAlternateColorCodes('&', code) + "Preview");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.3f);
        openChat(player);
    }

    private void handleGadgetClick(Player player, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getLore() == null) return;
        String idLine = ChatColor.stripColor(meta.getLore().get(0));
        if (idLine == null) return;

        // Lore format: "ID: gadget.elytra"
        String cosmeticId = idLine.replace("ID: ", "").trim();
        int cost = parseCost(meta.getLore());
        CosmeticsShop.purchase(player, cosmeticId, cost);
        openGadgets(player);
    }

    private void handleKillDeathClick(Player player, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getLore() == null) return;
        String idLine = ChatColor.stripColor(meta.getLore().get(0));
        if (idLine == null) return;
        String cosmeticId = idLine.replace("ID: ", "").trim();
        int cost = parseCost(meta.getLore());

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (!profile.hasCosmetic(cosmeticId)) {
            if (!CosmeticsShop.purchase(player, cosmeticId, cost)) return;
        }

        // Apply selection
        switch (cosmeticId) {
            case "kill.lightning", "kill.firework" -> profile.setSelectedKillEffect(cosmeticId);
            case "death.lightning", "death.firework" -> profile.setSelectedDeathEffect(cosmeticId);
            case "sound.kill.levelup" -> profile.setSelectedKillSound("ENTITY_PLAYER_LEVELUP");
            case "sound.death.wither" -> profile.setSelectedDeathSound("ENTITY_WITHER_DEATH");
        }

        Craftmen.get().saveProfile(profile);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        openKillDeath(player);
    }

    private ItemStack cosmeticSelectItem(String id, Material mat, String name, int cost, Profile profile, boolean selected) {
        boolean owned = profile != null && profile.hasCosmetic(id);
        List<String> lore = new java.util.ArrayList<>();
        lore.add("§7ID: §f" + id);
        if (owned) {
            lore.add("§aOwned" + (selected ? " §6(Selected)" : ""));
            lore.add("§eClick to select");
        } else {
            lore.add("§7Cost: §b" + cost + " gems");
            lore.add("§eClick to purchase");
        }
        return item(owned ? Material.LIME_DYE : mat, name + (selected ? " §6(Selected)" : ""), lore);
    }

    private int parseCost(List<String> lore) {
        for (String l : lore) {
            String s = ChatColor.stripColor(l);
            if (s == null) continue;
            if (s.startsWith("Cost: ")) {
                try {
                    return Integer.parseInt(s.replace("Cost: ", "").replace(" gems", "").trim());
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private ItemStack chatColorItem(String code, String display, int cost, Profile profile) {
        String id = "chatcolor." + code;
        boolean owned = profile != null && profile.hasCosmetic(id);
        boolean selected = profile != null && code.equalsIgnoreCase(profile.getSelectedChatColor());

        Material mat = owned ? Material.LIME_DYE : Material.GRAY_DYE;
        String name = display + (selected ? " §6(Selected)" : "");
        List<String> lore = owned
                ? List.of("§7Color: §f" + code, "§aOwned", "§eClick to select")
                : List.of("§7Color: §f" + code, "§7Cost: §b" + cost + " gems", "§eClick to purchase");
        return item(mat, name, lore);
    }

    private ItemStack gadgetItem(String id, Material mat, String name, int cost, Profile profile, List<String> extra) {
        boolean owned = profile != null && profile.hasCosmetic(id);
        List<String> lore = new java.util.ArrayList<>();
        lore.add("§7ID: §f" + id);
        lore.addAll(extra);
        if (owned) {
            lore.add("§aOwned");
        } else {
            lore.add("§7Cost: §b" + cost + " gems");
            lore.add("§eClick to purchase");
        }
        return item(owned ? Material.LIME_DYE : mat, name, lore);
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
