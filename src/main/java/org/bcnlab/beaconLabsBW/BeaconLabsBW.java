package org.bcnlab.beaconLabsBW;

import lombok.Getter;
import org.bcnlab.beaconLabsBW.arena.ArenaManager;
import org.bcnlab.beaconLabsBW.command.BedwarsCommandHandler;
import org.bcnlab.beaconLabsBW.command.ForceTeamCommand;
import org.bcnlab.beaconLabsBW.command.ForceMapCommand;
import org.bcnlab.beaconLabsBW.command.ForceStartCommand;
import org.bcnlab.beaconLabsBW.command.ModeCommand;
import org.bcnlab.beaconLabsBW.command.ShopVillagerCommand;
import org.bcnlab.beaconLabsBW.config.ConfigManager;
import org.bcnlab.beaconLabsBW.game.GameManager;
import org.bcnlab.beaconLabsBW.generator.GeneratorManager;
import org.bcnlab.beaconLabsBW.listeners.BlockListener;
import org.bcnlab.beaconLabsBW.listeners.EntityListener;
import org.bcnlab.beaconLabsBW.listeners.PlayerListener;
import org.bcnlab.beaconLabsBW.listeners.InventoryListener;
import org.bcnlab.beaconLabsBW.listeners.BlockPhysicsListener;
import org.bcnlab.beaconLabsBW.shop.ShopManager;
import org.bcnlab.beaconLabsBW.shop.TeamUpgradeManager;
import org.bcnlab.beaconLabsBW.shop.VillagerManager;
import org.bcnlab.beaconLabsBW.game.ultimates.UltimatesManager;
import org.bcnlab.beaconLabsBW.game.ultimates.UltimatesListener;
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
    private TeamUpgradeManager teamUpgradeManager;
    private UltimatesManager ultimatesManager;
    private VillagerManager villagerManager;
    
    // Command handlers
    private BedwarsCommandHandler commandHandler;
    private ForceTeamCommand forceTeamCommand;
    private ForceMapCommand forceMapCommand;
    private ForceStartCommand forceStartCommand;
    private ModeCommand modeCommand;
    private ShopVillagerCommand shopVillagerCommand;

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
        this.teamUpgradeManager = new TeamUpgradeManager(this);
        this.ultimatesManager = new UltimatesManager(this);
        this.villagerManager = new VillagerManager(this);
        this.gameManager = new GameManager(this);
        
        // Register commands
        this.commandHandler = new BedwarsCommandHandler(this);
        getCommand("labsbw").setExecutor(commandHandler);
        getCommand("labsbw").setTabCompleter(commandHandler);
        
        // Register standalone admin commands
        this.forceTeamCommand = new ForceTeamCommand(this);
        getCommand("forceteam").setExecutor(forceTeamCommand);
        getCommand("forceteam").setTabCompleter(forceTeamCommand);
        
        this.forceMapCommand = new ForceMapCommand(this);
        getCommand("forcemap").setExecutor(forceMapCommand);
        getCommand("forcemap").setTabCompleter(forceMapCommand);
        
        this.forceStartCommand = new ForceStartCommand(this);
        getCommand("forcestart").setExecutor(forceStartCommand);
        getCommand("forcestart").setTabCompleter(forceStartCommand);
        
        this.modeCommand = new ModeCommand(this);
        getCommand("mode").setExecutor(modeCommand);
        getCommand("mode").setTabCompleter(modeCommand);
        
        this.shopVillagerCommand = new ShopVillagerCommand(this);
        getCommand("shopnpc").setExecutor(shopVillagerCommand);
        getCommand("shopnpc").setTabCompleter(shopVillagerCommand);
        
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
        
        // Clean up ultimates resources
        if (ultimatesManager != null) {
            ultimatesManager.cleanup();
        }
        
        getLogger().info(pluginPrefix + "BeaconLabsBW was disabled!");
    }
    
    private PlayerListener playerListener;
    
    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        // Store the PlayerListener instance to access void check methods
        playerListener = new PlayerListener(this);
        pm.registerEvents(playerListener, this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new EntityListener(this), this);
        pm.registerEvents(new InventoryListener(this), this);
        pm.registerEvents(new UltimatesListener(this), this);
        pm.registerEvents(new BlockPhysicsListener(this), this); // Register the new block physics listener
        // The VillagerManager already registers itself as a listener in its constructor
        
        // Register and start our armor fix listener to handle armor durability issues
        org.bcnlab.beaconLabsBW.listeners.ArmorFixListener armorFixListener = new org.bcnlab.beaconLabsBW.listeners.ArmorFixListener(this);
        pm.registerEvents(armorFixListener, this);
        armorFixListener.startArmorFixTask();
        
        // Start void check task
        playerListener.startVoidCheck();
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
        config.addDefault("generators.diamond.interval", 30);
        
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
