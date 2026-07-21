package cl.drakescraft.slimemarket;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class MaterialPolicy {
    private final Set<String> allowedIds;
    private final List<String> allowedFragments;
    private final Set<String> allowedAddons;
    private final List<String> blockedAddons;
    private final List<String> blockedPrefixes;
    private final List<String> blockedFragments;
    private final List<String> blockedClasses;
    private final List<String> blockedMaterials;

    MaterialPolicy(FileConfiguration config) {
        this(
            config.getStringList("catalog.allowed-ids"),
            config.getStringList("catalog.allowed-id-fragments"),
            config.getStringList("catalog.allowed-addons"),
            config.getStringList("catalog.blocked-addons"),
            config.getStringList("catalog.blocked-id-prefixes"),
            config.getStringList("catalog.blocked-id-fragments"),
            config.getStringList("catalog.blocked-class-fragments"),
            config.getStringList("catalog.blocked-material-fragments")
        );
    }

    MaterialPolicy(
        List<String> allowedIds,
        List<String> allowedFragments,
        List<String> allowedAddons,
        List<String> blockedAddons,
        List<String> blockedPrefixes,
        List<String> blockedFragments,
        List<String> blockedClasses,
        List<String> blockedMaterials
    ) {
        this.allowedIds = new HashSet<>(normalize(allowedIds));
        this.allowedFragments = normalize(allowedFragments);
        this.allowedAddons = new HashSet<>(normalize(allowedAddons));
        this.blockedAddons = normalize(blockedAddons);
        this.blockedPrefixes = normalize(blockedPrefixes);
        this.blockedFragments = normalize(blockedFragments);
        this.blockedClasses = normalize(blockedClasses);
        this.blockedMaterials = normalize(blockedMaterials);
    }

    /** Aplica denegaciones antes de cualquier lista permitida para que un override no venda equipo peligroso. */
    boolean isAllowed(String id, String addon, String className, String materialName, boolean explicitlyConfigured) {
        final String normalizedId = normalize(id);
        final String normalizedAddon = normalize(addon);
        final String normalizedClass = normalize(className);
        final String normalizedMaterial = normalize(materialName);

        if (containsAny(normalizedAddon, blockedAddons)
            || startsWithAny(normalizedId, blockedPrefixes)
            || containsAny(normalizedId, blockedFragments)
            || containsAny(normalizedClass, blockedClasses)
            || containsAny(normalizedMaterial, blockedMaterials)
            || isEquipmentMaterial(normalizedMaterial)) {
            return false;
        }

        return explicitlyConfigured
            || (isAllowedAddon(normalizedAddon)
            && (allowedIds.contains(normalizedId) || containsAny(normalizedId, allowedFragments)));
    }

    private static boolean isEquipmentMaterial(String material) {
        return material.endsWith("_SWORD")
            || material.endsWith("_PICKAXE")
            || material.endsWith("_AXE")
            || material.endsWith("_SHOVEL")
            || material.endsWith("_HOE")
            || material.endsWith("_HELMET")
            || material.endsWith("_CHESTPLATE")
            || material.endsWith("_LEGGINGS")
            || material.endsWith("_BOOTS")
            || Set.of("BOW", "CROSSBOW", "TRIDENT", "MACE", "ELYTRA", "SHIELD").contains(material);
    }

    private boolean isAllowedAddon(String addon) {
        return allowedAddons.isEmpty() || allowedAddons.contains(addon);
    }

    private static boolean containsAny(String value, List<String> candidates) {
        return candidates.stream().anyMatch(value::contains);
    }

    private static boolean startsWithAny(String value, List<String> candidates) {
        return candidates.stream().anyMatch(value::startsWith);
    }

    private static List<String> normalize(List<String> values) {
        return values.stream().map(MaterialPolicy::normalize).filter(value -> !value.isBlank()).toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).trim();
    }
}
