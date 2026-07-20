package cl.drakescraft.slimemarket;

import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

final class MarketAuditLogger {
    private static final String HEADER = "timestamp,uuid,player,item_id,addon,amount,unit_price,total,balance_after\n";
    private final DrakesSlimeMarket plugin;
    private final Path auditFile;
    private final ConcurrentLinkedQueue<String> pending = new ConcurrentLinkedQueue<>();
    private final Object fileLock = new Object();

    MarketAuditLogger(DrakesSlimeMarket plugin) {
        this.plugin = plugin;
        this.auditFile = plugin.getDataFolder().toPath().resolve("audit").resolve("purchases.csv");
        final long flushTicks = Math.max(2L, plugin.getConfig().getLong("audit.flush-seconds", 10L)) * 20L;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::flushSafely, flushTicks, flushTicks);
    }

    /** Encola una linea CSV inmutable para no bloquear el tick de compra con acceso a disco. */
    void purchase(Player player, CatalogEntry entry, int amount, double unitPrice, double total, double balanceAfter) {
        pending.add(String.join(",",
            csv(Instant.now().toString()),
            csv(player.getUniqueId().toString()),
            csv(player.getName()),
            csv(entry.id()),
            csv(entry.addon()),
            Integer.toString(amount),
            money(unitPrice),
            money(total),
            money(balanceAfter)
        ));
    }

    void close() {
        flushSafely();
    }

    private void flushSafely() {
        final List<String> batch = new ArrayList<>();
        String line;
        while ((line = pending.poll()) != null) {
            batch.add(line);
        }
        if (batch.isEmpty()) {
            return;
        }

        synchronized (fileLock) {
            try {
                Files.createDirectories(auditFile.getParent());
                if (Files.notExists(auditFile)) {
                    Files.writeString(auditFile, HEADER, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                }
                Files.write(auditFile, batch, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } catch (IOException exception) {
                batch.forEach(pending::add);
                plugin.getLogger().severe("No se pudo escribir la auditoria del mercado: " + exception.getMessage());
            }
        }
    }

    private static String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String money(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
