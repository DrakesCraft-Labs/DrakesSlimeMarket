package cl.drakescraft.slimemarket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingEngineTest {

    @Test
    void demandAndWealthIncreasePriceInsideConfiguredCeiling() {
        final double price = PricingEngine.calculate(100.0D, 40L, 500_000_000.0D, 100_000_000.0D,
            0.85D, 1.85D, 0.02D, 1.45D, 1.03D);

        assertTrue(price > 100.0D);
        assertTrue(price <= 185.0D);
    }

    @Test
    void priceCannotFallBelowFloor() {
        final double price = PricingEngine.calculate(100.0D, 0L, 0.0D, 100_000_000.0D,
            0.85D, 1.85D, 0.02D, 1.45D, 0.5D);

        assertEquals(85.0D, price);
    }

    @Test
    void pulseIsStableInsideWindow() {
        final double first = PricingEngine.pulse("COPPER_INGOT", 42L, 3.0D);
        final double second = PricingEngine.pulse("COPPER_INGOT", 42L, 3.0D);

        assertEquals(first, second);
        assertTrue(first >= 0.97D && first <= 1.03D);
    }
}
