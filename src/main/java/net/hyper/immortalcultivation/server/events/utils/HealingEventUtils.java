package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.server.events.server.HealingEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Healing Event Utilities for the Immortal Cultivation Mod
 *
 * Contains business logic for healing-related event processing, following DRY principles
 * and maintaining separation of concerns. This utility class handles the core healing
 * mechanics while leaving event subscription to handler classes.
 *
 * Key Features:
 * - Healing conversion between vanilla and cultivation systems
 * - Health restoration with proper stat management
 * - Vanilla health display synchronization
 * - Healing source identification and tracking
 * - Performance optimized calculations and history management
 *
 * Architecture:
 * - Static utility methods for stateless operations
 * - Integration with CombatCalculationUtils for conversion operations
 * - Integration with PlayerEventUtils for vanilla health display
 * - Thread-safe operations for server environments
 * - Comprehensive error handling and logging
 */
public class HealingEventUtils {

    // === CONSTANTS === //

    private static final String UTILS_NAME = "HealingEventUtils";
    private static final long HEALING_HISTORY_MAX_AGE = 300000L; // 5 minutes
    private static final long HEALING_HISTORY_EXPIRED_THRESHOLD = 1000L; // 1 second buffer for cleanup

    // === CORE HEALING PROCESSING === //

    /**
     * Processes incoming healing for a player through the cultivation system.
     * Handles healing conversion, application, vanilla health sync, and history tracking.
     *
     * @param player The server player receiving healing
     * @param statsAttachment The player's stats attachment
     * @param vanillaHealing The original vanilla healing amount
     * @param historyMap The healing history tracking map
     */
    public static void processHealing(ServerPlayer player,
                                      PlayerStatsAttachment statsAttachment,
                                      float vanillaHealing,
                                      Map<UUID, HealingEventHandler.HealingHistory> historyMap) {

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing {} vanilla healing for player: {}",
                UTILS_NAME, vanillaHealing, player.getName().getString());
        }

        try {
            // Validate healing parameters
            if (!validateHealingParameters(player, statsAttachment, vanillaHealing)) {
                return;
            }

            // Convert vanilla healing to custom cultivation healing
            long customHealing = CombatCalculationUtils.convertVanillaToCultivationHealing(vanillaHealing);

            if (customHealing <= 0) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] No healing to apply after conversion for player: {}",
                        UTILS_NAME, player.getName().getString());
                }
                return;
            }

            // Apply enhanced healing if player has regeneration effects
            customHealing = applyHealingEnhancements(player, customHealing);

            // Apply healing to health stat
            long healthBefore = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);
            long actualHealing = statsAttachment.addStat(IStatsData.StatType.HEALTH, customHealing);
            long healthAfter = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);

            if (actualHealing > 0) {
                // Update vanilla health display for compatibility
                updateVanillaHealthDisplay(player, statsAttachment);

                // Apply healing feedback effects
                applyHealingFeedback(player, actualHealing, healthAfter);

                // Track healing in history for analysis
                trackHealingHistory(player.getUUID(), vanillaHealing, historyMap);

                // Mark attachment for sync
                statsAttachment.markDirty();

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Applied {} healing to {} (vanilla: {}, before: {}, after: {})",
                        UTILS_NAME, actualHealing, player.getName().getString(), vanillaHealing,
                        healthBefore, healthAfter);
                }
            } else {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] No actual healing applied to player: {} (already at maximum health)",
                        UTILS_NAME, player.getName().getString());
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error processing healing for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === HEALING ENHANCEMENT === //

    /**
     * Applies healing enhancements based on player effects and cultivation state.
     *
     * @param player The server player receiving healing
     * @param baseHealing The base healing amount
     * @return Enhanced healing amount
     */
    private static long applyHealingEnhancements(ServerPlayer player, long baseHealing) {
        long enhancedHealing = baseHealing;

        try {
            // Check for regeneration effect
            if (player.hasEffect(MobEffects.REGENERATION)) {
                int amplifier = player.getEffect(MobEffects.REGENERATION).getAmplifier();
                double multiplier = 1.0 + (amplifier + 1) * 0.25; // +25% per level
                enhancedHealing = Math.round(enhancedHealing * multiplier);

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Applied regeneration enhancement for {}: {}x multiplier (level {})",
                        UTILS_NAME, player.getName().getString(), multiplier, amplifier + 1);
                }
            }

            // Check for absorption effect (provides overheal protection)
            if (player.hasEffect(MobEffects.ABSORPTION)) {
                // Absorption effect could provide temporary health bonus
                // This is handled separately by vanilla, but we could integrate it
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Player {} has absorption effect during healing",
                        UTILS_NAME, player.getName().getString());
                }
            }

            // Future enhancement: Add cultivation realm-based healing bonuses
            // Could check player cultivation level and provide healing multipliers

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error applying healing enhancements for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
            return baseHealing; // Return original value on error
        }

        return enhancedHealing;
    }

    // === HEALTH DISPLAY MANAGEMENT === //

    /**
     * Updates the vanilla health display to reflect cultivation health changes.
     * Delegates to existing AttributeModifierUtils.refreshStats() for consistency.
     *
     * @param player The server player
     * @param statsAttachment The player's stats attachment
     */
    private static void updateVanillaHealthDisplay(ServerPlayer player, PlayerStatsAttachment statsAttachment) {
        try {
            // Delegate to existing AttributeModifierUtils which handles health display updates
            AttributeModifierUtils.refreshPlayerAttributes(player);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Updated vanilla health display for player: {}",
                    UTILS_NAME, player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error updating vanilla health display for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === HEALING FEEDBACK === //

    /**
     * Applies visual and audio feedback effects for healing received.
     *
     * @param player The player receiving healing
     * @param healing The amount of healing applied
     * @param currentHealth The player's current health after healing
     */
    private static void applyHealingFeedback(ServerPlayer player, long healing, long currentHealth) {
        try {
            // Reset hurt time to remove damage visuals
            if (player.hurtTime > 0) {
                player.hurtTime = 0;
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Reset hurt time for healing player: {}",
                        UTILS_NAME, player.getName().getString());
                }
            }

            // Future enhancement: Add particle effects for healing
            // Could send custom packets to client for healing visuals

            // Future enhancement: Play healing sounds
            // Could play different sounds based on healing amount or source

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error applying healing feedback for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === HEALING HISTORY MANAGEMENT === //

    /**
     * Tracks healing in player history for analysis and debugging.
     *
     * @param playerId The player's UUID
     * @param healingAmount The amount of healing received
     * @param historyMap The healing history tracking map
     */
    private static void trackHealingHistory(UUID playerId, float healingAmount,
                                            Map<UUID, HealingEventHandler.HealingHistory> historyMap) {
        try {
            HealingEventHandler.HealingHistory history = historyMap.computeIfAbsent(
                playerId, k -> new HealingEventHandler.HealingHistory());
            history.recordHealing(healingAmount);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Tracked healing history for player {}: {} (total: {}, count: {})",
                    UTILS_NAME, playerId, healingAmount, history.getTotalHealing(), history.getHealingCount());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error tracking healing history for player {}: {}",
                UTILS_NAME, playerId, e.getMessage(), e);
        }
    }

    /**
     * Cleans up stale healing history entries to prevent memory leaks.
     *
     * @param historyMap The healing history map to clean
     */
    public static void cleanupStaleHealingHistory(Map<UUID, HealingEventHandler.HealingHistory> historyMap) {
        long currentTime = System.currentTimeMillis();
        int removedHistories = 0;
        int removedEvents = 0;

        try {
            Iterator<Map.Entry<UUID, HealingEventHandler.HealingHistory>> iterator =
                historyMap.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, HealingEventHandler.HealingHistory> entry = iterator.next();
                HealingEventHandler.HealingHistory history = entry.getValue();

                // Remove old events from history
                int removedFromHistory = history.removeOldEvents(HEALING_HISTORY_MAX_AGE);
                removedEvents += removedFromHistory;

                // Remove empty histories
                if (history.isEmpty() ||
                    (currentTime - history.getCreationTime()) > HEALING_HISTORY_MAX_AGE + HEALING_HISTORY_EXPIRED_THRESHOLD) {
                    iterator.remove();
                    removedHistories++;
                }
            }

            if (ImmortalCultivation.LOGGER.isDebugEnabled() && (removedHistories > 0 || removedEvents > 0)) {
                ImmortalCultivation.LOGGER.debug("[{}] Cleanup removed {} healing histories and {} individual events",
                    UTILS_NAME, removedHistories, removedEvents);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during healing history cleanup: {}",
                UTILS_NAME, e.getMessage(), e);
        }
    }

    // === UTILITY METHODS === //

    /**
     * Validates that all required parameters for healing processing are present and valid.
     *
     * @param player The player (must not be null)
     * @param statsAttachment The stats attachment (must not be null)
     * @param vanillaHealing The healing amount (must be positive)
     * @return True if all parameters are valid, false otherwise
     */
    public static boolean validateHealingParameters(ServerPlayer player,
                                                    PlayerStatsAttachment statsAttachment,
                                                    float vanillaHealing) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("[{}] Null player provided for healing processing", UTILS_NAME);
            return false;
        }

        if (statsAttachment == null) {
            ImmortalCultivation.LOGGER.warn("[{}] Null stats attachment for player: {}",
                UTILS_NAME, player.getName().getString());
            return false;
        }

        if (vanillaHealing <= 0) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Non-positive healing amount for player {}: {}",
                    UTILS_NAME, player.getName().getString(), vanillaHealing);
            }
            return false;
        }

        return true;
    }

    /**
     * Checks if a player can receive healing based on current state.
     *
     * @param player The server player
     * @param statsAttachment The player's stats attachment
     * @return True if the player can receive healing, false otherwise
     */
    public static boolean canPlayerReceiveHealing(ServerPlayer player, PlayerStatsAttachment statsAttachment) {
        try {
            // Check if player is alive
            if (!player.isAlive()) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Player {} is not alive, cannot receive healing",
                        UTILS_NAME, player.getName().getString());
                }
                return false;
            }

            // Check if health is already at maximum
            long currentHealth = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);
            long maxHealth = statsAttachment.getMaxStat(IStatsData.StatType.HEALTH);

            if (currentHealth >= maxHealth) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Player {} is already at maximum health ({}/{})",
                        UTILS_NAME, player.getName().getString(), currentHealth, maxHealth);
                }
                return false;
            }

            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error checking if player {} can receive healing: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Calculates the maximum healing that can be applied to a player.
     *
     * @param statsAttachment The player's stats attachment
     * @return Maximum healing amount that can be applied
     */
    public static long calculateMaxPossibleHealing(PlayerStatsAttachment statsAttachment) {
        try {
            long currentHealth = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);
            long maxHealth = statsAttachment.getMaxStat(IStatsData.StatType.HEALTH);
            return Math.max(0L, maxHealth - currentHealth);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error calculating max possible healing: {}",
                UTILS_NAME, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Generic method to safely apply healing with error handling and validation.
     *
     * @param statsAttachment The stats attachment to modify
     * @param healingAmount The amount of healing to apply
     * @param operationName The name of the operation for logging
     * @return The actual amount of healing applied
     */
    public static long safeHealingOperation(PlayerStatsAttachment statsAttachment,
                                            long healingAmount,
                                            String operationName) {
        try {
            if (healingAmount <= 0) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Non-positive healing amount in {}: {}",
                        UTILS_NAME, operationName, healingAmount);
                }
                return 0L;
            }

            return statsAttachment.addStat(IStatsData.StatType.HEALTH, healingAmount);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during {} operation: {}",
                UTILS_NAME, operationName, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Identifies the source of healing for logging and analysis purposes.
     *
     * @param player The player receiving healing
     * @param healingAmount The amount of healing
     * @return A string describing the likely healing source
     */
    public static String identifyHealingSource(ServerPlayer player, float healingAmount) {
        try {
            // Check for active potion effects
            if (player.hasEffect(MobEffects.REGENERATION)) {
                return "Regeneration Effect";
            }

            if (player.hasEffect(MobEffects.HEAL)) {
                return "Instant Health Potion";
            }

            // Check healing amount ranges to guess source
            if (healingAmount >= 20.0f) {
                return "Large Healing Source (Pill/High-tier Potion)";
            } else if (healingAmount >= 6.0f) {
                return "Medium Healing Source (Food/Potion)";
            } else if (healingAmount >= 1.0f) {
                return "Small Healing Source (Food/Natural Regeneration)";
            } else {
                return "Minimal Healing Source";
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error identifying healing source for player {}: {}",
                UTILS_NAME, player.getName().getString(), e.getMessage(), e);
            return "Unknown Source";
        }
    }
}