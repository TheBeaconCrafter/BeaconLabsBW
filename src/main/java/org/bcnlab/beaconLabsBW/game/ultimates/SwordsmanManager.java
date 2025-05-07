package org.bcnlab.beaconLabsBW.game.ultimates;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Swordsman-specific abilities and teleport functionality
 */
public class SwordsmanManager {
    private final BeaconLabsBW plugin;
    // Track swordsman teleport back locations and expiration times
    private final Map<UUID, org.bukkit.Location> swordsmanTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, Long> teleportExpirationTimes = new ConcurrentHashMap<>();
    // How long a teleport opportunity lasts (in milliseconds)
    private static final long TELEPORT_EXPIRATION_TIME = 8000; // 8 seconds

    public SwordsmanManager(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }

    /**
     * Process a swordsman dash ability activation
     *
     * @param player The player
     * @return true if the dash was activated, false if teleported back
     */
    public boolean processSwordsmanDash(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Check if player has the option to teleport back
        if (hasTeleportOption(playerId)) {
            // This is a second click - teleport back
            teleportPlayerBack(player);
            return false;
        }
        
        // This is a first click - perform the dash
        performDash(player);
        return true;
    }
    
    /**
     * Check if a player has an active teleport option
     *
     * @param playerId The player UUID
     * @return true if teleport option is available
     */
    public boolean hasTeleportOption(UUID playerId) {
        // Check if the player has a stored location
        if (!swordsmanTeleports.containsKey(playerId)) {
            return false;
        }
        
        // Check if the teleport option has expired
        long expirationTime = teleportExpirationTimes.getOrDefault(playerId, 0L);
        if (System.currentTimeMillis() > expirationTime) {
            // Expired - clean up
            swordsmanTeleports.remove(playerId);
            teleportExpirationTimes.remove(playerId);
            return false;
        }
        
        // Valid teleport option exists
        return true;
    }
    
    /**
     * Perform the dash part of the swordsman ability
     *
     * @param player The player
     */
    private void performDash(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Store original location for teleport-back feature
        final org.bukkit.Location originalLocation = player.getLocation().clone();
        
        // Calculate dash direction
        Vector direction = player.getLocation().getDirection().normalize().multiply(5.0).setY(0.3);
        
        // Apply dash
        player.setVelocity(direction);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1));
        
        // Damage nearby players in the path
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (target != player && target.getWorld() == player.getWorld() &&
                    target.getLocation().distance(player.getLocation()) < 4.0) {
                    target.damage(7.0, player);
                    target.setVelocity(direction.clone().multiply(0.8));
                    
                    // Add hit effect
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                        15, 0.4, 0.4, 0.4, 0.1);
                }
            }
        }, 5L);
        
        // Visual effects for dash
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 
                                      10, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 
                                      15, 0.2, 0.2, 0.2, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);

        // Store the original location for teleport-back feature
        storeTeleportLocation(playerId, originalLocation);
        
        // Notify player about teleport-back option
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isDead() && hasTeleportOption(playerId)) {
                player.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "Right-click again with your sword to teleport back!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            }
        }, 10L); // 0.5 seconds later
    }
    
    /**
     * Store a teleport location for a player
     *
     * @param playerId The player UUID
     * @param location The location to store
     */
    private void storeTeleportLocation(UUID playerId, org.bukkit.Location location) {
        swordsmanTeleports.put(playerId, location);
        teleportExpirationTimes.put(playerId, System.currentTimeMillis() + TELEPORT_EXPIRATION_TIME);
        
        // Schedule task to notify about expiration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && hasTeleportOption(playerId)) {
                clearTeleportOption(playerId);
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Your teleport opportunity has expired!");
            }
        }, 20L * 8); // 8 seconds
    }
    
    /**
     * Teleport a player back to their original location
     *
     * @param player The player
     */
    private void teleportPlayerBack(Player player) {
        UUID playerId = player.getUniqueId();
        org.bukkit.Location originalLocation = swordsmanTeleports.get(playerId);
        
        // Clear stored data
        clearTeleportOption(playerId);
        
        // Prepare player with brief invulnerability before teleport
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 4, false, false));
        
        // Add pre-teleport effect
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 
                                      30, 0.5, 1.0, 0.5, 0.1);
        
        // Teleport player back
        player.teleport(originalLocation);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 4, false, false));
        
        // Add teleport effects
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0),
                                     40, 0.5, 1.0, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "You've been teleported back!");
    }
    
    /**
     * Clear a player's teleport option
     *
     * @param playerId The player UUID
     */
    public void clearTeleportOption(UUID playerId) {
        swordsmanTeleports.remove(playerId);
        teleportExpirationTimes.remove(playerId);
    }
}
