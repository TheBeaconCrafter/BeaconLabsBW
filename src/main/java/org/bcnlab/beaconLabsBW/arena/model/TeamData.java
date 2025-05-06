package org.bcnlab.beaconLabsBW.arena.model;

import com.google.gson.annotations.Expose;
import lombok.Data;

/**
 * Represents a team in the BedWars game
 */
@Data
public class TeamData {
    
    @Expose
    private String name;
    
    @Expose
    private String color;
    
    @Expose
    private SerializableLocation spawnLocation;
    
    @Expose
    private SerializableLocation bedLocation;
    
    /**
     * Creates a new team with the given name and color
     * 
     * @param name Team name
     * @param color Team color (e.g., "RED", "BLUE")
     */
    public TeamData(String name, String color) {
        this.name = name;
        this.color = color;
    }
}
