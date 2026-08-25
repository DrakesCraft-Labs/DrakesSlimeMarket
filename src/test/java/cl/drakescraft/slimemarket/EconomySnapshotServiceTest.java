package cl.drakescraft.slimemarket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

    /** El índice prefiere las cuentas persistentes de Essentials sobre perfiles Bukkit históricos. */
    @Test
    void prefersAuthoritativeEssentialsUsersOverOnlineFallback() {
        final UUID persisted = UUID.randomUUID();
        final UUID staleOnline = UUID.randomUUID();

        assertEquals(Set.of(persisted), EconomySnapshotService.chooseWalletAccountIds(
            Optional.of(Set.of(persisted)), List.of()));
        assertEquals(Set.of(), EconomySnapshotService.chooseWalletAccountIds(Optional.of(Set.of()), List.of()));
    }
}
