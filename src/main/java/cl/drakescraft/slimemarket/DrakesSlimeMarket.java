package cl.drakescraft.slimemarket;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class DrakesSlimeMarket extends JavaPlugin {

    private MarketCatalog catalog;
    private DynamicPricing pricing;
    private MarketAuditLogger auditLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            getLogger().severe("Vault no entrego una economia; deshabilitando mercado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        final Economy economy = provider.getProvider();
        catalog = new MarketCatalog(this);
        pricing = new DynamicPricing(this, catalog, new EconomySnapshotService(this, economy));
        auditLogger = new MarketAuditLogger(this);
        final MarketMenu menu = new MarketMenu(this, economy, catalog, pricing, auditLogger);
        final PluginCommand command = getCommand("sfmercado");
        if (command == null) {
            getLogger().severe("plugin.yml no registro /sfmercado; deshabilitando mercado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        command.setExecutor(menu);
        command.setTabCompleter(menu);
        getServer().getPluginManager().registerEvents(menu, this);

        final long refreshTicks = Math.max(60L, getConfig().getLong("pricing.refresh-seconds", 1800L)) * 20L;
        getServer().getScheduler().runTaskTimer(this, this::refreshMarket, 40L, refreshTicks);
        getLogger().info("Mercado cargado; el catalogo se construira desde el registro real de Slimefun.");
    }

    @Override
    public void onDisable() {
        if (auditLogger != null) {
            auditLogger.close();
        }
    }

    /** Recarga reglas, vuelve a descubrir materiales y publica una ventana de precios nueva. */
    void reloadMarket() {
        reloadConfig();
        catalog.reloadPolicy();
        refreshMarket();
    }

    /** Ejecuta el refresco sincronizado porque el registro de Slimefun pertenece al hilo del servidor. */
    void refreshMarket() {
        try {
            catalog.refresh();
            pricing.refresh();
        } catch (RuntimeException exception) {
            getLogger().severe("No se pudo refrescar el mercado: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
