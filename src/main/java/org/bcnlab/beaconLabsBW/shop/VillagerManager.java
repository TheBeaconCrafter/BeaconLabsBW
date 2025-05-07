package org.bcnlab.beaconLabsBW.shop;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.arena.model.SerializableLocation;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.MerchantInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages shop and team upgrade villagers for BedWars
 */
public class VillagerManager implements Listener {
    
    private final BeaconLabsBW plugin;
    // Store villagers by UUID for faster lookup by entity
    private final Map<UUID, ShopVillager> shopVillagersByUUID = new HashMap<>();
    // Track which arena a villager belongs to (using entity UUID as key)
    private final Map<UUID, String> villagerArenaMap = new HashMap<>();
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
     * Spawn a shop villager from data (e.g., loaded from config).
     * Returns the created ShopVillager instance, which might have an invalid entity if spawning failed.
     */
    public ShopVillager spawnVillagerFromData(ShopVillagerData data, Arena arena) {
        if (data == null || data.getLocation() == null || data.getType() == null || arena == null) return null;
        Location loc = data.getLocation().toBukkitLocation();
        if (loc == null) {
             plugin.getLogger().warning("Failed to get Bukkit location for villager type " + data.getType() + " in arena " + arena.getName());
             return null;
        }

        // Check if a valid managed villager already exists nearby
        ShopVillager existingVillager = findVillagerNearLocation(loc, 1.0);
        if (existingVillager != null) {
            plugin.getLogger().fine("Valid villager already present near location for " + data.getType() + " in arena " + arena.getName() + ". Using existing.");
            // Ensure it's correctly mapped
            if (!shopVillagersByUUID.containsKey(existingVillager.getEntityUUID())) {
                 shopVillagersByUUID.put(existingVillager.getEntityUUID(), existingVillager);
            }
            if (!villagerArenaMap.containsKey(existingVillager.getEntityUUID())) {
                 villagerArenaMap.put(existingVillager.getEntityUUID(), arena.getName());
            }
            return existingVillager;
        }

        // If no valid villager exists nearby, proceed to spawn a new one
        plugin.getLogger().info("Spawning new villager for type " + data.getType() + " in arena " + arena.getName() + " at " + loc);
        ShopVillager newVillager = new ShopVillager(plugin, data.getType(), loc, isEditMode); // This internally calls spawnVillager()
        UUID entityUUID = newVillager.getEntityUUID();
        
        if (entityUUID != null && newVillager.getEntity() != null && newVillager.getEntity().isValid()) {
            shopVillagersByUUID.put(entityUUID, newVillager);
            villagerArenaMap.put(entityUUID, arena.getName());
            plugin.getLogger().info("Successfully spawned and cached villager: " + entityUUID);
            return newVillager;
        } else {
            plugin.getLogger().warning("Failed to spawn or get valid entity for villager type " + data.getType() + " in arena " + arena.getName());
            newVillager.remove(); // Clean up potentially partially spawned entity
            return null; // Indicate failure
        }
    }
    
    // Renamed and slightly adjusted logic
    private ShopVillager findVillagerNearLocation(Location loc, double radius) {
        if (loc == null) return null;
        double radiusSquared = radius * radius;
        for (ShopVillager sv : shopVillagersByUUID.values()) {
            // Check if the cached entity is valid and in the right world/location
            if (sv.getEntity() != null && sv.getEntity().isValid() && 
                sv.getEntity().getWorld().equals(loc.getWorld()) && 
                sv.getEntity().getLocation().distanceSquared(loc) < radiusSquared) {
                return sv;
            }
        }
        return null;
    }
    
    /**
     * Spawn a shop villager via command.
     */
    public ShopVillager spawnShopVillager(ShopVillager.VillagerType type, Location location, String arenaName) {
        // Simplified: Relies on the core ShopVillager constructor and assumes success if UUID is returned.
        ShopVillager villager = new ShopVillager(plugin, type, location, isEditMode);
        UUID entityUUID = villager.getEntityUUID();
        if (entityUUID != null) {
            shopVillagersByUUID.put(entityUUID, villager);
            if (arenaName != null) {
                villagerArenaMap.put(entityUUID, arenaName);
            }
            return villager;
        } else {
             plugin.getLogger().warning("Failed to spawn villager entity via command.");
             villager.remove(); // Clean up
             return null;
        }
    }

    /**
     * Remove a shop villager.
     */
    public void removeShopVillager(ShopVillager villager) {
        if (villager == null) return;
        UUID entityUUID = villager.getEntityUUID();
        if (entityUUID != null) {
            shopVillagersByUUID.remove(entityUUID);
            villagerArenaMap.remove(entityUUID);
        }
        villager.remove(); // Remove the entity itself
    }
    
    /**
     * Remove all villagers for a specific arena.
     */
    public void removeArenaVillagers(String arenaName) {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : villagerArenaMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(arenaName)) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (UUID uuid : toRemove) {
            ShopVillager villager = shopVillagersByUUID.get(uuid);
            if (villager != null) {
                removeShopVillager(villager);
            }
        }
    }

    /**
     * Clear all managed villagers (e.g., on plugin disable).
     */
    public void removeAllVillagers() {
        for (ShopVillager villager : new ArrayList<>(shopVillagersByUUID.values())) {
             removeShopVillager(villager);
        }
        shopVillagersByUUID.clear();
        villagerArenaMap.clear();
    }
    
    /**
     * Toggle edit mode for all villagers
     * 
     * @param editMode Whether edit mode is enabled
     */
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        for (ShopVillager villager : shopVillagersByUUID.values()) {
            villager.setEditing(editMode);
        }
    }
    
    /**
     * Get a shop villager by its entity UUID.
     */
    public ShopVillager getShopVillagerByUUID(UUID uuid) {
        return shopVillagersByUUID.get(uuid);
    }

    /**
     * Get a shop villager by its entity.
     */
    public ShopVillager getShopVillagerByEntity(Entity entity) {
        if (entity == null) return null;
        return shopVillagersByUUID.get(entity.getUniqueId());
    }

    /**
     * Get the arena name associated with a villager.
     */
    public String getVillagerArenaName(ShopVillager villager) {
        if (villager == null || villager.getEntityUUID() == null) return null;
        return villagerArenaMap.get(villager.getEntityUUID());
    }

    /**
     * Find the nearest managed shop villager within a radius.
     */
    public ShopVillager findNearestVillager(Location location, double radius) {
        ShopVillager nearest = null;
        double closestDistSq = radius * radius;

        for (ShopVillager villager : shopVillagersByUUID.values()) {
            if (villager.getLocation() != null && villager.getLocation().getWorld().equals(location.getWorld())) {
                double distSq = villager.getLocation().distanceSquared(location);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    nearest = villager;
                }
            }
        }
        return nearest;
    }
    
    /**
     * Get all shop villagers (returns a copy).
     */
    public List<ShopVillager> getShopVillagers() {
        return new ArrayList<>(shopVillagersByUUID.values());
    }
    
    /**
     * Get shop villagers for a specific arena.
     */
    public List<ShopVillager> getArenaVillagers(String arenaName) {
        List<ShopVillager> result = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : villagerArenaMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(arenaName)) {
                ShopVillager villager = shopVillagersByUUID.get(entry.getKey());
                if (villager != null) {
                    result.add(villager);
                }
            }
        }
        return result;
    }
      /**
     * Handle shop villager interactions
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager clickedVillager)) {
            return;
        }

        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            // Not in a game, or game doesn't support shops perhaps. Edit mode should still work.
            ShopVillager shopVillagerInstanceForEdit = getShopVillagerByEntity(clickedVillager);
            if (shopVillagerInstanceForEdit != null && shopVillagerInstanceForEdit.isEditing()) {
                shopVillagerInstanceForEdit.handleInteraction(player, event);
                event.setCancelled(true);
            }
            return; 
        }

        Arena arena = game.getArena();
        ShopVillagerData foundData = null;
        String foundKey = null;

        // Find configured shop data near the clicked location
        if (arena != null && arena.getShopVillagers() != null) {
            for (Map.Entry<String, ShopVillagerData> entry : arena.getShopVillagers().entrySet()) {
                SerializableLocation storedLoc = entry.getValue().getLocation();
                if (storedLoc != null) {
                    Location worldLoc = storedLoc.toBukkitLocation();
                    // Increased tolerance slightly, check world matches
                    if (worldLoc != null && clickedVillager.getWorld().equals(worldLoc.getWorld()) && 
                        clickedVillager.getLocation().distanceSquared(worldLoc) < 1.5 * 1.5) { // Use 1.5 block radius
                        foundData = entry.getValue();
                        foundKey = entry.getKey();
                        break;
                    }
                }
            }
        }

        if (foundData != null) {
            // We found a configured shop location nearby for this arena.
            event.setCancelled(true); // Prevent default menu since this *should* be our shop

            // Try to get the cached ShopVillager instance using the clicked entity's UUID
            ShopVillager shopVillagerInstance = getShopVillagerByEntity(clickedVillager);

            // Check if the instance is missing, if the entity associated with it is invalid/different,
            // or if the type doesn't match the config (in case an old entity of wrong type is there)
            if (shopVillagerInstance == null || 
                !shopVillagerInstance.getEntityUUID().equals(clickedVillager.getUniqueId()) || 
                !shopVillagerInstance.getEntity().isValid() ||
                 shopVillagerInstance.getType() != foundData.getType()) {
                
                 plugin.getLogger().warning("Villager cache mismatch/invalid for key " + foundKey + ". Clicked: " + clickedVillager.getUniqueId() + ", Type: " + clickedVillager.getVillagerType() + ". Expected Type: " + foundData.getType() + ". Attempting to re-link/respawn.");

                 // Remove the incorrect/invalid entity that was clicked
                 clickedVillager.remove();

                 // Attempt to spawn based on config data (this also updates the cache and returns the instance)
                 shopVillagerInstance = spawnVillagerFromData(foundData, arena);

                 // Verify the new instance and its entity
                 if (shopVillagerInstance == null || shopVillagerInstance.getEntity() == null || !shopVillagerInstance.getEntity().isValid()) {
                     plugin.getLogger().severe("Failed to respawn valid villager for key " + foundKey + ". Interaction aborted.");
                     player.sendMessage(ChatColor.RED + "Error interacting with shop. Please notify an admin. [ERR: RESP]");
                     return; // Event is already cancelled
                 }
                 plugin.getLogger().info("Successfully respawned/re-linked villager for key " + foundKey + ": " + shopVillagerInstance.getEntityUUID());
            }
            
            // Proceed with the interaction using the (potentially newly spawned and) validated instance
            shopVillagerInstance.handleInteraction(player, event);

        } else {
             // Villager not at a known shop location for this arena. Allow default interaction.
             plugin.getLogger().fine("Player " + player.getName() + " interacted with a non-shop villager or one not configured for this arena at " + clickedVillager.getLocation());
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

    /**
     * Prevent merchant inventory from opening for our shop villagers
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory)) {
            return;
        }

        MerchantInventory merchantInventory = (MerchantInventory) event.getInventory();
        if (merchantInventory.getHolder() instanceof Villager) {
            Villager villager = (Villager) merchantInventory.getHolder();
            ShopVillager shopVillager = getShopVillagerByEntity(villager);
            
            if (shopVillager != null) {
                // This is one of our custom shop villagers, prevent opening the default merchant inventory.
                event.setCancelled(true);
                // Optionally, open the custom shop for the player if this event is a result of an interaction
                // However, onPlayerInteractEntity should primarily handle this.
                // If direct inventory opening by other means is possible and should lead to shop, add logic here.
                // For now, just ensure the default one doesn't open.
                if (event.getPlayer() instanceof Player) {
                    Player player = (Player) event.getPlayer();
                    // Check if the player is already in a custom menu, or if we should open one.
                    // For simplicity, we'll rely on onPlayerInteractEntity to open the menu.
                    // This handler primarily prevents the default UI.
                    plugin.getLogger().fine("Prevented default MerchantInventory from opening for ShopVillager for player " + player.getName());
                }
            }
        }
    }

    /**
     * Spawn arena villagers with a delay to ensure chunks are loaded
     * 
     * @param arena The arena to spawn villagers for
     */
    public void spawnArenaVillagersWithDelay(Arena arena) {
        if (arena == null) return;
        
        // Remove existing villagers first
        removeArenaVillagers(arena.getName());
        
        Map<String, ShopVillagerData> villagersMap = arena.getShopVillagers();
        if (villagersMap == null) {
            plugin.getLogger().warning("Arena '" + arena.getName() + "' has no shopVillagers map. Initializing empty map.");
            arena.setShopVillagers(new HashMap<>());
            return;
        }
        
        // Ensure chunks are loaded
        if (arena.getWorldName() != null) {
            final org.bukkit.World world = Bukkit.getWorld(arena.getWorldName());
            if (world != null) {
                // Schedule the villager spawning with a delay
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (ShopVillagerData villagerData : villagersMap.values()) {
                        if (villagerData.getLocation() != null) {
                            // Pre-load the chunk where villager will spawn
                            Location loc = villagerData.getLocation().toBukkitLocation();
                            if (loc != null) {
                                world.loadChunk(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
                                
                                // Spawn after an additional small delay
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    spawnVillagerFromData(villagerData, arena);
                                }, 5L); // 5 tick delay (0.25 seconds)
                            }
                        }
                    }
                    plugin.getLogger().info("Spawned shop villagers for arena " + arena.getName());
                }, 20L); // 1 second delay
            }
        }
    }
}
