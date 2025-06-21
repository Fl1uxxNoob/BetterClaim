package net.fliuxx.betterClaim;

import net.fliuxx.betterClaim.commands.ClaimAdminCommand;
import net.fliuxx.betterClaim.commands.ClaimCommand;
import net.fliuxx.betterClaim.listeners.ClaimProtectionListener;
import net.fliuxx.betterClaim.listeners.GUIListener;
import net.fliuxx.betterClaim.managers.ClaimManager;
import net.fliuxx.betterClaim.managers.ConfigManager;
import net.fliuxx.betterClaim.managers.DatabaseManager;
import net.fliuxx.betterClaim.managers.GUIManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class BetterClaim extends JavaPlugin {
    
    private static BetterClaim instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ClaimManager claimManager;
    private GUIManager guiManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Start auto-save task
        startAutoSaveTask();
        
        getLogger().info("BetterClaim has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Save all claims before shutdown
        if (claimManager != null) {
            claimManager.saveAllClaims();
        }
        
        // Close database connection
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        
        getLogger().info("BetterClaim has been disabled successfully!");
    }
    
    private void initializeManagers() {
        try {
            // Initialize configuration manager
            configManager = new ConfigManager(this);
            configManager.loadConfigs();
            
            // Initialize database manager
            databaseManager = new DatabaseManager(this);
            databaseManager.initializeDatabase();
            
            // Initialize claim manager
            claimManager = new ClaimManager(this);
            claimManager.loadClaims();
            
            // Initialize GUI manager
            guiManager = new GUIManager(this);
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize managers", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void registerCommands() {
        getCommand("claim").setExecutor(new ClaimCommand(this));
        getCommand("claimadmin").setExecutor(new ClaimAdminCommand(this));
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ClaimProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    }
    
    private void startAutoSaveTask() {
        int interval = configManager.getAutoSaveInterval() * 20 * 60; // Convert minutes to ticks
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (claimManager != null) {
                claimManager.saveAllClaims();
            }
        }, interval, interval);
    }
    
    public void reloadConfigs() {
        configManager.loadConfigs();
        claimManager.reloadClaims();
    }
    
    // Getters
    public static BetterClaim getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public ClaimManager getClaimManager() {
        return claimManager;
    }
    
    public GUIManager getGuiManager() {
        return guiManager;
    }
}
