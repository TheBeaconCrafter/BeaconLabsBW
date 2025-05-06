package org.bcnlab.beaconLabsBW.generator;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.GeneratorData;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import static org.bcnlab.beaconLabsBW.generator.GeneratorType.*;

/**
 * Represents an active resource generator in a BedWars game
 */
public class ActiveGenerator {
    
    private final BeaconLabsBW plugin;
    private final GeneratorData generatorData;
    private final Game game;
    private final Location location;
    
    private BukkitTask generatorTask;
    private ArmorStand hologram;
    
    private int timer;
    private final int interval;      /**
     * Creates a new active generator
     *
     * @param plugin The plugin instance
     * @param generatorData The generator data
     * @param game The game this generator belongs to
     */    public ActiveGenerator(BeaconLabsBW plugin, GeneratorData generatorData, Game game) {
        this.plugin = plugin;
        this.generatorData = generatorData;
        this.game = game;
        this.location = generatorData.getLocation().toBukkitLocation();
          // Base intervals based on generator type
        int baseInterval = switch (generatorData.getType()) {
            case IRON -> plugin.getConfigManager().getIronInterval();
            case GOLD -> plugin.getConfigManager().getGoldInterval();
            case TEAM -> plugin.getConfigManager().getIronInterval(); // TEAM generator uses iron interval as base
            case EMERALD -> plugin.getConfigManager().getEmeraldInterval();
            case DIAMOND -> plugin.getConfigManager().getDiamondInterval();
        };
        
        // Apply forge upgrade interval reduction for TEAM generators
        if (generatorData.getType() == GeneratorType.TEAM) {
            String teamName = generatorData.getTeam();
            if (teamName != null) {
                int forgeLevel = game.getPlugin().getTeamUpgradeManager().getUpgradeLevel(
                    teamName, 
                    org.bcnlab.beaconLabsBW.shop.TeamUpgrade.UpgradeType.FORGE
                );
                
                // Reduce interval based on forge level
                if (forgeLevel > 0) {
                    baseInterval = switch (forgeLevel) {
                        case 1 -> Math.max(baseInterval - 1, 1); // 1 second faster
                        case 2 -> Math.max(baseInterval - 2, 1); // 2 seconds faster
                        case 3 -> Math.max(baseInterval - 3, 1); // 3 seconds faster
                        default -> baseInterval;
                    };
                }
            }
        }
        
        // Scale interval based on number of players (faster with more players)
        int playerCount = game.getPlayers().size();
        if (playerCount > 0) {
            int scaleFactor = Math.min(Math.max(playerCount / 2, 1), 4); // Scale factor between 1-4
            // Reduce interval (faster spawning) with more players
            this.interval = Math.max(baseInterval - (scaleFactor - 1), 1);
        } else {
            this.interval = baseInterval;
        }
        
        this.timer = interval;
    }
    
    /**
     * Start the generator
     */
    public void start() {
        if (location == null) return;
        
        // Create hologram
        createHologram();
          // Start spawning items
        generatorTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if game still exists and is running
                if (game == null || game.getState() != org.bcnlab.beaconLabsBW.game.GameState.RUNNING) {
                    return;
                }
                
                if (timer <= 0) {
                    // Spawn item
                    spawnResource();
                    timer = interval;
                    updateHologram();
                } else {
                    timer--;
                    
                    if (timer % 5 == 0) {
                        updateHologram();
                    }
                }
                
                // Spawn particles
                spawnParticles();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    /**
     * Stop the generator
     */
    public void stop() {
        if (generatorTask != null) {
            generatorTask.cancel();
        }
        
        if (hologram != null) {
            hologram.remove();
        }
    }
    
    /**
     * Create the hologram display
     */
    private void createHologram() {
        if (location == null) return;
        
        World world = location.getWorld();
        if (world == null) return;
        
        Location holoLoc = location.clone().add(0, 1.5, 0);
        
        hologram = (ArmorStand) world.spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setMarker(true);
        hologram.setCustomNameVisible(true);
        
        updateHologram();
    }
      /**
     * Update the hologram text
     */
    private void updateHologram() {
        if (hologram == null) return;
          String type = switch (generatorData.getType()) {
            case IRON -> "§f§lIron";
            case GOLD -> "§6§lGold";
            case TEAM -> ""; // No hologram for TEAM generators, we'll handle this below
            case EMERALD -> "§a§lEmerald";
            case DIAMOND -> "§b§lDiamond";
        };
        
        // Don't show holograms for team generators
        if (generatorData.getType() == GeneratorType.TEAM) {
            hologram.setCustomNameVisible(false);
        } else {
            hologram.setCustomNameVisible(true);
            hologram.setCustomName(type + " §7- §r" + timer + "s");
        }
    }
      /**
     * Spawn resource item
     */
    private void spawnResource() {
        if (location == null) return;
        
        World world = location.getWorld();
        if (world == null) return;
          
        // For TEAM generators, we need to handle dropping both iron and gold
        if (generatorData.getType() == GeneratorType.TEAM) {
            // Handle team generator with forge upgrade
            String teamName = generatorData.getTeam();
            int forgeLevel = 0;
            
            if (teamName != null) {
                forgeLevel = game.getPlugin().getTeamUpgradeManager().getUpgradeLevel(
                    teamName, 
                    org.bcnlab.beaconLabsBW.shop.TeamUpgrade.UpgradeType.FORGE
                );
            }
              // Always drop iron, with quantity based on forge level
            int ironCount = 1;
            if (forgeLevel > 0) {
                // Increase iron output based on forge level
                switch (forgeLevel) {
                    case 1 -> ironCount = 2;    // 100% more (+1)
                    case 2 -> ironCount = 2;    // 100% more (+1)
                    case 3 -> ironCount = 3;    // 200% more (+2)
                }
            }
            
            // Drop iron with appropriate count
            for (int i = 0; i < ironCount; i++) {
                dropResource(world, location, Material.IRON_INGOT);
            }
            
            // Drop gold based on forge upgrade
            if (forgeLevel > 0) {
                // Always drop gold if forge is upgraded
                int goldCount = switch (forgeLevel) {
                    case 1 -> 1;    // 1 gold
                    case 2 -> 2;    // 2 gold 
                    case 3 -> 3;    // 3 gold
                    default -> 1;
                };
                
                for (int i = 0; i < goldCount; i++) {
                    dropResource(world, location, Material.GOLD_INGOT);
                }
            } else {
                // No forge upgrades, 40% chance for gold
                if (Math.random() < 0.4) {
                    dropResource(world, location, Material.GOLD_INGOT);
                }
            }
            
            return;
        }
          
        // For regular generators
        Material material = switch (generatorData.getType()) {
            case IRON -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case TEAM -> Material.IRON_INGOT; // Default to iron (shouldn't reach here due to the check above)
            case EMERALD -> Material.EMERALD;
            case DIAMOND -> Material.DIAMOND;
        };
        
        dropResource(world, location, material);
    }
    
    /**
     * Helper method to drop a resource item
     * 
     * @param world The world to drop in
     * @param location The location to drop at
     * @param material The material to drop
     */
    private void dropResource(World world, Location location, Material material) {
        ItemStack item = new ItemStack(material, 1);
        Location spawnLoc = location.clone().add(0, 0.5, 0);
        
        Item droppedItem = world.dropItem(spawnLoc, item);
        droppedItem.setVelocity(new Vector(0, 0.1, 0));
        
        // Set custom pickup delay to prevent immediately picking up
        droppedItem.setPickupDelay(10);
        
        // Entity collection after 45 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (droppedItem.isValid() && !droppedItem.isDead()) {
                    droppedItem.remove();
                }
            }
        }.runTaskLater(plugin, 900L); // 45 seconds
    }
      /**
     * Spawn particles around the generator
     */
    private void spawnParticles() {
        if (location == null) return;
        
        World world = location.getWorld();
        if (world == null) return;        Particle particle = switch (generatorData.getType()) {
            case IRON -> Particle.CRIT;
            case GOLD -> Particle.FLAME;
            case TEAM -> timer % 2 == 0 ? Particle.CRIT : Particle.FLAME; // Alternate between iron and gold particles
            case EMERALD -> Particle.HAPPY_VILLAGER;
            case DIAMOND -> Particle.FLAME;
        };
        
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            double x = location.getX() + 0.5 * Math.cos(angle);
            double z = location.getZ() + 0.5 * Math.sin(angle);
            
            world.spawnParticle(
                particle,
                x,
                location.getY() + 0.2,
                z,
                1,
                0, 0.1, 0,
                0
            );
        }
    }
}
