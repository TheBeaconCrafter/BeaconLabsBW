package org.bcnlab.beaconLabsBW.arena.model;

import com.google.gson.annotations.Expose;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

import org.bcnlab.beaconLabsBW.shop.ShopVillagerData;

/**
 * Represents a BedWars arena
 */
@Data
public class Arena {
    
    @Expose
    private String name;
    
    @Expose
    private String worldName;
    
    @Expose
    private Map<String, TeamData> teams = new HashMap<>();
    
    @Expose
    private Map<String, GeneratorData> generators = new HashMap<>();
    
    @Expose
    private SerializableLocation lobbySpawn;
    
    @Expose
    private SerializableLocation spectatorSpawn;
    
    @Expose
    private boolean enabled = false;
    
    @Expose
    private int minPlayers = 2;
    
    @Expose
    private int maxPlayers = 16;
    
    @Expose
    private Map<String, ShopVillagerData> shopVillagers = new HashMap<>();
    
    // Transient properties (not serialized)
    private transient World world;
    
    /**
     * Creates a new BedWars arena with the specified name
     * 
     * @param name Name of the arena
     * @param worldName Name of the world
     */
    public Arena(String name, String worldName) {
        this.name = name;
        this.worldName = worldName;
    }
    
    /**
     * Add a team to this arena
     * 
     * @param teamName Name of the team (e.g., "Red", "Blue")
     * @param teamData Team data containing spawn locations and bed location
     */
    public void addTeam(String teamName, TeamData teamData) {
        teams.put(teamName, teamData);
    }
    
    /**
     * Get a team from this arena
     * 
     * @param teamName Name of the team
     * @return TeamData or null if not found
     */
    public TeamData getTeam(String teamName) {
        return teams.get(teamName);
    }
    
    /**
     * Checks if the arena is fully configured and ready to play
     * 
     * @return True if ready, false otherwise
     */
    public boolean isConfigured() {
        if (teams.isEmpty() || lobbySpawn == null || spectatorSpawn == null) {
            return false;
        }
        
        // Check that each team has a spawn and bed location
        for (TeamData team : teams.values()) {
            if (team.getSpawnLocation() == null || team.getBedLocation() == null) {
                return false;
            }
        }
          // Need at least one generator of each type
        boolean hasIron = false;
        boolean hasGold = false;
        boolean hasEmerald = false;
        boolean hasDiamond = false;
          for (GeneratorData generator : generators.values()) {
            switch (generator.getType()) {
                case IRON -> hasIron = true;
                case GOLD -> hasGold = true;
                case TEAM -> {
                    // TEAM generators count as both iron and gold
                    hasIron = true;
                    hasGold = true;
                }
                case EMERALD -> hasEmerald = true;
                case DIAMOND -> hasDiamond = true;
            }
        }
        
        // Diamond generators are optional
        return hasIron && hasGold && hasEmerald;
    }
}
