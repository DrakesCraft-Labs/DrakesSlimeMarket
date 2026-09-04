package cl.drakescraft.slimemarket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;

/** Selects a deterministic rotating subset without changing the underlying safe catalog. */
final class CatalogRotation {
    private final boolean enabled;
    private final long refreshSeconds;
    private final int offersPerCategory;

    CatalogRotation(FileConfiguration config) {
        enabled = config.getBoolean("catalog.rotation.enabled", true);
        refreshSeconds = Math.max(60L, config.getLong("catalog.rotation.refresh-seconds", 1200L));
        offersPerCategory = Math.clamp(config.getInt("catalog.rotation.offers-per-category", 14), 7, 45);
    }

    List<CatalogEntry> select(String categoryId, List<CatalogEntry> candidates) {
        if (!enabled || candidates.isEmpty()) {
            return candidates;
        }
        long window = Instant.now().getEpochSecond() / refreshSeconds;
        List<CatalogEntry> rotated = new ArrayList<>(candidates);
        rotated.sort(Comparator.<CatalogEntry>comparingLong(entry -> score(categoryId, entry.id(), window))
            .thenComparing(CatalogEntry::id));
        if (candidates.size() <= offersPerCategory) {
            return List.copyOf(rotated);
        }
        return List.copyOf(rotated.subList(0, offersPerCategory));
    }

    private static long score(String categoryId, String itemId, long window) {
        return Integer.toUnsignedLong(Objects.hash(categoryId, itemId, window));
    }
}