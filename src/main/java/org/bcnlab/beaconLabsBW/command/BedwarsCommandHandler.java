package org.bcnlab.beaconLabsBW.command;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.arena.model.GeneratorData;
import org.bcnlab.beaconLabsBW.arena.model.SerializableLocation;
import org.bcnlab.beaconLabsBW.arena.model.TeamData;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.generator.GeneratorType;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles BedWars commands and tab completion
 */
public class BedwarsCommandHandler implements CommandExecutor, TabCompleter {
    
    private final BeaconLabsBW plugin;
    
    public BedwarsCommandHandler(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Most commands require a player
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage(sender, plugin.getPrefix() + "&cThis command must be executed by a player.");
            return true;
        }
        
        // No arguments shows help
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        // Process command based on first argument
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help" -> showHelp(player);
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "list" -> handleList(player);
            case "edit" -> handleEdit(player, args);
            case "setspawn" -> handleSetSpawn(player, args);
            case "setbed" -> handleSetBed(player, args);
            case "setlobby" -> handleSetLobby(player);
            case "setspectator" -> handleSetSpectator(player);
            case "addteam" -> handleAddTeam(player, args);
            case "addgenerator" -> handleAddGenerator(player, args);
            case "removegenerator" -> handleRemoveGenerator(player, args);
            case "save" -> handleSave(player);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "start" -> handleStart(player, args);
            case "stop" -> handleStop(player, args);
            case "shop" -> handleShop(player);
            default -> {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUnknown command. Use &e/bw help &cfor a list of commands.");
                return true;
            }
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        MessageUtils.sendMessage(player, "&8------- &4BeaconLabsBW &8-------");
        
        if (player.hasPermission("bedwars.admin")) {
            MessageUtils.sendMessage(player, "&e/bw create <name> &7- Create a new arena");
            MessageUtils.sendMessage(player, "&e/bw delete <name> &7- Delete an arena");
            MessageUtils.sendMessage(player, "&e/bw list &7- List available arenas");
            MessageUtils.sendMessage(player, "&e/bw edit <name> &7- Edit an arena");
            MessageUtils.sendMessage(player, "&e/bw setspawn <team> &7- Set team spawn");
            MessageUtils.sendMessage(player, "&e/bw setbed <team> &7- Set team bed");
            MessageUtils.sendMessage(player, "&e/bw setlobby &7- Set lobby spawn");
            MessageUtils.sendMessage(player, "&e/bw setspectator &7- Set spectator spawn");
            MessageUtils.sendMessage(player, "&e/bw addteam <name> <color> &7- Add a team");
            MessageUtils.sendMessage(player, "&e/bw addgenerator <type> [team] &7- Add a generator");
            MessageUtils.sendMessage(player, "&e/bw removegenerator &7- Remove nearest generator");
            MessageUtils.sendMessage(player, "&e/bw save &7- Save arena changes");
            MessageUtils.sendMessage(player, "&e/bw start <arena> &7- Force start a game");
            MessageUtils.sendMessage(player, "&e/bw stop <arena> &7- Force stop a game");
        }
        
        MessageUtils.sendMessage(player, "&e/bw join [arena] &7- Join a game");
        MessageUtils.sendMessage(player, "&e/bw leave &7- Leave current game");
        MessageUtils.sendMessage(player, "&e/bw shop &7- Open shop (during game)");
    }
    
    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("bedwars.create")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to create arenas.");
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw create <name>");
            return;
        }
        
        String arenaName = args[1];
        
        // Check if arena already exists
        if (plugin.getArenaManager().arenaExists(arenaName)) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cAn arena with that name already exists.");
            return;
        }
        
        // Create the arena
        Arena arena = plugin.getArenaManager().createArena(arenaName, player.getWorld().getName());
        
        if (arena != null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aArena &e" + arenaName + " &acreated successfully!");
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Use &e/bw edit " + arenaName + " &7to configure it.");
        } else {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cFailed to create arena.");
        }
    }
    
    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("bedwars.delete")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to delete arenas.");
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw delete <name>");
            return;
        }
        
        String arenaName = args[1];
        
        // Check if arena exists
        if (!plugin.getArenaManager().arenaExists(arenaName)) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cArena not found.");
            return;
        }
        
        // Check if arena is in use
        Game game = plugin.getGameManager().getActiveGames().get(arenaName.toLowerCase());
        if (game != null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThis arena is currently in use. Stop the game first.");
            return;
        }
        
        // Delete the arena
        boolean deleted = plugin.getArenaManager().deleteArena(arenaName);
        
        if (deleted) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aArena &e" + arenaName + " &adeleted successfully!");
        } else {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cFailed to delete arena.");
        }
    }
    
    private void handleList(Player player) {
        if (!player.hasPermission("bedwars.admin")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to list arenas.");
            return;
        }
        
        // Get all arena names
        java.util.Set<String> arenaNames = plugin.getArenaManager().getArenaNames();
        
        if (arenaNames.isEmpty()) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cNo arenas have been created yet.");
            return;
        }
        
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&eAvailable arenas:");
        
        for (String name : arenaNames) {
            Arena arena = plugin.getArenaManager().getArena(name);
            if (arena == null) continue;
            
            // Show arena status
            String status;
            if (plugin.getGameManager().getActiveGames().containsKey(name.toLowerCase())) {
                status = "&aActive";
            } else if (!arena.isConfigured()) {
                status = "&cNot Configured";
            } else {
                status = "&eAvailable";
            }
            
            MessageUtils.sendMessage(player, "&7- &f" + name + " &8[" + status + "&8]");
        }
    }
    
    private void handleEdit(Player player, String[] args) {
        if (!player.hasPermission("bedwars.edit")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to edit arenas.");
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw edit <name>");
            return;
        }
        
        String arenaName = args[1];
        
        // Check if arena exists
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cArena not found.");
            return;
        }
        
        // Check if arena is in use
        Game game = plugin.getGameManager().getActiveGames().get(arenaName.toLowerCase());
        if (game != null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThis arena is currently in use. Stop the game first.");
            return;
        }
        
        // Set player to edit mode
        plugin.getArenaManager().setEditing(player, arena);
        
        // Show edit instructions
        MessageUtils.sendMessage(player, "&8------- &4BedWars Editor &8-------");
        MessageUtils.sendMessage(player, "&7You are now editing &e" + arena.getName());
        MessageUtils.sendMessage(player, "&7Use these commands to configure the arena:");
        MessageUtils.sendMessage(player, "&e/bw setspawn <team> &7- Set team spawn");
        MessageUtils.sendMessage(player, "&e/bw setbed <team> &7- Set team bed");
        MessageUtils.sendMessage(player, "&e/bw setlobby &7- Set lobby spawn");
        MessageUtils.sendMessage(player, "&e/bw setspectator &7- Set spectator spawn");
        MessageUtils.sendMessage(player, "&e/bw addteam <name> <color> &7- Add a team");
        MessageUtils.sendMessage(player, "&e/bw addgenerator <type> [team] &7- Add a generator");
        MessageUtils.sendMessage(player, "&e/bw removegenerator &7- Remove nearest generator");
        MessageUtils.sendMessage(player, "&e/bw save &7- Save changes");
    }
    
    private void handleSetSpawn(Player player, String[] args) {
        if (!player.hasPermission("bedwars.edit")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to edit arenas.");
            return;
        }
        
        // Check if player is in edit mode
        Arena arena = plugin.getArenaManager().getEditingArena(player);
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in edit mode. Use /bw edit <arena>");
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw setspawn <team>");
            return;
        }
        
        String teamName = args[1];
        
        // Check if team exists
        TeamData team = arena.getTeam(teamName);
        if (team == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cTeam &e" + teamName + " &cdoes not exist. Create it first with /bw addteam");
            return;
        }
        
        // Set team spawn location
        team.setSpawnLocation(new SerializableLocation(player.getLocation()));
        
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aSet spawn for team &e" + teamName);
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Don't forget to use &e/bw save &7when done editing!");
    }
    
    private void handleSetBed(Player player, String[] args) {
        if (!player.hasPermission("bedwars.edit")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to edit arenas.");
            return;
        }
        
        // Check if player is in edit mode
        Arena arena = plugin.getArenaManager().getEditingArena(player);
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in edit mode. Use /bw edit <arena>");
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw setbed <team>");
            return;
        }
        
        String teamName = args[1];
        
        // Check if team exists
        TeamData team = arena.getTeam(teamName);
        if (team == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cTeam &e" + teamName + " &cdoes not exist. Create it first with /bw addteam");
            return;
        }
        
        // Set team bed location
        team.setBedLocation(new SerializableLocation(player.getLocation()));
        
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aSet bed for team &e" + teamName);
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Don't forget to use &e/bw save &7when done editing!");
    }
    
    private void handleSetLobby(Player player) {
        if (!player.hasPermission("bedwars.edit")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to edit arenas.");
            return;
        }
        
        // Check if player is in edit mode
        Arena arena = plugin.getArenaManager().getEditingArena(player);
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in edit mode. Use /bw edit <arena>");
            return;
        }
        
        // Set lobby location
        arena.setLobbySpawn(new SerializableLocation(player.getLocation()));
        
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aSet lobby spawn for arena &e" + arena.getName());
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Don't forget to use &e/bw save &7when done editing!");
    }
    
    private void handleSetSpectator(Player player) {
        if (!player.hasPermission("bedwars.edit")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to edit arenas.");
            return;
        }
        
        // Check if player is in edit mode
        Arena arena = plugin.getArenaManager().getEditingArena(player);
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in edit mode. Use /bw edit <arena>");
            return;
        }
        
        // Set spectator location
        arena.setSpectatorSpawn(new SerializableLocation(player.getLocation()));
        
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aSet spectator spawn for arena &e" + arena.getName());
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Don't forget to use &e/bw save &7when done editing!");
    }
    
    private void handleAddTeam(Player player, String[] args) {
        if (!player.hasPermission("bedwars.edit")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to edit arenas.");
            return;
        }
        
        // Check if player is in edit mode
        Arena arena = plugin.getArenaManager().getEditingArena(player);
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in edit mode. Use /bw edit <arena>");
            return;
        }
        
        if (args.length < 3) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw addteam <name> <color>");
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Colors: RED, BLUE, GREEN, YELLOW, AQUA, WHITE, PINK, GRAY");
            return;
        }
        
        String teamName = args[1];
        String teamColor = args[2].toUpperCase();
        
        // Validate color
        List<String> validColors = Arrays.asList("RED", "BLUE", "GREEN", "YELLOW", "AQUA", "WHITE", "PINK", "GRAY");
        if (!validColors.contains(teamColor)) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cInvalid color. Use: RED, BLUE, GREEN, YELLOW, AQUA, WHITE, PINK, GRAY");
            return;
        }
        
        // Check if team already exists
        if (arena.getTeam(teamName) != null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cA team with that name already exists.");
            return;
        }
        
        // Create team
        TeamData team = new TeamData(teamName, teamColor);
        arena.addTeam(teamName, team);
        
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aAdded team &e" + teamName + " &awith color &e" + teamColor);
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Now set its spawn with &e/bw setspawn " + teamName);
    }
    
    private void handleAddGenerator(Player player, String[] args) {
        if (!player.hasPermission("bedwars.edit")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to edit arenas.");
            return;
        }
        
        // Check if player is in edit mode
        Arena arena = plugin.getArenaManager().getEditingArena(player);
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in edit mode. Use /bw edit <arena>");
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw addgenerator <type> [team]");
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Types: IRON, GOLD, EMERALD");
            return;
        }
        
        String typeStr = args[1].toUpperCase();
        String team = args.length > 2 ? args[2] : null;
        
        // Validate team if provided
        if (team != null && arena.getTeam(team) == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cTeam &e" + team + " &cdoes not exist.");
            return;
        }
        
        // Validate generator type
        GeneratorType type;
        try {
            type = GeneratorType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cInvalid generator type. Use: IRON, GOLD, EMERALD");
            return;
        }
        
        // Create generator
        Location location = player.getLocation();
        SerializableLocation serializableLocation = new SerializableLocation(location);
        GeneratorData generator = plugin.getGeneratorManager().createGenerator(arena, type, serializableLocation, team);
        
        if (generator != null) {
            String teamMsg = team == null ? "shared" : "team " + team;
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aAdded &e" + type + " &agenerator for &e" + teamMsg);
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Don't forget to use &e/bw save &7when done editing!");
        } else {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cFailed to add generator.");
        }
    }
      private void handleRemoveGenerator(Player player, String[] args) {
        if (!player.hasPermission("bedwars.edit")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to edit arenas.");
            return;
        }
        
        // Check if player is in edit mode
        Arena arena = plugin.getArenaManager().getEditingArena(player);
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in edit mode. Use /bw edit <arena>");
            return;
        }
        
        // Find the nearest generator
        Location playerLocation = player.getLocation();
        GeneratorData nearestGenerator = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (GeneratorData generator : arena.getGenerators().values()) {
            SerializableLocation genLocation = generator.getLocation();
            if (genLocation == null) continue;
            
            Location location = genLocation.toBukkitLocation();
            if (location == null || !location.getWorld().equals(playerLocation.getWorld())) continue;
            
            double distance = location.distanceSquared(playerLocation);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestGenerator = generator;
            }
        }
        
        if (nearestGenerator != null && nearestDistance <= 25) { // Within 5 blocks
            // Remove the generator
            plugin.getGeneratorManager().removeGenerator(arena, nearestGenerator.getId());
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aRemoved &e" + nearestGenerator.getType() + " &agenerator.");
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Don't forget to use &e/bw save &7when done editing!");
        } else {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cNo generators found nearby (within 5 blocks).");
        }
    }
    
    private void handleSave(Player player) {
        if (!player.hasPermission("bedwars.edit")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to edit arenas.");
            return;
        }
        
        // Check if player is in edit mode
        Arena arena = plugin.getArenaManager().getEditingArena(player);
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in edit mode. Use /bw edit <arena>");
            return;
        }
        
        // Save arena
        plugin.getArenaManager().saveArena(arena);
        
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aSaved arena &e" + arena.getName());
        
        // Check if arena is fully configured
        if (arena.isConfigured()) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aArena is fully configured and ready to play!");
        } else {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cArena is not fully configured yet.");
            
            // Check what's missing
            if (arena.getTeams().isEmpty()) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&7- Add teams with &e/bw addteam <name> <color>");
            }
            
            if (arena.getLobbySpawn() == null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&7- Set lobby spawn with &e/bw setlobby");
            }
            
            if (arena.getSpectatorSpawn() == null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&7- Set spectator spawn with &e/bw setspectator");
            }
            
            // Check for incomplete team setups
            for (String teamName : arena.getTeams().keySet()) {
                TeamData team = arena.getTeam(teamName);
                
                if (team.getSpawnLocation() == null) {
                    MessageUtils.sendMessage(player, plugin.getPrefix() + "&7- Set spawn for team &e" + teamName + " &7with &e/bw setspawn " + teamName);
                }
                
                if (team.getBedLocation() == null) {
                    MessageUtils.sendMessage(player, plugin.getPrefix() + "&7- Set bed for team &e" + teamName + " &7with &e/bw setbed " + teamName);
                }
            }
            
            // Check generators
            boolean hasIron = false;
            boolean hasGold = false;
            boolean hasEmerald = false;
            
            for (GeneratorData generator : arena.getGenerators().values()) {
                switch (generator.getType()) {
                    case IRON -> hasIron = true;
                    case GOLD -> hasGold = true;
                    case EMERALD -> hasEmerald = true;
                }
            }
            
            if (!hasIron) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&7- Add an iron generator with &e/bw addgenerator IRON");
            }
            
            if (!hasGold) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&7- Add a gold generator with &e/bw addgenerator GOLD");
            }
            
            if (!hasEmerald) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&7- Add an emerald generator with &e/bw addgenerator EMERALD");
            }
        }
    }
    
    private void handleJoin(Player player, String[] args) {
        if (!player.hasPermission("bedwars.play")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to play BedWars.");
            return;
        }
        
        // Check if player is already in a game
        if (plugin.getGameManager().getPlayerGame(player) != null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou are already in a game! Use /bw leave first.");
            return;
        }
        
        // If arena specified, try to join that one
        if (args.length > 1) {
            String arenaName = args[1];
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            
            if (arena == null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cArena &e" + arenaName + " &cdoesn't exist.");
                return;
            }
            
            // Check if game is running for this arena
            Game game = plugin.getGameManager().getActiveGames().get(arenaName.toLowerCase());
            
            if (game == null) {
                // Start new game
                game = plugin.getGameManager().startGame(arena);
                
                if (game == null) {
                    MessageUtils.sendMessage(player, plugin.getPrefix() + "&cFailed to start game. Arena might not be fully configured.");
                    return;
                }
            }
            
            // Join the game
            if (plugin.getGameManager().addPlayerToGame(player, game)) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&aJoined game &e" + arena.getName());
            } else {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cCouldn't join game. It might be full or already started.");
            }
        } else {
            // Find a suitable game
            if (plugin.getGameManager().joinGame(player)) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&aJoined a BedWars game!");
            } else {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cNo games available to join.");
            }
        }
    }
    
    private void handleLeave(Player player) {
        if (!player.hasPermission("bedwars.play")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to play BedWars.");
            return;
        }
        
        // Check if player is in a game
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou are not in a game.");
            return;
        }
        
        // Leave the game
        plugin.getGameManager().removePlayerFromGame(player);
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aYou have left the game.");
    }
    
    private void handleStart(Player player, String[] args) {
        if (!player.hasPermission("bedwars.admin")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to start games.");
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw start <arena>");
            return;
        }
        
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cArena &e" + arenaName + " &cdoesn't exist.");
            return;
        }
        
        // Check if game is already running
        Game existingGame = plugin.getGameManager().getActiveGames().get(arenaName.toLowerCase());
        if (existingGame != null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cGame is already running for this arena.");
            return;
        }
        
        // Start new game
        Game game = plugin.getGameManager().startGame(arena);
        
        if (game != null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aStarted game for arena &e" + arena.getName());
        } else {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cFailed to start game. Arena might not be fully configured.");
        }
    }
    
    private void handleStop(Player player, String[] args) {
        if (!player.hasPermission("bedwars.admin")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to stop games.");
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw stop <arena>");
            return;
        }
        
        String arenaName = args[1];
        Game game = plugin.getGameManager().getActiveGames().get(arenaName.toLowerCase());
        
        if (game == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cNo active game found for arena &e" + arenaName);
            return;
        }
        
        // End the game
        game.endGame(null);
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aGame for arena &e" + arenaName + " &astopped.");
    }
    
    private void handleShop(Player player) {
        if (!player.hasPermission("bedwars.play")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to use the shop.");
            return;
        }
        
        // Check if player is in a game
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in a game to use the shop.");
            return;
        }
        
        // Open shop
        plugin.getShopManager().openShop(player);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String partial = args[0].toLowerCase();
            
            List<String> commands = new ArrayList<>();
            if (sender.hasPermission("bedwars.admin")) {
                commands.addAll(Arrays.asList(
                    "help", "create", "delete", "list", "edit", "setspawn", "setbed", 
                    "setlobby", "setspectator", "addteam", "addgenerator", "removegenerator", 
                    "save", "start", "stop", "shop"
                ));
            } else {
                commands.addAll(Arrays.asList("help", "join", "leave", "shop"));
            }
            
            for (String cmd : commands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            // Second argument - depends on first argument
            String subCommand = args[0].toLowerCase();
            String partial = args[1].toLowerCase();
            
            switch (subCommand) {
                case "edit", "delete", "join", "start", "stop" -> {
                    // Arena names
                    for (String arena : plugin.getArenaManager().getArenaNames()) {
                        if (arena.toLowerCase().startsWith(partial)) {
                            completions.add(arena);
                        }
                    }
                }
                case "setspawn", "setbed" -> {
                    // Team names if in edit mode
                    if (sender instanceof Player player) {
                        Arena arena = plugin.getArenaManager().getEditingArena(player);
                        if (arena != null) {
                            for (String team : arena.getTeams().keySet()) {
                                if (team.toLowerCase().startsWith(partial)) {
                                    completions.add(team);
                                }
                            }
                        }
                    }
                }
                case "addgenerator" -> {
                    // Generator types
                    for (String type : Arrays.asList("IRON", "GOLD", "EMERALD")) {
                        if (type.startsWith(partial.toUpperCase())) {
                            completions.add(type);
                        }
                    }
                }
            }
        } else if (args.length == 3) {
            // Third argument - depends on first two arguments
            String subCommand = args[0].toLowerCase();
            String partial = args[2].toLowerCase();
            
            switch (subCommand) {
                case "addteam" -> {
                    // Team colors
                    for (String color : Arrays.asList("RED", "BLUE", "GREEN", "YELLOW", "AQUA", "WHITE", "PINK", "GRAY")) {
                        if (color.startsWith(partial.toUpperCase())) {
                            completions.add(color);
                        }
                    }
                }
                case "addgenerator" -> {
                    // Team names if in edit mode
                    if (sender instanceof Player player) {
                        Arena arena = plugin.getArenaManager().getEditingArena(player);
                        if (arena != null) {
                            for (String team : arena.getTeams().keySet()) {
                                if (team.toLowerCase().startsWith(partial)) {
                                    completions.add(team);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return completions;
    }
}
