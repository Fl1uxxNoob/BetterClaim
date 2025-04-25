package net.fliuxx.betterClaim.database;

import net.fliuxx.betterClaim.BetterClaim;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final BetterClaim plugin;
    private Connection connection;
    private final String dbFileName;

    public DatabaseManager(BetterClaim plugin) {
        this.plugin = plugin;
        this.dbFileName = plugin.getConfig().getString("database.filename", "claims.db");
        initialize();
    }

    private void initialize() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                plugin.getLogger().severe("Impossibile creare la cartella del plugin!");
                return;
            }

            String jdbcUrl = "jdbc:sqlite:" + new File(dataFolder, dbFileName);
            connection = DriverManager.getConnection(jdbcUrl);

            // Attivare i vincoli di integrità referenziale
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }

            // Creare tabella dei claim
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS claims (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        world TEXT,
                        chunkX INTEGER,
                        chunkZ INTEGER,
                        owner TEXT,
                        UNIQUE(world, chunkX, chunkZ)
                    )
                """);
            }

            // Creare tabella dei membri
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS claim_members (
                        claim_id INTEGER,
                        player TEXT,
                        FOREIGN KEY(claim_id) REFERENCES claims(id) ON DELETE CASCADE,
                        UNIQUE(claim_id, player)
                    )
                """);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante l'inizializzazione del database", e);
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la chiusura della connessione al database", e);
        }
    }

    public boolean createClaim(Player player, Chunk chunk) {
        try {
            // Verificare se il chunk è già stato claimato
            if (isChunkClaimed(chunk)) {
                return false;
            }

            // Verificare se il giocatore ha già raggiunto il limite massimo di claim
            int maxClaims = plugin.getConfig().getInt("settings.max-claims", 2);
            if (!player.hasPermission(plugin.getConfig().getString("permissions.claims-unlimited")) &&
                    getPlayerClaimCount(player.getName()) >= maxClaims) {
                return false;
            }

            String sql = "INSERT INTO claims (world, chunkX, chunkZ, owner) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, chunk.getWorld().getName());
                ps.setInt(2, chunk.getX());
                ps.setInt(3, chunk.getZ());
                ps.setString(4, player.getName());
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la creazione del claim", e);
            return false;
        }
    }

    public boolean removeClaim(Player player, Chunk chunk) {
        try {
            // Verificare se il giocatore è il proprietario
            String owner = getChunkOwner(chunk);
            if (owner == null) {
                return false;
            }

            if (!owner.equals(player.getName()) && !player.hasPermission(plugin.getConfig().getString("permissions.admin-bypass"))) {
                return false;
            }

            int claimId = getClaimId(chunk);
            if (claimId == -1) {
                return false;
            }

            // Rimuovere tutti i membri
            String memberSql = "DELETE FROM claim_members WHERE claim_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(memberSql)) {
                ps.setInt(1, claimId);
                ps.executeUpdate();
            }

            // Rimuovere il claim
            String claimSql = "DELETE FROM claims WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(claimSql)) {
                ps.setInt(1, claimId);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la rimozione del claim", e);
            return false;
        }
    }

    public boolean isChunkClaimed(Chunk chunk) {
        String sql = "SELECT id FROM claims WHERE world = ? AND chunkX = ? AND chunkZ = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chunk.getWorld().getName());
            ps.setInt(2, chunk.getX());
            ps.setInt(3, chunk.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la verifica del claim", e);
            return false;
        }
    }

    public String getChunkOwner(Chunk chunk) {
        String sql = "SELECT owner FROM claims WHERE world = ? AND chunkX = ? AND chunkZ = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chunk.getWorld().getName());
            ps.setInt(2, chunk.getX());
            ps.setInt(3, chunk.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("owner") : null;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il recupero del proprietario del claim", e);
            return null;
        }
    }

    public int getClaimId(Chunk chunk) {
        String sql = "SELECT id FROM claims WHERE world = ? AND chunkX = ? AND chunkZ = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chunk.getWorld().getName());
            ps.setInt(2, chunk.getX());
            ps.setInt(3, chunk.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il recupero dell'ID del claim", e);
            return -1;
        }
    }

    public int getPlayerClaimCount(String playerName) {
        String sql = "SELECT COUNT(*) AS count FROM claims WHERE owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("count") : 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il conteggio dei claim del giocatore", e);
            return 0;
        }
    }

    public boolean addMemberToClaim(int claimId, String playerName) {
        String sql = "INSERT INTO claim_members (claim_id, player) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, claimId);
            ps.setString(2, playerName);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                return false; // Il giocatore è già un membro
            }
            plugin.getLogger().log(Level.SEVERE, "Errore durante l'aggiunta di un membro al claim", e);
            return false;
        }
    }

    public boolean removeMemberFromClaim(int claimId, String playerName) {
        String sql = "DELETE FROM claim_members WHERE claim_id = ? AND player = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, claimId);
            ps.setString(2, playerName);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la rimozione di un membro dal claim", e);
            return false;
        }
    }

    public boolean isPlayerMemberOfClaim(int claimId, String playerName) {
        String sql = "SELECT player FROM claim_members WHERE claim_id = ? AND player = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, claimId);
            ps.setString(2, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la verifica di un membro del claim", e);
            return false;
        }
    }

    public List<String> getClaimMembers(int claimId) {
        List<String> members = new ArrayList<>();
        String sql = "SELECT player FROM claim_members WHERE claim_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, claimId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getString("player"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il recupero dei membri del claim", e);
        }
        return members;
    }

    public boolean canPlayerBuildInChunk(Player player, Chunk chunk) {
        String owner = getChunkOwner(chunk);
        if (owner == null) {
            return true; // Il chunk non è claimato
        }

        // Admin bypass
        if (player.hasPermission(plugin.getConfig().getString("permissions.admin-bypass"))) {
            return true;
        }

        // Proprietario del claim
        if (owner.equals(player.getName())) {
            return true;
        }

        // Membro del claim
        int claimId = getClaimId(chunk);
        return claimId != -1 && isPlayerMemberOfClaim(claimId, player.getName());
    }
}