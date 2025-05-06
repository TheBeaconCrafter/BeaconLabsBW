package org.bcnlab.beaconLabsBW.game.ultimates;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Represents the different ultimate classes that players can choose in Ultimates mode
 */
public enum UltimateClass {
    SWORDSMAN(
        "Swordsman",
        ChatColor.RED,
        Material.IRON_SWORD,
        "Enables players to fling themselves forward and damage enemies",
        "Dash forward at high speed, knocking back and damaging enemies in your path"
    ),
    HEALER(
        "Healer", 
        ChatColor.GREEN,
        Material.GOLDEN_APPLE,
        "Provides healing to teammates within a small radius",
        "Keep your team alive with periodic healing auras"
    ),
    FROZO(
        "Frozo", 
        ChatColor.AQUA,
        Material.PACKED_ICE,
        "Allows players to slow enemies down in a small radius",
        "Freeze your enemies with ice magic, slowing their movement"
    ),
    BUILDER(
        "Builder", 
        ChatColor.YELLOW,
        Material.BRICKS,
        "Grants the ability to easily build with wool including bridges and walls",
        "Fast bridge building and passive wool generation"
    ),
    GATHERER(
        "Gatherer", 
        ChatColor.LIGHT_PURPLE,
        Material.EMERALD,
        "Gives a chance to duplicate emeralds and diamonds from generators",
        "Double resources and get free diamond upgrades when your bed is destroyed"
    ),
    DEMOLITION(
        "Demolition", 
        ChatColor.DARK_RED,
        Material.TNT,
        "Allows players to burn down connected wool and drops TNT upon death",
        "Destroy enemy bases with fire and explosives"
    ),
    KANGAROO(
        "Kangaroo", 
        ChatColor.GOLD,
        Material.RABBIT_FOOT,
        "Enables double jumping and gives magic milk upon breaking a bed",
        "Jump high and keep 50% of your resources when you die"
    );
    
    private final String displayName;
    private final ChatColor color;
    private final Material icon;
    private final String description;
    private final String ability;
    
    UltimateClass(String displayName, ChatColor color, Material icon, String description, String ability) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.description = description;
        this.ability = ability;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getAbility() {
        return ability;
    }
    
    public String getFormattedName() {
        return color + displayName;
    }
      /**
     * Get a random ultimate class
     * 
     * @return A randomly selected ultimate class
     */
    public static UltimateClass getRandomClass() {
        UltimateClass[] values = UltimateClass.values();
        int randomIndex = (int) (Math.random() * values.length);
        return values[randomIndex];
    }
}
