package net.hyper.immortalcultivation.common.interfaces;

/**
 * Unified cultivation interface supporting multiple cultivation paths
 */
public interface ICultivationData {

    enum CultivationType {
        QI("Qi"),
        BODY("Body");

        private final String displayName;
        CultivationType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // === Core === //

    long getBaseCultivation(CultivationType type);
    long getCurrentCultivation(CultivationType type);
    void setCurrentCultivation(CultivationType type, long value);
    long getMaxCultivation(CultivationType type);
    void setMaxCultivation(CultivationType type, long value);
    void addCultivation(CultivationType type, long amount, boolean respectMaximum);
    void subtractCultivation(CultivationType type, long amount, boolean respectMinimum);
    double getProgress(CultivationType type);

    // === Rates === //

    double getBaseCultivationRate(CultivationType type);
    double getEffectiveCultivationRate(CultivationType type);

    // === Fractional Accumulation === //

    double getRegenFraction(CultivationType type);
    void setRegenFraction(CultivationType type, double fraction);
    void addRegenFraction(CultivationType type, double amount);
}