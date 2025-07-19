package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.attachments.*;
import net.hyper.immortalcultivation.core.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.core.interfaces.ISyncable;
import net.hyper.immortalcultivation.core.interfaces.ITickable;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Unified Network Manager for the Immortal Cultivation Mod
 *
 * This class serves as the central hub for all network synchronization and tick processing
 * logic, replacing the previous ICModTickEvent system with a more sophisticated approach
 * that includes scheduling, prioritization, and performance optimization.
 *
 * Key Features:
 * - Unified tick and sync processing
 * - Scheduled sync operations with priority queuing
 * - Performance optimization through load balancing
 * - Comprehensive debug and monitoring capabilities
 * - Integration with existing packet registry
 * - Player-specific processing distribution
 *
 * Architecture:
 * - Singleton pattern for global access
 * - Event-driven processing through PlayerTickEvent
 * - Concurrent data structures for thread safety
 * - Modular design for easy extension
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class NetworkManager {

    // === SINGLETON INSTANCE === //
    public static final NetworkManager INSTANCE = new NetworkManager();

    // === CONFIGURATION CONSTANTS === //
    private static final int TICKABLE_INTERVAL = 1; // Tick every tick for ITickable attachments
    private static final int SYNC_CHECK_INTERVAL = 1; // Check for syncs every 5 ticks (4 times per second)
    private static final int MAX_SYNCS_PER_TICK = 5; // Maximum syncs to process per tick
    private static final int EMERGENCY_SYNC_PRIORITY = 0; // Highest priority for emergency syncs
    private static final int FULL_SYNC_PRIORITY = 1; // High priority for full syncs
    private static final int DELTA_SYNC_PRIORITY = 2; // Normal priority for delta syncs
    private static final int CLEANUP_INTERVAL = 12000; // Cleanup every 5 minutes (6000 ticks)

    // === DEBUG AND MONITORING === //
    private static boolean DEBUG_SYNC = false;
    private static boolean DEBUG_TICK = false;
    private static boolean DEBUG_PERFORMANCE = false;

    // === SYNC SCHEDULING SYSTEM === //

    /**
     * Represents a scheduled sync operation with priority and metadata
     */
    private static class ScheduledSync {
        final ServerPlayer player;
        final Supplier<AttachmentType<?>> attachmentSupplier;
        final boolean isFullSync;
        final int priority;
        final long scheduledTime;
        final String syncId;

        /**
         * Creates a new scheduled sync operation
         *
         * @param player The player to sync for
         * @param attachmentSupplier Supplier for the attachment type
         * @param isFullSync Whether this should be a full sync or delta sync
         * @param priority Priority level (lower = higher priority)
         */
        public ScheduledSync(ServerPlayer player, Supplier<AttachmentType<?>> attachmentSupplier,
                             boolean isFullSync, int priority) {
            this.player = player;
            this.attachmentSupplier = attachmentSupplier;
            this.isFullSync = isFullSync;
            this.priority = priority;
            this.scheduledTime = System.currentTimeMillis();
            this.syncId = generateSyncId(player, attachmentSupplier);
        }

        private String generateSyncId(ServerPlayer player, Supplier<AttachmentType<?>> supplier) {
            return player.getUUID().toString() + "_" + supplier.get().toString();
        }
    }

    // === DATA STRUCTURES === //

    // Priority queue for scheduled syncs (sorted by priority, then by time)
    private final Queue<ScheduledSync> syncQueue = new PriorityQueue<>(
        Comparator.comparingInt((ScheduledSync sync) -> sync.priority)
            .thenComparingLong(sync -> sync.scheduledTime)
    );

    // Active sync operations to prevent duplicates
    private final Set<String> activeSyncs = ConcurrentHashMap.newKeySet();

    // Performance tracking
    private final Map<String, Long> performanceTimers = new ConcurrentHashMap<>();
    private final Map<String, Integer> syncCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSyncTimes = new ConcurrentHashMap<>();

    // Player-specific data
    private final Map<UUID, Integer> playerTickOffsets = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCleanupTimes = new ConcurrentHashMap<>();

    // === CONSTRUCTOR === //
    private NetworkManager() {
        ImmortalCultivation.LOGGER.info("Initializing ICModNetworkManager...");
    }

    // === MAIN EVENT HANDLER === //

    /**
     * Main tick event handler that processes all tick and sync operations
     * This replaces the functionality from ICModTickEvent
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }

        INSTANCE.processPlayerTick(player);
    }

    /**
     * Processes tick operations for a specific player
     * Handles both ITickable processing and sync scheduling
     *
     * @param player The player to process
     */
    private void processPlayerTick(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // Get or create player-specific tick offset for load balancing
        int playerOffset = playerTickOffsets.computeIfAbsent(playerId,
            uuid -> uuid.hashCode() % SYNC_CHECK_INTERVAL);

        // === Process ITickable Attachments === //
        if ((player.tickCount + playerOffset) % TICKABLE_INTERVAL == 0) {
            processTickableAttachments(player);
        }

        // === Process Sync Operations === //
        if ((player.tickCount + playerOffset) % SYNC_CHECK_INTERVAL == 0) {
            checkAndProcessSyncs(player);
        }

        // === Periodic Cleanup === //
        if ((player.tickCount + playerOffset) % CLEANUP_INTERVAL == 0) {
            performPeriodicCleanup(player);
        }

        // === Process Scheduled Syncs === //
        processSyncQueue();
    }

    // === TICKABLE PROCESSING === //

    /**
     * Processes all ITickable attachments for a player
     * This replaces the tickable processing from ICModTickEvent
     *
     * @param player The player whose attachments to tick
     */
    private void processTickableAttachments(ServerPlayer player) {
        if (DEBUG_TICK) {
            ImmortalCultivation.LOGGER.debug("Processing tickable attachments for player: {}",
                player.getName().getString());
        }

        startPerformanceTimer("tickable_processing");

        try {
            // Process all ITickable attachments
            player.getExistingData(AttachmentRegistry.PLAYER_CULTIVATION.get())
                .ifPresent(ITickable::tick);
            player.getExistingData(AttachmentRegistry.PLAYER_BREAKTHROUGH.get())
                .ifPresent(ITickable::tick);
            player.getExistingData(AttachmentRegistry.PLAYER_STATS.get())
                .ifPresent(ITickable::tick);

            if (DEBUG_TICK) {
                ImmortalCultivation.LOGGER.debug("Completed tickable processing for player: {}",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing tickable attachments for player {}: {}",
                player.getName().getString(), e.getMessage());
            e.printStackTrace();
        } finally {
            endPerformanceTimer("tickable_processing", 5);
        }
    }

    // === SYNC SCHEDULING === //

    /**
     * Schedules a sync operation for a specific player and attachment
     * This provides a unified interface for all sync scheduling needs
     *
     * @param player The player to sync for
     * @param attachmentSupplier Supplier for the attachment type
     * @param isFullSync Whether to perform a full sync or delta sync
     */
    public void scheduleSync(ServerPlayer player, Supplier<AttachmentType<?>> attachmentSupplier, boolean isFullSync) {
        scheduleSync(player, attachmentSupplier, isFullSync, isFullSync ? FULL_SYNC_PRIORITY : DELTA_SYNC_PRIORITY);
    }

    /**
     * Schedules a sync operation with specific priority
     *
     * @param player The player to sync for
     * @param attachmentSupplier Supplier for the attachment type
     * @param isFullSync Whether to perform a full sync or delta sync
     * @param priority Priority level (lower = higher priority)
     */
    public void scheduleSync(ServerPlayer player, Supplier<AttachmentType<?>> attachmentSupplier,
                             boolean isFullSync, int priority) {
        ScheduledSync sync = new ScheduledSync(player, attachmentSupplier, isFullSync, priority);

        // Prevent duplicate syncs
        if (activeSyncs.add(sync.syncId)) {
            syncQueue.offer(sync);

            if (DEBUG_SYNC) {
                ImmortalCultivation.LOGGER.debug("Scheduled {} sync for player {} attachment {} with priority {}",
                    isFullSync ? "full" : "delta",
                    player.getName().getString(),
                    attachmentSupplier.get().toString(),
                    priority);
            }
        } else if (DEBUG_SYNC) {
            ImmortalCultivation.LOGGER.debug("Skipped duplicate sync for player {} attachment {}",
                player.getName().getString(), attachmentSupplier.get().toString());
        }
    }

    /**
     * Schedules an emergency sync with highest priority
     * Used for critical synchronization needs
     *
     * @param player The player to sync for
     * @param attachmentSupplier Supplier for the attachment type
     */
    public void scheduleEmergencySync(ServerPlayer player, Supplier<AttachmentType<?>> attachmentSupplier) {
        scheduleSync(player, attachmentSupplier, true, EMERGENCY_SYNC_PRIORITY);

        ImmortalCultivation.LOGGER.warn("Emergency sync scheduled for player {} attachment {}",
            player.getName().getString(), attachmentSupplier.get().toString());
    }

    // === PERFORMANCE MONITORING === //

    /**
     * Starts a performance timer for the given operation
     *
     * @param operation The operation name to track
     */
    private void startPerformanceTimer(String operation) {
        if (DEBUG_PERFORMANCE) {
            performanceTimers.put(operation, System.nanoTime());
        }
    }

    /**
     * Ends a performance timer and logs if it exceeds the threshold
     *
     * @param operation The operation name being tracked
     * @param warnThresholdMs Warning threshold in milliseconds
     */
    private void endPerformanceTimer(String operation, long warnThresholdMs) {
        if (DEBUG_PERFORMANCE) {
            Long startTime = performanceTimers.remove(operation);
            if (startTime != null) {
                long duration = System.nanoTime() - startTime;
                long durationMs = duration / 1_000_000;

                if (durationMs > warnThresholdMs) {
                    ImmortalCultivation.LOGGER.warn("Operation '{}' took {}ms (exceeds {}ms threshold)",
                        operation, durationMs, warnThresholdMs);
                } else if (DEBUG_PERFORMANCE) {
                    ImmortalCultivation.LOGGER.debug("Operation '{}' completed in {}ms",
                        operation, durationMs);
                }
            }
        }
    }

    // === DEBUG AND CONFIGURATION === //

    /**
     * Enables or disables sync debugging
     *
     * @param enabled Whether to enable sync debugging
     */
    public static void setDebugSync(boolean enabled) {
        DEBUG_SYNC = enabled;
        ImmortalCultivation.LOGGER.info("NetworkManager sync debugging {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Enables or disables tick debugging
     *
     * @param enabled Whether to enable tick debugging
     */
    public static void setDebugTick(boolean enabled) {
        DEBUG_TICK = enabled;
        ImmortalCultivation.LOGGER.info("NetworkManager tick debugging {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Enables or disables performance debugging
     *
     * @param enabled Whether to enable performance debugging
     */
    public static void setDebugPerformance(boolean enabled) {
        DEBUG_PERFORMANCE = enabled;
        ImmortalCultivation.LOGGER.info("NetworkManager performance debugging {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Gets the current number of pending sync operations
     *
     * @return Number of pending syncs in the queue
     */
    public int getPendingSyncCount() {
        return syncQueue.size();
    }

    /**
     * Gets the current number of active sync operations
     *
     * @return Number of active syncs being processed
     */
    public int getActiveSyncCount() {
        return activeSyncs.size();
    }

    // === SYNC PROCESSING === //

    /**
     * Checks and processes synchronization for all attachments of a player
     * This replaces the sync checking logic from ICModTickEvent
     *
     * @param player The player to check syncs for
     */
    private void checkAndProcessSyncs(ServerPlayer player) {
        if (DEBUG_SYNC) {
            ImmortalCultivation.LOGGER.debug("Checking sync for player {} at tick {}",
                player.getName().getString(), player.tickCount);
        }

        startPerformanceTimer("sync_check");

        try {
            // Check all ISyncable attachments for dirty state
            checkAndSendSyncPacket(player, player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get()), "PlayerCultivation");
            checkAndSendSyncPacket(player, player.getData(AttachmentRegistry.PLAYER_BREAKTHROUGH.get()), "PlayerBreakthrough");
            checkAndSendSyncPacket(player, player.getData(AttachmentRegistry.PLAYER_STATS.get()), "PlayerStats");
            checkAndSendSyncPacket(player, player.getData(AttachmentRegistry.PLAYER_RACE.get()), "PlayerRace");
            checkAndSendSyncPacket(player, player.getData(AttachmentRegistry.PLAYER_TALENTS.get()), "PlayerTalents");
            checkAndSendSyncPacket(player, player.getData(AttachmentRegistry.PLAYER_PILL_CONSUMPTION.get()), "PlayerPillConsumption");
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error during sync check for player {}: {}",
                player.getName().getString(), e.getMessage());
            e.printStackTrace();
        } finally {
            endPerformanceTimer("sync_check", 10);
        }
    }

    /**
     * Checks a specific attachment for sync needs and sends packet if necessary
     * This replaces the checkAndSendSyncPacket method from ICModTickEvent
     *
     * @param player The player the attachment belongs to
     * @param syncable The attachment to check
     * @param attachmentName Human-readable name for debugging
     */
    private void checkAndSendSyncPacket(ServerPlayer player, ISyncable syncable, String attachmentName) {
        if (DEBUG_SYNC) {
            ImmortalCultivation.LOGGER.debug("=== checkAndSendSyncPacket called for {} ===", attachmentName);
        }

        if (syncable == null) {
            ImmortalCultivation.LOGGER.warn("Null {} attachment for player {}",
                attachmentName, player.getName().getString());
            return;
        }

        // Ensure player reference is set before checking dirty state
        ensurePlayerReferenceSet(syncable, player, attachmentName);

        boolean isDirty = syncable.isDirty();
        if (DEBUG_SYNC) {
            ImmortalCultivation.LOGGER.debug("{} isDirty: {}", attachmentName, isDirty);
        }

        if (isDirty) {
            try {
                if (DEBUG_SYNC) {
                    ImmortalCultivation.LOGGER.debug("About to call createPacket for {}", attachmentName);
                }

                Optional<ISyncPacket> packetOptional = syncable.createPacket(false);

                if (DEBUG_SYNC) {
                    ImmortalCultivation.LOGGER.debug("createPacket returned: {}",
                        packetOptional.isPresent() ? "PACKET" : "EMPTY");
                }

                if (packetOptional.isPresent()) {
                    ISyncPacket packet = packetOptional.get();
                    NetworkRegistry.sendToPlayer(packet, player);
                    syncable.clearDirty();

                    // Update tracking
                    String syncKey = player.getUUID() + "_" + attachmentName;
                    syncCounts.merge(syncKey, 1, Integer::sum);
                    lastSyncTimes.put(syncKey, System.currentTimeMillis());

                    if (DEBUG_SYNC) {
                        ImmortalCultivation.LOGGER.debug("Successfully sent {} packet", attachmentName);
                    }
                } else {
                    if (DEBUG_SYNC) {
                        ImmortalCultivation.LOGGER.debug("{} returned empty packet (dirty flag NOT cleared)", attachmentName);
                    }
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Exception in checkAndSendSyncPacket for {}: {}",
                    attachmentName, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes the sync queue to handle scheduled sync operations
     * Limits the number of syncs processed per tick for performance
     */
    private void processSyncQueue() {
        if (syncQueue.isEmpty()) {
            return;
        }

        startPerformanceTimer("sync_queue_processing");
        int processedCount = 0;

        try {
            while (!syncQueue.isEmpty() && processedCount < MAX_SYNCS_PER_TICK) {
                ScheduledSync sync = syncQueue.poll();
                if (sync != null) {
                    processScheduledSync(sync);
                    activeSyncs.remove(sync.syncId);
                    processedCount++;
                }
            }

            if (DEBUG_SYNC && processedCount > 0) {
                ImmortalCultivation.LOGGER.debug("Processed {} scheduled syncs", processedCount);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing sync queue: {}", e.getMessage());
            e.printStackTrace();
        } finally {
            endPerformanceTimer("sync_queue_processing", 5);
        }
    }

    /**
     * Processes a single scheduled sync operation
     *
     * @param sync The scheduled sync to process
     */
    private void processScheduledSync(ScheduledSync sync) {
        try {
            // Verify player is still valid
            if (!sync.player.isAlive() || sync.player.hasDisconnected()) {
                if (DEBUG_SYNC) {
                    ImmortalCultivation.LOGGER.debug("Skipping sync for disconnected/dead player: {}",
                        sync.player.getName().getString());
                }
                return;
            }

            // Get the attachment
            Object attachment = sync.player.getData(sync.attachmentSupplier.get());
            if (attachment instanceof ISyncable syncable) {
                ensurePlayerReferenceSet(syncable, sync.player, sync.attachmentSupplier.get().toString());

                Optional<ISyncPacket> packetOptional = syncable.createPacket(sync.isFullSync);
                if (packetOptional.isPresent()) {
                    ISyncPacket packet = packetOptional.get();
                    NetworkRegistry.sendToPlayer(packet, sync.player);
                    syncable.clearDirty();

                    if (DEBUG_SYNC) {
                        ImmortalCultivation.LOGGER.debug("Processed scheduled {} sync for player {} attachment {}",
                            sync.isFullSync ? "full" : "delta",
                            sync.player.getName().getString(),
                            sync.attachmentSupplier.get().toString());
                    }
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing scheduled sync for player {}: {}",
                sync.player.getName().getString(), e.getMessage());
            e.printStackTrace();
        }
    }

    // === ATTACHMENT HELPER METHODS === //

    /**
     * Ensures player references are properly set on attachments
     * This replaces the ensurePlayerReferenceSet method from ICModTickEvent
     *
     * @param syncable The attachment to set player reference on
     * @param player The player to set as reference
     * @param attachmentName Name for debugging purposes
     */
    private void ensurePlayerReferenceSet(ISyncable syncable, ServerPlayer player, String attachmentName) {
        if (syncable instanceof PlayerTalentsAttachment talents) {
            talents.setPlayer(player);
            if (DEBUG_SYNC) {
                ImmortalCultivation.LOGGER.debug("Set player reference for PlayerTalentsAttachment");
            }
        } else if (syncable instanceof PlayerStatsAttachment stats) {
            stats.setPlayer(player);
            if (DEBUG_SYNC) {
                ImmortalCultivation.LOGGER.debug("Set player reference for PlayerResourceAttachment");
            }
        } else if (syncable instanceof PlayerCultivationAttachment cultivation) {
            cultivation.setPlayer(player);
            if (DEBUG_SYNC) {
                ImmortalCultivation.LOGGER.debug("Set player reference for PlayerCultivationAttachment");
            }
        } else if (syncable instanceof PlayerRaceAttachment race) {
            race.setPlayer(player);
            if (DEBUG_SYNC) {
                ImmortalCultivation.LOGGER.debug("Set player reference for PlayerRaceAttachment");
            }
        } else if (syncable instanceof PlayerBreakthroughAttachment breakthrough) {
            breakthrough.setPlayer(player);
            if (DEBUG_SYNC) {
                ImmortalCultivation.LOGGER.debug("Set player reference for PlayerBreakthroughAttachment");
            }
        } else if (syncable instanceof PlayerPillConsumptionAttachment pills) {
            pills.setPlayer(player);
            if (DEBUG_SYNC) {
                ImmortalCultivation.LOGGER.debug("Set player reference for PlayerPillConsumptionAttachment");
            }
        }
    }

    // === CLEANUP AND UTILITY METHODS === //

    /**
     * Performs periodic cleanup to prevent memory leaks and optimize performance
     *
     * @param player The player to perform cleanup for
     */
    private void performPeriodicCleanup(ServerPlayer player) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        // Only perform cleanup if enough time has passed
        Long lastCleanup = lastCleanupTimes.get(playerId);
        if (lastCleanup != null && (currentTime - lastCleanup) < 300_000) { // 5 minutes
            return;
        }

        startPerformanceTimer("periodic_cleanup");

        try {
            // Clean old sync tracking data
            String playerPrefix = playerId.toString() + "_";
            syncCounts.entrySet().removeIf(entry ->
                entry.getKey().startsWith(playerPrefix) &&
                    (currentTime - lastSyncTimes.getOrDefault(entry.getKey(), 0L)) > 600_000); // 10 minutes

            lastSyncTimes.entrySet().removeIf(entry ->
                entry.getKey().startsWith(playerPrefix) &&
                    (currentTime - entry.getValue()) > 600_000); // 10 minutes

            lastCleanupTimes.put(playerId, currentTime);

            if (DEBUG_SYNC) {
                ImmortalCultivation.LOGGER.debug("Performed periodic cleanup for player: {}",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error during periodic cleanup for player {}: {}",
                player.getName().getString(), e.getMessage());
        } finally {
            endPerformanceTimer("periodic_cleanup", 5);
        }
    }

    /**
     * Forces a full sync of all attachments for a player
     * Useful for debugging or when data integrity issues are detected
     *
     * @param player The player to force sync for
     */
    public void forceFullSyncAll(ServerPlayer player) {
        ImmortalCultivation.LOGGER.info("Forcing full sync of all attachments for player: {}",
            player.getName().getString());

        // Schedule full sync for all attachments with emergency priority
        scheduleSync(player, AttachmentRegistry.PLAYER_CULTIVATION::get, true, EMERGENCY_SYNC_PRIORITY);
        scheduleSync(player, AttachmentRegistry.PLAYER_TALENTS::get, true, EMERGENCY_SYNC_PRIORITY);
        scheduleSync(player, AttachmentRegistry.PLAYER_RACE::get, true, EMERGENCY_SYNC_PRIORITY);
        scheduleSync(player, AttachmentRegistry.PLAYER_STATS::get, true, EMERGENCY_SYNC_PRIORITY);
        scheduleSync(player, AttachmentRegistry.PLAYER_BREAKTHROUGH::get, true, EMERGENCY_SYNC_PRIORITY);
        scheduleSync(player, AttachmentRegistry.PLAYER_PILL_CONSUMPTION::get, true, EMERGENCY_SYNC_PRIORITY);
    }

    /**
     * Gets detailed statistics about sync operations for debugging
     *
     * @return Map containing various statistics
     */
    public Map<String, Object> getSyncStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingSyncs", syncQueue.size());
        stats.put("activeSyncs", activeSyncs.size());
        stats.put("totalSyncEntries", syncCounts.size());
        stats.put("performanceTimers", performanceTimers.size());
        stats.put("trackedPlayers", playerTickOffsets.size());

        // Calculate total syncs
        int totalSyncs = syncCounts.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("totalSyncs", totalSyncs);

        return stats;
    }

    /**
     * Clears all tracking data and resets the manager
     * Should only be used for debugging or testing
     */
    public void reset() {
        ImmortalCultivation.LOGGER.warn("Resetting ICModNetworkManager - all tracking data will be cleared");

        syncQueue.clear();
        activeSyncs.clear();
        performanceTimers.clear();
        syncCounts.clear();
        lastSyncTimes.clear();
        playerTickOffsets.clear();
        lastCleanupTimes.clear();
    }
}