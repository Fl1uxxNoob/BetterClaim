package net.fliuxx.betterClaim.commands;

import net.fliuxx.betterClaim.BetterClaim;
import net.fliuxx.betterClaim.database.DatabaseManager;
import net.fliuxx.betterClaim.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClaimCommand implements CommandExecutor, TabCompleter {

    private final BetterClaim plugin;
    private final DatabaseManager databaseManager;

    public ClaimCommand(BetterClaim plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.colorize("&cQuesto comando può essere eseguito solo da un giocatore!"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "claim":
            case "add":
                claimChunk(player);
                break;
            case "unclaim":
            case "remove":
                unclaimChunk(player);
                break;
            case "info":
                showClaimInfo(player);
                break;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage(Utils.colorize("&cUtilizzo: /claim invite <giocatore>"));
                    return true;
                }
                invitePlayer(player, args[1]);
                break;
            case "list":
                listClaims(player);
                break;
            case "help":
                showHelp(player);
                break;
            default:
                player.sendMessage(Utils.colorize("&cComando sconosciuto. Usa /claim help per l'elenco dei comandi."));
                break;
        }

        return true;
    }

    private void claimChunk(Player player) {
        Chunk chunk = player.getLocation().getChunk();

        if (databaseManager.isChunkClaimed(chunk)) {
            player.sendMessage(Utils.getMessage("settings.claim-already-exists", "&cQuesto chunk è già stato claimato!"));
            return;
        }

        int maxClaims = plugin.getConfig().getInt("settings.max-claims", 2);
        if (!player.hasPermission(plugin.getConfig().getString("permissions.claims-unlimited")) &&
                databaseManager.getPlayerClaimCount(player.getName()) >= maxClaims) {
            String message = Utils.getMessage("settings.max-claims-reached", "&cHai raggiunto il limite massimo di claim (%max%)!");
            message = Utils.replacePlaceholders(message, "%max%", String.valueOf(maxClaims));
            player.sendMessage(message);
            return;
        }

        if (databaseManager.createClaim(player, chunk)) {
            player.sendMessage(Utils.getMessage("settings.claim-message", "&aHai claimato questo chunk con successo!"));
        } else {
            player.sendMessage(Utils.colorize("&cErrore durante la creazione del claim. Riprova più tardi."));
        }
    }

    private void unclaimChunk(Player player) {
        Chunk chunk = player.getLocation().getChunk();

        if (!databaseManager.isChunkClaimed(chunk)) {
            player.sendMessage(Utils.getMessage("settings.no-claim-here", "&cNon c'è nessun claim in questo chunk!"));
            return;
        }

        String owner = databaseManager.getChunkOwner(chunk);
        if (!owner.equals(player.getName()) && !player.hasPermission(plugin.getConfig().getString("permissions.admin-bypass"))) {
            player.sendMessage(Utils.getMessage("settings.not-your-claim", "&cQuesto claim non ti appartiene!"));
            return;
        }

        if (databaseManager.removeClaim(player, chunk)) {
            player.sendMessage(Utils.getMessage("settings.claim-removed", "&aHai rimosso questo claim con successo!"));
        } else {
            player.sendMessage(Utils.colorize("&cErrore durante la rimozione del claim. Riprova più tardi."));
        }
    }

    private void showClaimInfo(Player player) {
        Chunk chunk = player.getLocation().getChunk();

        if (!databaseManager.isChunkClaimed(chunk)) {
            player.sendMessage(Utils.getMessage("settings.no-claim-here", "&cNon c'è nessun claim in questo chunk!"));
            return;
        }

        String owner = databaseManager.getChunkOwner(chunk);
        int claimId = databaseManager.getClaimId(chunk);
        List<String> members = databaseManager.getClaimMembers(claimId);

        player.sendMessage(Utils.colorize("&a=== Informazioni Claim ==="));
        player.sendMessage(Utils.colorize("&7Posizione: &f" + Utils.getChunkCoordinates(chunk)));
        player.sendMessage(Utils.colorize("&7Proprietario: &f" + owner));

        if (members.isEmpty()) {
            player.sendMessage(Utils.colorize("&7Membri: &fNessuno"));
        } else {
            player.sendMessage(Utils.colorize("&7Membri: &f" + String.join(", ", members)));
        }
    }

    private void invitePlayer(Player player, String targetName) {
        Chunk chunk = player.getLocation().getChunk();

        if (!databaseManager.isChunkClaimed(chunk)) {
            player.sendMessage(Utils.getMessage("settings.no-claim-here", "&cNon c'è nessun claim in questo chunk!"));
            return;
        }

        String owner = databaseManager.getChunkOwner(chunk);
        if (!owner.equals(player.getName()) && !player.hasPermission(plugin.getConfig().getString("permissions.admin-bypass"))) {
            player.sendMessage(Utils.getMessage("settings.not-your-claim", "&cQuesto claim non ti appartiene!"));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(Utils.getMessage("invite.player-not-found", "&cIl giocatore non è online!"));
            return;
        }

        int claimId = databaseManager.getClaimId(chunk);
        if (databaseManager.isPlayerMemberOfClaim(claimId, target.getName())) {
            player.sendMessage(Utils.getMessage("invite.already-invited", "&cQuesto giocatore è già stato invitato a questo claim!"));
            return;
        }

        if (databaseManager.addMemberToClaim(claimId, target.getName())) {
            String inviteSentMsg = Utils.getMessage("invite.invite-sent", "&aHai invitato %player% al tuo claim!");
            inviteSentMsg = Utils.replacePlaceholders(inviteSentMsg, "%player%", target.getName());
            player.sendMessage(inviteSentMsg);

            String inviteReceivedMsg = Utils.getMessage("invite.invite-received", "&a%player% ti ha invitato al suo claim!");
            inviteReceivedMsg = Utils.replacePlaceholders(inviteReceivedMsg, "%player%", player.getName());
            target.sendMessage(inviteReceivedMsg);

            String playerAddedMsg = Utils.getMessage("invite.player-added", "&aHai aggiunto %player% al tuo claim!");
            playerAddedMsg = Utils.replacePlaceholders(playerAddedMsg, "%player%", target.getName());
            player.sendMessage(playerAddedMsg);
        } else {
            player.sendMessage(Utils.colorize("&cErrore durante l'invito del giocatore. Riprova più tardi."));
        }
    }

    private void listClaims(Player player) {
        int count = databaseManager.getPlayerClaimCount(player.getName());

        if (count == 0) {
            player.sendMessage(Utils.colorize("&cNon hai ancora creato nessun claim!"));
            return;
        }

        player.sendMessage(Utils.colorize("&aHai &f" + count + "&a claim."));
        player.sendMessage(Utils.colorize("&aUtilizza &f/claim info &aper vedere informazioni sul claim in cui ti trovi."));
    }

    private void showHelp(Player player) {
        player.sendMessage(Utils.colorize("&a=== BetterClaim - Aiuto ==="));
        player.sendMessage(Utils.colorize("&f/claim claim &7- Claima il chunk in cui ti trovi"));
        player.sendMessage(Utils.colorize("&f/claim unclaim &7- Rimuovi il claim dal chunk in cui ti trovi"));
        player.sendMessage(Utils.colorize("&f/claim info &7- Mostra informazioni sul claim in cui ti trovi"));
        player.sendMessage(Utils.colorize("&f/claim invite <giocatore> &7- Invita un giocatore al tuo claim"));
        player.sendMessage(Utils.colorize("&f/claim list &7- Mostra il numero dei tuoi claim"));
        player.sendMessage(Utils.colorize("&f/claim help &7- Mostra questo messaggio di aiuto"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("claim", "unclaim", "info", "invite", "list", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}