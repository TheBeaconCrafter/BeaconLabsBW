package org.bcnlab.beaconLabsBW.game.ultimates;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles wool generation for Builder ultimate class
 */
public class BuilderWoolTask implements Runnable {
    
    private final BeaconLabsBW plugin;
    private static final long WOOL_GENERATION_INTERVAL = 15; // Seconds between wool generation
    private static final int WOOL_GENERATION_AMOUNT = 4; // Amount of wool to generate at each interval
    private final Map<UUID, Long> lastWoolGeneration = new HashMap<>();
    
    public BuilderWoolTask(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        for (Game game : plugin.getGameManager().getActiveGames().values()) {
            // Only run for ultimates mode games
            if (game.getGameMode() != org.bcnlab.beaconLabsBW.game.GameMode.ULTIMATES) {
                continue;
            }
            
            // Process each player
            for (UUID playerId : game.getPlayers()) {
                // Check if player is a builder
                UltimateClass playerClass = game.getPlayerUltimateClass(playerId);
                if (playerClass != UltimateClass.BUILDER) {
                    continue;
                }
                
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline() || game.isSpectator(player)) {
                    continue;
                }
                
                // Check if it's time to generate wool
                long now = System.currentTimeMillis();
                if (!lastWoolGeneration.containsKey(playerId) || 
                    (now - lastWoolGeneration.get(playerId)) >= (WOOL_GENERATION_INTERVAL * 1000)) {
                    
                    // Generate wool based on the player's team
                    String team = game.getPlayerTeam(player);
                    if (team != null) {
                        Material woolType = game.getTeamWoolMaterial(team);
                        ItemStack wool = new ItemStack(woolType, WOOL_GENERATION_AMOUNT);
                        
                        // Add wool to inventory
                        Map<Integer, ItemStack> leftover = player.getInventory().addItem(wool);
                        
                        // If there was room in the inventory
                        if (leftover.isEmpty()) {
                            player.sendMessage(ChatColor.YELLOW + "Your Builder ability generated " + 
                                               WOOL_GENERATION_AMOUNT + " wool blocks!");
                            
                            // Play a subtle sound
                            player.playSound(player.getLocation(), 
                                            org.bukkit.Sound.ENTITY_CHICKEN_EGG, 0.5f, 1.2f);
                        }
                        
                        // Update last generation time
                        lastWoolGeneration.put(playerId, now);
                    }
                }
            }
        }
    }
}
