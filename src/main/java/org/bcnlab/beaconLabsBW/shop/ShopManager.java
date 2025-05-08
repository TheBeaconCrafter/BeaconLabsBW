package org.bcnlab.beaconLabsBW.shop;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameMode;
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

/**
 * Manages the BedWars shop system
 */
public class ShopManager {
    
    private final BeaconLabsBW plugin;
    private final List<ShopItem> shopItems = new ArrayList<>();
    private final Map<UUID, ShopCategory> playerCategory = new HashMap<>();

    // Helper class for Tool Upgrade Information
    private static class ToolUpgradeInfo {
        final Material nextTierMaterial;
        final Material currency;
        final int cost;
        final String displayName;
        final boolean isMaxTier;

        ToolUpgradeInfo(Material nextTierMaterial, Material currency, int cost, String displayName, boolean isMaxTier) {
            this.nextTierMaterial = nextTierMaterial;
            this.currency = currency;
            this.cost = cost;
            this.displayName = displayName;
            this.isMaxTier = isMaxTier;
        }
    }
    
    /**
     * Creates a new ShopManager
     * 
     * @param plugin The plugin instance
     */
    public ShopManager(BeaconLabsBW plugin) {
        this.plugin = plugin;
        
        // Register shop items
        registerItems();
    }
      /**
     * Register all available shop items
     */
    private void registerItems() {
        // Quick buy category
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Wool", Material.WHITE_WOOL, 16, Material.IRON_INGOT, 4, "16 wool blocks"));
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Stone Sword", Material.STONE_SWORD, 1, Material.IRON_INGOT, 10, "Stone sword"));
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Chainmail Armor", Material.CHAINMAIL_CHESTPLATE, 1, Material.IRON_INGOT, 40, "Chainmail armor protection"));
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Bow", Material.BOW, 1, Material.GOLD_INGOT, 12, "Bow"));
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Arrows", Material.ARROW, 8, Material.GOLD_INGOT, 2, "8 arrows"));
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Speed Potion", Material.POTION, 1, Material.EMERALD, 1, "Speed II (45 seconds)"));
        
        // Ultimates category
        addItem(ShopCategory.ULTIMATES, new UltimateShopItem("Swordsman", Material.IRON_SWORD, org.bcnlab.beaconLabsBW.game.ultimates.UltimateClass.SWORDSMAN, 
            "Dash forward and damage enemies"));
        addItem(ShopCategory.ULTIMATES, new UltimateShopItem("Healer", Material.GOLDEN_APPLE, org.bcnlab.beaconLabsBW.game.ultimates.UltimateClass.HEALER,
            "Provides healing to teammates in a small radius"));
        addItem(ShopCategory.ULTIMATES, new UltimateShopItem("Frozo", Material.PACKED_ICE, org.bcnlab.beaconLabsBW.game.ultimates.UltimateClass.FROZO,
            "Slow enemies down in a small radius"));
        addItem(ShopCategory.ULTIMATES, new UltimateShopItem("Builder", Material.BRICKS, org.bcnlab.beaconLabsBW.game.ultimates.UltimateClass.BUILDER,
            "Fast bridging and passive wool generation"));
        addItem(ShopCategory.ULTIMATES, new UltimateShopItem("Gatherer", Material.EMERALD, org.bcnlab.beaconLabsBW.game.ultimates.UltimateClass.GATHERER,
            "Chance to duplicate resources and portable ender chest"));
        addItem(ShopCategory.ULTIMATES, new UltimateShopItem("Demolition", Material.TNT, org.bcnlab.beaconLabsBW.game.ultimates.UltimateClass.DEMOLITION,
            "Burn connected wool and drop TNT on death"));
        addItem(ShopCategory.ULTIMATES, new UltimateShopItem("Kangaroo", Material.RABBIT_FOOT, org.bcnlab.beaconLabsBW.game.ultimates.UltimateClass.KANGAROO,
            "Double jump and save resources upon death"));
        
        // Blocks category
        addItem(ShopCategory.BLOCKS, new ShopItem("Wool", Material.WHITE_WOOL, 16, Material.IRON_INGOT, 4, "16 wool blocks"));
        addItem(ShopCategory.BLOCKS, new ShopItem("Wood", Material.OAK_PLANKS, 16, Material.GOLD_INGOT, 4, "16 wooden planks"));
        addItem(ShopCategory.BLOCKS, new ShopItem("Stone", Material.STONE, 16, Material.IRON_INGOT, 12, "16 stone blocks"));
        addItem(ShopCategory.BLOCKS, new ShopItem("End Stone", Material.END_STONE, 12, Material.IRON_INGOT, 24, "12 end stone blocks"));
        addItem(ShopCategory.BLOCKS, new ShopItem("Obsidian", Material.OBSIDIAN, 4, Material.EMERALD, 4, "4 obsidian blocks"));
        
        // Melee category
        addItem(ShopCategory.MELEE, new ShopItem("Stone Sword", Material.STONE_SWORD, 1, Material.IRON_INGOT, 10, "Stone sword"));
        addItem(ShopCategory.MELEE, new ShopItem("Iron Sword", Material.IRON_SWORD, 1, Material.GOLD_INGOT, 7, "Iron sword"));
        addItem(ShopCategory.MELEE, new ShopItem("Diamond Sword", Material.DIAMOND_SWORD, 1, Material.EMERALD, 4, "Diamond sword"));
        addItem(ShopCategory.MELEE, new ShopItem("Knockback Stick", Material.STICK, 1, Material.GOLD_INGOT, 5, "Knockback I stick"));

        // Armor category
        addItem(ShopCategory.ARMOR, new ShopItem("Chainmail Armor", Material.CHAINMAIL_CHESTPLATE, 1, Material.IRON_INGOT, 40, "Permanent chainmail armor"));
        addItem(ShopCategory.ARMOR, new ShopItem("Iron Armor", Material.IRON_CHESTPLATE, 1, Material.GOLD_INGOT, 12, "Permanent iron armor"));
        addItem(ShopCategory.ARMOR, new ShopItem("Diamond Armor", Material.DIAMOND_CHESTPLATE, 1, Material.EMERALD, 6, "Permanent diamond armor"));
        
        // Tools category
        addItem(ShopCategory.TOOLS, new ShopItem("Shears", Material.SHEARS, 1, Material.IRON_INGOT, 20, "Permanent shears"));
        addItem(ShopCategory.TOOLS, new ShopItem("Upgrade Pickaxe", Material.WOODEN_PICKAXE, 1, Material.AIR, 0, "Upgrade your Pickaxe tier by tier."));
        addItem(ShopCategory.TOOLS, new ShopItem("Upgrade Axe", Material.WOODEN_AXE, 1, Material.AIR, 0, "Upgrade your Axe tier by tier."));
        
        // Ranged category
        addItem(ShopCategory.RANGED, new ShopItem("Bow", Material.BOW, 1, Material.GOLD_INGOT, 12, "Bow"));
        addItem(ShopCategory.RANGED, new ShopItem("Power Bow", Material.BOW, 1, Material.GOLD_INGOT, 24, "Power I bow"));
        addItem(ShopCategory.RANGED, new ShopItem("Punch Bow", Material.BOW, 1, Material.EMERALD, 6, "Punch I Power I bow"));
        addItem(ShopCategory.RANGED, new ShopItem("Arrows", Material.ARROW, 8, Material.GOLD_INGOT, 2, "8 arrows"));
        
        // Potions category
        addItem(ShopCategory.POTIONS, new ShopItem("Speed Potion", Material.POTION, 1, Material.EMERALD, 1, "Speed II (45 seconds)"));
        addItem(ShopCategory.POTIONS, new ShopItem("Jump Potion", Material.POTION, 1, Material.EMERALD, 1, "Jump V (45 seconds)"));
        addItem(ShopCategory.POTIONS, new ShopItem("Invisibility", Material.POTION, 1, Material.EMERALD, 2, "Invisibility (30 seconds)"));
        
        // Utility category
        addItem(ShopCategory.UTILITY, new ShopItem("Golden Apple", Material.GOLDEN_APPLE, 1, Material.GOLD_INGOT, 3, "Golden apple"));
        addItem(ShopCategory.UTILITY, new ShopItem("TNT", Material.TNT, 1, Material.GOLD_INGOT, 4, "TNT (instant ignition)"));
        addItem(ShopCategory.UTILITY, new ShopItem("Water Bucket", Material.WATER_BUCKET, 1, Material.GOLD_INGOT, 3, "Water bucket"));
        addItem(ShopCategory.UTILITY, new ShopItem("Bridge Egg", Material.EGG, 1, Material.EMERALD, 2, "Creates bridge in direction thrown"));
        addItem(ShopCategory.UTILITY, new ShopItem("Shield", Material.SHIELD, 1, Material.GOLD_INGOT, 10, "Standard shield"));
        addItem(ShopCategory.UTILITY, new ShopItem("Fireball", Material.FIRE_CHARGE, 1, Material.IRON_INGOT, 40, "Throwable Fireball!"));
        addItem(ShopCategory.UTILITY, new ShopItem("Dream Defender", Material.VILLAGER_SPAWN_EGG, 1, Material.IRON_INGOT, 120, "Iron Golem to defend your base"));
    }
    
    /**
     * Add an item to the shop
     * 
     * @param category The category
     * @param item The shop item
     */
    private void addItem(ShopCategory category, ShopItem item) {
        item.setCategory(category);
        shopItems.add(item);
    }
    
    /**
     * Open the shop for a player
     * 
     * @param player The player
     */    public void openShop(Player player) {
        if (player == null) return;
        
        // Default to quick buy category
        ShopCategory category = playerCategory.getOrDefault(player.getUniqueId(), ShopCategory.QUICK_BUY);
        
        // Check if the selected category is ULTIMATES but player is not in an ultimates game
        Game game = plugin.getGameManager().getPlayerGame(player);
        boolean ultimatesEnabled = (game != null && game.getGameMode() == GameMode.ULTIMATES);
        
        if (category == ShopCategory.ULTIMATES && !ultimatesEnabled) {
            // Switch to QUICK_BUY if trying to access ultimates in a non-ultimates game
            category = ShopCategory.QUICK_BUY;
        }
        
        openCategoryMenu(player, category);
    }
      /**
     * Open a specific category menu for a player
     * 
     * @param player The player
     * @param category The category
     */
    public void openCategoryMenu(Player player, ShopCategory category) {
        plugin.getLogger().info("[ShopManager] openCategoryMenu called for player: " + player.getName() + " with category: " + category.name());
        if (player == null || category == null) {
             plugin.getLogger().info("[ShopManager] Aborting menu open: player or category is null.");
             return;
        }
        
        // Store player's current category
        playerCategory.put(player.getUniqueId(), category);
        
        // Create inventory
        Inventory inventory = Bukkit.createInventory(
            null,
            54,
            ChatColor.DARK_GRAY + "BedWars Shop: " + category.getDisplayName()
        );
        
        // Check if player is in a game with ultimates enabled
        Game game = plugin.getGameManager().getPlayerGame(player);
        boolean ultimatesEnabled = (game != null && game.getGameMode() == GameMode.ULTIMATES);
        
        // Add category selector items at the top
        int slot = 0;
        for (ShopCategory cat : ShopCategory.values()) {
            // Skip ULTIMATES category if not in ultimates game mode
            if (cat == ShopCategory.ULTIMATES && !ultimatesEnabled) {
                continue;
            }
            ItemStack categoryItem = createCategoryItem(cat, cat == category);
            inventory.setItem(slot++, categoryItem);
        }
        
        // Add separator
        for (int i = 9; i < 18; i++) {
            inventory.setItem(i, createGlassPane());
        }
        
        // Add items for this category
        slot = 18;
        for (ShopItem item : shopItems) {
            if (item.getCategory() == category) {
                inventory.setItem(slot++, createShopItemStack(item, player));
            }
        }
        
        // Open the inventory
        player.openInventory(inventory);
        plugin.getLogger().info("[ShopManager] Opening inventory for player: " + player.getName());
    }
    
    /**
     * Create an ItemStack for a category button
     * 
     * @param category The category
     * @param selected Whether this category is selected
     * @return ItemStack for this category
     */
    private ItemStack createCategoryItem(ShopCategory category, boolean selected) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName((selected ? ChatColor.GREEN : ChatColor.YELLOW) + category.getDisplayName());
            
            List<String> lore = new ArrayList<>();
            lore.add(selected ? ChatColor.GRAY + "Currently selected" : ChatColor.GRAY + "Click to view");
            meta.setLore(lore);
            
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create an ItemStack for a shop item
     * 
     * @param shopItem The shop item
     * @param player The player
     * @return ItemStack representation
     */
    private ItemStack createShopItemStack(ShopItem shopItem, Player player) {
        ItemStack item = new ItemStack(shopItem.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String displayName = shopItem.getName();
            List<String> lore = new ArrayList<>();
            Material displayMaterial = shopItem.getMaterial();
            int displayCost = shopItem.getCost();
            Material displayCurrency = shopItem.getCurrency();
            boolean purchaseDisabled = false;

            if (shopItem.getName().equals("Upgrade Pickaxe")) {
                ToolUpgradeInfo upgradeInfo = getNextToolUpgradeInfo(player, "PICKAXE");
                displayName = upgradeInfo.isMaxTier ? ChatColor.GREEN + upgradeInfo.displayName : ChatColor.YELLOW + "Upgrade to " + upgradeInfo.displayName;
                if(upgradeInfo.isMaxTier) {
                    displayMaterial = Material.DIAMOND_PICKAXE; // Show diamond pick if maxed
                } else {
                    // Show the icon of the tier they are ABOUT to purchase, or wooden if buying the first one.
                    displayMaterial = upgradeInfo.nextTierMaterial;
                }

                lore.add(ChatColor.GRAY + shopItem.getDescription());
                lore.add("");
                if (upgradeInfo.isMaxTier) {
                    lore.add(ChatColor.GREEN + "Max Tier Reached!");
                    purchaseDisabled = true;
                } else {
                    displayCost = upgradeInfo.cost;
                    displayCurrency = upgradeInfo.currency;
                    lore.add(ChatColor.WHITE + "Cost: " + ChatColor.YELLOW + displayCost + " " + MessageUtils.colorize(getCurrencyName(displayCurrency)));
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "Click to purchase!");
                }
            } else if (shopItem.getName().equals("Upgrade Axe")) {
                ToolUpgradeInfo upgradeInfo = getNextToolUpgradeInfo(player, "AXE");
                displayName = upgradeInfo.isMaxTier ? ChatColor.GREEN + upgradeInfo.displayName : ChatColor.YELLOW + "Upgrade to " + upgradeInfo.displayName;
                if(upgradeInfo.isMaxTier) {
                     displayMaterial = Material.DIAMOND_AXE; // Show diamond axe if maxed
                } else {
                    // Icon should be what they are buying, or wooden if buying the first one (nextTierMaterial will be WOODEN_AXE in that case)
                    displayMaterial = upgradeInfo.nextTierMaterial; 
                }

                lore.add(ChatColor.GRAY + shopItem.getDescription());
                lore.add("");
                if (upgradeInfo.isMaxTier) {
                    lore.add(ChatColor.GREEN + "Max Tier Reached!");
                    purchaseDisabled = true;
                } else {
                    displayCost = upgradeInfo.cost;
                    displayCurrency = upgradeInfo.currency;
                    lore.add(ChatColor.WHITE + "Cost: " + ChatColor.YELLOW + displayCost + " " + MessageUtils.colorize(getCurrencyName(displayCurrency)));
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "Click to purchase!");
                }
            } else {
                // Standard item display
                displayName = ChatColor.WHITE + shopItem.getName();
                lore.add(ChatColor.GRAY + shopItem.getDescription());
                lore.add("");
                if (shopItem.isFree()) {
                    lore.add(ChatColor.GREEN + "FREE");
                } else {
                    lore.add(ChatColor.WHITE + "Cost: " + ChatColor.YELLOW + displayCost + " " + MessageUtils.colorize(getCurrencyName(displayCurrency)));
                }
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to purchase!");
            }

            item.setType(displayMaterial); // Update material for icon
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            if (purchaseDisabled) { // Simplified condition: upgradeInfo.isMaxTier is implied if purchaseDisabled is true for these items
                 // meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, false); // Removed LUCK enchantment
                 // meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                 // Consider other ways to make it look disabled if needed, e.g. different colored name part
            }
            item.setItemMeta(meta);
        }
        return item;
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
     * Handle a purchase from the shop
     * 
     * @param player The player
     * @param slot The inventory slot clicked
     * @return true if purchase was handled, false otherwise
     */
    public boolean handlePurchase(Player player, int slot) {
        if (player == null) return false;
        
        // Check if it's a category selection (top row)
        if (slot < 9) {
            ShopCategory[] categories = ShopCategory.values();
            if (slot < categories.length) {
                openCategoryMenu(player, categories[slot]);
                return true;
            }
            return false;
        }
        
        // Ignore separator row
        if (slot >= 9 && slot < 18) {
            return false;
        }
        
        // Handle item purchase
        ShopCategory category = playerCategory.getOrDefault(player.getUniqueId(), ShopCategory.QUICK_BUY);
        int itemIndex = slot - 18;
        
        List<ShopItem> categoryItems = shopItems.stream()
            .filter(item -> item.getCategory() == category)
            .toList();
            
        if (itemIndex >= 0 && itemIndex < categoryItems.size()) {
            ShopItem item = categoryItems.get(itemIndex);
            processPurchase(player, item);
            return true;
        }
        
        return false;
    }    /**
     * Process a shop purchase
     * 
     * @param player The player
     * @param item The item being purchased
     */    private void processPurchase(Player player, ShopItem item) {
        if (item instanceof UltimateShopItem) {
            UltimateShopItem ultimateItem = (UltimateShopItem) item;
            UltimateClassHandler.handleUltimateSelection(player, ultimateItem, plugin);
            return;
        }
        
        // --- New Armor Check --- 
        if (isArmor(item.getMaterial())) {
            int purchaseTier = getArmorTierLevel(item.getMaterial());
            int playerTier = getPlayerArmorTier(player);
            if (purchaseTier <= playerTier) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou already have this tier of armor or better!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); // Use a 'fail' sound
                return; // Cancel purchase
            }
        }
        // --- End New Armor Check ---

        String itemName = item.getName();
        if (itemName.equals("Upgrade Pickaxe") || itemName.equals("Upgrade Axe")) {
            String toolType = itemName.equals("Upgrade Pickaxe") ? "PICKAXE" : "AXE";
            ToolUpgradeInfo upgradeInfo = getNextToolUpgradeInfo(player, toolType);

            if (upgradeInfo.isMaxTier) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou already have the max tier " + toolType.toLowerCase() + "!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            Material currentToolMat = null;
            if (upgradeInfo.nextTierMaterial == Material.STONE_PICKAXE) currentToolMat = Material.WOODEN_PICKAXE;
            else if (upgradeInfo.nextTierMaterial == Material.IRON_PICKAXE) currentToolMat = Material.STONE_PICKAXE;
            else if (upgradeInfo.nextTierMaterial == Material.DIAMOND_PICKAXE) currentToolMat = Material.IRON_PICKAXE;
            else if (upgradeInfo.nextTierMaterial == Material.WOODEN_PICKAXE) currentToolMat = null; // Buying the first one

            if (upgradeInfo.nextTierMaterial == Material.STONE_AXE) currentToolMat = Material.WOODEN_AXE;
            else if (upgradeInfo.nextTierMaterial == Material.IRON_AXE) currentToolMat = Material.STONE_AXE;
            else if (upgradeInfo.nextTierMaterial == Material.DIAMOND_AXE) currentToolMat = Material.IRON_AXE;
            else if (upgradeInfo.nextTierMaterial == Material.WOODEN_AXE) currentToolMat = null; // Buying the first one
            
            // Check if player has the prerequisite tool (if not buying the first wooden one)
            boolean hasPrerequisite = false;
            if (currentToolMat != null) {
                for (ItemStack invItem : player.getInventory().getContents()) {
                    if (invItem != null && invItem.getType() == currentToolMat) {
                        hasPrerequisite = true;
                        break;
                    }
                }
            } else {
                hasPrerequisite = true; // No prerequisite needed if buying the first wooden tool
            }

            if (!hasPrerequisite) {
                // This case should ideally be handled by getNextToolUpgradeInfo returning the wooden tool info if player has nothing.
                // If currentToolMat is not null, it means they are upgrading from an existing tool.
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou need a " + (currentToolMat != null ? currentToolMat.name().toLowerCase().replace("_", " ") : "base tool") + " to upgrade!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                return;
            }
            
            removeCurrency(player, upgradeInfo.currency, upgradeInfo.cost);
            if (currentToolMat != null) {
                player.getInventory().remove(currentToolMat);
            }

            ItemStack newTool = new ItemStack(upgradeInfo.nextTierMaterial, 1);
            ItemMeta meta = newTool.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.WHITE + upgradeInfo.displayName);
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + toolType + " - Keep on Death (may downgrade)");
                meta.setLore(lore);
                newTool.setItemMeta(meta);
            }
            player.getInventory().addItem(newTool);

            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aPurchased &e" + upgradeInfo.displayName + "&a!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            playPurchaseAnimation(player, item);
            openCategoryMenu(player, playerCategory.getOrDefault(player.getUniqueId(), ShopCategory.QUICK_BUY));
            return;
        }
        
        // Check if item is free or if player has enough currency
        if (!item.isFree() && !hasCurrency(player, item.getCurrency(), item.getCost())) {
            String currencyName = getCurrencyName(item.getCurrency());
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have enough " + currencyName + "!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            return;
        }
        
        // Special handling for sword upgrades - remove existing swords (if not upgradable swords)
        // If swords become upgradable, this logic needs to adapt like pickaxes/axes.
        if (item.getMaterial().name().contains("SWORD") && !item.getName().contains("Upgrade")) { // Assuming no "Sword Upgrade" items for now
            removeExistingSwords(player);
        }

        // Handle permanent tool upgrades - don't let players buy the same tool twice (for non-upgradable tools like Shears)
        if (!item.getName().contains("Upgrade")) { // Only apply to non-upgrade items
            if (!handlePermanentToolUpgrades(player, item)) {
                 return;
            }
        }
          // Remove the currency
        removeCurrency(player, item.getCurrency(), item.getCost());
        
        // Determine item to give - handle team-colored wool
        Material material = item.getMaterial();
        if (material == Material.WHITE_WOOL) {
            material = getTeamColoredWool(player);
        }
        
        // Create item stack
        ItemStack itemStack = new ItemStack(material, item.getAmount());
        
        // Apply item customizations
        customizeShopItem(itemStack, item);
          // Special handling for armor
        if (isArmor(item.getMaterial())) {
            // Get armorType for armor differentiation
            String armorType = item.getMaterial().name();
            
            // Process armor upgrade based on tier
            if (armorType.contains("CHAINMAIL") || armorType.contains("IRON") || armorType.contains("DIAMOND")) {
                equipArmorUpgrade(player, itemStack, item.getName());
            } else {
                equipArmor(player, itemStack);
            }
        } else {
            // Give item to inventory
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);
            
            // Drop any items that didn't fit
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
        
        // Confirmation message and sound
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aYou purchased &e" + item.getName() + "&a!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        // Play purchase animation
        playPurchaseAnimation(player, item);
    }
    
    /**
     * Play special effects when purchasing items
     * 
     * @param player The player
     * @param item The shop item
     */
    private void playPurchaseAnimation(Player player, ShopItem item) {
        // Special effects based on item type
        if (item.getMaterial() == Material.TNT) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_TNT_PRIMED, 0.5f, 1.0f);        } else if (item.getMaterial() == Material.DIAMOND_SWORD || 
                  item.getMaterial() == Material.DIAMOND_CHESTPLATE) {
            // Use a basic particle type that exists in all versions
            player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, 
                                           player.getLocation().add(0, 1.0, 0), 
                                           20, 0.5, 0.5, 0.5, 0.1);
        }
    }
    
    /**
     * Remove existing swords from player inventory
     * 
     * @param player The player
     */
    private void removeExistingSwords(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().name().contains("SWORD")) {
                player.getInventory().remove(item);
            }
        }
    }
    
    /**
     * Apply customizations to shop items based on name/description
     * 
     * @param itemStack The item to customize
     * @param shopItem The shop item with details
     */    private void customizeShopItem(ItemStack itemStack, ShopItem shopItem) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            // Set custom name if needed (already done for direct purchases, but good for consistency)
            if (shopItem.getName() != null && !shopItem.getName().isEmpty() && !meta.hasDisplayName()) {
                 meta.setDisplayName(ChatColor.WHITE + shopItem.getName());
            }
            
            // Special enchantments based on item name
            if (shopItem.getName().contains("Knockback")) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 1, true);
            }
              if (shopItem.getName().contains("Power")) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.POWER, 1, true);
            }
            
            if (shopItem.getName().contains("Punch")) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.PUNCH, 1, true);
            }

            // Handle Potion Effects for Potion Items
            if (itemStack.getType() == Material.POTION) {
                if (meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
                    boolean effectSet = false;
                    if (shopItem.getName().equalsIgnoreCase("Speed Potion")) {
                        potionMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 45 * 20, 1), true); // Speed II for 45s
                        potionMeta.setColor(org.bukkit.Color.fromRGB(125, 184, 241)); // Light blue
                        // We'll rely on the general meta.setDisplayName later if needed, or set it here if specific.
                        // For now, let PotionMeta handle its default display or let lore describe it.
                        // Or, if we want to override the default potion name:
                        potionMeta.setDisplayName(ChatColor.AQUA + "Speed Potion II");
                        effectSet = true;
                    } else if (shopItem.getName().equalsIgnoreCase("Jump Potion")) {
                        potionMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 45 * 20, 4), true); // Jump V for 45s
                        potionMeta.setColor(org.bukkit.Color.LIME); // Lime green
                        potionMeta.setDisplayName(ChatColor.GREEN + "Jump Potion V");
                        effectSet = true;
                    } else if (shopItem.getName().equalsIgnoreCase("Invisibility")) {
                        potionMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 30 * 20, 0), true); // Invisibility for 30s
                        potionMeta.setColor(org.bukkit.Color.GRAY); // Gray
                        potionMeta.setDisplayName(ChatColor.GRAY + "Invisibility Potion");
                        effectSet = true;
                    }

                    if (effectSet) {
                        // Add flag to hide the default "No Effects" or redundant effect list if using custom name
                        // potionMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_POTION_EFFECTS); // Removed as per user feedback - flag may not exist in this API version
                    }
                }
            }
            
            // Make tools and weapons unbreakable
            Material type = itemStack.getType();
            if (type.name().contains("SWORD") || 
                type.name().contains("AXE") || 
                type.name().contains("PICKAXE") ||
                type.name().contains("SHOVEL") || 
                type == Material.SHEARS ||
                type == Material.BOW ||
                type == Material.CROSSBOW ||
                type == Material.SHIELD ||
                type == Material.FISHING_ROD) {
                
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }
            
            itemStack.setItemMeta(meta);
        }
    }
  
    
    /**
     * Check if player has enough currency
     * 
     * @param player The player
     * @param currency The currency type
     * @param amount The amount needed
     * @return true if player has enough, false otherwise
     */
    private boolean hasCurrency(Player player, Material currency, int amount) {
        int count = 0;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == currency) {
                count += item.getAmount();
                if (count >= amount) return true;
            }
        }
        
        return false;
    }
    
    /**
     * Remove currency from player's inventory
     * 
     * @param player The player
     * @param currency The currency type
     * @param amount The amount to remove
     */
    private void removeCurrency(Player player, Material currency, int amount) {
        int remaining = amount;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == currency) {
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
     * Get a friendly currency name
     * 
     * @param currency The currency material
     * @return Friendly name
     */
    private String getCurrencyName(Material currency) {
        return switch (currency) {
            case IRON_INGOT -> "&fIron";
            case GOLD_INGOT -> "&6Gold";
            case EMERALD -> "&aEmerald";
            default -> currency.name();
        };
    }
    
    /**
     * Check if a material is armor
     * 
     * @param material The material
     * @return true if armor, false otherwise
     */
    private boolean isArmor(Material material) {
        return material.name().contains("HELMET") ||
               material.name().contains("CHESTPLATE") ||
               material.name().contains("LEGGINGS") ||
               material.name().contains("BOOTS");
    }
      /**
     * Equip armor on a player
     * 
     * @param player The player
     * @param armor The armor item
     */
    private void equipArmor(Player player, ItemStack armor) {
        Material type = armor.getType();
        
        // For armor upgrades, only replace leggings and boots, not helmet or chestplate
        // If the player doesn't have a helmet/chestplate yet, we'll equip those too
        if (type.name().contains("HELMET")) {
            if (player.getInventory().getHelmet() == null) {
                player.getInventory().setHelmet(armor);
            }
        } else if (type.name().contains("CHESTPLATE")) {
            if (player.getInventory().getChestplate() == null) {
                player.getInventory().setChestplate(armor);
            }
        } else if (type.name().contains("LEGGINGS")) {
            player.getInventory().setLeggings(armor);
        } else if (type.name().contains("BOOTS")) {
            player.getInventory().setBoots(armor);
        }
    }
    
    /**
     * Handle armor upgrades for players - specifically creating appropriate items for each armor slot
     * 
     * @param player The player to upgrade armor for
     * @param chestplate The chestplate item that was purchased (used to determine armor tier)
     * @param armorName The name of the armor set
     */
    private void equipArmorUpgrade(Player player, ItemStack chestplate, String armorName) {
        // Determine material type based on the chestplate
        String matName = chestplate.getType().name();
        Material legMaterial = null;
        Material bootMaterial = null;
        
        if (matName.contains("CHAINMAIL")) {
            legMaterial = Material.CHAINMAIL_LEGGINGS;
            bootMaterial = Material.CHAINMAIL_BOOTS;
        } else if (matName.contains("IRON")) {
            legMaterial = Material.IRON_LEGGINGS;
            bootMaterial = Material.IRON_BOOTS;
        } else if (matName.contains("DIAMOND")) {
            legMaterial = Material.DIAMOND_LEGGINGS;
            bootMaterial = Material.DIAMOND_BOOTS;
        } else {
            // Fallback in case of unexpected material
            equipArmor(player, chestplate);
            return;
        }
        
        // Create leggings and boots with the same properties
        ItemStack leggings = new ItemStack(legMaterial);
        ItemStack boots = new ItemStack(bootMaterial);
        
        // Copy meta data from chestplate if any
        if (chestplate.hasItemMeta()) {
            ItemMeta baseMeta = chestplate.getItemMeta();
            
            // Set up leggings
            ItemMeta legMeta = leggings.getItemMeta();
            if (legMeta != null && baseMeta != null) {
                if (baseMeta.hasDisplayName()) {
                    legMeta.setDisplayName(baseMeta.getDisplayName());
                }
                legMeta.setUnbreakable(true);
                legMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                leggings.setItemMeta(legMeta);
            }
            
            // Set up boots
            ItemMeta bootMeta = boots.getItemMeta();
            if (bootMeta != null && baseMeta != null) {
                if (baseMeta.hasDisplayName()) {
                    bootMeta.setDisplayName(baseMeta.getDisplayName());
                }
                bootMeta.setUnbreakable(true);
                bootMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                boots.setItemMeta(bootMeta);
            }
        } else {
            // Make sure they're unbreakable even without existing metadata
            ItemMeta legMeta = leggings.getItemMeta();
            if (legMeta != null) {
                legMeta.setUnbreakable(true);
                legMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                leggings.setItemMeta(legMeta);
            }
            
            ItemMeta bootMeta = boots.getItemMeta();
            if (bootMeta != null) {
                bootMeta.setUnbreakable(true);
                bootMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                boots.setItemMeta(bootMeta);
            }
        }
        
        // Apply to player - only upgrade leggings and boots, player keeps their helmet and chestplate
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        
        // Give player the chestplate if they don't have one already
        if (player.getInventory().getChestplate() == null) {
            player.getInventory().setChestplate(chestplate);
        }
    }
    
    /**
     * Check if player already has the item
     * 
     * @param player The player
     * @param material The item material to check
     * @return true if player has the item, false otherwise
     */
    private boolean playerHasItem(Player player, Material material) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handle permanent tool upgrades - don't let players buy the same tool twice
     * 
     * @param player The player
     * @param shopItem The shop item
     * @return true if purchase should continue, false if it should be canceled
     */
    private boolean handlePermanentToolUpgrades(Player player, ShopItem shopItem) {
        // Check if this is a permanent tool
        if (shopItem.getDescription().contains("Permanent")) {
            // Check if player already has this item
            if (playerHasItem(player, shopItem.getMaterial())) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou already have this item!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get team-colored wool based on player's team
     * 
     * @param player The player
     * @return Material for the team's wool color, or WHITE_WOOL if no team
     */
    private Material getTeamColoredWool(Player player) {
        // Find the player's game
        org.bcnlab.beaconLabsBW.game.Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null) {
            // Get the player's team
            String teamName = game.getPlayerTeam(player);
            
            if (teamName != null) {
                // Get the team data
                org.bcnlab.beaconLabsBW.arena.model.TeamData teamData = game.getArena().getTeam(teamName);
                
                if (teamData != null) {
                    // Convert team color to wool material
                    return switch (teamData.getColor().toUpperCase()) {
                        case "RED" -> Material.RED_WOOL;
                        case "BLUE" -> Material.BLUE_WOOL;
                        case "GREEN" -> Material.GREEN_WOOL;
                        case "YELLOW" -> Material.YELLOW_WOOL;
                        case "AQUA" -> Material.LIGHT_BLUE_WOOL;
                        case "WHITE" -> Material.WHITE_WOOL;
                        case "PINK" -> Material.PINK_WOOL;
                        case "GRAY" -> Material.GRAY_WOOL;
                        default -> Material.WHITE_WOOL;
                    };
                }
            }
        }
        
        return Material.WHITE_WOOL;
    }
    
    /**
     * Open the team upgrades menu for a player
     * 
     * @param player The player to open the menu for
     */
    public void openTeamUpgradesMenu(Player player) {
        // Check if player is in a game
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "You must be in a game to use team upgrades!");
            return;
        }
        
        // Get player's team
        String team = game.getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You must be on a team to use team upgrades!");
            return;
        }
        
        // Get the team's color for display
        ChatColor teamColor = MessageUtils.getTeamChatColor(team);
        
        // Create the upgrades menu
        Inventory upgradeMenu = Bukkit.createInventory(null, 36, teamColor + team + " Team Upgrades");
        
        // Let the TeamUpgradeManager handle populating the menu
        plugin.getTeamUpgradeManager().populateTeamUpgradeMenu(upgradeMenu, player);
        
        // Play sound
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
        
        // Open the menu
        player.openInventory(upgradeMenu);
    }

    private ToolUpgradeInfo getNextToolUpgradeInfo(Player player, String toolType /* "PICKAXE" or "AXE" */) {
        Material currentMaterial = null;
        ItemStack[] inventory = player.getInventory().getContents();
        for (ItemStack item : inventory) {
            if (item != null && item.getType().name().contains(toolType)) {
                // Prioritize higher tier tools if multiple exist (e.g. a wooden and stone)
                if (currentMaterial == null || getTierLevel(item.getType()) > getTierLevel(currentMaterial)) {
                    currentMaterial = item.getType();
                }
            }
        }

        // Default to needing the first tier if no tool is found
        if (currentMaterial == null) {
            if (toolType.equals("PICKAXE")) return new ToolUpgradeInfo(Material.WOODEN_PICKAXE, Material.IRON_INGOT, 5, "Wooden Pickaxe", false); // Cost for base wooden pickaxe
            if (toolType.equals("AXE")) return new ToolUpgradeInfo(Material.WOODEN_AXE, Material.IRON_INGOT, 5, "Wooden Axe", false); // Cost for base wooden axe
             return new ToolUpgradeInfo(Material.BARRIER, Material.AIR, 0, "Error", true); // Should not happen
        }

        if (toolType.equals("PICKAXE")) {
            if (currentMaterial == Material.WOODEN_PICKAXE) return new ToolUpgradeInfo(Material.STONE_PICKAXE, Material.IRON_INGOT, 10, "Stone Pickaxe", false);
            if (currentMaterial == Material.STONE_PICKAXE) return new ToolUpgradeInfo(Material.IRON_PICKAXE, Material.IRON_INGOT, 25, "Iron Pickaxe", false);
            if (currentMaterial == Material.IRON_PICKAXE) return new ToolUpgradeInfo(Material.DIAMOND_PICKAXE, Material.GOLD_INGOT, 5, "Diamond Pickaxe", false);
            if (currentMaterial == Material.DIAMOND_PICKAXE) return new ToolUpgradeInfo(Material.DIAMOND_PICKAXE, Material.AIR, 0, "Max Tier Pickaxe", true);
        } else if (toolType.equals("AXE")) {
            if (currentMaterial == Material.WOODEN_AXE) return new ToolUpgradeInfo(Material.STONE_AXE, Material.IRON_INGOT, 10, "Stone Axe", false);
            if (currentMaterial == Material.STONE_AXE) return new ToolUpgradeInfo(Material.IRON_AXE, Material.IRON_INGOT, 25, "Iron Axe", false);
            if (currentMaterial == Material.IRON_AXE) return new ToolUpgradeInfo(Material.DIAMOND_AXE, Material.GOLD_INGOT, 5, "Diamond Axe", false);
            if (currentMaterial == Material.DIAMOND_AXE) return new ToolUpgradeInfo(Material.DIAMOND_AXE, Material.AIR, 0, "Max Tier Axe", true);
        }
        return new ToolUpgradeInfo(Material.BARRIER, Material.AIR, 0, "Error", true); // Should indicate error or unknown state
    }

    private int getTierLevel(Material material) {
        if (material.name().contains("WOODEN")) return 1;
        if (material.name().contains("STONE")) return 2;
        if (material.name().contains("IRON")) return 3;
        if (material.name().contains("DIAMOND")) return 4;
        return 0; // Default for non-tiered or unknown
    }

    private int getArmorTierLevel(Material material) {
        if (material == null) return 0;
        String name = material.name();
        if (name.startsWith("LEATHER_")) return 1; // Team armor base
        if (name.startsWith("CHAINMAIL_")) return 2;
        if (name.startsWith("IRON_")) return 3;
        if (name.startsWith("DIAMOND_")) return 4;
        return 0; // Not armor or unknown
    }

    private int getPlayerArmorTier(Player player) {
        int maxTier = 0;
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            maxTier = Math.max(maxTier, getArmorTierLevel(armorPiece != null ? armorPiece.getType() : null));
        }
        return maxTier;
    }
}
