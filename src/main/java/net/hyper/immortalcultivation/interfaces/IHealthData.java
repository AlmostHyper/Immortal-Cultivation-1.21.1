package net.hyper.immortalcultivation.interfaces;

import net.minecraft.nbt.CompoundTag;

public interface IHealthData {

    // === Health === //

    /**
     * Gets the current Health amount
     * @return Current Health value
     */
    long getCurrentHealth();

    /**
     * Sets the current Health amount (clamped between 0 and max)
     * @param health The new Health amount
     */
    void setCurrentHealth(long health);

    /**
     * Adds Health to the current amount (respects maximum)
     * @param amount Amount to add
     * @return Actual amount added
     */
    default long addHealth(long amount) {
        long current = getCurrentHealth();
        long max = getMaxHealth();
        long newAmount = Math.min(current + amount, max);
        setCurrentHealth(newAmount);
        return newAmount - current;
    }

    /**
     * Consumes Health from the current amount
     * FIXED: Now returns actual amount consumed, not requested amount
     *
     * @param amount Amount to consume
     * @return Actual amount consumed (may be less than requested if insufficient health)
     */
    default long consumeHealth(long amount) {
        long current = getCurrentHealth();
        long toConsume = Math.min(current, amount);
        setCurrentHealth(current - toConsume);
        return toConsume; // ✅ Return actual consumed amount
    }

    /**
     * Tries to consume Health, consuming as much as possible
     * @param amount Desired amount to consume
     * @return Actual amount consumed
     */
    default long tryConsumeHealth(long amount) {
        long current = getCurrentHealth();
        long toConsume = Math.min(current, amount);
        setCurrentHealth(current - toConsume);
        return toConsume;
    }

    /**
     * Checks if the player has enough Health
     * @param amount Amount to check
     * @return True if player has at least this much Health
     */
    default boolean hasHealth(long amount) {
        return getCurrentHealth() >= amount;
    }

    /**
     * Gets the Health as a percentage of maximum (0.0 to 1.0)
     * @return Health percentage
     */
    default double getHealthPercentage() {
        long max = getMaxHealth();
        return max > 0 ? (double) getCurrentHealth() / max : 0.0;
    }

    // === Regeneration === //

    /**
     * Gets the base Health regeneration rate per second
     * @return Regeneration rate
     */
    double getBaseHealthRegenerationRate();

    /**
     * Gets the base circulating Health regeneration rate per second
     * @return Circulating regeneration rate
     */
    double getBaseCirculatingHealthRegenerationRate();

    /**
     * Gets the effective Health regeneration rate (including modifiers)
     * @return Effective regeneration rate
     */
    default double getEffectiveHealthRegenerationRate() {
        return getBaseHealthRegenerationRate(); // Override for modifiers
    }

    /**
     * Gets the effective circulating Health regeneration rate (including modifiers)
     * @return Effective circulating regeneration rate
     */
    default double getEffectiveCirculatingHealthRegenerationRate() {
        return getBaseCirculatingHealthRegenerationRate(); // Override for modifiers
    }

    /**
     * Checks if Health regeneration is currently active
     * @return True if Health should regenerate
     */
    default boolean canRegenerateHealth() {
        return getCurrentHealth() < getMaxHealth();
    }

    /**
     * Gets the fraction of current regeneration
     * @return Fractional regeneration rate
     */
    double getRegenFraction();

    /**
     * Sets the fraction of current regeneration
     * @param fraction The new fractional amount
     */
    void setRegenFraction(double fraction);

    /**
     * Adds to fractional rate
     */
    default void addRegenFraction(double amount) {
        setRegenFraction(getRegenFraction() + amount);
    }

    // === Capacity === //

    /**
     * Gets the base maximum Health (without modifiers)
     * @return Base maximum Health
     */
    long getBaseMaxHealth();

    /**
     * Gets the effective maximum Health (including modifiers)
     * Default implementation returns base max Health - override for modifiers
     * @return Effective maximum Health
     */
    default long getMaxHealth() {
        return getBaseMaxHealth(); // Override for modifiers
    }

    // === State  === //

    /**
     * Checks if Health is at maximum capacity
     * @return True if Health is full
     */
    default boolean isHealthFull() {
        return getCurrentHealth() >= getMaxHealth();
    }

    /**
     * Checks if Health is completely empty
     * @return True if Health is empty
     */
    default boolean isHealthEmpty() {
        return getCurrentHealth() <= 0;
    }

    /**
     * Gets remaining Health capacity
     * @return How much more Health can be stored
     */
    default long getRemainingHealthCapacity() {
        return getMaxHealth() - getCurrentHealth();
    }

    // === Tick === //

    /**
     * Called every tick to update Health state
     */
    default void tick() {
        // Only cultivate if not at max capacity
        if (canRegenerateHealth()) {
            double regenRate = getEffectiveHealthRegenerationRate();

            if (regenRate > 0) {
                // Use the built-in regen fraction methods
                addRegenFraction(regenRate / 20.0); // Convert per-second to per-tick

                if (getRegenFraction() >= 1.0) {
                    long cultivationToAdd = (long) getRegenFraction();
                    setRegenFraction(getRegenFraction() - cultivationToAdd);
                    addHealth(cultivationToAdd);
                }
            }
        }
    }

    // === Events === //

    /**
     * Called when Health changes (for syncing, effects, etc.)
     * @param oldHealth Previous Health amount
     * @param newHealth New Health amount
     */
    default void onHealthChanged(long oldHealth, long newHealth) {
    }

    /**
     * Called when Health is consumed
     * @param amount Amount consumed
     */
    default void onHealthConsumed(long amount) {
    }

    /**
     * Called when Health is added/regenerated
     * @param amount Amount added
     */
    default void onHealthAdded(long amount) {
    }
}