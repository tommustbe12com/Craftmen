package com.tommustbe12.craftmen.souls;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
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
    private final Map<UUID, ItemStack> seaTridentRestore = new HashMap<>();

    private BukkitTask actionbarTask;
    private BukkitTask passiveTask;

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
        if (c != SoulCharacter.GOOP) {
            setCooldown(player, "special", System.currentTimeMillis());
        }
    }

    public void applySoulLoadout(Player player) {
        if (player == null) return;

        // shard always in slot 0, and locked by listeners
        player.getInventory().setItem(0, SoulsItems.shardOfSoul());

        SoulCharacter c = getSelected(player);
        if (c == null) c = SoulCharacter.GOOP;

        switch (c) {
            case GOOP -> {
                // passive handled in task
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 60 * 30, 1, true, false, false));
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
                ItemStack trident = new ItemStack(org.bukkit.Material.TRIDENT);
                ItemMeta meta = trident.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§bRiptide Trident");
                    meta.setUnbreakable(true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.RIPTIDE, 3, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS, org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                    trident.setItemMeta(meta);
                }
                player.getInventory().setItem(1, trident);
            }
        }
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
                if (!tryUseCooldown(player, "goop1", BASE_COOLDOWN_MS)) return;
                if (player.isSneaking()) {
                    goopLaunchSelf(player);
                } else {
                    goopBounce(player);
                }
                player.sendActionBar("§aUsed [1]");
            } else {
                if (!tryUseCooldown(player, "goop2", GOOP_RIGHT_CLICK_COOLDOWN_MS)) return;
                goopFreeze(player);
                player.sendActionBar("§bUsed [2]");
            }
            return;
        }

        if (left) {
            useBase(player);
            player.sendActionBar("§aUsed [1]");
        } else {
            useSpecial(player);
            player.sendActionBar("§bUsed [2]");
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

        // Shard of Soul cannot leave the hotbar (slots 0-8). Allow rearranging within hotbar only.
        int slot = e.getSlot();
        boolean playerInv = e.getClickedInventory() != null && e.getClickedInventory().getType() == org.bukkit.event.inventory.InventoryType.PLAYER;
        boolean hotbarSlot = playerInv && slot >= 0 && slot <= 8;

        if (!hotbarSlot || e.isShiftClick()) {
            e.setCancelled(true);
            return;
        }
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

        // Only allow placing shard into hotbar slots (0-8) within the player inventory.
        for (int rawSlot : e.getRawSlots()) {
            // Player inventory hotbar raw slots are 0-8 when the top inventory is the player inventory view.
            if (rawSlot < 0 || rawSlot > 8) {
                e.setCancelled(true);
                return;
            }
        }
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
    }

    private void useBase(Player player) {
        SoulCharacter c = getSelected(player);
        if (c == null) c = SoulCharacter.GOOP;

        // Goop is handled by click handler (2 base abilities, no special).
        if (c == SoulCharacter.GOOP) return;

        if (!tryUseCooldown(player, "base", BASE_COOLDOWN_MS)) return;

        switch (c) {
            case DEVILS_FROST -> frostStun(player);
            case VOICE_OF_THE_SEA -> seaRiptideBoost(player);
            default -> {}
        }
    }

    private void useSpecial(Player player) {
        if (!tryUseCooldown(player, "special", SPECIAL_COOLDOWN_MS)) return;

        SoulCharacter c = getSelected(player);
        if (c == null) c = SoulCharacter.GOOP;

        switch (c) {
            case DEVILS_FROST -> frostHearts(player);
            case VOICE_OF_THE_SEA -> seaThunderstorm(player);
            default -> player.sendMessage(ChatColor.RED + "No special ability for this soul yet.");
        }
    }

    private void goopBounce(Player caster) {
        Player target = findNearestEnemy(caster, 8.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return;
        }
        Vector away = target.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize();
        target.setVelocity(away.multiply(1.6).setY(0.35));
        caster.playSound(caster.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0f, 1.0f);
        target.playSound(target.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0f, 0.8f);
    }

    private void goopFreeze(Player caster) {
        Player target = findNearestEnemy(caster, 8.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return;
        }
        freeze(target, 2_000L);
        caster.playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        target.playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
    }

    private void frostStun(Player caster) {
        Player target = findNearestEnemy(caster, 8.0);
        if (target == null) {
            caster.playSound(caster.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            caster.sendMessage(ChatColor.RED + "No target in range.");
            return;
        }
        freeze(target, 3_000L);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 3, 4, true, false, false));
        caster.playSound(caster.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
    }

    private void frostHearts(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        originalMaxHealth.putIfAbsent(player.getUniqueId(), attr.getBaseValue());

        double original = originalMaxHealth.get(player.getUniqueId());
        attr.setBaseValue(original + 10.0);
        player.setHealth(Math.min(player.getHealth() + 10.0, attr.getBaseValue()));
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

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
    }

    private void seaRiptideBoost(Player player) {
        Vector dir = player.getLocation().getDirection().normalize();
        // Faster/farther dash that can go upward based on look direction.
        Vector vel = dir.multiply(2.4);
        vel.setY(Math.max(dir.getY() * 1.4, 0.25));
        player.setVelocity(vel);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.1f);
    }

    private void seaThunderstorm(Player player) {
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

        // Give caster a Channeling trident for the duration (restore previous slot after).
        if (!seaTridentRestore.containsKey(id)) {
            seaTridentRestore.put(id, player.getInventory().getItem(1));
            ItemStack trident = new ItemStack(org.bukkit.Material.TRIDENT);
            ItemMeta meta = trident.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§bChanneling Trident");
                meta.setUnbreakable(true);
                meta.addEnchant(org.bukkit.enchantments.Enchantment.CHANNELING, 1, true);
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
                meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS, org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                trident.setItemMeta(meta);
            }
            player.getInventory().setItem(1, trident);
        }

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

            // Restore slot 1 item
            ItemStack restore = seaTridentRestore.remove(id);
            if (player.isOnline()) {
                player.getInventory().setItem(1, restore);
                player.updateInventory();
            }
        }, 20L * 20L);
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

    private boolean tryUseCooldown(Player player, String key, long cooldownMs) {
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
        cooldowns.put(player.getUniqueId() + ":" + key, now);
        return true;
    }

    private void setCooldown(Player player, String key, long atMillis) {
        cooldowns.put(player.getUniqueId() + ":" + key, atMillis);
    }

    private void startTasks() {
        actionbarTask = Bukkit.getScheduler().runTaskTimer(Craftmen.get(), this::tickActionbar, 10L, 10L);
        passiveTask = Bukkit.getScheduler().runTaskTimer(Craftmen.get(), this::tickPassives, 20L, 20L);
    }

    private void goopLaunchSelf(Player player) {
        if (player == null) return;
        Vector dir = player.getLocation().getDirection().normalize();
        Vector vel = dir.multiply(1.8);
        vel.setY(Math.max(dir.getY(), 0.15) + 0.25);
        player.setVelocity(vel);
        player.playSound(player.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0f, 1.2f);
    }

    private void tickActionbar() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isInSouls(player)) continue;
            ItemStack main = player.getInventory().getItemInMainHand();
            if (!SoulsItems.isShardOfSoul(main)) continue;

            long now = System.currentTimeMillis();
            SoulCharacter c = getSelected(player);
            if (c == null) c = SoulCharacter.GOOP;

            long baseRemaining = remaining(player, (c == SoulCharacter.GOOP) ? "goop1" : "base", BASE_COOLDOWN_MS, now);
            long slot2Remaining = (c == SoulCharacter.GOOP)
                    ? remaining(player, "goop2", GOOP_RIGHT_CLICK_COOLDOWN_MS, now)
                    : remaining(player, "special", SPECIAL_COOLDOWN_MS, now);

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
                }
                case VOICE_OF_THE_SEA -> {
                    if (player.getWorld().hasStorm()) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false, false));
                    }
                }
            }
        }
    }

    private long remaining(Player player, String key, long cooldownMs, long now) {
        long last = cooldowns.getOrDefault(player.getUniqueId() + ":" + key, 0L);
        return (last + cooldownMs) - now;
    }
}

