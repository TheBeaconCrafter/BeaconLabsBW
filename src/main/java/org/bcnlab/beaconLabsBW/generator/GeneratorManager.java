package org.bcnlab.beaconLabsBW.generator;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.arena.model.GeneratorData;
import org.bcnlab.beaconLabsBW.arena.model.SerializableLocation;

import java.util.UUID;

/**
 * Manages resource generators in the BedWars plugin
 */
public class GeneratorManager {
    
    private final BeaconLabsBW plugin;
    
    public GeneratorManager(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Create a new generator in an arena
     * 
     * @param arena The arena
     * @param type The generator type
     * @param location The generator location
     * @param team The team (null for shared generators)
     * @return The created generator data
     */
    public GeneratorData createGenerator(Arena arena, GeneratorType type, SerializableLocation location, String team) {
        if (arena == null || type == null || location == null) {
            return null;
        }
        
        // Generate unique ID for this generator
        String generatorId = UUID.randomUUID().toString().substring(0, 8);
        
        // Create generator data
        GeneratorData generatorData = new GeneratorData(generatorId, type, location, team);
        
        // Add to arena
        arena.getGenerators().put(generatorId, generatorData);
        
        return generatorData;
    }
    
    /**
     * Remove a generator from an arena
     * 
     * @param arena The arena
     * @param generatorId The generator ID
     * @return true if removed, false otherwise
     */
    public boolean removeGenerator(Arena arena, String generatorId) {
        if (arena == null || generatorId == null) {
            return false;
        }
        
        return arena.getGenerators().remove(generatorId) != null;
    }
}
