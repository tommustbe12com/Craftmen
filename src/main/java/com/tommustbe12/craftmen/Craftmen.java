package com.tommustbe12.craftmen;

import com.tommustbe12.craftmen.arena.ArenaManager;
import com.tommustbe12.craftmen.game.GameManager;
import com.tommustbe12.craftmen.game.impl.BoxingGame;
import com.tommustbe12.craftmen.listener.CombatListener;
import com.tommustbe12.craftmen.listener.PlayerListener;
import com.tommustbe12.craftmen.command.SetupArenaCommand;
import com.tommustbe12.craftmen.match.MatchManager;
import com.tommustbe12.craftmen.profile.ProfileManager;
import com.tommustbe12.craftmen.queue.QueueManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Craftmen extends JavaPlugin {

    private static Craftmen instance;

    private ProfileManager profileManager;
    private GameManager gameManager;
    private ArenaManager arenaManager;
    private QueueManager queueManager;
    private MatchManager matchManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        profileManager = new ProfileManager();
        gameManager = new GameManager();
        arenaManager = new ArenaManager();
        matchManager = new MatchManager();
        queueManager = new QueueManager();

        // Register example game
        gameManager.registerGame(new BoxingGame());

        arenaManager.loadArenas();

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new CombatListener(), this);

        getCommand("setuparena").setExecutor(new SetupArenaCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Craftmen get() { return instance; }

    public ProfileManager getProfileManager() { return profileManager; }
    public GameManager getGameManager() { return gameManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public MatchManager getMatchManager() { return matchManager; }
}