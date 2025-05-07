package org.bcnlab.beaconLabsBW.command;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameMode;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command handler for the /mode command
 * This command changes the game mode between normal and ultimates
 */
public class ModeCommand implements CommandExecutor, TabCompleter {
    
    private final BeaconLabsBW plugin;
    
    public ModeCommand(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cThis command must be executed by a player.");
            return true;
        }
        
        // Check if player is in a game
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in a game to use this command.");
            return true;
        }
        
        // Check if the game is in waiting or starting state
        if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cGame mode can only be changed before the game starts.");
            return true;
        }
        
        // Check arguments
        if (args.length < 1) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /mode <normal|ultimates>");
            return true;
        }
        
        String modeArg = args[0].toLowerCase();
        GameMode newMode;
        
        switch (modeArg) {
            case "normal" -> newMode = GameMode.NORMAL;
            case "ultimates" -> newMode = GameMode.ULTIMATES;
            default -> {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cInvalid mode. Use 'normal' or 'ultimates'.");
                return true;
            }
        }
        
        // Set the game mode
        game.setGameMode(newMode);
        game.broadcastMessage("&e" + player.getName() + " &achanged the game mode to &e" + newMode.getDisplayName());
        
        if (newMode == GameMode.ULTIMATES) {
            game.broadcastMessage("&aUltimates mode enabled! Select your ultimate class in the shop.");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String mode : Arrays.asList("normal", "ultimates")) {
                if (mode.startsWith(partial)) {
                    completions.add(mode);
                }
            }
        }
        
        return completions;
    }
}
