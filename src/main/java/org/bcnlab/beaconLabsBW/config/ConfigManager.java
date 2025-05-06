package org.bcnlab.beaconLabsBW.config;

import lombok.Getter;
import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Handles all configuration-related operations
 */
@Getter
public class ConfigManager {
    
    private final BeaconLabsBW plugin;
    private final FileConfiguration config;
    
    // Game settings
    private final boolean autoStartEnabled;
    private final int minPlayers;
    private final int lobbyCountdown;
    private final int gameTime;
    
    // Generator settings
    private final int ironInterval;
    private final int goldInterval;
    private final int emeraldInterval;
    
    // Team settings
    private final int maxTeamPlayers;
    
    public ConfigManager(BeaconLabsBW plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        
        // Load configuration values
        this.autoStartEnabled = config.getBoolean("settings.auto-start", true);
        this.minPlayers = config.getInt("settings.min-players", 2);
        this.lobbyCountdown = config.getInt("settings.lobby-countdown", 30);
        this.gameTime = config.getInt("settings.game-time", 1200);
        
        this.ironInterval = config.getInt("generators.iron.interval", 2);
        this.goldInterval = config.getInt("generators.gold.interval", 5);
        this.emeraldInterval = config.getInt("generators.emerald.interval", 15);
        
        this.maxTeamPlayers = config.getInt("teams.max-players", 4);
    }
    
    /**
     * Reload the configuration from disk
     */
    public void reloadConfig() {
        plugin.reloadConfig();
    }
}
