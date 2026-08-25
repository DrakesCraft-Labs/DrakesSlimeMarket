package cl.drakescraft.slimemarket;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EconomySnapshotService {
    private final DrakesSlimeMarket plugin;
    private final Economy economy;
    private boolean warnedAboutSBank;

    EconomySnapshotService(DrakesSlimeMarket plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    /** Mide todas las wallets conocidas y todos los depositos persistidos de sBank. */
    EconomySnapshot capture() {
        double walletTotal = 0.0D;
        int walletAccounts = 0;
        final Set<String> excludedNames = new HashSet<>(plugin.getConfig()
            .getStringList("pricing.circulation.excluded-usernames").stream()
            .map(String::toLowerCase)
            .toList());

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            final String name = player.getName();
            final boolean excluded = name != null && excludedNames.contains(name.toLowerCase());
            final boolean hasEconomyAccount;
            try {
                hasEconomyAccount = economy.hasAccount(player);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("No se pudo comprobar la wallet de "
                    + (name == null ? player.getUniqueId() : name) + "; se excluye del indice.");
                continue;
            }
            if (!shouldIncludeWallet(player.hasPlayedBefore(), player.isOnline(), excluded, hasEconomyAccount)) {
                continue;
            }
            final double balance = economy.getBalance(player);
            if (Double.isFinite(balance) && balance > 0.0D) {
                walletTotal += balance;
            }
            walletAccounts++;
        }

        final BankSnapshot bankSnapshot = readSBank();
        return new EconomySnapshot(walletTotal, bankSnapshot.total(), walletAccounts, bankSnapshot.accounts(),
            bankSnapshot.complete());
    }

    /**
     * Evita que un proveedor Vault materialice saldos por defecto para perfiles Bukkit
     * históricos que no existen en la economía real.
     */
    static boolean shouldIncludeWallet(boolean hasPlayedBefore, boolean online, boolean excluded, boolean hasAccount) {
        return !excluded && hasAccount && (hasPlayedBefore || online);
    }

    private BankSnapshot readSBank() {
        final Plugin sBank = Bukkit.getPluginManager().getPlugin("sBank");
        if (sBank == null || !sBank.isEnabled()) {
            return new BankSnapshot(0.0D, 0, true);
        }

        try {
            final Method getDb = sBank.getClass().getMethod("getDb");
            final Object database = getDb.invoke(null);
            final Object persisted = database == null ? null
                : database.getClass().getMethod("getAllBanks").invoke(database);
            if (persisted instanceof List<?> banks) {
                final BankSnapshot snapshot = sumBanks(banks);
                warnedAboutSBank = false;
                return new BankSnapshot(snapshot.total(), snapshot.accounts(), true);
            }

            final Method getBanks = sBank.getClass().getMethod("getBanks");
            final Object loaded = getBanks.invoke(null);
            if (loaded instanceof Map<?, ?> banks) {
                final BankSnapshot snapshot = sumBanks(banks.values());
                warnOnce("sBank no entrego su base completa; se usaran solo cuentas cargadas.");
                return new BankSnapshot(snapshot.total(), snapshot.accounts(), false);
            }
            return new BankSnapshot(0.0D, 0, false);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("sBank esta activo pero no expuso balances compatibles: " + exception.getMessage());
            return new BankSnapshot(0.0D, 0, false);
        }
    }

    private BankSnapshot sumBanks(Iterable<?> banks) throws ReflectiveOperationException {
        double total = 0.0D;
        int accounts = 0;
        for (Object bank : banks) {
            if (bank == null) {
                continue;
            }
            final Object balance = bank.getClass().getMethod("getBalance").invoke(bank);
            if (balance instanceof Number number && Double.isFinite(number.doubleValue())) {
                total += Math.max(0.0D, number.doubleValue());
            }
            accounts++;
        }
        return new BankSnapshot(total, accounts, true);
    }

    private void warnOnce(String message) {
        if (!warnedAboutSBank) {
            plugin.getLogger().warning(message);
            warnedAboutSBank = true;
        }
    }

    record EconomySnapshot(double walletTotal, double bankTotal, int walletAccounts, int bankAccounts,
                           boolean bankSnapshotComplete) {
        double totalWealth() {
            return walletTotal + bankTotal;
        }
    }

    private record BankSnapshot(double total, int accounts, boolean complete) {
    }
}
