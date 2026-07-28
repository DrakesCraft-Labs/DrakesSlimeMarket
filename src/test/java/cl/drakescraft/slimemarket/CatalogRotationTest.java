package cl.drakescraft.slimemarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class CatalogRotationTest {
    @Test
    void capsLargeCategoriesAndKeepsTheSameWindowDeterministic() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("catalog.rotation.enabled", true);
        config.set("catalog.rotation.refresh-seconds", 1800L);
        config.set("catalog.rotation.offers-per-category", 9);
        CatalogRotation rotation = new CatalogRotation(config);
        List<CatalogEntry> entries = java.util.stream.IntStream.range(0, 20)
            .mapToObj(index -> new CatalogEntry("DUST_" + index, "Addon", "Dust " + index, null, 1D))
            .toList();

        List<CatalogEntry> first = rotation.select("basicos", entries);
        List<CatalogEntry> second = rotation.select("basicos", entries);
        assertEquals(9, first.size());
        assertEquals(first.stream().map(CatalogEntry::id).toList(), second.stream().map(CatalogEntry::id).toList());
    }

    @Test
    void leavesSmallCategoriesComplete() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("catalog.rotation.offers-per-category", 9);
        CatalogRotation rotation = new CatalogRotation(config);
        List<CatalogEntry> entries = List.of(new CatalogEntry("IRON_DUST", "Slimefun", "Iron Dust", null, 45D));

        assertTrue(rotation.select("basicos", entries).containsAll(entries));
    }
}