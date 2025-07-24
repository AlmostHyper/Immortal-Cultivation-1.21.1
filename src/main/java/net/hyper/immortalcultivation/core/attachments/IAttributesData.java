package net.hyper.immortalcultivation.core.attachments;

public interface IAttributesData {

    enum AttributeType {
        HEALTH("Health"),
        QI("Qi"),
        BODY("Body");

        private final String displayName;
        AttributeType(String displayName) {this.displayName = displayName;}
        public String getDisplayName() {return displayName;}
    }

    // === CORE STAT MANAGEMENT === //

    long getCurrentAttribute(AttributeType type);
    void setCurrentAttribute(AttributeType type, long value);
    long getMaxAttribute(AttributeType type);
    void setMaxAttribute(AttributeType type, long value);
    default long addAttribute(AttributeType type, long amount) {
        long current = getCurrentAttribute(type);
        long max = getMaxAttribute(type);
        long newAmount = Math.min(current + amount, max);
        setCurrentAttribute(type, newAmount);
        return newAmount - current;
    }
    default long consumeAttribute(AttributeType type, long amount) {
        long current = getCurrentAttribute(type);
        long toConsume = Math.min(current, amount);
        setCurrentAttribute(type, current - toConsume);
        return toConsume;
    }
    default long tryConsumeAttribute(AttributeType type, long amount) {
        return consumeAttribute(type, amount);
    }
    default boolean hasAttribute(AttributeType type, long amount) {
        return getCurrentAttribute(type) >= amount;
    }
    default double getAttributePercentage(AttributeType type) {
        long max = getMaxAttribute(type);
        return max > 0 ? (double) getCurrentAttribute(type) / max : 0.0;
    }

    // === REGENERATION SYSTEM === //

    double getAttributeRegenRate(AttributeType type);
    default double getEffectiveAttributeRegenerationRate(AttributeType type) {
        return getAttributeRegenRate(type);
    }
    default boolean canRegenerateAttribute(AttributeType type) {
        return getCurrentAttribute(type) < getMaxAttribute(type);
    }
    double getRegenFraction(AttributeType type);
    void setRegenFraction(AttributeType type, double fraction);
    default void addRegenFraction(AttributeType type, double amount) {
        setRegenFraction(type, getRegenFraction(type) + amount);
    }

    // === BONUSES AND MODIFIERS === //

    long getPermanentAttributeBonus(AttributeType type);
    void setPermanentAttributeBonus(AttributeType type, long bonus);
    double getPermanentAttributeRegenBonus(AttributeType type);
    void setPermanentAttributeRegenBonus(AttributeType type, double bonus);

    // === CALLBACK METHODS === //

    default void onAttributeChanged(AttributeType type, long oldValue, long newValue) {}
    default void onAttributeConsumed(AttributeType type, long amount) {}
    default void onAttributeAdded(AttributeType type, long amount) {}
}