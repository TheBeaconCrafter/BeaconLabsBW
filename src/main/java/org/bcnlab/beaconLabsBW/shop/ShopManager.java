package org.bcnlab.beaconLabsBW.shop;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
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
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Wool", Material.WHITE_WOOL, 4, Material.IRON_INGOT, 1, "4 wool blocks"));
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Stone Sword", Material.STONE_SWORD, 1, Material.IRON_INGOT, 10, "Stone sword"));
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Chainmail Armor", Material.CHAINMAIL_CHESTPLATE, 1, Material.IRON_INGOT, 40, "Chainmail armor protection"));
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Bow", Material.BOW, 1, Material.GOLD_INGOT, 12, "Bow"));
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Arrows", Material.ARROW, 8, Material.GOLD_INGOT, 2, "8 arrows"));
        addItem(ShopCategory.QUICK_BUY, new ShopItem("Speed Potion", Material.POTION, 1, Material.EMERALD, 1, "Speed II (45 seconds)"));
        
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
        addItem(ShopCategory.TOOLS, new ShopItem("Pickaxe", Material.IRON_PICKAXE, 1, Material.IRON_INGOT, 10, "Permanent iron pickaxe"));
        addItem(ShopCategory.TOOLS, new ShopItem("Axe", Material.IRON_AXE, 1, Material.IRON_INGOT, 10, "Permanent iron axe"));
        
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
     */
    public void openShop(Player player) {
        if (player == null) return;
        
        // Default to quick buy category
        ShopCategory category = playerCategory.getOrDefault(player.getUniqueId(), ShopCategory.QUICK_BUY);
        openCategoryMenu(player, category);
    }
    
    /**
     * Open a specific category menu for a player
     * 
     * @param player The player
     * @param category The category
     */
    public void openCategoryMenu(Player player, ShopCategory category) {
        if (player == null || category == null) return;
        
        // Store player's current category
        playerCategory.put(player.getUniqueId(), category);
        
        // Create inventory
        Inventory inventory = Bukkit.createInventory(
            null,
            54,
            ChatColor.DARK_GRAY + "BedWars Shop: " + category.getDisplayName()
        );
        
        // Add category selector items at the top
        int slot = 0;
        for (ShopCategory cat : ShopCategory.values()) {
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
                inventory.setItem(slot++, createShopItemStack(item));
            }
        }
        
        // Open the inventory
        player.openInventory(inventory);
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
     * @return ItemStack representation
     */
    private ItemStack createShopItemStack(ShopItem shopItem) {
        ItemStack item = new ItemStack(shopItem.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + shopItem.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + shopItem.getDescription());
            lore.add("");
            
            String costText = ChatColor.WHITE + "Cost: " + ChatColor.YELLOW + shopItem.getCost() + " ";
            switch (shopItem.getCurrency()) {
                case IRON_INGOT -> lore.add(costText + ChatColor.WHITE + "Iron");
                case GOLD_INGOT -> lore.add(costText + ChatColor.GOLD + "Gold");
                case EMERALD -> lore.add(costText + ChatColor.GREEN + "Emerald");
                default -> lore.add(costText + "Unknown");
            }
            
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to purchase!");
              meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            // Add potion flag if it exists in this version
            try {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_POTION_EFFECTS"));
            } catch (IllegalArgumentException ignored) {
                // Flag doesn't exist in this version, just continue
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
    }
      /**
     * Process a shop purchase
     * 
     * @param player The player
     * @param item The item being purchased
     */
    private void processPurchase(Player player, ShopItem item) {
        // Check if player has enough currency
        if (!hasCurrency(player, item.getCurrency(), item.getCost())) {
            String currencyName = getCurrencyName(item.getCurrency());
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have enough " + currencyName + "!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            return;
        }
        
        // Special handling for sword upgrades - remove existing swords
        if (item.getMaterial().name().contains("SWORD")) {
            removeExistingSwords(player);
        }
        
        // Handle permanent tool upgrades - don't let players buy the same tool twice
        if (!handlePermanentToolUpgrades(player, item)) {
            return;
        }
        
        // Remove the currency
        removeCurrency(player, item.getCurrency(), item.getCost());
        
        // Give the item
        ItemStack itemStack = new ItemStack(item.getMaterial(), item.getAmount());
        
        // Apply item customizations
        customizeShopItem(itemStack, item);
        
        // Special handling for armor
        if (isArmor(item.getMaterial())) {
            equipArmor(player, itemStack);
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
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
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
     */
    private void customizeShopItem(ItemStack itemStack, ShopItem shopItem) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            // Set custom name if needed
            if (shopItem.getName() != null && !shopItem.getName().isEmpty()) {
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
        
        if (type.name().contains("HELMET")) {
            player.getInventory().setHelmet(armor);
        } else if (type.name().contains("CHESTPLATE")) {
            player.getInventory().setChestplate(armor);
        } else if (type.name().contains("LEGGINGS")) {
            player.getInventory().setLeggings(armor);
        } else if (type.name().contains("BOOTS")) {
            player.getInventory().setBoots(armor);
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
}
