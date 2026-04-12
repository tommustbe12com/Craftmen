package com.tommustbe12.craftmen;

import com.tommustbe12.craftmen.arena.ArenaManager;
import com.tommustbe12.craftmen.badge.BadgeManager;
import com.tommustbe12.craftmen.badge.listener.BadgeChatListener;
import com.tommustbe12.craftmen.badge.command.BadgeAdminCommand;
import com.tommustbe12.craftmen.badge.command.BadgesCommand;
import com.tommustbe12.craftmen.command.*;
import com.tommustbe12.craftmen.customkit.CustomKitManager;
import com.tommustbe12.craftmen.ffa.FfaManager;
import com.tommustbe12.craftmen.ffa.command.FfaCommand;
import com.tommustbe12.craftmen.endfight.EndFightListener;
import com.tommustbe12.craftmen.endfight.EndFightManager;
import com.tommustbe12.craftmen.game.GameManager;
import com.tommustbe12.craftmen.game.impl.*;
import com.tommustbe12.craftmen.gems.GemManager;
import com.tommustbe12.craftmen.gems.command.GemsCommand;
import com.tommustbe12.craftmen.cosmetics.command.ShopCommand;
import com.tommustbe12.craftmen.cosmetics.gui.ShopMenu;
import com.tommustbe12.craftmen.cosmetics.listener.ChatColorListener;
import com.tommustbe12.craftmen.cosmetics.listener.KillDeathCosmeticsListener;
import com.tommustbe12.craftmen.hub.HubManager;
import com.tommustbe12.craftmen.hub.SpawnProtectionListener;
import com.tommustbe12.craftmen.kit.KitManager;
import com.tommustbe12.craftmen.kit.KitStorage;
import com.tommustbe12.craftmen.kit.command.KitCommand;
import com.tommustbe12.craftmen.kit.gui.KitEditorMenu;
import com.tommustbe12.craftmen.kit.gui.KitEditorShortcutListener;
import com.tommustbe12.craftmen.trim.ArmorTrimManager;
import com.tommustbe12.craftmen.trim.ArmorTrimStorage;
import com.tommustbe12.craftmen.trim.command.TrimsCommand;
import com.tommustbe12.craftmen.trim.gui.ArmorTrimMenu;
import com.tommustbe12.craftmen.listener.*;
import com.tommustbe12.craftmen.match.MatchManager;
import com.tommustbe12.craftmen.profile.Profile;
import com.tommustbe12.craftmen.profile.ProfileManager;
import com.tommustbe12.craftmen.party.PartyManager;
import com.tommustbe12.craftmen.party.PartyChatManager;
import com.tommustbe12.craftmen.party.command.PartyCommand;
import com.tommustbe12.craftmen.party.command.PartyChatCommand;
import com.tommustbe12.craftmen.party.command.PartyChatTabCompleter;
import com.tommustbe12.craftmen.party.command.PartyTabCompleter;
import com.tommustbe12.craftmen.party.ffa.PartyFfaMenu;
import com.tommustbe12.craftmen.party.listener.PartyChatListener;
import com.tommustbe12.craftmen.party.listener.PartyPlayerListener;
import com.tommustbe12.craftmen.queue.QueueManager;
import com.tommustbe12.craftmen.scoreboard.ScoreboardManager;
import com.tommustbe12.craftmen.web.ExposeData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public final class Craftmen extends JavaPlugin {

    private static Craftmen instance;

    private ExposeData webServer;

    private Location hubLocation;

    private ProfileManager profileManager;
    private GameManager gameManager;
    private ArenaManager arenaManager;
    private QueueManager queueManager;
    private MatchManager matchManager;
    private ScoreboardManager scoreboardManager;
    private HubManager hubManager;
    private EndFightManager endFightManager;
    private KitManager kitManager;
    private KitEditorMenu kitEditorMenu;
    private ArmorTrimManager armorTrimManager;
    private ArmorTrimMenu armorTrimMenu;
    private CustomKitManager customKitManager;
    private FfaManager ffaManager;
    private BadgeManager badgeManager;
    private PartyManager partyManager;
    private PartyChatManager partyChatManager;
    private PartyFfaMenu partyFfaMenu;
    private GemManager gemManager;
    private ShopMenu shopMenu;

    @Override
    public void onEnable() {
        instance = this;

        deleteEndFightWorlds();

        for (Handler handler : Bukkit.getLogger().getParent().getHandlers()) {
            handler.setFilter((LogRecord record) -> {
                String msg = record.getMessage();

                if (msg != null && (
                        msg.contains("Failed to save level ./endfight_") ||
                                msg.contains("NoSuchFileException: ./endfight_")
                )) {
                    return false; // block this log
                }

                return true;
            });
        }

        hubLocation = Bukkit.getWorld("world").getSpawnLocation();

        saveDefaultConfig();

        profileManager = new ProfileManager();
        gameManager = new GameManager();
        arenaManager = new ArenaManager();
        matchManager = new MatchManager();
        queueManager = new QueueManager();
        scoreboardManager = new ScoreboardManager();
        hubManager = new HubManager();
        endFightManager = new EndFightManager(this);
        kitManager = new KitManager(new KitStorage(this));
        kitEditorMenu = new KitEditorMenu(kitManager);
        armorTrimManager = new ArmorTrimManager(new ArmorTrimStorage(this));
        armorTrimMenu = new ArmorTrimMenu(armorTrimManager);
        customKitManager = new CustomKitManager(this);
        ffaManager = new FfaManager(this);
        badgeManager = new BadgeManager(this);
        partyManager = new PartyManager();
        partyChatManager = new PartyChatManager();
        partyFfaMenu = new PartyFfaMenu();
        gemManager = new GemManager();
        shopMenu = new ShopMenu();

        gameManager.registerGame(new BoxingGame());
        gameManager.registerGame(new ComboGame());
        gameManager.registerGame(new GappleGame());
        gameManager.registerGame(new SwordGame());
        gameManager.registerGame(new AxeGame());
        gameManager.registerGame(new SumoGame());
        gameManager.registerGame(new InvisGame());
        gameManager.registerGame(new NetheriteSwordGame());
        gameManager.registerGame(new PotionFightGame());
        gameManager.registerGame(new RandomKitGame());
        gameManager.registerGame(new UHCGame());
        gameManager.registerGame(new SMPGame());
        gameManager.registerGame(new MaceGame());
        gameManager.registerGame(new DPotionGame());
        gameManager.registerGame(new DSMPGame());
        gameManager.registerGame(new PotionGame());
        gameManager.registerGame(new CrystalGame());
        gameManager.registerGame(new SpleefGame());

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new CombatListener(), this);
        getServer().getPluginManager().registerEvents(hubManager, this);
        getServer().getPluginManager().registerEvents(customKitManager, this);
        getServer().getPluginManager().registerEvents(ffaManager, this);
        getServer().getPluginManager().registerEvents(badgeManager, this);
        getServer().getPluginManager().registerEvents(new BadgeChatListener(), this);
        getServer().getPluginManager().registerEvents(new MovementLockListener(), this);
        getServer().getPluginManager().registerEvents(new RegenListener(), this);
        getServer().getPluginManager().registerEvents(new HungerListener(), this);
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new EndFightListener(endFightManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDropListener(), this);
        getServer().getPluginManager().registerEvents(new EndermiteSpawnListener(), this);
        getServer().getPluginManager().registerEvents(new CountdownBlockListener(), this);
        getServer().getPluginManager().registerEvents(new SnowballHitListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnProtectionListener(), this);
        getServer().getPluginManager().registerEvents(kitEditorMenu, this);
        getServer().getPluginManager().registerEvents(new KitEditorShortcutListener(), this);
        getServer().getPluginManager().registerEvents(armorTrimMenu, this);
        getServer().getPluginManager().registerEvents(new PartyChatListener(), this);
        getServer().getPluginManager().registerEvents(new PartyPlayerListener(), this);
        getServer().getPluginManager().registerEvents(partyFfaMenu, this);
        getServer().getPluginManager().registerEvents(shopMenu, this);
        getServer().getPluginManager().registerEvents(new ChatColorListener(), this);
        getServer().getPluginManager().registerEvents(new KillDeathCosmeticsListener(), this);

        getCommand("checkstatus").setExecutor(new CheckStatusCommand());
        getCommand("hub").setExecutor(new HubCommand());
        getCommand("duel").setExecutor(new DuelCommand());
        getCommand("accept").setExecutor(new AcceptCommand());
        getCommand("leavequeue").setExecutor(new LeaveQueueCommand());
        getCommand("stat").setExecutor(new StatCommand());
        getCommand("forfeit").setExecutor(new ForfeitCommand());
        getCommand("queue").setExecutor(new QueueCommand());
        getCommand("spectate").setExecutor(new SpectateCommand());
        getCommand("endfight").setExecutor(new EndFightCommand(endFightManager));
        KitCommand kitCommand = new KitCommand(kitEditorMenu);
        getCommand("kit").setExecutor(kitCommand);
        getCommand("kit").setTabCompleter(kitCommand);
        getCommand("trims").setExecutor(new TrimsCommand());
        getCommand("ffa").setExecutor(new FfaCommand());
        getCommand("badges").setExecutor(new BadgesCommand());
        getCommand("badgeadmin").setExecutor(new BadgeAdminCommand());
        getCommand("party").setExecutor(new PartyCommand());
        getCommand("party").setTabCompleter(new PartyTabCompleter());
        getCommand("pc").setExecutor(new PartyChatCommand());
        getCommand("pc").setTabCompleter(new PartyChatTabCompleter());
        getCommand("gems").setExecutor(new GemsCommand());
        getCommand("shop").setExecutor(new ShopCommand());

        getCommand("stat").setTabCompleter(new StatCommand());

        saveDefaultConfig();
        loadProfiles();

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                scoreboardManager.update(player);
            }
        }, 20L, 20L);

        webServer = new ExposeData(this, 7512);
        webServer.startServer();
    }

    @Override
    public void onDisable() {
        saveProfiles();
        if (kitManager != null) kitManager.flushAll();
        if (armorTrimManager != null) armorTrimManager.flushAll();
        if (webServer != null) webServer.stop();
    }

    public Location getHubLocation() {
        return hubLocation;
    }

    public static Craftmen get() { return instance; }

    public ProfileManager getProfileManager() { return profileManager; }
    public GameManager getGameManager() { return gameManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public MatchManager getMatchManager() { return matchManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public HubManager getHubManager() { return hubManager; }
    public EndFightManager getEndFightManager() { return endFightManager; }
    public KitManager getKitManager() { return kitManager; }
    public KitEditorMenu getKitEditorMenu() { return kitEditorMenu; }
    public ArmorTrimManager getArmorTrimManager() { return armorTrimManager; }
    public ArmorTrimMenu getArmorTrimMenu() { return armorTrimMenu; }
    public CustomKitManager getCustomKitManager() { return customKitManager; }
    public FfaManager getFfaManager() { return ffaManager; }
    public BadgeManager getBadgeManager() { return badgeManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public PartyChatManager getPartyChatManager() { return partyChatManager; }
    public PartyFfaMenu getPartyFfaMenu() { return partyFfaMenu; }
    public GemManager getGemManager() { return gemManager; }
    public ShopMenu getShopMenu() { return shopMenu; }

    public void saveProfiles() {
        for (Profile profile : getProfileManager().getProfiles().values()) {
            String uuid = profile.getPlayer().getUniqueId().toString();
            String path = "stats." + uuid;

            getConfig().set(path + ".wins", profile.getWins());
            getConfig().set(path + ".losses", profile.getLosses());
            getConfig().set(path + ".last", profile.getLastPlayedGame());
            getConfig().set(path + ".ffa_kills", profile.getFfaKills());
            getConfig().set(path + ".ffa_deaths", profile.getFfaDeaths());
            getConfig().set(path + ".badge", profile.getSelectedBadgeId() == null ? null : profile.getSelectedBadgeId().toString());
            getConfig().set(path + ".gems", profile.getGems());
            getConfig().set(path + ".end_wins", profile.getEndWins());
            getConfig().set(path + ".kills_in_a_row", profile.getKillsInARow());
            getConfig().set(path + ".losses_in_a_row", profile.getLossesInARow());
            getConfig().set(path + ".claimed_badge_rewards", profile.getClaimedBadgeRewards().stream().map(UUID::toString).toList());
            getConfig().set(path + ".claimed_win_streak_rewards", profile.getClaimedWinStreakRewards().stream().toList());
            getConfig().set(path + ".purchased_cosmetics", profile.getPurchasedCosmetics().stream().toList());
            getConfig().set(path + ".selected_chat_color", profile.getSelectedChatColor());
            getConfig().set(path + ".selected_kill_effect", profile.getSelectedKillEffect());
            getConfig().set(path + ".selected_death_effect", profile.getSelectedDeathEffect());
            getConfig().set(path + ".selected_kill_sound", profile.getSelectedKillSound());
            getConfig().set(path + ".selected_death_sound", profile.getSelectedDeathSound());

            // save per-game wins/losses
            for (Map.Entry<String, Integer> entry : profile.getGameWins().entrySet()) {
                getConfig().set(path + ".gameWins." + entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Integer> entry : profile.getGameLosses().entrySet()) {
                getConfig().set(path + ".gameLosses." + entry.getKey(), entry.getValue());
            }
        }
        saveConfig();
    }

    public void loadProfiles() {
        if (!getConfig().contains("stats")) return;

        for (String uuidString : getConfig().getConfigurationSection("stats").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            Profile profile = getProfileManager().getProfile(player);
            loadProfile(profile);

            // load per-game wins/losses
            // (already handled by loadProfile)
        }
    }

    public void loadProfile(Profile profile) {
        if (profile == null || profile.getPlayer() == null) return;
        String uuidString = profile.getPlayer().getUniqueId().toString();
        String path = "stats." + uuidString;
        if (!getConfig().contains(path)) return;

        profile.setWins(getConfig().getInt(path + ".wins"));
        profile.setLosses(getConfig().getInt(path + ".losses"));
        profile.setLastPlayedGame(getConfig().getString(path + ".last"));
        profile.setFfaKills(getConfig().getInt(path + ".ffa_kills", 0));
        profile.setFfaDeaths(getConfig().getInt(path + ".ffa_deaths", 0));
        profile.setGems(getConfig().getInt(path + ".gems", 0));
        profile.setEndWins(getConfig().getInt(path + ".end_wins", 0));
        profile.setKillsInARow(getConfig().getInt(path + ".kills_in_a_row", 0));
        profile.setLossesInARow(getConfig().getInt(path + ".losses_in_a_row", 0));

        String badgeRaw = getConfig().getString(path + ".badge");
        if (badgeRaw != null) {
            try {
                profile.setSelectedBadgeId(UUID.fromString(badgeRaw));
            } catch (IllegalArgumentException ignored) {
                profile.setSelectedBadgeId(null);
            }
        }

        profile.getClaimedBadgeRewards().clear();
        for (String idRaw : getConfig().getStringList(path + ".claimed_badge_rewards")) {
            try { profile.getClaimedBadgeRewards().add(UUID.fromString(idRaw)); } catch (IllegalArgumentException ignored) {}
        }

        profile.getClaimedWinStreakRewards().clear();
        for (Object o : getConfig().getList(path + ".claimed_win_streak_rewards", java.util.List.of())) {
            if (o instanceof Integer i) profile.getClaimedWinStreakRewards().add(i);
            else if (o instanceof String s) {
                try { profile.getClaimedWinStreakRewards().add(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
            }
        }

        profile.getPurchasedCosmetics().clear();
        profile.getPurchasedCosmetics().addAll(getConfig().getStringList(path + ".purchased_cosmetics"));
        profile.setSelectedChatColor(getConfig().getString(path + ".selected_chat_color"));
        profile.setSelectedKillEffect(getConfig().getString(path + ".selected_kill_effect"));
        profile.setSelectedDeathEffect(getConfig().getString(path + ".selected_death_effect"));
        profile.setSelectedKillSound(getConfig().getString(path + ".selected_kill_sound"));
        profile.setSelectedDeathSound(getConfig().getString(path + ".selected_death_sound"));

        // per-game wins/losses
        if (getConfig().contains(path + ".gameWins")) {
            for (String game : getConfig().getConfigurationSection(path + ".gameWins").getKeys(false)) {
                profile.setGameWins(game.replace("_", " "), getConfig().getInt(path + ".gameWins." + game));
            }
        }
        if (getConfig().contains(path + ".gameLosses")) {
            for (String game : getConfig().getConfigurationSection(path + ".gameLosses").getKeys(false)) {
                profile.setGameLosses(game.replace("_", " "), getConfig().getInt(path + ".gameLosses." + game));
            }
        }
    }

    public void saveProfile(Profile profile) {
        String uuid = profile.getPlayer().getUniqueId().toString();
        String path = "stats." + uuid;

        getConfig().set(path + ".wins", profile.getWins());
        getConfig().set(path + ".losses", profile.getLosses());
        getConfig().set(path + ".last", profile.getLastPlayedGame());
        getConfig().set(path + ".ffa_kills", profile.getFfaKills());
        getConfig().set(path + ".ffa_deaths", profile.getFfaDeaths());
        getConfig().set(path + ".badge", profile.getSelectedBadgeId() == null ? null : profile.getSelectedBadgeId().toString());
        getConfig().set(path + ".gems", profile.getGems());
        getConfig().set(path + ".end_wins", profile.getEndWins());
        getConfig().set(path + ".kills_in_a_row", profile.getKillsInARow());
        getConfig().set(path + ".losses_in_a_row", profile.getLossesInARow());
        getConfig().set(path + ".claimed_badge_rewards", profile.getClaimedBadgeRewards().stream().map(UUID::toString).toList());
        getConfig().set(path + ".claimed_win_streak_rewards", profile.getClaimedWinStreakRewards().stream().toList());
        getConfig().set(path + ".purchased_cosmetics", profile.getPurchasedCosmetics().stream().toList());
        getConfig().set(path + ".selected_chat_color", profile.getSelectedChatColor());
        getConfig().set(path + ".selected_kill_effect", profile.getSelectedKillEffect());
        getConfig().set(path + ".selected_death_effect", profile.getSelectedDeathEffect());
        getConfig().set(path + ".selected_kill_sound", profile.getSelectedKillSound());
        getConfig().set(path + ".selected_death_sound", profile.getSelectedDeathSound());

        for (Map.Entry<String, Integer> entry : profile.getGameWins().entrySet()) {
            getConfig().set(path + ".gameWins." + entry.getKey().replace(" ", "_"), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : profile.getGameLosses().entrySet()) {
            getConfig().set(path + ".gameLosses." + entry.getKey().replace(" ", "_"), entry.getValue());
        }

        saveConfig();
    }

    private void deleteEndFightWorlds() {
        File serverDir = Bukkit.getWorldContainer(); // server root directory
        File[] files = serverDir.listFiles();

        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory() && file.getName().startsWith("endfight_")) {
                deleteDirectory(file);
                getLogger().info("Deleted world folder: " + file.getName());
            }
        }
    }

    private void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteDirectory(f);
                }
            }
        }
        file.delete();
    }
}
