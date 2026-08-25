package cl.drakescraft.slimemarket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EconomySnapshotServiceTest {

    /** Verifica que el indice no cuente perfiles Bukkit sin wallet real. */
    @Test
    void excludesHistoricalProfilesWithoutEconomyAccount() {
        assertFalse(EconomySnapshotService.shouldIncludeWallet(true, false, false, false));
    }

    /** Las cuentas reales, incluso conectadas por primera vez, siguen participando. */
    @Test
    void includesKnownOrOnlineEconomyAccountsUnlessExcluded() {
        assertTrue(EconomySnapshotService.shouldIncludeWallet(true, false, false, true));
        assertTrue(EconomySnapshotService.shouldIncludeWallet(false, true, false, true));
        assertFalse(EconomySnapshotService.shouldIncludeWallet(true, false, true, true));
    }
}
