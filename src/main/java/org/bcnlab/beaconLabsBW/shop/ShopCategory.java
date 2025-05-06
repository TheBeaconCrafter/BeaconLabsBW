package org.bcnlab.beaconLabsBW.shop;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Represents categories in the BedWars shop
 */
@Getter
public enum ShopCategory {
    QUICK_BUY("Quick Buy", Material.NETHER_STAR),
    BLOCKS("Blocks", Material.TERRACOTTA),
    MELEE("Melee", Material.GOLDEN_SWORD),
    ARMOR("Armor", Material.CHAINMAIL_CHESTPLATE),
    TOOLS("Tools", Material.STONE_PICKAXE),
    RANGED("Ranged", Material.BOW),
    POTIONS("Potions", Material.BREWING_STAND),
    UTILITY("Utility", Material.TNT),
    ULTIMATES("Ultimates", Material.BEACON);
    
    private final String displayName;
    private final Material icon;
    
    ShopCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }
}
