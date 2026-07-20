package cl.drakescraft.slimemarket;

import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

final class MarketCatalog {
    private final DrakesSlimeMarket plugin;
    private volatile List<CatalogEntry> entries = List.of();
    private volatile Map<String, CatalogEntry> entriesById = Map.of();
    private MaterialPolicy policy;

    MarketCatalog(DrakesSlimeMarket plugin) {
        this.plugin = plugin;
        reloadPolicy();
    }

    void reloadPolicy() {
        policy = new MaterialPolicy(plugin.getConfig());
    }

    /** Reconstruye el catalogo desde los items habilitados por Slimefun y cada addon ya cargado. */
    void refresh() {
        final ConfigurationSection overrides = plugin.getConfig().getConfigurationSection("catalog.entries");
        final boolean autoDiscovery = plugin.getConfig().getBoolean("catalog.auto-discovery", true);
        final Map<String, CatalogEntry> discovered = new LinkedHashMap<>();
        final Map<String, Integer> countsByAddon = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (SlimefunItem slimefunItem : Slimefun.getRegistry().getEnabledSlimefunItems()) {
            final String id = slimefunItem.getId();
            final boolean explicit = overrides != null && overrides.isConfigurationSection(id);
            if (!autoDiscovery && !explicit) {
                continue;
            }
            if (explicit && !overrides.getBoolean(id + ".enabled", true)) {
                continue;
            }

            final ItemStack prototype = slimefunItem.getItem();
            final String addon = slimefunItem.getAddon() == null ? "Slimefun" : slimefunItem.getAddon().getName();
            if (prototype == null || slimefunItem.isHidden() || slimefunItem.isDisabled()) {
                continue;
            }
            if (!policy.isAllowed(id, addon, slimefunItem.getClass().getSimpleName(), prototype.getType().name(), explicit)) {
                continue;
            }

            final double configuredPrice = explicit ? overrides.getDouble(id + ".base-price", -1.0D) : -1.0D;
            final double basePrice = configuredPrice > 0.0D ? configuredPrice : inferBasePrice(id);
            final String rawName = ChatColor.stripColor(slimefunItem.getItemName());
            final String displayName = rawName == null || rawName.isBlank() ? id : rawName;
            discovered.put(id, new CatalogEntry(id, addon, displayName, prototype.clone(), basePrice));
            countsByAddon.merge(addon, 1, Integer::sum);
        }

        final List<CatalogEntry> sorted = new ArrayList<>(discovered.values());
        sorted.sort(Comparator.comparing(CatalogEntry::addon, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CatalogEntry::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CatalogEntry::id));
        entries = List.copyOf(sorted);
        entriesById = Map.copyOf(discovered);
        plugin.getLogger().info("Catalogo actualizado: " + entries.size() + " materiales de "
            + countsByAddon.size() + " addons " + countsByAddon + ".");
    }

    List<CatalogEntry> entries() {
        return entries;
    }

    Optional<CatalogEntry> find(String id) {
        return Optional.ofNullable(entriesById.get(id));
    }

    private double inferBasePrice(String id) {
        final String normalized = id.toUpperCase(Locale.ROOT);
        final ConfigurationSection prices = plugin.getConfig().getConfigurationSection("base-prices");
        if (prices != null) {
            for (String family : prices.getKeys(false)) {
                if (!"DEFAULT".equalsIgnoreCase(family) && normalized.contains(family.toUpperCase(Locale.ROOT))) {
                    return Math.max(0.01D, prices.getDouble(family));
                }
            }
            return Math.max(0.01D, prices.getDouble("DEFAULT", 220.0D));
        }
        return 220.0D;
    }
}
