package net.fliuxx.betterClaim.managers;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.models.ClaimFlag;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {
    
    private final BetterClaim plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    
    public ConfigManager(BetterClaim plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        // Load main config
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load messages config
        loadMessagesConfig();
    }
    
    private void loadMessagesConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public void saveMessagesConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }
    
    // Config getters
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }
    
    public String getDatabaseFilename() {
        return config.getString("database.filename", "claims.db");
    }
    
    public int getMaxChunksPerClaim() {
        return config.getInt("claim.max-chunks-per-claim", 100);
    }
    
    public int getMaxClaimsPerPlayer() {
        return config.getInt("claim.max-claims-per-player", 1);
    }
    
    public boolean isRequireAdjacent() {
        return config.getBoolean("claim.require-adjacent", true);
    }
    
    public boolean isShowBorders() {
        return config.getBoolean("claim.show-borders", true);
    }
    
    public String getBorderParticle() {
        return config.getString("claim.border-particle", "REDSTONE");
    }
    
    public int getAutoSaveInterval() {
        return config.getInt("claim.auto-save-interval", 5);
    }
    
    public Map<ClaimFlag, Boolean> getDefaultFlags() {
        Map<ClaimFlag, Boolean> flags = new HashMap<>();
        
        for (ClaimFlag flag : ClaimFlag.values()) {
            String path = "default-flags." + flag.name().toLowerCase().replace('_', '-');
            flags.put(flag, config.getBoolean(path, flag.getDefaultValue()));
        }
        
        return flags;
    }
    
    public List<String> getTrustLevelPermissions(String trustLevel) {
        return config.getStringList("trust-levels." + trustLevel);
    }
    
    public String getGUITitle(String guiType) {
        return config.getString("gui.titles." + guiType, "&6&lGUI");
    }
    
    public int getCacheSize() {
        return config.getInt("performance.cache-size", 1000);
    }
    
    public int getCacheExpiration() {
        return config.getInt("performance.cache-expiration", 30);
    }
    
    public boolean isAsyncSaves() {
        return config.getBoolean("performance.async-saves", true);
    }
    
    // Messages getters
    public String getMessage(String path) {
        return messages.getString(path, "&cMessage not found: " + path);
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }
    
    public List<String> getMessageList(String path) {
        return messages.getStringList(path);
    }
    
    public String getPrefix() {
        return getMessage("prefix");
    }
}
