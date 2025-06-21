package net.fliuxx.betterClaim.listeners;

import net.fliuxx.betterClaim.BetterClaim;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GUIListener implements Listener {
    
    private final BetterClaim plugin;
    
    public GUIListener(BetterClaim plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if player has a GUI open
        if (!plugin.getGuiManager().hasOpenGUI(player)) {
            return;
        }
        
        // Let the specific GUI handle the click
        String guiType = plugin.getGuiManager().getOpenGUI(player);
        if (guiType != null) {
            // The GUI classes will handle their own click events
            // This is just a fallback to prevent item duplication
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // Clean up GUI tracking
        plugin.getGuiManager().closeGUI(player);
    }
}
