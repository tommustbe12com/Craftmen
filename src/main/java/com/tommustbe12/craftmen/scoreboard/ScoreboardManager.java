package com.tommustbe12.craftmen.scoreboard;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public void create(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        // ===== SIDEBAR OBJECTIVE =====
        Objective obj = board.registerNewObjective("craftmen", "dummy", "§b§lCRAFTMEN");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // ===== BELOW NAME HEALTH OBJECTIVE =====
        Objective health = board.registerNewObjective(
                "health",
                Criteria.HEALTH,
                "§c❤"
        );
        health.setDisplaySlot(DisplaySlot.BELOW_NAME);

        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);

        update(player);
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void update(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) return;

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);

        // Clear old teams
        for (Team team : board.getTeams()) {
            team.unregister();
        }

        Objective obj = board.getObjective("craftmen");
        if (obj == null) return;

        // Create lines using teams (no numbers show)
        addTeamLine(board, "line1", " "); // empty spacer
        addTeamLine(board, "wins", "§7Wins: §f" + profile.getWins());
        addTeamLine(board, "losses", "§7Losses: §f" + profile.getLosses());
        addTeamLine(board, "line2", "  "); // another spacer
        addTeamLine(board, "footer", "§bplay.craftmen.net");
    }

    private void addTeamLine(Scoreboard board, String teamName, String text) {
        Team team = board.registerNewTeam(teamName);
        String entry = "§" + (char) (0x1 + board.getTeams().size()); // unique dummy entry
        team.addEntry(entry);
        team.setPrefix(text);

        Objective obj = board.getObjective("craftmen");
        if (obj != null) obj.getScore(entry).setScore(0); // score is ignored visually
    }
}