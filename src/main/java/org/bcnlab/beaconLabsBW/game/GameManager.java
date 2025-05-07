package org.bcnlab.beaconLabsBW.game;

import lombok.Getter;
import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.arena.model.GeneratorData;
import org.bcnlab.beaconLabsBW.arena.model.TeamData;
import org.bcnlab.beaconLabsBW.shop.ShopVillagerData;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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
        
        // Filter for configured arenas
        List<Arena> configuredArenas = arenaNames.stream()
            .map(name -> plugin.getArenaManager().getArena(name))
            .filter(candidate -> candidate != null && candidate.isConfigured())
            .collect(Collectors.toList());
        
        if (configuredArenas.isEmpty()) {
            plugin.getLogger().info("No properly configured arenas found. Edit mode activated.");
            return;
        }
        
        // Choose a random configured arena
        Arena arena = configuredArenas.get(ThreadLocalRandom.current().nextInt(configuredArenas.size()));
        
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
        if (arena == null) {
            plugin.getLogger().warning("Attempted to start game with null arena");
            return null;
        }
        
        if (!arena.isConfigured()) {
            plugin.getLogger().warning("Attempted to start game with improperly configured arena: " + arena.getName());
            
            // Log specific missing configuration elements
            if (arena.getTeams().isEmpty()) {
                plugin.getLogger().warning("Arena " + arena.getName() + " has no teams");
            }
            
            if (arena.getLobbySpawn() == null) {
                plugin.getLogger().warning("Arena " + arena.getName() + " has no lobby spawn");
            }
            
            if (arena.getSpectatorSpawn() == null) {
                plugin.getLogger().warning("Arena " + arena.getName() + " has no spectator spawn");
            }
            
            // Check for configured generators
            boolean hasIron = false;
            boolean hasGold = false;
            boolean hasEmerald = false;
            boolean hasTeamGen = false;
            boolean hasDiamond = false; // Added diamond check
            
            for (GeneratorData generator : arena.getGenerators().values()) {
                switch (generator.getType()) {
                    case IRON -> hasIron = true;
                    case GOLD -> hasGold = true;
                    case TEAM -> hasTeamGen = true;
                    case EMERALD -> hasEmerald = true;
                    case DIAMOND -> hasDiamond = true;
                }
            }
            
            if (!hasIron && !hasTeamGen) plugin.getLogger().warning("Arena " + arena.getName() + " has no iron generators");
            if (!hasGold && !hasTeamGen) plugin.getLogger().warning("Arena " + arena.getName() + " has no gold generators");
            if (!hasEmerald) plugin.getLogger().warning("Arena " + arena.getName() + " has no emerald generators");
            
            return null;
        }
        
        // Check if there's already a game for this arena
        if (activeGames.containsKey(arena.getName().toLowerCase())) {
            Game existingGame = activeGames.get(arena.getName().toLowerCase());
            
            // If the game is in WAITING state, we can return it to allow joining
            if (existingGame.getState() == GameState.WAITING) {
                plugin.getLogger().info("Joining existing waiting game for arena: " + arena.getName());
                return existingGame;
            }
            
            plugin.getLogger().warning("Attempted to start game with arena that's already in use: " + arena.getName());
            return null;
        }
        
        Game game = new Game(plugin, arena);
        activeGames.put(arena.getName().toLowerCase(), game);
        game.setup();
        
        // Spawn villagers for this arena using the new delayed method
        plugin.getVillagerManager().spawnArenaVillagersWithDelay(arena);
        
        plugin.getLogger().info("Started new BedWars game with arena: " + arena.getName());
        return game;
    }
      /**
     * End a BedWars game
     * 
     * @param game The game to end
     */
    public void endGame(Game game) {
        if (game == null) return;

        String arenaNameKey = game.getArena().getName().toLowerCase();
        // Check if the game is actually in activeGames before trying to remove
        if (!activeGames.containsKey(arenaNameKey) && playerGameMap.values().stream().noneMatch(g -> g.equals(game))) {
            // If it's not in activeGames and no player is mapped to it, it might have already been ended.
            // We can still proceed with cleanup if needed, or just log and return.
            plugin.getLogger().info("GameManager.endGame called for game " + game.getGameId() + " which is not in activeGames or playerGameMap.");
            // Optionally, ensure cleanup if game state indicates it hasn't been fully cleaned
            if (game.getState() != GameState.WAITING) { // Assuming WAITING is the truly cleaned state
                 game.cleanup(); // Ensure cleanup is called
            }
            return; // Or decide if further actions are needed for already-ended games
        }

        plugin.getLogger().info("Ending game: " + game.getGameId() + " for arena: " + arenaNameKey);
        activeGames.remove(arenaNameKey);
        
        // Remove player mappings
        for (UUID playerId : game.getPlayers()) {
            playerGameMap.remove(playerId);
        }
        
        // Clear waiting lobby reference if this was the waiting lobby
        if (waitingLobby != null && waitingLobby.equals(game)) {
            waitingLobby = null;
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
      // Keep track of the active waiting lobby
    private Game waitingLobby = null;
    private Arena nextArena = null;
    
    /**
     * Choose a game for a player to join
     * 
     * @param player The player
     * @return true if joined, false otherwise
     */
    public boolean joinGame(Player player) {
        if (player == null) return false;
        
        // Check if player is already in a game (rejoin)
        Game existingGame = getPlayerGame(player);
        if (existingGame != null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou are already in a game!");
            return false;
        }
        
        // Check if there's already a waiting lobby game
        if (waitingLobby != null && waitingLobby.getState() == GameState.WAITING) {
            // Join the existing waiting lobby if it has space
            if (waitingLobby.getPlayers().size() < waitingLobby.getArena().getMaxPlayers()) {
                return addPlayerToGame(player, waitingLobby);
            } else {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThe waiting lobby is full!");
                return false;
            }
        } else if (waitingLobby != null && waitingLobby.getState() == GameState.STARTING) {
            // Join the starting game if there's still space
            if (waitingLobby.getPlayers().size() < waitingLobby.getArena().getMaxPlayers()) {
                return addPlayerToGame(player, waitingLobby);
            } else {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThe game is starting and is full!");
                return false;
            }
        } else if (haveRunningGames()) {
            // A game is running. Attempt to add the player as a spectator.
            Game runningGame = findRunningGame();
            if (runningGame != null) {
                if (addSpectatorToGame(player, runningGame)) {
                    // Successfully added as spectator
                    return true;
                } else {
                    // Failed to add as spectator (e.g., already in game?)
                    MessageUtils.sendMessage(player, plugin.getPrefix() + "&cCould not join the current game as a spectator.");
                    return false;
                }
            } else {
                // This case should technically not happen if haveRunningGames() is true,
                // but handle it defensively.
                plugin.getLogger().warning("haveRunningGames() was true, but findRunningGame() returned null.");
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cCould not find the running game to spectate.");
                return false;
            }
        } else {
            // No active waiting lobby or running game, create a new lobby.
            waitingLobby = null; // Clear any old reference
            
            // Choose an arena for the new game
            Arena arena = chooseNextArena();
            if (arena == null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cNo available arenas found!");
                return false;
            }
            
            Game newGame = startGame(arena);
            if (newGame == null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cFailed to start a new game!");
                return false;
            }
            
            waitingLobby = newGame;
            return addPlayerToGame(player, newGame);
        }
    }
    
    /**
     * Check if there are any running games
     * 
     * @return true if there are running games, false otherwise
     */
    public boolean haveRunningGames() {
        return activeGames.values().stream()
            .anyMatch(game -> game.getState() == GameState.RUNNING);
    }

    /**
     * Find the currently running game, if any.
     *
     * @return The running Game instance, or null if no game is running.
     */
    private Game findRunningGame() {
        return activeGames.values().stream()
            .filter(game -> game.getState() == GameState.RUNNING)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Choose the next arena to use
     * 
     * @return The selected arena
     */
    private Arena chooseNextArena() {
        if (nextArena != null && nextArena.isConfigured() && 
            !activeGames.containsKey(nextArena.getName().toLowerCase())) {
            Arena selected = nextArena;
            nextArena = null;
            return selected;
        }
        
        Set<String> arenaNames = plugin.getArenaManager().getArenaNames();
        if (arenaNames.isEmpty()) {
            return null;
        }
          // Convert to list for random access - arenas are available if:
        // 1. They are configured
        // 2. They are either not in use OR they have a game in WAITING state with room
        List<String> availableArenas = arenaNames.stream()
            .filter(name -> {
                Arena arena = plugin.getArenaManager().getArena(name);
                if (arena == null || !arena.isConfigured()) {
                    return false;
                }
                
                // Check if the arena is already in use
                Game existingGame = activeGames.get(name.toLowerCase());
                if (existingGame == null) {
                    return true; // Arena not in use, it's available
                }
                
                // If the existing game is in WAITING state and has room, consider it available
                return existingGame.getState() == GameState.WAITING && 
                       existingGame.getPlayers().size() < existingGame.getArena().getMaxPlayers();
            })
            .collect(Collectors.toList());
        
        plugin.getLogger().info("[GameManager] Available arenas for random selection: " + availableArenas);
        
        if (availableArenas.isEmpty()) {
            plugin.getLogger().warning("[GameManager] No available configured arenas found to choose from!");
            return null;
        }
        
        // Choose a random arena
        String randomArenaName = availableArenas.get(
            ThreadLocalRandom.current().nextInt(availableArenas.size()));
        return plugin.getArenaManager().getArena(randomArenaName);
    }
    
    /**
     * Set the next arena to be used
     * 
     * @param arena The arena to use next
     */
    public void setNextArena(Arena arena) {
        nextArena = arena;
    }
    
    /**
     * Get the current waiting lobby
     * 
     * @return The waiting lobby game, or null if none
     */
    public Game getWaitingLobby() {
        return waitingLobby;
    }

    /**
     * Add a player directly as a spectator to a running game.
     *
     * @param player The player to add.
     * @param game The game to add the spectator to.
     * @return true if successfully added, false otherwise.
     */
    public boolean addSpectatorToGame(Player player, Game game) {
        if (player == null || game == null) {
            return false;
        }

        // Ensure player isn't already mapped to a game
        if (playerGameMap.containsKey(player.getUniqueId())) {
            plugin.getLogger().warning("Attempted to add spectator " + player.getName() + " who is already mapped to a game.");
            return false;
        }

        // Attempt to add spectator via the Game object
        if (game.addSpectator(player)) {
            // Map the player to the game they are spectating
            playerGameMap.put(player.getUniqueId(), game);
            return true;
        } else {
            // Adding spectator failed (e.g., game not running, player already in game)
            return false;
        }
    }
}
