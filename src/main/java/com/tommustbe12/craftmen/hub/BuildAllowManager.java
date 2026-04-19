package com.tommustbe12.craftmen.hub;

import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Spawn build bypass:
 * - Temporary: /buildallow (cleared on join)
 * - Permanent: /builder (persisted in config)
 */
public final class BuildAllowManager implements Listener {

    private static final String CONFIG_PATH = "builders";

    private final Set<UUID> tempAllowed = new HashSet<>();
    private final Set<UUID> permanentAllowed = new HashSet<>();

    public BuildAllowManager() {
        loadPermanent();
    }

    public boolean canBuild(Player player) {
        if (player == null) return false;
        UUID id = player.getUniqueId();
        return tempAllowed.contains(id) || permanentAllowed.contains(id);
    }

    public boolean isPermanent(OfflinePlayer player) {
        if (player == null) return false;
        return permanentAllowed.contains(player.getUniqueId());
    }

    public void setTempAllowed(OfflinePlayer player, boolean allowed) {
        if (player == null) return;
        if (allowed) tempAllowed.add(player.getUniqueId());
        else tempAllowed.remove(player.getUniqueId());
    }

    public void setPermanentAllowed(OfflinePlayer player, boolean allowed) {
        if (player == null) return;
        if (allowed) permanentAllowed.add(player.getUniqueId());
        else permanentAllowed.remove(player.getUniqueId());
        savePermanent();
    }

    private void loadPermanent() {
        permanentAllowed.clear();
        List<String> list = Craftmen.get().getConfig().getStringList(CONFIG_PATH);
        for (String raw : list) {
            try {
                permanentAllowed.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void savePermanent() {
        Craftmen.get().getConfig().set(CONFIG_PATH, permanentAllowed.stream().map(UUID::toString).toList());
        Craftmen.get().saveConfig();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Temp access must be re-granted each session.
        tempAllowed.remove(e.getPlayer().getUniqueId());
    }

    public void notifyStatus(Player sender, OfflinePlayer target) {
        if (sender == null || target == null) return;
        boolean temp = tempAllowed.contains(target.getUniqueId());
        boolean perm = permanentAllowed.contains(target.getUniqueId());
        sender.sendMessage(ChatColor.GRAY + "Build access for " + ChatColor.YELLOW + target.getName()
                + ChatColor.GRAY + ": "
                + (temp ? (ChatColor.GREEN + "TEMP ") : "")
                + (perm ? (ChatColor.AQUA + "PERM") : (temp ? "" : (ChatColor.RED + "NONE"))));
    }
}
