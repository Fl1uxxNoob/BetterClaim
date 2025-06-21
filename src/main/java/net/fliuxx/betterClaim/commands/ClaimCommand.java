package net.fliuxx.betterClaim.commands;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.models.Claim;
import net.fliuxx.betterClaim.models.ClaimFlag;
import net.fliuxx.betterClaim.models.ClaimMember;
import net.fliuxx.betterClaim.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ClaimCommand implements CommandExecutor, TabCompleter {
    
    private final BetterClaim plugin;
    
    public ClaimCommand(BetterClaim plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.colorize(plugin.getConfigManager().getMessage("player-only")));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("betterclaim.claim")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player);
                break;
            case "expand":
                handleExpand(player);
                break;
            case "delete":
                handleDelete(player);
                break;
            case "info":
                handleInfo(player);
                break;
            case "list":
                handleList(player);
                break;
            case "trust":
                handleTrust(player, args);
                break;
            case "untrust":
                handleUntrust(player, args);
                break;
            case "flag":
                handleFlag(player, args);
                break;
            case "gui":
                handleGUI(player);
                break;
            case "help":
            default:
                sendHelp(player);
                break;
        }
        
        return true;
    }
    
    private void handleCreate(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        
        if (!plugin.getClaimManager().canClaim(player, chunk)) {
            // Check specific reason
            Claim existingClaim = plugin.getClaimManager().getClaim(chunk);
            if (existingClaim != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", existingClaim.getOwnerName());
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("claim.already-claimed", placeholders)
                ));
                return;
            }
            
            List<Claim> playerClaims = plugin.getClaimManager().getClaims(player);
            if (playerClaims.size() >= plugin.getConfigManager().getMaxClaimsPerPlayer()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("max", String.valueOf(plugin.getConfigManager().getMaxClaimsPerPlayer()));
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("claim.max-claims-reached", placeholders)
                ));
                return;
            }
            
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.cannot-claim-here")
            ));
            return;
        }
        
        Claim claim = plugin.getClaimManager().createClaim(player, chunk);
        if (claim != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("x", String.valueOf(chunk.getX()));
            placeholders.put("z", String.valueOf(chunk.getZ()));
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.created", placeholders)
            ));
        } else {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.cannot-claim-here")
            ));
        }
    }
    
    private void handleExpand(Player player) {
        Chunk currentChunk = player.getLocation().getChunk();
        
        // Check if current chunk is already claimed by someone else
        Claim existingClaim = plugin.getClaimManager().getClaim(currentChunk);
        if (existingClaim != null) {
            if (!existingClaim.getOwnerUUID().equals(player.getUniqueId())) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", existingClaim.getOwnerName());
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("claim.already-claimed", placeholders)
                ));
                return;
            } else {
                player.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + 
                    plugin.getConfigManager().getMessage("claim.already-own-chunk")
                ));
                return;
            }
        }
        
        // Find adjacent claims owned by the player
        List<Claim> playerClaims = plugin.getClaimManager().getClaims(player);
        if (playerClaims.isEmpty()) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.expand.no-claims")
            ));
            return;
        }
        
        // Check if current chunk is adjacent to any of player's claims
        Claim adjacentClaim = null;
        for (Claim claim : playerClaims) {
            if (isChunkAdjacentToClaim(currentChunk, claim)) {
                adjacentClaim = claim;
                break;
            }
        }
        
        if (adjacentClaim == null) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.expand.not-adjacent")
            ));
            return;
        }
        
        // Check if claim can be expanded (size limits)
        if (adjacentClaim.getChunkCount() >= plugin.getConfigManager().getMaxChunksPerClaim()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("max", String.valueOf(plugin.getConfigManager().getMaxChunksPerClaim()));
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.expand.max-chunks", placeholders)
            ));
            return;
        }
        
        // Expand the claim
        if (plugin.getClaimManager().expandClaim(adjacentClaim, currentChunk)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("x", String.valueOf(currentChunk.getX()));
            placeholders.put("z", String.valueOf(currentChunk.getZ()));
            placeholders.put("claim", adjacentClaim.getName());
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.expand.success", placeholders)
            ));
        } else {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.expand.failed")
            ));
        }
    }
    
    private boolean isChunkAdjacentToClaim(Chunk chunk, Claim claim) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        String world = chunk.getWorld().getName();
        
        if (!claim.getWorld().equals(world)) {
            return false;
        }
        
        // Check all chunks in the claim to see if any are adjacent
        return claim.getChunks().stream().anyMatch(chunkLocation -> {
            int claimChunkX = chunkLocation.getX();
            int claimChunkZ = chunkLocation.getZ();
            
            // Check if chunks are adjacent (sharing a side, not diagonal)
            return (Math.abs(chunkX - claimChunkX) == 1 && chunkZ == claimChunkZ) ||
                   (Math.abs(chunkZ - claimChunkZ) == 1 && chunkX == claimChunkX);
        });
    }
    
    private void handleDelete(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaim(chunk);
        
        if (claim == null) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-claimed")
            ));
            return;
        }
        
        if (!claim.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("betterclaim.admin")) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-your-claim")
            ));
            return;
        }
        
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
    }
    
    private void handleInfo(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaim(chunk);
        
        if (claim == null) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-claimed")
            ));
            return;
        }
        
        player.sendMessage(MessageUtils.colorize("&6&lClaim Information"));
        player.sendMessage(MessageUtils.colorize("&7Owner: &e" + claim.getOwnerName()));
        player.sendMessage(MessageUtils.colorize("&7Name: &e" + claim.getName()));
        player.sendMessage(MessageUtils.colorize("&7World: &e" + claim.getWorld()));
        player.sendMessage(MessageUtils.colorize("&7Chunks: &e" + claim.getChunkCount()));
        player.sendMessage(MessageUtils.colorize("&7Members: &e" + claim.getMembers().size()));
        player.sendMessage(MessageUtils.colorize("&7Created: &e" + claim.getCreatedAt()));
    }
    
    private void handleList(Player player) {
        List<Claim> claims = plugin.getClaimManager().getClaims(player);
        
        if (claims.isEmpty()) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.no-claims")
            ));
            return;
        }
        
        player.sendMessage(MessageUtils.colorize("&6&lYour Claims:"));
        for (Claim claim : claims) {
            player.sendMessage(MessageUtils.colorize(
                "&7- &e" + claim.getName() + " &7(" + claim.getChunkCount() + " chunks)"
            ));
        }
    }
    
    private void handleTrust(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                "&cUsage: /claim trust <player> [trust_level]"
            ));
            return;
        }
        
        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaim(chunk);
        
        if (claim == null) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-claimed")
            ));
            return;
        }
        
        if (!claim.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("betterclaim.admin")) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-your-claim")
            ));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", args[1]);
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("error.player-not-found", placeholders)
            ));
            return;
        }
        
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("trust.cannot-trust-self")
            ));
            return;
        }
        
        String trustLevel = args.length > 2 ? args[2].toLowerCase() : "trusted";
        if (!Arrays.asList("trusted", "moderator", "admin").contains(trustLevel)) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                "&cInvalid trust level. Available: trusted, moderator, admin"
            ));
            return;
        }
        
        ClaimMember existingMember = claim.getMember(target.getUniqueId());
        if (existingMember != null) {
            existingMember.setTrustLevel(trustLevel);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            placeholders.put("trust_level", trustLevel);
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("trust.trust-level-updated", placeholders)
            ));
        } else {
            ClaimMember member = new ClaimMember(target.getUniqueId(), target.getName(), trustLevel);
            claim.addMember(member);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            placeholders.put("trust_level", trustLevel);
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("trust.added", placeholders)
            ));
        }
        
        plugin.getDatabaseManager().saveClaim(claim);
    }
    
    private void handleUntrust(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                "&cUsage: /claim untrust <player>"
            ));
            return;
        }
        
        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaim(chunk);
        
        if (claim == null) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-claimed")
            ));
            return;
        }
        
        if (!claim.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("betterclaim.admin")) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-your-claim")
            ));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", args[1]);
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("error.player-not-found", placeholders)
            ));
            return;
        }
        
        if (!claim.hasMember(target.getUniqueId())) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("trust.not-trusted", placeholders)
            ));
            return;
        }
        
        claim.removeMember(target.getUniqueId());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        player.sendMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("trust.removed", placeholders)
        ));
        
        plugin.getDatabaseManager().saveClaim(claim);
    }
    
    private void handleFlag(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                "&cUsage: /claim flag <flag> <true|false>"
            ));
            return;
        }
        
        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaim(chunk);
        
        if (claim == null) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-claimed")
            ));
            return;
        }
        
        if (!claim.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("betterclaim.admin")) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-your-claim")
            ));
            return;
        }
        
        ClaimFlag flag = ClaimFlag.fromString(args[1]);
        if (flag == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("flag", args[1]);
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("flag.invalid", placeholders)
            ));
            return;
        }
        
        boolean value;
        try {
            value = Boolean.parseBoolean(args[2]);
        } catch (Exception e) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                "&cInvalid value. Use true or false."
            ));
            return;
        }
        
        claim.setFlag(flag, value);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("flag", flag.getDisplayName());
        placeholders.put("value", String.valueOf(value));
        player.sendMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("flag.updated", placeholders)
        ));
        
        plugin.getDatabaseManager().saveClaim(claim);
    }
    
    private void handleGUI(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaim(chunk);
        
        if (claim == null) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-claimed")
            ));
            return;
        }
        
        if (!claim.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("betterclaim.admin")) {
            player.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("claim.not-your-claim")
            ));
            return;
        }
        
        plugin.getGuiManager().openClaimGUI(player, claim);
    }
    
    private void sendHelp(Player player) {
        List<String> helpMessages = plugin.getConfigManager().getMessageList("help.claim");
        for (String message : helpMessages) {
            player.sendMessage(MessageUtils.colorize(message));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            return Arrays.asList("create", "expand", "delete", "info", "list", "trust", "untrust", "flag", "gui", "help")
                    .stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "trust":
                case "untrust":
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "flag":
                    return Arrays.stream(ClaimFlag.values())
                            .map(flag -> flag.name().toLowerCase().replace('_', '-'))
                            .filter(flag -> flag.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("trust")) {
                return Arrays.asList("trusted", "moderator", "admin")
                        .stream()
                        .filter(level -> level.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("flag")) {
                return Arrays.asList("true", "false")
                        .stream()
                        .filter(value -> value.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
}
