package net.fliuxx.betterClaim.gui;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.models.Claim;
import net.fliuxx.betterClaim.models.ClaimFlag;
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

public class FlagGUI implements Listener {
    
    private final BetterClaim plugin;
    private final Claim claim;
    private final Map<Player, Inventory> openInventories;
    
    public FlagGUI(BetterClaim plugin, Claim claim) {
        this.plugin = plugin;
        this.claim = claim;
        this.openInventories = new HashMap<>();
        
        // Register this GUI as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void open(Player player) {
        String title = MessageUtils.colorize(plugin.getConfigManager().getGUITitle("claim-flags"));
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        setupFlagItems(inventory);
        
        player.openInventory(inventory);
        openInventories.put(player, inventory);
    }
    
    private void setupFlagItems(Inventory inventory) {
        ClaimFlag[] flags = ClaimFlag.values();
        
        for (int i = 0; i < flags.length && i < 45; i++) {
            ClaimFlag flag = flags[i];
            ItemStack flagItem = createFlagItem(flag);
            inventory.setItem(i, flagItem);
        }
        
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(MessageUtils.colorize("&7&lBack to Claim Menu"));
        backItem.setItemMeta(backMeta);
        inventory.setItem(49, backItem);
        
        // Fill empty slots with glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 45; i < 49; i++) {
            inventory.setItem(i, filler);
        }
        for (int i = 50; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }
    
    private ItemStack createFlagItem(ClaimFlag flag) {
        boolean currentValue = claim.getFlag(flag);
        
        // Choose material based on flag type and current value
        Material material = getFlagMaterial(flag, currentValue);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(MessageUtils.colorize(
            (currentValue ? "&a&l" : "&c&l") + flag.getDisplayName()
        ));
        
        meta.setLore(Arrays.asList(
            MessageUtils.colorize("&7" + flag.getDescription()),
            "",
            MessageUtils.colorize("&7Current Value: " + (currentValue ? "&aEnabled" : "&cDisabled")),
            MessageUtils.colorize("&7Default Value: " + (flag.getDefaultValue() ? "&aEnabled" : "&cDisabled")),
            "",
            MessageUtils.colorize("&eClick to toggle")
        ));
        
        item.setItemMeta(meta);
        return item;
    }
    
    private Material getFlagMaterial(ClaimFlag flag, boolean enabled) {
        Material baseMaterial;
        
        switch (flag) {
            case PVP:
                baseMaterial = Material.DIAMOND_SWORD;
                break;
            case MOB_SPAWNING:
                baseMaterial = Material.ZOMBIE_HEAD;
                break;
            case MOB_DAMAGE:
                baseMaterial = Material.BONE;
                break;
            case EXPLOSIONS:
                baseMaterial = Material.TNT;
                break;
            case FIRE_SPREAD:
                baseMaterial = Material.FIRE_CHARGE;
                break;
            case LAVA_FLOW:
                baseMaterial = Material.LAVA_BUCKET;
                break;
            case WATER_FLOW:
                baseMaterial = Material.WATER_BUCKET;
                break;
            case ITEM_PICKUP:
                baseMaterial = Material.HOPPER;
                break;
            case BLOCK_BREAK:
                baseMaterial = Material.DIAMOND_PICKAXE;
                break;
            case BLOCK_PLACE:
                baseMaterial = Material.GRASS_BLOCK;
                break;
            case CONTAINER_ACCESS:
                baseMaterial = Material.CHEST;
                break;
            case DOOR_ACCESS:
                baseMaterial = Material.OAK_DOOR;
                break;
            case BUTTON_ACCESS:
                baseMaterial = Material.STONE_BUTTON;
                break;
            case LEVER_ACCESS:
                baseMaterial = Material.LEVER;
                break;
            case PRESSURE_PLATE_ACCESS:
                baseMaterial = Material.STONE_PRESSURE_PLATE;
                break;
            case REDSTONE_ACCESS:
                baseMaterial = Material.REDSTONE;
                break;
            case ENTITY_INTERACT:
                baseMaterial = Material.LEAD;
                break;
            case ANIMAL_DAMAGE:
                baseMaterial = Material.BEEF;
                break;
            default:
                baseMaterial = Material.PAPER;
        }
        
        return baseMaterial;
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
        
        if (slot == 49) { // Back button
            player.closeInventory();
            plugin.getGuiManager().openClaimGUI(player, claim);
            return;
        }
        
        // Handle flag toggle
        ClaimFlag[] flags = ClaimFlag.values();
        if (slot >= 0 && slot < flags.length) {
            ClaimFlag flag = flags[slot];
            boolean currentValue = claim.getFlag(flag);
            boolean newValue = !currentValue;
            
            claim.setFlag(flag, newValue);
            
            // Save claim to database
            plugin.getDatabaseManager().saveClaim(claim);
            
            // Update the GUI
            ItemStack newItem = createFlagItem(flag);
            event.getInventory().setItem(slot, newItem);
            
            // Send confirmation message
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("flag", flag.getDisplayName());
            placeholders.put("value", String.valueOf(newValue));
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("flag.updated", placeholders)
            ));
        }
    }
}
