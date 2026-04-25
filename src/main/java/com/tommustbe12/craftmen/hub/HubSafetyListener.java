package com.tommustbe12.craftmen.hub;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.PlayerState;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.block.SignChangeEvent;
import io.papermc.paper.event.player.PlayerOpenSignEvent;

public final class HubSafetyListener implements Listener {

    private boolean shouldProtect(Player player) {
        if (player == null) return false;
        if (Craftmen.get().getEndFightManager().isInGame(player)) return false;
        if (Craftmen.get().getFfaManager().isInFfa(player)) return false;
        Match match = Craftmen.get().getMatchManager().getMatch(player);
        if (match != null) return false;

        // Allow explicitly allowed builders to edit/build in spawn.
        if (Craftmen.get().getBuildAllowManager() != null && Craftmen.get().getBuildAllowManager().canBuild(player)) return false;
        // Allow OPs in creative to edit/build.
        if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) return false;

        return Craftmen.get().getProfileManager().getProfile(player).getState() == PlayerState.LOBBY;
    }

    private static boolean isDoorOrTrapdoor(Material type) {
        if (type == null) return false;
        String n = type.name();
        return n.endsWith("_DOOR") || n.endsWith("_TRAPDOOR");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractBlock(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!shouldProtect(player)) return;

        Block clicked = e.getClickedBlock();
        if (clicked == null) return;

        Material type = clicked.getType();

        // Don't allow wind charges to toggle doors/trapdoors (but allow wind charge propulsion elsewhere).
        if (isDoorOrTrapdoor(type) && e.getItem() != null && e.getItem().getType() == Material.WIND_CHARGE) {
            e.setCancelled(true);
            return;
        }

        // Allow doors/trapdoors to be opened/closed normally...
        if (isDoorOrTrapdoor(type)) {
            return;
        }

        // Block taking items out of decorated pots.
        if (type == Material.DECORATED_POT) {
            e.setCancelled(true);
            return;
        }

        // Block opening containers (chests, barrels, shulkers, ender chests, etc).
        BlockState state = clicked.getState();
        if (state instanceof Container) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpenInventory(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!shouldProtect(player)) return;

        // Only block real block containers. Allow plugin GUIs (custom holders) to work.
        if (e.getInventory().getHolder() instanceof BlockState bs) {
            if (bs instanceof Container) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        if (!shouldProtect(player)) return;

        // Block item frame rotation / painting/map-art interactions / etc.
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (!shouldProtect(e.getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageEntity(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;
        if (!shouldProtect(player)) return;

        Entity victim = e.getEntity();
        if (victim instanceof Player) return; // spawn protection handles PvP/damage in protected area

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        Projectile proj = e.getEntity();
        if (!(proj.getShooter() instanceof Player player)) return;
        if (!shouldProtect(player)) return;

        // Allow wind charges to launch (propel players). Protections are handled by explode/hanging hooks.
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (e.getEntity() == null) return;
        if (e.getEntityType() != null && e.getEntityType().name().equalsIgnoreCase("WIND_CHARGE")) {
            // Wind charge explosions can break hanging entities / mess with builds.
            e.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignOpen(PlayerOpenSignEvent e) {
        if (!shouldProtect(e.getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent e) {
        if (!shouldProtect(e.getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        if (!(e.getRemover() instanceof Player player)) return;
        if (!shouldProtect(player)) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreakAny(HangingBreakEvent e) {
        // Wind charges and other physics/explosions can break item frames/paintings without a player remover.
        if (e.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION
                || e.getCause() == HangingBreakEvent.RemoveCause.PHYSICS
                || e.getCause() == HangingBreakEvent.RemoveCause.OBSTRUCTION) {
            // Only protect if the entity is in our protected spawn region-ish by checking nearby players:
            // If any online player in LOBBY is within range and shouldProtect would apply, cancel.
            // (Cheap heuristic without needing exact region bounds here.)
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!shouldProtect(p)) continue;
                if (p.getWorld() != null && p.getWorld().equals(e.getEntity().getWorld())
                        && p.getLocation().distanceSquared(e.getEntity().getLocation()) < (120.0 * 120.0)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent e) {
        if (!(e.getAttacker() instanceof Player player)) return;
        if (!shouldProtect(player)) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent e) {
        if (!(e.getAttacker() instanceof Player player)) return;
        if (!shouldProtect(player)) return;
        e.setCancelled(true);
    }
}
