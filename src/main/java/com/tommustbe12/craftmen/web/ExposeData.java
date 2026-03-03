package com.tommustbe12.craftmen.web;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.util.UUID;

public class ExposeData extends NanoHTTPD {

    private final JavaPlugin plugin;
    private final String API_KEY = "S3pzBol3hr3Ca39K4laK_356kaB";

    public ExposeData(JavaPlugin plugin, int port) {
        super(port);
        this.plugin = plugin;
    }

    public void startServer() {
        try {
            start(SOCKET_READ_TIMEOUT, false);
            plugin.getLogger().info("Craftmen Web API started on port " + getListeningPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // converts URL key (Netherite_Sword) to game name (Netherite Sword)
    private String urlKeyToGameName(String key) {
        return key.replace("_", " ");
    }

    // builds a fresh JSONObject for a player's stats from live profile + config fallback
    private JSONObject buildPlayerStats(OfflinePlayer offlinePlayer) {
        JSONObject json = new JSONObject();
        json.put("name", offlinePlayer.getName());
        json.put("uuid", offlinePlayer.getUniqueId().toString());
        json.put("online", offlinePlayer.isOnline());
        json.put("firstPlayed", offlinePlayer.getFirstPlayed());
        json.put("lastPlayed", offlinePlayer.getLastPlayed());

        Player onlinePlayer = offlinePlayer.getPlayer();
        if (onlinePlayer != null) {
            json.put("health", onlinePlayer.getHealth());
            json.put("level", onlinePlayer.getLevel());
            json.put("world", onlinePlayer.getWorld().getName());

            // always re-fetch profile live
            Profile profile = Craftmen.get().getProfileManager().getProfile(onlinePlayer);
            if (profile != null) {
                json.put("wins", profile.getWins());
                json.put("losses", profile.getLosses());
                json.put("gameWins", buildGameStatsJson(profile.getGameWins()));
                json.put("gameLosses", buildGameStatsJson(profile.getGameLosses()));
            }
        } else {
            // offline — read from config (always re-read, not cached)
            String uuidStr = offlinePlayer.getUniqueId().toString();
            String path = "stats." + uuidStr;
            Craftmen.get().reloadConfig(); // fresh read
            if (Craftmen.get().getConfig().contains(path)) {
                json.put("wins", Craftmen.get().getConfig().getInt(path + ".wins"));
                json.put("losses", Craftmen.get().getConfig().getInt(path + ".losses"));
                json.put("gameWins", buildGameStatsFromConfig(path + ".gameWins"));
                json.put("gameLosses", buildGameStatsFromConfig(path + ".gameLosses"));
            }
        }

        return json;
    }

    // builds a JSONObject from a Map<String, Integer>, converting game names to URL-safe keys
    private JSONObject buildGameStatsJson(java.util.Map<String, Integer> map) {
        JSONObject obj = new JSONObject();
        for (java.util.Map.Entry<String, Integer> entry : map.entrySet()) {
            obj.put(entry.getKey().replace(" ", "_"), entry.getValue());
        }
        return obj;
    }

    // reads game stats from config, converting keys to URL-safe
    private JSONObject buildGameStatsFromConfig(String path) {
        JSONObject obj = new JSONObject();
        if (Craftmen.get().getConfig().contains(path)) {
            for (String game : Craftmen.get().getConfig().getConfigurationSection(path).getKeys(false)) {
                obj.put(game.replace(" ", "_"), Craftmen.get().getConfig().getInt(path + "." + game));
            }
        }
        return obj;
    }

    @Override
    public Response serve(IHTTPSession session) {

        String key = session.getHeaders().get("api-key");
        if (key == null || !key.equals(API_KEY)) {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED,
                    "application/json",
                    "{\"error\":\"Unauthorized\"}");
        }

        String uri = session.getUri();

        // GET /player/<name>
        if (uri.startsWith("/player/")) {
            String name = uri.replace("/player/", "");
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);

            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND,
                        "application/json",
                        "{\"error\":\"Player not found\"}");
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json",
                    buildPlayerStats(offlinePlayer).toJSONString());
        }

        // GET /player/<name>/game/<Game_Name>
        if (uri.matches("/player/[^/]+/game/[^/]+")) {
            String[] parts = uri.split("/");
            String playerName = parts[2];
            String gameName = urlKeyToGameName(parts[4]);

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND,
                        "application/json", "{\"error\":\"Player not found\"}");
            }

            JSONObject json = new JSONObject();
            json.put("name", offlinePlayer.getName());
            json.put("game", gameName);

            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null) {
                Profile profile = Craftmen.get().getProfileManager().getProfile(onlinePlayer);
                if (profile != null) {
                    json.put("wins", profile.getGameWins(gameName));
                    json.put("losses", profile.getGameLosses(gameName));
                }
            } else {
                Craftmen.get().reloadConfig();
                String path = "stats." + offlinePlayer.getUniqueId();
                json.put("wins", Craftmen.get().getConfig().getInt(path + ".gameWins." + gameName, 0));
                json.put("losses", Craftmen.get().getConfig().getInt(path + ".gameLosses." + gameName, 0));
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toJSONString());
        }

        // GET /online
        if (uri.equalsIgnoreCase("/online")) {
            JSONObject json = new JSONObject();
            json.put("count", Bukkit.getOnlinePlayers().size());

            JSONArray players = new JSONArray();
            for (Player p : Bukkit.getOnlinePlayers()) {
                players.add(buildPlayerStats(p));
            }
            json.put("players", players);

            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toJSONString());
        }

        // GET /leaderboard
        if (uri.equalsIgnoreCase("/leaderboard")) {
            Craftmen.get().reloadConfig(); // fresh read for offline players
            JSONArray arr = new JSONArray();

            if (Craftmen.get().getConfig().contains("stats")) {
                Craftmen.get().getConfig().getConfigurationSection("stats").getKeys(false)
                        .stream()
                        .map(uuidStr -> {
                            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));

                            // if online, use live profile
                            Player online = op.getPlayer();
                            if (online != null) {
                                return buildPlayerStats(online);
                            }

                            // offline fallback
                            return buildPlayerStats(op);
                        })
                        .sorted((a, b) -> {
                            int winsA = ((Number) a.get("wins")).intValue();
                            int winsB = ((Number) b.get("wins")).intValue();
                            return Integer.compare(winsB, winsA);
                        })
                        .forEach(arr::add);
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json", arr.toJSONString());
        }

        // GET /leaderboard/game/<Game_Name>
        if (uri.startsWith("/leaderboard/game/")) {
            String gameName = urlKeyToGameName(uri.replace("/leaderboard/game/", ""));
            Craftmen.get().reloadConfig();
            JSONArray arr = new JSONArray();

            if (Craftmen.get().getConfig().contains("stats")) {
                Craftmen.get().getConfig().getConfigurationSection("stats").getKeys(false)
                        .stream()
                        .map(uuidStr -> {
                            JSONObject entry = new JSONObject();
                            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                            entry.put("name", op.getName());
                            entry.put("uuid", uuidStr);
                            entry.put("game", gameName);

                            Player online = op.getPlayer();
                            if (online != null) {
                                Profile profile = Craftmen.get().getProfileManager().getProfile(online);
                                entry.put("wins", profile != null ? profile.getGameWins(gameName) : 0);
                                entry.put("losses", profile != null ? profile.getGameLosses(gameName) : 0);
                            } else {
                                String path = "stats." + uuidStr;
                                entry.put("wins", Craftmen.get().getConfig().getInt(path + ".gameWins." + gameName, 0));
                                entry.put("losses", Craftmen.get().getConfig().getInt(path + ".gameLosses." + gameName, 0));
                            }
                            return entry;
                        })
                        .sorted((a, b) -> {
                            int winsA = ((Number) a.get("wins")).intValue();
                            int winsB = ((Number) b.get("wins")).intValue();
                            return Integer.compare(winsB, winsA);
                        })
                        .forEach(arr::add);
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json", arr.toJSONString());
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND,
                "application/json",
                "{\"error\":\"Invalid endpoint\"}");
    }
}