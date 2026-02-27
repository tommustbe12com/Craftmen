package com.tommustbe12.craftmen;

import com.tommustbe12.craftmen.arena.ArenaManager;
import com.tommustbe12.craftmen.command.CheckStatusCommand;
import com.tommustbe12.craftmen.command.HubCommand;
import com.tommustbe12.craftmen.game.GameManager;
import com.tommustbe12.craftmen.game.impl.*;
import com.tommustbe12.craftmen.hub.HubManager;
import com.tommustbe12.craftmen.listener.*;
import com.tommustbe12.craftmen.command.SetupArenaCommand;
import com.tommustbe12.craftmen.match.MatchManager;
import com.tommustbe12.craftmen.profile.Profile;
import com.tommustbe12.craftmen.profile.ProfileManager;
import com.tommustbe12.craftmen.queue.QueueManager;
import com.tommustbe12.craftmen.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class Craftmen extends JavaPlugin {

    private static Craftmen instance;

    private Location hubLocation;

    private ProfileManager profileManager;
    private GameManager gameManager;
    private ArenaManager arenaManager;
    private QueueManager queueManager;
    private MatchManager matchManager;
    private ScoreboardManager scoreboardManager;

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

        // Register example game
        gameManager.registerGame(new BoxingGame());
        gameManager.registerGame(new ComboGame());
        gameManager.registerGame(new GappleGame());
        gameManager.registerGame(new SwordGame());
        gameManager.registerGame(new AxeGame());

        arenaManager.loadArenas();

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new CombatListener(), this);
        getServer().getPluginManager().registerEvents(new HubManager(), this);
        getServer().getPluginManager().registerEvents(new MovementLockListener(), this);
        getServer().getPluginManager().registerEvents(new RegenListener(), this);
        getServer().getPluginManager().registerEvents(new HungerListener(), this);

        getCommand("setuparena").setExecutor(new SetupArenaCommand());
        getCommand("checkstatus").setExecutor(new CheckStatusCommand());
        getCommand("hub").setExecutor(new HubCommand());

        saveDefaultConfig();
        loadProfiles();

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                scoreboardManager.update(player);
            }
        }, 20L, 20L);
    }

    @Override
    public void onDisable() {
        saveProfiles();
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

    public void loadProfiles() {

        if (!getConfig().contains("stats")) return;

        for (String uuidString : getConfig().getConfigurationSection("stats").getKeys(false)) {

            UUID uuid = UUID.fromString(uuidString);

            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            Profile profile = getProfileManager().getProfile(player);

            profile.setWins(getConfig().getInt("stats." + uuidString + ".wins"));
            profile.setLosses(getConfig().getInt("stats." + uuidString + ".losses"));
            profile.setDeaths(getConfig().getInt("stats." + uuidString + ".deaths"));
        }
    }

    public void saveProfiles() {

        for (Profile profile : getProfileManager().getProfiles().values()) {

            String uuid = profile.getPlayer().getUniqueId().toString();
            String path = "stats." + uuid;

            getConfig().set(path + ".wins", profile.getWins());
            getConfig().set(path + ".losses", profile.getLosses());
            getConfig().set(path + ".deaths", profile.getDeaths());
        }

        saveConfig();
    }

    public void saveProfile(Profile profile) {

        String uuid = profile.getPlayer().getUniqueId().toString();
        String path = "stats." + uuid;

        getConfig().set(path + ".wins", profile.getWins());
        getConfig().set(path + ".losses", profile.getLosses());
        getConfig().set(path + ".deaths", profile.getDeaths());

        saveConfig();
    }
}