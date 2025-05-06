package org.bcnlab.beaconLabsBW.game;

import lombok.Getter;
import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.arena.model.GeneratorData;
import org.bcnlab.beaconLabsBW.arena.model.SerializableLocation;
import org.bcnlab.beaconLabsBW.arena.model.TeamData;
import org.bcnlab.beaconLabsBW.generator.ActiveGenerator;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents a running BedWars game
 */
@Getter
public class Game {
      private final BeaconLabsBW plugin;
    private final Arena arena;
    private final String gameId; // Unique identifier for this game instance
    
    private GameState state = GameState.WAITING;
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> teams = new ConcurrentHashMap<>();
    private final Set<UUID> spectators = ConcurrentHashMap.newKeySet();
    
    // Game state tracking
    private final Map<String, Boolean> bedStatus = new ConcurrentHashMap<>();
    private final List<Block> placedBlocks = new ArrayList<>();
    private final List<ActiveGenerator> activeGenerators = new ArrayList<>();
    
    // Tasks
    private BukkitTask countdownTask;
    private BukkitTask gameTimerTask;
    private int countdown;
    private int gameTimer;
      // Game statistics tracking
    private final Map<UUID, Integer> playerKills = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerBedBreaks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerDeaths = new ConcurrentHashMap<>();
    
    // Scoreboard manager
    private org.bcnlab.beaconLabsBW.utils.GameScoreboard scoreboardManager;
      /**
     * Creates a new BedWars game
     *
     * @param plugin The plugin instance
     * @param arena The arena to use
     */
    public Game(BeaconLabsBW plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.countdown = plugin.getConfigManager().getLobbyCountdown();
        this.gameTimer = plugin.getConfigManager().getGameTime();
        this.gameId = "game_" + System.currentTimeMillis() + "_" + arena.getName();
    }
    
    /**
     * Get the game's unique ID
     * @return The game ID
     */
    public String getGameId() {
        return gameId;
    }
      /**
     * Set up the game arena
     */    public void setup() {
        // Initialize team data
        for (String teamName : arena.getTeams().keySet()) {
            teams.put(teamName, ConcurrentHashMap.newKeySet());
            bedStatus.put(teamName, true); // All beds start intact
        }
        
        // Set game state to waiting
        state = GameState.WAITING;
        
        // Initialize scoreboard manager
        scoreboardManager = new org.bcnlab.beaconLabsBW.utils.GameScoreboard(plugin, this);
    }
    
    /**
     * Start all resource generators for the game
     */
    private void startGenerators() {
        // Clean up any existing generators first
        stopGenerators();
        
        for (GeneratorData genData : arena.getGenerators().values()) {
            Location location = genData.getLocation().toBukkitLocation();
            if (location != null) {
                ActiveGenerator generator = new ActiveGenerator(plugin, genData, this);
                generator.start();
                activeGenerators.add(generator);
            }
        }
    }
    
    /**
     * Stop all resource generators
     */
    private void stopGenerators() {
        for (ActiveGenerator generator : activeGenerators) {
            generator.stop();
        }
        activeGenerators.clear();
    }
    
    /**
     * Add a player to the game
     *
     * @param player The player to add
     * @return true if added, false otherwise
     */
    public boolean addPlayer(Player player) {
        if (state != GameState.WAITING && state != GameState.STARTING) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThis game has already started!");
            return false;
        }
        
        if (players.size() >= arena.getMaxPlayers()) {
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cThis game is full!");
            return false;
        }
        
        // Add to player set
        players.add(player.getUniqueId());
        
        // Teleport to lobby
        teleportToLobby(player);
          // Reset player
        resetPlayer(player);
        
        // Set up player scoreboard
        scoreboardManager.setupScoreboard(player);
        
        // Announce join
        broadcastMessage("&e" + player.getName() + " &7joined the game! &8(" + players.size() + "/" + arena.getMaxPlayers() + ")");
        
        // Start countdown if enough players
        if (players.size() >= plugin.getConfigManager().getMinPlayers() && state == GameState.WAITING) {
            startCountdown();
        }
        
        return true;
    }
    
    /**
     * Remove a player from the game
     *
     * @param player The player to remove
     */
    public void removePlayer(Player player) {
        if (player == null) return;
        
        UUID playerId = player.getUniqueId();
        
        // Check if they're a spectator
        boolean wasSpectator = spectators.contains(playerId);
        spectators.remove(playerId);
        
        // Check if they're a player
        if (!players.contains(playerId)) {
            return;
        }
        
        players.remove(playerId);
        
        // Remove from team
        String team = playerTeams.remove(playerId);
        if (team != null) {
            teams.getOrDefault(team, Collections.emptySet()).remove(playerId);
        }
        
        // Announce leave if not a spectator
        if (!wasSpectator) {
            broadcastMessage("&e" + player.getName() + " &7left the game! &8(" + players.size() + "/" + arena.getMaxPlayers() + ")");
        }
        
        // Reset player
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        resetPlayer(player);
        
        // Check for game end
        if (state == GameState.RUNNING) {
            checkGameEnd();
        } else if (state == GameState.STARTING && players.size() < plugin.getConfigManager().getMinPlayers()) {
            cancelCountdown();
        }
    }
    
    /**
     * Start the countdown to begin the game
     */
    private void startCountdown() {
        if (state != GameState.WAITING) return;
        
        state = GameState.STARTING;
        countdown = plugin.getConfigManager().getLobbyCountdown();
        
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdown <= 0) {
                    // Start the game
                    startGame();
                    cancel();
                    return;
                }
                
                if (countdown <= 5 || countdown % 10 == 0) {
                    broadcastMessage("&eGame starting in &c" + countdown + " &eseconds!");
                    for (UUID playerId : players) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }
                
                countdown--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    /**
     * Cancel the countdown
     */
    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        
        state = GameState.WAITING;
        broadcastMessage("&cNot enough players! Countdown cancelled.");
    }    /**
     * Start the game
     */
    private void startGame() {
        if (state != GameState.STARTING) return;
        
        state = GameState.RUNNING;
        
        // First clean up any leftover items and entities
        clearArenaItems();
          
        // Clean up any Dream Defenders from previous games
        cleanupDreamDefenders();
        
        // Place team beds for the game
        placeTeamBeds();
          
        // Assign players to teams
        assignTeams();
        
        // Reset team upgrades
        plugin.getTeamUpgradeManager().resetUpgrades();
        
        // Teleport players to their team spawns
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                String team = playerTeams.get(playerId);
                if (team != null) {
                    TeamData teamData = arena.getTeam(team);
                    if (teamData != null) {
                        // Teleport to team spawn
                        SerializableLocation spawnLoc = teamData.getSpawnLocation();
                        if (spawnLoc != null) {
                            player.teleport(spawnLoc.toBukkitLocation());
                        }
                        
                        // Give team armor
                        giveTeamArmor(player, teamData);
                        
                        // Initial equipment
                        giveInitialEquipment(player);
                    }
                }
            }
        }
        
        // Start resource generators
        startGenerators();
        
        // Update tab list with team colors
        scoreboardManager.updateTabList();
        
        // Start scoreboard update task
        scoreboardManager.startTask();
        
        // Announce game start
        broadcastMessage("&a&lGAME STARTED! &eProtect your bed and destroy other beds!");
        
        // Start game timer
        startGameTimer();
    }
    
    /**
     * Start the game timer
     */
    private void startGameTimer() {
        gameTimer = plugin.getConfigManager().getGameTime();
        
        gameTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameTimer <= 0) {
                    // End the game due to time limit
                    endGame(null); // No winner, it's a draw
                    cancel();
                    return;
                }
                
                // Periodic events
                if (gameTimer % 60 == 0) {
                    int minutes = gameTimer / 60;
                    broadcastMessage("&eGame ends in &c" + minutes + " &eminutes!");
                }
                
                gameTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    /**
     * Assign players to teams
     */
    private void assignTeams() {
        List<UUID> unassignedPlayers = new ArrayList<>(players);
        List<String> availableTeams = new ArrayList<>(arena.getTeams().keySet());
        Collections.shuffle(availableTeams);
        
        int maxPerTeam = plugin.getConfigManager().getMaxTeamPlayers();
        
        // First, assign players who already chose a team (for future team selection feature)
        Iterator<UUID> it = unassignedPlayers.iterator();
        while (it.hasNext()) {
            UUID playerId = it.next();
            String team = playerTeams.get(playerId);
            
            if (team != null && availableTeams.contains(team)) {
                if (teams.get(team).size() < maxPerTeam) {
                    // Keep the player in their chosen team
                    it.remove();
                } else {
                    // Team is full, remove the preference
                    playerTeams.remove(playerId);
                }
            }
        }
        
        // Then, assign remaining players evenly
        int teamIndex = 0;
        for (UUID playerId : unassignedPlayers) {
            if (teamIndex >= availableTeams.size()) {
                teamIndex = 0;
            }
            
            String team = availableTeams.get(teamIndex);
            
            // Check if team is full
            if (teams.get(team).size() >= maxPerTeam) {
                // Find next non-full team
                boolean foundTeam = false;
                for (int i = 0; i < availableTeams.size(); i++) {
                    teamIndex = (teamIndex + 1) % availableTeams.size();
                    team = availableTeams.get(teamIndex);
                    if (teams.get(team).size() < maxPerTeam) {
                        foundTeam = true;
                        break;
                    }
                }
                
                if (!foundTeam) {
                    // All teams are full, can't assign this player
                    continue;
                }
            }
            
            // Assign player to team
            teams.get(team).add(playerId);
            playerTeams.put(playerId, team);
            
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&7You are on team " + getTeamDisplayName(team));
            }
            
            teamIndex++;
        }
    }
    
    /**
     * Give team-colored armor to a player
     *
     * @param player The player
     * @param teamData The team data
     */
    private void giveTeamArmor(Player player, TeamData teamData) {
        Color teamColor = getTeamColor(teamData.getColor());
        
        // Create leather armor with team color
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        LeatherArmorMeta chestplateMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        
        helmetMeta.setColor(teamColor);
        chestplateMeta.setColor(teamColor);
        leggingsMeta.setColor(teamColor);
        bootsMeta.setColor(teamColor);
        
        helmet.setItemMeta(helmetMeta);
        chestplate.setItemMeta(chestplateMeta);
        leggings.setItemMeta(leggingsMeta);
        boots.setItemMeta(bootsMeta);
        
        // Give armor to player
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }
      /**
     * Give initial equipment to a player
     *
     * @param player The player
     */
    private void giveInitialEquipment(Player player) {
        // Wooden sword
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
        
        // No more wool blocks at the start - players should buy them from the shop
    }
    
    /**
     * Convert a team color string to a Bukkit Color
     *
     * @param colorName The color name
     * @return The Bukkit Color
     */
    private Color getTeamColor(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "RED" -> Color.RED;
            case "BLUE" -> Color.BLUE;
            case "GREEN" -> Color.GREEN;
            case "YELLOW" -> Color.YELLOW;
            case "AQUA" -> Color.AQUA;
            case "WHITE" -> Color.WHITE;
            case "PINK" -> Color.fromRGB(255, 105, 180);
            case "GRAY" -> Color.GRAY;
            default -> Color.WHITE;
        };
    }
    
    /**
     * Get the appropriate wool material for a team color
     * 
     * @param colorName The team color name
     * @return The wool material
     */
    private Material getTeamWoolMaterial(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "RED" -> Material.RED_WOOL;
            case "BLUE" -> Material.BLUE_WOOL;
            case "GREEN" -> Material.GREEN_WOOL;
            case "YELLOW" -> Material.YELLOW_WOOL;
            case "AQUA" -> Material.LIGHT_BLUE_WOOL;
            case "WHITE" -> Material.WHITE_WOOL;
            case "PINK" -> Material.PINK_WOOL;
            case "GRAY" -> Material.GRAY_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }
    
    /**
     * Get the appropriate bed material for a team color
     * 
     * @param colorName The team color name
     * @return The bed material
     */
    private Material getTeamBedMaterial(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "RED" -> Material.RED_BED;
            case "BLUE" -> Material.BLUE_BED;
            case "GREEN" -> Material.GREEN_BED;
            case "YELLOW" -> Material.YELLOW_BED;
            case "AQUA", "LIGHT_BLUE" -> Material.LIGHT_BLUE_BED;
            case "WHITE" -> Material.WHITE_BED;
            case "PINK" -> Material.PINK_BED;
            case "GRAY" -> Material.GRAY_BED;
            case "BLACK" -> Material.BLACK_BED;
            case "ORANGE" -> Material.ORANGE_BED;
            case "PURPLE" -> Material.PURPLE_BED;
            case "CYAN" -> Material.CYAN_BED;
            case "BROWN" -> Material.BROWN_BED;
            case "LIGHT_GRAY" -> Material.LIGHT_GRAY_BED;
            case "LIME" -> Material.LIME_BED;
            case "MAGENTA" -> Material.MAGENTA_BED;
            default -> Material.RED_BED;
        };
    }
    
    /**
     * Get a formatted team display name with color
     *
     * @param teamName The team name
     * @return Formatted team name
     */
    private String getTeamDisplayName(String teamName) {
        TeamData teamData = arena.getTeam(teamName);
        if (teamData == null) return "&f" + teamName;
        
        return switch (teamData.getColor().toUpperCase()) {
            case "RED" -> "&c" + teamName;
            case "BLUE" -> "&9" + teamName;
            case "GREEN" -> "&a" + teamName;
            case "YELLOW" -> "&e" + teamName;
            case "AQUA" -> "&b" + teamName;
            case "WHITE" -> "&f" + teamName;
            case "PINK" -> "&d" + teamName;
            case "GRAY" -> "&7" + teamName;
            default -> "&f" + teamName;
        };
    }
    
    /**
     * Teleport a player to the lobby
     *
     * @param player The player
     */
    private void teleportToLobby(Player player) {
        SerializableLocation lobbyLoc = arena.getLobbySpawn();
        if (lobbyLoc != null) {
            Location location = lobbyLoc.toBukkitLocation();
            if (location != null) {
                player.teleport(location);
            }
        }
    }
      /**
     * Reset a player's state
     *
     * @param player The player
     */    private void resetPlayer(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.getInventory().clear();
        
        // Clear enderchest contents
        player.getEnderChest().clear();
        
        // Use survival mode for active players, spectator for spectators
        if (spectators.contains(player.getUniqueId())) {
            player.setGameMode(GameMode.SPECTATOR);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }
      /**
     * Handle a player death
     *
     * @param player The player who died
     * @param killer The killer (can be null)
     */
    public void handlePlayerDeath(Player player, Player killer) {
        // Check if player is in this game
        if (!players.contains(player.getUniqueId())) return;
        
        // Record stats
        recordDeath(player);
        if (killer != null && players.contains(killer.getUniqueId())) {
            recordKill(killer);
            
            // Announce kill
            String playerTeam = playerTeams.get(player.getUniqueId());
            String killerTeam = playerTeams.get(killer.getUniqueId());
            
            broadcastMessage(getTeamDisplayName(killerTeam) + " &f" + killer.getName() + 
                    " &7killed " + getTeamDisplayName(playerTeam) + " &f" + player.getName());
        }
        
        // Get their team
        String team = playerTeams.get(player.getUniqueId());
        if (team == null) return;
        
        // Drop their resources
        dropPlayerResources(player);
        
        // Check if their bed is broken
        if (!bedStatus.getOrDefault(team, false)) {
            // Bed is broken, player is eliminated
            makePlayerSpectator(player);
            broadcastMessage("&e" + player.getName() + " &7was eliminated!");
            
            // Check for game end
            checkGameEnd();        } else {
            // Bed is intact, respawn after delay
            broadcastMessage("&e" + player.getName() + " &7will respawn in 5 seconds!");
            
            // Put player in spectator mode temporarily
            player.setGameMode(GameMode.SPECTATOR);
            
            // Schedule respawn
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Make sure they're still in the game
                if (players.contains(player.getUniqueId())) {
                    respawnPlayer(player);
                }
            }, 100L); // 5 seconds
        }
    }    /**
     * Drop resources from a player's inventory
     *
     * @param player The player
     */
    private void dropPlayerResources(Player player) {
        // List of materials to drop
        List<Material> resourceTypes = Arrays.asList(
                Material.IRON_INGOT,
                Material.GOLD_INGOT,
                Material.EMERALD,
                Material.DIAMOND
        );
        
        // Check death cause to determine if items should be dropped
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        Player killer = player.getKiller();
        
        // If player died in the void without being killed by a player, don't drop resources
        if (lastDamage != null && lastDamage.getCause() == EntityDamageEvent.DamageCause.VOID && killer == null) {
            return;
        }
        
        // Drop resources for other death causes or when killed by another player
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && resourceTypes.contains(item.getType())) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }
    
    /**
     * Make a player a spectator
     *
     * @param player The player
     */
    private void makePlayerSpectator(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Remove from team
        String team = playerTeams.remove(playerId);
        if (team != null) {
            teams.getOrDefault(team, Collections.emptySet()).remove(playerId);
        }
        
        // Add to spectators
        spectators.add(playerId);
        
        // Reset and set to spectator mode
        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
        
        // Teleport to spectator spawn
        SerializableLocation spectatorLoc = arena.getSpectatorSpawn();
        if (spectatorLoc != null) {
            Location location = spectatorLoc.toBukkitLocation();
            if (location != null) {
                player.teleport(location);
            }
        }
        
        MessageUtils.sendMessage(player, plugin.getPrefix() + "&eYou are now a spectator. Use /bw leave to exit the game.");
    }
      /**
     * Respawn a player at their team's spawn
     *
     * @param player The player
     */
    private void respawnPlayer(Player player) {
        // Save player's permanent tools before resetting
        Map<Material, ItemStack> permanentItems = savePermanentItems(player);
        
        // Reset player state
        resetPlayer(player);
        
        // Teleport to team spawn
        String team = playerTeams.get(player.getUniqueId());
        if (team != null) {
            TeamData teamData = arena.getTeam(team);
            if (teamData != null) {
                SerializableLocation spawnLoc = teamData.getSpawnLocation();
                if (spawnLoc != null) {
                    player.teleport(spawnLoc.toBukkitLocation());
                }
                  // Give team armor and initial equipment
                giveTeamArmor(player, teamData);
                giveInitialEquipment(player);
                
                // Apply team upgrades
                applyTeamUpgrades(player);
                
                // Restore permanent tools
                restorePermanentItems(player, permanentItems);
            }
        }
    }
    
    /**
     * Save permanent items (tools) from a player's inventory
     *
     * @param player The player
     * @return Map of saved items
     */
    private Map<Material, ItemStack> savePermanentItems(Player player) {
        Map<Material, ItemStack> saved = new HashMap<>();
        
        // Check for permanent tools in description
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir() && isPermanentTool(item)) {
                saved.put(item.getType(), item.clone());
            }
        }
        
        return saved;
    }
    
    /**
     * Check if an item is a permanent tool
     * 
     * @param item The item to check
     * @return true if permanent, false otherwise
     */
    private boolean isPermanentTool(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        
        Material type = item.getType();
        return type == Material.SHEARS || 
               type.name().contains("PICKAXE") ||
               type.name().contains("AXE");
    }
    
    /**
     * Restore permanent items to player's inventory
     * 
     * @param player The player
     * @param items Map of items to restore
     */
    private void restorePermanentItems(Player player, Map<Material, ItemStack> items) {
        for (ItemStack item : items.values()) {
            player.getInventory().addItem(item);
        }
    }
    
    /**
     * Handle bed broken event
     *
     * @param team The team whose bed was broken
     * @param player The player who broke the bed
     */
    public void handleBedBreak(String team, Player player) {
        if (team == null || !bedStatus.containsKey(team)) return;
        
        // Set bed status to broken
        bedStatus.put(team, false);
        
        // Record bed break stat
        recordBedBreak(player);
        
        // Get the player's team for colored announcement
        String playerTeam = playerTeams.get(player.getUniqueId());
        String playerTeamFormatted = playerTeam != null ? getTeamDisplayName(playerTeam) + " " : "";
        
        // Announce bed broken
        broadcastMessage("&c&lBED DESTROYED! " + getTeamDisplayName(team) + " &7bed was destroyed by " + 
                         playerTeamFormatted + "&e" + player.getName());
        
        // Play sound for all players
        for (UUID playerId : players) {
            Player gamePlayer = Bukkit.getPlayer(playerId);
            if (gamePlayer != null) {
                gamePlayer.playSound(gamePlayer.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
            }
        }
        
        // Send warning to affected team
        for (UUID teamPlayerId : teams.getOrDefault(team, Collections.emptySet())) {
            Player teamPlayer = Bukkit.getPlayer(teamPlayerId);
            if (teamPlayer != null) {
                MessageUtils.sendMessage(teamPlayer, "&c&lYOUR BED HAS BEEN DESTROYED! You will no longer respawn!");
                teamPlayer.playSound(teamPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
            }
        }
        
        // Award the player
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }
    
    /**
     * Record a placed block to track for cleanup
     * 
     * @param block The block that was placed
     */
    public void recordPlacedBlock(Block block) {
        if (block != null && state == GameState.RUNNING) {
            placedBlocks.add(block);
        }
    }
    
    /**
     * Check if a block was placed by a player in this game
     * 
     * @param block The block to check
     * @return true if placed during the game, false otherwise
     */
    public boolean isPlacedBlock(Block block) {
        return placedBlocks.contains(block);
    }
    
    /**
     * Check if the game should end
     */
    public void checkGameEnd() {
        if (state != GameState.RUNNING) return;
        
        // Count active teams (teams with at least one player)
        List<String> activeTeams = new ArrayList<>();
        
        for (Map.Entry<String, Set<UUID>> entry : teams.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                activeTeams.add(entry.getKey());
            }
        }
        
        // If there's 1 or 0 teams left, end the game
        if (activeTeams.size() <= 1) {
            String winningTeam = activeTeams.isEmpty() ? null : activeTeams.get(0);
            endGame(winningTeam);
        }
    }
    
    /**
     * End the game
     * 
     * @param winningTeam The winning team, or null for a draw
     */
    public void endGame(String winningTeam) {
        if (state == GameState.ENDING) return;
        
        state = GameState.ENDING;
        
        // Cancel tasks
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        if (gameTimerTask != null) {
            gameTimerTask.cancel();
        }
          // Stop generators
        for (ActiveGenerator generator : activeGenerators) {
            generator.stop();
        }
        activeGenerators.clear();
        
        // Stop scoreboard task
        if (scoreboardManager != null) {
            scoreboardManager.stopTask();
        }
          // Announce winner
        if (winningTeam != null) {
            broadcastTitle("&6&lGAME OVER!", getTeamDisplayName(winningTeam) + " &6team wins!");
            Set<UUID> winners = teams.getOrDefault(winningTeam, Collections.emptySet());
            
            String winnerNames = winners.stream()
                .map(id -> Bukkit.getPlayer(id))
                .filter(Objects::nonNull)
                .map(Player::getName)
                .collect(Collectors.joining(", "));
                
            broadcastMessage("&6&lWinners: &e" + winnerNames);
        } else {
            broadcastTitle("&6&lGAME OVER!", "&eIt's a draw!");
        }
        
        // Display game stats
        displayGameStats();
        
        // Set all players to spectator mode
        for (UUID playerId : new ArrayList<>(players)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
        
        // Display game stats
        displayGameStats();
        
        // Schedule game cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cleanup();
            plugin.getGameManager().endGame(this);
        }, 200L); // 10 seconds
    }
    
    /**
     * Record a player kill
     * 
     * @param player The player who got the kill
     */
    public void recordKill(Player player) {
        if (player == null) return;
        playerKills.put(player.getUniqueId(), playerKills.getOrDefault(player.getUniqueId(), 0) + 1);
    }
    
    /**
     * Record a player death
     * 
     * @param player The player who died
     */
    public void recordDeath(Player player) {
        if (player == null) return;
        playerDeaths.put(player.getUniqueId(), playerDeaths.getOrDefault(player.getUniqueId(), 0) + 1);
    }
    
    /**
     * Record a bed break
     * 
     * @param player The player who broke the bed
     */
    public void recordBedBreak(Player player) {
        if (player == null) return;
        playerBedBreaks.put(player.getUniqueId(), playerBedBreaks.getOrDefault(player.getUniqueId(), 0) + 1);
    }
    
    /**
     * Display game stats to all players
     */
    private void displayGameStats() {
        broadcastMessage("&6&l==== GAME STATS ====");
        
        // Find the top killer
        UUID topKiller = null;
        int topKills = -1;
        
        for (Map.Entry<UUID, Integer> entry : playerKills.entrySet()) {
            if (entry.getValue() > topKills) {
                topKills = entry.getValue();
                topKiller = entry.getKey();
            }
        }
        
        if (topKiller != null) {
            Player killer = Bukkit.getPlayer(topKiller);
            String killerName = killer != null ? killer.getName() : "Unknown";
            broadcastMessage("&eTop Killer: &c" + killerName + " &ewith &c" + topKills + " &ekills!");
        }
        
        // Display bed breaks
        if (!playerBedBreaks.isEmpty()) {
            StringBuilder bedBreakers = new StringBuilder();
            playerBedBreaks.forEach((id, count) -> {
                Player breaker = Bukkit.getPlayer(id);
                if (breaker != null) {
                    if (bedBreakers.length() > 0) bedBreakers.append(", ");
                    bedBreakers.append("&a").append(breaker.getName()).append(" &7(").append(count).append(")");
                }
            });
            broadcastMessage("&eBed Breakers: " + bedBreakers);
        }
        
        broadcastMessage("&6&l================");
    }
    
    /**
     * Clean up the game
     */
    public void cleanup() {
        // Restore placed blocks
        for (Block block : placedBlocks) {
            block.setType(Material.AIR);
        }
        placedBlocks.clear();
          // Remove dropped items and iron golems (Dream Defenders)
        World world = Bukkit.getWorld(arena.getWorldName());
        if (world != null) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    entity.remove();
                }                // Remove all Dream Defenders (Iron Golems) in the game world
                if (entity instanceof IronGolem) {
                    // Check if it's from this game or just remove all golems for safety
                    if (!entity.hasMetadata("game_id") || 
                        (entity.hasMetadata("game_id") && gameId.equals(entity.getMetadata("game_id").get(0).asString()))) {
                        // Cancel the timer task first if it exists
                        if (entity.hasMetadata("timer_task")) {
                            try {
                                int taskId = entity.getMetadata("timer_task").get(0).asInt();
                                Bukkit.getScheduler().cancelTask(taskId);
                            } catch (Exception e) {
                                // Ignore any errors with task cancellation
                            }
                        }
                        entity.remove();
                    }
                }
            }
        }
        
        // Return players to the server spawn
        for (UUID playerId : new HashSet<>(players)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                resetPlayer(player);
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
          // Clean up scoreboard
        if (scoreboardManager != null) {
            scoreboardManager.cleanup();
        }
          // Clear player lists
        players.clear();
        spectators.clear();
        playerTeams.clear();
        for (Set<UUID> teamPlayers : teams.values()) {
            teamPlayers.clear();
        }        // Place team beds for the next game
        placeTeamBeds();
    }
    
    /**
     * Send a message to all players in the game
     * 
     * @param message The message to send
     */
    public void broadcastMessage(String message) {
        String coloredMessage = MessageUtils.colorize(plugin.getPrefix() + message);
        
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(coloredMessage);
            }
        }
        
        for (UUID spectatorId : spectators) {
            Player spectator = Bukkit.getPlayer(spectatorId);
            if (spectator != null) {
                spectator.sendMessage(coloredMessage);
            }
        }
    }
    
    /**
     * Send a message to all players on a team
     * 
     * @param teamName The team name
     * @param message The message
     */
    public void sendTeamMessage(String teamName, String message) {
        String coloredMessage = MessageUtils.colorize(message);
        
        for (UUID playerId : teams.getOrDefault(teamName, Collections.emptySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(coloredMessage);
            }
        }
    }
    
    /**
     * Send a title to all players in the game
     * 
     * @param title The title
     * @param subtitle The subtitle
     */
    public void broadcastTitle(String title, String subtitle) {
        String coloredTitle = MessageUtils.colorize(title);
        String coloredSubtitle = MessageUtils.colorize(subtitle);
        
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendTitle(coloredTitle, coloredSubtitle, 10, 70, 20);
            }
        }
        
        for (UUID spectatorId : spectators) {
            Player spectator = Bukkit.getPlayer(spectatorId);
            if (spectator != null) {
                spectator.sendTitle(coloredTitle, coloredSubtitle, 10, 70, 20);
            }
        }
    }
    
    /**
     * Get the team of a player
     * 
     * @param player The player
     * @return The team name or null if not in a team
     */
    public String getPlayerTeam(Player player) {
        if (player == null) return null;
        return playerTeams.get(player.getUniqueId());
    }
    
    /**
     * Check if a player is in the game
     * 
     * @param player The player
     * @return true if in the game, false otherwise
     */
    public boolean isPlayerInGame(Player player) {
        if (player == null) return false;
        return players.contains(player.getUniqueId());
    }
    
    /**
     * Check if a player is a spectator
     * 
     * @param player The player
     * @return true if spectator, false otherwise
     */
    public boolean isSpectator(Player player) {
        if (player == null) return false;
        return spectators.contains(player.getUniqueId());
    }
    
    /**
     * Apply a team upgrade to a player
     * 
     * @param player The player
     * @param upgradeType The upgrade type
     * @param level The upgrade level
     */
    public void applyTeamUpgrade(Player player, org.bcnlab.beaconLabsBW.shop.TeamUpgrade.UpgradeType upgradeType, int level) {
        if (player == null || upgradeType == null || level <= 0) return;
        
        switch (upgradeType) {
            case SHARPNESS -> applySharpnessUpgrade(player, level);
            case PROTECTION -> applyProtectionUpgrade(player, level);
            case HASTE -> applyHasteUpgrade(player, level);
            case HEALING -> applyHealingUpgrade(player, level);
            case FORGE -> {
                // Forge is applied directly to generators by regeneration methods
            }
            case TRAP -> {
                // Trap is checked when enemies enter the base
            }
        }
    }
      /**
     * Apply sharpness upgrade to player's sword
     * 
     * @param player The player
     * @param level The upgrade level
     */
    private void applySharpnessUpgrade(Player player, int level) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().name().contains("SWORD")) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, level, true);
                    item.setItemMeta(meta);
                }
            }
        }
    }
    
    /**
     * Apply protection upgrade to player's armor
     * 
     * @param player The player
     * @param level The upgrade level
     */
    private void applyProtectionUpgrade(Player player, int level) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && !item.getType().isAir()) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, level, true);
                    item.setItemMeta(meta);
                }
            }
        }
    }
    
    /**
     * Apply haste upgrade to player
     * 
     * @param player The player
     * @param level The upgrade level
     */
    private void applyHasteUpgrade(Player player, int level) {
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.HASTE,
            Integer.MAX_VALUE,
            level - 1,
            false,
            false
        ));
    }
    
    /**
     * Apply healing upgrade to player
     * 
     * @param player The player
     * @param level The upgrade level
     */
    private void applyHealingUpgrade(Player player, int level) {
        // This is checked in a task that applies regen near base
    }
    
    /**
     * Apply all team upgrades to a player
     * 
     * @param player The player
     */
    private void applyTeamUpgrades(Player player) {
        String teamName = playerTeams.get(player.getUniqueId());
        if (teamName == null) return;
        
        org.bcnlab.beaconLabsBW.shop.TeamUpgradeManager upgradeManager = plugin.getTeamUpgradeManager();
        
        // Apply all upgrade types
        for (org.bcnlab.beaconLabsBW.shop.TeamUpgrade.UpgradeType type : 
             org.bcnlab.beaconLabsBW.shop.TeamUpgrade.UpgradeType.values()) {
            int level = upgradeManager.getUpgradeLevel(teamName, type);
            if (level > 0) {
                applyTeamUpgrade(player, type, level);
            }
        }
    }
    
    /**
     * Broadcast a message to a specific team
     * 
     * @param teamName The team name
     * @param message The message to broadcast
     */
    public void broadcastTeamMessage(String teamName, String message) {
        if (teamName == null || message == null) return;
        
        Set<UUID> teamPlayers = teams.getOrDefault(teamName, Collections.emptySet());
        for (UUID playerId : teamPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                MessageUtils.sendMessage(player, message);
            }
        }
    }
    
    /**
     * Get all members of a team
     * 
     * @param teamName The team name
     * @return Set of player UUIDs in the team
     */
    public Set<UUID> getTeamMembers(String teamName) {
        return teams.getOrDefault(teamName, Collections.emptySet());
    }
    
    /**
     * Clean up any Dream Defenders in the game world
     * This is used when starting a new game to remove any golems from previous games
     */
    private void cleanupDreamDefenders() {
        World world = Bukkit.getWorld(arena.getWorldName());
        if (world != null) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof IronGolem) {
                    // Remove all Iron Golems in the game world
                    if (entity.hasMetadata("timer_task")) {
                        int taskId = entity.getMetadata("timer_task").get(0).asInt();
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                    entity.remove();
                }
            }
        }
    }
      /**
     * Place team beds at the start of the game
     */
    private void placeTeamBeds() {
        // Reset bed status for the game
        for (String teamName : arena.getTeams().keySet()) {
            bedStatus.put(teamName, true); // All beds are intact at start
            
            // Place the physical bed block
            TeamData teamData = arena.getTeam(teamName);
            if (teamData != null && teamData.getBedLocation() != null) {
                Location bedLocation = teamData.getBedLocation().toBukkitLocation();
                if (bedLocation != null && bedLocation.getWorld() != null) {
                    try {
                        // Determine optimal direction based on surrounding blocks
                        BlockFace direction = determineOptimalBedDirection(bedLocation);
                        
                        // Get the appropriate bed color for this team
                        Material bedMaterial = getTeamBedMaterial(teamData.getColor());
                        
                        // Clear any blocks at the bed and foot locations first
                        Block bedBlock = bedLocation.getBlock();
                        Block footBlock = bedLocation.getBlock().getRelative(direction.getOppositeFace());
                        bedBlock.setType(Material.AIR);
                        footBlock.setType(Material.AIR);
                        
                        // Place the bed block (head part)
                        bedBlock.setType(bedMaterial);
                        
                        // Set the bed direction
                        Bed bedData = (Bed) bedBlock.getBlockData();
                        bedData.setPart(Bed.Part.HEAD);
                        bedData.setFacing(direction);
                        bedBlock.setBlockData(bedData);
                        
                        // Place the foot part in the correct direction
                        footBlock.setType(bedMaterial);
                        
                        Bed footData = (Bed) footBlock.getBlockData();
                        footData.setPart(Bed.Part.FOOT);
                        footData.setFacing(direction);
                        footBlock.setBlockData(footData);
                        
                        plugin.getLogger().info("Placed " + teamData.getColor() + " bed for team " + teamName);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error placing bed for team " + teamName + ": " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Clear dropped items in the arena at game start
     */
    private void clearArenaItems() {
        World world = Bukkit.getWorld(arena.getWorldName());
        if (world != null) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    entity.remove();
                }
            }
            plugin.getLogger().info("Cleared all dropped items in arena " + arena.getName());
        }
    }
    
    /**
     * Determine the best direction for placing a bed based on surrounding blocks
     * 
     * @param location The location where the bed head will be placed
     * @return The BlockFace representing the optimal direction
     */
    private BlockFace determineOptimalBedDirection(Location location) {
        // Check if we have any solid blocks around to place bed against
        Block block = location.getBlock();
        
        // Check all four horizontal directions
        BlockFace[] directions = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        
        // First look for walls to place bed against
        for (BlockFace face : directions) {
            Block relative = block.getRelative(face);
            if (relative.getType().isSolid() && !relative.getType().toString().contains("BED")) {
                // Return the opposite direction so the bed faces away from the wall
                return face.getOppositeFace();
            }
        }
        
        // If no walls found, look for space for the foot part
        for (BlockFace face : directions) {
            Block footBlock = block.getRelative(face.getOppositeFace());
            if (footBlock.getType().isAir() || footBlock.getType() == Material.CAVE_AIR) {
                return face;
            }
        }
        
        // Default to NORTH if no better option found
        return BlockFace.NORTH;
    }
}
