package org.bcnlab.beaconLabsBW.listeners;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.arena.model.Arena;
import org.bcnlab.beaconLabsBW.arena.model.SerializableLocation;
import org.bcnlab.beaconLabsBW.arena.model.TeamData;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameMode;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bcnlab.beaconLabsBW.shop.ShopItem;
import org.bcnlab.beaconLabsBW.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.entity.Fireball;
import org.bukkit.Sound;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles player-related events for BedWars
 */
public class PlayerListener implements Listener {
    
    private final BeaconLabsBW plugin;
    
    public PlayerListener(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
      @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Always try to join the player to a game automatically after a short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Check if player is already in a game (maybe rejoined)
            Game existingGame = plugin.getGameManager().getPlayerGame(player);
            if (existingGame == null) {
                // Not in a game, try to join one
                plugin.getGameManager().joinGame(player);
            }
        }, 10L);
    }
      @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clear any active cooldown display
        if (plugin.getGameManager().getPlayerGame(player) != null && 
            plugin.getGameManager().getPlayerGame(player).getGameMode() == GameMode.ULTIMATES) {
            plugin.getUltimatesManager().clearCooldownDisplay(player);
        }
        
        // Remove from any games
        plugin.getGameManager().removePlayerFromGame(player);
        
        // Exit edit mode if they were editing
        if (plugin.getArenaManager().isEditing(player)) {
            plugin.getArenaManager().stopEditing(player);
        }
    }
      @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            // Clear any active cooldown display
            if (game.getGameMode() == GameMode.ULTIMATES) {
                plugin.getUltimatesManager().clearCooldownDisplay(player);
            }
            
            // Clear drops in the game
            event.getDrops().clear();
            
            // No death message in the game
            event.setDeathMessage(null);
            
            // Handle game-specific death logic
            game.handlePlayerDeath(player, killer);
            
            // Skip death screen
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.spigot().respawn(), 2L);
        }
    }    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null) {
            // Clear any active cooldown display on respawn
            if (game.getGameMode() == GameMode.ULTIMATES) {
                plugin.getUltimatesManager().clearCooldownDisplay(player);
            }
            
            if (game.isSpectator(player)) {
                // Respawn at spectator spawn
                SerializableLocation spectatorLoc = game.getArena().getSpectatorSpawn();
                if (spectatorLoc != null) {
                    Location location = spectatorLoc.toBukkitLocation();
                    if (location != null) {
                        event.setRespawnLocation(location);
                    }
                }
            } else {
                // Respawn at team spawn if bed exists
                String team = game.getPlayerTeam(player);
                if (team != null) {
                    TeamData teamData = game.getArena().getTeam(team);
                    if (teamData != null && teamData.getSpawnLocation() != null) {
                        Location spawnLoc = teamData.getSpawnLocation().toBukkitLocation();
                        if (spawnLoc != null) {
                            event.setRespawnLocation(spawnLoc);
                        }
                    }
                }
                
                // Schedule a task to fix armor durability after respawn
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    org.bcnlab.beaconLabsBW.utils.ArmorHandler.fixPlayerArmor(player);
                }, 5L); // Delay of 5 ticks (0.25 seconds) to ensure armor is equipped first
            }
        }
    }
      @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        ItemStack item = event.getItem();
        
        // Check for interacting with beds 
        if (block != null && block.getType().name().contains("BED")) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game != null && game.getState() == GameState.RUNNING) {
                // Prevent setting spawn point with right-click
                if (action == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    // Optionally send a message:
                    // MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou cannot set your spawn point on game beds.");
                    return; // Stop processing for right-click
                }
                
                // Remove the LEFT_CLICK_BLOCK handling from here. It will be moved to BlockBreakEvent.
                /* 
                try {
                    // Handle bed breaking logic in game
                    // handleBedBreak(player, block, game); // MOVED
                } catch (Exception e) {
                    // ... (error handling) ...
                    event.setCancelled(true);
                    if (action == Action.RIGHT_CLICK_BLOCK) {
                         game.resetBedAtLocation(block.getLocation());
                     }
                }
                return; // Bed interaction handled (or cancelled)
                */
            }
        }
        
        // Handle Dream Defender (Iron Golem) spawn
        if ((action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) && 
            event.getHand() == EquipmentSlot.HAND) {
            
            if (item != null && item.getType() == Material.VILLAGER_SPAWN_EGG) {
                plugin.getLogger().info("[DreamDefender] Player " + player.getName() + " used Villager Spawn Egg.");
                // Check if the item is a Dream Defender
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && 
                    meta.getDisplayName().contains("Dream Defender")) {
                    plugin.getLogger().info("[DreamDefender] Item is named 'Dream Defender'. Proceeding...");
                    
                    event.setCancelled(true); // Cancel default spawn egg behavior
                    
                    // Check if player is in a game
                    Game game = plugin.getGameManager().getPlayerGame(player);
                    if (game != null && game.getState() == GameState.RUNNING) {
                        plugin.getLogger().info("[DreamDefender] Player is in running game " + game.getGameId() + ". Consuming item.");
                        // Consume the spawn egg
                        if (item.getAmount() > 1) {
                            item.setAmount(item.getAmount() - 1);
                        } else {
                            player.getInventory().setItemInMainHand(null);
                        }
                        
                        Location spawnLoc = player.getLocation();
                        plugin.getLogger().info("[DreamDefender] Attempting to spawn Iron Golem at: " + spawnLoc);
                        // Spawn an Iron Golem
                        try {
                            IronGolem golem = (IronGolem) player.getWorld().spawn(spawnLoc, IronGolem.class);
                            plugin.getLogger().info("[DreamDefender] Golem spawned successfully! Entity ID: " + golem.getEntityId());
                            golem.setCustomNameVisible(true);
                            golem.setPersistent(false); // Don't persist after world unload
                            
                            // Set metadata for team identification
                            String team = game.getPlayerTeam(player);
                            if (team != null) {
                                plugin.getLogger().info("[DreamDefender] Setting metadata: team=" + team + ", owner=" + player.getUniqueId() + ", game=" + game.getGameId());
                                String teamColor = game.getArena().getTeam(team).getColor();
                                ChatColor chatColor = MessageUtils.getChatColorFromString(teamColor);
                                golem.setCustomName(chatColor + "Dream Defender" + ChatColor.GRAY + " [2:00]");
                                golem.setMetadata("team", new FixedMetadataValue(plugin, team));
                                golem.setMetadata("owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                                golem.setMetadata("game_id", new FixedMetadataValue(plugin, game.getGameId()));
                                golem.setMetadata("spawn_time", new FixedMetadataValue(plugin, System.currentTimeMillis()));
                            } else {
                                plugin.getLogger().warning("[DreamDefender] Player " + player.getName() + " has no team, cannot set golem team metadata.");
                            }
                            
                            // Start timer task for despawning after 2 minutes
                            final int[] timeLeft = {120}; // 120 seconds = 2 minutes
                            final int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                                // Check if golem still exists
                                if (!golem.isValid() || golem.isDead()) {
                                    Bukkit.getScheduler().cancelTask(golem.getMetadata("timer_task").get(0).asInt());
                                    return;
                                }
                                
                                timeLeft[0]--;
                                
                                if (timeLeft[0] <= 0) {
                                    // Time's up, remove the golem
                                    golem.remove();
                                    Bukkit.getScheduler().cancelTask(golem.getMetadata("timer_task").get(0).asInt());
                                    return;
                                }
                                
                                // Update name to show time left
                                int minutes = timeLeft[0] / 60;
                                int seconds = timeLeft[0] % 60;
                                String timeString = String.format("%d:%02d", minutes, seconds);
                                
                                String teamColor = game.getArena().getTeam(team).getColor();
                                ChatColor chatColor = MessageUtils.getChatColorFromString(teamColor);
                                golem.setCustomName(chatColor + "Dream Defender" + ChatColor.GRAY + " [" + timeString + "]");
                                
                            }, 20L, 20L).getTaskId(); // Run every second
                            
                            // Store task ID for cleanup
                            golem.setMetadata("timer_task", new FixedMetadataValue(plugin, taskId));
                            
                            // Play spawn sound
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 1.0f);
                            MessageUtils.sendMessage(player, plugin.getPrefix() + "&aYou spawned a Dream Defender!");
                        } catch (Exception e) {
                            plugin.getLogger().severe("[DreamDefender] FAILED to spawn Iron Golem: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                         plugin.getLogger().info("[DreamDefender] Player not in running game or game is null.");
                    }
                } else {
                     plugin.getLogger().info("[DreamDefender] Item meta/display name does not match 'Dream Defender'. Name: " + (meta != null ? meta.getDisplayName() : "null"));
                }
            }
        }
        
        // Handle Fireball Throw
        if (item != null && item.getType() == Material.FIRE_CHARGE && 
            (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            
            // Check if it's the specific shop item (optional, could check lore/name)
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains("Fireball")) {
                Game game = plugin.getGameManager().getPlayerGame(player);
                if (game != null && game.getState() == GameState.RUNNING) {
                    event.setCancelled(true); // Prevent default Fire Charge behavior

                    // Consume item
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(null);
                    }

                    // Launch Fireball
                    Fireball fireball = player.launchProjectile(Fireball.class);
                    fireball.setShooter(player); // Set shooter for tracking/damage attribution
                    fireball.setIsIncendiary(false); // Prevent it from starting fires randomly
                    fireball.setYield(2.0F); // Explosion power (TNT is 4.0F)
                    // Store metadata to identify it later
                    fireball.setMetadata("bw_fireball", new FixedMetadataValue(plugin, true));
                    player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
                }
            }
        }
        
        // Handle shop keeper interaction
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            if (item != null && item.getType() == Material.EMERALD) {
                // Check if player is in a game
                Game game = plugin.getGameManager().getPlayerGame(player);
                if (game != null && game.getState() == GameState.RUNNING) {
                    // Open the shop
                    plugin.getShopManager().openShop(player);
                    event.setCancelled(true);
                }
            }
        }
    }    // Track last interaction time to prevent spam clicks on beds
    private final java.util.Map<java.util.UUID, Long> lastBedInteraction = new java.util.HashMap<>();
    private static final long BED_INTERACTION_COOLDOWN = 500; // 0.5 seconds
    private void handleBedBreak(Player player, Block bed, Game game) {
        if (bed == null || game == null) return;
        
        // Anti-spam check
        long now = System.currentTimeMillis();
        long lastTime = lastBedInteraction.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastTime < BED_INTERACTION_COOLDOWN) {
            return; // Silently ignore rapid clicks
        }
        lastBedInteraction.put(player.getUniqueId(), now);
        
        // Get player's team
        String playerTeam = game.getPlayerTeam(player);
        if (playerTeam == null) return;
        
        // Check whose bed this is
        for (String teamName : game.getArena().getTeams().keySet()) {
            TeamData team = game.getArena().getTeam(teamName);
            if (team == null || team.getBedLocation() == null) continue;
            
            Location bedLoc = team.getBedLocation().toBukkitLocation();
            if (bedLoc != null && isSameBed(bed.getLocation(), bedLoc)) {
                // Can't break your own bed
                if (teamName.equals(playerTeam)) {
                    MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou cannot break your own bed!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return;
                }
                
                // Find and destroy the entire bed (head and foot parts) to prevent bed drops
                // This fixes the issue of beds sometimes dropping as items
                destroyBedParts(bed);
                
                // Handle bed break in the game
                game.handleBedBreak(teamName, player);
                return;
            }
        }
    }
    
    /**
     * Destroy all parts of a bed to prevent item drops
     * 
     * @param bedBlock Any part of the bed
     */
    private void destroyBedParts(Block bedBlock) {
        if (bedBlock.getType().toString().contains("BED")) {
            // Get the block data to find connected bed parts
            org.bukkit.block.data.type.Bed bed = (org.bukkit.block.data.type.Bed) bedBlock.getBlockData();
            
            // Get the facing direction of the bed
            org.bukkit.block.BlockFace facing = bed.getFacing();
            
            // Get both parts of the bed
            Block headBlock = bed.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD ? 
                              bedBlock : bedBlock.getRelative(facing);
                              
            Block footBlock = bed.getPart() == org.bukkit.block.data.type.Bed.Part.FOOT ? 
                              bedBlock : bedBlock.getRelative(facing.getOppositeFace());
            
            // Set both parts to AIR to avoid item drops
            headBlock.setType(Material.AIR, false);
            footBlock.setType(Material.AIR, false);
        }
    }
    
    private boolean isSameBed(Location loc1, Location loc2) {
        if (!loc1.getWorld().equals(loc2.getWorld())) return false;
        
        // Check if the two locations are within 1 block of each other (bed is two blocks)
        return Math.abs(loc1.getBlockX() - loc2.getBlockX()) <= 1 && 
               Math.abs(loc1.getBlockY() - loc2.getBlockY()) <= 1 && 
               Math.abs(loc1.getBlockZ() - loc2.getBlockZ()) <= 1;
    }
      @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            
            if (event.getView().getTitle().startsWith("§8BedWars Shop:")) {
                // Handle shop interaction
                event.setCancelled(true);
                plugin.getShopManager().handlePurchase(player, event.getRawSlot());
                return;
            }
            
            // Game-specific inventory protections
            if (game != null && game.getState() == GameState.RUNNING) {
                // Prevent armor removal
                if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
                    event.setCancelled(true);
                    return;
                }
                
                // Prevent placing permanent tools in other inventories
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || 
                    event.getAction() == InventoryAction.PLACE_ALL ||
                    event.getAction() == InventoryAction.PLACE_ONE || 
                    event.getAction() == InventoryAction.PLACE_SOME) {
                    
                    ItemStack currentItem = event.getCurrentItem();
                    if (currentItem != null && (isArmor(currentItem.getType()) || isPermanentTool(currentItem))) {
                        event.setCancelled(true);
                        MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou cannot move permanent items!");
                        return;
                    }
                }
            }
        }
    }
      @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null) {
            ItemStack item = event.getItemDrop().getItemStack();
            
            // Don't allow dropping armor
            if (isArmor(item.getType())) {
                event.setCancelled(true);
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou cannot drop armor!");
                return;
            }
            
            // Don't allow dropping permanent tools
            if (isPermanentTool(item)) {
                event.setCancelled(true);
                MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou cannot drop permanent tools!");
                return;
            }
        }
    }
    
    private boolean isArmor(Material material) {
        return material.name().contains("HELMET") ||
               material.name().contains("CHESTPLATE") ||
               material.name().contains("LEGGINGS") ||
               material.name().contains("BOOTS");
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
               type.name().contains("AXE") ||
               type == Material.WOODEN_SWORD ||
               type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD ||
               type == Material.DIAMOND_SWORD;
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            
            if (game != null) {
                // Disable damage in lobby
                if (game.getState() != GameState.RUNNING) {
                    event.setCancelled(true);
                    return;
                }
                
                // Prevent spectators from taking damage
                if (game.isSpectator(player)) {
                    event.setCancelled(true);
                    return;
                }
                
                // Prevent fall damage from excessive heights
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL && 
                    event.getDamage() > 10) {
                    event.setDamage(10); // Cap fall damage
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player defender) {
            Game game = plugin.getGameManager().getPlayerGame(attacker);
            
            if (game != null && game == plugin.getGameManager().getPlayerGame(defender)) {
                // Disable PvP in non-running state
                if (game.getState() != GameState.RUNNING) {
                    event.setCancelled(true);
                    return;
                }
                
                // Disable damage to teammates
                String attackerTeam = game.getPlayerTeam(attacker);
                String defenderTeam = game.getPlayerTeam(defender);
                
                if (attackerTeam != null && attackerTeam.equals(defenderTeam)) {
                    event.setCancelled(true);
                    return;
                }
                
                // Spectators can't attack or be attacked
                if (game.isSpectator(attacker) || game.isSpectator(defender)) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null) {
            // Only allow certain commands during a game
            String message = event.getMessage().toLowerCase();
            String[] args = message.split(" ");
            String cmd = args[0]; // The base command, e.g., "/bw"

            // Allow admins to use any command
            if (player.hasPermission("bedwars.admin")) {
                return;
            }
            
            // Allow basic /bw commands like /bw leave
            if (cmd.equalsIgnoreCase("/bw") || 
                cmd.equalsIgnoreCase("/labsbw") || 
                cmd.equalsIgnoreCase("/bedwars")) {

                if (args.length > 1) {
                    String subCommand = args[1];
                    // Check specific sub-commands for permissions
                    if (subCommand.equalsIgnoreCase("shop")) {
                        if (!player.hasPermission("bedwars.shop.use")) {
                            event.setCancelled(true);
                            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou do not have permission to use the shop command.");
                            return;
                        }
                    } else if (subCommand.equalsIgnoreCase("upgrades")) {
                        if (!player.hasPermission("bedwars.upgrades.use")) {
                            event.setCancelled(true);
                            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou do not have permission to use the upgrades command.");
                            return;
                        }
                    }
                    // Allow other /bw subcommands like /bw leave
                    return; 
                } else {
                    // Allow base /bw command itself (if it does anything)
                    return;
                }
            }
            
            // Block other commands
            event.setCancelled(true);
            MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou cannot use commands during a game! Use &e/bw leave &cto quit.");
        }
    }
    
    // Check for players falling into the void
    private org.bukkit.scheduler.BukkitTask voidCheckTask;
    
    /**
     * Start the void check task
     */
    public void startVoidCheck() {
        if (voidCheckTask != null) {
            voidCheckTask.cancel();
        }
        
        voidCheckTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Game game : plugin.getGameManager().getActiveGames().values()) {
                if (game.getState() != GameState.RUNNING) continue;
                
                // Check each player
                for (UUID playerId : game.getPlayers()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null || game.isSpectator(player)) continue;
                    
                    // Check if player is below void level
                    if (player.getLocation().getY() < 0) {
                        // Teleport to a safe location temporarily to prevent actual death event
                        SerializableLocation specLoc = game.getArena().getSpectatorSpawn();
                        if (specLoc != null) {
                            Location safeLocation = specLoc.toBukkitLocation();
                            if (safeLocation != null) {
                                player.teleport(safeLocation);
                            }
                        }
                        
                        // Handle as a void death
                        Player killer = player.getKiller(); // Can be null
                        game.handlePlayerDeath(player, killer);
                    }
                }
            }
        }, 10L, 10L); // Check every half second
    }
    
    /**
     * Stop the void check task
     */
    public void stopVoidCheck() {
        if (voidCheckTask != null) {
            voidCheckTask.cancel();
            voidCheckTask = null;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST) // High priority to ensure we handle it
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if it's a bed
        if (block.getType().name().contains("BED")) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game != null && game.getState() == GameState.RUNNING) {
                // Prevent bed item drop
                event.setDropItems(false);

                // Call the existing handleBedBreak logic 
                // (which needs to be accessible or moved here)
                handleBedBreak(player, block, game);
                
                // Note: handleBedBreak calls destroyBedParts, which sets to AIR.
                // This should be okay even after BlockBreakEvent, but ensure no conflicts.
                // If handleBedBreak is private, its logic needs to be moved here or made public/accessible.
            } else {
                // If not in a running game (e.g. lobby, post-game), cancel bed breaking
                event.setCancelled(true);
            }
        }
        // Optional: Prevent breaking other non-placed blocks during the game?
        // else {
        //     Game game = plugin.getGameManager().getPlayerGame(player);
        //     if (game != null && game.getState() == GameState.RUNNING && !game.isPlacedBlock(block)) {
        //         event.setCancelled(true);
        //         MessageUtils.sendMessage(player, plugin.getPrefix() + "&cYou can only break blocks placed by players!");
        //     }
        // }
    }
}
