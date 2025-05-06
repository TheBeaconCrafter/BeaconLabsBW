package org.bcnlab.beaconLabsBW.listeners;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.util.UUID;

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
        
        // Prevent regular iron golems from spawning naturally in the world
        if (entity instanceof IronGolem && !entity.hasMetadata("team")) {
            // Only allow custom iron golems (Dream Defenders)
            for (Game game : plugin.getGameManager().getActiveGames().values()) {
                if (entity.getWorld().getName().equals(game.getArena().getWorldName())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
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
                    // Only affect player-placed blocks
                    event.blockList().removeIf(block -> !game.isPlacedBlock(block));
                      // Preserve certain blocks
                    event.blockList().removeIf(block -> block.getType().name().contains("BED"));
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
                return;
            }
            
            // Handle Dream Defender targeting
            if (event.getEntity() instanceof IronGolem golem && golem.hasMetadata("team")) {
                // Get the golem's team
                String golemTeam = golem.getMetadata("team").get(0).asString();
                
                // Get the target player's team
                String playerTeam = game != null ? game.getPlayerTeam(player) : null;
                
                // Cancel targeting if player is on the same team as the golem
                if (golemTeam != null && golemTeam.equals(playerTeam)) {
                    event.setCancelled(true);
                }
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
        
        // Handle spawning iron golem (Dream Defender)
        if (event.getEntity() instanceof IronGolem golem && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            // Golems are spawned with metadata, so check for it
            if (golem.hasMetadata("team")) {
                // Schedule a task to make the golem target enemies
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    makeGolemTargetEnemies(golem);
                }, 10L); // Small delay to ensure metadata is properly set
            }
        }
    }
    
    /**
     * Make an Iron Golem actively search for enemies
     * 
     * @param golem The Iron Golem to update
     */
    private void makeGolemTargetEnemies(IronGolem golem) {
        // Check if the golem has team metadata
        if (!golem.hasMetadata("team")) return;
        
        String golemTeam = golem.getMetadata("team").get(0).asString();
        
        // Get all games to find which one this golem belongs to
        for (Game game : plugin.getGameManager().getActiveGames().values()) {
            if (golem.getWorld().getName().equals(game.getArena().getWorldName())) {
                
                // Schedule a repeating task to find targets
                Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                    // If the golem is dead or removed, cancel task
                    if (golem.isDead() || !golem.isValid()) {
                        task.cancel();
                        return;
                    }
                    
                    // Look for the nearest enemy player if not already targeting someone
                    if (golem.getTarget() == null) {
                        Player nearestEnemy = null;
                        double nearestDistance = 20.0; // Max target range
                        
                        // Look for nearby enemy players
                        for (UUID playerId : game.getPlayers()) {
                            Player otherPlayer = Bukkit.getPlayer(playerId);
                            
                            if (otherPlayer != null && otherPlayer.getWorld().equals(golem.getWorld()) && 
                                !game.isSpectator(otherPlayer)) {
                                
                                String playerTeam = game.getPlayerTeam(otherPlayer);
                                
                                // If player is on a different team, consider them as a target
                                if (playerTeam != null && !playerTeam.equals(golemTeam)) {
                                    double distance = otherPlayer.getLocation().distance(golem.getLocation());
                                    if (distance < nearestDistance) {
                                        nearestEnemy = otherPlayer;
                                        nearestDistance = distance;
                                    }
                                }
                            }
                        }
                        
                        // Set the target if we found one
                        if (nearestEnemy != null) {
                            golem.setTarget(nearestEnemy);
                        }
                    }
                }, 20L, 20L); // Check every second
                
                // We found the right game, no need to continue
                break;
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
                // Bridge egg implementation - track it to create a bridge when it lands
                projectile.setMetadata("bridge_egg", new FixedMetadataValue(plugin, true));
            }
        }
    }
    
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        
        // Check if player is in a game
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            // Prevent interaction with item frames and paintings
            if (entity instanceof ItemFrame || entity instanceof Painting) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onHangingBreak(org.bukkit.event.hanging.HangingBreakEvent event) {
        // Prevent breaking of hanging entities (item frames, paintings) in game worlds
        for (Game game : plugin.getGameManager().getActiveGames().values()) {
            if (event.getEntity().getWorld().getName().equals(game.getArena().getWorldName())) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler
    public void onItemFrameRotate(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame) {
            Player player = event.getPlayer();
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game != null) {
                event.setCancelled(true);
            }
        }
    }
}
