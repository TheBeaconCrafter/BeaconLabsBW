package org.bcnlab.beaconLabsBW.shop;

import lombok.Getter;
import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages team upgrades in BedWars
 */
public class TeamUpgradeManager {
    
    private final BeaconLabsBW plugin;
    private final List<TeamUpgrade> availableUpgrades = new ArrayList<>();
    
    // Track team upgrade levels - team name -> upgrade type -> level
    private final Map<String, Map<TeamUpgrade.UpgradeType, Integer>> teamUpgrades = new ConcurrentHashMap<>();
    
    /**
     * Create a new TeamUpgradeManager
     * 
     * @param plugin The plugin instance
     */
    public TeamUpgradeManager(BeaconLabsBW plugin) {
        this.plugin = plugin;
        registerUpgrades();
    }
    
    /**
     * Register available upgrades
     */
    private void registerUpgrades() {
        // Sharpness upgrade
        availableUpgrades.add(new TeamUpgrade(
            "Sharpened Swords",
            "Your team's swords gain Sharpness",
            Material.DIAMOND,
            new int[]{4, 8, 12},
            TeamUpgrade.UpgradeType.SHARPNESS
        ));
        
        // Protection upgrade
        availableUpgrades.add(new TeamUpgrade(
            "Reinforced Armor",
            "Your team's armor gains Protection",
            Material.DIAMOND,
            new int[]{5, 10, 20},
            TeamUpgrade.UpgradeType.PROTECTION
        ));
        
        // Forge upgrade
        availableUpgrades.add(new TeamUpgrade(
            "Iron Forge",
            "Increases resource generation speed",
            Material.DIAMOND,
            new int[]{4, 8, 16},
            TeamUpgrade.UpgradeType.FORGE
        ));
        
        // Haste upgrade
        availableUpgrades.add(new TeamUpgrade(
            "Maniac Miner",
            "All players on your team get Haste",
            Material.DIAMOND,
            new int[]{2, 4},
            TeamUpgrade.UpgradeType.HASTE
        ));
        
        // Healing upgrade
        availableUpgrades.add(new TeamUpgrade(
            "Heal Pool",
            "Gain regeneration while at your base",
            Material.DIAMOND,
            new int[]{3, 6},
            TeamUpgrade.UpgradeType.HEALING
        ));
        
        // Trap upgrade
        availableUpgrades.add(new TeamUpgrade(
            "Trap",
            "Sets a trap that reveals invisible players",
            Material.DIAMOND,
            new int[]{2},
            TeamUpgrade.UpgradeType.TRAP
        ));
    }
    
    /**
     * Open the team upgrades menu for a player
     * 
     * @param player The player
     * @param game The game they're in
     */
    public void openUpgradesMenu(Player player, Game game) {
        String teamName = game.getPlayerTeam(player);
        if (teamName == null) return;
        
        // Create inventory
        Inventory inventory = Bukkit.createInventory(
            null,
            27,
            ChatColor.DARK_GRAY + "Team Upgrades"
        );
        
        // Get team upgrade levels
        Map<TeamUpgrade.UpgradeType, Integer> upgradeLevels = teamUpgrades
            .computeIfAbsent(teamName, k -> new ConcurrentHashMap<>());
        
        // Add upgrades
        int slot = 10;
        for (TeamUpgrade upgrade : availableUpgrades) {
            inventory.setItem(slot++, createUpgradeItem(upgrade, upgradeLevels.getOrDefault(upgrade.getType(), 0)));
        }
        
        // Add separator
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, createGlassPane());
        }
        for (int i = 18; i < 27; i++) {
            inventory.setItem(i, createGlassPane());
        }
        
        // Open the inventory
        player.openInventory(inventory);
    }
    
    /**
     * Create an ItemStack for an upgrade
     * 
     * @param upgrade The upgrade
     * @param currentLevel Current level (0 = not purchased)
     * @return ItemStack representing the upgrade
     */
    private ItemStack createUpgradeItem(TeamUpgrade upgrade, int currentLevel) {
        Material material = getMaterialForUpgrade(upgrade.getType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            int nextLevel = currentLevel + 1;
            boolean maxed = nextLevel > upgrade.getMaxLevel();
            
            meta.setDisplayName((maxed ? ChatColor.GREEN : ChatColor.YELLOW) + upgrade.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + upgrade.getDescription());
            lore.add("");
            
            if (maxed) {
                lore.add(ChatColor.GREEN + "Tier " + currentLevel + " (MAX)");
                lore.add(ChatColor.GRAY + upgrade.getEffectDescription(currentLevel));
                lore.add("");
                lore.add(ChatColor.GREEN + "✓ MAXED");
            } else {
                lore.add(ChatColor.YELLOW + "Tier " + nextLevel);
                lore.add(ChatColor.GRAY + upgrade.getEffectDescription(nextLevel));
                lore.add("");
                
                String costText = ChatColor.WHITE + "Cost: " + ChatColor.AQUA + 
                    upgrade.getCost(nextLevel) + " Diamonds";
                lore.add(costText);
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to purchase!");
            }
            
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Get material for an upgrade type
     * 
     * @param type Upgrade type
     * @return Material
     */
    private Material getMaterialForUpgrade(TeamUpgrade.UpgradeType type) {
        return switch (type) {
            case SHARPNESS -> Material.DIAMOND_SWORD;
            case PROTECTION -> Material.DIAMOND_CHESTPLATE;
            case FORGE -> Material.FURNACE;
            case HASTE -> Material.GOLDEN_PICKAXE;
            case HEALING -> Material.BEACON;
            case TRAP -> Material.REDSTONE;
        };
    }
    
    /**
     * Create a glass pane for separators
     * 
     * @return Glass pane ItemStack
     */
    private ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }
    
    /**
     * Handle an upgrade purchase
     * 
     * @param player The player
     * @param slot The inventory slot clicked
     * @param game The game
     * @return true if handled, false otherwise
     */
    public boolean handlePurchase(Player player, int slot, Game game) {
        String teamName = game.getPlayerTeam(player);
        if (teamName == null) return false;
        
        // Check if it's an upgrade slot
        if (slot < 10 || slot > 15) {
            return false;
        }
        
        int upgradeIndex = slot - 10;
        if (upgradeIndex < 0 || upgradeIndex >= availableUpgrades.size()) {
            return false;
        }
        
        TeamUpgrade upgrade = availableUpgrades.get(upgradeIndex);
        
        // Get current level
        Map<TeamUpgrade.UpgradeType, Integer> upgradeLevels = teamUpgrades
            .computeIfAbsent(teamName, k -> new ConcurrentHashMap<>());
        int currentLevel = upgradeLevels.getOrDefault(upgrade.getType(), 0);
        
        // Check if already maxed
        int nextLevel = currentLevel + 1;
        if (nextLevel > upgrade.getMaxLevel()) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThis upgrade is already at maximum level!");
            return true;
        }
        
        // Check if player has enough diamonds
        int cost = upgrade.getCost(nextLevel);
        if (!hasDiamonds(player, cost)) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have enough diamonds! Need " + cost + ".");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            return true;
        }
        
        // Remove diamonds and apply upgrade
        removeDiamonds(player, cost);
        upgradeLevels.put(upgrade.getType(), nextLevel);
        
        // Announce upgrade
        game.broadcastTeamMessage(teamName, "&a&lTEAM UPGRADE! &f" + player.getName() + 
            " &7purchased &f" + upgrade.getName() + " &7(Tier " + nextLevel + ")");
        
        // Apply the upgrade effect
        applyUpgradeEffect(game, teamName, upgrade.getType(), nextLevel);
        
        // Success message and sound
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aYou purchased &e" + upgrade.getName() + 
            " &aTier " + nextLevel + "!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        // Refresh menu
        openUpgradesMenu(player, game);
        return true;
    }
    
    /**
     * Apply an upgrade effect to team members
     * 
     * @param game The game
     * @param teamName The team name
     * @param type The upgrade type
     * @param level The new level
     */
    private void applyUpgradeEffect(Game game, String teamName, TeamUpgrade.UpgradeType type, int level) {
        // Apply effects based on upgrade type
        Set<UUID> teamMembers = game.getTeamMembers(teamName);
        if (teamMembers == null) return;
        
        for (UUID playerId : teamMembers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) continue;
            
            game.applyTeamUpgrade(player, type, level);
        }
    }
    
    /**
     * Check if player has enough diamonds
     * 
     * @param player The player
     * @param amount Number of diamonds needed
     * @return true if has enough, false otherwise
     */
    private boolean hasDiamonds(Player player, int amount) {
        int count = 0;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                count += item.getAmount();
                if (count >= amount) return true;
            }
        }
        
        return false;
    }
    
    /**
     * Remove diamonds from player's inventory
     * 
     * @param player The player
     * @param amount Amount to remove
     */
    private void removeDiamonds(Player player, int amount) {
        int remaining = amount;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                int itemAmount = item.getAmount();
                
                if (itemAmount <= remaining) {
                    // Remove entire stack
                    remaining -= itemAmount;
                    item.setAmount(0);
                } else {
                    // Remove partial stack
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
                
                if (remaining <= 0) break;
            }
        }
    }
    
    /**
     * Get the upgrade level for a team
     * 
     * @param teamName The team name
     * @param type Upgrade type
     * @return Current level
     */
    public int getUpgradeLevel(String teamName, TeamUpgrade.UpgradeType type) {
        if (teamName == null) return 0;
        
        Map<TeamUpgrade.UpgradeType, Integer> upgrades = teamUpgrades.get(teamName);
        if (upgrades == null) return 0;
        
        return upgrades.getOrDefault(type, 0);
    }
    
    /**
     * Reset all team upgrades (for new game)
     */
    public void resetUpgrades() {
        teamUpgrades.clear();
    }
}
