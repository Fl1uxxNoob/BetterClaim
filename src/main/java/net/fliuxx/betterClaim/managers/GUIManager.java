package net.fliuxx.betterClaim.managers;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.gui.AdminGUI;
import net.fliuxx.betterClaim.gui.ClaimGUI;
import net.fliuxx.betterClaim.gui.FlagGUI;
import net.fliuxx.betterClaim.gui.TrustGUI;
import net.fliuxx.betterClaim.models.Claim;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager {
    
    private final BetterClaim plugin;
    private final Map<UUID, String> openGUIs;
    
    public GUIManager(BetterClaim plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
    }
    
    public void openClaimGUI(Player player, Claim claim) {
        ClaimGUI gui = new ClaimGUI(plugin, claim);
        gui.open(player);
        openGUIs.put(player.getUniqueId(), "claim");
    }
    
    public void openFlagGUI(Player player, Claim claim) {
        FlagGUI gui = new FlagGUI(plugin, claim);
        gui.open(player);
        openGUIs.put(player.getUniqueId(), "flag");
    }
    
    public void openTrustGUI(Player player, Claim claim) {
        TrustGUI gui = new TrustGUI(plugin, claim);
        gui.open(player);
        openGUIs.put(player.getUniqueId(), "trust");
    }
    
    public void openAdminGUI(Player player) {
        AdminGUI gui = new AdminGUI(plugin);
        gui.open(player);
        openGUIs.put(player.getUniqueId(), "admin");
    }
    
    public String getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }
    
    public void closeGUI(Player player) {
        openGUIs.remove(player.getUniqueId());
    }
    
    public boolean hasOpenGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }
}
