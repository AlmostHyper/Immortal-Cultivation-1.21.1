package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.server.events.data.CombatEventData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.UUID;

/**
 * Combat Event Utilities for the Immortal Cultivation Mod
 *
 * Contains business logic for combat-related event processing, following DRY principles
 * and maintaining separation of concerns. This utility class handles the core combat
 * mechanics while leaving event subscription to handler classes.
 *
 * Refactored to work with CombatEventData for centralized data management.
 *
 * Key Features:
 * - Damage processing with cultivation system integration
 * - Immunity management for multi-entity combat scenarios
 * - Health display synchronization with vanilla systems
 * - Damage feedback and visual effects handling
 * - Performance optimized calculations and cleanup
 *
 * Architecture:
 * - Static utility methods for stateless operations
 * - Integration with CombatEventData for data management
 * - Integration with CombatCalculationUtils for mathematical operations
 * - Thread-safe operations through data manager
 * - Comprehensive error handling and logging
 */
public class CombatEventUtils {

    // === CONSTANTS === //
    private static final String UTILS_NAME = "CombatEventUtils";

    // === CORE DAMAGE PROCESSING === //

    /**
     * Processes incoming damage for a player through the cultivation system.
     * Handles damage conversion, application, immunity setting, and feedback.
     * Now uses CombatEventData for centralized data management.
     *
     * @param player The server player taking damage
     * @param statsAttachment The player's stats attachment
     * @param vanillaDamage The original vanilla damage amount
     * @param damageSource The source of the damage
     * @param sourceKey The unique key for this damage source
     * @param combatData The combat data manager
     */
    public static void processDamage(ServerPlayer player,
                                     PlayerStatsAttachment statsAttachment,
                                     float vanillaDamage,
                                     DamageSource damageSource,
                                     String sourceKey,
                                     CombatEventData combatData) {

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing {} vanilla damage for player: {}",
                UTILS_NAME, vanillaDamage, player.getName().getString());
        }

        try {
            UUID playerId = player.getUUID();

            // Convert vanilla damage to custom cultivation damage
            long customDamage = CombatCalculationUtils.convertVanillaToCultivationDamage(vanillaDamage);

            // Get current stats data for health management
            long currentHealth = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Current health: {}, Custom damage: {}",
                    UTILS_NAME, currentHealth, customDamage);
            }

            // Store vanilla health for protection system compatibility
            combatData.updateLastKnownVanillaHealth(playerId, player.getHealth());

            // Apply damage through cultivation system
            long finalDamage = Math.min(customDamage, currentHealth);
            long newHealth = Math.max(0, currentHealth - finalDamage);

            // Update cultivation health
            statsAttachment.setCurrentStat(IStatsData.StatType.HEALTH, newHealth);

            // Calculate immunity duration based on damage taken
            long immunityDuration = CombatCalculationUtils.calculateImmunityDuration(finalDamage, damageSource);

            // Set immunity using data manager (duration-based method)
            combatData.setPlayerImmunity(playerId, sourceKey, immunityDuration);

            // Apply damage feedback effects
            applyDamageFeedback(player, damageSource, finalDamage, newHealth <= 0);

            // Update vanilla health display for compatibility
            updateVanillaHealthDisplay(player, statsAttachment);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Applied {} damage to player {}, new health: {}, immunity: {}ms",
                    UTILS_NAME, finalDamage, player.getName().getString(), newHealth, immunityDuration);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error processing damage for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === IMMUNITY MANAGEMENT === //

    /**
     * Checks if a player is immune to a specific damage source using data manager.
     *
     * @param playerId The player's UUID
     * @param sourceKey The damage source key
     * @param combatData The combat data manager
     * @return True if the player is immune, false otherwise
     */
    public static boolean isPlayerImmune(UUID playerId, String sourceKey, CombatEventData combatData) {
        try {
            boolean immune = combatData.isPlayerImmune(playerId, sourceKey);

            if (immune && ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Player {} is immune to damage source: {}",
                    UTILS_NAME, playerId, sourceKey);
            }

            return immune;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error checking immunity for player {}: {}",
                UTILS_NAME, playerId, e.getMessage(), e);
            return false; // Default to not immune on error
        }
    }

    /**
     * Sets immunity for a player against a specific damage source using data manager.
     *
     * @param playerId The player's UUID
     * @param sourceKey The damage source key
     * @param durationMs The immunity duration in milliseconds
     * @param combatData The combat data manager
     */
    public static void setPlayerImmunity(UUID playerId, String sourceKey, long durationMs,
                                         CombatEventData combatData) {
        if (durationMs <= 0) {
            return;
        }

        try {
            combatData.setPlayerImmunity(playerId, sourceKey, durationMs);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Set immunity for player {} against {} for {}ms",
                    UTILS_NAME, playerId, sourceKey, durationMs);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error setting immunity for player {}: {}",
                UTILS_NAME, playerId, e.getMessage(), e);
        }
    }

    /**
     * Clears all immunity entries for a specific player using data manager.
     *
     * @param playerId The player's UUID
     * @param combatData The combat data manager
     */
    public static void clearPlayerImmunity(UUID playerId, CombatEventData combatData) {
        try {
            combatData.clearPlayerImmunity(playerId);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Cleared all immunity for player: {}", UTILS_NAME, playerId);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error clearing immunity for player {}: {}",
                UTILS_NAME, playerId, e.getMessage(), e);
        }
    }

    // === HEALTH MANAGEMENT === //

    /**
     * Updates the vanilla health display to reflect cultivation health changes.
     * Maintains compatibility with vanilla systems while using cultivation health.
     *
     * @param player The server player
     * @param statsData The player's stats data
     */
    public static void updateVanillaHealthDisplay(ServerPlayer player, IStatsData statsData) {
        try {
            long currentHealth = statsData.getCurrentStat(IStatsData.StatType.HEALTH);
            long maxHealth = statsData.getMaxStat(IStatsData.StatType.HEALTH);

            // Calculate display health as percentage of max health
            float displayHealth = maxHealth > 0 ?
                (float) currentHealth / maxHealth * player.getMaxHealth() : 0.0f;

            // Clamp to valid range
            displayHealth = Math.max(0.0f, Math.min(displayHealth, player.getMaxHealth()));

            // Update vanilla health for display purposes
            player.setHealth(displayHealth);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Updated vanilla health display for player {} to {} (cultivation: {}/{})",
                    UTILS_NAME, player.getName().getString(), displayHealth, currentHealth, maxHealth);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error updating health display for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Synchronizes vanilla health with cultivation health for protection systems.
     *
     * @param player The server player
     * @param combatData The combat data manager
     */
    public static void synchronizeVanillaHealth(ServerPlayer player, CombatEventData combatData) {
        try {
            Float lastKnownHealth = combatData.getLastKnownVanillaHealth(player.getUUID());

            if (lastKnownHealth != null && Math.abs(player.getHealth() - lastKnownHealth) > 0.1f) {
                // Vanilla health changed externally (e.g., other mods), update our tracking
                combatData.updateLastKnownVanillaHealth(player.getUUID(), player.getHealth());

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Synchronized vanilla health change for player {} from {} to {}",
                        UTILS_NAME, player.getName().getString(), lastKnownHealth, player.getHealth());
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error synchronizing vanilla health for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === FEEDBACK EFFECTS === //

    /**
     * Applies damage feedback effects including visual, audio, and physical feedback.
     *
     * @param player The server player taking damage
     * @param damageSource The source of damage
     * @param damage The amount of damage taken
     * @param isKilling Whether this damage is lethal
     */
    private static void applyDamageFeedback(ServerPlayer player, DamageSource damageSource,
                                            long damage, boolean isKilling) {
        try {
            // Apply hurt time and visual feedback
            CombatCalculationUtils.applyDamageFeedback(player, damageSource, damage);

            // Handle death if needed
            if (isKilling) {
                handleCultivationDeath(player, damageSource);
            }

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Applied damage feedback for player {} (damage: {}, killing: {})",
                    UTILS_NAME, player.getName().getString(), damage, isKilling);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error applying damage feedback for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Handles player death through the cultivation system.
     *
     * @param player The dying player
     * @param damageSource The cause of death
     */
    private static void handleCultivationDeath(ServerPlayer player, DamageSource damageSource) {
        try {
            // Trigger cultivation-specific death handling
            // This could include soul preservation, reincarnation mechanics, etc.

            // For now, use vanilla death mechanics with cultivation context
            player.hurt(damageSource, Float.MAX_VALUE);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Handled cultivation death for player {} from source: {}",
                    UTILS_NAME, player.getName().getString(), damageSource.type().msgId());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error handling cultivation death for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }
}