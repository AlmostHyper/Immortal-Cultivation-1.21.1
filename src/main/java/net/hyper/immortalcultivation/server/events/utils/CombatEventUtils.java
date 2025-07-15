package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combat Event Utilities for the Immortal Cultivation Mod
 *
 * Contains business logic for combat-related event processing, following DRY principles
 * and maintaining separation of concerns. This utility class handles the core combat
 * mechanics while leaving event subscription to handler classes.
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
 * - Integration with CombatCalculationUtils for mathematical operations
 * - Thread-safe operations for server environments
 * - Comprehensive error handling and logging
 */
public class CombatEventUtils {

    // === CONSTANTS === //

    private static final String UTILS_NAME = "CombatEventUtils";
    private static final long IMMUNITY_EXPIRED_THRESHOLD = 1000L; // 1 second buffer for cleanup

    // === CORE DAMAGE PROCESSING === //

    /**
     * Processes incoming damage for a player through the cultivation system.
     * Handles damage conversion, application, immunity setting, and feedback.
     *
     * @param player The server player taking damage
     * @param statsAttachment The player's stats attachment
     * @param vanillaDamage The original vanilla damage amount
     * @param damageSource The source of the damage
     * @param sourceKey The unique key for this damage source
     * @param immunityMap The immunity tracking map
     * @param healthMap The vanilla health tracking map
     */
    public static void processDamage(ServerPlayer player,
                                     PlayerStatsAttachment statsAttachment,
                                     float vanillaDamage,
                                     DamageSource damageSource,
                                     String sourceKey,
                                     Map<UUID, Map<String, Long>> immunityMap,
                                     Map<UUID, Float> healthMap) {

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing {} vanilla damage for player: {}",
                UTILS_NAME, vanillaDamage, player.getName().getString());
        }

        try {
            // Convert vanilla damage to custom cultivation damage
            long customDamage = CombatCalculationUtils.convertToCustomDamage(vanillaDamage, player);

            if (customDamage <= 0) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] No damage to apply after conversion for player: {}",
                        UTILS_NAME, player.getName().getString());
                }
                return;
            }

            // Apply damage to health stat
            long healthBefore = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);
            long actualDamage = statsAttachment.consumeStat(IStatsData.StatType.HEALTH, customDamage);
            long healthAfter = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);

            if (actualDamage > 0) {
                // Set immunity to prevent damage spam
                long immunityDuration = CombatCalculationUtils.calculateImmunityDuration(actualDamage);
                setPlayerImmunity(player.getUUID(), sourceKey, immunityDuration, immunityMap);

                // Update vanilla health display for compatibility
                updateVanillaHealthDisplay(player, statsAttachment, healthMap);

                // Apply damage feedback effects
                applyDamageFeedback(player, actualDamage, healthAfter, damageSource);

                // Mark attachment for sync
                statsAttachment.markDirty();

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Applied {} damage to {} (vanilla: {}, before: {}, after: {})",
                        UTILS_NAME, actualDamage, player.getName().getString(), vanillaDamage,
                        healthBefore, healthAfter);
                }
            } else {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] No actual damage applied to player: {} (health protected or at minimum)",
                        UTILS_NAME, player.getName().getString());
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error processing damage for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === IMMUNITY MANAGEMENT === //

    /**
     * Checks if a player is immune to damage from a specific source.
     *
     * @param playerId The player's UUID
     * @param sourceKey The damage source key
     * @param immunityMap The immunity tracking map
     * @return True if the player is immune, false otherwise
     */
    public static boolean isPlayerImmune(UUID playerId, String sourceKey, Map<UUID, Map<String, Long>> immunityMap) {
        Map<String, Long> playerImmunities = immunityMap.get(playerId);
        if (playerImmunities == null) {
            return false;
        }

        Long immunityEnd = playerImmunities.get(sourceKey);
        if (immunityEnd == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime > immunityEnd) {
            playerImmunities.remove(sourceKey);
            // Clean up empty player entries
            if (playerImmunities.isEmpty()) {
                immunityMap.remove(playerId);
            }
            return false;
        }

        return true;
    }

    /**
     * Sets immunity for a player against a specific damage source.
     *
     * @param playerId The player's UUID
     * @param sourceKey The damage source key
     * @param durationMs The immunity duration in milliseconds
     * @param immunityMap The immunity tracking map
     */
    public static void setPlayerImmunity(UUID playerId, String sourceKey, long durationMs,
                                         Map<UUID, Map<String, Long>> immunityMap) {
        if (durationMs <= 0) {
            return;
        }

        long immunityEnd = System.currentTimeMillis() + durationMs;
        immunityMap.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(sourceKey, immunityEnd);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Set immunity for player {} against {} for {}ms",
                UTILS_NAME, playerId, sourceKey, durationMs);
        }
    }

    /**
     * Clears all immunity entries for a specific player.
     *
     * @param playerId The player's UUID
     * @param immunityMap The immunity tracking map
     */
    public static void clearPlayerImmunity(UUID playerId, Map<UUID, Map<String, Long>> immunityMap) {
        if (immunityMap.remove(playerId) != null) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Cleared all immunity for player: {}", UTILS_NAME, playerId);
            }
        }
    }

    /**
     * Cleans up expired immunity entries to prevent memory leaks.
     *
     * @param immunityMap The immunity tracking map to clean
     */
    public static void cleanupExpiredImmunities(Map<UUID, Map<String, Long>> immunityMap) {
        long currentTime = System.currentTimeMillis();
        int removedEntries = 0;
        int removedPlayers = 0;

        Iterator<Map.Entry<UUID, Map<String, Long>>> playerIterator = immunityMap.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, Map<String, Long>> playerEntry = playerIterator.next();
            Map<String, Long> playerImmunities = playerEntry.getValue();

            Iterator<Map.Entry<String, Long>> immunityIterator = playerImmunities.entrySet().iterator();
            while (immunityIterator.hasNext()) {
                Map.Entry<String, Long> immunityEntry = immunityIterator.next();
                if (currentTime > immunityEntry.getValue() + IMMUNITY_EXPIRED_THRESHOLD) {
                    immunityIterator.remove();
                    removedEntries++;
                }
            }

            if (playerImmunities.isEmpty()) {
                playerIterator.remove();
                removedPlayers++;
            }
        }

        if (ImmortalCultivation.LOGGER.isDebugEnabled() && (removedEntries > 0 || removedPlayers > 0)) {
            ImmortalCultivation.LOGGER.debug("[{}] Cleanup removed {} immunity entries and {} empty player records",
                UTILS_NAME, removedEntries, removedPlayers);
        }
    }

    // === HEALTH DISPLAY MANAGEMENT === //

    /**
     * Updates the vanilla health display to reflect cultivation health changes.
     * Maintains compatibility with vanilla health bars and other mods.
     *
     * @param player The server player
     * @param statsAttachment The player's stats attachment
     * @param healthMap The vanilla health tracking map
     */
    public static void updateVanillaHealthDisplay(ServerPlayer player,
                                                  PlayerStatsAttachment statsAttachment,
                                                  Map<UUID, Float> healthMap) {
        try {
            long currentHealth = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);
            long maxHealth = statsAttachment.getMaxStat(IStatsData.StatType.HEALTH);

            // Calculate proportional vanilla health (0-20 scale)
            float vanillaMaxHealth = player.getMaxHealth();
            float proportionalHealth = maxHealth > 0 ?
                (float) currentHealth / maxHealth * vanillaMaxHealth : 0.0f;

            // Ensure health is within bounds
            proportionalHealth = Math.max(0.0f, Math.min(proportionalHealth, vanillaMaxHealth));

            // Update player's vanilla health
            player.setHealth(proportionalHealth);

            // Track the change
            Float previousHealth = healthMap.put(player.getUUID(), proportionalHealth);

            if (ImmortalCultivation.LOGGER.isDebugEnabled() &&
                (previousHealth == null || Math.abs(previousHealth - proportionalHealth) > 0.1f)) {
                ImmortalCultivation.LOGGER.debug("[{}] Updated vanilla health for {}: {} -> {} (cultivation: {}/{})",
                    UTILS_NAME, player.getName().getString(),
                    previousHealth != null ? previousHealth : "null", proportionalHealth,
                    currentHealth, maxHealth);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error updating vanilla health display for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === DAMAGE FEEDBACK === //

    /**
     * Delegates damage feedback to the existing CombatCalculationUtils implementation.
     * Maintains separation of concerns while utilizing existing tested functionality.
     *
     * @param player The player taking damage
     * @param damage The amount of damage taken
     * @param remainingHealth The player's remaining health (used as maxHealth for scaling)
     * @param damageSource The source of the damage
     */
    public static void applyDamageFeedback(ServerPlayer player, long damage, long remainingHealth,
                                           DamageSource damageSource) {
        try {
            // Delegate to existing implementation in CombatCalculationUtils
            CombatCalculationUtils.applyDamageFeedback(player, damage, remainingHealth, damageSource);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Applied damage feedback for player: {} (damage: {})",
                    UTILS_NAME, player.getName().getString(), damage);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error applying damage feedback for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === UTILITY METHODS === //

    /**
     * Generic method to safely apply stat modifications with error handling.
     *
     * @param <T> The stat type
     * @param statsAttachment The stats attachment to modify
     * @param statType The type of stat to modify
     * @param operation The operation to perform
     * @param operationName The name of the operation for logging
     * @return True if the operation was successful, false otherwise
     */
    public static <T extends Enum<T>> boolean safeStatOperation(PlayerStatsAttachment statsAttachment,
                                                                IStatsData.StatType statType,
                                                                java.util.function.Function<IStatsData.StatType, Long> operation,
                                                                String operationName) {
        try {
            Long result = operation.apply(statType);
            return result != null && result >= 0;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during {} operation for stat {}: {}",
                UTILS_NAME, operationName, statType, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates that all required parameters for damage processing are present and valid.
     *
     * @param player The player (must not be null)
     * @param statsAttachment The stats attachment (must not be null)
     * @param vanillaDamage The damage amount (must be positive)
     * @param damageSource The damage source (must not be null)
     * @return True if all parameters are valid, false otherwise
     */
    public static boolean validateDamageParameters(ServerPlayer player,
                                                   PlayerStatsAttachment statsAttachment,
                                                   float vanillaDamage,
                                                   DamageSource damageSource) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("[{}] Null player provided for damage processing", UTILS_NAME);
            return false;
        }

        if (statsAttachment == null) {
            ImmortalCultivation.LOGGER.warn("[{}] Null stats attachment for player: {}",
                UTILS_NAME, player.getName().getString());
            return false;
        }

        if (vanillaDamage < 0) {
            ImmortalCultivation.LOGGER.warn("[{}] Negative damage amount for player {}: {}",
                UTILS_NAME, player.getName().getString(), vanillaDamage);
            return false;
        }

        if (damageSource == null) {
            ImmortalCultivation.LOGGER.warn("[{}] Null damage source for player: {}",
                UTILS_NAME, player.getName().getString());
            return false;
        }

        return true;
    }
}