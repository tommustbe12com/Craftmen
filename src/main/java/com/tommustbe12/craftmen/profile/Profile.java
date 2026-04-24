package com.tommustbe12.craftmen.profile;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class Profile {

    private final Player player;
    private PlayerState state;

    private int wins;
    private int losses;

    private int ffaKills;
    private int ffaDeaths;

    private java.util.UUID selectedBadgeId;

    private int gems;
    private int endWins;

    // streaks (duels/matches)
    private int killsInARow;
    private int lossesInARow;

    // one-time claim tracking
    private final Set<java.util.UUID> claimedBadgeRewards = new HashSet<>();
    private final Set<Integer> claimedWinStreakRewards = new HashSet<>();

    private final Set<String> purchasedCosmetics = new HashSet<>();
    private String selectedChatColor; // legacy & code like "&b"

    private String selectedNameColor; // & color like "&f" (default white)
    private String selectedNameStyles; // concatenated style codes, e.g. "&l&o" (no &k)

    private String selectedKillEffect; // e.g. "kill.lightning"
    private String selectedDeathEffect; // e.g. "death.explosion"
    private String selectedKillSound; // e.g. "ENTITY_PLAYER_LEVELUP"
    private String selectedDeathSound; // e.g. "ENTITY_WITHER_DEATH"

    private final Map<String, Integer> gameWins = new HashMap<>();
    private final Map<String, Integer> gameLosses = new HashMap<>();

    private String lastPlayedGame;

    // transient selection (not persisted) for Souls mode
    private com.tommustbe12.craftmen.souls.SoulCharacter selectedSoulCharacter;

    public Profile(Player player) {
        this.player = player;
        this.state = PlayerState.LOBBY;
    }

    public Player getPlayer() { return player; }

    public PlayerState getState() { return state; }
    public void setState(PlayerState state) { this.state = state; }

    public int getWins() { return wins; }
    public void addWin(String gameName) {
        this.wins++;
        gameWins.merge(gameName, 1, Integer::sum);
        // win streak
        this.killsInARow++;
        this.lossesInARow = 0;
    }

    public int getLosses() { return losses; }
    public void addLoss(String gameName) {
        this.losses++;
        gameLosses.merge(gameName, 1, Integer::sum);
        // loss streak
        this.lossesInARow++;
        this.killsInARow = 0;
    }

    public int getFfaKills() { return ffaKills; }
    public void addFfaKill() { this.ffaKills++; }
    public void setFfaKills(int ffaKills) { this.ffaKills = ffaKills; }

    public int getFfaDeaths() { return ffaDeaths; }
    public void addFfaDeath() { this.ffaDeaths++; }
    public void setFfaDeaths(int ffaDeaths) { this.ffaDeaths = ffaDeaths; }

    public java.util.UUID getSelectedBadgeId() { return selectedBadgeId; }
    public void setSelectedBadgeId(java.util.UUID selectedBadgeId) { this.selectedBadgeId = selectedBadgeId; }

    public int getGems() { return gems; }
    public void addGems(int amount) { this.gems += Math.max(0, amount); }
    public void setGems(int gems) { this.gems = Math.max(0, gems); }

    public int getEndWins() { return endWins; }
    public void addEndWin() { this.endWins++; }
    public void setEndWins(int endWins) { this.endWins = Math.max(0, endWins); }

    public int getKillsInARow() { return killsInARow; }
    public void setKillsInARow(int killsInARow) { this.killsInARow = Math.max(0, killsInARow); }

    public int getLossesInARow() { return lossesInARow; }
    public void setLossesInARow(int lossesInARow) { this.lossesInARow = Math.max(0, lossesInARow); }

    public Set<java.util.UUID> getClaimedBadgeRewards() { return claimedBadgeRewards; }
    public boolean hasClaimedBadgeReward(java.util.UUID badgeId) { return claimedBadgeRewards.contains(badgeId); }
    public void claimBadgeReward(java.util.UUID badgeId) { if (badgeId != null) claimedBadgeRewards.add(badgeId); }

    public Set<Integer> getClaimedWinStreakRewards() { return claimedWinStreakRewards; }
    public boolean hasClaimedWinStreakReward(int streak) { return claimedWinStreakRewards.contains(streak); }
    public void claimWinStreakReward(int streak) { claimedWinStreakRewards.add(streak); }

    public Set<String> getPurchasedCosmetics() { return purchasedCosmetics; }
    public boolean hasCosmetic(String id) { return id != null && purchasedCosmetics.contains(id); }
    public void addCosmetic(String id) { if (id != null) purchasedCosmetics.add(id); }

    public String getSelectedChatColor() { return selectedChatColor; }
    public void setSelectedChatColor(String selectedChatColor) { this.selectedChatColor = selectedChatColor; }

    public String getSelectedNameColor() { return selectedNameColor; }
    public void setSelectedNameColor(String selectedNameColor) { this.selectedNameColor = selectedNameColor; }

    public String getSelectedNameStyles() { return selectedNameStyles; }
    public void setSelectedNameStyles(String selectedNameStyles) { this.selectedNameStyles = selectedNameStyles; }

    public String getSelectedKillEffect() { return selectedKillEffect; }
    public void setSelectedKillEffect(String selectedKillEffect) { this.selectedKillEffect = selectedKillEffect; }

    public String getSelectedDeathEffect() { return selectedDeathEffect; }
    public void setSelectedDeathEffect(String selectedDeathEffect) { this.selectedDeathEffect = selectedDeathEffect; }

    public String getSelectedKillSound() { return selectedKillSound; }
    public void setSelectedKillSound(String selectedKillSound) { this.selectedKillSound = selectedKillSound; }

    public String getSelectedDeathSound() { return selectedDeathSound; }
    public void setSelectedDeathSound(String selectedDeathSound) { this.selectedDeathSound = selectedDeathSound; }

    public Map<String, Integer> getGameWins() { return gameWins; }
    public Map<String, Integer> getGameLosses() { return gameLosses; }

    public int getGameWins(String gameName) { return gameWins.getOrDefault(gameName, 0); }
    public int getGameLosses(String gameName) { return gameLosses.getOrDefault(gameName, 0); }

    public void setWins(int wins) { this.wins = wins; }
    public void setLosses(int losses) { this.losses = losses; }
    public void setGameWins(String gameName, int value) { gameWins.put(gameName, value); }
    public void setGameLosses(String gameName, int value) { gameLosses.put(gameName, value); }

    public void addStat(String stat, int amount) {
        switch (stat.toLowerCase()) {
            case "wins" -> this.wins += amount;
            case "losses" -> this.losses += amount;
            case "ffa_kill", "ffa_kills" -> this.ffaKills += amount;
            case "ffa_death", "ffa_deaths" -> this.ffaDeaths += amount;
            case "gems" -> this.gems += amount;
            case "endwins", "end_wins" -> this.endWins += amount;
        }
    }

    public void setStat(String stat, int value) {
        switch (stat.toLowerCase()) {
            case "wins" -> this.wins = value;
            case "losses" -> this.losses = value;
            case "ffa_kill", "ffa_kills" -> this.ffaKills = value;
            case "ffa_death", "ffa_deaths" -> this.ffaDeaths = value;
            case "gems" -> this.gems = Math.max(0, value);
            case "endwins", "end_wins" -> this.endWins = Math.max(0, value);
        }
    }

    public String getLastPlayedGame() {
        return lastPlayedGame != null ? lastPlayedGame : "None";
    }

    public void setLastPlayedGame(String gameName) {
        this.lastPlayedGame = gameName;
    }

    public com.tommustbe12.craftmen.souls.SoulCharacter getSelectedSoulCharacter() {
        return selectedSoulCharacter;
    }

    public void setSelectedSoulCharacter(com.tommustbe12.craftmen.souls.SoulCharacter selectedSoulCharacter) {
        this.selectedSoulCharacter = selectedSoulCharacter;
    }
}
