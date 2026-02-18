package com.fretka46.fDailyRewards.Utils;

import com.fretka46.fDailyRewards.FDailyRewards;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Integration utility for PlaceholderAPI support.
 * This class provides helper methods to work with PlaceholderAPI placeholders.
 */
public class PlaceholderAPIIntegration {

    private static boolean isPlaceholderAPIAvailable = false;

    /**
     * Check if PlaceholderAPI is installed and enabled on the server.
     * This should be called during plugin initialization.
     */
    public static void initialize() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            isPlaceholderAPIAvailable = true;
            Log.info("PlaceholderAPI found! Placeholder support enabled.");
        } else {
            Log.info("PlaceholderAPI not found. Placeholder support disabled.");
        }
    }

    /**
     * Check if PlaceholderAPI is available.
     * @return true if PlaceholderAPI is installed and enabled
     */
    public static boolean isAvailable() {
        return isPlaceholderAPIAvailable;
    }

    /**
     * Parse placeholders in a message for a specific player.
     * If PlaceholderAPI is not available, returns the original message.
     * 
     * @param player The player to parse placeholders for
     * @param message The message containing placeholders
     * @return The message with placeholders replaced
     */
    public static String parsePlaceholders(Player player, String message) {
        if (!isPlaceholderAPIAvailable) {
            return message;
        }
        
        // Import statement would be here once dependencies are resolved:
        // import me.clip.placeholderapi.PlaceholderAPI;
        // return PlaceholderAPI.setPlaceholders(player, message);
        
        // For now, we'll use reflection to avoid compilation errors
        // when PlaceholderAPI is not available
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method method = papiClass.getMethod("setPlaceholders", Player.class, String.class);
            return (String) method.invoke(null, player, message);
        } catch (Exception e) {
            Log.warning("Failed to parse PlaceholderAPI placeholders: " + e.getMessage());
            return message;
        }
    }
}
