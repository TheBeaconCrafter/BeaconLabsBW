package org.bcnlab.beaconLabsBW.shop;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameMode;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;

import org.bukkit.entity.Player;

/**
 * Handles ultimate class selections in the shop
 */
public class UltimateClassHandler {
    
    /**
     * Handle selection of an ultimate class
     * 
     * @param player The player
     * @param ultimateItem The ultimate class item
     * @param plugin The plugin instance
     */
    public static void handleUltimateSelection(Player player, UltimateShopItem ultimateItem, BeaconLabsBW plugin) {
        // Check if player is in a game
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in a game to select an ultimate class.");
            return;
        }
        
        // Check if game is in ultimates mode
        if (game.getGameMode() != GameMode.ULTIMATES) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUltimates mode is not enabled in this game.");
            player.closeInventory();
            return;
        }
        
        // Set the player's ultimate class
        game.setPlayerUltimateClass(player.getUniqueId(), ultimateItem.getUltimateClass());
        
        // Show confirmation message with colored class name
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aYou selected the " + 
            ultimateItem.getUltimateClass().getFormattedName() + " &aultimate class!");
        
        // Play confirmation sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        
        // Close inventory after selection
        player.closeInventory();
    }
}
