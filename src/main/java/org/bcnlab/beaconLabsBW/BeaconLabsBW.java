package org.bcnlab.beaconLabsBW;

import lombok.Getter;
import org.bcnlab.beaconLabsBW.arena.ArenaManager;
import org.bcnlab.beaconLabsBW.command.BedwarsCommandHandler;
import org.bcnlab.beaconLabsBW.config.ConfigManager;
import org.bcnlab.beaconLabsBW.game.GameManager;
import org.bcnlab.beaconLabsBW.generator.GeneratorManager;
import org.bcnlab.beaconLabsBW.listeners.BlockListener;
import org.bcnlab.beaconLabsBW.listeners.EntityListener;
import org.bcnlab.beaconLabsBW.listeners.PlayerListener;
import org.bcnlab.beaconLabsBW.shop.ShopManager;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for the BeaconLabsBW plugin
 * A full-featured BedWars implementation for Spigot 1.21.4
 * 
 * Features:
 * - Multiple arenas with in-game edit mode
 * - Team-based gameplay with colored teams
 * - Resource generators (iron, gold, emerald)
 * - Shop system for purchasing items
 * - Automatic game start and management
 * - Spectator mode after death
 * - World protection and restoration
 */
@Getter
public final class BeaconLabsBW extends JavaPlugin {

    private String pluginPrefix;
    private String pluginVersion = "1.0";
    
    // Managers
    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private GeneratorManager generatorManager;
    private ShopManager shopManager;
    
    // Command handler
    private BedwarsCommandHandler commandHandler;

    @Override
    public void onEnable() {
        // Initialize configuration
        createDefaultConfig();
        loadConfig();
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.arenaManager = new ArenaManager(this);
        this.generatorManager = new GeneratorManager(this);
        this.shopManager = new ShopManager(this);
        this.gameManager = new GameManager(this);
        
        // Register commands
        this.commandHandler = new BedwarsCommandHandler(this);
        getCommand("labsbw").setExecutor(commandHandler);
        getCommand("labsbw").setTabCompleter(commandHandler);
        
        // Register event listeners
        registerListeners();
        
        // Attempt to auto-start if configured
        if (configManager.isAutoStartEnabled()) {
            gameManager.attemptAutoStart();
        }

        getLogger().info(pluginPrefix + "BeaconLabsBW was enabled!");
    }

    @Override
    public void onDisable() {
        // Clean up any running games
        if (gameManager != null) {
            gameManager.shutdownAllGames();
        }
        
        // Save any pending arena changes
        if (arenaManager != null) {
            arenaManager.saveAllArenas();
        }
        
        getLogger().info(pluginPrefix + "BeaconLabsBW was disabled!");
    }
    
    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new EntityListener(this), this);
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        pluginPrefix = config.getString("plugin-prefix", "&4BedWars &8» ");
    }

    private void createDefaultConfig() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        
        // Plugin basics
        config.addDefault("plugin-prefix", "&4BedWars &8» ");
        
        // Game settings
        config.addDefault("settings.auto-start", true);
        config.addDefault("settings.min-players", 2);
        config.addDefault("settings.lobby-countdown", 30);
        config.addDefault("settings.game-time", 1200); // 20 minutes
        
        // Generator settings
        config.addDefault("generators.iron.interval", 2);
        config.addDefault("generators.gold.interval", 5);
        config.addDefault("generators.emerald.interval", 15);
        
        // Team settings
        config.addDefault("teams.max-players", 4);
        
        saveConfig();
    }

    public String getPrefix() {
        return MessageUtils.colorize(pluginPrefix);
    }

    public String getVersion() {
        return pluginVersion;
    }
}
