package org.bcnlab.beaconLabsBW.shop;

import lombok.Data;
import org.bukkit.Material;

/**
 * Represents an item in the BedWars shop
 */
@Data
public class ShopItem {
    
    private final String name;
    private final Material material;
    private final int amount;
    private final Material currency;
    private final int cost;
    private final String description;
    private ShopCategory category;
    
    /**
     * Check if the item is free
     * 
     * @return True if the item is free
     */
    public boolean isFree() {
        return false;
    }
    
    /**
     * Create a new shop item
     * 
     * @param name Item display name
     * @param material Item material
     * @param amount Item amount
     * @param currency Currency material (IRON_INGOT, GOLD_INGOT, or EMERALD)
     * @param cost Item cost
     * @param description Item description
     */
    public ShopItem(String name, Material material, int amount, Material currency, int cost, String description) {
        this.name = name;
        this.material = material;
        this.amount = amount;
        this.currency = currency;
        this.cost = cost;
        this.description = description;
    }
}
