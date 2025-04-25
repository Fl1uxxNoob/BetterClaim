package net.fliuxx.betterClaim.utils;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class Utils {

    /**
     * Colora un messaggio con i codici di colore di Minecraft
     *
     * @param message Il messaggio da colorare
     * @return Il messaggio colorato
     */
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Ottiene le coordinate del chunk in formato leggibile
     *
     * @param chunk Il chunk
     * @return Una stringa con le coordinate del chunk
     */
    public static String getChunkCoordinates(Chunk chunk) {
        return "(" + chunk.getX() + ", " + chunk.getZ() + ") nel mondo " + chunk.getWorld().getName();
    }

    /**
     * Ottiene un messaggio dalla configurazione e lo colora
     *
     * @param path Il percorso nella configurazione
     * @param defaultMessage Il messaggio predefinito se non trovato
     * @return Il messaggio colorato
     */
    public static String getMessage(String path, String defaultMessage) {
        String message = net.fliuxx.betterClaim.BetterClaim.getInstance().getConfig().getString(path, defaultMessage);
        return colorize(message);
    }

    /**
     * Sostituisce i placeholder in un messaggio
     *
     * @param message Il messaggio con i placeholder
     * @param placeholders Array di placeholder e valori (es. "%player%", "Fl1uxxNoob")
     * @return Il messaggio con i placeholder sostituiti
     */
    public static String replacePlaceholders(String message, String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Il numero di parametri deve essere pari");
        }

        String result = message;
        for (int i = 0; i < placeholders.length; i += 2) {
            result = result.replace(placeholders[i], placeholders[i + 1]);
        }
        return result;
    }

    /**
     * Invia un messaggio colorato al giocatore
     *
     * @param player Il giocatore
     * @param message Il messaggio da inviare
     */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(colorize(message));
    }
}