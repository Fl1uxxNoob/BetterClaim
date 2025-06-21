package net.fliuxx.betterClaim.managers;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.models.Claim;
import net.fliuxx.betterClaim.models.ClaimFlag;
import net.fliuxx.betterClaim.models.ClaimMember;
import net.fliuxx.betterClaim.models.ChunkLocation;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClaimManager {
    
    private final BetterClaim plugin;
    private final Map<ChunkLocation, Claim> claimsByChunk;
    private final Map<UUID, List<Claim>> claimsByOwner;
    private final Map<Integer, Claim> claimsById;
    
    public ClaimManager(BetterClaim plugin) {
        this.plugin = plugin;
        this.claimsByChunk = new ConcurrentHashMap<>();
        this.claimsByOwner = new ConcurrentHashMap<>();
        this.claimsById = new ConcurrentHashMap<>();
    }
    
    public void loadClaims() {
        plugin.getDatabaseManager().loadAllClaims().thenAccept(claims -> {
            claimsByChunk.clear();
            claimsByOwner.clear();
            claimsById.clear();
            
            for (Claim claim : claims) {
                // Index by chunks
                for (ChunkLocation chunk : claim.getChunks()) {
                    claimsByChunk.put(chunk, claim);
                }
                
                // Index by owner
                claimsByOwner.computeIfAbsent(claim.getOwnerUUID(), k -> new ArrayList<>()).add(claim);
                
                // Index by ID
                claimsById.put(claim.getId(), claim);
            }
            
            plugin.getLogger().info("Loaded " + claims.size() + " claims from database");
        });
    }
    
    public void reloadClaims() {
        loadClaims();
    }
    
    public Claim getClaim(ChunkLocation chunk) {
        return claimsByChunk.get(chunk);
    }
    
    public Claim getClaim(Chunk chunk) {
        return getClaim(new ChunkLocation(chunk));
    }
    
    public List<Claim> getClaims(UUID ownerUUID) {
        return claimsByOwner.getOrDefault(ownerUUID, new ArrayList<>());
    }
    
    public List<Claim> getClaims(Player player) {
        return getClaims(player.getUniqueId());
    }
    
    public Claim getClaimById(int id) {
        return claimsById.get(id);
    }
    
    public boolean canClaim(Player player, Chunk chunk) {
        ChunkLocation chunkLoc = new ChunkLocation(chunk);
        
        // Check if chunk is already claimed
        if (claimsByChunk.containsKey(chunkLoc)) {
            return false;
        }
        
        // Check max claims per player
        List<Claim> playerClaims = getClaims(player);
        if (playerClaims.size() >= plugin.getConfigManager().getMaxClaimsPerPlayer()) {
            return false;
        }
        
        // Check if player has existing claims
        if (!playerClaims.isEmpty() && plugin.getConfigManager().isRequireAdjacent()) {
            // Check if chunk is adjacent to existing claim
            Claim existingClaim = playerClaims.get(0);
            if (existingClaim.getChunks().size() >= plugin.getConfigManager().getMaxChunksPerClaim()) {
                return false;
            }
            
            return isChunkAdjacent(chunkLoc, existingClaim);
        }
        
        return true;
    }
    
    private boolean isChunkAdjacent(ChunkLocation newChunk, Claim existingClaim) {
        for (ChunkLocation existingChunk : existingClaim.getChunks()) {
            if (areChunksAdjacent(newChunk, existingChunk)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean areChunksAdjacent(ChunkLocation chunk1, ChunkLocation chunk2) {
        if (!chunk1.getWorld().equals(chunk2.getWorld())) {
            return false;
        }
        
        int deltaX = Math.abs(chunk1.getX() - chunk2.getX());
        int deltaZ = Math.abs(chunk1.getZ() - chunk2.getZ());
        
        return (deltaX == 1 && deltaZ == 0) || (deltaX == 0 && deltaZ == 1);
    }
    
    public Claim createClaim(Player player, Chunk chunk) {
        if (!canClaim(player, chunk)) {
            return null;
        }
        
        ChunkLocation chunkLoc = new ChunkLocation(chunk);
        
        // Check if player already has a claim (for expansion)
        List<Claim> playerClaims = getClaims(player);
        
        if (!playerClaims.isEmpty() && plugin.getConfigManager().isRequireAdjacent()) {
            // Expand existing claim
            Claim existingClaim = playerClaims.get(0);
            existingClaim.addChunk(chunkLoc);
            
            // Update index
            claimsByChunk.put(chunkLoc, existingClaim);
            
            // Save to database
            plugin.getDatabaseManager().saveClaim(existingClaim);
            
            return existingClaim;
        } else {
            // Create new claim
            Claim claim = new Claim(player.getUniqueId(), player.getName(), chunk.getWorld().getName());
            claim.addChunk(chunkLoc);
            
            // Set default flags
            Map<ClaimFlag, Boolean> defaultFlags = plugin.getConfigManager().getDefaultFlags();
            for (Map.Entry<ClaimFlag, Boolean> entry : defaultFlags.entrySet()) {
                claim.setFlag(entry.getKey(), entry.getValue());
            }
            
            // Save to database
            plugin.getDatabaseManager().saveClaim(claim).thenAccept(v -> {
                // Update indices
                claimsByChunk.put(chunkLoc, claim);
                claimsByOwner.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(claim);
                claimsById.put(claim.getId(), claim);
            });
            
            return claim;
        }
    }
    
    public boolean expandClaim(Claim claim, Chunk chunk) {
        if (getClaim(chunk) != null) {
            return false; // Chunk already claimed
        }
        
        ChunkLocation chunkLoc = new ChunkLocation(chunk);
        
        // Add chunk to the claim
        claim.addChunk(chunkLoc);
        
        // Update index
        claimsByChunk.put(chunkLoc, claim);
        
        // Save to database
        plugin.getDatabaseManager().saveClaim(claim);
        
        return true;
    }
    
    public boolean deleteClaim(Claim claim) {
        try {
            // Remove from indices
            for (ChunkLocation chunk : claim.getChunks()) {
                claimsByChunk.remove(chunk);
            }
            
            List<Claim> ownerClaims = claimsByOwner.get(claim.getOwnerUUID());
            if (ownerClaims != null) {
                ownerClaims.remove(claim);
                if (ownerClaims.isEmpty()) {
                    claimsByOwner.remove(claim.getOwnerUUID());
                }
            }
            
            claimsById.remove(claim.getId());
            
            // Delete from database
            plugin.getDatabaseManager().deleteClaim(claim);
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to delete claim: " + e.getMessage());
            return false;
        }
    }
    
    public boolean hasPermission(Player player, Chunk chunk, ClaimFlag flag) {
        Claim claim = getClaim(chunk);
        if (claim == null) {
            return true; // No claim, no restrictions
        }
        
        // Owner has all permissions
        if (claim.getOwnerUUID().equals(player.getUniqueId())) {
            return true;
        }
        
        // Check bypass permission
        if (player.hasPermission("betterclaim.bypass")) {
            return true;
        }
        
        // Check claim flag
        if (!claim.getFlag(flag)) {
            return false; // Flag is disabled
        }
        
        // Check trust level
        ClaimMember member = claim.getMember(player.getUniqueId());
        if (member == null) {
            return false; // Not trusted
        }
        
        // Check trust level permissions
        List<String> permissions = plugin.getConfigManager().getTrustLevelPermissions(member.getTrustLevel());
        return permissions.contains(flag.name().toLowerCase().replace('_', '-'));
    }
    
    public void saveAllClaims() {
        plugin.getLogger().info("Saving all claims...");
        
        List<Claim> allClaims = new ArrayList<>();
        allClaims.addAll(claimsById.values());
        
        for (Claim claim : allClaims) {
            plugin.getDatabaseManager().saveClaim(claim);
        }
        
        plugin.getLogger().info("Saved " + allClaims.size() + " claims");
    }
    
    public List<Claim> getAllClaims() {
        return new ArrayList<>(claimsById.values());
    }
    
    public List<Claim> getClaimsByPlayer(String playerName) {
        return claimsById.values().stream()
                .filter(claim -> claim.getOwnerName().equalsIgnoreCase(playerName))
                .collect(Collectors.toList());
    }
    
    public int getTotalClaims() {
        return claimsById.size();
    }
    
    public int getTotalChunks() {
        return claimsByChunk.size();
    }
}
