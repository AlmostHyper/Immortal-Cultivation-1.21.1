package net.hyper.immortalcultivation.core.attachments;

public interface IAttributesData {

    enum StatType {
        HEALTH("Health"),
        QI("Qi"),
        BODY("Body");

        private final String displayName;

        StatType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // === CORE STAT MANAGEMENT === //

    long getCurrentStat(StatType type);
    void setCurrentStat(StatType type, long value);
    long getMaxStat(StatType type);
    void setMaxStat(StatType type, long value);
    default long addStat(StatType type, long amount) {
        long current = getCurrentStat(type);
        long max = getMaxStat(type);
        long newAmount = Math.min(current + amount, max);
        setCurrentStat(type, newAmount);
        return newAmount - current;
    }
    default long consumeStat(StatType type, long amount) {
        long current = getCurrentStat(type);
        long toConsume = Math.min(current, amount);
        setCurrentStat(type, current - toConsume);
        return toConsume;
    }
    default long tryConsumeStat(StatType type, long amount) {
        return consumeStat(type, amount);
    }
    default boolean hasStat(StatType type, long amount) {
        return getCurrentStat(type) >= amount;
    }
    default double getStatPercentage(StatType type) {
        long max = getMaxStat(type);
        return max > 0 ? (double) getCurrentStat(type) / max : 0.0;
    }

    // === REGENERATION SYSTEM === //

    double getStatRegenRate(StatType type);
    default double getEffectiveStatRegenerationRate(StatType type) {
        return getStatRegenRate(type);
    }
    default boolean canRegenerateStat(StatType type) {
        return getCurrentStat(type) < getMaxStat(type);
    }
    double getRegenFraction(StatType type);
    void setRegenFraction(StatType type, double fraction);
    default void addRegenFraction(StatType type, double amount) {
        setRegenFraction(type, getRegenFraction(type) + amount);
    }

    // === BONUSES AND MODIFIERS === //

    long getPermanentStatBonus(StatType type);
    void setPermanentStatBonus(StatType type, long bonus);
    double getPermanentStatRegenBonus(StatType type);
    void setPermanentStatRegenBonus(StatType type, double bonus);

    // === CALLBACK METHODS === //

    default void onStatChanged(StatType type, long oldValue, long newValue) {}
    default void onStatConsumed(StatType type, long amount) {}
    default void onStatAdded(StatType type, long amount) {}
}