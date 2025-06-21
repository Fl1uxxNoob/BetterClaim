package net.fliuxx.betterClaim.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for message formatting, colorization, and sending
 */
public class MessageUtils {
    
    // Pattern for hex color codes (#RRGGBB)
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    /**
     * Colorize a string with both legacy and hex color codes
     * @param message The message to colorize
     * @return Colorized string
     */
    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        
        // Handle hex colors first
        message = colorizeHex(message);
        
        // Handle legacy color codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Colorize a list of strings
     * @param messages The list of messages to colorize
     * @return List of colorized strings
     */
    public static List<String> colorize(List<String> messages) {
        if (messages == null) {
            return List.of();
        }
        
        return messages.stream()
                .map(MessageUtils::colorize)
                .collect(Collectors.toList());
    }
    
    /**
     * Handle hex color codes in a string
     * @param message The message containing hex codes
     * @return Message with hex colors applied
     */
    private static String colorizeHex(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            // Convert hex to ChatColor format for older versions
            matcher.appendReplacement(buffer, "§x§" + 
                hexColor.charAt(0) + "§" + hexColor.charAt(1) + "§" +
                hexColor.charAt(2) + "§" + hexColor.charAt(3) + "§" +
                hexColor.charAt(4) + "§" + hexColor.charAt(5));
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    /**
     * Strip all color codes from a message
     * @param message The message to strip colors from
     * @return Plain text message
     */
    public static String stripColors(String message) {
        if (message == null) {
            return "";
        }
        
        // Strip hex colors
        message = HEX_PATTERN.matcher(message).replaceAll("");
        
        // Strip legacy colors
        return ChatColor.stripColor(message);
    }
    
    /**
     * Send a colorized message to a command sender
     * @param sender The command sender
     * @param message The message to send
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        
        sender.sendMessage(colorize(message));
    }
    
    /**
     * Send multiple colorized messages to a command sender
     * @param sender The command sender
     * @param messages The messages to send
     */
    public static void sendMessages(CommandSender sender, List<String> messages) {
        if (sender == null || messages == null) {
            return;
        }
        
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }
    
    /**
     * Send a colorized message to a player using Adventure API
     * @param player The player to send the message to
     * @param message The message to send
     */
    public static void sendAdventureMessage(Player player, String message) {
        if (player == null || message == null) {
            return;
        }
        
        Component component = LegacyComponentSerializer.legacySection().deserialize(colorize(message));
        player.sendMessage(component);
    }
    
    /**
     * Create a centered message for chat
     * @param message The message to center
     * @return Centered message with appropriate spacing
     */
    public static String centerMessage(String message) {
        if (message == null) {
            return "";
        }
        
        String stripped = stripColors(message);
        int messageLength = stripped.length();
        int maxLength = 60; // Approximate chat width
        
        if (messageLength >= maxLength) {
            return message;
        }
        
        int spaces = (maxLength - messageLength) / 2;
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < spaces; i++) {
            builder.append(" ");
        }
        
        builder.append(message);
        return builder.toString();
    }
    
    /**
     * Create a separator line for chat messages
     * @param character The character to use for the separator
     * @param length The length of the separator
     * @param color The color code for the separator
     * @return Formatted separator line
     */
    public static String createSeparator(char character, int length, String color) {
        StringBuilder builder = new StringBuilder();
        
        if (color != null && !color.isEmpty()) {
            builder.append(color);
        }
        
        for (int i = 0; i < length; i++) {
            builder.append(character);
        }
        
        return colorize(builder.toString());
    }
    
    /**
     * Create a default separator line
     * @return Default separator line
     */
    public static String createSeparator() {
        return createSeparator('-', 50, "&7");
    }
    
    /**
     * Format a progress bar for chat display
     * @param current Current value
     * @param max Maximum value
     * @param length Length of the progress bar
     * @param completedColor Color for completed portion
     * @param remainingColor Color for remaining portion
     * @return Formatted progress bar
     */
    public static String createProgressBar(int current, int max, int length, String completedColor, String remainingColor) {
        if (max <= 0) {
            return "";
        }
        
        double percentage = Math.min(1.0, (double) current / max);
        int completed = (int) (length * percentage);
        int remaining = length - completed;
        
        StringBuilder builder = new StringBuilder();
        
        // Completed portion
        if (completedColor != null && !completedColor.isEmpty()) {
            builder.append(completedColor);
        }
        for (int i = 0; i < completed; i++) {
            builder.append("█");
        }
        
        // Remaining portion
        if (remainingColor != null && !remainingColor.isEmpty()) {
            builder.append(remainingColor);
        }
        for (int i = 0; i < remaining; i++) {
            builder.append("█");
        }
        
        return colorize(builder.toString());
    }
    
    /**
     * Format a number with appropriate units (K, M, B, etc.)
     * @param number The number to format
     * @return Formatted number string
     */
    public static String formatNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            return String.format("%.1fK", number / 1000.0);
        } else if (number < 1000000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else {
            return String.format("%.1fB", number / 1000000000.0);
        }
    }
    
    /**
     * Format a time duration in milliseconds to a readable string
     * @param milliseconds The duration in milliseconds
     * @return Formatted time string
     */
    public static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Replace placeholders in a message with actual values
     * @param message The message with placeholders
     * @param placeholder The placeholder to replace (without {})
     * @param value The value to replace it with
     * @return Message with placeholder replaced
     */
    public static String replacePlaceholder(String message, String placeholder, String value) {
        if (message == null || placeholder == null || value == null) {
            return message;
        }
        
        return message.replace("{" + placeholder + "}", value);
    }
    
    /**
     * Check if a string contains color codes
     * @param message The message to check
     * @return true if the message contains color codes
     */
    public static boolean hasColors(String message) {
        if (message == null) {
            return false;
        }
        
        return message.contains("&") || HEX_PATTERN.matcher(message).find();
    }
    
    /**
     * Get the raw text length of a message (without color codes)
     * @param message The message to measure
     * @return Length of the message without color codes
     */
    public static int getRawLength(String message) {
        return stripColors(message).length();
    }
}
