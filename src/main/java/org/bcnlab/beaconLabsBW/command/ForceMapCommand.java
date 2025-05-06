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
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cArena &e" + arenaName + " &cdoesn't exist.");
            return true;
        }
        
        if (!arena.isConfigured()) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cArena &e" + arenaName + " &cis not fully configured.");
            return true;
        }
        
        // Check for an active waiting lobby
        Game waitingLobby = plugin.getGameManager().getWaitingLobby();
        if (waitingLobby == null || waitingLobby.getState() == GameState.RUNNING) {
            // If there's no waiting lobby or the game is running, just set the next arena
            plugin.getGameManager().setNextArena(arena);
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&aSet &e" + arena.getName() + " &aas the next arena.");
            return true;
        }
        
        // If the waiting lobby is already on this arena, do nothing
        if (waitingLobby.getArena().getName().equalsIgnoreCase(arena.getName())) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cThe current lobby is already using arena &e" + arena.getName());
            return true;
        }
        
        // Switch arenas - this requires stopping the current game and starting a new one
        Set<UUID> currentPlayers = new HashSet<>(waitingLobby.getPlayers());
        
        // End the current game
        plugin.getGameManager().endGame(waitingLobby);
        
        // Start a new game with the specified arena
        Game newGame = plugin.getGameManager().startGame(arena);
        
        if (newGame == null) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cFailed to start new game with arena &e" + arena.getName());
            return true;
        }
        
        // Transfer all players to the new game
        for (UUID playerId : currentPlayers) {
            Player gamePlayer = Bukkit.getPlayer(playerId);
            if (gamePlayer != null && gamePlayer.isOnline()) {
                plugin.getGameManager().addPlayerToGame(gamePlayer, newGame);
            }
        }
        
        // Broadcast map change
        for (UUID playerId : newGame.getPlayers()) {
            Player gamePlayer = Bukkit.getPlayer(playerId);
            if (gamePlayer != null) {
                MessageUtils.sendMessage(gamePlayer, plugin.getPrefix() + "&aThe map has been changed to &e" + arena.getName());
            }
        }
        
        MessageUtils.sendMessage(sender, plugin.getPrefix() + "&aSuccessfully switched arena to &e" + arena.getName());
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
