package net.hyper.immortalcultivation.server.events.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.server.events.utils.HealingEventUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Healing Event Handler for the Immortal Cultivation Mod
 *
 * Handles healing-related events following SOLID principles and DRY methodology.
 * Delegates business logic to utility classes while maintaining clean event handling.
 * Integrates with the cultivation system for enhanced healing mechanics.
 *
 * Key Features:
 * - Vanilla healing interception and conversion to cultivation system
 * - Healing source identification and tracking
 * - Integration with regeneration and pill systems
 * - Performance optimized with tracking structures
 * - Comprehensive debug logging for healing system monitoring
 *
 * Architecture:
 * - Handler manages event subscriptions and validation
 * - HealingEventUtils handles business logic and calculations
 * - CombatCalculationUtils provides healing conversion operations
 * - Maintains thread safety through concurrent collections
 */
public class HealingEventHandler {

    // === CONSTANTS === //

    private static final String HANDLER_NAME = "HealingEventHandler";
    private static final int HEALING_TRACKING_CLEANUP_INTERVAL = 12000; // Cleanup every 10 minutes

    // === TRACKING DATA === //

    /**
     * Tracks recent healing amounts per player for analysis and debugging.
     * Key: Player UUID, Value: Recent healing history with timestamps
     */
    private static final Map<UUID, HealingHistory> healingHistories = new ConcurrentHashMap<>();

    /**
     * Tracks last healing processing time for cleanup scheduling
     */
    private static long lastCleanupTime = 0L;

    // === EVENT HANDLERS === //

    /**
     * Handles healing events for players in the cultivation system.
     * Intercepts vanilla healing, converts to custom cultivation healing, and applies
     * proper health restoration while maintaining vanilla compatibility.
     *
     * @param event The healing event to process
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHealing(LivingHealEvent event) {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing healing event for entity: {}",
                HANDLER_NAME, event.getEntity().getClass().getSimpleName());
        }

        // Validate event and extract player
        ServerPlayer player = validateAndExtractPlayer(event);
        if (player == null) {
            return;
        }

        UUID playerId = player.getUUID();
        float vanillaHealing = event.getAmount();

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing {} healing for player: {}",
                HANDLER_NAME, vanillaHealing, player.getName().getString());
        }

        try {
            // Get player stats attachment
            PlayerStatsAttachment statsAttachment = getPlayerStatsAttachment(player);
            if (statsAttachment == null) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] No stats attachment found for player: {}",
                        HANDLER_NAME, player.getName().getString());
                }
                return;
            }

            // Cancel vanilla healing processing to use our custom system
            event.setAmount(0.0f);

            // Process healing through utility class
            HealingEventUtils.processHealing(
                player,
                statsAttachment,
                vanillaHealing,
                healingHistories
            );

            // Perform periodic cleanup
            performPeriodicCleanup();

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error processing healing for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
            // Don't cancel the event on error to maintain vanilla fallback behavior
            event.setAmount(vanillaHealing);
        }
    }

    // === VALIDATION METHODS === //

    /**
     * Validates the healing event and extracts the player if applicable.
     *
     * @param event The healing event to validate
     * @return The server player if valid, null otherwise
     */
    @Nullable
    private static ServerPlayer validateAndExtractPlayer(LivingHealEvent event) {
        // Check if the entity is a player
        if (!(event.getEntity() instanceof Player player)) {
            return null;
        }

        // Check if we're on the server side
        if (player.level().isClientSide()) {
            return null;
        }

        // Ensure it's a ServerPlayer for proper handling
        if (!(player instanceof ServerPlayer)) {
            return null;
        }

        return (ServerPlayer) player;
    }

    /**
     * Safely retrieves the PlayerStatsAttachment from a server player.
     *
     * @param player The server player to get attachment from
     * @return The stats attachment or null if not available
     */
    @Nullable
    private static PlayerStatsAttachment getPlayerStatsAttachment(ServerPlayer player) {
        try {
            return player.getData(AttachmentRegistry.PLAYER_STATS.get());
        } catch (Exception e) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Failed to get stats attachment for player {}: {}",
                    HANDLER_NAME, player.getName().getString(), e.getMessage());
            }
            return null;
        }
    }

    // === UTILITY METHODS === //

    /**
     * Performs periodic cleanup of stale healing history data to prevent memory leaks.
     * Only runs cleanup at specified intervals to avoid performance impact.
     */
    private static void performPeriodicCleanup() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > HEALING_TRACKING_CLEANUP_INTERVAL) {
            HealingEventUtils.cleanupStaleHealingHistory(healingHistories);
            lastCleanupTime = currentTime;

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Performed healing history cleanup", HANDLER_NAME);
            }
        }
    }

    // === HEALING HISTORY MANAGEMENT === //

    /**
     * Inner class representing healing history for a player.
     * Used for analysis, debugging, and potential anti-exploit measures.
     */
    public static class HealingHistory {
        private final Map<Long, Float> healingEvents; // timestamp -> amount
        private final long creationTime;
        private float totalHealing;
        private int healingCount;

        public HealingHistory() {
            this.healingEvents = new ConcurrentHashMap<>();
            this.creationTime = System.currentTimeMillis();
            this.totalHealing = 0.0f;
            this.healingCount = 0;
        }

        /**
         * Records a healing event.
         *
         * @param amount The amount of healing
         */
        public void recordHealing(float amount) {
            long timestamp = System.currentTimeMillis();
            healingEvents.put(timestamp, amount);
            totalHealing += amount;
            healingCount++;
        }

        /**
         * Gets the total healing amount recorded.
         *
         * @return Total healing amount
         */
        public float getTotalHealing() {
            return totalHealing;
        }

        /**
         * Gets the number of healing events recorded.
         *
         * @return Number of healing events
         */
        public int getHealingCount() {
            return healingCount;
        }

        /**
         * Gets the creation time of this history.
         *
         * @return Creation timestamp
         */
        public long getCreationTime() {
            return creationTime;
        }

        /**
         * Removes healing events older than the specified time.
         *
         * @param maxAge Maximum age in milliseconds
         * @return Number of events removed
         */
        public int removeOldEvents(long maxAge) {
            long cutoffTime = System.currentTimeMillis() - maxAge;
            int removedCount = 0;

            healingEvents.entrySet().removeIf(entry -> {
                boolean isOld = entry.getKey() < cutoffTime;
                if (isOld) {
                    totalHealing -= entry.getValue();
                    healingCount--;
                }
                return isOld;
            });

            return removedCount;
        }

        /**
         * Checks if this history is empty (no recent events).
         *
         * @return True if no events are recorded
         */
        public boolean isEmpty() {
            return healingEvents.isEmpty();
        }
    }

    /**
     * Creates or updates healing history for a player.
     *
     * @param playerId The player's UUID
     * @param healingAmount The amount of healing to record
     * @return The updated healing history
     */
    public static HealingHistory updateHealingHistory(UUID playerId, float healingAmount) {
        HealingHistory history = healingHistories.computeIfAbsent(playerId, k -> new HealingHistory());
        history.recordHealing(healingAmount);
        return history;
    }

    /**
     * Gets the healing history for a player.
     *
     * @param playerId The player's UUID
     * @return The healing history or null if not available
     */
    @Nullable
    public static HealingHistory getHealingHistory(UUID playerId) {
        return healingHistories.get(playerId);
    }

    /**
     * Clears healing history data for a player (cleanup).
     *
     * @param playerId The player's UUID
     */
    public static void clearPlayerHealingData(UUID playerId) {
        healingHistories.remove(playerId);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Cleared healing data for player: {}",
                HANDLER_NAME, playerId);
        }
    }

    /**
     * Forces a complete healing history cleanup for all players.
     * Useful for debugging or emergency cleanup.
     */
    public static void forceHealingHistoryCleanup() {
        HealingEventUtils.cleanupStaleHealingHistory(healingHistories);
        lastCleanupTime = System.currentTimeMillis();

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Forced healing history cleanup completed", HANDLER_NAME);
        }
    }

    // === DEBUG AND MONITORING === //

    /**
     * Gets current healing processing statistics for debugging purposes.
     *
     * @return Map containing healing processing statistics
     */
    public static Map<String, Object> getHealingProcessingStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("playersWithHistory", healingHistories.size());
        stats.put("totalHealingEvents", healingHistories.values().stream()
            .mapToInt(HealingHistory::getHealingCount)
            .sum());
        stats.put("totalHealingAmount", healingHistories.values().stream()
            .mapToDouble(HealingHistory::getTotalHealing)
            .sum());
        stats.put("lastCleanupTime", lastCleanupTime);
        return stats;
    }

    /**
     * Validates healing amounts to detect potential exploits or issues.
     *
     * @param healingAmount The healing amount to validate
     * @param player The player receiving healing
     * @return True if the healing amount is valid, false if suspicious
     */
    public static boolean validateHealingAmount(float healingAmount, ServerPlayer player) {
        // Basic validation checks
        if (healingAmount < 0) {
            ImmortalCultivation.LOGGER.warn("[{}] Negative healing amount detected for player {}: {}",
                HANDLER_NAME, player.getName().getString(), healingAmount);
            return false;
        }

        if (healingAmount > 1000.0f) { // Adjust threshold as needed
            ImmortalCultivation.LOGGER.warn("[{}] Unusually large healing amount for player {}: {}",
                HANDLER_NAME, player.getName().getString(), healingAmount);
            // Could be legitimate (high-tier pills), so don't reject but log
        }

        return true;
    }
}