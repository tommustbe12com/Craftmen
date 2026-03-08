package com.tommustbe12.craftmen;

import com.tommustbe12.craftmen.arena.ArenaManager;
import com.tommustbe12.craftmen.command.*;
import com.tommustbe12.craftmen.endfight.EndFightListener;
import com.tommustbe12.craftmen.endfight.EndFightManager;
import com.tommustbe12.craftmen.game.GameManager;
import com.tommustbe12.craftmen.game.impl.*;
import com.tommustbe12.craftmen.hub.HubManager;
import com.tommustbe12.craftmen.listener.*;
import com.tommustbe12.craftmen.match.MatchManager;
import com.tommustbe12.craftmen.profile.Profile;
import com.tommustbe12.craftmen.profile.ProfileManager;
import com.tommustbe12.craftmen.queue.QueueManager;
import com.tommustbe12.craftmen.scoreboard.ScoreboardManager;
import com.tommustbe12.craftmen.web.ExposeData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

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

    @Override
    public void onEnable() {
        instance = this;

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

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new CombatListener(), this);
        getServer().getPluginManager().registerEvents(hubManager, this);
        getServer().getPluginManager().registerEvents(new MovementLockListener(), this);
        getServer().getPluginManager().registerEvents(new RegenListener(), this);
        getServer().getPluginManager().registerEvents(new HungerListener(), this);
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new EndFightListener(endFightManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDropListener(), this);

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

    public void saveProfiles() {
        for (Profile profile : getProfileManager().getProfiles().values()) {
            String uuid = profile.getPlayer().getUniqueId().toString();
            String path = "stats." + uuid;

            getConfig().set(path + ".wins", profile.getWins());
            getConfig().set(path + ".losses", profile.getLosses());

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
            String path = "stats." + uuidString;

            profile.setWins(getConfig().getInt(path + ".wins"));
            profile.setLosses(getConfig().getInt(path + ".losses"));

            // load per-game wins/losses
            if (getConfig().contains(path + ".gameWins")) {
                for (String game : getConfig().getConfigurationSection(path + ".gameWins").getKeys(false)) {
                    profile.setGameWins(game, getConfig().getInt(path + ".gameWins." + game));
                }
            }
            if (getConfig().contains(path + ".gameLosses")) {
                for (String game : getConfig().getConfigurationSection(path + ".gameLosses").getKeys(false)) {
                    profile.setGameLosses(game, getConfig().getInt(path + ".gameLosses." + game));
                }
            }
        }
    }

    public void saveProfile(Profile profile) {
        String uuid = profile.getPlayer().getUniqueId().toString();
        String path = "stats." + uuid;

        getConfig().set(path + ".wins", profile.getWins());
        getConfig().set(path + ".losses", profile.getLosses());

        for (Map.Entry<String, Integer> entry : profile.getGameWins().entrySet()) {
            getConfig().set(path + ".gameWins." + entry.getKey().replace(" ", "_"), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : profile.getGameLosses().entrySet()) {
            getConfig().set(path + ".gameLosses." + entry.getKey().replace(" ", "_"), entry.getValue());
        }

        saveConfig();
    }
}