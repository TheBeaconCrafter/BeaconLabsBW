package org.bcnlab.beaconLabsBW.command;

import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.arena.model.SerializableLocation;
import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.shop.ShopVillager;
import org.bcnlab.beaconLabsBW.shop.ShopVillagerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command handler for placing and managing shop villagers
 */
public class ShopVillagerCommand implements CommandExecutor, TabCompleter {

    private final BeaconLabsBW plugin;
    private static final List<String> VILLAGER_TYPES = Arrays.asList("itemshop", "teamupgrades");
    private static final List<String> SUB_COMMANDS = Arrays.asList("place", "remove", "edit", "list");

    /**
     * Create a new ShopVillagerCommand
     * 
     * @param plugin The plugin instance
     */
    public ShopVillagerCommand(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }
        
        if (!player.hasPermission("labsbw.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "place":
                handlePlaceCommand(player, args);
                break;
            
            case "remove":
                handleRemoveCommand(player);
                break;
                
            case "edit":
                handleEditCommand(player, args);
                break;
                
            case "list":
                handleListCommand(player);
                break;
                
            default:
                sendUsage(player);
                break;
        }
        
        return true;
    }

    private void handlePlaceCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /shopnpc place <itemshop|teamupgrades> [arenaName]");
            return;
        }
        
        ShopVillager.VillagerType type;
        switch(args[1].toLowerCase()) {
            case "itemshop":
                type = ShopVillager.VillagerType.ITEM_SHOP;
                break;
            
            case "teamupgrades":
                type = ShopVillager.VillagerType.TEAM_UPGRADES;
                break;
                
            default:
                player.sendMessage(ChatColor.RED + "Invalid villager type. Must be 'itemshop' or 'teamupgrades'.");
                return;
        }
        
        String arenaName = (args.length > 2) ? args[2] : null;
        
        // Check if arena exists if specified
        Arena arena = null;
        if (arenaName != null) {
            arena = plugin.getArenaManager().getArena(arenaName);
            if (arena == null) {
                player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found.");
                return;
            }
        }
        
        // Place the villager at the player's location
        ShopVillager spawnedVillager = plugin.getVillagerManager().spawnShopVillager(type, player.getLocation(), arenaName);
          // Save the villager data to the arena config if an arena was specified
        if (arena != null && spawnedVillager != null) {
            ShopVillagerData villagerData = new ShopVillagerData(type, new SerializableLocation(player.getLocation()));
            // Use a unique key, e.g., based on location hash or a UUID
            String key = type.name() + "_" + player.getLocation().hashCode(); 
            
            // Initialize shopVillagers map if it's null
            if (arena.getShopVillagers() == null) {
                arena.setShopVillagers(new HashMap<>());
            }
            
            arena.getShopVillagers().put(key, villagerData);
            player.sendMessage(ChatColor.GOLD + "Villager data added to arena '" + arenaName + "'. Use /bw save to make it permanent.");
        } else if (arenaName != null) {
            player.sendMessage(ChatColor.RED + "Failed to spawn villager or save data to arena.");
            return; // Prevent success message if saving failed
        }

        String message = ChatColor.GREEN + "Placed " + type.getDisplayName() + 
            (arenaName != null ? " in arena " + arenaName : " globally (not saved to arena)");
        player.sendMessage(message);
    }

    private void handleRemoveCommand(Player player) {
        // Look for nearby villagers (within 2 blocks)
        ShopVillager nearestVillager = null;
        double closestDistance = Double.MAX_VALUE;
        String villagerArenaName = null;
        
        // Need to determine which arena the nearest villager belongs to
        // This requires VillagerManager to track this or iterate through arenas
        // For simplicity, let's assume we can get this info (needs VillagerManager changes)
        // Find nearest villager and its arena
        nearestVillager = plugin.getVillagerManager().findNearestVillager(player.getLocation(), 2.0);
        
        if (nearestVillager == null) {
            player.sendMessage(ChatColor.RED + "No shop villagers found nearby.");
            return;
        }
        
        // Find which arena this villager belongs to (needs enhancement in VillagerManager/ShopVillager)
        villagerArenaName = plugin.getVillagerManager().getVillagerArenaName(nearestVillager);

        // Remove data from the corresponding arena if it belongs to one
        if (villagerArenaName != null) {
            Arena arena = plugin.getArenaManager().getArena(villagerArenaName);
            if (arena != null) {
                // Find the key to remove - requires a way to link ShopVillager back to its key in Arena map
                String keyToRemove = null;
                SerializableLocation villagerLoc = new SerializableLocation(nearestVillager.getLocation());
                for (Map.Entry<String, ShopVillagerData> entry : arena.getShopVillagers().entrySet()) {
                    if (entry.getValue().getType() == nearestVillager.getType() && entry.getValue().getLocation().equals(villagerLoc)) {
                        keyToRemove = entry.getKey();
                        break;
                    }
                }
                if (keyToRemove != null) {
                    arena.getShopVillagers().remove(keyToRemove);
                    player.sendMessage(ChatColor.GOLD + "Villager data removed from arena '" + villagerArenaName + "'. Use /bw save to make it permanent.");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Warning: Could not find villager data in arena '" + villagerArenaName + "' config.");
                }
            } else {
                 player.sendMessage(ChatColor.YELLOW + "Warning: Arena '" + villagerArenaName + "' not found for removing villager data.");
            }
        }

        // Remove the actual villager entity and from VillagerManager lists
        plugin.getVillagerManager().removeShopVillager(nearestVillager);
        player.sendMessage(ChatColor.GREEN + "Removed " + nearestVillager.getType().getDisplayName() + ".");
    }
    
    private void handleEditCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /shopnpc edit <true|false>");
            return;
        }
        
        boolean editMode;
        
        try {
            editMode = Boolean.parseBoolean(args[1]);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Invalid edit mode. Must be 'true' or 'false'.");
            return;
        }
        
        // Set edit mode for all villagers
        plugin.getVillagerManager().setEditMode(editMode);
        
        player.sendMessage(ChatColor.GREEN + "Shop NPCs edit mode is now " + 
            (editMode ? "enabled" : "disabled") + ".");
    }
    
    private void handleListCommand(Player player) {
        List<ShopVillager> villagers = plugin.getVillagerManager().getShopVillagers();
        
        if (villagers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No shop villagers have been placed.");
            return;
        }
        
        player.sendMessage(ChatColor.GREEN + "Shop NPCs (" + villagers.size() + "):");
        
        for (int i = 0; i < villagers.size(); i++) {
            ShopVillager villager = villagers.get(i);
            String location = String.format("%.1f, %.1f, %.1f", 
                villager.getLocation().getX(), 
                villager.getLocation().getY(), 
                villager.getLocation().getZ());
            
            player.sendMessage(ChatColor.GRAY + "#" + (i + 1) + ": " + 
                villager.getType().getDisplayName() + " - " + location);
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Shop NPC Management Commands:");
        player.sendMessage(ChatColor.GRAY + "/shopnpc place <itemshop|teamupgrades> [arenaName]" + 
            ChatColor.WHITE + " - Place a shop villager");
        player.sendMessage(ChatColor.GRAY + "/shopnpc remove" + 
            ChatColor.WHITE + " - Remove the nearest shop villager");
        player.sendMessage(ChatColor.GRAY + "/shopnpc edit <true|false>" + 
            ChatColor.WHITE + " - Enable/disable edit mode");
        player.sendMessage(ChatColor.GRAY + "/shopnpc list" + 
            ChatColor.WHITE + " - List all shop villagers");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Complete subcommands
            String partialCommand = args[0].toLowerCase();
            completions.addAll(SUB_COMMANDS.stream()
                .filter(cmd -> cmd.startsWith(partialCommand))
                .collect(Collectors.toList()));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String partial = args[1].toLowerCase();
            
            if (subCommand.equals("place")) {
                // Complete villager types
                completions.addAll(VILLAGER_TYPES.stream()
                    .filter(type -> type.startsWith(partial))
                    .collect(Collectors.toList()));
            } else if (subCommand.equals("edit")) {
                // Complete boolean values
                if ("true".startsWith(partial)) completions.add("true");
                if ("false".startsWith(partial)) completions.add("false");
            }        } else if (args.length == 3 && args[0].equalsIgnoreCase("place")) {
            // Complete with available arena names (if arenas are available)
            String partial = args[2].toLowerCase();
            completions.addAll(plugin.getArenaManager().getArenaNames().stream()
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList()));
        }
        
        return completions;
    }
}
