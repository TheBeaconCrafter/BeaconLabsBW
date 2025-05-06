package org.bcnlab.beaconLabsBW.game;

import lombok.Getter;
import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages all BedWars games
 */
public class GameManager {
    
    private final BeaconLabsBW plugin;
    @Getter
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();
    
    private final Map<UUID, Game> playerGameMap = new ConcurrentHashMap<>();
    
    public GameManager(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Attempt to auto-start a game on server boot
     */
    public void attemptAutoStart() {
        plugin.getLogger().info("Attempting to auto-start BedWars game...");
        
        Set<String> arenaNames = plugin.getArenaManager().getArenaNames();
        if (arenaNames.isEmpty()) {
            plugin.getLogger().info("No arenas found. Edit mode activated.");
            return;
        }
        
        // Try to find a configured arena
        Arena arena = null;
        for (String arenaName : arenaNames) {
            Arena candidate = plugin.getArenaManager().getArena(arenaName);
            if (candidate != null && candidate.isConfigured()) {
                arena = candidate;
                break;
            }
        }
        
        if (arena == null) {
            plugin.getLogger().info("No properly configured arenas found. Edit mode activated.");
            return;
        }
        
        startGame(arena);
        plugin.getLogger().info("Auto-started BedWars game with arena: " + arena.getName());
    }
    
    /**
     * Start a new BedWars game
     * 
     * @param arena The arena to use
     * @return The created game, or null if arena is invalid
     */
    public Game startGame(Arena arena) {
        if (arena == null || !arena.isConfigured()) {
            return null;
        }
        
        // Check if there's already a game for this arena
        if (activeGames.containsKey(arena.getName().toLowerCase())) {
            return null;
        }
        
        Game game = new Game(plugin, arena);
        activeGames.put(arena.getName().toLowerCase(), game);
        game.setup();
        
        return game;
    }
    
    /**
     * End a BedWars game
     * 
     * @param game The game to end
     */
    public void endGame(Game game) {
        if (game == null) return;
        
        // Remove game
        activeGames.remove(game.getArena().getName().toLowerCase());
        
        // Remove player mappings
        for (UUID playerId : game.getPlayers()) {
            playerGameMap.remove(playerId);
        }
        
        // Clean up the game
        game.cleanup();
    }
    
    /**
     * Shut down all active games
     */
    public void shutdownAllGames() {
        for (Game game : new ArrayList<>(activeGames.values())) {
            MessageUtils.sendMessage(game.getPlayers(), plugin.getPrefix() + "&cGame ended - Server shutting down");
            endGame(game);
        }
    }
    
    /**
     * Add a player to a game
     * 
     * @param player The player to add
     * @param game The game to add to
     * @return true if added, false otherwise
     */
    public boolean addPlayerToGame(Player player, Game game) {
        if (player == null || game == null) return false;
        
        // Check if player is already in a game
        Game currentGame = getPlayerGame(player);
        if (currentGame != null) {
            return false;
        }
        
        if (game.addPlayer(player)) {
            playerGameMap.put(player.getUniqueId(), game);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get the game a player is in
     * 
     * @param player The player
     * @return The game or null if not in a game
     */
    public Game getPlayerGame(Player player) {
        if (player == null) return null;
        return playerGameMap.get(player.getUniqueId());
    }
    
    /**
     * Remove a player from their current game
     * 
     * @param player The player to remove
     * @return true if removed, false if not in a game
     */
    public boolean removePlayerFromGame(Player player) {
        if (player == null) return false;
        
        Game game = getPlayerGame(player);
        if (game == null) return false;
        
        game.removePlayer(player);
        playerGameMap.remove(player.getUniqueId());
        return true;
    }
    
    /**
     * Choose a game for a player to join
     * 
     * @param player The player
     * @return true if joined, false otherwise
     */
    public boolean joinGame(Player player) {
        if (player == null) return false;
        
        // Check if player is already in a game
        if (getPlayerGame(player) != null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou are already in a game!");
            return false;
        }
        
        // Find available games in lobby state
        List<Game> availableGames = activeGames.values().stream()
            .filter(g -> g.getState() == GameState.WAITING)
            .filter(g -> g.getPlayers().size() < g.getArena().getMaxPlayers())
            .toList();
            
        if (availableGames.isEmpty()) {
            // No available games, try to start one
            Set<String> arenaNames = plugin.getArenaManager().getArenaNames();
            if (arenaNames.isEmpty()) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cNo arenas available!");
                return false;
            }
            
            // Find a configured arena that isn't already in use
            Arena arena = null;
            for (String arenaName : arenaNames) {
                Arena candidate = plugin.getArenaManager().getArena(arenaName);
                if (candidate != null && candidate.isConfigured() && !activeGames.containsKey(arenaName.toLowerCase())) {
                    arena = candidate;
                    break;
                }
            }
            
            if (arena == null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cNo available arenas found!");
                return false;
            }
            
            Game newGame = startGame(arena);
            if (newGame == null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cFailed to start a new game!");
                return false;
            }
            
            return addPlayerToGame(player, newGame);
        } else {
            // Join a random available game
            Game game = availableGames.get(ThreadLocalRandom.current().nextInt(availableGames.size()));
            return addPlayerToGame(player, game);
        }
    }
}
