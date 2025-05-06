package org.bcnlab.beaconLabsBW.command;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for the /forcestart command
 * This command reduces the countdown timer to 3 seconds
 */
public class ForceStartCommand implements CommandExecutor, TabCompleter {
    
    private final BeaconLabsBW plugin;
    
    public ForceStartCommand(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bedwars.admin")) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cYou don't have permission to force start games.");
            return true;
        }
        
        // Check for an active waiting lobby
        Game waitingLobby = plugin.getGameManager().getWaitingLobby();
        if (waitingLobby == null) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cThere is no waiting lobby to force start.");
            return true;
        }
        
        if (waitingLobby.getState() == GameState.RUNNING) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cThe game has already started.");
            return true;
        }
        
        if (waitingLobby.getPlayers().size() < plugin.getConfigManager().getMinPlayers()) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cNot enough players to start the game.");
            return true;
        }
        
        // Force the game to start by setting the countdown to 3 seconds
        if (waitingLobby.getState() == GameState.STARTING) {
            waitingLobby.setCountdown(3);
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&aForcing game to start in 3 seconds!");
            waitingLobby.broadcastMessage("&a&lGame starting in &c3 &aseconds! &7(Force started by admin)");
        } else if (waitingLobby.getState() == GameState.WAITING) {
            // Start the countdown
            waitingLobby.startCountdown();
            waitingLobby.setCountdown(3);
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&aForcing game to start in 3 seconds!");
            waitingLobby.broadcastMessage("&a&lGame starting in &c3 &aseconds! &7(Force started by admin)");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // No tab completions for this command
        return new ArrayList<>();
    }
}
