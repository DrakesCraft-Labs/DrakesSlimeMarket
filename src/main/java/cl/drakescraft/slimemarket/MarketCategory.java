package cl.drakescraft.slimemarket;

import java.util.List;
import java.util.Locale;

record MarketCategory(String id, int slot, String material, String name, List<String> lore, List<String> idFragments) {

    /** Categories match only material IDs, never display names or mutable item metadata. */
    boolean matches(String itemId) {
        final String normalizedId = itemId.toUpperCase(Locale.ROOT);
        return idFragments.stream()
            .map(fragment -> fragment.toUpperCase(Locale.ROOT))
            .anyMatch(normalizedId::contains);
    }

}
