package org.bcnlab.beaconLabsBW.shop;

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
     * Populate a team upgrade menu inventory with available upgrades
     * 
     * @param inventory The inventory to populate
     * @param player The player viewing the menu
     */
    public void populateTeamUpgradeMenu(Inventory inventory, Player player) {
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            return;
        }
        
        String team = game.getPlayerTeam(player);
        if (team == null) {
            return;
        }
        
        // Initialize team upgrades map if not exists
        teamUpgrades.computeIfAbsent(team, k -> new ConcurrentHashMap<>());
        
        // Add upgrade items to the menu
        int slot = 10;
        for (TeamUpgrade upgrade : availableUpgrades) {
            // Get current level (0 = not purchased yet)
            int currentLevel = teamUpgrades.get(team).getOrDefault(upgrade.getType(), 0);
            boolean maxLevel = currentLevel >= upgrade.getCosts().length;
            
            // Create the item to display
            ItemStack item = createUpgradeItem(upgrade, currentLevel, maxLevel);
            
            // Add to inventory
            inventory.setItem(slot, item);
            
            // Increment slot counter (keep a nice layout)
            if (slot % 9 == 7) {
                slot += 3; // Move to next row
            } else {
                slot += 1; // Move to next column
            }
        }
        
        // Add team info item
        ItemStack teamInfo = new ItemStack(Material.BEACON);
        ItemMeta teamMeta = teamInfo.getItemMeta();
        if (teamMeta != null) {
            ChatColor teamColor = MessageUtils.getTeamChatColor(team);
            teamMeta.setDisplayName(teamColor + team + " Team");
            
            // List current upgrades
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.YELLOW + "Current Upgrades:");
            
            // Add info about each upgrade
            for (Map.Entry<TeamUpgrade.UpgradeType, Integer> entry : teamUpgrades.get(team).entrySet()) {
                TeamUpgrade.UpgradeType type = entry.getKey();
                int level = entry.getValue();
                
                // Find the upgrade
                for (TeamUpgrade upgrade : availableUpgrades) {
                    if (upgrade.getType() == type) {
                        lore.add(ChatColor.GRAY + "- " + upgrade.getName() + " " + 
                            ChatColor.AQUA + "Level " + level);
                        break;
                    }
                }
            }
            
            // If no upgrades yet
            if (teamUpgrades.get(team).isEmpty()) {
                lore.add(ChatColor.GRAY + "- None");
            }
            
            teamMeta.setLore(lore);
            teamInfo.setItemMeta(teamMeta);
        }
        
        // Place team info in the center
        inventory.setItem(4, teamInfo);
        
        // Add decorative glass panes
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        
        // Fill empty spaces with glass
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
            }
        }
    }
    
    /**
     * Create an item representing a team upgrade
     * 
     * @param upgrade The team upgrade
     * @param currentLevel The current level (0 = not purchased)
     * @param maxLevel Whether max level is reached
     * @return The created item
     */
    private ItemStack createUpgradeItem(TeamUpgrade upgrade, int currentLevel, boolean maxLevel) {
        ItemStack item = new ItemStack(upgrade.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set name and basic info
            meta.setDisplayName(ChatColor.GREEN + upgrade.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + upgrade.getDescription());
            lore.add("");
            
            // Show current level or max level
            if (maxLevel) {
                lore.add(ChatColor.GOLD + "Level: " + ChatColor.GREEN + "MAX");
                lore.add("");
                lore.add(ChatColor.YELLOW + "You have the maximum level!");
            } else {
                int nextLevel = currentLevel + 1;
                lore.add(ChatColor.GOLD + "Level: " + ChatColor.AQUA + 
                    (currentLevel > 0 ? currentLevel : "None") +
                    ChatColor.GRAY + " → " + ChatColor.GREEN + nextLevel);
                lore.add("");
                
                // Show cost for next level
                int cost = upgrade.getCosts()[currentLevel];
                lore.add(ChatColor.YELLOW + "Cost: " + ChatColor.AQUA + cost + " Diamonds");
                
                // Add click instructions
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to purchase!");
            }
            
            meta.setLore(lore);            // Add glow effect if any level is purchased
            if (currentLevel > 0) {
                // Add glow using standard enchantments
                addGlowEffect(meta);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
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
    }    /**
     * Add a glowing effect to an ItemMeta
     * 
     * @param meta The ItemMeta to add glow to
     */
    private void addGlowEffect(ItemMeta meta) {
        // Just add an enchantment flag
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        // Simply make the item appear special without enchanting
        // This is a workaround since we're having issues with enchantment constants
        // In a real implementation, you would use a proper enchantment
    }
}
