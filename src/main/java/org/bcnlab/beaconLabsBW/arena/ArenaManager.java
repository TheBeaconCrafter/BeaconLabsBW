package org.bcnlab.beaconLabsBW.arena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages the creation, loading, and saving of BedWars arenas
 */
public class ArenaManager {
    
    private final BeaconLabsBW plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final File arenaDirectory;
    private final Gson gson;
    
    // Track players currently editing arenas
    private final Map<Player, Arena> editing = new HashMap<>();
    
    public ArenaManager(BeaconLabsBW plugin) {
        this.plugin = plugin;
        this.arenaDirectory = new File(plugin.getDataFolder(), "arenas");
        
        // Create arenas directory if it doesn't exist
        if (!arenaDirectory.exists()) {
            arenaDirectory.mkdirs();
        }
        
        this.gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();
            
        // Load all arenas from disk
        loadArenas();
    }
    
    /**
     * Get an arena by name
     * 
     * @param name Arena name
     * @return The arena or null if not found
     */
    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }
    
    /**
     * Get all available arenas
     * 
     * @return Set of arena names
     */
    public Set<String> getArenaNames() {
        return arenas.keySet();
    }
    
    /**
     * Check if an arena exists
     * 
     * @param name Arena name
     * @return true if exists, false otherwise
     */
    public boolean arenaExists(String name) {
        return arenas.containsKey(name.toLowerCase());
    }
    
    /**
     * Create a new arena
     * 
     * @param name Arena name
     * @param worldName World name
     * @return The new arena, or null if one with that name already exists
     */
    public Arena createArena(String name, String worldName) {
        String lowerName = name.toLowerCase();
        
        if (arenas.containsKey(lowerName)) {
            return null;
        }
        
        Arena arena = new Arena(name, worldName);
        arenas.put(lowerName, arena);
        
        // Save the arena to disk
        saveArena(arena);
        
        return arena;
    }
    
    /**
     * Delete an arena
     * 
     * @param name Arena name
     * @return true if deleted, false if not found
     */
    public boolean deleteArena(String name) {
        String lowerName = name.toLowerCase();
        
        if (!arenas.containsKey(lowerName)) {
            return false;
        }
        
        // Remove from memory
        arenas.remove(lowerName);
        
        // Delete file
        File arenaFile = new File(arenaDirectory, lowerName + ".json");
        return arenaFile.delete();
    }
    
    /**
     * Save an arena to disk
     * 
     * @param arena The arena to save
     */
    public void saveArena(Arena arena) {
        if (arena == null) return;
        
        File arenaFile = new File(arenaDirectory, arena.getName().toLowerCase() + ".json");
        
        try (FileWriter writer = new FileWriter(arenaFile)) {
            gson.toJson(arena, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arena " + arena.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Save all arenas to disk
     */
    public void saveAllArenas() {
        for (Arena arena : arenas.values()) {
            saveArena(arena);
        }
    }
    
    /**
     * Load all arenas from disk
     */
    private void loadArenas() {
        File[] arenaFiles = arenaDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (arenaFiles == null) return;
        
        for (File arenaFile : arenaFiles) {
            try (FileReader reader = new FileReader(arenaFile)) {
                Arena arena = gson.fromJson(reader, Arena.class);
                arenas.put(arena.getName().toLowerCase(), arena);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load arena from " + arenaFile.getName() + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas from disk.");
    }
    
    /**
     * Set a player to be editing an arena
     * 
     * @param player The player
     * @param arena The arena
     */
    public void setEditing(Player player, Arena arena) {
        editing.put(player, arena);
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aYou are now editing arena &e" + arena.getName());
    }
    
    /**
     * Get the arena a player is editing
     * 
     * @param player The player
     * @return The arena or null if not editing
     */
    public Arena getEditingArena(Player player) {
        return editing.get(player);
    }
    
    /**
     * Check if a player is editing an arena
     * 
     * @param player The player
     * @return true if editing, false otherwise
     */
    public boolean isEditing(Player player) {
        return editing.containsKey(player);
    }
    
    /**
     * Stop a player from editing an arena
     * 
     * @param player The player
     */
    public void stopEditing(Player player) {
        Arena arena = editing.remove(player);
        if (arena != null) {
            saveArena(arena);
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aYou are no longer editing &e" + arena.getName());
        }
    }
}
