package org.bcnlab.beaconLabsBW.listeners;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Handles inventory-related events for BedWars
 */
public class InventoryListener implements Listener {
    
    private final BeaconLabsBW plugin;
    
    public InventoryListener(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check if this is a team upgrades menu
        if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "Team Upgrades")) {
            event.setCancelled(true);
            
            // Check if player is in a game
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game == null) return;
            
            // Handle the purchase
            if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize()) {
                plugin.getTeamUpgradeManager().handlePurchase(player, event.getRawSlot(), game);
            }
        }
        
        // Check if this is the shop menu
        else if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "BedWars Shop")) {
            event.setCancelled(true);
            
            // Check if player is in a game
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game == null) return;
              // Handle the purchase through ShopManager
            if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize()) {
                plugin.getShopManager().handlePurchase(player, event.getRawSlot());
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // Check if player is in a game
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) return;
        
        // Apply team upgrades again when closing the inventory
        // This ensures all upgrades are applied correctly when the player closes the menu
        String team = game.getPlayerTeam(player);
        if (team != null) {
            applyTeamUpgrades(player, game);
        }
    }
    
    /**
     * Apply team upgrades to a player
     * 
     * @param player The player
     * @param game The game
     */
    private void applyTeamUpgrades(Player player, Game game) {
        String teamName = game.getPlayerTeam(player);
        if (teamName == null) return;
        
        for (org.bcnlab.beaconLabsBW.shop.TeamUpgrade.UpgradeType type : 
             org.bcnlab.beaconLabsBW.shop.TeamUpgrade.UpgradeType.values()) {
            
            int level = plugin.getTeamUpgradeManager().getUpgradeLevel(teamName, type);
            if (level > 0) {
                game.applyTeamUpgrade(player, type, level);
            }
        }
    }
}
