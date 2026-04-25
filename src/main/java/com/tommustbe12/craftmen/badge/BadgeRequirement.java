package com.tommustbe12.craftmen.badge;

import com.tommustbe12.craftmen.Craftmen;
import com.tommustbe12.craftmen.profile.Profile;

import java.util.*;

public final class BadgeRequirement {

    enum Op { EQ, GT, GTE, LT, LTE }

    record Clause(String key, Op op, int value) {}

    private final List<Clause> clauses;

    private BadgeRequirement(List<Clause> clauses) {
        this.clauses = clauses;
    }

    public static Optional<BadgeRequirement> parse(String raw) {
        if (raw == null) return Optional.empty();
        String s = raw.trim();
        if (s.isEmpty()) return Optional.empty();

        List<Clause> clauses = new ArrayList<>();
        for (String part : s.split("&")) {
            String p = part.trim();
            if (p.isEmpty()) continue;

            Op op = null;
            String key;
            String valueRaw;

            if (p.contains(">=")) {
                op = Op.GTE;
                String[] a = p.split(">=", 2);
                key = a[0].trim();
                valueRaw = a[1].trim();
            } else if (p.contains("<=")) {
                op = Op.LTE;
                String[] a = p.split("<=", 2);
                key = a[0].trim();
                valueRaw = a[1].trim();
            } else if (p.contains(">")) {
                op = Op.GT;
                String[] a = p.split(">", 2);
                key = a[0].trim();
                valueRaw = a[1].trim();
            } else if (p.contains("<")) {
                op = Op.LT;
                String[] a = p.split("<", 2);
                key = a[0].trim();
                valueRaw = a[1].trim();
            } else if (p.contains("==")) {
                op = Op.EQ;
                String[] a = p.split("==", 2);
                key = a[0].trim();
                valueRaw = a[1].trim();
            } else if (p.contains("=")) {
                op = Op.EQ;
                String[] a = p.split("=", 2);
                key = a[0].trim();
                valueRaw = a[1].trim();
            } else {
                return Optional.empty();
            }

            int value;
            try {
                value = Integer.parseInt(valueRaw);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }

            if (key.isEmpty()) return Optional.empty();
            clauses.add(new Clause(key, op, value));
        }

        if (clauses.isEmpty()) return Optional.empty();
        return Optional.of(new BadgeRequirement(clauses));
    }

    List<Clause> getClauses() {
        return clauses;
    }

    public boolean matches(Profile profile) {
        for (Clause c : clauses) {
            int actual = resolve(profile, c.key);
            if (!compare(actual, c.op, c.value)) return false;
        }
        return true;
    }

    private static boolean compare(int actual, Op op, int value) {
        return switch (op) {
            case EQ -> actual == value;
            case GT -> actual > value;
            case GTE -> actual >= value;
            case LT -> actual < value;
            case LTE -> actual <= value;
        };
    }

    private static int resolve(Profile profile, String keyRaw) {
        String key = keyRaw.trim();
        if (key.isEmpty()) return 0;

        String k = key.toLowerCase(Locale.ROOT);
        if (k.equals("wins")) return profile.getWins();
        if (k.equals("losses")) return profile.getLosses();
        if (k.equals("ffa_kills") || k.equals("ffa_kill")) return profile.getFfaKills();
        if (k.equals("ffa_deaths") || k.equals("ffa_death")) return profile.getFfaDeaths();
        if (k.equals("endwins") || k.equals("end_wins")) return profile.getEndWins();
        if (k.equals("killsinarow") || k.equals("kills_in_a_row")) return profile.getKillsInARow();
        if (k.equals("lossinarow") || k.equals("lossesinarow") || k.equals("losses_in_a_row")) return profile.getLossesInARow();

        if (k.equals("mostkills")) {
            int max = 0;
            for (Profile p : Craftmen.get().getProfileManager().getProfiles().values()) {
                if (p.getFfaKills() > max) max = p.getFfaKills();
            }
            return profile.getFfaKills() >= 1 && profile.getFfaKills() == max ? 1 : 0;
        }

        if (k.equals("winsallgm") || k.equals("wins_all_gm")) {
            // For ">= X wins in EVERY normal queued gamemode", use the minimum wins across all registered games.
            int min = Integer.MAX_VALUE;
            for (var g : Craftmen.get().getGameManager().getGames()) {
                int w = profile.getGameWins(g.getName());
                if (w < min) min = w;
            }
            return min == Integer.MAX_VALUE ? 0 : min;
        }

        if (k.startsWith("game.")) {
            String rest = key.substring(5);
            // supports: game.crystalWins, game.crystalLosses (case-insensitive)
            String restLower = rest.toLowerCase(Locale.ROOT).trim();
            boolean isWins = restLower.endsWith("wins");
            boolean isLosses = restLower.endsWith("losses");
            if (!isWins && !isLosses) return 0;

            String gameKey = rest.substring(0, rest.length() - (isWins ? 4 : 6));
            String normalized = normalizeGameKey(gameKey);
            String gameName = Craftmen.get().getGameManager().getGames().stream()
                    .map(g -> g.getName())
                    .filter(n -> normalizeGameKey(n).equals(normalized))
                    .findFirst()
                    .orElse(null);
            if (gameName == null) return 0;
            return isWins ? profile.getGameWins(gameName) : profile.getGameLosses(gameName);
        }

        return 0;
    }

    static int resolveForDisplay(Profile profile, String keyRaw) {
        return resolve(profile, keyRaw);
    }

    private static String normalizeGameKey(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }
}
