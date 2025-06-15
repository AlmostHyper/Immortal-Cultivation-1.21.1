package net.hyper.immortalcultivation.cultivation;

import net.minecraft.nbt.CompoundTag;

public interface IHealthData {

    // === Core Data ===

    /**
     * Deserializes Health data from NBT
     * @param tag The NBT compound tag containing the data
     */
    void deserializeNBT(CompoundTag tag);

    /**
     * Serializes Health data to NBT
     * @return CompoundTag containing the serialized data
     */
    CompoundTag serializeNBT();

    // === Health Amount Methods ===

    /**
     * Gets the current health amount
     * @return Current health value
     */
    long getCurrentHealth();

    /**
     * Sets the current health amount (clamped between 0 and max)
     * @param health The new health amount
     */
    void setCurrentHealth(long health);

    /**
     * Adds health to the current amount (respects maximum)
     * @param amount Amount to add
     * @return Actual amount added
     */
    default long addHealth(double amount) {
        long current = getCurrentHealth();
        long max = getMaxHealth();
        long newAmount = (long) Math.min(current + amount, max);
        setCurrentHealth(newAmount);
        return newAmount - current; // Return actual amount added
    }

    /**
     * Damages the player by reducing health
     * @param amount Amount of damage to deal
     * @return true if damage was applied, false if player is already dead
     */
    default boolean damage(long amount) {
        if (getCurrentHealth() > 0) {
            setCurrentHealth(getCurrentHealth() - amount);
            return true;
        }
        return false;
    }

    /**
     * Tries to heal the player, healing as much as possible
     * @param amount Desired amount to heal
     * @return Actual amount healed
     */
    default long tryHeal(long amount) {
        long current = getCurrentHealth();
        long max = getMaxHealth();
        long toHeal = Math.min(amount, max - current);
        setCurrentHealth(current + toHeal);
        return toHeal;
    }

    /**
     * Checks if the player has enough health
     * @param amount Amount to check
     * @return true if player has at least this much health
     */
    default boolean hasHealth(long amount) {
        return getCurrentHealth() >= amount;
    }

    /**
     * Gets the health as a percentage of maximum (0.0 to 1.0)
     * @return Health percentage
     */
    default double getHealthPercentage() {
        double max = getMaxHealth();
        return max > 0 ? (double) getCurrentHealth() / max : 0.0;
    }

    // === Regeneration Methods ===

    /**
     * Gets the base health regeneration rate per second
     * @return Regeneration rate
     */
    double getBaseHealthRegenerationRate();

    /**
     * Gets the accelerated health regeneration rate per second
     * @return Accelerated regeneration rate
     */
    double getBaseAcceleratedHealthRegenerationRate();

    /**
     * Gets the effective health regeneration rate (including modifiers)
     * @return Effective regeneration rate
     */
    default double getEffectiveHealthRegenerationRate() {
        return getBaseHealthRegenerationRate(); // Override for modifiers
    }

    /**
     * Gets the effective accelerated health regeneration rate (including modifiers)
     * @return Effective accelerated regeneration rate
     */
    default double getEffectiveAcceleratedHealthRegenerationRate() {
        return getBaseAcceleratedHealthRegenerationRate(); // Override for modifiers
    }

    /**
     * Checks if health regeneration is currently active
     * @return true if health should regenerate
     */
    default boolean canRegenerateHealth() {
        return getCurrentHealth() < getMaxHealth() && isAlive();
    }

    // === Capacity Methods ===

    /**
     * Gets the base maximum health (without modifiers)
     * @return Base maximum health
     */
    long getBaseMaxHealth();

    /**
     * Gets the effective maximum health (including modifiers)
     * Default implementation returns base max health - override for modifiers
     * @return Effective maximum health
     */
    default long getMaxHealth() {
        return getBaseMaxHealth(); // Override for modifiers
    }

    // === State Methods ===

    /**
     * Checks if health is at maximum capacity
     * @return true if health is full
     */
    default boolean isHealthFull() {
        return getCurrentHealth() >= getMaxHealth();
    }

    /**
     * Checks if the player is dead (health <= 0)
     * @return true if dead
     */
    default boolean isDead() {
        return getCurrentHealth() <= 0;
    }

    /**
     * Checks if the player is alive (health > 0)
     * @return true if alive
     */
    default boolean isAlive() {
        return getCurrentHealth() > 0;
    }

    /**
     * Gets remaining health capacity
     * @return How much more health can be restored
     */
    default double getRemainingHealthCapacity() {
        return getMaxHealth() - getCurrentHealth();
    }

    /**
     * Checks if player is critically injured (health below threshold)
     * @param threshold Health threshold (default could be 25% of max)
     * @return true if critically injured
     */
    default boolean isCriticallyInjured(double threshold) {
        return getHealthPercentage() < threshold;
    }

    /**
     * Convenience method for critical injury check with default threshold
     * @return true if health is below 25% of maximum
     */
    default boolean isCriticallyInjured() {
        return isCriticallyInjured(0.25F);
    }

    // === Tick Method ===

    /**
     * Called every tick to update health state
     * Override this method for custom regeneration logic
     */
    default void tick() {
        // Only regenerate if not at max capacity and alive
        if (canRegenerateHealth()) {
            double regenRate = getEffectiveHealthRegenerationRate();

            // Handle fractional regeneration properly
            if (this instanceof HealthRegenerationState state) {
                state.addRegenFraction(regenRate / 20.0); // Convert per-second to per-tick

                if (state.getRegenFraction() >= 1.0) {
                    double healthToAdd = (double) state.getRegenFraction();
                    state.setRegenFraction(state.getRegenFraction() - healthToAdd);
                    addHealth(healthToAdd);
                }
            } else {
                // Simple regeneration without fractional tracking
                double regenAmount = (double) (regenRate / 20.0);
                if (regenAmount > 0) {
                    addHealth(regenAmount);
                }
            }
        }
    }

    // === Events/Callbacks ===

    /**
     * Called when health changes (for syncing, effects, etc.)
     * @param oldHealth Previous health amount
     * @param newHealth New health amount
     */
    default void onHealthChanged(double oldHealth, double newHealth) {
        // Override for custom behavior
    }

    /**
     * Called when player takes damage
     * @param amount Amount of damage taken
     */
    default void onDamageTaken(double amount) {
        // Override for custom behavior
    }

    /**
     * Called when player is healed
     * @param amount Amount healed
     */
    default void onHealed(double amount) {
        // Override for custom behavior
    }

    /**
     * Called when player dies (health reaches 0)
     */
    default void onDeath() {
        // Override for custom behavior
    }

    /**
     * Called when player revives (health goes from 0 to positive)
     */
    default void onRevive() {
        // Override for custom behavior
    }
}

/**
 * Helper interface for implementations that need fractional regeneration tracking
 */
interface HealthRegenerationState {
    double getRegenFraction();
    void setRegenFraction(double fraction);

    default void addRegenFraction(double amount) {
        setRegenFraction(getRegenFraction() + amount);
    }
}

