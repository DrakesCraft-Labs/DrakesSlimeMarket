package cl.drakescraft.slimemarket;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** Expone un unico indice economico para UltimateShop, HUDs y menus externos. */
final class MarketPlaceholderExpansion extends PlaceholderExpansion {
    private final DrakesSlimeMarket plugin;
    private final DynamicPricing pricing;

    MarketPlaceholderExpansion(DrakesSlimeMarket plugin, DynamicPricing pricing) {
        this.plugin = plugin;
        this.pricing = pricing;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "drakesmarket";
    }

    @Override
    public @NotNull String getAuthor() {
        return "JackStar6677-1";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        final DynamicPricing.MarketStats stats = pricing.stats();
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "buy_factor" -> decimal(stats.buyFactor());
            case "sell_factor" -> decimal(stats.sellFactor());
            case "total_wealth" -> decimal(stats.totalWealth());
            case "wallet_wealth" -> decimal(stats.walletWealth());
            case "bank_wealth" -> decimal(stats.bankWealth());
            case "updated_at" -> Long.toString(stats.refreshedAt());
            default -> null;
        };
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
