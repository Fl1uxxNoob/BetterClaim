package net.fliuxx.betterClaim.models;

import org.bukkit.Chunk;

import java.util.Objects;

public class ChunkLocation {
    
    private String world;
    private int x;
    private int z;
    
    public ChunkLocation(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }
    
    public ChunkLocation(Chunk chunk) {
        this.world = chunk.getWorld().getName();
        this.x = chunk.getX();
        this.z = chunk.getZ();
    }
    
    // Getters and setters
    public String getWorld() {
        return world;
    }
    
    public void setWorld(String world) {
        this.world = world;
    }
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getZ() {
        return z;
    }
    
    public void setZ(int z) {
        this.z = z;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ChunkLocation that = (ChunkLocation) obj;
        return x == that.x && z == that.z && Objects.equals(world, that.world);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }
    
    @Override
    public String toString() {
        return "ChunkLocation{" +
                "world='" + world + '\'' +
                ", x=" + x +
                ", z=" + z +
                '}';
    }
}
