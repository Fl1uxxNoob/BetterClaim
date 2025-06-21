package net.fliuxx.betterClaim.models;

import java.sql.Timestamp;
import java.util.*;

public class Claim {
    
    private Integer id;
    private UUID ownerUUID;
    private String ownerName;
    private String world;
    private String name;
    private Set<ChunkLocation> chunks;
    private Set<ClaimMember> members;
    private Map<ClaimFlag, Boolean> flags;
    private Timestamp createdAt;
    private Timestamp lastAccessed;
    
    public Claim(UUID ownerUUID, String ownerName, String world) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.world = world;
        this.chunks = new HashSet<>();
        this.members = new HashSet<>();
        this.flags = new EnumMap<>(ClaimFlag.class);
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.lastAccessed = new Timestamp(System.currentTimeMillis());
        
        // Initialize with default flags
        for (ClaimFlag flag : ClaimFlag.values()) {
            this.flags.put(flag, flag.getDefaultValue());
        }
    }
    
    // Getters and setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    public String getWorld() {
        return world;
    }
    
    public void setWorld(String world) {
        this.world = world;
    }
    
    public String getName() {
        return name != null ? name : ownerName + "'s Claim";
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Set<ChunkLocation> getChunks() {
        return new HashSet<>(chunks);
    }
    
    public void addChunk(ChunkLocation chunk) {
        this.chunks.add(chunk);
    }
    
    public void removeChunk(ChunkLocation chunk) {
        this.chunks.remove(chunk);
    }
    
    public boolean hasChunk(ChunkLocation chunk) {
        return this.chunks.contains(chunk);
    }
    
    public int getChunkCount() {
        return chunks.size();
    }
    
    public Set<ClaimMember> getMembers() {
        return new HashSet<>(members);
    }
    
    public void addMember(ClaimMember member) {
        this.members.add(member);
    }
    
    public void removeMember(UUID playerUUID) {
        this.members.removeIf(member -> member.getPlayerUUID().equals(playerUUID));
    }
    
    public ClaimMember getMember(UUID playerUUID) {
        return members.stream()
                .filter(member -> member.getPlayerUUID().equals(playerUUID))
                .findFirst()
                .orElse(null);
    }
    
    public boolean hasMember(UUID playerUUID) {
        return getMember(playerUUID) != null;
    }
    
    public Map<ClaimFlag, Boolean> getFlags() {
        return new EnumMap<>(flags);
    }
    
    public void setFlag(ClaimFlag flag, boolean value) {
        this.flags.put(flag, value);
    }
    
    public boolean getFlag(ClaimFlag flag) {
        return flags.getOrDefault(flag, flag.getDefaultValue());
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getLastAccessed() {
        return lastAccessed;
    }
    
    public void setLastAccessed(Timestamp lastAccessed) {
        this.lastAccessed = lastAccessed;
    }
    
    public void updateLastAccessed() {
        this.lastAccessed = new Timestamp(System.currentTimeMillis());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Claim claim = (Claim) obj;
        return Objects.equals(id, claim.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Claim{" +
                "id=" + id +
                ", ownerName='" + ownerName + '\'' +
                ", world='" + world + '\'' +
                ", chunks=" + chunks.size() +
                '}';
    }
}
