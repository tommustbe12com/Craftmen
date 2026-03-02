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

                Profile profile = Craftmen.get().getProfileManager().getProfile(onlinePlayer);
                if (profile != null) {
                    json.put("wins", profile.getWins());
                    json.put("losses", profile.getLosses());
                    json.put("gameWins", new JSONObject(profile.getGameWins()));
                    json.put("gameLosses", new JSONObject(profile.getGameLosses()));
                }
            } else {
                String uuidStr = offlinePlayer.getUniqueId().toString();
                String path = "stats." + uuidStr;
                if (Craftmen.get().getConfig().contains(path)) {
                    json.put("wins", Craftmen.get().getConfig().getInt(path + ".wins"));
                    json.put("losses", Craftmen.get().getConfig().getInt(path + ".losses"));

                    JSONObject gameWins = new JSONObject();
                    JSONObject gameLosses = new JSONObject();

                    if (Craftmen.get().getConfig().contains(path + ".gameWins")) {
                        for (String game : Craftmen.get().getConfig().getConfigurationSection(path + ".gameWins").getKeys(false)) {
                            gameWins.put(game, Craftmen.get().getConfig().getInt(path + ".gameWins." + game));
                        }
                    }
                    if (Craftmen.get().getConfig().contains(path + ".gameLosses")) {
                        for (String game : Craftmen.get().getConfig().getConfigurationSection(path + ".gameLosses").getKeys(false)) {
                            gameLosses.put(game, Craftmen.get().getConfig().getInt(path + ".gameLosses." + game));
                        }
                    }

                    json.put("gameWins", gameWins);
                    json.put("gameLosses", gameLosses);
                }
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toJSONString());
        }

        // GET /online
        if (uri.equalsIgnoreCase("/online")) {
            JSONObject json = new JSONObject();
            json.put("count", Bukkit.getOnlinePlayers().size());

            JSONArray players = new JSONArray();
            for (Player p : Bukkit.getOnlinePlayers()) {
                JSONObject pJson = new JSONObject();
                pJson.put("name", p.getName());
                pJson.put("uuid", p.getUniqueId().toString());
                pJson.put("world", p.getWorld().getName());

                Profile profile = Craftmen.get().getProfileManager().getProfile(p);
                if (profile != null) {
                    pJson.put("wins", profile.getWins());
                    pJson.put("losses", profile.getLosses());
                    pJson.put("gameWins", new JSONObject(profile.getGameWins()));
                    pJson.put("gameLosses", new JSONObject(profile.getGameLosses()));
                }
                players.add(pJson);
            }
            json.put("players", players);

            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toJSONString());
        }

        // GET /leaderboard
        if (uri.equalsIgnoreCase("/leaderboard")) {
            JSONArray arr = new JSONArray();

            if (Craftmen.get().getConfig().contains("stats")) {
                Craftmen.get().getConfig().getConfigurationSection("stats").getKeys(false)
                        .stream()
                        .map(uuidStr -> {
                            JSONObject entry = new JSONObject();
                            String path = "stats." + uuidStr;
                            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                            entry.put("name", op.getName());
                            entry.put("uuid", uuidStr);
                            entry.put("wins", Craftmen.get().getConfig().getInt(path + ".wins"));
                            entry.put("losses", Craftmen.get().getConfig().getInt(path + ".losses"));

                            JSONObject gameWins = new JSONObject();
                            JSONObject gameLosses = new JSONObject();

                            if (Craftmen.get().getConfig().contains(path + ".gameWins")) {
                                for (String game : Craftmen.get().getConfig().getConfigurationSection(path + ".gameWins").getKeys(false)) {
                                    gameWins.put(game, Craftmen.get().getConfig().getInt(path + ".gameWins." + game));
                                }
                            }
                            if (Craftmen.get().getConfig().contains(path + ".gameLosses")) {
                                for (String game : Craftmen.get().getConfig().getConfigurationSection(path + ".gameLosses").getKeys(false)) {
                                    gameLosses.put(game, Craftmen.get().getConfig().getInt(path + ".gameLosses." + game));
                                }
                            }

                            entry.put("gameWins", gameWins);
                            entry.put("gameLosses", gameLosses);
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