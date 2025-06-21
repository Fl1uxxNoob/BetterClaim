package net.fliuxx.betterClaim.listeners;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.models.Claim;
import net.fliuxx.betterClaim.models.ClaimFlag;
import net.fliuxx.betterClaim.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClaimProtectionListener implements Listener {
    
    private final BetterClaim plugin;
    private final Set<UUID> lastClaimNotified;
    
    // Interactive blocks that should be protected
    private final Set<Material> CONTAINER_BLOCKS = new HashSet<>(Arrays.asList(
        Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.SHULKER_BOX,
        Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER, Material.BREWING_STAND,
        Material.DISPENSER, Material.DROPPER, Material.HOPPER
    ));
    
    private final Set<Material> DOOR_BLOCKS = new HashSet<>(Arrays.asList(
        Material.OAK_DOOR, Material.BIRCH_DOOR, Material.SPRUCE_DOOR, Material.JUNGLE_DOOR,
        Material.ACACIA_DOOR, Material.DARK_OAK_DOOR, Material.CRIMSON_DOOR, Material.WARPED_DOOR,
        Material.IRON_DOOR, Material.OAK_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.SPRUCE_TRAPDOOR,
        Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
        Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.IRON_TRAPDOOR
    ));
    
    private final Set<Material> BUTTON_BLOCKS = new HashSet<>(Arrays.asList(
        Material.STONE_BUTTON, Material.OAK_BUTTON, Material.BIRCH_BUTTON, Material.SPRUCE_BUTTON,
        Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON,
        Material.CRIMSON_BUTTON, Material.WARPED_BUTTON, Material.POLISHED_BLACKSTONE_BUTTON
    ));
    
    private final Set<Material> LEVER_BLOCKS = new HashSet<>(Arrays.asList(
        Material.LEVER
    ));
    
    private final Set<Material> PRESSURE_PLATE_BLOCKS = new HashSet<>(Arrays.asList(
        Material.STONE_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE,
        Material.SPRUCE_PRESSURE_PLATE, Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE,
        Material.DARK_OAK_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE, Material.WARPED_PRESSURE_PLATE,
        Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
        Material.POLISHED_BLACKSTONE_PRESSURE_PLATE
    ));
    
    private final Set<Material> REDSTONE_BLOCKS = new HashSet<>(Arrays.asList(
        Material.REDSTONE_WIRE, Material.REPEATER, Material.COMPARATOR, Material.REDSTONE_TORCH,
        Material.REDSTONE_WALL_TORCH, Material.REDSTONE_BLOCK, Material.TARGET
    ));
    
    public ClaimProtectionListener(BetterClaim plugin) {
        this.plugin = plugin;
        this.lastClaimNotified = new HashSet<>();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        if (!plugin.getClaimManager().hasPermission(player, block.getChunk(), ClaimFlag.BLOCK_BREAK)) {
            event.setCancelled(true);
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("protection.block-break")
            ));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        if (!plugin.getClaimManager().hasPermission(player, block.getChunk(), ClaimFlag.BLOCK_PLACE)) {
            event.setCancelled(true);
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("protection.block-place")
            ));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Material material = block.getType();
        
        // Check container access
        if (CONTAINER_BLOCKS.contains(material)) {
            if (!plugin.getClaimManager().hasPermission(player, block.getChunk(), ClaimFlag.CONTAINER_ACCESS)) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("protection.container-access")
                ));
                return;
            }
        }
        
        // Check door access
        if (DOOR_BLOCKS.contains(material)) {
            if (!plugin.getClaimManager().hasPermission(player, block.getChunk(), ClaimFlag.DOOR_ACCESS)) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("protection.door-access")
                ));
                return;
            }
        }
        
        // Check button access
        if (BUTTON_BLOCKS.contains(material)) {
            if (!plugin.getClaimManager().hasPermission(player, block.getChunk(), ClaimFlag.BUTTON_ACCESS)) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("protection.button-access")
                ));
                return;
            }
        }
        
        // Check lever access
        if (LEVER_BLOCKS.contains(material)) {
            if (!plugin.getClaimManager().hasPermission(player, block.getChunk(), ClaimFlag.LEVER_ACCESS)) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("protection.lever-access")
                ));
                return;
            }
        }
        
        // Check pressure plate access
        if (PRESSURE_PLATE_BLOCKS.contains(material)) {
            if (!plugin.getClaimManager().hasPermission(player, block.getChunk(), ClaimFlag.PRESSURE_PLATE_ACCESS)) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("protection.pressure-plate-access")
                ));
                return;
            }
        }
        
        // Check redstone access
        if (REDSTONE_BLOCKS.contains(material)) {
            if (!plugin.getClaimManager().hasPermission(player, block.getChunk(), ClaimFlag.REDSTONE_ACCESS)) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("protection.redstone-access")
                ));
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        Entity entity = event.getEntity();
        
        // Check PvP
        if (entity instanceof Player) {
            Claim claim = plugin.getClaimManager().getClaim(entity.getLocation().getChunk());
            if (claim != null && !claim.getFlag(ClaimFlag.PVP)) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("protection.pvp")
                ));
                return;
            }
        }
        
        // Check animal damage
        if (entity instanceof Animals) {
            if (!plugin.getClaimManager().hasPermission(player, entity.getLocation().getChunk(), ClaimFlag.ANIMAL_DAMAGE)) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("protection.animal-damage")
                ));
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        Item item = event.getItem();
        
        if (!plugin.getClaimManager().hasPermission(player, item.getLocation().getChunk(), ClaimFlag.ITEM_PICKUP)) {
            event.setCancelled(true);
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("protection.item-pickup")
            ));
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return; // Same chunk, no need to check
        }
        
        if (!plugin.getConfigManager().isShowBorders()) {
            return; // Border notifications disabled
        }
        
        Player player = event.getPlayer();
        Claim fromClaim = plugin.getClaimManager().getClaim(event.getFrom().getChunk());
        Claim toClaim = plugin.getClaimManager().getClaim(event.getTo().getChunk());
        
        // Entering a claim
        if (fromClaim == null && toClaim != null) {
            showClaimEnterMessage(player, toClaim);
        }
        // Leaving a claim
        else if (fromClaim != null && toClaim == null) {
            showClaimLeaveMessage(player, fromClaim);
        }
        // Moving between different claims
        else if (fromClaim != null && toClaim != null && !fromClaim.equals(toClaim)) {
            showClaimLeaveMessage(player, fromClaim);
            showClaimEnterMessage(player, toClaim);
        }
    }
    
    private void showClaimEnterMessage(Player player, Claim claim) {
        if (lastClaimNotified.contains(player.getUniqueId())) {
            return; // Already notified recently
        }
        
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("claim", claim.getName());
        placeholders.put("player", claim.getOwnerName());
        
        player.sendMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("claim.enter", placeholders)
        ));
        
        lastClaimNotified.add(player.getUniqueId());
        
        // Remove from set after 3 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            lastClaimNotified.remove(player.getUniqueId());
        }, 60L);
    }
    
    private void showClaimLeaveMessage(Player player, Claim claim) {
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("claim", claim.getName());
        
        player.sendMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("claim.leave", placeholders)
        ));
    }
}
