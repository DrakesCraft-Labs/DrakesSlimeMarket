package cl.drakescraft.slimemarket;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import io.github.thebusybiscuit.slimefun4.api.services.NativeAccelerationService;

final class DynamicPricing {
    private final DrakesSlimeMarket plugin;
    private final MarketCatalog catalog;
    private final EconomySnapshotService snapshots;
    private final NativeAccelerationService nativeAcceleration;
    private final Map<String, LongAdder> demand = new ConcurrentHashMap<>();
    private volatile Map<String, Double> prices = Map.of();
    private volatile EconomySnapshotService.EconomySnapshot lastSnapshot =
        new EconomySnapshotService.EconomySnapshot(0.0D, 0.0D, 0, 0, false);
    private volatile long lastRefreshEpochSecond;

    DynamicPricing(
        DrakesSlimeMarket plugin,
        MarketCatalog catalog,
        EconomySnapshotService snapshots,
        NativeAccelerationService nativeAcceleration
    ) {
        this.plugin = plugin;
        this.catalog = catalog;
        this.snapshots = snapshots;
        this.nativeAcceleration = nativeAcceleration;
    }

    /** Publica precios inmutables para que cada menu vea una ventana economica consistente. */
    void refresh() {
        final EconomySnapshotService.EconomySnapshot snapshot = snapshots.capture();
        final long refreshSeconds = Math.max(60L, plugin.getConfig().getLong("pricing.refresh-seconds", 1800L));
        final long window = Instant.now().getEpochSecond() / refreshSeconds;
        final Map<String, Double> nextPrices = new HashMap<>();

        for (CatalogEntry entry : catalog.entries()) {
            final LongAdder counter = demand.remove(entry.id());
            final long itemDemand = counter == null ? 0L : counter.sum();
            final double pulse = PricingEngine.pulse(entry.id(), window, plugin.getConfig().getDouble("pricing.pulse-percent", 3.0D));
            final double referenceWealth = plugin.getConfig().getDouble("pricing.reference-wealth", 100_000_000.0D);
            final double minimumFactor = plugin.getConfig().getDouble("pricing.minimum-factor", 0.85D);
            final double maximumFactor = plugin.getConfig().getDouble("pricing.maximum-factor", 1.85D);
            final double demandStep = plugin.getConfig().getDouble("pricing.demand-step", 0.02D);
            final double maximumDemandFactor =
                plugin.getConfig().getDouble("pricing.maximum-demand-factor", 1.45D);
            final double price = nativeAcceleration != null
                ? nativeAcceleration.calculateMarketPrice(
                    entry.basePrice(), itemDemand, snapshot.totalWealth(), referenceWealth,
                    minimumFactor, maximumFactor, demandStep, maximumDemandFactor, pulse
                )
                : PricingEngine.calculate(
                    entry.basePrice(), itemDemand, snapshot.totalWealth(), referenceWealth,
                    minimumFactor, maximumFactor, demandStep, maximumDemandFactor, pulse
                );
            nextPrices.put(entry.id(), price);
        }

        prices = Map.copyOf(nextPrices);
        lastSnapshot = snapshot;
        lastRefreshEpochSecond = Instant.now().getEpochSecond();
        plugin.getLogger().info("Precios publicados con circulacion observada de "
            + Math.round(snapshot.totalWealth()) + " y " + nextPrices.size() + " ofertas.");
    }

    double unitPrice(CatalogEntry entry) {
        return prices.getOrDefault(entry.id(), entry.basePrice());
    }

    void registerPurchase(String id, int amount) {
        demand.computeIfAbsent(id, ignored -> new LongAdder()).add(Math.max(1, amount / 8));
    }

    MarketStats stats() {
        final double minimumFactor = plugin.getConfig().getDouble("pricing.minimum-factor", 0.85D);
        final double maximumFactor = plugin.getConfig().getDouble("pricing.maximum-factor", 1.85D);
        final double buyFactor = PricingEngine.wealthFactor(lastSnapshot.totalWealth(),
            plugin.getConfig().getDouble("pricing.reference-wealth", 100_000_000.0D), minimumFactor, maximumFactor);
        final double sellFactor = PricingEngine.sellFactor(buyFactor,
            plugin.getConfig().getDouble("pricing.general-market.sell-elasticity", 0.55D),
            plugin.getConfig().getDouble("pricing.general-market.minimum-sell-factor", 0.90D),
            plugin.getConfig().getDouble("pricing.general-market.maximum-sell-factor", 1.45D));
        return new MarketStats(prices.size(), lastSnapshot.totalWealth(), lastSnapshot.walletTotal(),
            lastSnapshot.bankTotal(), lastSnapshot.walletAccounts(), lastSnapshot.bankAccounts(),
            lastSnapshot.bankSnapshotComplete(), buyFactor, sellFactor, lastRefreshEpochSecond);
    }

    record MarketStats(int pricedItems, double totalWealth, double walletWealth, double bankWealth,
                       int walletAccounts, int bankAccounts, boolean bankSnapshotComplete,
                       double buyFactor, double sellFactor, long refreshedAt) {
    }
}
