package net.fliuxx.betterClaim.commands;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.models.Claim;
import net.fliuxx.betterClaim.models.ChunkLocation;
import net.fliuxx.betterClaim.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ClaimAdminCommand implements CommandExecutor, TabCompleter {
    
    private final BetterClaim plugin;
    
    public ClaimAdminCommand(BetterClaim plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("betterclaim.admin")) {
            sender.sendMessage(MessageUtils.colorize(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        
        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getGuiManager().openAdminGUI((Player) sender);
            } else {
                sendHelp(sender);
            }
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "list":
                handleList(sender, args);
                break;
            case "delete":
                handleDelete(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "teleport":
                handleTeleport(sender, args);
                break;
            case "gui":
                handleGUI(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "stats":
                handleStats(sender);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void handleList(CommandSender sender, String[] args) {
        if (args.length > 1) {
            // List claims for specific player
            String playerName = args[1];
            List<Claim> claims = plugin.getClaimManager().getClaimsByPlayer(playerName);
            
            if (claims.isEmpty()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", playerName);
                sender.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("admin.no-claims-found", placeholders)
                ));
                return;
            }
            
            sender.sendMessage(MessageUtils.colorize("&6&lClaims for " + playerName + ":"));
            for (Claim claim : claims) {
                sender.sendMessage(MessageUtils.colorize(
                    "&7- &e" + claim.getName() + " &7(ID: " + claim.getId() + ", Chunks: " + claim.getChunkCount() + ")"
                ));
            }
        } else {
            // List all claims
            List<Claim> allClaims = plugin.getClaimManager().getAllClaims();
            
            if (allClaims.isEmpty()) {
                sender.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    "&7No claims found."
                ));
                return;
            }
            
            sender.sendMessage(MessageUtils.colorize("&6&lAll Claims:"));
            for (Claim claim : allClaims.stream().limit(10).collect(Collectors.toList())) {
                sender.sendMessage(MessageUtils.colorize(
                    "&7- &e" + claim.getName() + " &7by &e" + claim.getOwnerName() + 
                    " &7(ID: " + claim.getId() + ", Chunks: " + claim.getChunkCount() + ")"
                ));
            }
            
            if (allClaims.size() > 10) {
                sender.sendMessage(MessageUtils.colorize("&7... and " + (allClaims.size() - 10) + " more"));
            }
        }
    }
    
    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                "&cUsage: /claimadmin delete <player>"
            ));
            return;
        }
        
        String playerName = args[1];
        List<Claim> claims = plugin.getClaimManager().getClaimsByPlayer(playerName);
        
        if (claims.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", playerName);
            sender.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("admin.no-claims-found", placeholders)
            ));
            return;
        }
        
        for (Claim claim : claims) {
            plugin.getClaimManager().deleteClaim(claim);
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        sender.sendMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("admin.claim-deleted", placeholders)
        ));
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                "&cUsage: /claimadmin info <player>"
            ));
            return;
        }
        
        String playerName = args[1];
        List<Claim> claims = plugin.getClaimManager().getClaimsByPlayer(playerName);
        
        if (claims.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", playerName);
            sender.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("admin.no-claims-found", placeholders)
            ));
            return;
        }
        
        Claim claim = claims.get(0); // Get first claim
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", claim.getOwnerName());
        placeholders.put("chunks", String.valueOf(claim.getChunkCount()));
        placeholders.put("created", claim.getCreatedAt().toString());
        
        sender.sendMessage(MessageUtils.colorize(
            plugin.getConfigManager().getMessage("admin.claim-info", placeholders)
        ));
    }
    
    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.colorize(plugin.getConfigManager().getMessage("player-only")));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                "&cUsage: /claimadmin teleport <player>"
            ));
            return;
        }
        
        Player player = (Player) sender;
        String playerName = args[1];
        List<Claim> claims = plugin.getClaimManager().getClaimsByPlayer(playerName);
        
        if (claims.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", playerName);
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("admin.no-claims-found", placeholders)
            ));
            return;
        }
        
        Claim claim = claims.get(0); // Get first claim
        ChunkLocation firstChunk = claim.getChunks().iterator().next();
        
        Location teleportLocation = new Location(
            Bukkit.getWorld(firstChunk.getWorld()),
            firstChunk.getX() * 16 + 8,
            Bukkit.getWorld(firstChunk.getWorld()).getHighestBlockYAt(firstChunk.getX() * 16 + 8, firstChunk.getZ() * 16 + 8) + 1,
            firstChunk.getZ() * 16 + 8
        );
        
        player.teleport(teleportLocation);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        player.sendMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("admin.teleported", placeholders)
        ));
    }
    
    private void handleGUI(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.colorize(plugin.getConfigManager().getMessage("player-only")));
            return;
        }
        
        plugin.getGuiManager().openAdminGUI((Player) sender);
    }
    
    private void handleReload(CommandSender sender) {
        plugin.reloadConfigs();
        sender.sendMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("reload-success")
        ));
    }
    
    private void handleStats(CommandSender sender) {
        int totalClaims = plugin.getClaimManager().getTotalClaims();
        int totalChunks = plugin.getClaimManager().getTotalChunks();
        
        sender.sendMessage(MessageUtils.colorize("&6&lBetterClaim Statistics:"));
        sender.sendMessage(MessageUtils.colorize("&7Total Claims: &e" + totalClaims));
        sender.sendMessage(MessageUtils.colorize("&7Total Chunks: &e" + totalChunks));
        sender.sendMessage(MessageUtils.colorize("&7Average Chunks per Claim: &e" + 
            (totalClaims > 0 ? String.format("%.2f", (double) totalChunks / totalClaims) : "0")));
    }
    
    private void sendHelp(CommandSender sender) {
        List<String> helpMessages = plugin.getConfigManager().getMessageList("help.admin");
        for (String message : helpMessages) {
            sender.sendMessage(MessageUtils.colorize(message));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("betterclaim.admin")) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            return Arrays.asList("list", "delete", "info", "teleport", "gui", "reload", "stats", "help")
                    .stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "list":
                case "delete":
                case "info":
                case "teleport":
                    return plugin.getClaimManager().getAllClaims().stream()
                            .map(Claim::getOwnerName)
                            .distinct()
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
}
