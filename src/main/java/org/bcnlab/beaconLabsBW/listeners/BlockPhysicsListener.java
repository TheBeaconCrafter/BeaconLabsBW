package org.bcnlab.beaconLabsBW.listeners;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

/**
 * Handles block physics and related events for BedWars
 * Prevents modification of non-player-placed blocks
 */
public class BlockPhysicsListener implements Listener {
    
    private final BeaconLabsBW plugin;
    
    public BlockPhysicsListener(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle block fade events (like snow melting, ice melting, etc.)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFade(BlockFadeEvent event) {
        // Check if this block is in a running game and not player-placed
        if (shouldProtectBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle block form events (like water freezing, etc.)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockForm(BlockFormEvent event) {
        // Check if this block is in a running game and not player-placed
        if (shouldProtectBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle block spread events (like fire spreading)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockSpread(BlockSpreadEvent event) {
        // Check if the source block is in a running game and not player-placed
        if (shouldProtectBlock(event.getSource()) || shouldProtectBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle block physics events (like gravity-affected blocks falling)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        // Check if this block is in a running game and not player-placed
        if (shouldProtectBlock(event.getBlock())) {
            // Only cancel if this would actually change the block
            // This prevents cancelling every physics event, which could cause lag
            if (event.getChangedType() != event.getBlock().getType()) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Handle water/lava flow events
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent event) {
        // Check if the liquid is flowing into a protected block location
        Block toBlock = event.getToBlock();
        
        // Protect original map blocks in game arenas from liquid flow
        if (shouldProtectBlock(toBlock)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle piston extend events
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        // Check if any of the blocks being moved are protected
        for (Block block : event.getBlocks()) {
            if (shouldProtectBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    /**
     * Handle piston retract events
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        // Check if any of the blocks being moved are protected
        for (Block block : event.getBlocks()) {
            if (shouldProtectBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    /**
     * Determines if a block should be protected from modification
     * 
     * @param block The block to check
     * @return true if the block should be protected, false otherwise
     */
    private boolean shouldProtectBlock(Block block) {
        // Check if this block is in a running game
        for (Game game : plugin.getGameManager().getActiveGames().values()) {
            if (game.getArena().getWorldName().equals(block.getWorld().getName())) {
                // Protect any blocks that weren't placed by players
                return !game.isPlacedBlock(block);
            }
        }
        return false;
    }
}
