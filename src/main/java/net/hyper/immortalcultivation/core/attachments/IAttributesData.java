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

    long getAttribute(AttributeType type);
    void setAttribute(AttributeType type, long value);
    long getMaxAttribute(AttributeType type);
    void setMaxAttribute(AttributeType type, long value);

    default long addAttribute(AttributeType type, long amount) {
        long current = getAttribute(type);
        long max = getMaxAttribute(type);
        long newAmount = Math.min(current + amount, max);
        setAttribute(type, newAmount);
        return newAmount - current;
    }

    default long consumeAttribute(AttributeType type, long amount) {
        long current = getAttribute(type);
        long toConsume = Math.min(current, amount);
        setAttribute(type, current - toConsume);
        return toConsume;
    }

    default long tryConsumeAttribute(AttributeType type, long amount) {return consumeAttribute(type, amount);}
    default boolean hasAttribute(AttributeType type, long amount) {return getAttribute(type) >= amount;}

    default double getAttributePercentage(AttributeType type) {
        long max = getMaxAttribute(type);
        return max > 0 ? (double) getAttribute(type) / max : 0.0;
    }

    double getAttributeRegenRate(AttributeType type);
    default double getEffectiveAttributeRegenerationRate(AttributeType type) {return getAttributeRegenRate(type);}
    default boolean canRegenerateAttribute(AttributeType type) {return getAttribute(type) < getMaxAttribute(type);}
    double getRegenFraction(AttributeType type);
    void setRegenFraction(AttributeType type, double fraction);
    default void addRegenFraction(AttributeType type, double amount) {setRegenFraction(type, getRegenFraction(type) + amount);}

    default void onAttributeChanged(AttributeType type, long oldValue, long newValue) {}
    default void onAttributeConsumed(AttributeType type, long amount) {}
    default void onAttributeAdded(AttributeType type, long amount) {}
}