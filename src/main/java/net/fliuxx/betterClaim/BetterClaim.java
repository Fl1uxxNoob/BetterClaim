package net.fliuxx.betterClaim;

import net.fliuxx.betterClaim.commands.ClaimCommand;
import net.fliuxx.betterClaim.database.DatabaseManager;
import net.fliuxx.betterClaim.listeners.ClaimProtectionListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public final class BetterClaim extends JavaPlugin {

    private static BetterClaim instance;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        // Configurazione
        saveDefaultConfig();

        try {
            // Database
            databaseManager = new DatabaseManager(this);

            // Comandi
            var commandExecutor = new ClaimCommand(this);
            var claimCommand = getCommand("claim");
            if (claimCommand != null) {
                claimCommand.setExecutor(commandExecutor);
                claimCommand.setTabCompleter(commandExecutor);
            } else {
                getLogger().severe("Impossibile registrare il comando 'claim'. Verifica il plugin.yml!");
            }

            // Listener
            getServer().getPluginManager().registerEvents(new ClaimProtectionListener(this), this);

            getLogger().info("BetterClaim è stato abilitato con successo!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Errore durante l'inizializzazione del plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Chiudere la connessione al database
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }

        getLogger().info("BetterClaim è stato disabilitato con successo!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public static BetterClaim getInstance() {
        return instance;
    }

    @Override
    public void saveDefaultConfig() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveResource("config.yml", false);
        }
    }
}