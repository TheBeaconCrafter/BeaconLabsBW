package org.bcnlab.beaconLabsBW.listeners;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

/**
 * Handles entity-related events for BedWars
 */
public class EntityListener implements Listener {
    
    private final BeaconLabsBW plugin;
    
    public EntityListener(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        
        // Prevent natural mob spawning in game worlds
        if (entity instanceof Monster || entity instanceof Animals) {
            for (Game game : plugin.getGameManager().getActiveGames().values()) {
                if (entity.getWorld().getName().equals(game.getArena().getWorldName())) {
                    // Cancel natural spawning
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        
        // For TNT in game, limit explosion blocks to only player-placed blocks
        for (Game game : plugin.getGameManager().getActiveGames().values()) {
            if (entity.getWorld().getName().equals(game.getArena().getWorldName())) {
                if (game.getState() == GameState.RUNNING) {
                    // Remove non-player placed blocks from explosion
                    event.blockList().removeIf(block -> !game.isPlacedBlock(block));
                } else {
                    // Cancel explosion if game is not running
                    event.setCancelled(true);
                }
                return;
            }
        }
    }
    
    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Entity remover = event.getRemover();
        
        if (remover instanceof Player player) {
            // Allow removal in edit mode
            if (plugin.getArenaManager().isEditing(player)) {
                return;
            }
            
            // Check if in a game
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game != null) {
                if (game.getState() != GameState.RUNNING) {
                    event.setCancelled(true);
                }
                return;
            }
            
            // Check if this is in an active arena
            for (Game activeGame : plugin.getGameManager().getActiveGames().values()) {
                if (event.getEntity().getWorld().getName().equals(activeGame.getArena().getWorldName())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            
            if (game != null) {
                // Players don't get hungry in BedWars
                event.setCancelled(true);
                event.setFoodLevel(20);
            }
        }
    }
    
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            
            if (game != null && game.isSpectator(player)) {
                // Mobs should not target spectators
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            
            if (game != null && game.isSpectator(player)) {
                // Spectators can't pick up items
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        
        // Allow only specific spawn reasons in game worlds
        for (Game game : plugin.getGameManager().getActiveGames().values()) {
            if (entity.getWorld().getName().equals(game.getArena().getWorldName())) {
                // Allow spawning from spawn eggs, custom mechanics, but prevent natural spawning
                CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
                if (reason != CreatureSpawnEvent.SpawnReason.CUSTOM && 
                    reason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG && 
                    reason != CreatureSpawnEvent.SpawnReason.DISPENSE_EGG) {
                    event.setCancelled(true);
                }
                return;
            }
        }
    }
    
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        
        if (projectile.getShooter() instanceof Player player) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            
            if (game != null && game.getState() != GameState.RUNNING) {
                // Can't shoot projectiles before game starts
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        
        if (projectile.getShooter() instanceof Player player) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            
            // Handle special projectiles like bridge eggs
            if (game != null && game.getState() == GameState.RUNNING && projectile instanceof Egg) {
                // Can implement bridge egg logic here
            }
        }
    }
}
