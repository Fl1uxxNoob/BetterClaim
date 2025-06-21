package net.fliuxx.betterClaim.utils;

import net.fliuxx.betterClaim.models.ChunkLocation;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class for claim-related operations and calculations
 */
public class ClaimUtils {
    
    /**
     * Get all chunks within a radius around a center chunk
     * @param center The center chunk location
     * @param radius The radius in chunks
     * @return List of chunk locations within the radius
     */
    public static List<ChunkLocation> getChunksInRadius(ChunkLocation center, int radius) {
        List<ChunkLocation> chunks = new ArrayList<>();
        
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                chunks.add(new ChunkLocation(center.getWorld(), x, z));
            }
        }
        
        return chunks;
    }
    
    /**
     * Check if two chunks are adjacent (share a border)
     * @param chunk1 First chunk
     * @param chunk2 Second chunk
     * @return true if chunks are adjacent
     */
    public static boolean areChunksAdjacent(ChunkLocation chunk1, ChunkLocation chunk2) {
        if (!chunk1.getWorld().equals(chunk2.getWorld())) {
            return false;
        }
        
        int deltaX = Math.abs(chunk1.getX() - chunk2.getX());
        int deltaZ = Math.abs(chunk1.getZ() - chunk2.getZ());
        
        return (deltaX == 1 && deltaZ == 0) || (deltaX == 0 && deltaZ == 1);
    }
    
    /**
     * Check if a chunk is adjacent to any chunk in a set
     * @param chunk The chunk to check
     * @param chunkSet The set of chunks to check against
     * @return true if the chunk is adjacent to any chunk in the set
     */
    public static boolean isChunkAdjacentToAny(ChunkLocation chunk, Set<ChunkLocation> chunkSet) {
        for (ChunkLocation existingChunk : chunkSet) {
            if (areChunksAdjacent(chunk, existingChunk)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the distance in blocks between two chunk locations
     * @param chunk1 First chunk
     * @param chunk2 Second chunk
     * @return Distance in blocks (returns -1 if different worlds)
     */
    public static double getChunkDistance(ChunkLocation chunk1, ChunkLocation chunk2) {
        if (!chunk1.getWorld().equals(chunk2.getWorld())) {
            return -1;
        }
        
        // Calculate center points of chunks
        double x1 = chunk1.getX() * 16 + 8;
        double z1 = chunk1.getZ() * 16 + 8;
        double x2 = chunk2.getX() * 16 + 8;
        double z2 = chunk2.getZ() * 16 + 8;
        
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2));
    }
    
    /**
     * Get the center location of a chunk
     * @param chunk The chunk location
     * @param world The world object
     * @return The center location of the chunk
     */
    public static Location getChunkCenter(ChunkLocation chunk, World world) {
        double x = chunk.getX() * 16 + 8;
        double z = chunk.getZ() * 16 + 8;
        double y = world.getHighestBlockYAt((int) x, (int) z) + 1;
        
        return new Location(world, x, y, z);
    }
    
    /**
     * Get all chunks that form the border of a claim area
     * @param chunks The chunks in the claim
     * @return List of chunk locations that form the border
     */
    public static List<ChunkLocation> getClaimBorderChunks(Set<ChunkLocation> chunks) {
        List<ChunkLocation> borderChunks = new ArrayList<>();
        
        for (ChunkLocation chunk : chunks) {
            // Check if this chunk has at least one side not adjacent to another claim chunk
            boolean isBorder = false;
            
            // Check all 4 directions
            ChunkLocation[] neighbors = {
                new ChunkLocation(chunk.getWorld(), chunk.getX() + 1, chunk.getZ()),
                new ChunkLocation(chunk.getWorld(), chunk.getX() - 1, chunk.getZ()),
                new ChunkLocation(chunk.getWorld(), chunk.getX(), chunk.getZ() + 1),
                new ChunkLocation(chunk.getWorld(), chunk.getX(), chunk.getZ() - 1)
            };
            
            for (ChunkLocation neighbor : neighbors) {
                if (!chunks.contains(neighbor)) {
                    isBorder = true;
                    break;
                }
            }
            
            if (isBorder) {
                borderChunks.add(chunk);
            }
        }
        
        return borderChunks;
    }
    
    /**
     * Calculate the total area of a claim in blocks
     * @param chunks The chunks in the claim
     * @return Total area in blocks
     */
    public static int calculateClaimArea(Set<ChunkLocation> chunks) {
        return chunks.size() * 256; // Each chunk is 16x16 = 256 blocks
    }
    
    /**
     * Get the bounding box of a claim (min/max coordinates)
     * @param chunks The chunks in the claim
     * @return int array with [minX, minZ, maxX, maxZ] in chunk coordinates
     */
    public static int[] getClaimBounds(Set<ChunkLocation> chunks) {
        if (chunks.isEmpty()) {
            return new int[]{0, 0, 0, 0};
        }
        
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        
        for (ChunkLocation chunk : chunks) {
            minX = Math.min(minX, chunk.getX());
            minZ = Math.min(minZ, chunk.getZ());
            maxX = Math.max(maxX, chunk.getX());
            maxZ = Math.max(maxZ, chunk.getZ());
        }
        
        return new int[]{minX, minZ, maxX, maxZ};
    }
    
    /**
     * Check if a location is within a chunk
     * @param location The location to check
     * @param chunk The chunk to check against
     * @return true if the location is within the chunk
     */
    public static boolean isLocationInChunk(Location location, ChunkLocation chunk) {
        if (!location.getWorld().getName().equals(chunk.getWorld())) {
            return false;
        }
        
        Chunk locationChunk = location.getChunk();
        return locationChunk.getX() == chunk.getX() && locationChunk.getZ() == chunk.getZ();
    }
    
    /**
     * Convert a location to a chunk location
     * @param location The location to convert
     * @return The chunk location containing the given location
     */
    public static ChunkLocation locationToChunk(Location location) {
        return new ChunkLocation(location.getChunk());
    }
    
    /**
     * Format chunk coordinates for display
     * @param chunk The chunk location
     * @return Formatted string representation
     */
    public static String formatChunkCoords(ChunkLocation chunk) {
        return String.format("%s (%d, %d)", chunk.getWorld(), chunk.getX(), chunk.getZ());
    }
    
    /**
     * Check if a player is standing in a specific chunk
     * @param player The player to check
     * @param chunk The chunk to check against
     * @return true if the player is in the chunk
     */
    public static boolean isPlayerInChunk(Player player, ChunkLocation chunk) {
        return isLocationInChunk(player.getLocation(), chunk);
    }
    
    /**
     * Get a list of online players within a claim area
     * @param chunks The chunks in the claim
     * @param world The world to check in
     * @return List of players within the claim
     */
    public static List<Player> getPlayersInClaim(Set<ChunkLocation> chunks, World world) {
        List<Player> playersInClaim = new ArrayList<>();
        
        for (Player player : world.getPlayers()) {
            ChunkLocation playerChunk = new ChunkLocation(player.getLocation().getChunk());
            if (chunks.contains(playerChunk)) {
                playersInClaim.add(player);
            }
        }
        
        return playersInClaim;
    }
}
