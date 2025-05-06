package org.bcnlab.beaconLabsBW.shop;

import org.bcnlab.beaconLabsBW.game.ultimates.UltimateClass;
import org.bukkit.Material;

/**
 * Represents an ultimate class selection item in the shop
 */
public class UltimateShopItem extends ShopItem {
    
    private final UltimateClass ultimateClass;
    
    /**
     * Create a new ultimate shop item
     * 
     * @param name The name of the item
     * @param material The material to display
     * @param ultimateClass The ultimate class this item represents
     * @param description The description of the ultimate
     */
    public UltimateShopItem(String name, Material material, UltimateClass ultimateClass, String description) {
        super(name, material, 1, null, 0, description);
        this.ultimateClass = ultimateClass;
    }
    
    /**
     * Get the ultimate class this item represents
     * 
     * @return The ultimate class
     */
    public UltimateClass getUltimateClass() {
        return ultimateClass;
    }
    
    /**
     * Ultimate items are free
     */
    public boolean isFree() {
        return true;
    }
}
