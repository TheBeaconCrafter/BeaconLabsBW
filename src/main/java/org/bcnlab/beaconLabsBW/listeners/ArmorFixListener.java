package org.bcnlab.beaconLabsBW.listeners;

import org.bcnlab.beaconLabsBW.BeaconLabsBW;
import org.bcnlab.beaconLabsBW.game.Game;
import org.bcnlab.beaconLabsBW.game.GameState;
import org.bcnlab.beaconLabsBW.utils.ArmorHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener to fix armor durability on player respawn
 */
public class ArmorFixListener implements Listener {
    
    private final BeaconLabsBW plugin;
    
    public ArmorFixListener(BeaconLabsBW plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null && game.getState() == GameState.RUNNING && !game.isSpectator(player)) {
            // Schedule task to fix armor after the player respawns
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        ArmorHandler.fixPlayerArmor(player);
                    }
                }
            }.runTaskLater(plugin, 5L); // Run 5 ticks (0.25 seconds) after respawn
        }
    }
    
    /**
     * Start a recurring task to fix armor durability for all players
     */
    public void startArmorFixTask() {
        // Run every 30 seconds to fix all player armor in all games
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Game game : plugin.getGameManager().getActiveGames().values()) {
                if (game.getState() == GameState.RUNNING) {
                    game.getPlayers().forEach(playerId -> {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && !game.isSpectator(player)) {
                            ArmorHandler.fixPlayerArmor(player);
                        }
                    });
                }
            }
        }, 200L, 600L); // Initial delay 10s, then every 30s
    }
}
