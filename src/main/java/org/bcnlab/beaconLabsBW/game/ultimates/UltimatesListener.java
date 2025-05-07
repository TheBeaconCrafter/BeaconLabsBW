package org.bcnlab.beaconLabsBW.game.ultimates;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles events related to ultimate abilities
 */
public class UltimatesListener implements Listener {
    
    private final BeaconLabsBW plugin;
    
    public UltimatesListener(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        // Check if player is in a game with ultimates enabled
        if (game == null || game.getGameMode() != GameMode.ULTIMATES) {
            return;
        }
        
        // Check if right-click action
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Get player's ultimate class
        UltimateClass playerClass = game.getPlayerUltimateClass(player.getUniqueId());
        if (playerClass == null) {
            return;
        }
        
        // Check if the item is the ultimate ability item
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        
        // Process abilities based on item and class
        if (playerClass == UltimateClass.SWORDSMAN && item.getType().name().contains("SWORD")) {
            // Activate the ability
            plugin.getUltimatesManager().activateUltimate(player, playerClass);
            event.setCancelled(true);
        } else if ((playerClass == UltimateClass.HEALER && item.getType() == Material.GOLDEN_APPLE) ||
            (playerClass == UltimateClass.FROZO && item.getType() == Material.PACKED_ICE) ||
            (playerClass == UltimateClass.GATHERER && item.getType() == Material.ENDER_CHEST) ||
            (playerClass == UltimateClass.DEMOLITION && item.getType() == Material.FIRE_CHARGE)) {
            
            // Activate the ability
            plugin.getUltimatesManager().activateUltimate(player, playerClass);
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        // Check if player is in a game with ultimates enabled
        if (game == null || game.getGameMode() != GameMode.ULTIMATES) {
            return;
        }
        
        // Check if player is a Builder and placed wool
        UltimateClass playerClass = game.getPlayerUltimateClass(player.getUniqueId());
        if (playerClass == UltimateClass.BUILDER && event.getBlock().getType().name().contains("WOOL")) {
            // Check if player is holding the Builder's tool
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            
            if (mainHand.getType() == Material.BRICKS || offHand.getType() == Material.BRICKS) {
                // Process fast bridge building
                plugin.getUltimatesManager().handleFastBridge(player, event.getBlock(), event.getBlockAgainst().getFace(event.getBlock()));
            }
        }
    }
    
    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        // Check for Kangaroo double jump
        if (game != null && game.getGameMode() == GameMode.ULTIMATES) {
            UltimateClass playerClass = game.getPlayerUltimateClass(player.getUniqueId());
            if (playerClass == UltimateClass.KANGAROO && player.getGameMode().name().contains("SURVIVAL")) {
                // Check if player is on ground or has recently jumped
                if (!player.isOnGround() && !player.isFlying()) {
                    // Process kangaroo double jump
                    if (plugin.getUltimatesManager().processKangarooJump(player)) {
                        event.setCancelled(true);
                        player.setFlying(false);
                        player.setAllowFlight(false);
                          // Start cooldown display on XP bar (10 seconds)
                        plugin.getUltimatesManager().startCooldownDisplay(player, 10);
                        
                        // Schedule flight re-enable
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline() && !player.isDead() && game.isPlayerInGame(player) && !game.isSpectator(player)) {
                                player.setAllowFlight(true);
                            }
                        }, 10 * 20L); // 10 seconds
                    }
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        // Check if player is in a game with ultimates enabled
        if (game == null || game.getGameMode() != GameMode.ULTIMATES) {
            return;
        }
        
        // Get player's ultimate class
        UltimateClass playerClass = game.getPlayerUltimateClass(player.getUniqueId());
        if (playerClass == null) {
            return;
        }
        
        // Handle class-specific death effects
        switch (playerClass) {
            case DEMOLITION -> plugin.getUltimatesManager().handleDemolitionDeath(player);
            case KANGAROO -> {
                // Process drops to potentially save some resources
                List<ItemStack> newDrops = plugin.getUltimatesManager().processKangarooDeathItems(
                    player, event.getDrops());
                
                // Replace drops with processed ones
                if (newDrops != null && !newDrops.isEmpty()) {
                    event.getDrops().clear();
                    event.getDrops().addAll(newDrops);
                }
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        // Check if player is in a game with ultimates enabled
        if (game == null || game.getGameMode() != GameMode.ULTIMATES) {
            return;
        }
        
        // Check if it's a bed being broken
        Block block = event.getBlock();
        if (block.getType().name().contains("BED")) {
            // Get player's ultimate class
            UltimateClass playerClass = game.getPlayerUltimateClass(player.getUniqueId());
            if (playerClass == UltimateClass.KANGAROO) {
                // Give magic milk for breaking a bed
                plugin.getUltimatesManager().giveMagicMilk(player);
            }
        }
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        // Check if player is in a game with ultimates enabled
        if (game == null || game.getGameMode() != GameMode.ULTIMATES) {
            return;
        }
        
        // Get player's ultimate class
        UltimateClass playerClass = game.getPlayerUltimateClass(player.getUniqueId());
        if (playerClass == null) {
            return;
        }
        
        // Process class-specific damage events
        if (playerClass == UltimateClass.KANGAROO && 
            event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            // Reduce fall damage by 50% for Kangaroo class
            event.setDamage(event.getDamage() * 0.5);
        }
    }
}
