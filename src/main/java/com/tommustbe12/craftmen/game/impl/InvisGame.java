package com.tommustbe12.craftmen.game.impl;

import com.tommustbe12.craftmen.game.Game;
import com.tommustbe12.craftmen.kit.Kit;
import com.tommustbe12.craftmen.match.Match;
import com.tommustbe12.craftmen.Craftmen;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InvisGame extends Game {

    public InvisGame() {
        super("Invis", createInvisPotion());
    }

    private static ItemStack createInvisPotion() {
        ItemStack potion = new ItemStack(Material.POTION);

        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

        if (potionMeta != null) {
            potionMeta.setBasePotionType(PotionType.INVISIBILITY);

            potion.setItemMeta(potionMeta);
        }

        return potion;
    }

    private Enchantment ench(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    private ItemStack createArmor(Material material, int protLevel) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(ench("protection"), protLevel, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Kit createDefaultKit() {
        ItemStack[] contents = new ItemStack[36];
        contents[0] = new ItemStack(Material.DIAMOND_SWORD);
        return new Kit(contents, new ItemStack[4], null);
    }

    @Override
    protected void afterLoadoutApplied(Player player) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        ensureInvisWithReveal(player);
    }

    @Override
    public void onStart(Match match) {
        ensureInvisWithReveal(match.getP1());
        ensureInvisWithReveal(match.getP2());
    }

    @Override
    public void onEnd(Match match) {
        removeInvis(match.getP1());
        removeInvis(match.getP2());
    }

    private final Map<UUID, BukkitTask> revealTasks = new HashMap<>();

    private void ensureInvisWithReveal(Player player) {
        if (player == null) return;

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.INVISIBILITY,
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false
                )
        );

        // Every 10 seconds, briefly reveal (remove invis) for 0.5s so players can locate each other.
        revealTasks.computeIfAbsent(player.getUniqueId(), id ->
                org.bukkit.Bukkit.getScheduler().runTaskTimer(Craftmen.get(), () -> {
                    if (!player.isOnline()) {
                        BukkitTask t = revealTasks.remove(id);
                        if (t != null) t.cancel();
                        return;
                    }

                    boolean inInvisMatch = false;
                    Match match = Craftmen.get().getMatchManager().getMatch(player);
                    if (match != null && match.getGame() != null && "Invis".equalsIgnoreCase(match.getGame().getName())) {
                        inInvisMatch = true;
                    }
                    boolean inInvisFfa = false;
                    var ffaGame = Craftmen.get().getFfaManager().getGame(player);
                    if (ffaGame != null && "Invis".equalsIgnoreCase(ffaGame.getName())) {
                        inInvisFfa = true;
                    }

                    if (!inInvisMatch && !inInvisFfa) {
                        BukkitTask t = revealTasks.remove(id);
                        if (t != null) t.cancel();
                        player.removePotionEffect(PotionEffectType.INVISIBILITY);
                        return;
                    }

                    // Reveal now, then reapply shortly after.
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    org.bukkit.Bukkit.getScheduler().runTaskLater(Craftmen.get(), () -> {
                        if (!player.isOnline()) return;
                        var currentMatch = Craftmen.get().getMatchManager().getMatch(player);
                        var currentFfa = Craftmen.get().getFfaManager().getGame(player);
                        boolean stillInInvis = (currentMatch != null && currentMatch.getGame() != null && "Invis".equalsIgnoreCase(currentMatch.getGame().getName()))
                                || (currentFfa != null && "Invis".equalsIgnoreCase(currentFfa.getName()));
                        if (stillInInvis) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                        }
                    }, 10L);
                }, 20L * 10L, 20L * 10L)
        );
    }

    private void removeInvis(Player player) {
        if (player == null) return;
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        BukkitTask task = revealTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }
}
