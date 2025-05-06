package org.bcnlab.beaconLabsBW.listeners;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.arena.model.SerializableLocation;
import org.bcnlab.beaconLabsBW.arena.model.TeamData;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bcnlab.beaconLabsBW.shop.ShopItem;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles player-related events for BedWars
 */
public class PlayerListener implements Listener {
    
    private final BeaconLabsBW plugin;
    
    public PlayerListener(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if there's an auto-start game running
        if (plugin.getConfigManager().isAutoStartEnabled()) {
            for (Game game : plugin.getGameManager().getActiveGames().values()) {
                if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) {
                    // Auto join the player after a short delay
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        plugin.getGameManager().addPlayerToGame(player, game);
                    }, 10L);
                    break;
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Remove from any games
        plugin.getGameManager().removePlayerFromGame(player);
        
        // Exit edit mode if they were editing
        if (plugin.getArenaManager().isEditing(player)) {
            plugin.getArenaManager().stopEditing(player);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            // Clear drops in the game
            event.getDrops().clear();
            
            // No death message in the game
            event.setDeathMessage(null);
            
            // Handle game-specific death logic
            game.handlePlayerDeath(player, killer);
            
            // Skip death screen
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.spigot().respawn(), 2L);
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null) {
            if (game.isSpectator(player)) {
                // Respawn at spectator spawn
                SerializableLocation spectatorLoc = game.getArena().getSpectatorSpawn();
                if (spectatorLoc != null) {
                    Location location = spectatorLoc.toBukkitLocation();
                    if (location != null) {
                        event.setRespawnLocation(location);
                    }
                }
            } else {
                // Respawn at team spawn if bed exists
                String team = game.getPlayerTeam(player);
                if (team != null) {
                    TeamData teamData = game.getArena().getTeam(team);
                    if (teamData != null && teamData.getSpawnLocation() != null) {
                        Location spawnLoc = teamData.getSpawnLocation().toBukkitLocation();
                        if (spawnLoc != null) {
                            event.setRespawnLocation(spawnLoc);
                        }
                    }
                }
            }
        }
    }
      @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        // Check for right-clicking or left-clicking beds 
        if (block != null && 
            (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) && 
            block.getType().name().contains("BED")) {
            
            // Check if the player is in a game
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game != null && game.getState() == GameState.RUNNING) {
                // Handle bed breaking logic in game
                handleBedBreak(player, block, game);
                
                // Since bed is part of the game, don't run code below
                return;
            }
        }
        
        // Handle shop keeper interaction
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.EMERALD) {
                // Check if player is in a game
                Game game = plugin.getGameManager().getPlayerGame(player);
                if (game != null && game.getState() == GameState.RUNNING) {
                    // Open the shop
                    plugin.getShopManager().openShop(player);
                    event.setCancelled(true);
                }
            }
        }
    }    // Track last interaction time to prevent spam clicks on beds
    private final java.util.Map<java.util.UUID, Long> lastBedInteraction = new java.util.HashMap<>();
    private static final long BED_INTERACTION_COOLDOWN = 500; // 0.5 seconds
    
    private void handleBedBreak(Player player, Block bed, Game game) {
        if (bed == null || game == null) return;
        
        // Anti-spam check
        long now = System.currentTimeMillis();
        long lastTime = lastBedInteraction.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastTime < BED_INTERACTION_COOLDOWN) {
            return; // Silently ignore rapid clicks
        }
        lastBedInteraction.put(player.getUniqueId(), now);
        
        // Get player's team
        String playerTeam = game.getPlayerTeam(player);
        if (playerTeam == null) return;
        
        // Check whose bed this is
        for (String teamName : game.getArena().getTeams().keySet()) {
            TeamData team = game.getArena().getTeam(teamName);
            if (team == null || team.getBedLocation() == null) continue;
            
            Location bedLoc = team.getBedLocation().toBukkitLocation();
            if (bedLoc != null && isSameBed(bed.getLocation(), bedLoc)) {
                // Can't break your own bed
                if (teamName.equals(playerTeam)) {
                    MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou cannot break your own bed!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return;
                }
                
                // Handle bed break in the game
                game.handleBedBreak(teamName, player);
                return;
            }
        }
    }
    
    private boolean isSameBed(Location loc1, Location loc2) {
        if (!loc1.getWorld().equals(loc2.getWorld())) return false;
        
        // Check if the two locations are within 1 block of each other (bed is two blocks)
        return Math.abs(loc1.getBlockX() - loc2.getBlockX()) <= 1 && 
               Math.abs(loc1.getBlockY() - loc2.getBlockY()) <= 1 && 
               Math.abs(loc1.getBlockZ() - loc2.getBlockZ()) <= 1;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("§8BedWars Shop:")) {
            // Handle shop interaction
            event.setCancelled(true);
            
            if (event.getWhoClicked() instanceof Player player) {
                plugin.getShopManager().handlePurchase(player, event.getRawSlot());
            }
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null) {
            // Don't allow dropping armor
            ItemStack item = event.getItemDrop().getItemStack();
            if (isArmor(item.getType())) {
                event.setCancelled(true);
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou cannot drop armor!");
            }
        }
    }
    
    private boolean isArmor(Material material) {
        return material.name().contains("HELMET") ||
               material.name().contains("CHESTPLATE") ||
               material.name().contains("LEGGINGS") ||
               material.name().contains("BOOTS");
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            
            if (game != null) {
                // Disable damage in lobby
                if (game.getState() != GameState.RUNNING) {
                    event.setCancelled(true);
                    return;
                }
                
                // Prevent spectators from taking damage
                if (game.isSpectator(player)) {
                    event.setCancelled(true);
                    return;
                }
                
                // Prevent fall damage from excessive heights
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL && 
                    event.getDamage() > 10) {
                    event.setDamage(10); // Cap fall damage
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player defender) {
            Game game = plugin.getGameManager().getPlayerGame(attacker);
            
            if (game != null && game == plugin.getGameManager().getPlayerGame(defender)) {
                // Disable PvP in non-running state
                if (game.getState() != GameState.RUNNING) {
                    event.setCancelled(true);
                    return;
                }
                
                // Disable damage to teammates
                String attackerTeam = game.getPlayerTeam(attacker);
                String defenderTeam = game.getPlayerTeam(defender);
                
                if (attackerTeam != null && attackerTeam.equals(defenderTeam)) {
                    event.setCancelled(true);
                    return;
                }
                
                // Spectators can't attack or be attacked
                if (game.isSpectator(attacker) || game.isSpectator(defender)) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null) {
            // Only allow certain commands during a game
            String cmd = event.getMessage().toLowerCase().split(" ")[0];
            
            if (cmd.equalsIgnoreCase("/bw") || 
                cmd.equalsIgnoreCase("/labsbw") || 
                cmd.equalsIgnoreCase("/bedwars") ||
                player.hasPermission("bedwars.admin")) {
                // Allow BedWars commands and admin commands
                return;
            }
            
            // Block other commands
            event.setCancelled(true);
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou cannot use commands during a game! Use &e/bw leave &cto quit.");
        }
    }
}
