package org.bcnlab.beaconLabsBW.arena.model;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * A serializable representation of a Bukkit Location
 */
@Data
@NoArgsConstructor
public class SerializableLocation {
    
    @Expose
    private String worldName;
    
    @Expose
    private double x;
    
    @Expose
    private double y;
    
    @Expose
    private double z;
    
    @Expose
    private float yaw;
    
    @Expose
    private float pitch;
    
    /**
     * Construct from a Bukkit Location
     * 
     * @param location The location to convert
     */
    public SerializableLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Location or world cannot be null");
        }
        
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }
    
    /**
     * Convert back to a Bukkit Location
     * 
     * @return Bukkit Location object or null if world doesn't exist
     */
    public Location toBukkitLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        
        return new Location(world, x, y, z, yaw, pitch);
    }
}
