package net.hyper.immortalcultivation.cultivation;

import net.hyper.immortalcultivation.cultivation.combat.CultivationDamage;

public interface ICultivationDamageHandler {

    /**
     * Handle incoming cultivation damage with full precision
     * @param damage The cultivation damage instance
     * @return true if damage was successfully applied
     */
    boolean handleCultivationDamage(CultivationDamage damage);

    /**
     * Get the entity's current health as a long for precision
     * @return Current health in cultivation units
     */
    long getCultivationHealth();

    /**
     * Get maximum health as a long
     * @return Maximum health in cultivation units
     */
    long getMaxCultivationHealth();

    /**
     * Set health with long precision
     * @param health New health value
     */
    void setCultivationHealth(long health);

    /**
     * Check if entity can take this type of damage
     * @param damageType The cultivation damage type
     * @return true if vulnerable to this damage type
     */
    default boolean isVulnerableTo(String damageType) {
        return true;
    }

    /**
     * Get damage resistance for a specific type (0.0+ where 1.0+ = immune but reducible)
     * @param damageType The damage type to check
     * @return Resistance value
     */
    default float getDamageResistance(String damageType) {
        return 0.0f; // Default: no resistance to anything
    }

    /**
     * @param damageType Which damage type to modify resistance for
     * @param modifier How much to change resistance (+/- values)
     * @param duration How long the modifier lasts (ticks, -1 = permanent)
     */
    default void addResistanceModifier(String damageType, float modifier, int duration) {
        // Default implementation does nothing - override for custom behavior!
        // This lets simple entities ignore the advanced system while
        // complex entities (players, bosses) can implement full modifier support!
    }

    /**
     * @param damageType The damage type to remove modifiers from
     * @param modifierId Unique identifier for the specific modifier to remove
     */
    default void removeResistanceModifier(String damageType, String modifierId) {
        // Default implementation does nothing
    }

    /**
     * GET FINAL CALCULATED RESISTANCE (After all modifiers)
     * It takes the base resistance, applies all active modifiers, and gives
     * you the final effective resistance value.
     * ADVANCED CALCULATION EXAMPLE:
     * - Base fire resistance: 0.8f (80%)
     * - Fire protection potion: +0.3f
     * - Armor-piercing debuff: -0.5f
     * - Final resistance: 0.8 + 0.3 - 0.5 = 0.6f (60%)
     *
     * @param damageType The damage type to calculate final resistance for
     * @return Final resistance after all modifications (can be over 1.0f!)
     */
    default float getFinalResistance(String damageType) {
        // Default implementation just returns base resistance
        // Advanced implementations will factor in all active modifiers!
        return getDamageResistance(damageType);
    }

}
