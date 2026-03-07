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
        Scoreboard board = player.getScoreboard();
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        Objective obj = board.getObjective("craftmen");
        if (obj == null) {
            obj = board.registerNewObjective("craftmen", "dummy", "§b§lCRAFTMEN");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        Objective health = board.getObjective("health");
        if (health == null) {
            health = board.registerNewObjective("health", Criteria.HEALTH, "§c❤");
            health.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }

        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);

        update(player);
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        player.setScoreboard(mainBoard);
    }

    public void update(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) return;

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);

        board.getTeams().stream()
                .filter(team -> team.getName().startsWith("sb_"))
                .forEach(Team::unregister);

        Objective obj = board.getObjective("craftmen");
        if (obj == null) return;

        addTeamLine(board, "sb_line1", " "); // spacer
        addTeamLine(board, "sb_wins", "§7Wins: §f" + profile.getWins());
        addTeamLine(board, "sb_losses", "§7Losses: §f" + profile.getLosses());
        addTeamLine(board, "sb_line2", "  "); // another spacer
        addTeamLine(board, "sb_footer", "§bplay.craftmen.net");
    }

    private void addTeamLine(Scoreboard board, String teamName, String text) {
        Team team = board.registerNewTeam(teamName);
        String entry = "§" + (char) ('0' + board.getTeams().size());
        team.addEntry(entry);
        team.setPrefix(text);

        Objective obj = board.getObjective("craftmen");
        if (obj != null) obj.getScore(entry).setScore(0);
    }
}