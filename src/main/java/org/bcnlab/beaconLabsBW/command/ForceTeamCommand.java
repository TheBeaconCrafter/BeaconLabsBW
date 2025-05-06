package org.bcnlab.beaconLabsBW.command;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bukkit.Bukkit;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command handler for the /forceteam command
 * This command forces a player onto a specific team
 */
public class ForceTeamCommand implements CommandExecutor, TabCompleter {
    
    private final BeaconLabsBW plugin;
    
    public ForceTeamCommand(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bedwars.admin")) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cYou don't have permission to force team assignments.");
            return true;
        }
        
        // Check arguments
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cUsage: /forceteam [player] <team>");
            return true;
        }
        
        Player target;
        String teamName;
        
        if (args.length >= 2) {
            // Force another player
            target = Bukkit.getPlayerExact(args[0]);
            teamName = args[1];
            
            if (target == null) {
                MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cPlayer &e" + args[0] + " &cis not online.");
                return true;
            }
        } else {
            // Force command executor if they are a player
            if (!(sender instanceof Player player)) {
                MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cAs console, you must specify a player: /forceteam <player> <team>");
                return true;
            }
            
            target = (Player) sender;
            teamName = args[0];
        }
        
        // Check if target is in a game
        Game game = plugin.getGameManager().getPlayerGame(target);
        if (game == null) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cThe target player is not in a game.");
            return true;
        }
        
        // Check if the team exists in this game
        if (!game.getArena().getTeams().containsKey(teamName)) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cTeam &e" + teamName + " &cdoesn't exist in this game.");
            return true;
        }
        
        // Only allow team changes before game starts
        if (game.getState() == GameState.RUNNING) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cCannot change teams after the game has started.");
            return true;
        }
        
        // Remove from current team if any
        String currentTeam = game.getPlayerTeam(target);
        if (currentTeam != null) {
            game.getTeams().get(currentTeam).remove(target.getUniqueId());
        }
        
        // Add to new team
        game.getPlayerTeams().put(target.getUniqueId(), teamName);
        game.getTeams().computeIfAbsent(teamName, k -> ConcurrentHashMap.newKeySet()).add(target.getUniqueId());
        
        // Notify both players
        MessageUtils.sendMessage(target, plugin.getPrefix() + "&aYou have been assigned to team &e" + teamName);
        if (sender != target) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&aAssigned &e" + target.getName() + " &ato team &e" + teamName);
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - player names or team names if sender is in a game
            String partial = args[0].toLowerCase();
            
            // Always suggest online players first
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(partial)) {
                    completions.add(onlinePlayer.getName());
                }
            }
            
            // If the sender is in a game, also suggest team names
            if (sender instanceof Player player) {
                Game game = plugin.getGameManager().getPlayerGame(player);
                if (game != null) {
                    for (String team : game.getArena().getTeams().keySet()) {
                        if (team.toLowerCase().startsWith(partial)) {
                            completions.add(team);
                        }
                    }
                }
            }
        } else if (args.length == 2) {
            // Second argument - team names for the specified player
            String partial = args[1].toLowerCase();
            
            if (sender instanceof Player commander) {
                Player targetPlayer = Bukkit.getPlayerExact(args[0]);
                Game game = targetPlayer != null ? 
                    plugin.getGameManager().getPlayerGame(targetPlayer) : 
                    plugin.getGameManager().getPlayerGame(commander);
                    
                if (game != null) {
                    for (String team : game.getArena().getTeams().keySet()) {
                        if (team.toLowerCase().startsWith(partial)) {
                            completions.add(team);
                        }
                    }
                }
            }
        }
        
        return completions;
    }
}
