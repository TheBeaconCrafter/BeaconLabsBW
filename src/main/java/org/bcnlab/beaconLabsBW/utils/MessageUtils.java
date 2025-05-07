package org.bcnlab.beaconLabsBW.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utility class for handling messages and color codes
 */
public class MessageUtils {

    /**
     * Translate color codes in a message
     * 
     * @param message The message with color codes
     * @return The colored message
     */
    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Send a colored message to a player
     * 
     * @param player The player to send the message to
     * @param message The message to send
     */
    public static void sendMessage(Player player, String message) {
        if (player == null || message == null) return;
        player.sendMessage(colorize(message));
    }
    
    /**
     * Send a colored message to a command sender
     * 
     * @param sender The command sender to send the message to
     * @param message The message to send
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null) return;
        sender.sendMessage(colorize(message));
    }
    
    /**
     * Send a colored message to a collection of players by UUID
     * 
     * @param playerIds Collection of player UUIDs
     * @param message The message to send
     */
    public static void sendMessage(java.util.Set<java.util.UUID> playerIds, String message) {
        if (playerIds == null || message == null) return;
        for (java.util.UUID playerId : playerIds) {
            Player player = org.bukkit.Bukkit.getPlayer(playerId);
            if (player != null) {
                sendMessage(player, message);
            }
        }
    }
    
    /**
     * Get ChatColor from color name string
     * 
     * @param colorName The color name string
     * @return The ChatColor corresponding to the name
     */
    public static ChatColor getChatColorFromString(String colorName) {
        if (colorName == null) return ChatColor.WHITE;
        
        return switch (colorName.toUpperCase()) {
            case "RED" -> ChatColor.RED;
            case "BLUE" -> ChatColor.BLUE;
            case "GREEN" -> ChatColor.GREEN;
            case "YELLOW" -> ChatColor.YELLOW;
            case "AQUA" -> ChatColor.AQUA;
            case "WHITE" -> ChatColor.WHITE;
            case "PINK" -> ChatColor.LIGHT_PURPLE;
            case "GRAY" -> ChatColor.GRAY;
            default -> ChatColor.WHITE;
        };
    }
    
    /**
     * Get the ChatColor associated with a team color
     * 
     * @param teamName The name of the team
     * @return The ChatColor for that team
     */
    public static ChatColor getTeamChatColor(String teamName) {
        if (teamName == null) return ChatColor.WHITE;
        
        return switch (teamName.toUpperCase()) {
            case "RED" -> ChatColor.RED;
            case "BLUE" -> ChatColor.BLUE;
            case "GREEN" -> ChatColor.GREEN;
            case "YELLOW" -> ChatColor.YELLOW;
            case "AQUA" -> ChatColor.AQUA;
            case "WHITE" -> ChatColor.WHITE;
            case "PINK" -> ChatColor.LIGHT_PURPLE;
            case "GRAY" -> ChatColor.GRAY;
            default -> ChatColor.WHITE;
        };
    }
}
