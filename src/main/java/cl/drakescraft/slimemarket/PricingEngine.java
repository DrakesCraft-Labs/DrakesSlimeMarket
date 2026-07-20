package cl.drakescraft.slimemarket;

final class PricingEngine {
    private PricingEngine() {
    }

    /** Combina circulacion, demanda y pulso acotado sin permitir precios negativos o explosivos. */
    static double calculate(
        double basePrice,
        long demand,
        double totalWealth,
        double referenceWealth,
        double minimumFactor,
        double maximumFactor,
        double demandStep,
        double maximumDemandFactor,
        double pulseFactor
    ) {
        final double safeReference = Math.max(1.0D, referenceWealth);
        final double wealthRatio = Math.max(0.0D, totalWealth) / safeReference;
        final double wealthFactor = 0.90D + (Math.log1p(wealthRatio) / Math.log(11.0D)) * 0.65D;
        final double demandFactor = Math.min(maximumDemandFactor, 1.0D + Math.max(0L, demand) * demandStep);
        final double combined = clamp(wealthFactor * demandFactor * pulseFactor, minimumFactor, maximumFactor);
        return Math.max(0.01D, Math.round(basePrice * combined * 100.0D) / 100.0D);
    }

    static double pulse(String id, long window, double percent) {
        final int bucket = Math.floorMod((id + ':' + window).hashCode(), 2001);
        final double normalized = (bucket / 1000.0D) - 1.0D;
        return 1.0D + normalized * Math.max(0.0D, percent) / 100.0D;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
