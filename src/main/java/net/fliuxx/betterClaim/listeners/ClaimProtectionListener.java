package net.fliuxx.betterClaim.listeners;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.database.DatabaseManager;
import net.fliuxx.betterClaim.utils.Utils;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Iterator;

public class ClaimProtectionListener implements Listener {

    private final BetterClaim plugin;
    private final DatabaseManager databaseManager;

    public ClaimProtectionListener(BetterClaim plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("protection.enable-block-break", true)) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();

        if (!databaseManager.isChunkClaimed(chunk)) {
            return; // Il chunk non è claimato
        }

        if (!databaseManager.canPlayerBuildInChunk(player, chunk)) {
            event.setCancelled(true);
            player.sendMessage(Utils.getMessage("protection.protection-message", "&cNon hai il permesso per fare questo in questo claim!"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("protection.enable-block-place", true)) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();

        if (!databaseManager.isChunkClaimed(chunk)) {
            return; // Il chunk non è claimato
        }

        if (!databaseManager.canPlayerBuildInChunk(player, chunk)) {
            event.setCancelled(true);
            player.sendMessage(Utils.getMessage("protection.protection-message", "&cNon hai il permesso per fare questo in questo claim!"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("protection.enable-interact", true)) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Chunk chunk = block.getChunk();

        if (!databaseManager.isChunkClaimed(chunk)) {
            return; // Il chunk non è claimato
        }

        if (!databaseManager.canPlayerBuildInChunk(player, chunk)) {
            event.setCancelled(true);
            player.sendMessage(Utils.getMessage("protection.protection-message", "&cNon hai il permesso per fare questo in questo claim!"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getConfig().getBoolean("protection.enable-explosion", true)) {
            return;
        }

        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Chunk chunk = block.getChunk();

            if (databaseManager.isChunkClaimed(chunk)) {
                iterator.remove(); // Rimuovi il blocco dalla lista di esplosione
            }
        }
    }
}