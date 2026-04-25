package com.tommustbe12.craftmen.souls;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.FluidCollisionMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;
import org.bukkit.entity.TNTPrimed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

public final class SoulsManager implements Listener {

    public static final String GAME_NAME = "Souls";

    private static final long BASE_COOLDOWN_MS = 15_000L;
    private static final long GOOP_RIGHT_CLICK_COOLDOWN_MS = 25_000L;
    private static final long SPECIAL_COOLDOWN_MS = 3 * 60_000L;

    private final SoulsCharacterMenu characterMenu = new SoulsCharacterMenu();

    private final Map<UUID, SoulCharacter> selectedCharacter = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    private final Map<UUID, Long> frozenUntil = new HashMap<>();
    private final Map<UUID, Location> frozenFrom = new HashMap<>();

    private final Map<UUID, Double> originalMaxHealth = new HashMap<>();
    private final Map<UUID, Long> specialWeatherUntil = new HashMap<>();
    // (Voice of the Sea no longer swaps in tridents; ability works without item injection.)

    // Magnet passive (speed stacks while attacking).
    private final Map<UUID, Integer> magnetStacks = new HashMap<>();
    private final Map<UUID, Long> magnetLastHitAt = new HashMap<>();
    private static final long MAGNET_STACK_RESET_MS = 2_000L;
    private static final int MAGNET_MAX_SPEED_AMP = 3; // Speed IV max

    // Code Cracker passive (random effect every minute).
    private final Map<UUID, Long> genocideNextEffectAt = new HashMap<>();

    private final Random rng = new Random();

    // Sorcerer passive reach boost.
    private final Map<UUID, Double> originalReach = new HashMap<>();

    // Archangel passive double jump.
    private final Map<UUID, Integer> archangelJumpsUsed = new HashMap<>();

    // Archangel special invulnerability restore.
    private final Map<UUID, Boolean> archangelPrevInvulnerable = new HashMap<>();

    // Bounty Hunter special: hide armor by temporarily removing it.
    private final Map<UUID, ItemStack[]> bountyArmorRestore = new HashMap<>();

    // Copycat passive attack speed.
    private final Map<UUID, Double> originalAttackSpeed = new HashMap<>();

    // Bloody Monarch passive lifesteal.
    private final Map<UUID, Long> monarchLastBeamAt = new HashMap<>();

    // Dark Knight [1]: shadow marker.
    private final Map<UUID, Location> darkKnightShadow = new HashMap<>();
    private final Map<UUID, Long> darkKnightShadowExpiresAt = new HashMap<>();
    private static final long DARK_KNIGHT_SHADOW_TTL_MS = 15_000L;

    // Railgun: tag TNT spawned by ability to reduce damage.
    private final NamespacedKey railgunTntKey = new NamespacedKey(Craftmen.get(), "railgun_tnt");

    // Untamed Beast tracking.
    private final Map<UUID, ItemStack[]> beastDisarmRestore = new HashMap<>();
    private final Map<UUID, UUID> beastDuelTarget = new HashMap<>(); // caster -> target
    private final Map<UUID, Long> railgunDeathBoomAt = new HashMap<>();

    private BukkitTask actionbarTask;
    private BukkitTask passiveTask;

    private static void particle(Player player, Particle type, int count, double ox, double oy, double oz, double extra) {
        if (player == null || !player.isOnline()) return;
        if (player.getWorld() == null) return;
        player.getWorld().spawnParticle(type, player.getLocation().clone().add(0, 1.0, 0), count, ox, oy, oz, extra);
    }

    private static void particleAt(Location loc, Particle type, int count, double ox, double oy, double oz, double extra) {
        if (loc == null || loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(type, loc, count, ox, oy, oz, extra);
    }

    private static void particleItemAt(Location loc, ItemStack item, int count, double ox, double oy, double oz) {
        if (loc == null || loc.getWorld() == null) return;
        if (item == null) return;
        // ITEM particle requires data (an ItemStack); harmless + works across versions.
        loc.getWorld().spawnParticle(Particle.ITEM, loc, count, ox, oy, oz, 0.0, item);
    }

    public SoulsManager() {
        // SoulsCharacterMenu is its own listener; register it so clicks are handled/cancelled.
        Bukkit.getPluginManager().registerEvents(characterMenu, Craftmen.get());
        startTasks();
    }

    public SoulsCharacterMenu getCharacterMenu() {
        return characterMenu;
    }

    public void openCharacterSelect(Player player, Consumer<SoulCharacter> onPick) {
        characterMenu.open(player, picked -> {
            selectedCharacter.put(player.getUniqueId(), picked);
            Profile profile = Craftmen.get().getProfileManager().getProfile(player);
            if (profile != null) profile.setSelectedSoulCharacter(picked);
            if (onPick != null) onPick.accept(picked);
        });
    }

    public SoulCharacter getSelected(Player player) {
        if (player == null) return null;
        SoulCharacter c = selectedCharacter.get(player.getUniqueId());
        if (c != null) return c;
        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        return profile == null ? null : profile.getSelectedSoulCharacter();
    }

    public boolean isSoulsGame(com.tommustbe12.craftmen.game.Game game) {
        return game != null && GAME_NAME.equalsIgnoreCase(game.getName());
    }

    public boolean isInSouls(Player player) {
        if (player == null) return false;
        Match match = Craftmen.get().getMatchManager().getMatch(player);
        if (match != null && isSoulsGame(match.getGame())) return true;
        var ffaGame = Craftmen.get().getFfaManager().getGame(player);
        return ffaGame != null && isSoulsGame(ffaGame);
    }

    public void onRoundStart(Player player) {
        if (player == null) return;
        // Special starts on cooldown when the round starts (only for souls that actually have a special).
        SoulCharacter c = getSelected(player);
        if (c == null) c = SoulCharacter.GOOP;
        if (c == SoulCharacter.DEVILS_FROST
                || c == SoulCharacter.VOICE_OF_THE_SEA
                || c == SoulCharacter.ARTIFICIAL_GENOCIDE
                || c == SoulCharacter.COSMIC_DESTROYER
                || c == SoulCharacter.ARCHANGEL
                || c == SoulCharacter.BOUNTY_HUNTER
                || c == SoulCharacter.BLOODY_MONARCH
                || c == SoulCharacter.RAILGUN) {
            setCooldown(player, "special", System.currentTimeMillis());
        }
    }

    public void applySoulLoadout(Player player) {
        if (player == null) return;

        SoulCharacter c = getSelected(player);
        if (c == null) c = SoulCharacter.GOOP;

        // Replace the placeholder soul item (from kit editor) with the chosen soul icon, preserving its slot.
        int slot = findSoulItemSlot(player);
        if (slot < 0) slot = 8; // default hotbar slot 9
        player.getInventory().setItem(slot, SoulsItems.soulItem(c));

        switch (c) {
            case GOOP -> {
                // passive handled in task
            }
            case DEVILS_FROST -> {
                // frost walker passive: apply effect by enchanting boots if present, else ignore
                ItemStack boots = player.getInventory().getBoots();
                if (boots != null && boots.getType() != org.bukkit.Material.AIR) {
                    ItemMeta meta = boots.getItemMeta();
                    if (meta != null) {
                        meta.addEnchant(org.bukkit.enchantments.Enchantment.FROST_WALKER, 2, true);
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                        boots.setItemMeta(meta);
                    }
                }
            }
            case VOICE_OF_THE_SEA -> {
                // No extra trident given (ability works without it).
            }
        }
    }

    private int findSoulItemSlot(Player player) {
        if (player == null) return -1;
        for (int i = 0; i <= 8; i++) {
            if (SoulsItems.isShardOfSoul(player.getInventory().getItem(i))) return i;
        }
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = 9; i < storage.length; i++) {
            if (SoulsItems.isShardOfSoul(storage[i])) return i;
        }
        return -1;
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();
        if (!isInSouls(player)) return;

        ItemStack main = e.getMainHandItem();
        ItemStack off = e.getOffHandItem();
        if (SoulsItems.isShardOfSoul(main) || SoulsItems.isShardOfSoul(off)) {
            // prevent swapping shard away
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShardClick(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!isInSouls(player)) return;

        ItemStack item = e.getItem();
        if (!SoulsItems.isShardOfSoul(item)) return;

        Action action = e.getAction();
        if (action != Action.LEFT_CLICK_AIR
                && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Clicking the shard triggers abilities: left-click = [1], right-click = [2].
        e.setCancelled(true);
        SoulCharacter c = getSelected(player);
        if (c == null) c = SoulCharacter.GOOP;

        boolean left = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        if (c == SoulCharacter.GOOP) {
            if (left) {
                if (!isCooldownReady(player, "goop1", BASE_COOLDOWN_MS)) return;
                boolean ok = player.isSneaking() ? goopLaunchSelf(player) : goopBounce(player);
                if (!ok) return;
                setCooldown(player, "goop1", System.currentTimeMillis());
                player.sendActionBar("§aUsed [1]");
            } else {
                if (!isCooldownReady(player, "goop2", GOOP_RIGHT_CLICK_COOLDOWN_MS)) return;
                if (!goopFreeze(player)) return;
                setCooldown(player, "goop2", System.currentTimeMillis());
                player.sendActionBar("§bUsed [2]");
            }
            return;
        }

        if (c == SoulCharacter.MAGNET) {
            if (left) {
                if (!isCooldownReady(player, "magnet1", BASE_COOLDOWN_MS)) return;
                if (!magnetPull(player)) return;
                setCooldown(player, "magnet1", System.currentTimeMillis());
                player.sendActionBar("§aUsed [1]");
            } else {
                if (!isCooldownReady(player, "magnet2", BASE_COOLDOWN_MS)) return;
                if (!magnetPush(player)) return;
                setCooldown(player, "magnet2", System.currentTimeMillis());
                player.sendActionBar("§bUsed [2]");
            }
            return;
        }

        if (c == SoulCharacter.SORCERER) {
            if (left) {
                if (!isCooldownReady(player, "sorc1", BASE_COOLDOWN_MS)) return;
                if (!sorcererMove(player)) return;
                setCooldown(player, "sorc1", System.currentTimeMillis());
                player.sendActionBar("§aUsed [1]");
            } else {
                if (!isCooldownReady(player, "sorc2", BASE_COOLDOWN_MS)) return;
                if (!sorcererRandomTeleport(player)) return;
                setCooldown(player, "sorc2", System.currentTimeMillis());
                player.sendActionBar("§bUsed [2]");
            }
            return;
        }

        if (c == SoulCharacter.COPYCAT) {
            if (left) {
                if (!isCooldownReady(player, "copy1", BASE_COOLDOWN_MS)) return;
                if (!copycatRoll(player)) return;
                setCooldown(player, "copy1", System.currentTimeMillis());
                player.sendActionBar("§aUsed [1]");
            } else {
                if (!isCooldownReady(player, "copy2", 120_000L)) return;
                if (!copycatCopySpecial(player)) return;
                setCooldown(player, "copy2", System.currentTimeMillis());
                player.sendActionBar("§bUsed [2]");
            }
            return;
        }

        if (c == SoulCharacter.DARK_KNIGHT) {
            if (left) {
                if (!darkKnightShadowStep(player)) return;
            } else {
                if (!isCooldownReady(player, "dk2", BASE_COOLDOWN_MS)) return;
                if (!darkKnightBackstab(player)) return;
                setCooldown(player, "dk2", System.currentTimeMillis());
            }
            return;
        }

        if (c == SoulCharacter.UNTAMED_BEAST) {
            if (left) {
                if (!isCooldownReady(player, "beast1", BASE_COOLDOWN_MS)) return;
                if (!beastDisarm(player)) return;
                setCooldown(player, "beast1", System.currentTimeMillis());
                player.sendActionBar("§aUsed [1]");
            } else {
                if (!isCooldownReady(player, "beast2", BASE_COOLDOWN_MS)) return;
                if (!beastDuel(player)) return;
                setCooldown(player, "beast2", System.currentTimeMillis());
                player.sendActionBar("§bUsed [2]");
            }
            return;
        }

        if (left) {
            useBase(player);
            player.sendActionBar("§aUsed [1]");
        } else {
            if (c == SoulCharacter.KING_OF_HEAT) {
                if (!isCooldownReady(player, "heat2", BASE_COOLDOWN_MS)) return;
                if (!heatFlamethrower(player)) return;
                setCooldown(player, "heat2", System.currentTimeMillis());
                player.sendActionBar("§bUsed [2]");
            } else {
                useSpecial(player);
                player.sendActionBar("§bUsed [2]");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        if (!isInSouls(player)) return;
        if (SoulsItems.isShardOfSoul(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!isInSouls(player)) return;
        ItemStack current = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        boolean currentShard = SoulsItems.isShardOfSoul(current);
        boolean cursorShard = SoulsItems.isShardOfSoul(cursor);
        if (!currentShard && !cursorShard) return;

        // Soul item is not movable during matches/FFA (kit editor can move/save the placeholder in a separate flow).
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!isInSouls(player)) return;
        ItemStack old = e.getOldCursor();
        boolean draggingShard = SoulsItems.isShardOfSoul(old);
        if (!draggingShard) {
            for (ItemStack it : e.getNewItems().values()) {
                if (SoulsItems.isShardOfSoul(it)) {
                    draggingShard = true;
                    break;
                }
            }
        }
        if (!draggingShard) return;

        // Soul item is not movable during matches/FFA.
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!isInSouls(player)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        // (No souls currently have fall-damage immunity.)
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        long until = frozenUntil.getOrDefault(player.getUniqueId(), 0L);
        if (until <= System.currentTimeMillis()) return;

        Location from = frozenFrom.get(player.getUniqueId());
        if (from == null) from = e.getFrom();

        Location to = e.getTo();
        if (to == null) return;

        // lock x/z and cursor
        e.setTo(new Location(from.getWorld(), from.getX(), to.getY(), from.getZ(), from.getYaw(), from.getPitch()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        cooldowns.keySet().removeIf(k -> k.startsWith(id + ":"));
        frozenUntil.remove(id);
        frozenFrom.remove(id);
        selectedCharacter.remove(id);
        originalMaxHealth.remove(id);
        specialWeatherUntil.remove(id);
        magnetStacks.remove(id);
        magnetLastHitAt.remove(id);
        genocideNextEffectAt.remove(id);
        originalReach.remove(id);
        archangelJumpsUsed.remove(id);
        archangelPrevInvulnerable.remove(id);
        bountyArmorRestore.remove(id);
        originalAttackSpeed.remove(id);
        monarchLastBeamAt.remove(id);
        darkKnightShadow.remove(id);
        darkKnightShadowExpiresAt.remove(id);
    }

    private void useBase(Player player) {
        SoulCharacter c = getSelected(player);
        if (c == null) c = SoulCharacter.GOOP;

        // Goop + Magnet + Sorcerer + Copycat are handled by click handler (2 base abilities, no special).
        if (c == SoulCharacter.GOOP || c == SoulCharacter.MAGNET || c == SoulCharacter.SORCERER || c == SoulCharacter.COPYCAT || c == SoulCharacter.UNTAMED_BEAST) return;

        if (!isCooldownReady(player, "base", BASE_COOLDOWN_MS)) return;

        boolean ok = false;
        switch (c) {
            case DEVILS_FROST -> ok = frostStun(player);
            case VOICE_OF_THE_SEA -> ok = seaRiptideBoost(player);
            case ARTIFICIAL_GENOCIDE -> ok = genocideTeleport(player);
            case COSMIC_DESTROYER -> ok = cosmicSmash(player);
            case KING_OF_HEAT -> ok = heatFlameJump(player);
            case ARCHANGEL -> ok = archangelLevitate(player);
            case BOUNTY_HUNTER -> ok = bountySmokeBomb(player);
            case BLOODY_MONARCH -> ok = monarchBeam(player, false);
            case RAILGUN -> ok = railgunNuke(player);
            default -> {}
        }

        if (ok) setCooldown(player, "base", System.currentTimeMillis());
    }

    private void useSpecial(Player player) {
        if (!isCooldownReady(player, "special", SPECIAL_COOLDOWN_MS)) return;

        SoulCharacter c = getSelected(player);
        if (c == null) c = SoulCharacter.GOOP;

        boolean ok = false;
        switch (c) {
            case DEVILS_FROST -> ok = frostHearts(player);
            case VOICE_OF_THE_SEA -> ok = seaThunderstorm(player);
            case ARTIFICIAL_GENOCIDE -> ok = genocideShuffle(player);
            case COSMIC_DESTROYER -> ok = cosmicBlackhole(player);
            case KING_OF_HEAT -> ok = heatFlamethrower(player);
            case ARCHANGEL -> ok = archangelInvulnerable(player);
            case BOUNTY_HUNTER -> ok = bountyInvis(player);
            case BLOODY_MONARCH -> ok = monarchBeam(player, true);
            case RAILGUN -> ok = railgunStabShot(player);
            default -> player.sendMessage(ChatColor.RED + "No special ability for this soul yet.");
        }

        if (ok) setCooldown(player, "special", System.currentTimeMillis());
    }

    private boolean goopBounce(Player caster) {
        Player target = findNearestEnemy(caster, 8.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }
        Vector away = target.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize();
        target.setVelocity(away.multiply(1.6).setY(0.35));
        caster.playSound(caster.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0f, 1.0f);
        target.playSound(target.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0f, 0.8f);
        particleItemAt(target.getLocation().clone().add(0, 1.0, 0), new ItemStack(Material.SLIME_BALL), 18, 0.4, 0.25, 0.4);
        return true;
    }

    private boolean goopFreeze(Player caster) {
        Player target = findNearestEnemy(caster, 8.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }
        freeze(target, 2_000L);
        caster.playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        target.playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
        particleAt(target.getLocation().clone().add(0, 1.0, 0), Particle.SNOWFLAKE, 22, 0.35, 0.35, 0.35, 0.0);
        return true;
    }

    private boolean frostStun(Player caster) {
        Player target = findNearestEnemy(caster, 8.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }
        freeze(target, 3_000L);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 3, 4, true, false, false));
        caster.playSound(caster.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        particleAt(target.getLocation().clone().add(0, 1.0, 0), Particle.SNOWFLAKE, 28, 0.45, 0.45, 0.45, 0.0);
        return true;
    }

    private boolean frostHearts(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return false;
        originalMaxHealth.putIfAbsent(player.getUniqueId(), attr.getBaseValue());

        double original = originalMaxHealth.get(player.getUniqueId());
        attr.setBaseValue(original + 10.0);
        player.setHealth(Math.min(player.getHealth() + 10.0, attr.getBaseValue()));
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        particle(player, Particle.HEART, 8, 0.5, 0.6, 0.5, 0.0);

        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
            if (!player.isOnline()) return;
            var a = player.getAttribute(Attribute.MAX_HEALTH);
            if (a == null) return;
            a.setBaseValue(10.0); // 5 hearts
            player.setHealth(Math.min(player.getHealth(), 10.0));

            Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
                if (!player.isOnline()) return;
                var a2 = player.getAttribute(Attribute.MAX_HEALTH);
                if (a2 == null) return;
                Double orig = originalMaxHealth.remove(player.getUniqueId());
                if (orig != null) {
                    a2.setBaseValue(orig);
                    player.setHealth(Math.min(player.getHealth(), orig));
                }
            }, 20L * 10L);
        }, 20L * 30L);
        return true;
    }

    private boolean seaRiptideBoost(Player player) {
        Vector dir = player.getLocation().getDirection().normalize();
        // Faster/farther dash that can go upward based on look direction.
        Vector vel = dir.multiply(2.4);
        vel.setY(Math.max(dir.getY() * 1.4, 0.25));
        player.setVelocity(vel);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.1f);
        particle(player, Particle.SPLASH, 20, 0.45, 0.35, 0.45, 0.0);
        return true;
    }

    private boolean seaThunderstorm(Player player) {
        World world = player.getWorld();
        boolean prevStorm = world.hasStorm();
        boolean prevThundering = world.isThundering();
        int prevDuration = world.getWeatherDuration();
        int prevThunderDuration = world.getThunderDuration();

        world.setStorm(true);
        world.setThundering(true);
        world.setWeatherDuration(20 * 20);
        world.setThunderDuration(20 * 20);

        UUID id = player.getUniqueId();
        specialWeatherUntil.put(id, System.currentTimeMillis() + 20_000L);

        Bukkit.getScheduler().runTaskTimer(Craftmen.get(), task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }
            long until = specialWeatherUntil.getOrDefault(id, 0L);
            if (until <= System.currentTimeMillis()) {
                task.cancel();
                return;
            }
            // Visual lightning + heavy damage to nearby enemies (without harming the caster).
            for (int i = 0; i < 4; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = 6 + Math.random() * 10;
                Location loc = player.getLocation().clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                loc.setY(world.getHighestBlockYAt(loc) + 1);
                world.strikeLightningEffect(loc);
            }

            Location center = player.getLocation();
            for (Player p : world.getPlayers()) {
                if (p == player) continue;
                if (!p.isOnline() || p.isDead()) continue;
                if (!isInSouls(p)) continue;
                if (!sameContext(player, p)) continue;
                if (p.getLocation().distanceSquared(center) > (14.0 * 14.0)) continue;

                // "Lots of damage": 12 hearts (24 damage). Clamp to leave at least 0.5 heart if needed by other plugins.
                double dmg = 24.0;
                world.strikeLightningEffect(p.getLocation());
                p.damage(dmg, player);
            }
        }, 0L, 20L);

        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
            specialWeatherUntil.remove(id);
            world.setStorm(prevStorm);
            world.setThundering(prevThundering);
            world.setWeatherDuration(prevDuration);
            world.setThunderDuration(prevThunderDuration);
        }, 20L * 20L);
        return true;
    }

    private void freeze(Player target, long millis) {
        UUID id = target.getUniqueId();
        frozenUntil.put(id, System.currentTimeMillis() + millis);
        frozenFrom.put(id, target.getLocation().clone());
        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
            long until = frozenUntil.getOrDefault(id, 0L);
            if (until <= System.currentTimeMillis()) {
                frozenUntil.remove(id);
                frozenFrom.remove(id);
            }
        }, Math.max(1L, millis / 50L));
    }

    private Player findNearestEnemy(Player caster, double range) {
        if (caster == null) return null;
        Location from = caster.getLocation();
        Player best = null;
        double bestDist = range * range;

        for (Player p : caster.getWorld().getPlayers()) {
            if (p == caster) continue;
            if (!p.isOnline() || p.isDead()) continue;
            if (!isInSouls(p)) continue;
            if (!sameContext(caster, p)) continue;

            double d = p.getLocation().distanceSquared(from);
            if (d <= bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private boolean sameContext(Player a, Player b) {
        // If both are in match, ensure same match. If both in FFA, ensure same instance (damage rules already).
        Match ma = Craftmen.get().getMatchManager().getMatch(a);
        Match mb = Craftmen.get().getMatchManager().getMatch(b);
        if (ma != null || mb != null) return ma != null && ma.equals(mb);

        UUID ia = Craftmen.get().getFfaManager().getPlayerInstanceId(a);
        UUID ib = Craftmen.get().getFfaManager().getPlayerInstanceId(b);
        return ia != null && ia.equals(ib);
    }

    private boolean isCooldownReady(Player player, String key, long cooldownMs) {
        if (player == null) return false;
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId() + ":" + key, 0L);
        long remaining = (last + cooldownMs) - now;
        if (remaining > 0) {
            long sec = Math.max(1, (long) Math.ceil(remaining / 1000.0));
            player.sendMessage(ChatColor.RED + "Cooldown: " + sec + "s");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return false;
        }
        return true;
    }

    private void setCooldown(Player player, String key, long atMillis) {
        cooldowns.put(player.getUniqueId() + ":" + key, atMillis);
    }

    private void startTasks() {
        actionbarTask = Bukkit.getScheduler().runTaskTimer(Craftmen.get(), this::tickActionbar, 10L, 10L);
        passiveTask = Bukkit.getScheduler().runTaskTimer(Craftmen.get(), this::tickPassives, 20L, 20L);
    }

    private boolean goopLaunchSelf(Player player) {
        if (player == null) return false;
        Vector dir = player.getLocation().getDirection().normalize();
        Vector vel = dir.multiply(1.8);
        vel.setY(Math.max(dir.getY(), 0.15) + 0.25);
        player.setVelocity(vel);
        player.playSound(player.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0f, 1.2f);
        return true;
    }

    private void tickActionbar() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isInSouls(player)) continue;
            ItemStack main = player.getInventory().getItemInMainHand();
            if (!SoulsItems.isShardOfSoul(main)) continue;

            long now = System.currentTimeMillis();
            SoulCharacter c = getSelected(player);
            if (c == null) c = SoulCharacter.GOOP;

            String key1 = (c == SoulCharacter.GOOP) ? "goop1" : (c == SoulCharacter.MAGNET) ? "magnet1" : "base";
            String key2 = (c == SoulCharacter.GOOP) ? "goop2" : (c == SoulCharacter.MAGNET) ? "magnet2" : "special";
            if (c == SoulCharacter.SORCERER) {
                key1 = "sorc1";
                key2 = "sorc2";
            }
            if (c == SoulCharacter.COPYCAT) {
                key1 = "copy1";
                key2 = "copy2";
            }
            if (c == SoulCharacter.DARK_KNIGHT) {
                key1 = "dk1";
                key2 = "dk2";
            }
            if (c == SoulCharacter.UNTAMED_BEAST) {
                key1 = "beast1";
                key2 = "beast2";
            }
            if (c == SoulCharacter.KING_OF_HEAT) {
                key2 = "heat2";
            }

            long baseRemaining = remaining(player, key1, BASE_COOLDOWN_MS, now);
            long slot2Remaining = (c == SoulCharacter.GOOP)
                    ? remaining(player, key2, GOOP_RIGHT_CLICK_COOLDOWN_MS, now)
                    : (c == SoulCharacter.MAGNET)
                    ? remaining(player, key2, BASE_COOLDOWN_MS, now)
                    : (c == SoulCharacter.SORCERER)
                    ? remaining(player, key2, BASE_COOLDOWN_MS, now)
                    : (c == SoulCharacter.COPYCAT)
                    ? remaining(player, key2, BASE_COOLDOWN_MS, now)
                    : (c == SoulCharacter.DARK_KNIGHT)
                    ? remaining(player, key2, BASE_COOLDOWN_MS, now)
                    : (c == SoulCharacter.UNTAMED_BEAST)
                    ? remaining(player, key2, BASE_COOLDOWN_MS, now)
                    : (c == SoulCharacter.KING_OF_HEAT)
                    ? remaining(player, key2, BASE_COOLDOWN_MS, now)
                    : remaining(player, key2, SPECIAL_COOLDOWN_MS, now);

            String one = formatCooldownToken(1, baseRemaining, true);
            String two = formatCooldownToken(2, slot2Remaining, false);
            player.sendActionBar(one + " §8" + two);
        }
    }

    private static String formatCooldownToken(int num, long remainingMs, boolean base) {
        String readyColor = base ? "§a" : "§b";
        String cdColor = base ? "§c" : "§7";
        if (remainingMs <= 0) return readyColor + "[" + num + "]";
        long sec = Math.max(1, (long) Math.ceil(remainingMs / 1000.0));
        return cdColor + "[" + num + " " + sec + "s]";
    }

    private void tickPassives() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isInSouls(player)) continue;
            SoulCharacter c = getSelected(player);
            if (c == null) c = SoulCharacter.GOOP;

            switch (c) {
                case GOOP -> {
                    // Passive: Speed I
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, false));
                    particleItemAt(player.getLocation().clone().add(0, 1.0, 0), new ItemStack(Material.SLIME_BALL), 4, 0.35, 0.25, 0.35);
                }
                case DEVILS_FROST -> {
                    // Frost Walker passive: ensure boots are enchanted while in Souls.
                    ItemStack boots = player.getInventory().getBoots();
                    if (boots != null && boots.getType() != org.bukkit.Material.AIR) {
                        ItemMeta meta = boots.getItemMeta();
                        if (meta != null && !meta.hasEnchant(org.bukkit.enchantments.Enchantment.FROST_WALKER)) {
                            meta.addEnchant(org.bukkit.enchantments.Enchantment.FROST_WALKER, 2, true);
                            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                            boots.setItemMeta(meta);
                        }
                    }
                    particle(player, Particle.SNOWFLAKE, 6, 0.45, 0.35, 0.45, 0.0);
                }
                case VOICE_OF_THE_SEA -> {
                    if (player.getWorld().hasStorm()) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false, false));
                    }
                    particle(player, Particle.SPLASH, 6, 0.45, 0.25, 0.45, 0.0);
                }
                case MAGNET -> tickMagnetPassive(player);
                case ARTIFICIAL_GENOCIDE -> tickGenocidePassive(player);
                case SORCERER -> tickSorcererPassive(player);
                case KING_OF_HEAT -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false, false));
                    particle(player, Particle.FLAME, 5, 0.35, 0.2, 0.35, 0.01);
                }
                case ARCHANGEL -> tickArchangelPassive(player);
                case BOUNTY_HUNTER -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false, false));
                    particle(player, Particle.SMOKE, 6, 0.45, 0.25, 0.45, 0.01);
                }
                case COPYCAT -> tickCopycatPassive(player);
            case BLOODY_MONARCH -> {
                particle(player, Particle.DRIPPING_DRIPSTONE_LAVA, 4, 0.35, 0.25, 0.35, 0.0);
            }
            case UNTAMED_BEAST -> {
                particle(player, Particle.CRIT, 5, 0.35, 0.25, 0.35, 0.0);
            }
            default -> {}
        }
        }

        // Restore Sorcerer reach for anyone no longer in Souls / not Sorcerer.
        for (var entry : new HashMap<>(originalReach).entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline() || !isInSouls(p) || getSelected(p) != SoulCharacter.SORCERER) {
                if (p != null && p.isOnline()) {
                    Attribute reachAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("player.entity_interaction_range"));
                    if (reachAttr != null) {
                        AttributeInstance attr = p.getAttribute(reachAttr);
                        if (attr != null) attr.setBaseValue(entry.getValue());
                    }
                }
                originalReach.remove(entry.getKey());
            }
        }

        // Restore Copycat attack speed when not in Souls / not Copycat.
        for (var entry : new HashMap<>(originalAttackSpeed).entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline() || !isInSouls(p) || getSelected(p) != SoulCharacter.COPYCAT) {
                if (p != null && p.isOnline()) {
                    var attr = p.getAttribute(Attribute.ATTACK_SPEED);
                    if (attr != null) attr.setBaseValue(entry.getValue());
                }
                originalAttackSpeed.remove(entry.getKey());
            }
        }
    }

    private void tickMagnetPassive(Player player) {
        long now = System.currentTimeMillis();
        long last = magnetLastHitAt.getOrDefault(player.getUniqueId(), 0L);
        if (last <= 0 || (now - last) > MAGNET_STACK_RESET_MS) {
            magnetStacks.remove(player.getUniqueId());
            return;
        }
        int stacks = Math.max(1, magnetStacks.getOrDefault(player.getUniqueId(), 1));
        int amp = Math.min(MAGNET_MAX_SPEED_AMP, stacks - 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, amp, true, false, false));
        particle(player, Particle.CRIT, 6, 0.35, 0.25, 0.35, 0.0);
    }

    private void tickGenocidePassive(Player player) {
        long now = System.currentTimeMillis();
        long next = genocideNextEffectAt.getOrDefault(player.getUniqueId(), now);
        if (now < next) return;
        genocideNextEffectAt.put(player.getUniqueId(), now + 60_000L);

        PotionEffectType[] pool = {
                PotionEffectType.SPEED,
                PotionEffectType.STRENGTH,
                PotionEffectType.REGENERATION,
                PotionEffectType.RESISTANCE,
                PotionEffectType.JUMP_BOOST,
                PotionEffectType.HASTE,
                PotionEffectType.SLOWNESS,
                PotionEffectType.WEAKNESS,
                PotionEffectType.POISON,
                PotionEffectType.BLINDNESS
        };
        PotionEffectType type = pool[rng.nextInt(pool.length)];
        int amp = rng.nextInt(2); // I-II
        int seconds = 45;
        player.addPotionEffect(new PotionEffect(type, 20 * seconds, amp, true, true, true));
        particle(player, Particle.ENCHANT, 20, 0.6, 0.5, 0.6, 0.0);
    }

    private void tickSorcererPassive(Player player) {
        Attribute reachAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("player.entity_interaction_range"));
        if (reachAttr == null) return;
        AttributeInstance attr = player.getAttribute(reachAttr);
        if (attr == null) return;

        UUID id = player.getUniqueId();
        originalReach.putIfAbsent(id, attr.getBaseValue());

        double original = originalReach.getOrDefault(id, attr.getBaseValue());
        double desired = original + 0.5;
        if (Math.abs(attr.getBaseValue() - desired) > 0.0001) {
            attr.setBaseValue(desired);
        }
        particle(player, Particle.ENCHANT, 10, 0.5, 0.45, 0.5, 0.0);
    }

    private void tickArchangelPassive(Player player) {
        // Allow one extra jump mid-air.
        if (player.isOnGround()) {
            archangelJumpsUsed.put(player.getUniqueId(), 0);
            player.setAllowFlight(true);
            return;
        }
        // In air: keep allowFlight on so PlayerToggleFlightEvent can fire for double jump.
        player.setAllowFlight(true);
        particle(player, Particle.END_ROD, 3, 0.35, 0.35, 0.35, 0.0);
    }

    private void tickCopycatPassive(Player player) {
        // "0.5% faster" attack cooldown => tiny attack speed buff.
        var attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr == null) return;

        UUID id = player.getUniqueId();
        originalAttackSpeed.putIfAbsent(id, attr.getBaseValue());
        double base = originalAttackSpeed.getOrDefault(id, attr.getBaseValue());
        double desired = base * 1.005;
        if (Math.abs(attr.getBaseValue() - desired) > 0.0001) {
            attr.setBaseValue(desired);
        }
        particle(player, Particle.NOTE, 2, 0.35, 0.25, 0.35, 0.0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArchangelDoubleJump(PlayerToggleFlightEvent e) {
        Player player = e.getPlayer();
        if (!isInSouls(player)) return;
        if (getSelected(player) != SoulCharacter.ARCHANGEL) return;

        // Only in actual fight contexts.
        var prof = Craftmen.get().getProfileManager().getProfile(player);
        if (prof == null || (prof.getState() != com.tommustbe12.craftmen.profile.PlayerState.IN_MATCH
                && prof.getState() != com.tommustbe12.craftmen.profile.PlayerState.FFA_FIGHTING)) {
            return;
        }

        int used = archangelJumpsUsed.getOrDefault(player.getUniqueId(), 0);
        if (used >= 2) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);
        archangelJumpsUsed.put(player.getUniqueId(), used + 1);
        player.setFlying(false);

        Vector v = player.getLocation().getDirection().normalize().multiply(0.45);
        v.setY(0.55);
        player.setVelocity(v);
        player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.4f);
    }

    private boolean magnetPull(Player caster) {
        Player target = findNearestEnemy(caster, 20.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }
        Vector delta = target.getLocation().toVector().subtract(caster.getLocation().toVector());
        Vector dir = delta.lengthSquared() < 0.01 ? caster.getLocation().getDirection().normalize() : delta.normalize();
        // Pull is intentionally softer than push.
        caster.setVelocity(dir.clone().multiply(1.05).setY(Math.max(dir.getY() * 0.65, 0.08)));
        target.setVelocity(dir.clone().multiply(-1.05).setY(Math.max(-dir.getY() * 0.65, 0.08)));
        caster.playSound(caster.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.2f);
        target.playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.9f);
        particle(caster, Particle.CRIT, 14, 0.45, 0.35, 0.45, 0.0);
        particle(target, Particle.CRIT, 14, 0.45, 0.35, 0.45, 0.0);
        particleAt(caster.getLocation().clone().add(0, 1.0, 0), Particle.ELECTRIC_SPARK, 25, 0.6, 0.45, 0.6, 0.02);
        particleAt(target.getLocation().clone().add(0, 1.0, 0), Particle.ELECTRIC_SPARK, 25, 0.6, 0.45, 0.6, 0.02);
        return true;
    }

    private boolean magnetPush(Player caster) {
        Player target = findNearestEnemy(caster, 14.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }
        Vector away = target.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize();
        caster.setVelocity(away.clone().multiply(-1.4).setY(0.25));
        target.setVelocity(away.clone().multiply(1.8).setY(0.35));
        caster.playSound(caster.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 1.2f);
        target.playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 0.9f);
        particleAt(target.getLocation().clone().add(0, 1.0, 0), Particle.CLOUD, 18, 0.45, 0.25, 0.45, 0.02);
        particleAt(caster.getLocation().clone().add(0, 1.0, 0), Particle.ELECTRIC_SPARK, 25, 0.6, 0.45, 0.6, 0.02);
        particleAt(target.getLocation().clone().add(0, 1.0, 0), Particle.ELECTRIC_SPARK, 25, 0.6, 0.45, 0.6, 0.02);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSoulsHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!isInSouls(damager) || !isInSouls(victim)) return;
        if (!sameContext(damager, victim)) return;

        SoulCharacter c = getSelected(damager);
        if (c == SoulCharacter.MAGNET) {
            UUID id = damager.getUniqueId();
            magnetLastHitAt.put(id, System.currentTimeMillis());
            int stacks = magnetStacks.getOrDefault(id, 0) + 1;
            magnetStacks.put(id, Math.min(1 + MAGNET_MAX_SPEED_AMP, stacks));
        } else if (c == SoulCharacter.BLOODY_MONARCH) {
            // Lifesteal: heal 25% of damage dealt.
            double heal = e.getFinalDamage() * 0.25;
            if (heal > 0) {
                double newHp = Math.min(damager.getHealth() + heal, damager.getMaxHealth());
                damager.setHealth(newHp);
                particle(damager, Particle.HEART, 1, 0.25, 0.35, 0.25, 0.0);
            }
        } else if (c == SoulCharacter.DARK_KNIGHT) {
            // Passive: 25% chance to blind on hit.
            if (rng.nextDouble() <= 0.25) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 0, true, false, false));
                particleAt(victim.getLocation().clone().add(0, 1.0, 0), Particle.SMOKE, 10, 0.35, 0.35, 0.35, 0.02);
            }
        } else if (c == SoulCharacter.UNTAMED_BEAST) {
            // Passive: 10% more damage at <= 2 hearts (20% hp).
            double threshold = damager.getMaxHealth() * 0.20;
            if (damager.getHealth() <= threshold) {
                // Can't change final damage here (MONITOR), but apply a small extra hit.
                victim.setNoDamageTicks(0);
                victim.damage(Math.max(0.1, e.getFinalDamage() * 0.10), damager);
            }
        }

        // Untamed Beast [2]: punch releases the duel.
        UUID casterId = damager.getUniqueId();
        UUID targetId = beastDuelTarget.get(casterId);
        if (targetId != null && targetId.equals(victim.getUniqueId())) {
            beastDuelTarget.remove(casterId);
            unfreeze(damager);
            unfreeze(victim);

            Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
                if (!damager.isOnline() || !victim.isOnline()) return;
                if (!isInSouls(damager) || !isInSouls(victim)) return;
                if (!sameContext(damager, victim)) return;

                Vector v = damager.getLocation().getDirection().normalize().multiply(2.2);
                v.setY(Math.max(0.35, v.getY()));
                victim.setVelocity(v);

                new org.bukkit.scheduler.BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (!victim.isOnline() || !isInSouls(victim)) {
                            cancel();
                            return;
                        }
                        ticks++;
                        if (ticks > 45) {
                            cancel();
                            return;
                        }
                        if (!victim.getLocation().getBlock().isPassable()) {
                            Location boom = victim.getLocation().clone();
                            boom.getWorld().createExplosion(boom, 2.4f, false, true, damager);
                            cancel();
                        }
                    }
                }.runTaskTimer(Craftmen.get(), 1L, 1L);
            }, 10L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBeastDamageBoost(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!isInSouls(damager) || !isInSouls(victim)) return;
        if (!sameContext(damager, victim)) return;
        if (getSelected(damager) != SoulCharacter.UNTAMED_BEAST) return;

        double threshold = damager.getMaxHealth() * 0.20;
        if (damager.getHealth() <= threshold) {
            e.setDamage(e.getDamage() * 1.10);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRailgunDeathBoom(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!isInSouls(player)) return;
        if (getSelected(player) != SoulCharacter.RAILGUN) return;
        if (!sameContext(player, player)) return;

        double finalHp = player.getHealth() - e.getFinalDamage();
        if (finalHp > 0) return;

        long now = System.currentTimeMillis();
        long last = railgunDeathBoomAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 1500L) return;
        railgunDeathBoomAt.put(player.getUniqueId(), now);

        Location loc = player.getLocation().clone();
        loc.getWorld().createExplosion(loc, 2.8f, false, true, player);
    }

    private boolean beastDisarm(Player caster) {
        Player target = findNearestEnemy(caster, 10.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }

        UUID tid = target.getUniqueId();
        if (!beastDisarmRestore.containsKey(tid)) {
            beastDisarmRestore.put(tid, target.getInventory().getStorageContents());
        }

        ItemStack[] contents = target.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            if (SoulsItems.isShardOfSoul(it)) continue;
            Material m = it.getType();
            Material repl = disarmToWood(m);
            if (repl == null) continue;
            contents[i] = new ItemStack(repl, it.getAmount());
        }
        target.getInventory().setStorageContents(contents);
        target.updateInventory();

        caster.playSound(caster.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, 0.9f, 1.3f);
        target.playSound(target.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, 0.9f, 1.0f);

        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
            if (!target.isOnline()) return;
            ItemStack[] restore = beastDisarmRestore.remove(tid);
            if (restore != null) {
                target.getInventory().setStorageContents(restore);
                target.updateInventory();
            }
        }, 20L * 2L);

        return true;
    }

    private static Material disarmToWood(Material m) {
        if (m == null) return null;
        String n = m.name();
        if (n.endsWith("_SWORD")) return Material.WOODEN_SWORD;
        if (n.endsWith("_AXE")) return Material.WOODEN_AXE;
        if (n.endsWith("_PICKAXE")) return Material.WOODEN_PICKAXE;
        if (n.endsWith("_SHOVEL")) return Material.WOODEN_SHOVEL;
        if (n.endsWith("_HOE")) return Material.WOODEN_HOE;
        if ("MACE".equals(n)) return Material.WOODEN_AXE;
        if ("TRIDENT".equals(n)) return Material.WOODEN_SWORD;
        if ("BOW".equals(n) || "CROSSBOW".equals(n)) return Material.BOW;
        return null;
    }

    private boolean beastDuel(Player caster) {
        Player target = findNearestEnemy(caster, 10.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }

        Location upC = caster.getLocation().clone().add(0, 10.0, 0);
        Location upT = target.getLocation().clone().add(0, 10.0, 0);
        caster.teleport(upC);
        target.teleport(upT);

        freeze(caster, 10_000L);
        freeze(target, 10_000L);
        beastDuelTarget.put(caster.getUniqueId(), target.getUniqueId());

        caster.playSound(caster.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.1f);
        target.playSound(target.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.0f);
        return true;
    }

    private void unfreeze(Player p) {
        if (p == null) return;
        frozenUntil.remove(p.getUniqueId());
        frozenFrom.remove(p.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onRailgunTntDamageReduce(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof TNTPrimed tnt)) return;
        Byte tagged = tnt.getPersistentDataContainer().get(railgunTntKey, org.bukkit.persistence.PersistentDataType.BYTE);
        if (tagged == null || tagged != (byte) 1) return;

        // Railgun [1] TNT is visual (damage handled directly on cast).
        e.setDamage(0.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRailgunTntImmunity(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!isInSouls(player)) return;
        if (getSelected(player) != SoulCharacter.RAILGUN) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && e.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }
        e.setCancelled(true);
    }

    private boolean darkKnightShadowStep(Player caster) {
        UUID id = caster.getUniqueId();
        long now = System.currentTimeMillis();

        Location shadow = darkKnightShadow.get(id);
        long expires = darkKnightShadowExpiresAt.getOrDefault(id, 0L);
        if (shadow != null && expires > now) {
            if (!isCooldownReady(caster, "dk1", BASE_COOLDOWN_MS)) return false;
            caster.teleport(shadow);
            caster.playSound(shadow, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.2f);
            particleAt(shadow.clone().add(0, 1.0, 0), Particle.SMOKE, 35, 0.5, 0.45, 0.5, 0.02);

            darkKnightShadow.remove(id);
            darkKnightShadowExpiresAt.remove(id);
            setCooldown(caster, "dk1", now);
            return true;
        }

        // Set shadow (no cooldown yet).
        Location mark = caster.getLocation().clone();
        darkKnightShadow.put(id, mark);
        darkKnightShadowExpiresAt.put(id, now + DARK_KNIGHT_SHADOW_TTL_MS);
        caster.playSound(mark, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.9f);
        particleAt(mark.clone().add(0, 1.0, 0), Particle.SQUID_INK, 18, 0.35, 0.35, 0.35, 0.0);

        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
            long until = darkKnightShadowExpiresAt.getOrDefault(id, 0L);
            if (until <= System.currentTimeMillis()) {
                darkKnightShadow.remove(id);
                darkKnightShadowExpiresAt.remove(id);
            }
        }, DARK_KNIGHT_SHADOW_TTL_MS / 50L);
        return true;
    }

    private boolean darkKnightBackstab(Player caster) {
        Player target = findNearestEnemy(caster, 12.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }

        Vector behind = target.getLocation().getDirection().normalize().multiply(-1.2);
        Location dest = target.getLocation().clone().add(behind);
        dest.setYaw(target.getLocation().getYaw());
        dest.setPitch(caster.getLocation().getPitch());
        if (!dest.getBlock().isPassable()) {
            dest = target.getLocation().clone().add(0, 0.1, 0);
        }

        caster.teleport(dest);
        caster.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.6f);
        particleAt(dest.clone().add(0, 1.0, 0), Particle.REVERSE_PORTAL, 18, 0.35, 0.35, 0.35, 0.02);
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 2, 0, true, false, false));
        return true;
    }

    private boolean railgunNuke(Player caster) {
        Player target = findNearestEnemy(caster, 18.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }

        Location center = target.getLocation().clone();
        World world = center.getWorld();
        if (world == null) return false;

        caster.playSound(caster.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.6f);
        particleAt(center.clone().add(0, 1.0, 0), Particle.SMOKE, 35, 1.2, 0.6, 1.2, 0.02);

        // Deal only ~2 hearts total from the nuke (TNT is mostly visual).
        target.setNoDamageTicks(0);
        target.damage(4.0, caster);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Location spawn = center.clone().add(dx + 0.5, 8.0, dz + 0.5);
                TNTPrimed tnt = world.spawn(spawn, TNTPrimed.class, ent -> {
                    ent.setFuseTicks(35 + rng.nextInt(10));
                    ent.setYield(0.0f);
                    ent.setIsIncendiary(false);
                    ent.setSource(caster);
                    ent.getPersistentDataContainer().set(railgunTntKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                });
                if (tnt != null) {
                    world.spawnParticle(Particle.CLOUD, spawn, 6, 0.15, 0.15, 0.15, 0.01);
                }
            }
        }
        return true;
    }

    private boolean railgunStabShot(Player caster) {
        Player target = findNearestEnemy(caster, 20.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }

        // Orbital strike cannon: straight line down at aim point to carve a deep hole.
        World world = caster.getWorld();
        Location start = caster.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        RayTraceResult hit = world.rayTraceBlocks(start, dir, 35.0, FluidCollisionMode.NEVER, true);

        Location impact = (hit != null && hit.getHitPosition() != null)
                ? new Location(world, hit.getHitPosition().getX(), hit.getHitPosition().getY(), hit.getHitPosition().getZ())
                : target.getLocation().clone();

        double ix = impact.getX();
        double iz = impact.getZ();
        int topY = Math.min(world.getMaxHeight() - 2, impact.getBlockY() + 18);
        int bottomY = Math.max(world.getMinHeight() + 1, impact.getBlockY() - 28);

        caster.playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.6f);
        for (int y = topY; y >= bottomY; y -= 2) {
            Location ex = new Location(world, ix, y, iz);
            world.spawnParticle(Particle.END_ROD, ex, 4, 0.1, 0.1, 0.1, 0.0);
            world.createExplosion(ex, 2.2f, false, true, caster);
        }

        // Damage: 5 hearts.
        target.setNoDamageTicks(0);
        target.damage(10.0, caster);

        return true;
    }

    private boolean genocideTeleport(Player player) {
        var world = player.getWorld();
        Vector dir = player.getLocation().getDirection().normalize();
        RayTraceResult hit = world.rayTraceBlocks(player.getEyeLocation(), dir, 10.0, FluidCollisionMode.NEVER, true);

        Location dest;
        if (hit != null && hit.getHitPosition() != null) {
            Vector v = hit.getHitPosition();
            dest = new Location(world, v.getX(), v.getY(), v.getZ());
            dest.subtract(dir.clone().multiply(0.6));
        } else {
            dest = player.getLocation().clone().add(dir.clone().multiply(10.0));
        }
        dest.setYaw(player.getLocation().getYaw());
        dest.setPitch(player.getLocation().getPitch());

        if (!dest.getBlock().isPassable()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        player.teleport(dest);
        player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        particleAt(dest.clone().add(0, 1.0, 0), Particle.REVERSE_PORTAL, 18, 0.35, 0.35, 0.35, 0.02);
        return true;
    }

    private boolean genocideShuffle(Player caster) {
        Player target = findNearestEnemy(caster, 10.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }
        ItemStack[] storage = target.getInventory().getStorageContents();

        // Never move the target's soul ability item.
        Map<Integer, ItemStack> locked = new HashMap<>();
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < storage.length; i++) {
            ItemStack it = storage[i];
            if (SoulsItems.isShardOfSoul(it)) {
                locked.put(i, it);
            } else {
                list.add(it);
            }
        }
        Collections.shuffle(list, rng);

        ItemStack[] shuffled = new ItemStack[storage.length];
        int idx = 0;
        for (int i = 0; i < shuffled.length; i++) {
            ItemStack keep = locked.get(i);
            if (keep != null) {
                shuffled[i] = keep;
            } else {
                shuffled[i] = idx < list.size() ? list.get(idx++) : null;
            }
        }

        target.getInventory().setStorageContents(shuffled);
        caster.playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.7f);
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        particleAt(target.getLocation().clone().add(0, 1.0, 0), Particle.ENCHANT, 30, 0.7, 0.5, 0.7, 0.0);
        return true;
    }

    private boolean cosmicSmash(Player caster) {
        Location center = caster.getLocation();
        int r = 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Location l = center.clone().add(dx, dy, dz);
                    if (l.distanceSquared(center) > 7.5) continue;
                    var block = l.getBlock();
                    if (block.isEmpty()) continue;
                    Material t = block.getType();
                    if (t == Material.BEDROCK || t == Material.BARRIER) continue;
                    if (t.name().contains("CHEST")) continue;
                    block.setType(Material.AIR, false);
                }
            }
        }

        for (Player p : caster.getWorld().getPlayers()) {
            if (p == caster) continue;
            if (!isInSouls(p)) continue;
            if (!sameContext(caster, p)) continue;
            if (p.getLocation().distanceSquared(center) > (4.0 * 4.0)) continue;
            p.damage(4.0, caster); // 2 hearts
        }

        caster.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.9f);
        particleAt(center.clone().add(0, 1.0, 0), Particle.SMOKE, 35, 0.8, 0.35, 0.8, 0.02);
        particleAt(center.clone().add(0, 1.0, 0), Particle.END_ROD, 12, 0.7, 0.35, 0.7, 0.0);
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCosmicArmorDamage(PlayerItemDamageEvent e) {
        Player player = e.getPlayer();
        if (!isInSouls(player)) return;
        if (getSelected(player) != SoulCharacter.COSMIC_DESTROYER) return;
        int reduced = (int) Math.floor(e.getDamage() * 0.9);
        e.setDamage(Math.max(0, reduced));
    }

    private boolean cosmicBlackhole(Player caster) {
        Location center = caster.getLocation().clone();
        caster.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.7f);

        // Initial "space rupture" around the cast point.
        {
            int r = 3;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        Location l = center.clone().add(dx, dy, dz);
                        if (l.distanceSquared(center) > (r * r + 2)) continue;
                        var block = l.getBlock();
                        if (block.isEmpty()) continue;
                        Material t = block.getType();
                        if (t == Material.BEDROCK || t == Material.BARRIER) continue;
                        if (t.name().contains("CHEST")) continue;
                        block.setType(Material.AIR, false);
                    }
                }
            }
            particleAt(center.clone().add(0, 1.0, 0), Particle.SMOKE, 70, 1.4, 0.6, 1.4, 0.03);
            particleAt(center.clone().add(0, 1.0, 0), Particle.SQUID_INK, 40, 1.2, 0.5, 1.2, 0.0);
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.6f);
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || !isInSouls(caster)) {
                    cancel();
                    return;
                }
                ticks += 5;
                if (ticks > 20 * 5) { // 5 seconds
                    // Final burst: extra damage + fling players out.
                    for (Player p : center.getWorld().getPlayers()) {
                        if (p == caster) continue;
                        if (!isInSouls(p)) continue;
                        if (!sameContext(caster, p)) continue;
                        double dist2 = p.getLocation().distanceSquared(center);
                        if (dist2 > (12.0 * 12.0)) continue;

                        p.setNoDamageTicks(0);
                        p.damage(4.0, caster); // 2 hearts final pop

                        Vector out = p.getLocation().toVector().subtract(center.toVector());
                        if (out.lengthSquared() < 0.01) out = new Vector(rng.nextDouble() - 0.5, 0, rng.nextDouble() - 0.5);
                        out = out.normalize().multiply(1.6);
                        out.setY(0.65);
                        // Add randomness so they get scattered.
                        out.add(new Vector((rng.nextDouble() - 0.5) * 0.6, 0, (rng.nextDouble() - 0.5) * 0.6));
                        p.setVelocity(out);
                    }

                    particleAt(center.clone().add(0, 1.0, 0), Particle.SMOKE, 90, 1.8, 0.7, 1.8, 0.05);
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.85f);
                    cancel();
                    return;
                }

                for (Player p : center.getWorld().getPlayers()) {
                    if (p == caster) continue;
                    if (!isInSouls(p)) continue;
                    if (!sameContext(caster, p)) continue;
                    double dist2 = p.getLocation().distanceSquared(center);
                    if (dist2 > (12.0 * 12.0)) continue;

                    Vector pull = center.toVector().subtract(p.getLocation().toVector());
                    if (pull.lengthSquared() > 0.01) {
                        Vector vel = pull.normalize().multiply(0.55);
                        vel.setY(Math.min(0.3, Math.max(-0.1, vel.getY())));
                        p.setVelocity(p.getVelocity().add(vel));
                    }

                    // Black-hole visuals around affected players.
                    particleAt(p.getLocation().clone().add(0, 1.0, 0), Particle.SMOKE, 10, 0.35, 0.35, 0.35, 0.01);
                    particleAt(p.getLocation().clone().add(0, 1.0, 0), Particle.SQUID_INK, 6, 0.25, 0.25, 0.25, 0.0);

                    // Minor damage + blindness while trapped (kept light).
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, true, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 0, true, false, false));
                    if (ticks % 20 == 0) {
                        p.damage(1.0, caster); // 0.5 heart per second
                    }

                    // Damage enemy armor durability.
                    damageArmorPieces(p, 2);
                }
            }
        }.runTaskTimer(Craftmen.get(), 0L, 5L);

        // Strong swirl at the activation point.
        particleAt(center.clone().add(0, 1.0, 0), Particle.SMOKE, 45, 1.0, 0.45, 1.0, 0.02);
        particleAt(center.clone().add(0, 1.0, 0), Particle.SQUID_INK, 25, 0.9, 0.4, 0.9, 0.0);
        return true;
    }

    private void damageArmorPieces(Player target, int amount) {
        if (target == null || amount <= 0) return;
        ItemStack[] armor = target.getInventory().getArmorContents();
        boolean changed = false;
        for (int i = 0; i < armor.length; i++) {
            ItemStack it = armor[i];
            if (it == null || it.getType() == Material.AIR) continue;
            ItemMeta meta = it.getItemMeta();
            if (!(meta instanceof Damageable dmg)) continue;
            dmg.setDamage(dmg.getDamage() + amount);
            it.setItemMeta((ItemMeta) dmg);
            changed = true;
        }
        if (changed) {
            target.playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.7f, 1.4f);
        }
    }

    private boolean sorcererMove(Player caster) {
        Player target = findNearestEnemy(caster, 12.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }

        // Telekinesis: caster "drags" the target around by aiming their mouse.
        // Kept intentionally short + not too strong.
        caster.playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.1f);

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || !target.isOnline()) {
                    cancel();
                    return;
                }
                if (!isInSouls(caster) || !isInSouls(target) || !sameContext(caster, target)) {
                    cancel();
                    return;
                }

                ticks += 2;
                if (ticks > 20 * 3) { // 3s
                    cancel();
                    return;
                }

                Vector look = caster.getLocation().getDirection().normalize();
                Location desired = caster.getLocation().clone().add(look.clone().multiply(6.0));
                desired.setY(caster.getLocation().getY() + 0.6);

                Vector to = desired.toVector().subtract(target.getLocation().toVector());
                if (to.lengthSquared() > 0.01) {
                    Vector vel = to.normalize().multiply(0.65);
                    vel.setY(Math.max(-0.15, Math.min(0.35, vel.getY())));
                    target.setVelocity(vel);
                }

                particleAt(target.getLocation().clone().add(0, 1.0, 0), Particle.ENCHANT, 8, 0.45, 0.35, 0.45, 0.0);
            }
        }.runTaskTimer(Craftmen.get(), 0L, 2L);

        return true;
    }

    private boolean sorcererRandomTeleport(Player caster) {
        boolean shifting = caster.isSneaking();
        Player target = shifting ? caster : findNearestEnemy(caster, 12.0);
        if (!shifting && target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }

        Location base = target.getLocation().clone();
        int baseY = base.getBlockY();
        for (int tries = 0; tries < 24; tries++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = 6.0 + rng.nextDouble() * 6.0;
            double x = base.getX() + Math.cos(angle) * dist;
            double z = base.getZ() + Math.sin(angle) * dist;

            // Keep the teleport near the current fighting Y-level to avoid roof/barrier tops.
            for (int dy = 1; dy >= -2; dy--) {
                Location dest = new Location(base.getWorld(), x, baseY + dy, z, base.getYaw(), base.getPitch());
                if (!isSafeTeleportSpot(dest)) continue;
                target.teleport(dest);
                target.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                particleAt(dest.clone().add(0, 1.0, 0), Particle.REVERSE_PORTAL, 18, 0.35, 0.35, 0.35, 0.02);
                return true;
            }
        }

        return false;
    }

    private boolean isSafeTeleportSpot(Location dest) {
        if (dest == null || dest.getWorld() == null) return false;
        var feet = dest.getBlock();
        var head = dest.clone().add(0, 1, 0).getBlock();
        var below = dest.clone().add(0, -1, 0).getBlock();

        if (!feet.isPassable() || !head.isPassable()) return false;
        if (below.getType() == Material.BARRIER) return false;
        if (feet.getType() == Material.BARRIER || head.getType() == Material.BARRIER) return false;
        // Don't allow mid-air teleports; require solid ground.
        if (below.isPassable()) return false;
        return true;
    }

    private boolean heatFlameJump(Player caster) {
        Vector dir = caster.getLocation().getDirection().normalize();
        Vector vel = dir.multiply(1.4);
        vel.setY(Math.max(0.55, dir.getY() * 0.6 + 0.45));
        caster.setVelocity(vel);

        // Break a little ground around the caster (same radius idea as cosmic, but smaller).
        Location center = caster.getLocation();
        int r = 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Location l = center.clone().add(dx, dy, dz);
                    if (l.distanceSquared(center) > 7.5) continue;
                    var block = l.getBlock();
                    if (block.isEmpty()) continue;
                    Material t = block.getType();
                    if (t == Material.BEDROCK || t == Material.BARRIER) continue;
                    if (t.name().contains("CHEST")) continue;
                    block.setType(Material.AIR, false);
                }
            }
        }

        caster.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.1f);
        caster.getWorld().spawnParticle(org.bukkit.Particle.FLAME, center, 40, 0.6, 0.2, 0.6, 0.02);
        particleAt(center.clone().add(0, 1.0, 0), Particle.LAVA, 6, 0.5, 0.25, 0.5, 0.0);

        // Slight extra fire pressure nearby.
        for (Player p : caster.getWorld().getPlayers()) {
            if (p == caster) continue;
            if (!isInSouls(p) || !sameContext(caster, p)) continue;
            if (p.getLocation().distanceSquared(center) > (4.0 * 4.0)) continue;
            p.setNoDamageTicks(0);
            p.damage(1.0, caster); // 0.5 heart
            p.setFireTicks(Math.max(p.getFireTicks(), 80));
        }
        return true;
    }

    private boolean heatFlamethrower(Player caster) {
        Location origin = caster.getLocation().clone();
        caster.playSound(origin, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.2f);

        new org.bukkit.scheduler.BukkitRunnable() {
            int sec = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || !isInSouls(caster)) {
                    cancel();
                    return;
                }
                sec++;
                if (sec > 10) {
                    cancel();
                    return;
                }

                Location loc = caster.getEyeLocation();
                Vector look = loc.getDirection().normalize();

                caster.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc.add(look.clone().multiply(1.2)), 25, 0.35, 0.25, 0.35, 0.01);
                caster.playSound(caster.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.7f, 1.4f);

                for (Player p : caster.getWorld().getPlayers()) {
                    if (p == caster) continue;
                    if (!isInSouls(p) || !sameContext(caster, p)) continue;
                    if (p.getLocation().distanceSquared(caster.getLocation()) > (11.0 * 11.0)) continue;

                    Vector to = p.getEyeLocation().toVector().subtract(caster.getEyeLocation().toVector());
                    if (to.lengthSquared() < 0.01) continue;
                    double angle = look.angle(to.normalize());
                    if (angle > Math.toRadians(55)) continue;

                    // Force damage even if they're taking frequent hits (invulnerability frames).
                    p.setNoDamageTicks(0);
                    p.damage(6.0, caster); // 3 hearts per second
                    p.setFireTicks(Math.max(p.getFireTicks(), 80));
                }
            }
        }.runTaskTimer(Craftmen.get(), 0L, 20L);

        return true;
    }

    private boolean archangelLevitate(Player caster) {
        Player target = findNearestEnemy(caster, 10.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20 * 3, 2, true, false, false));
        caster.playSound(caster.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.0f, 1.4f);
        target.playSound(target.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.0f, 1.1f);
        particleAt(target.getLocation().clone().add(0, 1.0, 0), Particle.END_ROD, 18, 0.35, 0.6, 0.35, 0.0);
        return true;
    }

    private boolean archangelInvulnerable(Player caster) {
        UUID id = caster.getUniqueId();
        archangelPrevInvulnerable.putIfAbsent(id, caster.isInvulnerable());
        caster.setInvulnerable(true);
        caster.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 10, 0, true, false, false));
        caster.playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 0.9f, 1.3f);
        particle(caster, Particle.TOTEM_OF_UNDYING, 18, 0.6, 0.7, 0.6, 0.0);

        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
            if (!caster.isOnline()) return;
            Boolean prev = archangelPrevInvulnerable.remove(id);
            caster.setInvulnerable(prev != null && prev);
            caster.playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.8f);
        }, 20L * 10L);

        return true;
    }

    private boolean bountySmokeBomb(Player caster) {
        Location c = caster.getLocation().clone().add(0, 1.0, 0);
        particleAt(c, Particle.SMOKE, 70, 1.2, 0.6, 1.2, 0.05);
        particleAt(c, Particle.CLOUD, 35, 1.0, 0.4, 1.0, 0.02);
        caster.playSound(caster.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 0.7f, 1.4f);

        // Brief blindness to nearby enemies (soft CC).
        for (Player p : caster.getWorld().getPlayers()) {
            if (p == caster) continue;
            if (!isInSouls(p) || !sameContext(caster, p)) continue;
            if (p.getLocation().distanceSquared(caster.getLocation()) > (8.0 * 8.0)) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 0, true, false, false));
        }
        return true;
    }

    private boolean bountyInvis(Player caster) {
        UUID id = caster.getUniqueId();

        // Remove armor to make them truly invisible (armor included).
        if (!bountyArmorRestore.containsKey(id)) {
            bountyArmorRestore.put(id, caster.getInventory().getArmorContents());
        }
        caster.getInventory().setArmorContents(new ItemStack[4]);

        caster.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 10, 0, true, false, false));
        particle(caster, Particle.SMOKE, 30, 0.8, 0.6, 0.8, 0.02);
        caster.playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.6f);

        Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
            if (!caster.isOnline()) return;
            ItemStack[] restore = bountyArmorRestore.remove(id);
            if (restore != null) caster.getInventory().setArmorContents(restore);
        }, 20L * 10L);

        return true;
    }

    private boolean copycatRoll(Player caster) {
        boolean good = rng.nextBoolean();
        if (good) {
            caster.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 30, 2, true, true, true)); // Str III
            caster.playSound(caster.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
            particle(caster, Particle.HAPPY_VILLAGER, 10, 0.6, 0.45, 0.6, 0.0);
        } else {
            caster.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 30, 0, true, true, true)); // Weakness I
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.2f);
            particle(caster, Particle.SMOKE, 12, 0.6, 0.45, 0.6, 0.02);
        }
        return true;
    }

    private boolean copycatCopySpecial(Player caster) {
        Player target = findNearestEnemy(caster, 12.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return false;
        }

        SoulCharacter copied = getSelected(target);
        if (copied == null) return false;

        boolean ok = switch (copied) {
            case DEVILS_FROST -> frostHearts(caster);
            case VOICE_OF_THE_SEA -> seaThunderstorm(caster);
            case ARTIFICIAL_GENOCIDE -> genocideShuffle(caster);
            case COSMIC_DESTROYER -> cosmicBlackhole(caster);
            case KING_OF_HEAT -> heatFlamethrower(caster);
            case ARCHANGEL -> archangelInvulnerable(caster);
            case BOUNTY_HUNTER -> bountyInvis(caster);
            case BLOODY_MONARCH -> monarchBeam(caster, true);
            default -> false;
        };

        if (ok) {
            particle(caster, Particle.ENCHANT, 25, 0.8, 0.6, 0.8, 0.0);
            caster.playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.6f);
        }
        return ok;
    }

    private boolean monarchBeam(Player caster, boolean mega) {
        long now = System.currentTimeMillis();
        long last = monarchLastBeamAt.getOrDefault(caster.getUniqueId(), 0L);
        if (now - last < 200) return false; // tiny debounce for spammy ray traces
        monarchLastBeamAt.put(caster.getUniqueId(), now);

        double range = mega ? 22.0 : 18.0;
        var world = caster.getWorld();
        var start = caster.getEyeLocation();
        Vector dir = start.getDirection().normalize();

        // 2x2 (big) beam hitbox.
        RayTraceResult hit = world.rayTraceEntities(start, dir, range, 1.0, ent -> ent instanceof Player p
                && p != caster
                && p.isOnline()
                && isInSouls(p)
                && sameContext(caster, p));

        Location end = start.clone().add(dir.clone().multiply(range));
        Player target = null;
        if (hit != null) {
            end = hit.getHitPosition() != null
                    ? new Location(world, hit.getHitPosition().getX(), hit.getHitPosition().getY(), hit.getHitPosition().getZ())
                    : end;
            if (hit.getHitEntity() instanceof Player p) target = p;
        }

        // Beam particles along the path (vanilla red dust).
        int steps = (int) Math.max(6, range * 2);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Location p = start.clone().add(dir.clone().multiply(range * t));
            Particle.DustOptions dust = new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 0, 0), mega ? 4.5f : 3.5f);
            world.spawnParticle(Particle.DUST, p, mega ? 8 : 6, 0.12, 0.12, 0.12, 0.0, dust);
            world.spawnParticle(Particle.DRIPPING_DRIPSTONE_LAVA, p, mega ? 2 : 1, 0.05, 0.05, 0.05, 0.0);
        }

        caster.playSound(caster.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, mega ? 0.6f : 0.9f);

        if (target != null) {
            target.setNoDamageTicks(0);
            double dmg = mega ? 16.0 : 3.0; // 8 hearts vs 1.5 hearts
            target.damage(dmg, caster);
            particleAt(target.getLocation().clone().add(0, 1.0, 0), Particle.DRIPPING_DRIPSTONE_LAVA, mega ? 18 : 10, 0.5, 0.35, 0.5, 0.0);

            // Stronger knockback for base beam.
            if (!mega) {
                Vector kb = target.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(1.25);
                kb.setY(0.35);
                target.setVelocity(kb);
            } else {
                // Mega beam: explode/break ground at the hit location.
                Location center = target.getLocation().clone();
                int r = 5;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -2; dy <= 1; dy++) {
                        for (int dz = -r; dz <= r; dz++) {
                            Location l = center.clone().add(dx, dy, dz);
                            if (l.distanceSquared(center) > (r * r + 1)) continue;
                            var block = l.getBlock();
                            if (block.isEmpty()) continue;
                            Material t = block.getType();
                            if (t == Material.BEDROCK || t == Material.BARRIER) continue;
                            if (t.name().contains("CHEST")) continue;
                            block.setType(Material.AIR, false);
                        }
                    }
                }
                particleAt(center.clone().add(0, 1.0, 0), Particle.SMOKE, 85, 1.8, 0.6, 1.8, 0.05);
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.75f);
            }
        }

        return true;
    }

    private long remaining(Player player, String key, long cooldownMs, long now) {
        long last = cooldowns.getOrDefault(player.getUniqueId() + ":" + key, 0L);
        return (last + cooldownMs) - now;
    }
}
