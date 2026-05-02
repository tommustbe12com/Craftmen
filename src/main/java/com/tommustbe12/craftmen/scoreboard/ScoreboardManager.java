package com.tommustbe12.craftmen.scoreboard;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Locale;

public class ScoreboardManager {

    private static final String SIDEBAR_OBJECTIVE = "cm_sb";
    private static final String HEALTH_OBJECTIVE = "cm_hp";
    private static final String SIDEBAR_TITLE = "\u00A7b\u00A7lCRAFTMEN";
    private static final String HEALTH_TITLE = "\u00A7c\u2764";
    private static final String TEAM_PREFIX = "cm_";

    public void create(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard current = player.getScoreboard();
        Scoreboard board = current;
        Scoreboard main = manager.getMainScoreboard();

        // If player is still on the shared main scoreboard, move them to a personal board
        // and copy existing teams so rank/nametag plugins continue to work.
        if (board == null || board == main) {
            board = manager.getNewScoreboard();
            copyTeams(main, board);
            player.setScoreboard(board);
        }

        ensureSidebarObjective(board);
        ensureHealthObjective(board);
        forceHealthLine(board, player);
        update(player);
    }

    public void remove(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == null) return;

        // Main scoreboard is shared globally, so do not unregister objectives there on player quit.
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) return;

        clearFromBoard(board);
    }

    public void update(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = player.getScoreboard();
        if (board == null) return;

        // Never write Craftmen scoreboard data onto the shared main scoreboard.
        if (board == manager.getMainScoreboard()) {
            create(player);
            board = player.getScoreboard();
            if (board == null || board == manager.getMainScoreboard()) return;
        }

        Objective sidebar = ensureSidebarObjective(board);
        if (sidebar == null) return;

        ensureHealthObjective(board);
        forceHealthLine(board, player);

        // Hide & Seek: keep nametags hidden by ensuring the hs_hidden team exists on this player's board.
        if (Craftmen.get().getHideSeekManager() != null && Craftmen.get().getHideSeekManager().isInGame(player)) {
            Craftmen.get().getHideSeekManager().applyNametagHidingFor(player);
        }

        Profile profile = Craftmen.get().getProfileManager().getProfile(player);
        if (profile == null) return;

        int ping = player.getPing();

        setLine(board, sidebar, "line1", " ", 7);
        setLine(board, sidebar, "wins", "\u00A77Wins: \u00A7f" + profile.getWins(), 6);
        setLine(board, sidebar, "losses", "\u00A77Losses: \u00A7f" + profile.getLosses(), 5);
        setLine(board, sidebar, "gems", "\u00A7bGems: \u00A7f" + profile.getGems(), 4);
        setLine(board, sidebar, "ping", "\u00A77Ping: \u00A7f" + ping + "ms", 3);
        setLine(board, sidebar, "line2", "  ", 2);
        setLine(board, sidebar, "footer", "\u00A7bplay.craftmen.net", 1);
    }

    private Objective ensureSidebarObjective(Scoreboard board) {
        Objective ours = board.getObjective(SIDEBAR_OBJECTIVE);
        if (ours == null) {
            ours = board.registerNewObjective(SIDEBAR_OBJECTIVE, Criteria.DUMMY, SIDEBAR_TITLE);
        } else {
            ours.setDisplayName(SIDEBAR_TITLE);
        }

        Objective currentSidebar = board.getObjective(DisplaySlot.SIDEBAR);
        if (currentSidebar == null || SIDEBAR_OBJECTIVE.equals(currentSidebar.getName())) {
            ours.setDisplaySlot(DisplaySlot.SIDEBAR);
            return ours;
        }

        return null;
    }

    private void ensureHealthObjective(Scoreboard board) {
        Objective ours = board.getObjective(HEALTH_OBJECTIVE);
        if (ours == null) {
            ours = board.registerNewObjective(HEALTH_OBJECTIVE, Criteria.HEALTH, HEALTH_TITLE);
        } else {
            ours.setDisplayName(HEALTH_TITLE);
        }

        Objective currentBelow = board.getObjective(DisplaySlot.BELOW_NAME);
        if (currentBelow == null || HEALTH_OBJECTIVE.equals(currentBelow.getName())) {
            ours.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
    }

    private void forceHealthLine(Scoreboard board, Player player) {
        if (board == null || player == null) return;
        Objective health = board.getObjective(HEALTH_OBJECTIVE);
        if (health == null) return;

        // Some setups show 0 until the first damage/regen event. Force an initial score write.
        int hp = (int) Math.ceil(player.getHealth());
        String entry = player.getName();
        if (entry == null) return;
        entry = entry.length() > 40 ? entry.substring(0, 40) : entry;
        try {
            health.getScore(entry).setScore(hp);
        } catch (IllegalArgumentException ignored) {
            // If another plugin uses a different entry scheme, just skip rather than error spam.
        }
    }

    private void clearFromBoard(Scoreboard board) {
        if (board == null) return;

        for (Team team : board.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }

        Objective sidebar = board.getObjective(SIDEBAR_OBJECTIVE);
        if (sidebar != null) sidebar.unregister();

        Objective health = board.getObjective(HEALTH_OBJECTIVE);
        if (health != null) health.unregister();
    }

    private void setLine(Scoreboard board, Objective objective, String key, String text, int score) {
        String teamName = TEAM_PREFIX + key;
        Team team = board.getTeam(teamName);
        String entry;

        if (team == null) {
            team = board.registerNewTeam(teamName);
            entry = "\u00A7" + Integer.toHexString(score);
            team.addEntry(entry);
        } else {
            entry = team.getEntries().stream().findFirst().orElse(null);
            if (entry == null) {
                entry = "\u00A7" + Integer.toHexString(score);
                team.addEntry(entry);
            }
        }

        // Always (re)bind score to guarantee line visibility.
        objective.getScore(entry).setScore(score);
        team.setPrefix(text);
    }

    private void copyTeams(Scoreboard source, Scoreboard target) {
        for (Team sourceTeam : source.getTeams()) {
            if (sourceTeam.getName().startsWith(TEAM_PREFIX)) {
                continue;
            }

            Team targetTeam = target.getTeam(sourceTeam.getName());
            if (targetTeam == null) {
                targetTeam = target.registerNewTeam(sourceTeam.getName());
            }

            targetTeam.setPrefix(sourceTeam.getPrefix());
            targetTeam.setSuffix(sourceTeam.getSuffix());
            targetTeam.setColor(sourceTeam.getColor());
            targetTeam.setCanSeeFriendlyInvisibles(sourceTeam.canSeeFriendlyInvisibles());
            targetTeam.setAllowFriendlyFire(sourceTeam.allowFriendlyFire());

            for (String entry : sourceTeam.getEntries()) {
                targetTeam.addEntry(entry);
            }
        }
    }
}
