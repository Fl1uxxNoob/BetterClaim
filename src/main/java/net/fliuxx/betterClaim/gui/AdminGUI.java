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
import java.util.List;
import java.util.Map;

public class AdminGUI implements Listener {
    
    private final BetterClaim plugin;
    private final Map<Player, Inventory> openInventories;
    private final Map<Player, Integer> currentPage;
    private final int ITEMS_PER_PAGE = 21;
    
    public AdminGUI(BetterClaim plugin) {
        this.plugin = plugin;
        this.openInventories = new HashMap<>();
        this.currentPage = new HashMap<>();
        
        // Register this GUI as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void open(Player player) {
        currentPage.put(player, 0);
        openMainMenu(player);
    }
    
    private void openMainMenu(Player player) {
        String title = MessageUtils.colorize(plugin.getConfigManager().getGUITitle("admin-main"));
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        setupMainMenuItems(inventory, player);
        
        player.openInventory(inventory);
        openInventories.put(player, inventory);
    }
    
    private void setupMainMenuItems(Inventory inventory, Player player) {
        // Plugin Statistics
        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(MessageUtils.colorize("&6&lPlugin Statistics"));
        statsMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Total Claims: &e" + plugin.getClaimManager().getTotalClaims()),
            MessageUtils.colorize("&7Total Chunks: &e" + plugin.getClaimManager().getTotalChunks()),
            MessageUtils.colorize("&7Average Chunks per Claim: &e" + 
                (plugin.getClaimManager().getTotalClaims() > 0 ? 
                    String.format("%.2f", (double) plugin.getClaimManager().getTotalChunks() / plugin.getClaimManager().getTotalClaims()) : "0")),
            "",
            MessageUtils.colorize("&7Click for detailed statistics")
        ));
        statsItem.setItemMeta(statsMeta);
        inventory.setItem(4, statsItem);
        
        // List All Claims
        ItemStack listItem = new ItemStack(Material.MAP);
        ItemMeta listMeta = listItem.getItemMeta();
        listMeta.setDisplayName(MessageUtils.colorize("&a&lList All Claims"));
        listMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7View and manage all claims"),
            MessageUtils.colorize("&7on the server."),
            "",
            MessageUtils.colorize("&eClick to browse claims")
        ));
        listItem.setItemMeta(listMeta);
        inventory.setItem(20, listItem);
        
        // Search Claims
        ItemStack searchItem = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchItem.getItemMeta();
        searchMeta.setDisplayName(MessageUtils.colorize("&b&lSearch Claims"));
        searchMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Search for claims by player name"),
            MessageUtils.colorize("&7or claim properties."),
            "",
            MessageUtils.colorize("&7Use: &e/claimadmin list <player>")
        ));
        searchItem.setItemMeta(searchMeta);
        inventory.setItem(22, searchItem);
        
        // Server Configuration
        ItemStack configItem = new ItemStack(Material.REDSTONE);
        ItemMeta configMeta = configItem.getItemMeta();
        configMeta.setDisplayName(MessageUtils.colorize("&c&lServer Configuration"));
        configMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Max Claims per Player: &e" + plugin.getConfigManager().getMaxClaimsPerPlayer()),
            MessageUtils.colorize("&7Max Chunks per Claim: &e" + plugin.getConfigManager().getMaxChunksPerClaim()),
            MessageUtils.colorize("&7Require Adjacent: &e" + plugin.getConfigManager().isRequireAdjacent()),
            MessageUtils.colorize("&7Show Borders: &e" + plugin.getConfigManager().isShowBorders()),
            "",
            MessageUtils.colorize("&7Edit config.yml to modify")
        ));
        configItem.setItemMeta(configMeta);
        inventory.setItem(24, configItem);
        
        // Reload Configuration
        ItemStack reloadItem = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta reloadMeta = reloadItem.getItemMeta();
        reloadMeta.setDisplayName(MessageUtils.colorize("&e&lReload Configuration"));
        reloadMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Reload plugin configuration"),
            MessageUtils.colorize("&7and messages from files."),
            "",
            MessageUtils.colorize("&eClick to reload")
        ));
        reloadItem.setItemMeta(reloadMeta);
        inventory.setItem(40, reloadItem);
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.ARROW);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(MessageUtils.colorize("&7&lClose"));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(49, closeItem);
        
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
    
    private void openClaimsList(Player player, int page) {
        String title = MessageUtils.colorize("&c&lAll Claims - Page " + (page + 1));
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        List<Claim> allClaims = plugin.getClaimManager().getAllClaims();
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allClaims.size());
        
        // Add claim items
        for (int i = startIndex; i < endIndex; i++) {
            Claim claim = allClaims.get(i);
            ItemStack claimItem = createClaimItem(claim);
            inventory.setItem(i - startIndex, claimItem);
        }
        
        // Navigation buttons
        if (page > 0) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.setDisplayName(MessageUtils.colorize("&a&lPrevious Page"));
            prevItem.setItemMeta(prevMeta);
            inventory.setItem(45, prevItem);
        }
        
        if (endIndex < allClaims.size()) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.setDisplayName(MessageUtils.colorize("&a&lNext Page"));
            nextItem.setItemMeta(nextMeta);
            inventory.setItem(53, nextItem);
        }
        
        // Back button
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(MessageUtils.colorize("&c&lBack to Main Menu"));
        backItem.setItemMeta(backMeta);
        inventory.setItem(49, backItem);
        
        player.openInventory(inventory);
        openInventories.put(player, inventory);
        currentPage.put(player, page);
    }
    
    private ItemStack createClaimItem(Claim claim) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(MessageUtils.colorize("&e&l" + claim.getName()));
        meta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Owner: &e" + claim.getOwnerName()),
            MessageUtils.colorize("&7World: &e" + claim.getWorld()),
            MessageUtils.colorize("&7Chunks: &e" + claim.getChunkCount()),
            MessageUtils.colorize("&7Members: &e" + claim.getMembers().size()),
            MessageUtils.colorize("&7Created: &e" + claim.getCreatedAt()),
            "",
            MessageUtils.colorize("&aLeft Click: &7Teleport to claim"),
            MessageUtils.colorize("&cRight Click: &7Delete claim")
        ));
        
        item.setItemMeta(meta);
        return item;
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
        
        String title = event.getView().getTitle();
        
        if (title.contains("Admin Panel")) {
            handleMainMenuClick(player, event.getSlot());
        } else if (title.contains("All Claims")) {
            handleClaimsListClick(player, event.getSlot(), event.isRightClick());
        }
    }
    
    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 20: // List All Claims
                openClaimsList(player, 0);
                break;
            case 40: // Reload Configuration
                plugin.reloadConfigs();
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("reload-success")
                ));
                player.closeInventory();
                break;
            case 49: // Close
                player.closeInventory();
                break;
        }
    }
    
    private void handleClaimsListClick(Player player, int slot, boolean rightClick) {
        List<Claim> allClaims = plugin.getClaimManager().getAllClaims();
        int currentPageNum = currentPage.getOrDefault(player, 0);
        int claimIndex = currentPageNum * ITEMS_PER_PAGE + slot;
        
        if (slot == 45) { // Previous page
            openClaimsList(player, currentPageNum - 1);
            return;
        }
        
        if (slot == 53) { // Next page
            openClaimsList(player, currentPageNum + 1);
            return;
        }
        
        if (slot == 49) { // Back to main menu
            openMainMenu(player);
            return;
        }
        
        if (claimIndex >= 0 && claimIndex < allClaims.size()) {
            Claim claim = allClaims.get(claimIndex);
            
            if (rightClick) {
                // Delete claim
                if (plugin.getClaimManager().deleteClaim(claim)) {
                    player.sendMessage(MessageUtils.colorize(
                        plugin.getConfigManager().getPrefix() + 
                        "&aSuccessfully deleted claim owned by &e" + claim.getOwnerName()
                    ));
                    openClaimsList(player, currentPageNum); // Refresh the list
                } else {
                    player.sendMessage(MessageUtils.colorize(
                        plugin.getConfigManager().getPrefix() + 
                        plugin.getConfigManager().getMessage("error.internal")
                    ));
                }
            } else {
                // Teleport to claim
                if (!claim.getChunks().isEmpty()) {
                    var firstChunk = claim.getChunks().iterator().next();
                    var world = Bukkit.getWorld(firstChunk.getWorld());
                    if (world != null) {
                        var location = new org.bukkit.Location(
                            world,
                            firstChunk.getX() * 16 + 8,
                            world.getHighestBlockYAt(firstChunk.getX() * 16 + 8, firstChunk.getZ() * 16 + 8) + 1,
                            firstChunk.getZ() * 16 + 8
                        );
                        player.teleport(location);
                        player.sendMessage(MessageUtils.colorize(
                            plugin.getConfigManager().getPrefix() + 
                            "&aTeleported to claim owned by &e" + claim.getOwnerName()
                        ));
                        player.closeInventory();
                    }
                }
            }
        }
    }
}
