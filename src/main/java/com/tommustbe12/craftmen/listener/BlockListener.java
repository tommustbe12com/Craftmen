package com.tommustbe12.craftmen.listener;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;


public class BlockListener implements Listener {

    private static boolean isSpleef(Player player) {
        if (player == null) return false;

        Match match = Craftmen.get().getMatchManager().getMatch(player);
        if (match != null && match.getGame() != null && "Spleef".equalsIgnoreCase(match.getGame().getName())) return true;

        var ffaGame = Craftmen.get().getFfaManager().getGame(player);
        return ffaGame != null && "Spleef".equalsIgnoreCase(ffaGame.getName());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();

        // OP bypass
        if (player.isOp()) return;
        // Spawn builder bypass
        if (Craftmen.get().getBuildAllowManager() != null && Craftmen.get().getBuildAllowManager().canBuild(player)) return;

        if (Craftmen.get().getEndFightManager().isInGame(player)) return;

        PlayerState state = Craftmen.get().getProfileManager().getProfile(player).getState();

        if (state != PlayerState.IN_MATCH && state != PlayerState.FFA_FIGHTING) {
            e.setCancelled(true);
            player.sendMessage("§cYou can't break this here!");
            return;
        }

        // Only spleef
        if (!isSpleef(player)) return;

        Material type = e.getBlock().getType();
        if (type != Material.SNOW_BLOCK && type != Material.SNOW && type != Material.POWDER_SNOW) return; // idk if well use powdered snow but still

        // Never drop items/exp in Spleef from snow blocks.
        e.setDropItems(false);
        e.setExpToDrop(0);

        // 20% chance (1 in 5) to refund 4 snowballs when breaking a snow block in Spleef.
        if (type == Material.SNOW_BLOCK) {
            int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 6); // 1-5
            if (roll == 1) {
                var leftovers = player.getInventory().addItem(new ItemStack(Material.SNOWBALL, 4));
                for (ItemStack left : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            }
        }
    }

    @EventHandler
    public void onSpleefBlockDrop(BlockDropItemEvent e) {
        Player player = e.getPlayer();
        if (!isSpleef(player)) return;

        Material type = e.getBlockState().getType();
        if (type != Material.SNOW_BLOCK && type != Material.SNOW && type != Material.POWDER_SNOW) return;

        // Safety net: remove any drops that still got generated.
        e.getItems().clear();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();

        // op bypass
        if (player.isOp()) return;
        // Spawn builder bypass
        if (Craftmen.get().getBuildAllowManager() != null && Craftmen.get().getBuildAllowManager().canBuild(player)) return;

        if(Craftmen.get().getEndFightManager().isInGame(e.getPlayer())) return;

        PlayerState state = Craftmen.get()
                .getProfileManager()
                .getProfile(player)
                .getState();

        if (state != PlayerState.IN_MATCH && state != PlayerState.FFA_FIGHTING) {
            e.setCancelled(true);
            player.sendMessage("§cYou can't place this here!");
        }
    }
}
