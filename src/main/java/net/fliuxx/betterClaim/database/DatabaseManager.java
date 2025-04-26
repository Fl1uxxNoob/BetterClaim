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

            // Creare tabella delle regioni di claim
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS claim_regions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        owner TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
            }

            // Creare tabella dei chunk di claim
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS claim_chunks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        region_id INTEGER,
                        world TEXT,
                        chunkX INTEGER,
                        chunkZ INTEGER,
                        FOREIGN KEY(region_id) REFERENCES claim_regions(id) ON DELETE CASCADE,
                        UNIQUE(world, chunkX, chunkZ)
                    )
                """);
            }

            // Creare tabella dei membri
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS claim_members (
                        region_id INTEGER,
                        player TEXT,
                        FOREIGN KEY(region_id) REFERENCES claim_regions(id) ON DELETE CASCADE,
                        UNIQUE(region_id, player)
                    )
                """);
            }

            // Migrazione dei dati se le vecchie tabelle esistono
            migrateOldDataIfNeeded();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante l'inizializzazione del database", e);
        }
    }

    private void migrateOldDataIfNeeded() {
        try {
            // Controlla se esiste la tabella claims
            boolean oldTablesExist = false;
            try (Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='claims'")) {
                    oldTablesExist = rs.next();
                }
            }

            if (oldTablesExist) {
                plugin.getLogger().info("Migrazione dei dati dal vecchio formato al nuovo...");

                // Recupera tutti i claim esistenti e raggruppali per proprietario
                Map<String, List<Map<String, Object>>> claimsByOwner = new HashMap<>();
                try (Statement statement = connection.createStatement()) {
                    try (ResultSet rs = statement.executeQuery("SELECT id, world, chunkX, chunkZ, owner FROM claims")) {
                        while (rs.next()) {
                            Map<String, Object> claim = new HashMap<>();
                            claim.put("id", rs.getInt("id"));
                            claim.put("world", rs.getString("world"));
                            claim.put("chunkX", rs.getInt("chunkX"));
                            claim.put("chunkZ", rs.getInt("chunkZ"));

                            String owner = rs.getString("owner");
                            claimsByOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(claim);
                        }
                    }
                }

                // Per ogni proprietario, crea una regione e aggiungi i suoi chunk
                for (Map.Entry<String, List<Map<String, Object>>> entry : claimsByOwner.entrySet()) {
                    String owner = entry.getKey();
                    List<Map<String, Object>> claims = entry.getValue();

                    // Crea una nuova regione per il proprietario
                    int regionId;
                    String regionSql = "INSERT INTO claim_regions (owner) VALUES (?)";
                    try (PreparedStatement ps = connection.prepareStatement(regionSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, owner);
                        ps.executeUpdate();
                        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                regionId = generatedKeys.getInt(1);
                            } else {
                                throw new SQLException("Creating region failed, no ID obtained.");
                            }
                        }
                    }

                    // Aggiungi tutti i chunk alla regione
                    String chunkSql = "INSERT INTO claim_chunks (region_id, world, chunkX, chunkZ) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(chunkSql)) {
                        for (Map<String, Object> claim : claims) {
                            ps.setInt(1, regionId);
                            ps.setString(2, (String) claim.get("world"));
                            ps.setInt(3, (Integer) claim.get("chunkX"));
                            ps.setInt(4, (Integer) claim.get("chunkZ"));
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    // Migra i membri dei claim
                    for (Map<String, Object> claim : claims) {
                        int claimId = (Integer) claim.get("id");
                        try (PreparedStatement ps = connection.prepareStatement("SELECT player FROM claim_members WHERE claim_id = ?")) {
                            ps.setInt(1, claimId);
                            try (ResultSet rs = ps.executeQuery()) {
                                String memberSql = "INSERT OR IGNORE INTO claim_members (region_id, player) VALUES (?, ?)";
                                try (PreparedStatement insertPs = connection.prepareStatement(memberSql)) {
                                    while (rs.next()) {
                                        String player = rs.getString("player");
                                        insertPs.setInt(1, regionId);
                                        insertPs.setString(2, player);
                                        insertPs.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                }

                // Rinomina le vecchie tabelle per sicurezza
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE claims RENAME TO claims_old");
                    statement.execute("ALTER TABLE claim_members RENAME TO claim_members_old");
                }

                plugin.getLogger().info("Migrazione completata con successo!");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la migrazione dei dati", e);
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

            String playerName = player.getName();
            int maxClaims = plugin.getConfig().getInt("settings.max-claims", 2);

            // Recupera tutte le regioni dell'utente
            List<Integer> playerRegionIds = getPlayerRegionIds(playerName);

            // Verifica se ha raggiunto il limite massimo di regioni
            if (!player.hasPermission(plugin.getConfig().getString("permissions.claims-unlimited")) &&
                    playerRegionIds.size() >= maxClaims) {
                return false;
            }

            // Verifica se il chunk è adiacente a un claim esistente dello stesso player
            boolean isAdjacentToExistingClaim = isAdjacentToExistingClaim(chunk, playerName);

            // Se il player non ha claim o il chunk è adiacente, procedi
            if (playerRegionIds.isEmpty() || isAdjacentToExistingClaim) {
                // Se è adiacente, uniscilo alla regione esistente
                if (isAdjacentToExistingClaim) {
                    int regionId = getAdjacentClaimRegionId(chunk, playerName);
                    if (regionId != -1) {
                        return addChunkToRegion(regionId, chunk);
                    }
                }

                // Se non è adiacente o non abbiamo trovato una regione, crea una nuova regione
                int regionId;
                String regionSql = "INSERT INTO claim_regions (owner) VALUES (?)";
                try (PreparedStatement ps = connection.prepareStatement(regionSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, playerName);
                    ps.executeUpdate();
                    try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            regionId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Creating region failed, no ID obtained.");
                        }
                    }
                }

                // Aggiungi il chunk alla nuova regione
                return addChunkToRegion(regionId, chunk);
            } else {
                // Il chunk non è adiacente a nessun claim esistente
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la creazione del claim", e);
            return false;
        }
    }

    private boolean addChunkToRegion(int regionId, Chunk chunk) {
        String sql = "INSERT INTO claim_chunks (region_id, world, chunkX, chunkZ) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, regionId);
            ps.setString(2, chunk.getWorld().getName());
            ps.setInt(3, chunk.getX());
            ps.setInt(4, chunk.getZ());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante l'aggiunta del chunk alla regione", e);
            return false;
        }
    }

    private List<Integer> getPlayerRegionIds(String playerName) {
        List<Integer> regionIds = new ArrayList<>();
        String sql = "SELECT id FROM claim_regions WHERE owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    regionIds.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il recupero delle regioni del giocatore", e);
        }
        return regionIds;
    }

    private boolean isAdjacentToExistingClaim(Chunk chunk, String playerName) {
        // Cerca nei chunk adiacenti
        int[] offsetX = {-1, 1, 0, 0};
        int[] offsetZ = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            int adjacentX = chunk.getX() + offsetX[i];
            int adjacentZ = chunk.getZ() + offsetZ[i];

            String sql = "SELECT cc.id FROM claim_chunks cc " +
                    "JOIN claim_regions cr ON cc.region_id = cr.id " +
                    "WHERE cc.world = ? AND cc.chunkX = ? AND cc.chunkZ = ? AND cr.owner = ?";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, chunk.getWorld().getName());
                ps.setInt(2, adjacentX);
                ps.setInt(3, adjacentZ);
                ps.setString(4, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Errore durante la verifica dell'adiacenza", e);
            }
        }

        return false;
    }

    private int getAdjacentClaimRegionId(Chunk chunk, String playerName) {
        // Cerca nei chunk adiacenti
        int[] offsetX = {-1, 1, 0, 0};
        int[] offsetZ = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            int adjacentX = chunk.getX() + offsetX[i];
            int adjacentZ = chunk.getZ() + offsetZ[i];

            String sql = "SELECT cc.region_id FROM claim_chunks cc " +
                    "JOIN claim_regions cr ON cc.region_id = cr.id " +
                    "WHERE cc.world = ? AND cc.chunkX = ? AND cc.chunkZ = ? AND cr.owner = ?";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, chunk.getWorld().getName());
                ps.setInt(2, adjacentX);
                ps.setInt(3, adjacentZ);
                ps.setString(4, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("region_id");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Errore durante il recupero della regione adiacente", e);
            }
        }

        return -1;
    }

    public boolean removeClaim(Player player, Chunk chunk) {
        try {
            // Verificare se il chunk è claimato
            if (!isChunkClaimed(chunk)) {
                return false;
            }

            // Ottenere le informazioni sul chunk
            String sql = "SELECT cc.id as chunk_id, cc.region_id, cr.owner " +
                    "FROM claim_chunks cc " +
                    "JOIN claim_regions cr ON cc.region_id = cr.id " +
                    "WHERE cc.world = ? AND cc.chunkX = ? AND cc.chunkZ = ?";

            int chunkId = -1;
            int regionId = -1;
            String owner = null;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, chunk.getWorld().getName());
                ps.setInt(2, chunk.getX());
                ps.setInt(3, chunk.getZ());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        chunkId = rs.getInt("chunk_id");
                        regionId = rs.getInt("region_id");
                        owner = rs.getString("owner");
                    } else {
                        return false;
                    }
                }
            }

            // Verificare se il giocatore è il proprietario o un amministratore
            if (!owner.equals(player.getName()) && !player.hasPermission(plugin.getConfig().getString("permissions.admin-bypass"))) {
                return false;
            }

            // Rimuovere il chunk
            String deleteChunkSql = "DELETE FROM claim_chunks WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(deleteChunkSql)) {
                ps.setInt(1, chunkId);
                ps.executeUpdate();
            }

            // Verificare se ci sono altri chunk nella regione
            int remainingChunks = 0;
            String countSql = "SELECT COUNT(*) as count FROM claim_chunks WHERE region_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(countSql)) {
                ps.setInt(1, regionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        remainingChunks = rs.getInt("count");
                    }
                }
            }

            // Se non ci sono più chunk nella regione, rimuovi anche la regione e i suoi membri
            if (remainingChunks == 0) {
                String deleteRegionSql = "DELETE FROM claim_regions WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(deleteRegionSql)) {
                    ps.setInt(1, regionId);
                    ps.executeUpdate();
                }
            } else {
                // Controlla se la rimozione di questo chunk ha diviso la regione in parti non connesse
                // (Questa è una funzionalità avanzata che può essere implementata in futuro)
            }

            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la rimozione del claim", e);
            return false;
        }
    }

    public boolean isChunkClaimed(Chunk chunk) {
        String sql = "SELECT id FROM claim_chunks WHERE world = ? AND chunkX = ? AND chunkZ = ?";
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
        String sql = "SELECT cr.owner FROM claim_chunks cc " +
                "JOIN claim_regions cr ON cc.region_id = cr.id " +
                "WHERE cc.world = ? AND cc.chunkX = ? AND cc.chunkZ = ?";
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

    public int getRegionId(Chunk chunk) {
        String sql = "SELECT region_id FROM claim_chunks WHERE world = ? AND chunkX = ? AND chunkZ = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chunk.getWorld().getName());
            ps.setInt(2, chunk.getX());
            ps.setInt(3, chunk.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("region_id") : -1;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il recupero dell'ID della regione", e);
            return -1;
        }
    }

    public int getPlayerRegionCount(String playerName) {
        String sql = "SELECT COUNT(*) as count FROM claim_regions WHERE owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("count") : 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il conteggio delle regioni del giocatore", e);
            return 0;
        }
    }

    public int getPlayerChunkCount(String playerName) {
        String sql = "SELECT COUNT(*) as count FROM claim_chunks cc " +
                "JOIN claim_regions cr ON cc.region_id = cr.id " +
                "WHERE cr.owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("count") : 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il conteggio dei chunk del giocatore", e);
            return 0;
        }
    }

    public boolean addMemberToRegion(int regionId, String playerName) {
        String sql = "INSERT INTO claim_members (region_id, player) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, regionId);
            ps.setString(2, playerName);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                return false; // Il giocatore è già un membro
            }
            plugin.getLogger().log(Level.SEVERE, "Errore durante l'aggiunta di un membro alla regione", e);
            return false;
        }
    }

    public boolean removeMemberFromRegion(int regionId, String playerName) {
        String sql = "DELETE FROM claim_members WHERE region_id = ? AND player = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, regionId);
            ps.setString(2, playerName);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la rimozione di un membro dalla regione", e);
            return false;
        }
    }

    public boolean isPlayerMemberOfRegion(int regionId, String playerName) {
        String sql = "SELECT player FROM claim_members WHERE region_id = ? AND player = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, regionId);
            ps.setString(2, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante la verifica di un membro della regione", e);
            return false;
        }
    }

    public List<String> getRegionMembers(int regionId) {
        List<String> members = new ArrayList<>();
        String sql = "SELECT player FROM claim_members WHERE region_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, regionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getString("player"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il recupero dei membri della regione", e);
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

        // Membro della regione
        int regionId = getRegionId(chunk);
        return regionId != -1 && isPlayerMemberOfRegion(regionId, player.getName());
    }

    public List<Chunk> getRegionChunks(int regionId, org.bukkit.World world) {
        List<Chunk> chunks = new ArrayList<>();
        String sql = "SELECT chunkX, chunkZ FROM claim_chunks WHERE region_id = ? AND world = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, regionId);
            ps.setString(2, world.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int x = rs.getInt("chunkX");
                    int z = rs.getInt("chunkZ");
                    chunks.add(world.getChunkAt(x, z));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore durante il recupero dei chunk della regione", e);
        }
        return chunks;
    }
}