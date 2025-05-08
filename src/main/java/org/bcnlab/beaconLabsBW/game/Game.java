package org.bcnlab.beaconLabsBW.game;

import lombok.Getter;
import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.game.ultimates.UltimateClass;
import org.bcnlab.beaconLabsBW.arena.model.GeneratorData;
import org.bcnlab.beaconLabsBW.arena.model.SerializableLocation;
import org.bcnlab.beaconLabsBW.arena.model.TeamData;
import org.bcnlab.beaconLabsBW.generator.ActiveGenerator;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
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
    
    // Game mode
    private GameMode gameMode = GameMode.NORMAL;
    private final Map<UUID, UltimateClass> playerUltimateClasses = new ConcurrentHashMap<>();
    
    // Game state tracking
    private final Map<String, Boolean> bedStatus = new ConcurrentHashMap<>();
    private final List<Block> placedBlocks = new ArrayList<>();
    private final List<ActiveGenerator> activeGenerators = new ArrayList<>();
    
    // Tasks
    private BukkitTask countdownTask;
    private BukkitTask gameTimerTask;
    private BukkitTask slowHealTask; // Add task variable
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
        
        // Clear any fire from the arena before game setup continues
        clearAllFireInArena();
        
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
     * Set the countdown timer value
     * 
     * @param countdown The new countdown value in seconds
     */
    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }
    
    /**
     * Start the countdown if it's not already started
     */
    public void startCountdown() {
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
        
        // Clear fire again just before game truly starts (after countdown)
        // This ensures any fire placed during WAITING/STARTING by other means is also gone.
        clearAllFireInArena();
        
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
                        
                        // Give ultimates items if in ultimates mode
                        if (gameMode == GameMode.ULTIMATES) {
                            giveUltimateItems(player);
                        }
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
        
        // Disable natural regeneration for the game world
        World world = Bukkit.getWorld(arena.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.NATURAL_REGENERATION, false); // Keep this false
            plugin.getLogger().info("[Game " + gameId + "] Set naturalRegeneration to false for world: " + world.getName());
        }
        
        // Start game timer
        startGameTimer();
        // Start slow heal task
        startSlowHealTask();
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
        
        // Set armor to be unbreakable to fix durability issues
        helmetMeta.setUnbreakable(true);
        chestplateMeta.setUnbreakable(true);
        leggingsMeta.setUnbreakable(true);
        bootsMeta.setUnbreakable(true);
        
        // Hide the unbreakable flag from players
        helmetMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
        chestplateMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
        leggingsMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
        bootsMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
        
        helmet.setItemMeta(helmetMeta);
        chestplate.setItemMeta(chestplateMeta);
        leggings.setItemMeta(leggingsMeta);
        boots.setItemMeta(bootsMeta);
        
        // Give armor to player
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }    /**
     * Give initial equipment to a player
     *
     * @param player The player
     */
    private void giveInitialEquipment(Player player) {
        // Give a wooden sword by default.
        ItemStack woodenSword = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta swordMeta = woodenSword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.setUnbreakable(true);
            swordMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
            woodenSword.setItemMeta(swordMeta);
        }
        player.getInventory().addItem(woodenSword); 
        
        // No initial tools given anymore based on previous request
        // player.getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE)); 
        // player.getInventory().addItem(new ItemStack(Material.WOODEN_AXE));
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
    public Material getTeamWoolMaterial(String colorName) {
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
    }    /**
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
        
        // Clear potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        // Disable flight (for Kangaroo ultimate)
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Clear cooldown display if in ultimates mode
        if (gameMode == GameMode.ULTIMATES) {
            plugin.getUltimatesManager().clearCooldownDisplay(player);
        }
        
        // Use survival mode for active players, spectator for spectators
        if (spectators.contains(player.getUniqueId())) {
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        } else {
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
    }
      /**
     * Handle a player death
     *
     * @param player The player who died
     * @param killer The player who killed this player, or null if no killer or self-kill
     */
    public void handlePlayerDeath(Player player, Player killer) {
        // Check if player is in this game
        if (!players.contains(player.getUniqueId())) return;

        // Downgrade tools before anything else happens to inventory or stats
        downgradeTools(player);
        
        // Get last damage cause for void check
        EntityDamageEvent lastDamageCause = player.getLastDamageCause();
        boolean voidDeath = (lastDamageCause != null && lastDamageCause.getCause() == EntityDamageEvent.DamageCause.VOID);

        // Record stats & Announce Kill/Death
        recordDeath(player);
        String playerTeamDisplay = getTeamDisplayName(playerTeams.get(player.getUniqueId()));
        
        if (killer != null && players.contains(killer.getUniqueId()) && killer != player) { 
            recordKill(killer);
            String killerTeamDisplay = getTeamDisplayName(playerTeams.get(killer.getUniqueId()));
            broadcastMessage(killerTeamDisplay + " &f" + killer.getName() + 
                    " &7killed " + playerTeamDisplay + " &f" + player.getName());
        } else if (voidDeath) {
            // Handle void death announcement
            broadcastMessage(playerTeamDisplay + " &f" + player.getName() + " &7fell into the void.");
            killer = null; // Ensure killer is null for resource transfer logic if void death
        } else {
            // Handle other deaths (e.g., environmental, suicide - without specific cause messages for now)
             broadcastMessage(playerTeamDisplay + " &f" + player.getName() + " &7died.");
             killer = null; // Treat as no killer for resource transfer
        }
        
        // Get their team
        String team = playerTeams.get(player.getUniqueId());
        if (team == null) return;

        // Give resources to killer or delete them
        transferOrDeletePlayerResources(player, killer);
        
        // Handle ultimate abilities that trigger on death
        if (gameMode == GameMode.ULTIMATES) {
            handleUltimateOnDeath(player, killer);
        }
        
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
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            
            // Schedule respawn
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Make sure they're still in the game
                if (players.contains(player.getUniqueId())) {
                    respawnPlayer(player);
                }
            }, 100L); // 5 seconds
        }
    }
    
    /**
     * Give resources to killer or delete them
     * @param player The player
     * @param killer The player who killed this player, or null if no killer or self-kill
     */
    private void transferOrDeletePlayerResources(Player player, Player killer) {
        List<Material> resourceTypes = Arrays.asList(
                Material.IRON_INGOT,
                Material.GOLD_INGOT,
                Material.EMERALD,
                Material.DIAMOND
        );

        ArrayList<ItemStack> itemsToTransfer = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && resourceTypes.contains(item.getType())) {
                itemsToTransfer.add(item.clone()); // Clone to avoid issues with concurrent modification
            }
        }
        
        // Manually remove items from the dying player's inventory
        for (Material resourceType : resourceTypes) {
            player.getInventory().remove(resourceType);
        }

        if (killer != null && killer.isOnline() && players.contains(killer.getUniqueId()) && killer != player) {
            // Transfer to killer's inventory
            for (ItemStack itemToGive : itemsToTransfer) {
                HashMap<Integer, ItemStack> leftover = killer.getInventory().addItem(itemToGive);
                if (!leftover.isEmpty()) {
                    // If killer's inventory is full, drop remaining at killer's location (or decide to delete)
                    // For now, let's drop at killer's location as a common fallback.
                    for (ItemStack drop : leftover.values()) {
                        killer.getWorld().dropItemNaturally(killer.getLocation(), drop);
                    }
                }
            }
            MessageUtils.sendMessage(killer, plugin.getPrefix() + "&aYou received resources from your kill!");
        } else {
            // If no killer, or void death, or killer is not valid, resources are deleted (already removed from player's inv)
            // No items are dropped on the ground at the death location.
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
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        
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
        // Save player's permanent tools AND armor before resetting
        Map<Material, ItemStack> permanentItems = savePermanentItems(player);
        
        // Reset player state (clears inventory, effects, etc.)
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
                // Give basic team armor FIRST
                giveTeamArmor(player, teamData);
                
                // Give initial equipment (e.g., wooden sword, non-armor items)
                giveInitialEquipment(player);

                // Re-equip saved permanent armor and add saved tools
                // This will overwrite the basic team armor if better armor was saved.
                List<ItemStack> toolsToRestore = new ArrayList<>();
                for (ItemStack savedItem : permanentItems.values()) {
                    Material type = savedItem.getType();
                    if (type.name().contains("HELMET")) {
                        player.getInventory().setHelmet(savedItem);
                    } else if (type.name().contains("CHESTPLATE")) {
                        player.getInventory().setChestplate(savedItem);
                    } else if (type.name().contains("LEGGINGS")) {
                        player.getInventory().setLeggings(savedItem);
                    } else if (type.name().contains("BOOTS")) {
                        player.getInventory().setBoots(savedItem);
                    } else {
                        // It's a tool, add to list to be given to inventory
                        toolsToRestore.add(savedItem);
                    }
                }
                // Add tools to inventory
                for (ItemStack tool : toolsToRestore) {
                    player.getInventory().addItem(tool);
                }
                
                // Apply team upgrades (sharpness, protection on currently equipped armor, haste)
                applyTeamUpgrades(player);
                
                // Fix armor durability again after all armor is set and upgrades applied
                org.bcnlab.beaconLabsBW.utils.ArmorHandler.fixPlayerArmor(player);
                
                // Give ultimates items if in ultimates mode
                if (gameMode == GameMode.ULTIMATES) {
                    giveUltimateItems(player);
                }
            }
        }
    }
    
    /**
     * Save permanent items (tools and armor) from a player's inventory
     *
     * @param player The player
     * @return Map of saved items
     */
    private Map<Material, ItemStack> savePermanentItems(Player player) {
        Map<Material, ItemStack> saved = new HashMap<>();
        
        // Check for permanent tools and armor in inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir() && isPermanentToolOrArmor(item)) { // Renamed check method
                saved.put(item.getType(), item.clone());
            }
        }
        // Also check equipped armor slots
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && !item.getType().isAir() && isPermanentToolOrArmor(item)) { // Renamed check method
                 if (!saved.containsKey(item.getType())) { // Avoid duplicates if also in main inventory (though unlikely for armor)
                    saved.put(item.getType(), item.clone());
                }
            }
        }
        return saved;
    }
    
    /**
     * Check if an item is a permanent tool or armor
     * 
     * @param item The item to check
     * @return true if permanent, false otherwise
     */
    private boolean isPermanentToolOrArmor(ItemStack item) { // Renamed method
        if (item == null || item.getType().isAir()) return false;
        
        Material type = item.getType();
        // Check for tools
        boolean isTool = type == Material.SHEARS || 
               type.name().contains("PICKAXE") || // Matches WOODEN_PICKAXE, STONE_PICKAXE, etc.
               type.name().contains("AXE");      // Matches WOODEN_AXE, STONE_AXE, etc.
        if (isTool) return true;

        // Check for armor (based on material name, assuming standard armor types)
        // This relies on permanent armor from shop having appropriate material types.
        boolean isArmor = type.name().contains("HELMET") ||
               type.name().contains("CHESTPLATE") ||
               type.name().contains("LEGGINGS") ||
               type.name().contains("BOOTS");
        // Ensure it's not leather armor unless specifically marked permanent (which it isn't by default)
        if (isArmor && type.name().startsWith("LEATHER_")) return false; // Default team armor is not "permanent"
        
        return isArmor; // If it made it here and isArmor, it's non-leather armor
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
        if (slowHealTask != null) { // Cancel slow heal task
             slowHealTask.cancel();
             plugin.getLogger().info("[Game " + gameId + "] Cancelled slow heal task.");
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
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            }
        }
        
        // Display game stats
        displayGameStats();
          // Schedule game cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Kick all players from the server
            for (UUID playerId : new ArrayList<>(players)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&', 
                        "&6&lBEDWARS\n&r\n" + 
                        (winningTeam != null ? getTeamDisplayName(winningTeam) + " &6team wins!" : "&eIt's a draw!") +
                        "\n&r\n&7Thanks for playing!"));
                }
            }
            
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
        broadcastMessage("&6&l&m-------------------------------------");
        broadcastMessage("                     &6&lGAME STATS");
        broadcastMessage(""); // Spacer

        // Combine all players who participated (based on kills, deaths, or bed breaks)
        Set<UUID> participants = new HashSet<>();
        participants.addAll(playerKills.keySet());
        participants.addAll(playerDeaths.keySet());
        participants.addAll(playerBedBreaks.keySet());

        if (participants.isEmpty()) {
            broadcastMessage("&7No player statistics available for this game.");
        } else {
            List<UUID> sortedParticipants = new ArrayList<>(participants);
            // Optional: Sort players by kills or another metric
            sortedParticipants.sort((p1, p2) -> Integer.compare(playerKills.getOrDefault(p2, 0), playerKills.getOrDefault(p1, 0))); // Sort descending by kills

            broadcastMessage("            &ePlayer            &aKills &cDeaths  &bK/D  &dBeds");
            broadcastMessage("&7&m-------------------------------------");

            for (UUID playerId : sortedParticipants) {
                Player player = Bukkit.getPlayer(playerId);
                String playerName = player != null ? player.getName() : ("Offline(" + playerId.toString().substring(0, 6) + ")");
                
                // Ensure player name fits in alignment (adjust padding as needed)
                playerName = String.format("%-18s", playerName); // Left-align, pad to 18 chars

                int kills = playerKills.getOrDefault(playerId, 0);
                int deaths = playerDeaths.getOrDefault(playerId, 0);
                int bedBreaks = playerBedBreaks.getOrDefault(playerId, 0);

                // Calculate K/D Ratio
                double kdr;
                if (deaths == 0) {
                    kdr = kills; // Treat as KDR = Kills if deaths are 0
                } else {
                    kdr = (double) kills / deaths;
                }
                String kdrFormatted = String.format("%.2f", kdr);

                // Format stats line
                String statsLine = String.format("&e%s &7- &a%3d   &c%3d   &b%5s  &d%3d", 
                                               playerName, 
                                               kills, 
                                               deaths, 
                                               kdrFormatted, 
                                               bedBreaks);
                broadcastMessage(statsLine);
            }
        }

        broadcastMessage(""); // Spacer
        broadcastMessage("&6&l&m-------------------------------------");
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

        // Clear fire at the very end of cleanup as well
        clearAllFireInArena();
        
        // Remove dropped items and iron golems (Dream Defenders)
        World world = Bukkit.getWorld(arena.getWorldName());
        if (world != null) {
            // Restore natural regeneration (ensure this stays)
            world.setGameRule(GameRule.NATURAL_REGENERATION, true);
            plugin.getLogger().info("[Game " + gameId + "] Restored naturalRegeneration to true for world: " + world.getName());
            
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
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
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
     * Reset and fix a bed at a specific location if it's broken or incorrectly placed.
     * This is used as a fallback when a player interacts with a bed and encounters an error.
     * 
     * @param location The location of the bed block that was interacted with
     */
    public void resetBedAtLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        
        // Find which team's bed is at or near this location
        for (String teamName : arena.getTeams().keySet()) {
            TeamData teamData = arena.getTeam(teamName);
            if (teamData != null && teamData.getBedLocation() != null) {
                Location bedLocation = teamData.getBedLocation().toBukkitLocation();
                
                // Check if this is close enough to the team's bed location (within 2 blocks)
                if (bedLocation != null && 
                    bedLocation.getWorld().equals(location.getWorld()) &&
                    bedLocation.distance(location) < 3) {
                    
                    plugin.getLogger().info("Attempting to repair " + teamName + " team's bed after interaction issue");
                    
                    // Get the original bed direction based on the team's bed location yaw
                    BlockFace direction = yawToFace(bedLocation.getYaw());
                    
                    // Get the appropriate bed color for this team
                    Material bedMaterial = getTeamBedMaterial(teamData.getColor());
                    
                    // Clear a 3x3x1 area around the bed location to ensure no blocks are interfering
                    Block centerBlock = bedLocation.getBlock();
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            Block block = centerBlock.getRelative(x, 0, z);
                            if (block.getType().name().contains("BED")) {
                                block.setType(Material.AIR);
                            }
                        }
                    }
                    
                    // Place a completely new bed
                    Block bedBlock = bedLocation.getBlock();
                    Block footBlock = bedLocation.getBlock().getRelative(direction.getOppositeFace());
                    
                    // Make sure both blocks are clear
                    bedBlock.setType(Material.AIR);
                    footBlock.setType(Material.AIR);
                    
                    // Place the bed with a different approach
                    try {
                        // First place both blocks as the bed material
                        bedBlock.setType(bedMaterial);
                        footBlock.setType(bedMaterial);
                        
                        // Configure the head part
                        Bed bedData = (Bed) bedBlock.getBlockData();
                        bedData.setPart(Bed.Part.HEAD);
                        bedData.setFacing(direction);
                        bedBlock.setBlockData(bedData);
                        
                        // Configure the foot part
                        Bed footData = (Bed) footBlock.getBlockData();
                        footData.setPart(Bed.Part.FOOT);
                        footData.setFacing(direction);
                        footBlock.setBlockData(footData);
                        
                        // Force update of both blocks
                        bedBlock.getState().update(true, false);
                        footBlock.getState().update(true, false);
                        
                        plugin.getLogger().info("Successfully repaired bed for team " + teamName);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to repair bed: " + e.getMessage());
                    }
                    
                    // We found and tried to repair the bed, so return
                    return;
                }
            }
        }
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
    }    /**
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
                    try {                        // Determine bed direction from the bed location's yaw
                        BlockFace direction = yawToFace(bedLocation.getYaw());
                        
                        // Get the appropriate bed color for this team
                        Material bedMaterial = getTeamBedMaterial(teamData.getColor());
                        
                        // Clear any blocks at the bed and foot locations first
                        Block bedBlock = bedLocation.getBlock();
                        Block footBlock = bedLocation.getBlock().getRelative(direction.getOppositeFace());
                        
                        // Check if the foot position is valid (not obstructed by solid blocks)
                        // If it's not valid, fall back to the original method
                        if (footBlock.getType().isSolid() && !footBlock.getType().toString().contains("BED")) {
                            direction = determineOptimalBedDirection(bedLocation);
                            footBlock = bedLocation.getBlock().getRelative(direction.getOppositeFace());
                            plugin.getLogger().info("Using fallback bed direction for " + teamName + " team");
                        }
                        
                        // Make sure both blocks are clear before placing the bed
                        bedBlock.setType(Material.AIR);
                        footBlock.setType(Material.AIR);
                          // We'll place the bed using a safer approach - placing both parts with a synchronized approach
                        // to avoid the "air block" error
                        
                        try {
                            // First place both blocks as the bed material
                            bedBlock.setType(bedMaterial);
                            footBlock.setType(bedMaterial);
                            
                            // Configure the head part
                            Bed bedData = (Bed) bedBlock.getBlockData();
                            bedData.setPart(Bed.Part.HEAD);
                            bedData.setFacing(direction);
                            bedBlock.setBlockData(bedData);
                            
                            // Configure the foot part
                            Bed footData = (Bed) footBlock.getBlockData();
                            footData.setPart(Bed.Part.FOOT);
                            footData.setFacing(direction);
                            footBlock.setBlockData(footData);
                            
                            // Force update of both blocks
                            bedBlock.getState().update(true, false);
                            footBlock.getState().update(true, false);
                        } catch (Exception e) {
                            // If there's an error with the connected bed parts, try an alternative approach
                            plugin.getLogger().warning("Error during bed placement, trying alternative method: " + e.getMessage());
                            
                            // Clear the blocks again
                            bedBlock.setType(Material.AIR);
                            footBlock.setType(Material.AIR);
                            
                            // Place head part first this time
                            bedBlock.setType(bedMaterial);
                            Bed bedData = (Bed) bedBlock.getBlockData();
                            bedData.setPart(Bed.Part.HEAD);
                            bedData.setFacing(direction);
                            bedBlock.setBlockData(bedData);
                            
                            // Update the world
                            bedBlock.getState().update(true, false);
                            
                            // Now place the foot part
                            footBlock.setType(bedMaterial);
                            Bed footData = (Bed) footBlock.getBlockData();
                            footData.setPart(Bed.Part.FOOT);
                            footData.setFacing(direction);
                            footBlock.setBlockData(footData);
                            
                            // Update the world
                            footBlock.getState().update(true, false);
                        }
                          plugin.getLogger().info("Placed " + teamData.getColor() + " bed for team " + teamName + 
                            " in direction " + direction + " (yaw: " + bedLocation.getYaw() + ")");
                        plugin.getLogger().info("  Head block at: " + bedBlock.getLocation() + ", Foot block at: " + footBlock.getLocation());
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
    }    /**
     * Convert a yaw value to a BlockFace direction
     *
     * @param yaw The yaw value to convert
     * @return The BlockFace corresponding to the yaw direction
     */
    private BlockFace yawToFace(float yaw) {
        // Normalize yaw to be between 0 and 360
        yaw = (yaw % 360);
        if (yaw < 0) yaw += 360;
        
        plugin.getLogger().info("Converting yaw " + yaw + " to BlockFace");
        
        // In Minecraft:
        // 0 or 360 = South
        // 90 = West
        // 180 = North
        // 270 = East
        
        // Beds are placed in the direction the player is facing
        if ((yaw >= 315 && yaw <= 360) || (yaw >= 0 && yaw < 45)) {
            return BlockFace.SOUTH; // Player facing south (yaw 0)
        } else if (yaw >= 45 && yaw < 135) {
            return BlockFace.WEST; // Player facing west (yaw 90)
        } else if (yaw >= 135 && yaw < 225) {
            return BlockFace.NORTH; // Player facing north (yaw 180)
        } else { // yaw >= 225 && yaw < 315
            return BlockFace.EAST; // Player facing east (yaw 270)
        }
    }

    /**
     * Determine the best direction for placing a bed based on surrounding blocks
     * Used as a fallback if yaw-based placement doesn't work
     * 
     * @param location The location where the bed head will be placed
     * @return The BlockFace representing the optimal direction
     */    private BlockFace determineOptimalBedDirection(Location location) {
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
    
    /**
     * Set the game mode for this BedWars game
     * 
     * @param gameMode The game mode to set
     */
    public void setGameMode(GameMode gameMode) {
        if (state == GameState.WAITING || state == GameState.STARTING) {
            this.gameMode = gameMode;
            broadcastMessage("&aGame mode has been set to &e" + gameMode.getDisplayName());
        }
    }
    
    /**
     * Get the current game mode
     * 
     * @return Current game mode
     */
    public GameMode getGameMode() {
        return gameMode;
    }
    
    /**
     * Set a player's ultimate class
     * 
     * @param playerId The player UUID
     * @param ultimateClass The ultimate class to assign
     */
    public void setPlayerUltimateClass(UUID playerId, UltimateClass ultimateClass) {
        if (gameMode == GameMode.ULTIMATES) {
            // Remove old ultimate items if player had a class before
            UltimateClass oldClass = playerUltimateClasses.get(playerId);
            if (oldClass != null) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    // Remove old ultimate item
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && isUltimateItem(item, oldClass)) {
                            player.getInventory().remove(item);
                        }
                    }
                    
                    // Remove Kangaroo flight if they were Kangaroo
                    if (oldClass == UltimateClass.KANGAROO) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    }
                }
            }
            
            // Set new class
            playerUltimateClasses.put(playerId, ultimateClass);
            
            // Give new ultimate items
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Create and give new ultimate item
                ItemStack ultimateItem = plugin.getUltimatesManager().createUltimateItem(player, ultimateClass);
                if (ultimateItem != null) {
                    player.getInventory().addItem(ultimateItem);
                }
                  // Handle class-specific setup
                switch (ultimateClass) {
                    case BUILDER:
                        // Give builder a small amount of wool to start with
                        ItemStack wool = new ItemStack(getTeamWoolMaterial(playerTeams.get(playerId)), 16);
                        player.getInventory().addItem(wool);
                        break;
                    case KANGAROO:
                        // Enable double-jump capability
                        player.setAllowFlight(true);
                        break;
                    // Other classes don't need special setup
                    case SWORDSMAN, HEALER, FROZO, GATHERER, DEMOLITION:
                        break;
                }
                
                MessageUtils.sendMessage(player, "&aYou have switched to the &e" + ultimateClass.getFormattedName() + " &aultimate class!");
                MessageUtils.sendMessage(player, "&e" + ultimateClass.getDescription());
            }
        }
    }    /**
     * Check if an item is a specific ultimate class's item
     * 
     * @param item The item to check
     * @param ultimateClass The ultimate class to check against
     * @return true if the item belongs to the ultimate class
     */
    private boolean isUltimateItem(ItemStack item, UltimateClass ultimateClass) {
        // Special case for Kangaroo which doesn't have an ultimate item
        if (ultimateClass == UltimateClass.KANGAROO) {
            return false; // Kangaroo doesn't use an item for its ability
        }
        
        if (item == null) return false;
        
        return switch (ultimateClass) {
            case SWORDSMAN -> item.getType() == Material.BLAZE_ROD;
            case HEALER -> item.getType() == Material.GOLDEN_APPLE;
            case FROZO -> item.getType() == Material.PACKED_ICE;
            case BUILDER -> item.getType() == Material.BRICKS;
            case GATHERER -> item.getType() == Material.ENDER_CHEST;
            case DEMOLITION -> item.getType() == Material.FIRE_CHARGE;
            case KANGAROO -> false; // Kangaroo doesn't use an item, but we need it in the switch for completeness
            default -> false;
        };
    }
    
    /**
     * Get a player's ultimate class
     * 
     * @param playerId The player UUID
     * @return The player's ultimate class, or null if not assigned
     */
    public UltimateClass getPlayerUltimateClass(UUID playerId) {
        return playerUltimateClasses.get(playerId);
    }
    
    /**
     * Give ultimate ability items to a player based on their selected class
     * 
     * @param player The player to give items to
     */    private void giveUltimateItems(Player player) {
        // Check if player has selected an ultimate class
        UltimateClass playerClass = getPlayerUltimateClass(player.getUniqueId());
        
        // If player hasn't chosen a class, give them a random one
        if (playerClass == null) {
            playerClass = UltimateClass.getRandomClass();
            setPlayerUltimateClass(player.getUniqueId(), playerClass);
            MessageUtils.sendMessage(player, "&eYou have been randomly assigned the " + playerClass.getFormattedName() + " &eclass!");
        }

        // If player is Swordsman, remove any existing wooden swords first
        if (playerClass == UltimateClass.SWORDSMAN) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() == Material.WOODEN_SWORD) {
                    player.getInventory().clear(i);
                }
            }
        }

          // Create the ultimate ability item and give it to the player
        // First clear any existing ultimate items
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (item.getType() == Material.BLAZE_ROD || item.getType() == Material.GOLDEN_APPLE || 
                item.getType() == Material.PACKED_ICE || item.getType() == Material.BRICKS || 
                item.getType() == Material.ENDER_CHEST || item.getType() == Material.FIRE_CHARGE)) {
                player.getInventory().remove(item);
            }
        }
          ItemStack ultimateItem = plugin.getUltimatesManager().createUltimateItem(player, playerClass);
        if (ultimateItem != null) {
            player.getInventory().addItem(ultimateItem);
        }
        
        // Send instructions on how to use the ability
        MessageUtils.sendMessage(player, "&b&lULTIMATE ABILITY: &eYou have the " + playerClass.getFormattedName() + " &eability!");
        MessageUtils.sendMessage(player, "&e" + playerClass.getDescription());
          // Give additional items based on class
        switch (playerClass) {
            case BUILDER:
                // First remove any team wool they might already have
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType().name().contains("WOOL")) {
                        player.getInventory().remove(item);
                    }
                }
                // Give builder a small amount of wool to start with
                ItemStack wool = new ItemStack(getTeamWoolMaterial(playerTeams.get(player.getUniqueId())), 16);
                player.getInventory().addItem(wool);
                break;
            case KANGAROO:
                // Enable double-jump capability
                player.setAllowFlight(true);
                break;
            // Other classes don't need special setup
            case SWORDSMAN, HEALER, FROZO, GATHERER, DEMOLITION:
                break;
        }
    }
    
    /**
     * Handle ultimate abilities that trigger on player death
     * 
     * @param player The player who died
     * @param killer The player who killed them (can be null)
     */    private void handleUltimateOnDeath(Player player, Player killer) {
        UltimateClass playerClass = getPlayerUltimateClass(player.getUniqueId());
        if (playerClass == null) return;
        
        switch (playerClass) {
            case DEMOLITION:
                // Drop TNT on death
                player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.TNT, 1));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);
                
                // Notify nearby players
                for (Player nearby : player.getLocation().getWorld().getPlayers()) {
                    if (nearby.getLocation().distance(player.getLocation()) <= 10) {
                        MessageUtils.sendMessage(nearby, "&c&lBOOM! &eA Demolition expert just died nearby!");
                    }
                }
                break;
                
            case KANGAROO:
                // Save 50% of resources on death
                if (Math.random() < 0.5) {
                    // Already handled in transferOrDeletePlayerResources method
                    MessageUtils.sendMessage(player, "&e&lKANGAROO: &aYou saved some resources in your inventory!");
                }
                break;
                
            // Add all remaining cases for completeness
            case SWORDSMAN:
            case HEALER:
            case FROZO:
            case BUILDER:
            case GATHERER:
                // These classes don't have special death effects
                break;
        }
    }

    /**
     * Add a player directly as a spectator.
     * 
     * @param player The player to add as a spectator.
     * @return true if successfully added, false otherwise (e.g., already in game or game not running).
     */
    public boolean addSpectator(Player player) {
        if (player == null) return false;
        UUID playerId = player.getUniqueId();

        // Only allow adding spectators to running games
        if (state != GameState.RUNNING) {
            plugin.getLogger().warning("Attempted to add spectator " + player.getName() + " to game " + gameId + " but state is " + state);
            return false;
        }

        // Check if player is already participating or spectating
        if (players.contains(playerId) || spectators.contains(playerId)) {
            plugin.getLogger().warning("Attempted to add spectator " + player.getName() + " who is already in game " + gameId);
            return false;
        }

        // Add to spectators set
        spectators.add(playerId);

        // Teleport to spectator spawn
        SerializableLocation spectatorLoc = arena.getSpectatorSpawn();
        if (spectatorLoc != null) {
            Location location = spectatorLoc.toBukkitLocation();
            if (location != null) {
                player.teleport(location);
            }
        }

        // Reset player state (will set spectator mode)
        resetPlayer(player);

        // Set up spectator scoreboard
        scoreboardManager.setupScoreboard(player); // Ensure this handles spectators

        MessageUtils.sendMessage(player, plugin.getPrefix() + "&aYou are now spectating the game!");
        broadcastMessage("&e" + player.getName() + " &7is now spectating.");

        return true;
    }

    // Method to clear fire
    private void clearAllFireInArena() {
        World world = Bukkit.getWorld(arena.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("[Game " + gameId + "] Cannot clear fire: World '" + arena.getWorldName() + "' not found.");
            return;
        }

        plugin.getLogger().info("[Game " + gameId + "] Attempting to clear fire in arena: " + arena.getName());
        int fireBlocksCleared = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            for (int x = 0; x < 16; x++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) { // Iterate through full world height in chunk
                    for (int z = 0; z < 16; z++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block.getType() == Material.FIRE) {
                            block.setType(Material.AIR);
                            fireBlocksCleared++;
                        }
                    }
                }
            }
        }
        if (fireBlocksCleared > 0) {
            plugin.getLogger().info("[Game " + gameId + "] Cleared " + fireBlocksCleared + " fire blocks from arena: " + arena.getName());
        }
    }

    private void downgradeTools(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;

            Material currentType = item.getType();
            Material downgradedType = null;

            // Determine downgrade path for pickaxes
            if (currentType == Material.DIAMOND_PICKAXE) downgradedType = Material.IRON_PICKAXE;
            else if (currentType == Material.IRON_PICKAXE) downgradedType = Material.STONE_PICKAXE;
            else if (currentType == Material.STONE_PICKAXE) downgradedType = Material.WOODEN_PICKAXE;
            // For WOODEN_PICKAXE, it remains wooden (no further downgrade by material)

            // Determine downgrade path for axes
            else if (currentType == Material.DIAMOND_AXE) downgradedType = Material.IRON_AXE;
            else if (currentType == Material.IRON_AXE) downgradedType = Material.STONE_AXE;
            else if (currentType == Material.STONE_AXE) downgradedType = Material.WOODEN_AXE;
            // For WOODEN_AXE, it remains wooden

            if (downgradedType != null) {
                ItemStack downgradedItem = new ItemStack(downgradedType, 1);
                // Attempt to copy relevant ItemMeta (like unbreakability, lore) if needed.
                // For now, relying on the shop to set these properties when tools are acquired/upgraded.
                // The name might change if it was specific (e.g. "Diamond Pickaxe"), 
                // but the lore "Keep on Death (may downgrade)" should ideally be preserved or re-added.
                // For simplicity, we create a new basic item of the downgraded type.
                // The shop adds lore; if we want to keep it, we need to copy meta.
                if (item.hasItemMeta()) {
                    ItemMeta oldMeta = item.getItemMeta();
                    ItemMeta newMeta = downgradedItem.getItemMeta();
                    if (newMeta != null && oldMeta != null) {
                        if (oldMeta.hasDisplayName()) {
                            // Update display name to reflect new tier
                            String oldName = oldMeta.getDisplayName();
                            if (oldName.contains("Diamond")) newMeta.setDisplayName(oldName.replace("Diamond", ChatColor.GRAY + "Iron")); // Example, needs proper color handling
                            else if (oldName.contains("Iron")) newMeta.setDisplayName(oldName.replace("Iron", ChatColor.GRAY + "Stone"));
                            else if (oldName.contains("Stone")) newMeta.setDisplayName(oldName.replace("Stone", ChatColor.GRAY + "Wood"));
                            else newMeta.setDisplayName(ChatColor.WHITE + downgradedType.name().toLowerCase().replace("_", " ")); // Fallback name
                        } else {
                             newMeta.setDisplayName(ChatColor.WHITE + downgradedType.name().toLowerCase().replace("_", " "));
                        }
                        if (oldMeta.hasLore()) {
                            newMeta.setLore(oldMeta.getLore()); // Preserve lore like "Keep on Death"
                        }
                        if (oldMeta.isUnbreakable()) {
                            newMeta.setUnbreakable(true);
                            newMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
                        }
                        downgradedItem.setItemMeta(newMeta);
                    }
                }
                contents[i] = downgradedItem;
            }
        }
        player.getInventory().setContents(contents);
    }

    // Add method to start the slow heal task
    private void startSlowHealTask() {
        if (slowHealTask != null) {
            slowHealTask.cancel();
        }
        
        slowHealTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.RUNNING) {
                    cancel(); // Stop task if game isn't running
                    return;
                }
                
                for (UUID playerId : players) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && !isSpectator(player) && player.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                        // double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(); // Old way
                        double maxHealth = player.getMaxHealth(); // Use getMaxHealth() for broader compatibility
                        if (player.getHealth() < maxHealth) {
                            // Heal by 1 health point (half a heart)
                            player.setHealth(Math.min(player.getHealth() + 1.0, maxHealth));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // Run every 10 seconds (200 ticks)
        plugin.getLogger().info("[Game " + gameId + "] Started slow heal task.");
    }
}


