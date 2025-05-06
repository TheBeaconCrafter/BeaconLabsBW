package org.bcnlab.beaconLabsBW.utils;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.scoreboard.Criteria;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the in-game scoreboard for BedWars
 */
public class GameScoreboard {
    
    private final BeaconLabsBW plugin;
    private final Game game;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private BukkitTask updateTask;
    
    public GameScoreboard(BeaconLabsBW plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }
    
    /**
     * Set up the scoreboard for a player
     * 
     * @param player The player
     */
    public void setupScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
          Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("bedwars", Criteria.DUMMY, 
                ChatColor.YELLOW + "" + ChatColor.BOLD + "BED WARS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        // Add entries
        updateScoreboard(player, scoreboard, objective);
        
        // Apply scoreboard to player
        player.setScoreboard(scoreboard);
        playerScoreboards.put(player.getUniqueId(), scoreboard);
    }
    
    /**
     * Update the scoreboard for a player
     * 
     * @param player The player
     * @param scoreboard The player's scoreboard
     * @param objective The objective
     */
    private void updateScoreboard(Player player, Scoreboard scoreboard, Objective objective) {
        // Clear previous scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        
        // Date display
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        String date = dateFormat.format(new Date());
        
        // Add new scores
        int score = 15;
        
        // Header
        objective.getScore(ChatColor.GRAY + date).setScore(score--);
        objective.getScore(" ").setScore(score--);
        
        // Game info
        int minutes = game.getGameTimer() / 60;
        int seconds = game.getGameTimer() % 60;
        String timeLeft = String.format("%02d:%02d", minutes, seconds);
        objective.getScore(ChatColor.WHITE + "Time Left: " + ChatColor.GREEN + timeLeft).setScore(score--);
        
        // Team status
        objective.getScore("  ").setScore(score--);
        objective.getScore(ChatColor.WHITE + "Teams:").setScore(score--);
        
        // Show all teams and their status
        for (String teamName : game.getArena().getTeams().keySet()) {
            boolean bedAlive = game.getBedStatus().getOrDefault(teamName, false);
            boolean teamAlive = !game.getTeams().getOrDefault(teamName, java.util.Collections.emptySet()).isEmpty();
            
            String teamColorCode = getTeamColorCode(teamName);
            String status;
            
            if (teamAlive) {
                status = bedAlive ? teamColorCode + "✓" : teamColorCode + "✗";
            } else {
                status = ChatColor.GRAY + "☠";
            }
            
            objective.getScore(teamColorCode + teamName + ": " + status).setScore(score--);
        }
        
        objective.getScore("   ").setScore(score--);
        
        // Player stats
        String playerTeam = game.getPlayerTeam(player);
        if (playerTeam != null) {
            objective.getScore(ChatColor.WHITE + "Your Team: " + 
                    getTeamColorCode(playerTeam) + playerTeam).setScore(score--);
        }
        
        int kills = game.getPlayerKills().getOrDefault(player.getUniqueId(), 0);
        objective.getScore(ChatColor.WHITE + "Kills: " + ChatColor.GREEN + kills).setScore(score--);
        
        int deaths = game.getPlayerDeaths().getOrDefault(player.getUniqueId(), 0);
        objective.getScore(ChatColor.WHITE + "Deaths: " + ChatColor.RED + deaths).setScore(score);
    }
    
    /**
     * Get the color code for a team name
     * 
     * @param teamName The team name
     * @return The ChatColor code
     */
    private String getTeamColorCode(String teamName) {
        if (teamName == null) return ChatColor.WHITE.toString();
        
        return switch (game.getArena().getTeam(teamName).getColor().toUpperCase()) {
            case "RED" -> ChatColor.RED.toString();
            case "BLUE" -> ChatColor.BLUE.toString();
            case "GREEN" -> ChatColor.GREEN.toString();
            case "YELLOW" -> ChatColor.YELLOW.toString();
            case "AQUA" -> ChatColor.AQUA.toString();
            case "WHITE" -> ChatColor.WHITE.toString();
            case "PINK" -> ChatColor.LIGHT_PURPLE.toString();
            case "GRAY" -> ChatColor.GRAY.toString();
            default -> ChatColor.WHITE.toString();
        };
    }
      /**
     * Start the scoreboard update task
     */
    public void startTask() {
        // Set up health displays initially
        setupPlayerHealthDisplays();
        
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Update individual scoreboards
            for (UUID playerId : game.getPlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                Scoreboard scoreboard = playerScoreboards.get(playerId);
                
                if (player != null && scoreboard != null) {
                    Objective objective = scoreboard.getObjective("bedwars");
                    if (objective != null) {
                        updateScoreboard(player, scoreboard, objective);
                    }
                }
            }
            
            // Update player health displays
            updatePlayerHealthDisplays();
        }, 20L, 10L); // Update every half second for more responsive health display
    }
    
    /**
     * Stop the scoreboard update task
     */
    public void stopTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }
    
    /**
     * Update the tab list for players
     */
    public void updateTabList() {
        // Create the team objects for all teams
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // Clear any existing teams with the same name first
        for (String teamName : game.getArena().getTeams().keySet()) {
            Team scoreboardTeam = mainScoreboard.getTeam("bw_" + teamName);
            if (scoreboardTeam != null) {
                scoreboardTeam.unregister();
            }
            
            scoreboardTeam = mainScoreboard.registerNewTeam("bw_" + teamName);
            org.bukkit.ChatColor teamColor = getTeamChatColor(teamName);
            scoreboardTeam.setColor(teamColor);
            scoreboardTeam.setPrefix(teamColor + "[" + teamName + "] ");
            
            // Add players to team
            for (UUID playerId : game.getTeams().getOrDefault(teamName, java.util.Collections.emptySet())) {
                Player teamPlayer = Bukkit.getPlayer(playerId);
                if (teamPlayer != null) {
                    scoreboardTeam.addEntry(teamPlayer.getName());
                }
            }
        }
    }
    
    /**
     * Get the ChatColor for a team
     * 
     * @param teamName The team name
     * @return The ChatColor
     */
    private ChatColor getTeamChatColor(String teamName) {
        if (teamName == null) return ChatColor.WHITE;
        
        return switch (game.getArena().getTeam(teamName).getColor().toUpperCase()) {
            case "RED" -> ChatColor.RED;
            case "BLUE" -> ChatColor.BLUE;
            case "GREEN" -> ChatColor.GREEN;
            case "YELLOW" -> ChatColor.YELLOW;
            case "AQUA" -> ChatColor.AQUA;
            case "WHITE" -> ChatColor.WHITE;
            case "PINK" -> ChatColor.LIGHT_PURPLE;
            case "GRAY" -> ChatColor.GRAY;
            default -> ChatColor.WHITE;
        };
    }
    
    /**
     * Clean up the scoreboard
     */
    public void cleanup() {
        stopTask();
        playerScoreboards.clear();
        
        // Unregister teams from the main scoreboard
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (String teamName : game.getArena().getTeams().keySet()) {
            Team scoreboardTeam = mainScoreboard.getTeam("bw_" + teamName);
            if (scoreboardTeam != null) {
                scoreboardTeam.unregister();
            }
        }
    }
    
    /**
     * Set up the health display below players' names
     */
    public void setupPlayerHealthDisplays() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        
        // Set up health objective for all players in the game
        Scoreboard mainScoreboard = manager.getMainScoreboard();
        
        // Remove any existing health objective first
        Objective healthObj = mainScoreboard.getObjective("health");
        if (healthObj != null) {
            healthObj.unregister();
        }
          // Create new health objective using non-deprecated method
        healthObj = mainScoreboard.registerNewObjective("health", Criteria.HEALTH, ChatColor.RED + "❤");
        healthObj.setDisplaySlot(DisplaySlot.BELOW_NAME);
        
        // Set up health for all players in the game
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                healthObj.getScore(player.getName()).setScore((int) Math.ceil(player.getHealth()));
            }
        }
    }
    
    /**
     * Update player health displays
     */
    public void updatePlayerHealthDisplays() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        
        Scoreboard mainScoreboard = manager.getMainScoreboard();
        Objective healthObj = mainScoreboard.getObjective("health");
        
        if (healthObj != null) {
            for (UUID playerId : game.getPlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    healthObj.getScore(player.getName()).setScore((int) Math.ceil(player.getHealth()));
                }
            }
        }
    }
}
