package org.bcnlab.beaconLabsBW.command;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.arena.model.GeneratorData;
import org.bcnlab.beaconLabsBW.arena.model.SerializableLocation;
import org.bcnlab.beaconLabsBW.arena.model.TeamData;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bcnlab.beaconLabsBW.generator.GeneratorType;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
            case "stop" -> handleStop(player, args);            case "shop" -> handleShop(player);
            case "upgrades" -> handleUpgrades(player);
            case "forceteam" -> {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThis command is now available as &e/forceteam");
                return true;
            }
            case "forcemap" -> {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThis command is now available as &e/forcemap");
                return true;
            }
            case "forcestart" -> {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThis command is now available as &e/forcestart");
                return true;
            }
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
            MessageUtils.sendMessage(player, "&e/bw save &7- Save arena changes");            MessageUtils.sendMessage(player, "&e/bw start <arena> &7- Force start a game");
            MessageUtils.sendMessage(player, "&e/bw stop <arena> &7- Force stop a game");            MessageUtils.sendMessage(player, "&e/forceteam [player] <team> &7- Force player onto team");
            MessageUtils.sendMessage(player, "&e/forcemap <arena> &7- Change map of waiting lobby");
            MessageUtils.sendMessage(player, "&e/forcestart &7- Reduce countdown to 3 seconds");
        }
        
        MessageUtils.sendMessage(player, "&e/bw join [arena] &7- Join a game");
        MessageUtils.sendMessage(player, "&e/bw leave &7- Leave current game");
        MessageUtils.sendMessage(player, "&e/bw shop &7- Open shop (during game)");
        MessageUtils.sendMessage(player, "&e/bw upgrades &7- Open team upgrades (during game)");
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
        }          if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw addgenerator <type> [team]");
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Types: IRON, GOLD, TEAM, EMERALD, DIAMOND");
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Note: TEAM generators replace individual IRON and GOLD generators for teams");
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
        } catch (IllegalArgumentException e) {            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cInvalid generator type. Use: IRON, GOLD, EMERALD, DIAMOND");
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
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&c═════════════════════════════════");
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cArena Configuration Incomplete");
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&c═════════════════════════════════");
            
            // Create a list of missing components
            List<String> missingComponents = new ArrayList<>();
            
            // Check teams
            if (arena.getTeams().isEmpty()) {
                missingComponents.add("&7- &cTeams: &eAdd teams with &6/bw addteam <name> <color>");
            }
            
            // Check spawns
            if (arena.getLobbySpawn() == null) {
                missingComponents.add("&7- &cLobby Spawn: &eSet with &6/bw setlobby");
            }
            
            if (arena.getSpectatorSpawn() == null) {
                missingComponents.add("&7- &cSpectator Spawn: &eSet with &6/bw setspectator");
            }
            
            // Check for incomplete team setups
            boolean hasIncompleteTeams = false;
            StringBuilder teamIssues = new StringBuilder();
            
            for (String teamName : arena.getTeams().keySet()) {
                TeamData team = arena.getTeam(teamName);
                
                if (team.getSpawnLocation() == null) {
                    teamIssues.append("\n&7  • &eTeam ").append(teamName).append(": &cMissing spawn &e(use &6/bw setspawn ").append(teamName).append("&e)");
                    hasIncompleteTeams = true;
                }
                
                if (team.getBedLocation() == null) {
                    teamIssues.append("\n&7  • &eTeam ").append(teamName).append(": &cMissing bed &e(use &6/bw setbed ").append(teamName).append("&e)");
                    hasIncompleteTeams = true;
                }
            }
            
            if (hasIncompleteTeams) {
                missingComponents.add("&7- &cIncomplete Team Setup:" + teamIssues.toString());
            }
            
            // Check generators
            boolean hasIron = false;
            boolean hasGold = false;
            boolean hasEmerald = false;
            boolean hasTeamGenerators = false;
            int teamGeneratorCount = 0;
            Set<String> teamsWithGenerators = new HashSet<>();
            
            for (GeneratorData generator : arena.getGenerators().values()) {
                switch (generator.getType()) {
                    case IRON -> hasIron = true;
                    case GOLD -> hasGold = true;
                    case TEAM -> {
                        // TEAM generators count as both iron and gold
                        hasIron = true;
                        hasGold = true;
                        hasTeamGenerators = true;
                        teamGeneratorCount++;
                        if (generator.getTeam() != null) {
                            teamsWithGenerators.add(generator.getTeam());
                        }
                    }
                    case EMERALD -> hasEmerald = true;
                    default -> {}
                }
            }
            
            StringBuilder generatorIssues = new StringBuilder();
            boolean hasGeneratorIssues = false;
            
            if (!hasIron && !hasTeamGenerators) {
                generatorIssues.append("\n&7  • &cMissing Iron generators &e(use &6/bw addgenerator IRON&e)");
                hasGeneratorIssues = true;
            }
            
            if (!hasGold && !hasTeamGenerators) {
                generatorIssues.append("\n&7  • &cMissing Gold generators &e(use &6/bw addgenerator GOLD&e)");
                hasGeneratorIssues = true;
            }
            
            if (!hasEmerald) {
                generatorIssues.append("\n&7  • &cMissing Emerald generators &e(use &6/bw addgenerator EMERALD&e)");
                hasGeneratorIssues = true;
            }
            
            // Check if all teams have generators if using TEAM generators
            if (hasTeamGenerators && !arena.getTeams().isEmpty()) {
                Set<String> teamsMissingGenerators = new HashSet<>(arena.getTeams().keySet());
                teamsMissingGenerators.removeAll(teamsWithGenerators);
                
                if (!teamsMissingGenerators.isEmpty()) {
                    generatorIssues.append("\n&7  • &cTeams missing generators:");
                    for (String teamName : teamsMissingGenerators) {
                        generatorIssues.append("\n&7    - &e").append(teamName).append(" &e(use &6/bw addgenerator TEAM ").append(teamName).append("&e)");
                    }
                    hasGeneratorIssues = true;
                }
            }
            
            if (hasGeneratorIssues) {
                missingComponents.add("&7- &cGenerator Issues:" + generatorIssues.toString());
            }
            
            // Display all missing components
            if (missingComponents.isEmpty()) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUnknown issue with configuration. Please check all settings.");
            } else {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&eMissing Components:");
                for (String component : missingComponents) {
                    MessageUtils.sendMessage(player, component);
                }
            }
        }
    }
      private void handleJoin(Player player, String[] args) {
        if (!player.hasPermission("bedwars.play")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to play BedWars.");
            return;
        }
        
        // Check if player is already in a game
        Game existingGame = plugin.getGameManager().getPlayerGame(player);
        if (existingGame != null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou are already in a game! Use /bw leave first.");
            return;
        }
        
        // Check if there's a waiting lobby already
        Game waitingLobby = plugin.getGameManager().getWaitingLobby();
        
        // If arena specified and no waiting lobby exists, try to join that specific arena
        if (args.length > 1 && waitingLobby == null && !plugin.getGameManager().haveRunningGames()) {
            String arenaName = args[1];
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            
            if (arena == null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cArena &e" + arenaName + " &cdoesn't exist.");
                return;
            }
            
            if (!arena.isConfigured()) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cArena &e" + arenaName + " &cis not fully configured.");
                return;
            }
            
            // Start new game with this arena
            Game game = plugin.getGameManager().startGame(arena);
            
            if (game == null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cFailed to start game. Arena might have issues.");
                return;
            }
            
            // Join the game
            if (plugin.getGameManager().addPlayerToGame(player, game)) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&aJoined game &e" + arena.getName());
            } else {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cCouldn't join game. It might be full or already started.");
            }
        } else if (waitingLobby != null || plugin.getGameManager().haveRunningGames()) {
            // There's already a game running or waiting, tell the player they can't specify an arena
            if (args.length > 1) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cCannot specify arena when a game is already active.");
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&7Use /bw join to join the current game.");
            }
            
            // Try to join the existing game
            if (plugin.getGameManager().joinGame(player)) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&aJoined a BedWars game!");
            } else {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cCouldn't join game. It might be full, already started, or you were not previously in it.");
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
        
        // Kick player from the server
        player.kickPlayer(ChatColor.translateAlternateColorCodes('&', 
            "&6&lBEDWARS\n&r\n&aYou have left the game\n&r\n&7Thanks for playing!"));
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
    
    private void handleUpgrades(Player player) {
        if (!player.hasPermission("bedwars.play")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to use team upgrades.");
            return;
        }
        
        // Check if player is in a game
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be in a game to use team upgrades.");
            return;
        }
        
        // Check if player is on a team
        String teamName = game.getPlayerTeam(player);
        if (teamName == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou must be on a team to use team upgrades.");
            return;
        }
        
        // Open team upgrades menu
        plugin.getTeamUpgradeManager().openUpgradesMenu(player, game);
    }
    
    /**
     * Handle the forceteam command
     * This command forces a player onto a specific team
     * 
     * @param player The player executing the command
     * @param args The command arguments
     */
    private void handleForceTeam(Player player, String[] args) {
        if (!player.hasPermission("bedwars.admin")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to force team assignments.");
            return;
        }
        
        // Check arguments
        if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw forceteam [player] <team>");
            return;
        }
        
        Player target;
        String teamName;
        
        if (args.length >= 3) {
            // Force another player
            target = Bukkit.getPlayerExact(args[1]);
            teamName = args[2];
            
            if (target == null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cPlayer &e" + args[1] + " &cis not online.");
                return;
            }
        } else {
            // Force command executor
            target = player;
            teamName = args[1];
        }
        
        // Check if target is in a game
        Game game = plugin.getGameManager().getPlayerGame(target);
        if (game == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThe target player is not in a game.");
            return;
        }
        
        // Check if the team exists in this game
        if (!game.getArena().getTeams().containsKey(teamName)) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cTeam &e" + teamName + " &cdoesn't exist in this game.");
            return;
        }
        
        // Only allow team changes before game starts
        if (game.getState() == GameState.RUNNING) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cCannot change teams after the game has started.");
            return;
        }
        
        // Remove from current team if any
        String currentTeam = game.getPlayerTeam(target);
        if (currentTeam != null) {
            game.getTeams().get(currentTeam).remove(target.getUniqueId());
        }
        
        // Add to new team
        game.getPlayerTeams().put(target.getUniqueId(), teamName);
        game.getTeams().computeIfAbsent(teamName, k -> ConcurrentHashMap.newKeySet()).add(target.getUniqueId());
        
        // Notify both players
        MessageUtils.sendMessage(target, plugin.getPrefix() + "&aYou have been assigned to team &e" + teamName);
        if (player != target) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aAssigned &e" + target.getName() + " &ato team &e" + teamName);
        }
    }
    
    /**
     * Handle the forcemap command
     * This command switches the waiting lobby to a different arena
     * 
     * @param player The player executing the command
     * @param args The command arguments
     */
    private void handleForceMap(Player player, String[] args) {
        if (!player.hasPermission("bedwars.admin")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to force map changes.");
            return;
        }
        
        // Check arguments
        if (args.length < 2) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cUsage: /bw forcemap <arena>");
            return;
        }
        
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cArena &e" + arenaName + " &cdoesn't exist.");
            return;
        }
        
        if (!arena.isConfigured()) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cArena &e" + arenaName + " &cis not fully configured.");
            return;
        }
        
        // Check for an active waiting lobby
        Game waitingLobby = plugin.getGameManager().getWaitingLobby();
        if (waitingLobby == null || waitingLobby.getState() == GameState.RUNNING) {
            // If there's no waiting lobby or the game is running, just set the next arena
            plugin.getGameManager().setNextArena(arena);
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aSet &e" + arena.getName() + " &aas the next arena.");
            return;
        }
        
        // If the waiting lobby is already on this arena, do nothing
        if (waitingLobby.getArena().getName().equalsIgnoreCase(arena.getName())) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThe current lobby is already using arena &e" + arena.getName());
            return;
        }
        
        // Switch arenas - this requires stopping the current game and starting a new one
        Set<UUID> currentPlayers = new HashSet<>(waitingLobby.getPlayers());
        
        // End the current game
        plugin.getGameManager().endGame(waitingLobby);
        
        // Start a new game with the specified arena
        Game newGame = plugin.getGameManager().startGame(arena);
        
        if (newGame == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cFailed to start new game with arena &e" + arena.getName());
            return;
        }
        
        // Transfer all players to the new game
        for (UUID playerId : currentPlayers) {
            Player gamePlayer = Bukkit.getPlayer(playerId);
            if (gamePlayer != null && gamePlayer.isOnline()) {
                plugin.getGameManager().addPlayerToGame(gamePlayer, newGame);
            }
        }
        
        // Broadcast map change
        for (UUID playerId : newGame.getPlayers()) {
            Player gamePlayer = Bukkit.getPlayer(playerId);
            if (gamePlayer != null) {
                MessageUtils.sendMessage(gamePlayer, plugin.getPrefix() + "&aThe map has been changed to &e" + arena.getName());
            }
        }
        
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aSuccessfully switched arena to &e" + arena.getName());
    }
    
    /**
     * Handle the forcestart command
     * This command reduces the countdown timer to 3 seconds
     * 
     * @param player The player executing the command
     */
    private void handleForceStart(Player player) {
        if (!player.hasPermission("bedwars.admin")) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou don't have permission to force start games.");
            return;
        }
        
        // Check for an active waiting lobby
        Game waitingLobby = plugin.getGameManager().getWaitingLobby();
        if (waitingLobby == null) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThere is no waiting lobby to force start.");
            return;
        }
        
        if (waitingLobby.getState() == GameState.RUNNING) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThe game has already started.");
            return;
        }
        
        if (waitingLobby.getPlayers().size() < plugin.getConfigManager().getMinPlayers()) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cNot enough players to start the game.");
            return;
        }
        
        // Force the game to start by setting the countdown to 3 seconds
        if (waitingLobby.getState() == GameState.STARTING) {
            waitingLobby.setCountdown(3);
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aForcing game to start in 3 seconds!");
            waitingLobby.broadcastMessage("&a&lGame starting in &c3 &aseconds! &7(Force started by admin)");
        } else if (waitingLobby.getState() == GameState.WAITING) {
            // Start the countdown
            waitingLobby.startCountdown();
            waitingLobby.setCountdown(3);
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aForcing game to start in 3 seconds!");
            waitingLobby.broadcastMessage("&a&lGame starting in &c3 &aseconds! &7(Force started by admin)");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String partial = args[0].toLowerCase();
              List<String> commands = new ArrayList<>();
            if (sender.hasPermission("bedwars.admin")) {                commands.addAll(Arrays.asList(
                    "help", "create", "delete", "list", "edit", "setspawn", "setbed", 
                    "setlobby", "setspectator", "addteam", "addgenerator", "removegenerator", 
                    "save", "start", "stop", "shop", "upgrades"
                ));
            } else {
                commands.addAll(Arrays.asList("help", "join", "leave", "shop", "upgrades"));
            }
            
            for (String cmd : commands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            // Second argument - depends on first argument
            String subCommand = args[0].toLowerCase();
            String partial = args[1].toLowerCase();            switch (subCommand) {
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
                }            case "addgenerator" -> {
                    // Generator types
                    for (String type : Arrays.asList("IRON", "GOLD", "EMERALD", "DIAMOND", "TEAM")) {
                        if (type.startsWith(partial.toUpperCase())) {
                            completions.add(type);
                        }
                    }
                }
            }        } else if (args.length == 3) {
            // Third argument - depends on first two arguments
            String subCommand = args[0].toLowerCase();
            String partial = args[2].toLowerCase();
            
            switch (subCommand) {                // Removed forceteam case as it's now a standalone command
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
