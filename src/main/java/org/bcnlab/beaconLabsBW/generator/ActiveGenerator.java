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
    private final int interval;
      /**
     * Creates a new active generator
     *
     * @param plugin The plugin instance
     * @param generatorData The generator data
     * @param game The game this generator belongs to
     */
    public ActiveGenerator(BeaconLabsBW plugin, GeneratorData generatorData, Game game) {
        this.plugin = plugin;
        this.generatorData = generatorData;
        this.game = game;
        this.location = generatorData.getLocation().toBukkitLocation();
          // Base intervals based on generator type
        int baseInterval = switch (generatorData.getType()) {
            case IRON -> plugin.getConfigManager().getIronInterval();
            case GOLD -> plugin.getConfigManager().getGoldInterval();
            case EMERALD -> plugin.getConfigManager().getEmeraldInterval();
            case DIAMOND -> plugin.getConfigManager().getDiamondInterval();
        };
        
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
            case EMERALD -> "§a§lEmerald";
            case DIAMOND -> "§b§lDiamond";
        };
        
        hologram.setCustomName(type + " §7- §r" + timer + "s");
    }
    
    /**
     * Spawn resource item
     */
    private void spawnResource() {
        if (location == null) return;
        
        World world = location.getWorld();
        if (world == null) return;
          Material material = switch (generatorData.getType()) {
            case IRON -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case EMERALD -> Material.EMERALD;
            case DIAMOND -> Material.DIAMOND;
        };
        
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
