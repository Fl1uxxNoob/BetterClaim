package net.fliuxx.betterClaim.gui;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.models.Claim;
import net.fliuxx.betterClaim.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ClaimGUI implements Listener {
    
    private final BetterClaim plugin;
    private final Claim claim;
    private final Map<Player, Inventory> openInventories;
    
    public ClaimGUI(BetterClaim plugin, Claim claim) {
        this.plugin = plugin;
        this.claim = claim;
        this.openInventories = new HashMap<>();
        
        // Register this GUI as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void open(Player player) {
        String title = MessageUtils.colorize(plugin.getConfigManager().getGUITitle("claim-main"));
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        
        setupItems(inventory);
        
        player.openInventory(inventory);
        openInventories.put(player, inventory);
    }
    
    private void setupItems(Inventory inventory) {
        // Claim Info
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(MessageUtils.colorize("&6&lClaim Information"));
        infoMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Owner: &e" + claim.getOwnerName()),
            MessageUtils.colorize("&7Name: &e" + claim.getName()),
            MessageUtils.colorize("&7World: &e" + claim.getWorld()),
            MessageUtils.colorize("&7Chunks: &e" + claim.getChunkCount()),
            MessageUtils.colorize("&7Members: &e" + claim.getMembers().size()),
            "",
            MessageUtils.colorize("&7Created: &e" + claim.getCreatedAt())
        ));
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(4, infoItem);
        
        // Manage Flags
        ItemStack flagsItem = new ItemStack(Material.REDSTONE);
        ItemMeta flagsMeta = flagsItem.getItemMeta();
        flagsMeta.setDisplayName(MessageUtils.colorize("&c&lManage Flags"));
        flagsMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Configure claim protection"),
            MessageUtils.colorize("&7and behavior settings."),
            "",
            MessageUtils.colorize("&eClick to open flag menu")
        ));
        flagsItem.setItemMeta(flagsMeta);
        inventory.setItem(11, flagsItem);
        
        // Manage Trust
        ItemStack trustItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta trustMeta = trustItem.getItemMeta();
        trustMeta.setDisplayName(MessageUtils.colorize("&a&lManage Trust"));
        trustMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Add or remove trusted players"),
            MessageUtils.colorize("&7and manage their permissions."),
            "",
            MessageUtils.colorize("&eClick to open trust menu")
        ));
        trustItem.setItemMeta(trustMeta);
        inventory.setItem(13, trustItem);
        
        // Delete Claim
        ItemStack deleteItem = new ItemStack(Material.BARRIER);
        ItemMeta deleteMeta = deleteItem.getItemMeta();
        deleteMeta.setDisplayName(MessageUtils.colorize("&4&lDelete Claim"));
        deleteMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Permanently delete this claim."),
            MessageUtils.colorize("&c&lWARNING: This cannot be undone!"),
            "",
            MessageUtils.colorize("&eClick to delete claim")
        ));
        deleteItem.setItemMeta(deleteMeta);
        inventory.setItem(15, deleteItem);
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.ARROW);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(MessageUtils.colorize("&7&lClose"));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(22, closeItem);
        
        // Fill empty slots with glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        if (!openInventories.containsKey(player)) return;
        if (!event.getInventory().equals(openInventories.get(player))) return;
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        switch (event.getSlot()) {
            case 11: // Manage Flags
                player.closeInventory();
                plugin.getGuiManager().openFlagGUI(player, claim);
                break;
            case 13: // Manage Trust
                player.closeInventory();
                plugin.getGuiManager().openTrustGUI(player, claim);
                break;
            case 15: // Delete Claim
                if (plugin.getClaimManager().deleteClaim(claim)) {
                    player.sendMessage(MessageUtils.colorize(
                        plugin.getConfigManager().getPrefix() + 
                        plugin.getConfigManager().getMessage("claim.deleted")
                    ));
                } else {
                    player.sendMessage(MessageUtils.colorize(
                        plugin.getConfigManager().getPrefix() + 
                        plugin.getConfigManager().getMessage("error.internal")
                    ));
                }
                player.closeInventory();
                break;
            case 22: // Close
                player.closeInventory();
                break;
        }
    }
}
