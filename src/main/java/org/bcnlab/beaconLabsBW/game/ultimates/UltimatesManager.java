package org.bcnlab.beaconLabsBW.game.ultimates;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ultimate abilities for BedWars Ultimates mode
 */
public class UltimatesManager implements Listener {

    private final BeaconLabsBW plugin;
    private final Map<UUID, BukkitTask> cooldownTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> abilityCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> healerTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> frozenPlayers = new ConcurrentHashMap<>();
    
    // Constants for ultimates
    private static final int SWORDSMAN_DASH_COOLDOWN = 10; // seconds
    private static final int HEALER_AURA_COOLDOWN = 15; // seconds
    private static final int FROZO_SLOWNESS_COOLDOWN = 20; // seconds
    private static final int FAST_BRIDGE_COOLDOWN = 1; // seconds
    private static final int GATHERER_DUPLICATION_CHANCE = 25; // percentage
    private static final int KANGAROO_JUMP_COOLDOWN = 5; // seconds

    public UltimatesManager(BeaconLabsBW plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Create an ultimate item for a player based on their class
     * 
     * @param player The player
     * @param ultimateClass The ultimate class
     * @return The ultimate item
     */
    public ItemStack createUltimateItem(Player player, UltimateClass ultimateClass) {
        ItemStack item;

        switch (ultimateClass) {
            case SWORDSMAN -> item = createItem(Material.BLAZE_ROD, ChatColor.RED + "Swordsman's Dash", 
                ChatColor.GRAY + "Right-click to dash forward", ChatColor.GRAY + "and damage enemies in your path");
                
            case HEALER -> item = createItem(Material.GOLDEN_APPLE, ChatColor.GREEN + "Healer's Aura", 
                ChatColor.GRAY + "Right-click to activate healing", ChatColor.GRAY + "for nearby teammates");
                
            case FROZO -> item = createItem(Material.PACKED_ICE, ChatColor.AQUA + "Frozo's Blast", 
                ChatColor.GRAY + "Right-click to slow nearby enemies");
                
            case BUILDER -> item = createItem(Material.BRICKS, ChatColor.YELLOW + "Builder's Tool", 
                ChatColor.GRAY + "Place wool blocks quickly", ChatColor.GRAY + "to build bridges and walls");
                
            case GATHERER -> item = createItem(Material.ENDER_CHEST, ChatColor.LIGHT_PURPLE + "Gatherer's Chest", 
                ChatColor.GRAY + "Right-click for portable storage", ChatColor.GRAY + "25% chance to duplicate resources");
                
            case DEMOLITION -> item = createItem(Material.FIRE_CHARGE, ChatColor.DARK_RED + "Demolition Charge", 
                ChatColor.GRAY + "Right-click to ignite wool", ChatColor.GRAY + "Drops TNT on death");
                
            case KANGAROO -> item = createItem(Material.RABBIT_FOOT, ChatColor.GOLD + "Kangaroo Jump", 
                ChatColor.GRAY + "Double-tap space to double jump", ChatColor.GRAY + "50% chance to save items on death");
                
            default -> item = new ItemStack(Material.BARRIER);
        }

        return item;
    }

    /**
     * Helper method to create an item with name and lore
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Activate a player's ultimate ability based on their class
     * 
     * @param player The player
     * @param ultimateClass The ultimate class
     * @return True if activated successfully
     */
    public boolean activateUltimate(Player player, UltimateClass ultimateClass) {
        UUID playerId = player.getUniqueId();
        
        // Check for cooldown
        if (abilityCooldowns.containsKey(playerId)) {
            long timeLeft = (abilityCooldowns.get(playerId) - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Ability on cooldown for " + timeLeft + " seconds!");
                return false;
            }
        }

        // Activate based on class
        switch (ultimateClass) {
            case SWORDSMAN -> activateSwordsmanDash(player);
            case HEALER -> activateHealerAura(player);
            case FROZO -> activateFrozo(player);
            case BUILDER -> {} // Passive ability
            case GATHERER -> activateGathererChest(player);
            case DEMOLITION -> activateDemolitionCharge(player);
            case KANGAROO -> {} // Double jump handled by event listener
            default -> {
                return false;
            }
        }

        return true;
    }

    /**
     * Activate Swordsman's dash ability
     */
    private void activateSwordsmanDash(Player player) {
        // Set cooldown
        UUID playerId = player.getUniqueId();
        abilityCooldowns.put(playerId, System.currentTimeMillis() + (SWORDSMAN_DASH_COOLDOWN * 1000));
        
        // Calculate dash direction
        Vector direction = player.getLocation().getDirection().multiply(2.0).setY(0.2);
        
        // Apply dash
        player.setVelocity(direction);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1));
        
        // Damage nearby players in the path
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (target != player && target.getWorld() == player.getWorld() &&
                    target.getLocation().distance(player.getLocation()) < 2.0) {
                    target.damage(4.0, player);
                    target.setVelocity(direction.clone().multiply(0.5));
                }
            }
        }, 5L);
        
        player.sendMessage(ChatColor.RED + "Swordsman Dash activated!");
    }

    /**
     * Activate Healer's aura ability
     */    private void activateHealerAura(Player player) {
        // Set cooldown
        UUID playerId = player.getUniqueId();
        abilityCooldowns.put(playerId, System.currentTimeMillis() + (HEALER_AURA_COOLDOWN * 1000));
        
        // Cancel any existing task
        if (healerTasks.containsKey(playerId)) {
            healerTasks.get(playerId).cancel();
        }
        
        // Initial visual and sound effects
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0),
                                      30, 0.5, 0.5, 0.5, 0.1);
                                      
        // Start healing aura
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Display healing aura effect
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 
                                         5, 1.5, 0.5, 1.5, 0.1);
            
            // Heal nearby teammates
            String playerTeam = plugin.getGameManager().getPlayerGame(player).getPlayerTeam(player);
            if (playerTeam != null) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (target != player && target.getWorld() == player.getWorld() &&
                        target.getLocation().distance(player.getLocation()) < 5.0 &&
                        playerTeam.equals(plugin.getGameManager().getPlayerGame(target).getPlayerTeam(target))) {
                        
                        // Apply healing
                        if (target.getHealth() < target.getMaxHealth()) {
                            target.setHealth(Math.min(target.getHealth() + 1.0, target.getMaxHealth()));
                            target.sendMessage(ChatColor.GREEN + "You are being healed by " + player.getName() + "!");
                            
                            // Visual effect on healed player
                            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1, 0), 
                                                       2, 0.3, 0.3, 0.3, 0);
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                        }
                    }
                }
            }
        }, 20L, 20L);
        
        healerTasks.put(playerId, task);
        
        // Cancel task after 10 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (healerTasks.containsKey(playerId)) {
                healerTasks.get(playerId).cancel();
                healerTasks.remove(playerId);
                player.sendMessage(ChatColor.GREEN + "Healing aura has ended.");
                
                // End sound effect
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.5f);
            }
        }, 200L);
        
        player.sendMessage(ChatColor.GREEN + "Healing aura activated for 10 seconds!");
    }

    /**
     * Activate Frozo's slow ability
     */    private void activateFrozo(Player player) {
        // Set cooldown
        UUID playerId = player.getUniqueId();
        abilityCooldowns.put(playerId, System.currentTimeMillis() + (FROZO_SLOWNESS_COOLDOWN * 1000));
        
        // Get player team
        String playerTeam = plugin.getGameManager().getPlayerGame(player).getPlayerTeam(player);
        
        // Visual effect for activation
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, player.getLocation().add(0, 1, 0),
                                     30, 3.0, 0.5, 3.0, 0.1);
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(),
                                     50, 3.0, 0.2, 3.0, 0.1);
                                     
        // Create a visual ice wave effect that expands outward
        for (int i = 1; i <= 5; i++) {
            final int radius = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    player.getWorld().spawnParticle(
                        Particle.FALLING_DUST, 
                        player.getLocation().add(x, 0, z), 
                        5, 0.2, 0, 0.2, 0, Material.ICE.createBlockData()
                    );
                }
            }, i * 2L);
        }
        
        // Apply slowness to nearby enemies
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target != player && target.getWorld() == player.getWorld() &&
                target.getLocation().distance(player.getLocation()) < 5.0) {
                
                String targetTeam = plugin.getGameManager().getPlayerGame(target).getPlayerTeam(target);
                if (!playerTeam.equals(targetTeam)) {
                    // Apply slowness and mining fatigue
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 0));
                    target.sendMessage(ChatColor.AQUA + "You've been frozen by " + player.getName() + "!");
                    
                    // Play sound at target
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 1.2f);
                    
                    // Track frozen players
                    UUID targetId = target.getUniqueId();
                    if (frozenPlayers.containsKey(targetId)) {
                        frozenPlayers.get(targetId).cancel();
                    }
                    
                    // Add frost particle effects
                    BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                        target.getWorld().spawnParticle(
                            Particle.ITEM_SNOWBALL,
                            target.getLocation().add(0, 1, 0), 
                            10, 0.5, 0.5, 0.5, 0
                        );
                        // Add ice block particles
                        target.getWorld().spawnParticle(
                            Particle.FALLING_DUST,
                            target.getLocation().add(0, 0.5, 0),
                            3, 0.2, 0.2, 0.2, 0, Material.ICE.createBlockData()
                        );
                    }, 0L, 5L);
                    
                    frozenPlayers.put(targetId, task);
                    
                    // Cancel effects after 5 seconds
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (frozenPlayers.containsKey(targetId)) {
                            frozenPlayers.get(targetId).cancel();
                            frozenPlayers.remove(targetId);
                            // Play thawing sound
                            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.2f);
                            target.sendMessage(ChatColor.GREEN + "You are no longer frozen!");
                        }
                    }, 100L);
                }
            }
        }
        
        player.sendMessage(ChatColor.AQUA + "Frozo's Blast activated!");
    }

    /**
     * Activate Gatherer's Ender Chest ability
     */
    private void activateGathererChest(Player player) {
        // Open ender chest
        player.openInventory(player.getEnderChest());
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Portable Ender Chest opened!");
    }

    /**
     * Activate Demolition Charge ability
     */
    private void activateDemolitionCharge(Player player) {
        // Set cooldown 
        UUID playerId = player.getUniqueId();
        abilityCooldowns.put(playerId, System.currentTimeMillis() + 15000); // 15 seconds
        
        // Find targeted block
        Block targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock != null && targetBlock.getType().toString().contains("WOOL")) {
            // Start fire chain reaction
            burnConnectedWool(targetBlock, new HashSet<>());
            player.sendMessage(ChatColor.DARK_RED + "Wool ignited!");
        } else {
            player.sendMessage(ChatColor.DARK_RED + "Must target wool blocks!");
            // Refund cooldown
            abilityCooldowns.remove(playerId);
        }
    }
    
    /**
     * Burns connected wool blocks recursively
     */
    private void burnConnectedWool(Block block, Set<Block> processed) {
        // Check if block is wool and not already processed
        if (block != null && block.getType().toString().contains("WOOL") && !processed.contains(block)) {
            processed.add(block);
            
            // Set block on fire
            Block above = block.getRelative(BlockFace.UP);
            if (above.getType().isAir()) {
                above.setType(Material.FIRE);
            }
            
            // Schedule block to burn away
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                block.setType(Material.AIR);
            }, 20L); // 1 second delay
            
            // Check adjacent blocks (limit recursion depth to avoid lag)
            if (processed.size() < 50) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, 
                                                         BlockFace.SOUTH, BlockFace.WEST,
                                                         BlockFace.UP, BlockFace.DOWN}) {
                        burnConnectedWool(block.getRelative(face), processed);
                    }
                }, 5L);
            }
        }
    }

    /**
     * Handle Builder's fast bridge ability
     */
    public void handleFastBridge(Player player, Block block, BlockFace face) {
        // Only work with wool blocks
        if (!block.getType().toString().contains("WOOL")) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        if (abilityCooldowns.containsKey(playerId) && 
            abilityCooldowns.get(playerId) > System.currentTimeMillis()) {
            return;
        }
        
        // Set short cooldown to prevent spam
        abilityCooldowns.put(playerId, System.currentTimeMillis() + (FAST_BRIDGE_COOLDOWN * 1000));
        
        // Get material from player's hand
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType().toString().contains("WOOL")) {
            Material woolType = handItem.getType();
            
            // Place a line of 5 blocks
            BlockFace[] directions = {face, face.getOppositeFace().getOppositeFace().getOppositeFace(),
                                     face.getOppositeFace(), face.getOppositeFace().getOppositeFace()};
            
            int blocksPlaced = 0;
            for (BlockFace direction : directions) {
                Block targetBlock = block.getRelative(direction);
                if (targetBlock.getType().isAir() && handItem.getAmount() > 0) {
                    targetBlock.setType(woolType);
                    blocksPlaced++;
                    
                    // Remove from player's inventory
                    if (!player.getGameMode().toString().contains("CREATIVE")) {
                        handItem.setAmount(handItem.getAmount() - 1);
                    }
                }
            }
            
            if (blocksPlaced > 0) {
                player.sendMessage(ChatColor.YELLOW + "Fast bridge activated!");
            }
        }
    }
      /**
     * Process Kangaroo double jump 
     */
    public boolean processKangarooJump(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (abilityCooldowns.containsKey(playerId) && 
            abilityCooldowns.get(playerId) > System.currentTimeMillis()) {
            return false;
        }
        
        // Set cooldown
        abilityCooldowns.put(playerId, System.currentTimeMillis() + (KANGAROO_JUMP_COOLDOWN * 1000));
        
        // Apply jump boost
        Vector velocity = player.getVelocity();
        velocity.setY(0.8);
        player.setVelocity(velocity);
        
        // Add temporary fall damage immunity
        player.setFallDistance(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 3, false, false));
        
        // Add visual and sound effects
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.1, 0.3, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
        
        // Add trail particles
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (player.isOnline() && !player.isOnGround()) {
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, -0.2, 0), 
                                               1, 0, 0, 0, 0);
            } else {
                task.cancel();
            }
        }, 1L, 2L);
        
        player.sendMessage(ChatColor.GOLD + "Double Jump!");
        return true;
    }
    
    /**
     * Check if gatherer should duplicate a resource
     */
    public boolean shouldDuplicateResource(Player player) {
        // Check if player is a gatherer
        UUID playerId = player.getUniqueId();
        if (plugin.getGameManager().getPlayerGame(player) == null) {
            return false;
        }
        
        UltimateClass playerClass = plugin.getGameManager().getPlayerGame(player).getPlayerUltimateClass(playerId);
        if (playerClass == UltimateClass.GATHERER) {
            // 25% chance to duplicate
            return new Random().nextInt(100) < GATHERER_DUPLICATION_CHANCE;
        }
        return false;
    }
    
    /**
     * Handle TNT drop for Demolition class on death
     */
    public void handleDemolitionDeath(Player player) {
        UUID playerId = player.getUniqueId();
        if (plugin.getGameManager().getPlayerGame(player) == null) {
            return;
        }
        
        UltimateClass playerClass = plugin.getGameManager().getPlayerGame(player).getPlayerUltimateClass(playerId);
        if (playerClass == UltimateClass.DEMOLITION) {
            // Drop TNT at death location
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.TNT, 1));
            player.getWorld().createExplosion(player.getLocation(), 0.0F, false, false);
        }
    }
    
    /**
     * Handle Kangaroo resource saving on death (50% chance)
     */
    public List<ItemStack> processKangarooDeathItems(Player player, List<ItemStack> drops) {
        UUID playerId = player.getUniqueId();
        if (plugin.getGameManager().getPlayerGame(player) == null) {
            return drops;
        }
        
        UltimateClass playerClass = plugin.getGameManager().getPlayerGame(player).getPlayerUltimateClass(playerId);
        if (playerClass == UltimateClass.KANGAROO && new Random().nextBoolean()) {
            // 50% chance to save items
            List<ItemStack> savedItems = new ArrayList<>();
            
            // Save only resources (iron, gold, emerald, diamond)
            for (ItemStack item : drops) {
                Material type = item.getType();
                if (type == Material.IRON_INGOT || type == Material.GOLD_INGOT || 
                    type == Material.EMERALD || type == Material.DIAMOND) {
                    
                    // Save half of the resources
                    if (item.getAmount() > 1) {
                        ItemStack half = item.clone();
                        half.setAmount(item.getAmount() / 2);
                        savedItems.add(half);
                    }
                }
            }
            
            if (!savedItems.isEmpty()) {
                player.sendMessage(ChatColor.GOLD + "Your Kangaroo ability saved some resources!");
                return savedItems;
            }
        }
        
        return drops;
    }
    
    /**
     * Give magic milk to Kangaroo when they break a bed
     */
    public void giveMagicMilk(Player player) {
        UUID playerId = player.getUniqueId();
        if (plugin.getGameManager().getPlayerGame(player) == null) {
            return;
        }
        
        UltimateClass playerClass = plugin.getGameManager().getPlayerGame(player).getPlayerUltimateClass(playerId);
        if (playerClass == UltimateClass.KANGAROO) {
            // Create magic milk
            ItemStack milk = new ItemStack(Material.MILK_BUCKET);
            ItemMeta meta = milk.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Magic Milk");
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Prevents you from triggering",
                        ChatColor.GRAY + "enemy traps for 30 seconds"));
                milk.setItemMeta(meta);
            }
            
            // Give to player
            player.getInventory().addItem(milk);
            player.sendMessage(ChatColor.GOLD + "Your Kangaroo ability gave you Magic Milk!");
        }
    }
    
    /**
     * Give diamond upgrade to Gatherer when their bed is destroyed
     */
    public void giveGathererDiamondUpgrade(Player player) {
        UUID playerId = player.getUniqueId();
        if (plugin.getGameManager().getPlayerGame(player) == null) {
            return;
        }
        
        UltimateClass playerClass = plugin.getGameManager().getPlayerGame(player).getPlayerUltimateClass(playerId);
        if (playerClass == UltimateClass.GATHERER) {
            // Give diamonds
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Your Gatherer ability gave you diamonds as compensation!");
        }
    }
}
