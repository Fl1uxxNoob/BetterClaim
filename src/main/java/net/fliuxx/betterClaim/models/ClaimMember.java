package net.fliuxx.betterClaim.models;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

public class ClaimMember {
    
    private UUID playerUUID;
    private String playerName;
    private String trustLevel;
    private Timestamp addedAt;
    
    public ClaimMember(UUID playerUUID, String playerName, String trustLevel) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.trustLevel = trustLevel;
        this.addedAt = new Timestamp(System.currentTimeMillis());
    }
    
    // Getters and setters
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public String getTrustLevel() {
        return trustLevel;
    }
    
    public void setTrustLevel(String trustLevel) {
        this.trustLevel = trustLevel;
    }
    
    public Timestamp getAddedAt() {
        return addedAt;
    }
    
    public void setAddedAt(Timestamp addedAt) {
        this.addedAt = addedAt;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ClaimMember that = (ClaimMember) obj;
        return Objects.equals(playerUUID, that.playerUUID);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(playerUUID);
    }
    
    @Override
    public String toString() {
        return "ClaimMember{" +
                "playerName='" + playerName + '\'' +
                ", trustLevel='" + trustLevel + '\'' +
                '}';
    }
}
