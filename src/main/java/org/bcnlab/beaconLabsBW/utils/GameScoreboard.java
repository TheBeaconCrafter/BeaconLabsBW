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
        
        // If we're in the lobby and haven't started the task yet, start it now
        if ((game.getState() == org.bcnlab.beaconLabsBW.game.GameState.WAITING || 
             game.getState() == org.bcnlab.beaconLabsBW.game.GameState.STARTING) && 
            updateTask == null) {
            startTask();
        }
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
        
        // Check game state and display appropriate scoreboard
        if (game.getState() == org.bcnlab.beaconLabsBW.game.GameState.WAITING || 
            game.getState() == org.bcnlab.beaconLabsBW.game.GameState.STARTING) {
            // Display the lobby scoreboard
            updateLobbyScoreboard(player, scoreboard, objective);
        } else {
            // Display the in-game scoreboard
            updateInGameScoreboard(player, scoreboard, objective);
        }
    }
    
    /**
     * Update the lobby scoreboard for a player
     * 
     * @param player The player
     * @param scoreboard The player's scoreboard
     * @param objective The objective
     */
    private void updateLobbyScoreboard(Player player, Scoreboard scoreboard, Objective objective) {
        // Date display
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        String date = dateFormat.format(new Date());
        
        // Add new scores
        int score = 15;
        
        // Header
        objective.getScore(ChatColor.GRAY + date).setScore(score--);
        objective.getScore(" ").setScore(score--);
        
        // Game info
        objective.getScore(ChatColor.WHITE + "Map: " + ChatColor.GREEN + game.getArena().getName()).setScore(score--);
        objective.getScore("  ").setScore(score--);
        
        // Player count
        int minPlayers = plugin.getConfigManager().getMinPlayers();
        String playersText = ChatColor.WHITE + "Players: " + ChatColor.GREEN + game.getPlayers().size() + 
                ChatColor.GRAY + "/" + game.getArena().getMaxPlayers();
        objective.getScore(playersText).setScore(score--);
        
        // Show countdown if game is starting
        if (game.getState() == org.bcnlab.beaconLabsBW.game.GameState.STARTING) {
            objective.getScore("   ").setScore(score--);
            objective.getScore(ChatColor.WHITE + "Game starting in: " + 
                    ChatColor.GREEN + game.getCountdown() + "s").setScore(score--);
        } else {
            objective.getScore("   ").setScore(score--);
            objective.getScore(ChatColor.WHITE + "Waiting for " + 
                    ChatColor.GREEN + Math.max(0, minPlayers - game.getPlayers().size()) + 
                    ChatColor.WHITE + " more players").setScore(score--);
        }
        
        // Show server IP
        objective.getScore("    ").setScore(score--);
        objective.getScore(ChatColor.YELLOW + "bcnlab.org").setScore(score);
    }
    
    /**
     * Update the in-game scoreboard for a player
     * 
     * @param player The player
     * @param scoreboard The player's scoreboard
     * @param objective The objective
     */
    private void updateInGameScoreboard(Player player, Scoreboard scoreboard, Objective objective) {
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
    }    /**
     * Start the scoreboard update task
     */
    public void startTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Force update all players' scoreboards to keep player counts current
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
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) { // Check if manager is available
            Scoreboard mainScoreboard = manager.getMainScoreboard();
            if (mainScoreboard != null) { // Check if scoreboard is available
                for (String teamName : game.getArena().getTeams().keySet()) {
                    Team scoreboardTeam = mainScoreboard.getTeam("bw_" + teamName);
                    if (scoreboardTeam != null) {
                        try {
                            scoreboardTeam.unregister();
                        } catch (IllegalStateException e) {
                            // Ignore if team is already unregistered (can happen during shutdown races)
                            plugin.getLogger().fine("Scoreboard team bw_" + teamName + " was already unregistered during cleanup.");
                        } catch (Exception e) {
                             plugin.getLogger().warning("Unexpected error unregistering scoreboard team bw_" + teamName + ": " + e.getMessage());
                        }
                    }
                }
                
                // Also cleanup health display
                 Objective healthObj = mainScoreboard.getObjective("bw_hp");
                if (healthObj != null) {
                    try {
                        healthObj.unregister();
                    } catch (Exception e) {
                         plugin.getLogger().warning("Unexpected error unregistering health objective 'bw_hp': " + e.getMessage());
                    }
                }
                Objective healthObjOld = mainScoreboard.getObjective("bwhealth");
                 if (healthObjOld != null) {
                    try {
                        healthObjOld.unregister();
                    } catch (Exception e) {
                         plugin.getLogger().warning("Unexpected error unregistering health objective 'bwhealth': " + e.getMessage());
                    }
                }
            } else {
                plugin.getLogger().warning("Main scoreboard was null during cleanup.");
            }
        } else {
            plugin.getLogger().warning("ScoreboardManager was null during cleanup (likely during shutdown).");
        }
    }
}
