package org.bcnlab.beaconLabsBW;

import org.bcnlab.beaconLabsBW.command.LabsBedwarsCommand;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class BeaconLabsBW extends JavaPlugin {

    private String pluginPrefix;
    private String pluginVersion = "1.0";

    @Override
    public void onEnable() {
        createDefaultConfig();
        loadConfig();

        // Commands
        getCommand("labsbw").setExecutor(new LabsBedwarsCommand(this));

        getLogger().info(pluginPrefix + "BeaconLabsBW was enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(pluginPrefix + "BeaconLabsBW was disabled!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();

        pluginPrefix = config.getString("plugin-prefix", "&4BedWars &8» ");
    }

    private void createDefaultConfig() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        config.addDefault("plugin-prefix", "&4BedWars &8» ");
        saveConfig();
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', pluginPrefix);
    }

    public String getVersion() {
        return pluginVersion;
    }
}
