package net.fliuxx.betterClaim.managers;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.models.Claim;
import net.fliuxx.betterClaim.models.ClaimFlag;
import net.fliuxx.betterClaim.models.ClaimMember;
import net.fliuxx.betterClaim.models.ChunkLocation;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    
    private final BetterClaim plugin;
    private String databaseUrl;
    
    public DatabaseManager(BetterClaim plugin) {
        this.plugin = plugin;
    }
    
    public void initializeDatabase() {
        try {
            // Create database file if it doesn't exist
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            String filename = plugin.getConfigManager().getDatabaseFilename();
            File databaseFile = new File(dataFolder, filename);
            
            // Store database URL for creating connections
            databaseUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            
            // Create tables with a fresh connection
            createTables();
            
            plugin.getLogger().info("Database initialized successfully");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(databaseUrl);
        // Configure SQLite connection for better concurrency
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA cache_size=10000");
            stmt.execute("PRAGMA temp_store=memory");
            stmt.execute("PRAGMA mmap_size=268435456"); // 256MB
        }
        return conn;
    }
    
    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // Claims table
        String createClaimsTable = """
            CREATE TABLE IF NOT EXISTS claims (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                owner_uuid TEXT NOT NULL,
                owner_name TEXT NOT NULL,
                world TEXT NOT NULL,
                name TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // Claim chunks table
        String createChunksTable = """
            CREATE TABLE IF NOT EXISTS claim_chunks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                claim_id INTEGER NOT NULL,
                chunk_x INTEGER NOT NULL,
                chunk_z INTEGER NOT NULL,
                FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE,
                UNIQUE(claim_id, chunk_x, chunk_z)
            )
        """;
        
        // Claim flags table
        String createFlagsTable = """
            CREATE TABLE IF NOT EXISTS claim_flags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                claim_id INTEGER NOT NULL,
                flag_name TEXT NOT NULL,
                flag_value BOOLEAN NOT NULL,
                FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE,
                UNIQUE(claim_id, flag_name)
            )
        """;
        
        // Claim members table
        String createMembersTable = """
            CREATE TABLE IF NOT EXISTS claim_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                claim_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                trust_level TEXT NOT NULL,
                added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE,
                UNIQUE(claim_id, player_uuid)
            )
        """;
        
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createClaimsTable);
                stmt.execute(createChunksTable);
                stmt.execute(createFlagsTable);
                stmt.execute(createMembersTable);
            }
        }
    }
    
    public CompletableFuture<Void> saveClaim(Claim claim) {
        if (plugin.getConfigManager().isAsyncSaves()) {
            return CompletableFuture.runAsync(() -> saveClaimSync(claim));
        } else {
            return CompletableFuture.completedFuture(saveClaimSync(claim));
        }
    }
    
    private Void saveClaimSync(Claim claim) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            if (claim.getId() == null) {
                // Insert new claim
                insertClaim(claim, conn);
            } else {
                // Update existing claim
                updateClaim(claim, conn);
            }
            
            // Save chunks
            saveClaimChunks(claim, conn);
            
            // Save flags
            saveClaimFlags(claim, conn);
            
            // Save members
            saveClaimMembers(claim, conn);
            
            conn.commit();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save claim: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    private void insertClaim(Claim claim, Connection conn) throws SQLException {
        String sql = "INSERT INTO claims (owner_uuid, owner_name, world, name) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, claim.getOwnerUUID().toString());
            stmt.setString(2, claim.getOwnerName());
            stmt.setString(3, claim.getWorld());
            stmt.setString(4, claim.getName());
            
            stmt.executeUpdate();
            
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    claim.setId(keys.getInt(1));
                }
            }
        }
    }
    
    private void updateClaim(Claim claim, Connection conn) throws SQLException {
        String sql = "UPDATE claims SET owner_name = ?, name = ?, last_accessed = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, claim.getOwnerName());
            stmt.setString(2, claim.getName());
            stmt.setInt(3, claim.getId());
            
            stmt.executeUpdate();
        }
    }
    
    private void saveClaimChunks(Claim claim, Connection conn) throws SQLException {
        // Delete existing chunks
        String deleteSql = "DELETE FROM claim_chunks WHERE claim_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, claim.getId());
            stmt.executeUpdate();
        }
        
        // Insert chunks
        String insertSql = "INSERT INTO claim_chunks (claim_id, chunk_x, chunk_z) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            for (ChunkLocation chunk : claim.getChunks()) {
                stmt.setInt(1, claim.getId());
                stmt.setInt(2, chunk.getX());
                stmt.setInt(3, chunk.getZ());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    
    private void saveClaimFlags(Claim claim, Connection conn) throws SQLException {
        // Delete existing flags
        String deleteSql = "DELETE FROM claim_flags WHERE claim_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, claim.getId());
            stmt.executeUpdate();
        }
        
        // Insert flags
        String insertSql = "INSERT INTO claim_flags (claim_id, flag_name, flag_value) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            for (Map.Entry<ClaimFlag, Boolean> entry : claim.getFlags().entrySet()) {
                stmt.setInt(1, claim.getId());
                stmt.setString(2, entry.getKey().name());
                stmt.setBoolean(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    
    private void saveClaimMembers(Claim claim, Connection conn) throws SQLException {
        // Delete existing members
        String deleteSql = "DELETE FROM claim_members WHERE claim_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, claim.getId());
            stmt.executeUpdate();
        }
        
        // Insert members
        String insertSql = "INSERT INTO claim_members (claim_id, player_uuid, player_name, trust_level) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            for (ClaimMember member : claim.getMembers()) {
                stmt.setInt(1, claim.getId());
                stmt.setString(2, member.getPlayerUUID().toString());
                stmt.setString(3, member.getPlayerName());
                stmt.setString(4, member.getTrustLevel());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    
    public CompletableFuture<List<Claim>> loadAllClaims() {
        return CompletableFuture.supplyAsync(this::loadAllClaimsSync);
    }
    
    private List<Claim> loadAllClaimsSync() {
        List<Claim> claims = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM claims ORDER BY created_at DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Claim claim = createClaimFromResultSet(rs);
                    
                    // Load chunks
                    loadClaimChunks(claim, conn);
                    
                    // Load flags
                    loadClaimFlags(claim, conn);
                    
                    // Load members
                    loadClaimMembers(claim, conn);
                    
                    claims.add(claim);
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load claims: " + e.getMessage());
            e.printStackTrace();
        }
        
        return claims;
    }
    
    private Claim createClaimFromResultSet(ResultSet rs) throws SQLException {
        Claim claim = new Claim(
            UUID.fromString(rs.getString("owner_uuid")),
            rs.getString("owner_name"),
            rs.getString("world")
        );
        
        claim.setId(rs.getInt("id"));
        claim.setName(rs.getString("name"));
        claim.setCreatedAt(rs.getTimestamp("created_at"));
        claim.setLastAccessed(rs.getTimestamp("last_accessed"));
        
        return claim;
    }
    
    private void loadClaimChunks(Claim claim, Connection conn) throws SQLException {
        String sql = "SELECT chunk_x, chunk_z FROM claim_chunks WHERE claim_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, claim.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ChunkLocation chunk = new ChunkLocation(
                        claim.getWorld(),
                        rs.getInt("chunk_x"),
                        rs.getInt("chunk_z")
                    );
                    claim.addChunk(chunk);
                }
            }
        }
    }
    
    private void loadClaimFlags(Claim claim, Connection conn) throws SQLException {
        String sql = "SELECT flag_name, flag_value FROM claim_flags WHERE claim_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, claim.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        ClaimFlag flag = ClaimFlag.valueOf(rs.getString("flag_name"));
                        boolean value = rs.getBoolean("flag_value");
                        claim.setFlag(flag, value);
                    } catch (IllegalArgumentException e) {
                        // Flag no longer exists, skip it
                        plugin.getLogger().warning("Unknown flag found in database: " + rs.getString("flag_name"));
                    }
                }
            }
        }
    }
    
    private void loadClaimMembers(Claim claim, Connection conn) throws SQLException {
        String sql = "SELECT player_uuid, player_name, trust_level, added_at FROM claim_members WHERE claim_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, claim.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ClaimMember member = new ClaimMember(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("trust_level")
                    );
                    member.setAddedAt(rs.getTimestamp("added_at"));
                    claim.addMember(member);
                }
            }
        }
    }
    
    public CompletableFuture<Void> deleteClaim(Claim claim) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = "DELETE FROM claims WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, claim.getId());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete claim: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public void closeConnection() {
        // No persistent connection to close since we use connection per operation
        plugin.getLogger().info("Database connection pool closed");
    }
}
