package net.hyper.immortalcultivation.server.events.utils;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class CombatCalculationUtils {

    /**
     * Converts vanilla damage to custom damage using simple 1:1 scaling.
     * Maintains compatibility with vanilla and all mods.
     *
     * @param vanillaDamage Original vanilla damage
     * @param player The player taking damage
     * @return Custom damage (1:1 conversion)
     */
    private static long convertToCustomDamage(float vanillaDamage, Player player) {
        // Simple 1:1 conversion for vanilla/mod compatibility
        return Math.round(vanillaDamage);
    }

    /**
     * Converts vanilla healing to custom healing using simple 1:1 scaling.
     *
     * @param vanillaHealing Original vanilla healing
     * @param player The player being healed
     * @return Custom healing (1:1 conversion)
     */
    private static long convertToCustomHealing(float vanillaHealing, Player player) {
        // Simple 1:1 conversion - REMOVED the *100 multiplier that was causing instant heals
        return Math.round(vanillaHealing);
    }

    /**
     * Calculates immunity duration based on damage amount
     *
     * @param damage Damage taken
     * @return Immunity duration in milliseconds
     */
    private static long calculateImmunityDuration(long damage) {
        // Base immunity of 500ms, with scaling
        return 500L + Math.min(damage / 10, 1000L);
    }

    /**
     * Generates a unique key for a damage source
     *
     * @param damageSource The damage source
     * @return Unique string key
     */
    private static String generateDamageSourceKey(DamageSource damageSource) {
        if (damageSource.getEntity() != null) {
            return damageSource.type().msgId() + "_" + damageSource.getEntity().getUUID();
        }
        return damageSource.type().msgId();
    }

    /**
     * Applies damage feedback effects (knockback, hurt animation, etc.)
     *
     * @param player The player
     * @param damage Damage amount
     * @param maxHealth Maximum health for scaling
     * @param damageSource Damage source
     */
    private static void applyDamageFeedback(Player player, long damage, long maxHealth, DamageSource damageSource) {
        // Apply hurt animation
        player.hurtTime = 10;
        player.hurtDuration = 10;

        // Apply knockback if appropriate
        if (damageSource.getEntity() != null) {
            // Calculate knockback direction and apply
            // Implementation depends on your specific needs
        }
    }
}
