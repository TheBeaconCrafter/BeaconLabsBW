package org.bcnlab.beaconLabsBW.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.entity.Player;

/**
 * Utility class to handle armor durability and respawn issues
 */
public class ArmorHandler {

    /**
     * Set an armor item to be unbreakable
     * 
     * @param item The armor item to modify
     * @return The modified item
     */
    public static ItemStack makeUnbreakable(ItemStack item) {
        if (item == null) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            
            // Reset any damage
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(0);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Fix all armor currently worn by a player to be unbreakable
     * 
     * @param player The player whose armor should be fixed
     */
    public static void fixPlayerArmor(Player player) {
        if (player == null) return;
        
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();
        
        // Fix each piece if it exists
        if (helmet != null) {
            player.getInventory().setHelmet(makeUnbreakable(helmet));
        }
        
        if (chestplate != null) {
            player.getInventory().setChestplate(makeUnbreakable(chestplate));
        }
        
        if (leggings != null) {
            player.getInventory().setLeggings(makeUnbreakable(leggings));
        }
        
        if (boots != null) {
            player.getInventory().setBoots(makeUnbreakable(boots));
        }
    }
}
