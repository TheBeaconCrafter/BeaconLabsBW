package org.bcnlab.beaconLabsBW.arena.model;

import com.google.gson.annotations.Expose;
import lombok.Data;
import org.bcnlab.beaconLabsBW.generator.GeneratorType;

/**
 * Represents a resource generator in BedWars
 */
@Data
public class GeneratorData {
    
    @Expose
    private String id;
    
    @Expose
    private GeneratorType type;
    
    @Expose
    private SerializableLocation location;
    
    @Expose
    private String team; // null for shared generators
    
    /**
     * Creates a new generator configuration
     * 
     * @param id Unique identifier for this generator
     * @param type Type of resource (IRON, GOLD, EMERALD)
     * @param location Location of the generator
     * @param team Team that owns this generator, or null for shared
     */
    public GeneratorData(String id, GeneratorType type, SerializableLocation location, String team) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.team = team;
    }
}
