package cl.drakescraft.slimemarket.bridge;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * DrakesSlimeMarket Java 21 Project Panama (FFM API) Native Rust Bridge
 * High-Performance Native Market Price Indexer & Item Trade Calculator.
 */
public final class RustNativeBridge {
    private static final Logger LOGGER = Logger.getLogger("DrakesSlimeMarket-RustBridge");
    private static boolean isNativeLoaded = false;
    private static MethodHandle solveEnergyTickMH;

    public static void initialize(Path nativeLibPath) {
        try {
            System.load(nativeLibPath.toAbsolutePath().toString());
            SymbolLookup lookup = SymbolLookup.loaderLookup();
            Linker linker = Linker.nativeLinker();

            MemorySegment symbol = lookup.find("slimefun_solve_energy_tick").orElse(null);
            if (symbol != null) {
                solveEnergyTickMH = linker.downcallHandle(symbol, FunctionDescriptor.of(ValueLayout.JAVA_LONG));
                isNativeLoaded = true;
                LOGGER.info("⚡ [DrakesSlimeMarket] Successfully bound to Slimefun-Rust Native FFM Market Engine!");
            }
        } catch (Throwable t) {
            LOGGER.warning("⚠️ [DrakesSlimeMarket] Slimefun-Rust native library not loaded: " + t.getMessage());
        }
    }

    public static long solveEnergyTick() {
        if (isNativeLoaded && solveEnergyTickMH != null) {
            try {
                return (long) solveEnergyTickMH.invokeExact();
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    public static boolean isNativeLoaded() {
        return isNativeLoaded;
    }
}
