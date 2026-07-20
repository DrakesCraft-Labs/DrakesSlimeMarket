package cl.drakescraft.slimemarket;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;

final class EconomySnapshotService {
    private final DrakesSlimeMarket plugin;
    private final Economy economy;
    private boolean warnedAboutSBank;

    EconomySnapshotService(DrakesSlimeMarket plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    /** Mide wallets conectadas y depositos sBank sin enlazar el plugin a sus clases internas. */
    EconomySnapshot capture() {
        double walletTotal = 0.0D;
        for (Player player : Bukkit.getOnlinePlayers()) {
            walletTotal += Math.max(0.0D, economy.getBalance(player));
        }

        final BankSnapshot bankSnapshot = readSBank();
        return new EconomySnapshot(walletTotal, bankSnapshot.total(), Bukkit.getOnlinePlayers().size(), bankSnapshot.accounts());
    }

    private BankSnapshot readSBank() {
        final Plugin sBank = Bukkit.getPluginManager().getPlugin("sBank");
        if (sBank == null || !sBank.isEnabled()) {
            return new BankSnapshot(0.0D, 0);
        }

        try {
            final Method getBanks = sBank.getClass().getMethod("getBanks");
            final Object result = getBanks.invoke(null);
            if (!(result instanceof Map<?, ?> banks)) {
                return new BankSnapshot(0.0D, 0);
            }

            double total = 0.0D;
            for (Object bank : banks.values()) {
                if (bank == null) {
                    continue;
                }
                final Object balance = bank.getClass().getMethod("getBalance").invoke(bank);
                if (balance instanceof Number number) {
                    total += Math.max(0.0D, number.doubleValue());
                }
            }
            warnedAboutSBank = false;
            return new BankSnapshot(total, banks.size());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!warnedAboutSBank) {
                plugin.getLogger().warning("sBank esta activo pero no expuso balances compatibles: " + exception.getMessage());
                warnedAboutSBank = true;
            }
            return new BankSnapshot(0.0D, 0);
        }
    }

    record EconomySnapshot(double walletTotal, double bankTotal, int onlineWallets, int bankAccounts) {
        double totalWealth() {
            return walletTotal + bankTotal;
        }
    }

    private record BankSnapshot(double total, int accounts) {
    }
}
