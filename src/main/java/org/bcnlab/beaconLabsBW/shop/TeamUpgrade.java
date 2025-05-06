package org.bcnlab.beaconLabsBW.shop;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Represents a team upgrade in BedWars
 */
@Getter
public class TeamUpgrade {
    
    private final String name;
    private final String description;
    private final Material currency;
    private final int[] tierCosts;
    private final int maxLevel;
    private final UpgradeType type;
    
    /**
     * Create a new team upgrade
     * 
     * @param name Upgrade name
     * @param description Upgrade description 
     * @param currency Currency material
     * @param tierCosts Cost for each tier
     * @param type Upgrade type
     */
    public TeamUpgrade(String name, String description, Material currency, int[] tierCosts, UpgradeType type) {
        this.name = name;
        this.description = description;
        this.currency = currency;
        this.tierCosts = tierCosts;
        this.maxLevel = tierCosts.length;
        this.type = type;
    }
    
    /**
     * Get the upgrade effect description for a specific tier
     * 
     * @param tier The tier (1-based)
     * @return Effect description
     */
    public String getEffectDescription(int tier) {
        if (tier < 1 || tier > maxLevel) {
            return "Invalid tier";
        }
        
        return switch (type) {
            case SHARPNESS -> "Sharpness " + tier;
            case PROTECTION -> "Protection " + tier;
            case HASTE -> "Haste " + tier;
            case FORGE -> switch (tier) {
                case 1 -> "+50% Resources";
                case 2 -> "+100% Resources";
                case 3 -> "+200% Resources";
                default -> "+50% Resources";
            };
            case HEALING -> switch (tier) {
                case 1 -> "Regeneration I";
                case 2 -> "Regeneration II";
                default -> "Regeneration I";
            };
            case TRAP -> "Team Base Defense";
        };
    }
    
    /**
     * Get the cost for a specific tier
     * 
     * @param tier The tier (1-based)
     * @return Cost in the currency
     */
    public int getCost(int tier) {
        if (tier < 1 || tier > maxLevel) {
            return 0;
        }
        return tierCosts[tier - 1];
    }
    
    /**
     * Types of team upgrades
     */
    public enum UpgradeType {
        SHARPNESS,
        PROTECTION,
        HASTE,
        FORGE,
        HEALING,
        TRAP
    }
}
