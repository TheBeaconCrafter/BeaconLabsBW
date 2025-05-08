package org.bcnlab.beaconLabsBW.command;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Command handler for the /forcemap command
 * This command switches the waiting lobby to a different arena
 */
public class ForceMapCommand implements CommandExecutor, TabCompleter {
    
    private final BeaconLabsBW plugin;
    
    public ForceMapCommand(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bedwars.admin")) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cYou don't have permission to force map changes.");
            return true;
        }
        
        // Check arguments
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cUsage: /forcemap <arena>");
            return true;
        }
        
        String arenaName = args[0];
        Arena targetArena = plugin.getArenaManager().getArena(arenaName);
        
        if (targetArena == null) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cArena &e" + arenaName + " &cdoesn't exist.");
            return true;
        }
        
        if (!targetArena.isConfigured()) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cArena &e" + arenaName + " &cis not fully configured.");
            return true;
        }
        
        Game currentWaitingLobby = plugin.getGameManager().getWaitingLobby();

        // Condition for IMMEDIATE switch:
        // A waiting lobby exists, AND it's in WAITING or STARTING state.
        if (currentWaitingLobby != null && 
            (currentWaitingLobby.getState() == GameState.WAITING || currentWaitingLobby.getState() == GameState.STARTING)) {
            
            // If the waiting lobby is already on this target arena, do nothing specific other than inform.
            if (currentWaitingLobby.getArena().getName().equalsIgnoreCase(targetArena.getName())) {
                MessageUtils.sendMessage(sender, plugin.getPrefix() + "&eThe current lobby is already using arena &6" + targetArena.getName() + "&e.");
                return true;
            }
            
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&6Switching active lobby to &e" + targetArena.getName() + "&6...");

            Set<UUID> currentPlayers = new HashSet<>(currentWaitingLobby.getPlayers());
            
            // End the current game (this should handle countdown cancellation if it's in STARTING state)
            plugin.getGameManager().endGame(currentWaitingLobby); // This also nullifies GameManager.waitingLobby if it matches
            
            // Start a new game with the specified arena
            Game newGame = plugin.getGameManager().startGame(targetArena);
            
            if (newGame == null) {
                MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cFailed to start new game with arena &e" + targetArena.getName() + ". The previous lobby was ended.");
                // Attempt to restart a lobby with any available map or the old map if possible?
                // For now, admin needs to manually start a new game or players need to rejoin.
                return true;
            }
            
            // Transfer all players to the new game
            for (UUID playerId : currentPlayers) {
                Player gamePlayer = Bukkit.getPlayer(playerId);
                if (gamePlayer != null && gamePlayer.isOnline()) {
                    // Explicitly remove player from any old game context first
                    // This ensures their state is fully reset and mappings cleared by GameManager
                    plugin.getGameManager().removePlayerFromGame(gamePlayer);
                    
                    // Now, add to the new game
                    if (!plugin.getGameManager().addPlayerToGame(gamePlayer, newGame)) {
                        MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cWarning: Failed to transfer player &e" + gamePlayer.getName() + " &cto the new lobby.");
                    }
                }
            }
            
            // Announce to players in the new game
            newGame.broadcastMessage("&aThe map has been changed to &e" + targetArena.getName() + "&a by an admin.");
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&aSuccessfully switched arena to &e" + targetArena.getName());

        } else {
            // Fallback: No suitable WAITING/STARTING lobby for immediate player transfer,
            // OR the existing lobby is RUNNING or in another state.
            // The goal now is to ensure the targetArena becomes the active lobby.

            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&6Attempting to set &e" + targetArena.getName() + " &6as the active lobby...");

            // 1. End any existing game that might be considered the "waitingLobby" by GameManager, 
            if (currentWaitingLobby != null) {
                MessageUtils.sendMessage(sender, plugin.getPrefix() + "&7Stopping current game/lobby for arena '" + currentWaitingLobby.getArena().getName() + "' (State: " + currentWaitingLobby.getState() + ").");
                plugin.getGameManager().endGame(currentWaitingLobby); 
            }

            // 2. Check if there's ANY other game running on the *targetArena* itself. 
            Game gameOnTargetArena = plugin.getGameManager().getActiveGames().get(targetArena.getName().toLowerCase());
            if (gameOnTargetArena != null) {
                 MessageUtils.sendMessage(sender, plugin.getPrefix() + "&7Stopping existing game on target arena '" + targetArena.getName() + "' (State: " + gameOnTargetArena.getState() + ").");
                 plugin.getGameManager().endGame(gameOnTargetArena);
            }
            
            // 3. Now, start a new game with the targetArena.
            Game newLobby = plugin.getGameManager().startGame(targetArena);

            if (newLobby != null) {
                MessageUtils.sendMessage(sender, plugin.getPrefix() + "&aSuccessfully started new lobby with &e" + targetArena.getName() + "&a. Players can now join it.");
                
                // Teleport the command sender to the new lobby if they are a player
                if (sender instanceof Player) {
                    Player adminPlayer = (Player) sender;
                    // We need to add the admin to the game for teleportation to lobby spawn to work as expected
                    // and for them to be part of this new lobby context.
                    if (plugin.getGameManager().addPlayerToGame(adminPlayer, newLobby)) {
                        MessageUtils.sendMessage(adminPlayer, plugin.getPrefix() + "&6Teleporting you to the new lobby for &e" + targetArena.getName() + "&6.");
                        // Game.addPlayer already teleports to lobby and resets player.
                    } else {
                        MessageUtils.sendMessage(adminPlayer, plugin.getPrefix() + "&cCould not automatically add you to the new lobby. Please join manually.");
                        // As a fallback, try a direct teleport to the arena's lobby spawn if known, 
                        // but being in the game is better.
                        if (targetArena.getLobbySpawn() != null && targetArena.getLobbySpawn().toBukkitLocation() != null) {
                            adminPlayer.teleport(targetArena.getLobbySpawn().toBukkitLocation());
                        }
                    }
                }
            } else {
                MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cFailed to start a new lobby with &e" + targetArena.getName() + "&c. Please check server logs.");
                plugin.getGameManager().setNextArena(targetArena);
                MessageUtils.sendMessage(sender, plugin.getPrefix() + "&6Set &e" + targetArena.getName() + " &6as the next arena instead.");
            }
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - arena names
            String partial = args[0].toLowerCase();
            
            // Suggest arena names
            for (String arena : plugin.getArenaManager().getArenaNames()) {
                if (arena.toLowerCase().startsWith(partial)) {
                    completions.add(arena);
                }
            }
        }
        
        return completions;
    }
}
