package org.bcnlab.beaconLabsBW.listeners;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles block-related events for BedWars
 */
public class BlockListener implements Listener {
    
    private final BeaconLabsBW plugin;
    
    public BlockListener(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    // List of blocks that can be broken by players (blocks sold in the shop)
    private static final java.util.Set<Material> BREAKABLE_BLOCKS = new java.util.HashSet<>(java.util.Arrays.asList(
        Material.WHITE_WOOL, Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,
        Material.YELLOW_WOOL, Material.LIGHT_BLUE_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL,
        Material.OAK_PLANKS, Material.STONE, Material.END_STONE, Material.OBSIDIAN, 
        Material.TERRACOTTA, Material.GLASS
    ));
    
    /**
     * Check if a material is a block that can be broken by players
     * 
     * @param material Block material
     * @return true if breakable, false otherwise
     */
    private boolean isBreakableBlock(Material material) {
        return material != null && BREAKABLE_BLOCKS.contains(material);
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
              // Special handling for TNT - instant ignition
            if (block.getType() == Material.TNT) {
                event.setCancelled(true); // Cancel the placement
                block.setType(Material.AIR); // Make sure the block is not placed
                
                // Remove one TNT from the player's hand
                ItemStack heldItem = event.getItemInHand();
                if (heldItem.getAmount() > 1) {
                    heldItem.setAmount(heldItem.getAmount() - 1);
                    player.getInventory().setItemInMainHand(heldItem);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                
                // Spawn primed TNT instead
                TNTPrimed tnt = player.getWorld().spawn(block.getLocation().add(0.5, 0.0, 0.5), TNTPrimed.class);
                tnt.setFuseTicks(40); // 2-second fuse (40 ticks)
                tnt.setSource(player); // Set the player as the source
                
                // Play the primed sound
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_TNT_PRIMED, 0.5f, 1.0f);
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
            }            // Check if this is a bed block (handled by PlayerListener)
            if (isBedBlock(block.getType())) {
                // Cancel the event - PlayerListener will handle the bed breaking
                event.setCancelled(true);
                return;
            }
              // Allow breaking blocks that were placed during the game
            if (game.isPlacedBlock(block)) {
                return;
            } 
            // Don't allow breaking blocks that weren't placed by players, even if they're valid shop blocks
            else if (isBreakableBlock(block.getType())) {
                event.setCancelled(true);
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou can only break blocks placed by players!");
                return;
            }
            else {
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
