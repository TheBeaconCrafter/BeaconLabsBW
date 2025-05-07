package org.bcnlab.beaconLabsBW.shop;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages shop and team upgrade villagers for BedWars
 */
public class VillagerManager implements Listener {
    
    private final BeaconLabsBW plugin;
    private final List<ShopVillager> shopVillagers = new ArrayList<>();
    private final Map<String, List<ShopVillager>> arenaVillagers = new HashMap<>();
    private boolean isEditMode = false;
    
    /**
     * Create a new VillagerManager
     * 
     * @param plugin The plugin instance
     */
    public VillagerManager(BeaconLabsBW plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Spawn a shop villager
     * 
     * @param type The type of villager to spawn
     * @param location The location to spawn the villager
     * @param arenaName The arena name (or null for global)
     * @return The created shop villager
     */
    public ShopVillager spawnShopVillager(ShopVillager.VillagerType type, Location location, String arenaName) {
        ShopVillager villager = new ShopVillager(plugin, type, location, isEditMode);
        shopVillagers.add(villager);
        
        // Add to arena-specific list if arena name is provided
        if (arenaName != null) {
            arenaVillagers.computeIfAbsent(arenaName, k -> new ArrayList<>()).add(villager);
        }
        
        return villager;
    }
    
    /**
     * Remove a shop villager
     * 
     * @param villager The villager to remove
     */
    public void removeShopVillager(ShopVillager villager) {
        // Remove from lists
        shopVillagers.remove(villager);
        for (List<ShopVillager> list : arenaVillagers.values()) {
            list.remove(villager);
        }
        
        // Remove the entity
        villager.remove();
    }
    
    /**
     * Remove all villagers for a specific arena
     * 
     * @param arenaName The arena name
     */
    public void removeArenaVillagers(String arenaName) {
        List<ShopVillager> villagers = arenaVillagers.get(arenaName);
        if (villagers != null) {
            for (ShopVillager villager : new ArrayList<>(villagers)) {
                removeShopVillager(villager);
            }
            arenaVillagers.remove(arenaName);
        }
    }
    
    /**
     * Toggle edit mode for all villagers
     * 
     * @param editMode Whether edit mode is enabled
     */
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        
        // Update all shop villagers
        for (ShopVillager villager : shopVillagers) {
            villager.setEditing(editMode);
        }
    }
    
    /**
     * Get a shop villager by its entity
     * 
     * @param entity The entity to check
     * @return The shop villager, or null if not found
     */
    public ShopVillager getShopVillagerByEntity(Entity entity) {
        for (ShopVillager villager : shopVillagers) {
            if (villager.isEntity(entity)) {
                return villager;
            }
        }
        return null;
    }
    
    /**
     * Get all shop villagers
     * 
     * @return List of all shop villagers
     */
    public List<ShopVillager> getShopVillagers() {
        return new ArrayList<>(shopVillagers);
    }
    
    /**
     * Get shop villagers for a specific arena
     * 
     * @param arenaName The arena name
     * @return List of shop villagers for the arena
     */
    public List<ShopVillager> getArenaVillagers(String arenaName) {
        return arenaVillagers.getOrDefault(arenaName, new ArrayList<>());
    }
    
    /**
     * Handle shop villager interactions
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) {
            return;
        }
        
        // Check if this is one of our shop villagers
        ShopVillager shopVillager = getShopVillagerByEntity(event.getRightClicked());
        if (shopVillager != null) {
            Player player = event.getPlayer();
            
            // Let the shop villager handle the interaction
            shopVillager.handleInteraction(player, event);
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle shop villager damage (prevent it)
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Villager)) {
            return;
        }
        
        // Check if this is one of our shop villagers
        ShopVillager shopVillager = getShopVillagerByEntity(event.getEntity());
        if (shopVillager != null) {
            // Let the shop villager handle the damage
            shopVillager.handleDamage(event);
        }
    }
}
