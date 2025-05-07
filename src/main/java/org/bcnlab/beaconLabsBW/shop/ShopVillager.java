package org.bcnlab.beaconLabsBW.shop;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Represents a shop villager entity in BedWars
 */
public class ShopVillager implements Listener {
    
    private final BeaconLabsBW plugin;
    private final String name;
    private final VillagerType type;
    private final Location location;
    private Villager entity;
    private boolean isEditing;
    
    /**
     * Type of shop villager
     */
    public enum VillagerType {
        ITEM_SHOP("Item Shop"),
        TEAM_UPGRADES("Team Upgrades");
        
        private final String displayName;
        
        VillagerType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Create a new shop villager
     * 
     * @param plugin The plugin instance
     * @param type The type of villager
     * @param location The location to spawn the villager
     * @param isEditing Whether the map is in edit mode
     */
    public ShopVillager(BeaconLabsBW plugin, VillagerType type, Location location, boolean isEditing) {
        this.plugin = plugin;
        this.type = type;
        this.name = getFormattedName();
        this.location = location;
        this.isEditing = isEditing;
        
        // Spawn the villager
        spawnVillager();
    }
    
    /**
     * Get the formatted name for this villager
     * 
     * @return The formatted display name
     */
    private String getFormattedName() {
        ChatColor color = (type == VillagerType.ITEM_SHOP) ? ChatColor.GREEN : ChatColor.AQUA;
        return color + type.getDisplayName();
    }
    
    /**
     * Spawn the villager at the location
     */
    private void spawnVillager() {
        World world = location.getWorld();
        if (world == null) return;
        
        // Create villager entity
        this.entity = (Villager) world.spawnEntity(location, EntityType.VILLAGER);
        
        // Set properties
        entity.setCustomName(name);
        entity.setCustomNameVisible(true);
        entity.setAI(false); // Always disable AI to prevent movement
        entity.setSilent(true); // Make villager silent
        entity.setPersistent(true);
        entity.setInvulnerable(true);
        entity.setCanPickupItems(false); // Prevent picking up items
        
        // Set villager type based on role
        if (type == VillagerType.ITEM_SHOP) {
            entity.setProfession(Villager.Profession.TOOLSMITH);
        } else {
            entity.setProfession(Villager.Profession.LIBRARIAN);
        }
        
        // Add visual effects
        if (isEditing) {
            // In edit mode, give a visual indicator
            entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        }
    }
    
    /**
     * Handle right-click interaction with the villager
     * 
     * @param player The player who clicked
     * @param event The interaction event
     */    
    public void handleInteraction(Player player, PlayerInteractEntityEvent event) {
        plugin.getLogger().info("[ShopVillager] handleInteraction called by " + player.getName() + " for type: " + type.getDisplayName());

        // Prevent villager interaction while in edit mode
        if (isEditing) {
            plugin.getLogger().info("[ShopVillager] Interaction cancelled: isEditing is TRUE");
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This shop NPC is currently in edit mode.");
            return;
        }
        plugin.getLogger().info("[ShopVillager] isEditing is FALSE");

        // Get player's game
        org.bcnlab.beaconLabsBW.game.Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            plugin.getLogger().info("[ShopVillager] Interaction cancelled: Player is not in a game.");
            player.sendMessage(ChatColor.RED + "You must be in a game to use this!");
            return;
        }
        plugin.getLogger().info("[ShopVillager] Player is in game: " + game.getGameId());

        // Open appropriate menu
        if (type == VillagerType.ITEM_SHOP) {
            plugin.getLogger().info("[ShopVillager] Opening ITEM_SHOP for " + player.getName());
            plugin.getShopManager().openCategoryMenu(player, ShopCategory.QUICK_BUY);
        } else { // TEAM_UPGRADES
            plugin.getLogger().info("[ShopVillager] Opening TEAM_UPGRADES for " + player.getName());
            plugin.getTeamUpgradeManager().openUpgradesMenu(player, game);
        }
    }
    
    /**
     * Handle damage to the villager (cancel it)
     * 
     * @param event The damage event
     */
    public void handleDamage(EntityDamageEvent event) {
        event.setCancelled(true);
    }
    
    /**
     * Check if this shop villager matches the given entity
     * 
     * @param entity The entity to check
     * @return True if it's this shop villager
     */
    public boolean isEntity(Entity entity) {
        return this.entity != null && this.entity.equals(entity);
    }
    
    /**
     * Set editing mode
     * 
     * @param editing Whether the villager is in edit mode
     */
    public void setEditing(boolean editing) {
        this.isEditing = editing;
        
        // Update AI state
        if (entity != null && entity.isValid()) {
            entity.setAI(false); // Always disable AI
            
            if (editing) {
                // Add glowing effect in edit mode
                entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
            } else {
                // Remove glowing effect in game mode
                entity.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
    }
    
    /**
     * Get the entity for this shop villager
     * 
     * @return The villager entity
     */
    public Villager getEntity() {
        return entity;
    }
    
    /**
     * Get the location of this shop villager
     * 
     * @return The location
     */
    public Location getLocation() {
        return location;
    }
    
    /**
     * Get the type of this shop villager
     * 
     * @return The villager type
     */
    public VillagerType getType() {
        return type;
    }
    
    /**
     * Remove this shop villager
     */
    public void remove() {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }
}
