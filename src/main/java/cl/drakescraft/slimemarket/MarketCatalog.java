package cl.drakescraft.slimemarket;

import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
        final Map<String, Integer> inspectedByAddon = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

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
            inspectedByAddon.merge(addon, 1, Integer::sum);
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

        addVanillaEntries(discovered, countsByAddon);

        final List<CatalogEntry> sorted = new ArrayList<>(discovered.values());
        sorted.sort(Comparator.comparing(CatalogEntry::addon, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CatalogEntry::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CatalogEntry::id));
        entries = List.copyOf(sorted);
        entriesById = Map.copyOf(discovered);
        plugin.getLogger().info("Catalogo actualizado: " + entries.size() + " materiales seguros; "
            + countsByAddon.size() + " de " + inspectedByAddon.size() + " addons detectados publican ofertas "
            + countsByAddon + ".");
    }

    List<CatalogEntry> entries() {
        return entries;
    }

    /** Returns a curated slice of the catalog for one player-facing category. */
    List<CatalogEntry> entriesForCategory(MarketCategory category) {
        return entries.stream()
            .filter(entry -> category.matches(entry.id()))
            .toList();
    }

    List<MarketCategory> categories() {
        final ConfigurationSection section = plugin.getConfig().getConfigurationSection("catalog.categories");
        if (section == null) {
            return defaultCategories();
        }

        final List<MarketCategory> categories = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            final ConfigurationSection category = section.getConfigurationSection(id);
            if (category == null) {
                continue;
            }
            categories.add(new MarketCategory(
                id,
                category.getInt("slot", -1),
                category.getString("material", "CHEST"),
                category.getString("name", id),
                category.getStringList("lore"),
                category.getStringList("id-fragments")
            ));
        }
        return categories.isEmpty() ? defaultCategories() : categories;
    }

    /** Mantiene el mercado seguro y navegable incluso al arrancar con una configuración previa. */
    private static List<MarketCategory> defaultCategories() {
        return List.of(
            new MarketCategory("basicos", 20, "REDSTONE", "&aMateriales básicos",
                List.of("&7Polvos, minerales y restos.", "&8Compra: &f%count% materiales"),
                List.of("DUST", "POWDER", "NUGGET", "ORE", "SCRAP")),
            new MarketCategory("componentes", 22, "IRON_INGOT", "&eComponentes",
                List.of("&7Lingotes y piezas intermedias.", "&8Compra: &f%count% materiales"),
                List.of("INGOT", "PLATE", "ALLOY", "FIBER", "CLOTH", "RUBBER", "PLASTIC", "CARBON", "SILICON", "MAGNESIUM", "ALUMINUM", "ALUMINIUM", "LEAD", "TIN", "ZINC", "SILVER")),
            new MarketCategory("raros", 24, "AMETHYST_SHARD", "&dMateriales raros",
                List.of("&7Gemas y componentes escasos.", "&8Compra: &f%count% materiales"),
                List.of("GEM", "ESSENCE", "SHARD", "CRYSTAL"))
        );
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

    /** Adds only the explicit vanilla building whitelist; the marketplace never enumerates all materials. */
    private void addVanillaEntries(Map<String, CatalogEntry> discovered, Map<String, Integer> countsByAddon) {
        final ConfigurationSection vanilla = plugin.getConfig().getConfigurationSection("vanilla-catalog");
        if (vanilla == null || !vanilla.getBoolean("enabled", false)) {
            return;
        }

        final ConfigurationSection categories = vanilla.getConfigurationSection("categories");
        if (categories == null) {
            return;
        }

        for (String categoryId : categories.getKeys(false)) {
            final ConfigurationSection category = categories.getConfigurationSection(categoryId);
            if (category == null) {
                continue;
            }

            final double basePrice = Math.max(0.01D, category.getDouble("base-price", 1.0D));
            for (String materialName : category.getStringList("materials")) {
                final Material material = Material.matchMaterial(materialName);
                if (material == null || !material.isItem() || material.isAir()) {
                    plugin.getLogger().warning("Material vanilla invalido ignorado: " + materialName);
                    continue;
                }

                final String id = "VANILLA_" + categoryId.toUpperCase(Locale.ROOT) + "_" + material.name();
                final String displayName = prettyMaterialName(material);
                discovered.put(id, new CatalogEntry(id, "Vanilla", displayName, new ItemStack(material), basePrice));
                countsByAddon.merge("Vanilla", 1, Integer::sum);
            }
        }
    }

    private static String prettyMaterialName(Material material) {
        final String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        final StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }
}
