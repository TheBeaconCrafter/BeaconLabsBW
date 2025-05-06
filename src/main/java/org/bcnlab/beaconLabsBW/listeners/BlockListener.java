package org.bcnlab.beaconLabsBW.listeners;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Handles block-related events for BedWars
 */
public class BlockListener implements Listener {
    
    private final BeaconLabsBW plugin;
    
    public BlockListener(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if player is in edit mode
        Arena editingArena = plugin.getArenaManager().getEditingArena(player);
        if (editingArena != null) {
            // Allow placing in edit mode
            return;
        }
        
        // Check if player is in a game
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            if (game.getState() != GameState.RUNNING) {
                // Can't place blocks before game starts
                event.setCancelled(true);
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou can only place blocks during the game!");
                return;
            }
            
            // Record the block for cleanup later
            game.recordPlacedBlock(block);
            return;
        }
        
        // Check if this block is placed in an active arena
        for (Game activeGame : plugin.getGameManager().getActiveGames().values()) {
            if (block.getWorld().getName().equals(activeGame.getArena().getWorldName())) {
                // Can't place blocks in an active arena if not in the game
                event.setCancelled(true);
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou can't modify active game arenas!");
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if player is in edit mode
        Arena editingArena = plugin.getArenaManager().getEditingArena(player);
        if (editingArena != null) {
            // Allow breaking in edit mode
            return;
        }
        
        // Check if player is in a game
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            if (game.getState() != GameState.RUNNING) {
                // Can't break blocks before game starts
                event.setCancelled(true);
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou can only break blocks during the game!");
                return;
            }
            
            // Check if this is a special block (bed)
            if (isBedBlock(block.getType())) {
                // Let the player listener handle bed breaking
                return;
            }
            
            // Allow breaking blocks that were placed during the game
            if (game.isPlacedBlock(block)) {
                return;
            } else {
                // Prevent breaking original map blocks
                event.setCancelled(true);
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou can only break blocks placed by players!");
                return;
            }
        }
        
        // Check if this block is in an active arena
        for (Game activeGame : plugin.getGameManager().getActiveGames().values()) {
            if (block.getWorld().getName().equals(activeGame.getArena().getWorldName())) {
                // Can't break blocks in an active arena if not in the game
                event.setCancelled(true);
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou can't modify active game arenas!");
                return;
            }
        }
    }
      private boolean isBedBlock(Material material) {
        return material != null && material.name().contains("BED");
    }
}
