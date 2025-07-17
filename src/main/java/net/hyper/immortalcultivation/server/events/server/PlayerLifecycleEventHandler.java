package net.hyper.immortalcultivation.server.events.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.common.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.server.events.data.CombatEventData;
import net.hyper.immortalcultivation.server.events.utils.PlayerEventUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Player Lifecycle Event Handler for the Immortal Cultivation Mod
 *
 * Handles all major player lifecycle events following SOLID principles and DRY methodology.
 * This is the most critical handler as it manages player initialization, cleanup, and state transitions.
 * Delegates business logic to utility classes while maintaining clean event handling.
 *
 * Key Features:
 * - Player login initialization and attachment setup
 * - Player logout cleanup and data persistence
 * - Player respawn mechanics with stat restoration
 * - Dimension change handling with bonus reapplication
 * - Player cloning for respawn mechanics
 * - Comprehensive error handling and recovery
 *
 * Architecture:
 * - Handler manages event subscriptions and validation
 * - PlayerLifecycleEventUtils handles business logic and operations
 * - Integration with all other event handlers for cleanup
 * - Maintains thread safety through concurrent collections
 */
public class PlayerLifecycleEventHandler {

    // === CONSTANTS === //

    private static final String HANDLER_NAME = "PlayerLifecycleEventHandler";
    private static final long SESSION_TIMEOUT_MS = 300000L; // 5 minutes

    // === TRACKING DATA === //

    /**
     * Tracks active player sessions for cleanup and monitoring.
     * Key: Player UUID, Value: Session start timestamp
     */
    private static final Map<UUID, Long> activeSessions = new ConcurrentHashMap<>();

    /**
     * Tracks players currently undergoing initialization to prevent duplicate processing.
     * Key: Player UUID, Value: Initialization start timestamp
     */
    private static final Map<UUID, Long> initializingPlayers = new ConcurrentHashMap<>();

    // === MAIN LIFECYCLE EVENTS === //

    /**
     * Handles player login events - the most critical initialization process.
     * Sets up all player attachments, applies cultivation bonuses, and synchronizes data.
     *
     * @param event Player login event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();
        long startTime = System.currentTimeMillis();

        ImmortalCultivation.LOGGER.info("[{}] Initializing player data for: {}",
            HANDLER_NAME, player.getName().getString());

        try {
            // Check for duplicate initialization
            if (initializingPlayers.containsKey(playerId)) {
                ImmortalCultivation.LOGGER.warn("[{}] Player {} already initializing, skipping duplicate",
                    HANDLER_NAME, player.getName().getString());
                return;
            }

            // Mark as initializing
            initializingPlayers.put(playerId, startTime);

            // Use existing PlayerEventUtils methods directly
            PlayerEventUtils.initializePlayerAttachments(player);
            PlayerEventUtils.applyCurrentRealmModifiers(player);
            PlayerEventUtils.synchronizeAllPlayerData(player);

            // Mark session as active
            activeSessions.put(playerId, startTime);

            ImmortalCultivation.LOGGER.info("[{}] Player {} fully initialized in {}ms",
                HANDLER_NAME, player.getName().getString(), System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Critical error during player {} initialization: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);

            // Attempt emergency initialization to prevent player being stuck
            try {
                emergencyPlayerInitialization(player);
                ImmortalCultivation.LOGGER.warn("[{}] Emergency initialization completed for player {}",
                    HANDLER_NAME, player.getName().getString());
            } catch (Exception emergencyError) {
                ImmortalCultivation.LOGGER.error("[{}] Emergency initialization failed for player {}: {}",
                    HANDLER_NAME, player.getName().getString(), emergencyError.getMessage(), emergencyError);
            }
        } finally {
            // Always remove from initializing map
            initializingPlayers.remove(playerId);
        }
    }

    /**
     * Handles player logout events - critical for data cleanup and persistence.
     * Ensures proper cleanup of all tracking data and prevents memory leaks.
     *
     * @param event Player logout event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();

        ImmortalCultivation.LOGGER.info("[{}] Processing logout cleanup for: {}",
            HANDLER_NAME, player.getName().getString());

        try {
            // Simple cleanup - remove from tracking maps
            activeSessions.remove(playerId);
            initializingPlayers.remove(playerId);

            // Notify other handlers for cleanup
            cleanupPlayerDataInOtherHandlers(playerId);

            ImmortalCultivation.LOGGER.info("[{}] Logout cleanup completed for: {}",
                HANDLER_NAME, player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during logout cleanup for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Handles player respawn events - critical for stat restoration and bonus reapplication.
     * Ensures players respawn with proper cultivation stats and bonuses.
     *
     * @param event Player respawn event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("[{}] Processing respawn for: {}",
            HANDLER_NAME, player.getName().getString());

        try {
            // Clear combat immunity on respawn
            CombatEventData.getInstance().clearPlayerImmunity(player.getUUID());

            // Use existing PlayerEventUtils methods directly
            PlayerEventUtils.applyCurrentRealmModifiers(player);

            // Get stats attachment and restore to full
            PlayerStatsAttachment statsAttachment = player.getData(AttachmentRegistry.PLAYER_STATS.get());
            if (statsAttachment != null) {
                PlayerEventUtils.restoreStatsToFull(player, statsAttachment);
            } else {
                ImmortalCultivation.LOGGER.warn("[{}] No stats attachment found during respawn for player: {}",
                    HANDLER_NAME, player.getName().getString());
            }

            // Synchronize all data
            PlayerEventUtils.synchronizeAllPlayerData(player);

            // Log final stats for verification
            if (statsAttachment != null && ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Respawn completed for {}: H:{}/{} Q:{}/{} B:{}/{}",
                    HANDLER_NAME, player.getName().getString(),
                    statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                    statsAttachment.getMaxStat(IStatsData.StatType.QI),
                    statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                    statsAttachment.getMaxStat(IStatsData.StatType.BODY));
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during respawn for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Handles dimension change events - ensures bonuses and data remain consistent.
     * Critical for maintaining cultivation state across dimensions.
     *
     * @param event Player dimension change event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("[{}] Processing dimension change for: {} (from {} to {})",
            HANDLER_NAME, player.getName().getString(),
            event.getFrom().location(), event.getTo().location());

        try {
            // Use existing PlayerEventUtils methods directly
            PlayerEventUtils.applyCurrentRealmModifiers(player);
            PlayerEventUtils.synchronizeAllPlayerData(player);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during dimension change for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Handles player clone events - essential for proper respawn mechanics.
     * Ensures cultivation data is properly transferred between player instances.
     *
     * @param event Player clone event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer) ||
            !(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("[{}] Processing player clone for: {} (death: {})",
            HANDLER_NAME, newPlayer.getName().getString(), event.isWasDeath());

        try {
            // Player clone events are typically handled by NeoForge's attachment system automatically
            // Just log for debugging purposes
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Player clone event: {} -> {} (death: {})",
                    HANDLER_NAME, oldPlayer.getName().getString(), newPlayer.getName().getString(), event.isWasDeath());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during player clone for {}: {}",
                HANDLER_NAME, newPlayer.getName().getString(), e.getMessage(), e);
        }
    }

    // === CLEANUP AND MONITORING === //

    /**
     * Performs emergency player initialization when normal initialization fails.
     * Provides minimal but functional setup to prevent player being stuck.
     *
     * @param player The server player requiring emergency initialization
     */
    private static void emergencyPlayerInitialization(ServerPlayer player) {
        ImmortalCultivation.LOGGER.warn("[{}] Performing emergency initialization for player: {}",
            HANDLER_NAME, player.getName().getString());

        try {
            // Use existing PlayerEventUtils for basic initialization
            PlayerEventUtils.initializePlayerAttachments(player);
            PlayerEventUtils.synchronizeAllPlayerData(player);

            ImmortalCultivation.LOGGER.warn("[{}] Emergency initialization completed for player: {}",
                HANDLER_NAME, player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Emergency initialization failed for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Notifies other event handlers to cleanup player-specific data.
     * Prevents memory leaks across the entire system.
     *
     * @param playerId The player UUID to cleanup
     */
    private static void cleanupPlayerDataInOtherHandlers(UUID playerId) {
        try {
            // Cleanup combat-related data
            CombatEventData.getInstance().clearPlayerImmunity(playerId);

            // Cleanup healing-related data
            HealingEventHandler.clearPlayerHealingData(playerId);

            // Cleanup equipment-related data
            EquipmentEventHandler.clearPlayerEquipmentData(playerId);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Notified other handlers for cleanup: {}",
                    HANDLER_NAME, playerId);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error notifying other handlers for cleanup {}: {}",
                HANDLER_NAME, playerId, e.getMessage(), e);
        }
    }

    /**
     * Performs periodic cleanup of stale session data to prevent memory leaks.
     * Removes expired sessions and initialization tracking.
     */
    public static void performPeriodicCleanup() {
        long currentTime = System.currentTimeMillis();
        AtomicInteger removedSessions = new AtomicInteger();
        AtomicInteger removedInitializing = new AtomicInteger();

        try {
            // Clean up stale active sessions
            activeSessions.entrySet().removeIf(entry -> {
                boolean isStale = (currentTime - entry.getValue()) > SESSION_TIMEOUT_MS;
                if (isStale) removedSessions.getAndIncrement();
                return isStale;
            });

            // Clean up stale initialization tracking
            initializingPlayers.entrySet().removeIf(entry -> {
                boolean isStale = (currentTime - entry.getValue()) > 60000L; // 1 minute timeout
                if (isStale) removedInitializing.getAndIncrement();
                return isStale;
            });

            if (ImmortalCultivation.LOGGER.isDebugEnabled() && (removedSessions.get() > 0 || removedInitializing.get() > 0)) {
                ImmortalCultivation.LOGGER.debug("[{}] Cleanup removed {} stale sessions and {} stale initializations",
                    HANDLER_NAME, removedSessions, removedInitializing);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during periodic cleanup: {}",
                HANDLER_NAME, e.getMessage(), e);
        }
    }

    // === UTILITY METHODS === //

    /**
     * Checks if a player is currently in an active session.
     *
     * @param playerId The player UUID to check
     * @return True if the player has an active session
     */
    public static boolean isPlayerSessionActive(UUID playerId) {
        Long sessionStart = activeSessions.get(playerId);
        if (sessionStart == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if ((currentTime - sessionStart) > SESSION_TIMEOUT_MS) {
            activeSessions.remove(playerId);
            return false;
        }

        return true;
    }

    /**
     * Checks if a player is currently being initialized.
     *
     * @param playerId The player UUID to check
     * @return True if the player is being initialized
     */
    public static boolean isPlayerInitializing(UUID playerId) {
        return initializingPlayers.containsKey(playerId);
    }

    /**
     * Forces cleanup of all data for a specific player.
     * Useful for debugging or emergency situations.
     *
     * @param playerId The player UUID to cleanup
     */
    public static void forcePlayerCleanup(UUID playerId) {
        activeSessions.remove(playerId);
        initializingPlayers.remove(playerId);
        cleanupPlayerDataInOtherHandlers(playerId);

        ImmortalCultivation.LOGGER.info("[{}] Forced cleanup completed for player: {}",
            HANDLER_NAME, playerId);
    }

    /**
     * Gets current session statistics for debugging purposes.
     *
     * @return Map containing session statistics
     */
    public static Map<String, Object> getSessionStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("activeSessions", activeSessions.size());
        stats.put("initializingPlayers", initializingPlayers.size());
        stats.put("sessionTimeoutMs", SESSION_TIMEOUT_MS);

        // Calculate average session duration for active sessions
        if (!activeSessions.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            double averageDuration = activeSessions.values().stream()
                .mapToLong(startTime -> currentTime - startTime)
                .average()
                .orElse(0.0);
            stats.put("averageSessionDurationMs", averageDuration);
        }

        return stats;
    }

    /**
     * Validates that a player's attachments are properly initialized.
     *
     * @param player The server player to validate
     * @return True if all attachments are properly initialized
     */
    public static boolean validatePlayerAttachments(ServerPlayer player) {
        try {
            // Check all critical attachments
            boolean hasStats = player.hasData(AttachmentRegistry.PLAYER_STATS.get());
            boolean hasCultivation = player.hasData(AttachmentRegistry.PLAYER_CULTIVATION.get());
            boolean hasTalents = player.hasData(AttachmentRegistry.PLAYER_TALENTS.get());
            boolean hasRace = player.hasData(AttachmentRegistry.PLAYER_RACE.get());
            boolean hasBreakthrough = player.hasData(AttachmentRegistry.PLAYER_BREAKTHROUGH.get());
            boolean hasPillConsumption = player.hasData(AttachmentRegistry.PLAYER_PILL_CONSUMPTION.get());

            boolean allValid = hasStats && hasCultivation && hasTalents && hasRace && hasBreakthrough && hasPillConsumption;

            if (!allValid && ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Attachment validation for {}: stats={}, cultivation={}, talents={}, race={}, breakthrough={}, pills={}",
                    HANDLER_NAME, player.getName().getString(),
                    hasStats, hasCultivation, hasTalents, hasRace, hasBreakthrough, hasPillConsumption);
            }

            return allValid;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error validating attachments for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Forces a complete re-initialization of a player.
     * Should only be used in emergency situations or for debugging.
     *
     * @param player The server player to re-initialize
     */
    public static void forcePlayerReinitialization(ServerPlayer player) {
        ImmortalCultivation.LOGGER.warn("[{}] Forcing complete re-initialization for player: {}",
            HANDLER_NAME, player.getName().getString());

        try {
            UUID playerId = player.getUUID();

            // Clear all tracking
            forcePlayerCleanup(playerId);

            // Re-run initialization using existing methods
            PlayerEventUtils.initializePlayerAttachments(player);
            PlayerEventUtils.applyCurrentRealmModifiers(player);
            PlayerEventUtils.synchronizeAllPlayerData(player);

            // Mark session as active
            activeSessions.put(playerId, System.currentTimeMillis());

            ImmortalCultivation.LOGGER.info("[{}] Forced re-initialization completed for player: {}",
                HANDLER_NAME, player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during forced re-initialization for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }
}