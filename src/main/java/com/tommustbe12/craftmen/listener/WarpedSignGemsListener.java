package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class WarpedSignGemsListener implements Listener {

    private static final String TEAM_LINE = "- Craftmen Team";
    private static final int GEMS = 5;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRightClickSign(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block b = e.getClickedBlock();
        if (b == null) return;

        Material type = b.getType();
        if (type != Material.WARPED_SIGN && type != Material.WARPED_WALL_SIGN) return;
        if (!(b.getState() instanceof Sign sign)) return;

        // Check front side text.
        org.bukkit.block.sign.SignSide front = sign.getSide(Side.FRONT);
        String line3 = front.getLine(2); // 3rd line (0-index)
        if (line3 == null) return;
        if (!ChatColor.stripColor(line3).equals(TEAM_LINE)) return;

        String msg = front.getLine(1); // 2nd line
        if (msg == null) msg = "";

        Profile profile = Craftmen.get().getProfileManager().getProfile(e.getPlayer());
        if (profile == null) return;

        String key = rewardKey(b, msg);
        if (profile.hasClaimedWarpedSignReward(key)) {
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.2f);
            return;
        }

        profile.addGems(GEMS);
        profile.claimWarpedSignReward(key);
        Craftmen.get().saveProfile(profile);

        e.getPlayer().sendMessage(msg + ChatColor.AQUA + " You received " + GEMS + " gems!");
        e.getPlayer().sendActionBar(ChatColor.AQUA + "+" + GEMS + " gems");
        e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

    private static String rewardKey(Block block, String messageLine2) {
        if (block == null || block.getWorld() == null) return "unknown";
        // Key is stable per sign location (and message, in case signs are moved/replaced).
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ() + ":" + (messageLine2 == null ? "" : messageLine2);
    }
}
