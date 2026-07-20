package cl.drakescraft.slimemarket;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class DrakesSlimeMarket extends JavaPlugin {

    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            getLogger().severe("Vault no entregó una economía; deshabilitando mercado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        economy = provider.getProvider();
        final MarketMenu menu = new MarketMenu(this, economy);
        getCommand("sfmercado").setExecutor(menu);
        getServer().getPluginManager().registerEvents(menu, this);
    }
}
