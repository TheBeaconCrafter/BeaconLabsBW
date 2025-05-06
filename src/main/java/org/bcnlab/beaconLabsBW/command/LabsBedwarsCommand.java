package org.bcnlab.beaconLabsBW.command;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LabsBedwarsCommand implements CommandExecutor {
    private final BeaconLabsBW plugin;

    public LabsBedwarsCommand(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "BeaconLabsBW Version " + ChatColor.GOLD + plugin.getVersion() + ChatColor.RED + " by ItsBeacon");
        return true;
    }
}
