package net.fliuxx.betterClaim.gui;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.models.Claim;
import net.fliuxx.betterClaim.models.ClaimMember;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class TrustGUI implements Listener {
    
    private final BetterClaim plugin;
    private final Claim claim;
    private final Map<Player, Inventory> openInventories;
    private final Map<Player, Integer> currentPage;
    private final int ITEMS_PER_PAGE = 28;
    
    public TrustGUI(BetterClaim plugin, Claim claim) {
        this.plugin = plugin;
        this.claim = claim;
        this.openInventories = new HashMap<>();
        this.currentPage = new HashMap<>();
        
        // Register this GUI as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void open(Player player) {
        currentPage.put(player, 0);
        openTrustMenu(player, 0);
    }
    
    private void openTrustMenu(Player player, int page) {
        String title = MessageUtils.colorize(plugin.getConfigManager().getGUITitle("claim-trust") + " - Page " + (page + 1));
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        setupTrustItems(inventory, page);
        
        player.openInventory(inventory);
        openInventories.put(player, inventory);
        currentPage.put(player, page);
    }
    
    private void setupTrustItems(Inventory inventory, int page) {
        Set<ClaimMember> members = claim.getMembers();
        List<ClaimMember> memberList = new ArrayList<>(members);
        
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, memberList.size());
        
        // Add member items (4 rows of 7 items each, skipping the borders)
        int slot = 10; // Start from second row, second column
        for (int i = startIndex; i < endIndex; i++) {
            ClaimMember member = memberList.get(i);
            ItemStack memberItem = createMemberItem(member);
            inventory.setItem(slot, memberItem);
            
            slot++;
            if ((slot + 1) % 9 == 0) { // Skip right border
                slot += 2; // Skip to next row, second column
            }
            if (slot >= 46) break; // Stop before last row
        }
        
        // Navigation buttons
        if (page > 0) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.setDisplayName(MessageUtils.colorize("&a&lPrevious Page"));
            prevItem.setItemMeta(prevMeta);
            inventory.setItem(45, prevItem);
        }
        
        if (endIndex < memberList.size()) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.setDisplayName(MessageUtils.colorize("&a&lNext Page"));
            nextItem.setItemMeta(nextMeta);
            inventory.setItem(53, nextItem);
        }
        
        // Add Player button
        ItemStack addItem = new ItemStack(Material.EMERALD);
        ItemMeta addMeta = addItem.getItemMeta();
        addMeta.setDisplayName(MessageUtils.colorize("&a&lAdd Player"));
        addMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Use the command to add players:"),
            MessageUtils.colorize("&e/claim trust <player> [level]"),
            "",
            MessageUtils.colorize("&7Trust Levels:"),
            MessageUtils.colorize("&7- &etrusted &7(basic permissions)"),
            MessageUtils.colorize("&7- &emoderator &7(extended permissions)"),
            MessageUtils.colorize("&7- &eadmin &7(full permissions)")
        ));
        addItem.setItemMeta(addMeta);
        inventory.setItem(48, addItem);
        
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(MessageUtils.colorize("&7&lBack to Claim Menu"));
        backItem.setItemMeta(backMeta);
        inventory.setItem(49, backItem);
        
        // Help button
        ItemStack helpItem = new ItemStack(Material.BOOK);
        ItemMeta helpMeta = helpItem.getItemMeta();
        helpMeta.setDisplayName(MessageUtils.colorize("&6&lTrust System Help"));
        helpMeta.setLore(Arrays.asList(
            MessageUtils.colorize("&7Trust levels grant different permissions:"),
            "",
            MessageUtils.colorize("&e&lTrusted:"),
            MessageUtils.colorize("&7- Break/place blocks"),
            MessageUtils.colorize("&7- Use containers, doors, buttons"),
            MessageUtils.colorize("&7- Pick up items"),
            "",
            MessageUtils.colorize("&e&lModerator:"),
            MessageUtils.colorize("&7- All trusted permissions"),
            MessageUtils.colorize("&7- Use pressure plates, redstone"),
            MessageUtils.colorize("&7- Interact with entities"),
            "",
            MessageUtils.colorize("&e&lAdmin:"),
            MessageUtils.colorize("&7- All moderator permissions"),
            MessageUtils.colorize("&7- Damage animals")
        ));
        helpItem.setItemMeta(helpMeta);
        inventory.setItem(50, helpItem);
        
        // Fill borders with glass panes
        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderItem.setItemMeta(borderMeta);
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderItem);
            inventory.setItem(i + 45, borderItem);
        }
        
        // Side borders
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, borderItem);
            inventory.setItem(i * 9 + 8, borderItem);
        }
        
        // Clear the buttons area
        inventory.setItem(45, page > 0 ? inventory.getItem(45) : borderItem);
        inventory.setItem(48, inventory.getItem(48));
        inventory.setItem(49, inventory.getItem(49));
        inventory.setItem(50, inventory.getItem(50));
        inventory.setItem(53, endIndex < memberList.size() ? inventory.getItem(53) : borderItem);
    }
    
    private ItemStack createMemberItem(ClaimMember member) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        // Try to set the skull owner
        Player onlinePlayer = Bukkit.getPlayer(member.getPlayerUUID());
        if (onlinePlayer != null) {
            meta.setOwningPlayer(onlinePlayer);
        } else {
            // Fallback to name if player is offline
            meta.setOwner(member.getPlayerName());
        }
        
        meta.setDisplayName(MessageUtils.colorize("&e&l" + member.getPlayerName()));
        
        String trustColor = getTrustLevelColor(member.getTrustLevel());
        List<String> permissions = plugin.getConfigManager().getTrustLevelPermissions(member.getTrustLevel());
        
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Trust Level: " + trustColor + member.getTrustLevel()));
        lore.add(MessageUtils.colorize("&7Added: &e" + member.getAddedAt()));
        lore.add("");
        lore.add(MessageUtils.colorize("&7Permissions:"));
        
        for (String permission : permissions) {
            lore.add(MessageUtils.colorize("&8- &7" + permission.replace('-', ' ')));
        }
        
        lore.add("");
        lore.add(MessageUtils.colorize("&aLeft Click: &7Change trust level"));
        lore.add(MessageUtils.colorize("&cRight Click: &7Remove player"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private String getTrustLevelColor(String trustLevel) {
        switch (trustLevel.toLowerCase()) {
            case "trusted":
                return "&a";
            case "moderator":
                return "&b";
            case "admin":
                return "&c";
            default:
                return "&7";
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
        
        int slot = event.getSlot();
        int page = currentPage.getOrDefault(player, 0);
        
        // Handle navigation
        if (slot == 45 && page > 0) { // Previous page
            openTrustMenu(player, page - 1);
            return;
        }
        
        if (slot == 53) { // Next page
            openTrustMenu(player, page + 1);
            return;
        }
        
        if (slot == 49) { // Back to claim menu
            player.closeInventory();
            plugin.getGuiManager().openClaimGUI(player, claim);
            return;
        }
        
        // Handle member interaction
        if (clickedItem.getType() == Material.PLAYER_HEAD) {
            ClaimMember member = getMemberFromSlot(slot, page);
            if (member != null) {
                if (event.isRightClick()) {
                    // Remove member
                    claim.removeMember(member.getPlayerUUID());
                    plugin.getDatabaseManager().saveClaim(claim);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("player", member.getPlayerName());
                    player.sendMessage(MessageUtils.colorize(
                        plugin.getConfigManager().getPrefix() + 
                        plugin.getConfigManager().getMessage("trust.removed", placeholders)
                    ));
                    
                    // Refresh the GUI
                    openTrustMenu(player, page);
                } else {
                    // Cycle trust level
                    String newTrustLevel = getNextTrustLevel(member.getTrustLevel());
                    member.setTrustLevel(newTrustLevel);
                    plugin.getDatabaseManager().saveClaim(claim);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("player", member.getPlayerName());
                    placeholders.put("trust_level", newTrustLevel);
                    player.sendMessage(MessageUtils.colorize(
                        plugin.getConfigManager().getPrefix() + 
                        plugin.getConfigManager().getMessage("trust.trust-level-updated", placeholders)
                    ));
                    
                    // Refresh the GUI
                    openTrustMenu(player, page);
                }
            }
        }
    }
    
    private ClaimMember getMemberFromSlot(int slot, int page) {
        List<ClaimMember> memberList = new ArrayList<>(claim.getMembers());
        
        // Convert GUI slot to member index
        int row = slot / 9;
        int col = slot % 9;
        
        // Skip border slots
        if (row == 0 || row == 5 || col == 0 || col == 8) {
            return null;
        }
        
        // Calculate actual position in the grid (4 rows of 7 items)
        int gridRow = row - 1;
        int gridCol = col - 1;
        int gridIndex = gridRow * 7 + gridCol;
        
        int memberIndex = page * ITEMS_PER_PAGE + gridIndex;
        
        if (memberIndex >= 0 && memberIndex < memberList.size()) {
            return memberList.get(memberIndex);
        }
        
        return null;
    }
    
    private String getNextTrustLevel(String currentLevel) {
        switch (currentLevel.toLowerCase()) {
            case "trusted":
                return "moderator";
            case "moderator":
                return "admin";
            case "admin":
                return "trusted";
            default:
                return "trusted";
        }
    }
}
