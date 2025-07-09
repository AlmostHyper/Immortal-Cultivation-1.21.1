package net.hyper.immortalcultivation.server.event.world;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.IDirtyTrackable;
import net.hyper.immortalcultivation.common.interfaces.ITickable;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * UNIFIED CULTIVATION TICK EVENT SYSTEM
 *
 * High-performance tick handler that consolidates all cultivation system ticking
 * with advanced caching, load balancing, and performance monitoring.
 *
 * Performance Features:
 * - Multi-tier caching system with different staleness thresholds
 * - Dynamic load balancing with player-specific tick offsets
 * - Adaptive tick intervals based on system load
 * - Comprehensive performance monitoring and profiling
 * - Memory-efficient cache cleanup with smart eviction policies
 * - Batch processing for expensive operations
 * - Error isolation to prevent cascade failures
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class ICModTickEvent {

    // === PERFORMANCE CONSTANTS === //

    // Base tick intervals (can be dynamically adjusted)
    private static final int BASE_CULTIVATION_INTERVAL = 4;     // 5 times per second
    private static final int BASE_RESOURCE_INTERVAL = 8;        // 2.5 times per second
    private static final int BASE_COMBAT_STATS_INTERVAL = 20;   // 1 time per second
    private static final int BASE_SECONDARY_INTERVAL = 40;      // 0.5 times per second

    // Cache management intervals
    private static final int CACHE_CLEANUP_INTERVAL = 400;      // Every 20 seconds
    private static final int PERFORMANCE_LOG_INTERVAL = 1200;   // Every 60 seconds
    private static final int ADAPTIVE_ADJUSTMENT_INTERVAL = 600; // Every 30 seconds

    // Cache staleness thresholds (in milliseconds)
    private static final long PRIMARY_CACHE_STALENESS = 15000;   // 15 seconds
    private static final long SECONDARY_CACHE_STALENESS = 30000; // 30 seconds
    private static final long PERFORMANCE_CACHE_STALENESS = 5000; // 5 seconds

    // Performance thresholds
    private static final long MAX_TICK_TIME_NANOS = 2_000_000;   // 2ms max per player per tick
    private static final int MAX_PLAYERS_PER_TICK = 10;          // Process max 10 players per tick
    private static final double LOAD_ADJUSTMENT_THRESHOLD = 0.8;  // Adjust intervals at 80% load

    // === CACHING SYSTEM === //

    /**
     * Multi-tier attachment cache with performance tracking
     */
    private static class AttachmentCache {
        // Primary attachments (most frequently accessed)
        PlayerCultivationAttachment cultivation;
        PlayerResourceAttachment resources;
        PlayerCombatStatsAttachment combatStats;

        // Secondary attachments (less frequently accessed)
        PlayerTalentAttachment talents;
        PlayerRaceAttachment race;
        PlayerBreakthroughAttachment breakthrough;
        PlayerPillConsumptionAttachment pillConsumption;

        // Cache metadata
        long primaryCacheTime;
        long secondaryCacheTime;
        long lastAccessTime;
        int accessCount;
        boolean isValid;

        // Performance tracking
        long totalTickTime;
        int tickCount;

        AttachmentCache(ServerPlayer player) {
            refreshPrimary(player);
            refreshSecondary(player);
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount = 0;
            this.isValid = true;
        }

        void refreshPrimary(ServerPlayer player) {
            this.cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            this.resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            this.combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            this.primaryCacheTime = System.currentTimeMillis();

            // Ensure player references are set
            if (cultivation != null && cultivation.getPlayerSafe() != player) {
                cultivation.setPlayer(player);
            }
            if (resources != null && resources.getPlayerSafe() != player) {
                resources.setPlayer(player);
            }
            if (combatStats != null && combatStats.getPlayerSafe() != player) {
                combatStats.setPlayer(player);
            }
        }

        void refreshSecondary(ServerPlayer player) {
            this.talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            this.race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            this.breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
            this.pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());
            this.secondaryCacheTime = System.currentTimeMillis();

            // Ensure player references are set
            if (talents != null && talents.getPlayerSafe() != player) {
                talents.setPlayer(player);
            }
            if (race != null && race.getPlayerSafe() != player) {
                race.setPlayer(player);
            }
            if (breakthrough != null && breakthrough.getPlayerSafe() != player) {
                breakthrough.setPlayer(player);
            }
            if (pillConsumption != null && pillConsumption.getPlayerSafe() != player) {
                pillConsumption.setPlayer(player);
            }
        }

        boolean isPrimaryCacheStale() {
            return System.currentTimeMillis() - primaryCacheTime > PRIMARY_CACHE_STALENESS;
        }

        boolean isSecondaryCacheStale() {
            return System.currentTimeMillis() - secondaryCacheTime > SECONDARY_CACHE_STALENESS;
        }

        boolean shouldEvict() {
            long timeSinceAccess = System.currentTimeMillis() - lastAccessTime;
            return timeSinceAccess > SECONDARY_CACHE_STALENESS * 2 || !isValid;
        }

        void recordAccess() {
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount++;
        }

        void recordTickPerformance(long tickTimeNanos) {
            this.totalTickTime += tickTimeNanos;
            this.tickCount++;
        }

        double getAverageTickTime() {
            return tickCount > 0 ? (double) totalTickTime / tickCount / 1_000_000.0 : 0.0; // Convert to ms
        }

        void invalidate() {
            this.isValid = false;
        }
    }

    // === STATIC FIELDS ===

    // Main cache storage
    private static final Map<UUID, AttachmentCache> attachmentCache = new ConcurrentHashMap<>();

    // Performance monitoring
    private static final AtomicInteger globalTickCounter = new AtomicInteger(0);
    private static final AtomicLong totalProcessingTime = new AtomicLong(0);
    private static final AtomicInteger totalPlayersProcessed = new AtomicInteger(0);

    // Adaptive intervals (can be adjusted based on performance)
    private static volatile int currentCultivationInterval = BASE_CULTIVATION_INTERVAL;
    private static volatile int currentResourceInterval = BASE_RESOURCE_INTERVAL;
    private static volatile int currentCombatStatsInterval = BASE_COMBAT_STATS_INTERVAL;
    private static volatile int currentSecondaryInterval = BASE_SECONDARY_INTERVAL;

    // Load balancing
    private static final AtomicInteger playersThisTick = new AtomicInteger(0);
    private static volatile long lastOverloadTime = 0;

    // Performance tracking by operation type
    private static final EnumMap<TickOperation, AtomicLong> operationTimes = new EnumMap<>(TickOperation.class);
    private static final EnumMap<TickOperation, AtomicInteger> operationCounts = new EnumMap<>(TickOperation.class);

    static {
        for (TickOperation op : TickOperation.values()) {
            operationTimes.put(op, new AtomicLong(0));
            operationCounts.put(op, new AtomicInteger(0));
        }
    }

    private enum TickOperation {
        CULTIVATION, RESOURCES, COMBAT_STATS, SECONDARY_SYSTEMS, CACHE_MANAGEMENT
    }

    // === MAIN TICK HANDLER ===

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        long startTime = System.nanoTime();
        int currentTick = globalTickCounter.incrementAndGet();

        try {
            // Load balancing: skip if too many players processed this tick
            if (playersThisTick.get() >= MAX_PLAYERS_PER_TICK) {
                return;
            }

            // Get player-specific tick offset for load distribution
            int playerOffset = Math.abs(serverPlayer.getUUID().hashCode()) % 100;
            int adjustedTick = (serverPlayer.tickCount + playerOffset) % 1000;

            // Get or create cached attachments
            AttachmentCache cache = getOrCreateCache(serverPlayer);
            if (cache == null) {
                return; // Skip if cache creation failed
            }

            playersThisTick.incrementAndGet();
            boolean processedAnyOperation = false;

            // Process cultivation (highest priority)
            if (adjustedTick % currentCultivationInterval == 0) {
                if (processCultivation(serverPlayer, cache, startTime)) {
                    processedAnyOperation = true;
                }
            }

            // Process resources (high priority)
            if (adjustedTick % currentResourceInterval == 0) {
                if (processResources(serverPlayer, cache, startTime)) {
                    processedAnyOperation = true;
                }
            }

            // Process combat stats (medium priority)
            if (adjustedTick % currentCombatStatsInterval == 0) {
                if (processCombatStats(serverPlayer, cache, startTime)) {
                    processedAnyOperation = true;
                }
            }

            // Process secondary systems (low priority)
            if (adjustedTick % currentSecondaryInterval == 0) {
                if (processSecondarySystem(serverPlayer, cache, startTime)) {
                    processedAnyOperation = true;
                }
            }

            // Record performance metrics if any operation was processed
            if (processedAnyOperation) {
                long processingTime = System.nanoTime() - startTime;
                cache.recordTickPerformance(processingTime);
                totalProcessingTime.addAndGet(processingTime);
                totalPlayersProcessed.incrementAndGet();

                // Check for performance issues
                if (processingTime > MAX_TICK_TIME_NANOS) {
                    handlePerformanceOverload(serverPlayer, processingTime);
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error in cultivation tick for player {}: {}",
                serverPlayer.getName().getString(), e.getMessage());
            // Invalidate cache to force refresh
            AttachmentCache cache = attachmentCache.get(serverPlayer.getUUID());
            if (cache != null) {
                cache.invalidate();
            }
        }

        // Process pending unified syncs every few ticks to batch efficiently
        if (currentTick % 4 == 0) { // Every 4 ticks = 5 times per second
            NetworkSyncManager.processPendingSyncs();
        }

        // Periodic maintenance
        if (currentTick % CACHE_CLEANUP_INTERVAL == 0) {
            performCacheCleanup();
        }

        if (currentTick % PERFORMANCE_LOG_INTERVAL == 0) {
            logPerformanceMetrics();
        }

        if (currentTick % ADAPTIVE_ADJUSTMENT_INTERVAL == 0) {
            adjustTickIntervals();
        }

        // Reset player counter every tick
        if (currentTick % 20 == 0) { // Every second
            playersThisTick.set(0);
        }
    }

    // === PROCESSING METHODS ===

    /**
     * Generic attachment processing method that works with any IDirtyTrackable
     */
    private static boolean processAttachment(ServerPlayer player, IDirtyTrackable attachment,
                                             int tickInterval, String attachmentName, TickOperation operation) {
        if (attachment == null) return false;

        long operationStart = System.nanoTime();

        try {
            // Call tick method if the attachment supports it
            if (attachment instanceof ITickable tickable) {
                for (int i = 0; i < tickInterval; i++) {
                    tickable.tick();
                }
            }

            // Instead of calling forceSync() directly, let NetworkSyncManager handle it
            // The individual attachment's shouldSyncNow() logic is still respected
            if (attachment.shouldSyncNow()) {
                // Determine sync priority based on attachment type and change frequency
                NetworkSyncManager.SyncPriority priority = determineSyncPriority(attachment, attachmentName);

                // Schedule sync through the unified system
                NetworkSyncManager.scheduleSync(player, priority);

            }

            return true;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.warn("Error processing {} for player {}: {}",
                attachmentName, player.getName().getString(), e.getMessage());
            return false;
        } finally {
            long duration = System.nanoTime() - operationStart;
            operationTimes.get(operation).addAndGet(duration);
            operationCounts.get(operation).incrementAndGet();
        }
    }

    // Update the calling methods:
    private static boolean processResources(ServerPlayer player, AttachmentCache cache, long startTime) {
        return processAttachment(player, cache.resources, currentResourceInterval, "resources", TickOperation.RESOURCES);
    }

    private static boolean processCultivation(ServerPlayer player, AttachmentCache cache, long startTime) {
        return processAttachment(player, cache.cultivation, currentCultivationInterval, "cultivation", TickOperation.CULTIVATION);
    }

    private static boolean processCombatStats(ServerPlayer player, AttachmentCache cache, long startTime) {
        return processAttachment(player, cache.combatStats, currentCombatStatsInterval, "combat stats", TickOperation.COMBAT_STATS);
    }

    private static boolean processSecondarySystem(ServerPlayer player, AttachmentCache cache, long startTime) {
        long operationStart = System.nanoTime();

        try {
            // Refresh secondary cache if needed
            if (cache.isSecondaryCacheStale()) {
                cache.refreshSecondary(player);
            }

            boolean processed = false;

            // Process talents using the generic system
            if (cache.talents instanceof IDirtyTrackable) {
                if (processAttachment(player, (IDirtyTrackable) cache.talents,
                    currentSecondaryInterval, "talents", TickOperation.SECONDARY_SYSTEMS)) {
                    processed = true;
                }
            }

            // Process race using the generic system
            if (cache.race instanceof IDirtyTrackable) {
                if (processAttachment(player, (IDirtyTrackable) cache.race,
                    currentSecondaryInterval, "race", TickOperation.SECONDARY_SYSTEMS)) {
                    processed = true;
                }
            }

            // Process breakthrough using the generic system
            if (cache.breakthrough instanceof IDirtyTrackable) {
                if (processAttachment(player, (IDirtyTrackable) cache.breakthrough,
                    currentSecondaryInterval, "breakthrough", TickOperation.SECONDARY_SYSTEMS)) {
                    processed = true;
                }
            }

            // Process pill consumption using the generic system
            if (cache.pillConsumption instanceof IDirtyTrackable) {
                if (processAttachment(player, (IDirtyTrackable) cache.pillConsumption,
                    currentSecondaryInterval, "pill consumption", TickOperation.SECONDARY_SYSTEMS)) {
                    processed = true;
                }
            }

            return processed;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.warn("Error processing secondary systems for player {}: {}",
                player.getName().getString(), e.getMessage());
            return false;
        } finally {
            long duration = System.nanoTime() - operationStart;
            operationTimes.get(TickOperation.SECONDARY_SYSTEMS).addAndGet(duration);
            operationCounts.get(TickOperation.SECONDARY_SYSTEMS).incrementAndGet();
        }
    }

    // === CACHE MANAGEMENT ===

    private static AttachmentCache getOrCreateCache(ServerPlayer player) {
        UUID playerId = player.getUUID();
        AttachmentCache cache = attachmentCache.get(playerId);

        if (cache == null || !cache.isValid) {
            cache = new AttachmentCache(player);
            attachmentCache.put(playerId, cache);
        } else {
            // Check if primary cache needs refresh
            if (cache.isPrimaryCacheStale()) {
                cache.refreshPrimary(player);
            }

            // Record access for LRU eviction
            cache.recordAccess();
        }

        return cache;
    }

    private static void performCacheCleanup() {
        long cleanupStart = System.nanoTime();

        try {
            int initialSize = attachmentCache.size();
            attachmentCache.entrySet().removeIf(entry -> entry.getValue().shouldEvict());
            int finalSize = attachmentCache.size();

            if (globalTickCounter.get() % (PERFORMANCE_LOG_INTERVAL / 4) == 0) {
                ImmortalCultivation.LOGGER.debug("Cache cleanup: {} -> {} entries", initialSize, finalSize);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error during cache cleanup: {}", e.getMessage());
        } finally {
            long duration = System.nanoTime() - cleanupStart;
            operationTimes.get(TickOperation.CACHE_MANAGEMENT).addAndGet(duration);
            operationCounts.get(TickOperation.CACHE_MANAGEMENT).incrementAndGet();
        }
    }

    // === PERFORMANCE MONITORING ===

    private static void handlePerformanceOverload(ServerPlayer player, long processingTime) {
        lastOverloadTime = System.currentTimeMillis();
        ImmortalCultivation.LOGGER.warn("Performance overload detected for player {}: {:.2f}ms",
            player.getName().getString(), processingTime / 1_000_000.0);

        // Consider invalidating cache to force refresh
        AttachmentCache cache = attachmentCache.get(player.getUUID());
        if (cache != null) {
            cache.invalidate();
        }
    }

    private static void adjustTickIntervals() {
        long currentTime = System.currentTimeMillis();
        boolean recentOverload = (currentTime - lastOverloadTime) < 30000; // 30 seconds

        double averageTickTime = totalPlayersProcessed.get() > 0 ?
            (double) totalProcessingTime.get() / totalPlayersProcessed.get() / 1_000_000.0 : 0.0;

        if (recentOverload || averageTickTime > LOAD_ADJUSTMENT_THRESHOLD) {
            // Increase intervals to reduce load
            currentCultivationInterval = Math.min(currentCultivationInterval + 1, BASE_CULTIVATION_INTERVAL * 2);
            currentResourceInterval = Math.min(currentResourceInterval + 1, BASE_RESOURCE_INTERVAL * 2);
            currentCombatStatsInterval = Math.min(currentCombatStatsInterval + 2, BASE_COMBAT_STATS_INTERVAL * 2);
            currentSecondaryInterval = Math.min(currentSecondaryInterval + 4, BASE_SECONDARY_INTERVAL * 2);

            ImmortalCultivation.LOGGER.info("Adjusted tick intervals due to high load. Cultivation: {}, Resources: {}, Combat: {}, Secondary: {}",
                currentCultivationInterval, currentResourceInterval, currentCombatStatsInterval, currentSecondaryInterval);
        } else if (averageTickTime < LOAD_ADJUSTMENT_THRESHOLD * 0.5) {
            // Decrease intervals to improve responsiveness
            currentCultivationInterval = Math.max(currentCultivationInterval - 1, BASE_CULTIVATION_INTERVAL);
            currentResourceInterval = Math.max(currentResourceInterval - 1, BASE_RESOURCE_INTERVAL);
            currentCombatStatsInterval = Math.max(currentCombatStatsInterval - 1, BASE_COMBAT_STATS_INTERVAL);
            currentSecondaryInterval = Math.max(currentSecondaryInterval - 2, BASE_SECONDARY_INTERVAL);
        }
    }

    private static void logPerformanceMetrics() {
        int totalPlayers = totalPlayersProcessed.get();
        if (totalPlayers == 0) return;

        double avgProcessingTime = (double) totalProcessingTime.get() / totalPlayers / 1_000_000.0;

        ImmortalCultivation.LOGGER.info("=== Cultivation Tick Performance ===");
        ImmortalCultivation.LOGGER.info("Players processed: {}, Avg time: {:.3f}ms", totalPlayers, avgProcessingTime);
        ImmortalCultivation.LOGGER.info("Cache size: {}", attachmentCache.size());
        ImmortalCultivation.LOGGER.info("Current intervals - Cultivation: {}, Resources: {}, Combat: {}, Secondary: {}",
            currentCultivationInterval, currentResourceInterval, currentCombatStatsInterval, currentSecondaryInterval);

        // Log operation-specific metrics
        for (TickOperation op : TickOperation.values()) {
            int count = operationCounts.get(op).get();
            if (count > 0) {
                double avgTime = (double) operationTimes.get(op).get() / count / 1_000_000.0;
                ImmortalCultivation.LOGGER.info("{}: {} operations, {:.3f}ms avg", op.name(), count, avgTime);
            }
        }

        // Reset counters
        totalProcessingTime.set(0);
        totalPlayersProcessed.set(0);
        for (TickOperation op : TickOperation.values()) {
            operationTimes.get(op).set(0);
            operationCounts.get(op).set(0);
        }
    }

    // === EVENT HANDLERS ===

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            clearPlayerCache(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Invalidate cache to force refresh of all attachments
            AttachmentCache cache = attachmentCache.get(serverPlayer.getUUID());
            if (cache != null) {
                cache.invalidate();
            }
        }
    }

    // === PUBLIC API ===

    /**
     * Clear cache for a specific player (useful for dimension changes, logout, etc.)
     */
    public static void clearPlayerCache(UUID playerId) {
        attachmentCache.remove(playerId);
    }

    /**
     * Clear all caches (useful for server shutdown)
     */
    public static void clearAllCaches() {
        attachmentCache.clear();
        totalProcessingTime.set(0);
        totalPlayersProcessed.set(0);
        for (TickOperation op : TickOperation.values()) {
            operationTimes.get(op).set(0);
            operationCounts.get(op).set(0);
        }
    }

    /**
     * Force refresh cache for a specific player
     */
    public static void refreshPlayerCache(ServerPlayer player) {
        AttachmentCache cache = attachmentCache.get(player.getUUID());
        if (cache != null) {
            cache.refreshPrimary(player);
            cache.refreshSecondary(player);
        }
    }

    /**
     * Get current performance metrics
     */
    public static String getPerformanceReport() {
        int totalPlayers = totalPlayersProcessed.get();
        if (totalPlayers == 0) {
            return "No performance data available";
        }

        double avgProcessingTime = (double) totalProcessingTime.get() / totalPlayers / 1_000_000.0;
        return String.format("Players: %d, Avg Time: %.3fms, Cache Size: %d",
            totalPlayers, avgProcessingTime, attachmentCache.size());
    }

    // === NETWORK DELEGATION === //

    /**
     * Determines sync priority based on attachment type and change patterns.
     * Critical changes (health, combat) get higher priority than informational changes (statistics).
     */
    private static NetworkSyncManager.SyncPriority determineSyncPriority(IDirtyTrackable attachment, String attachmentName) {
        // Critical priority for health/combat-related changes
        if (attachment instanceof PlayerResourceAttachment) {
            PlayerResourceAttachment resources = (PlayerResourceAttachment) attachment;
            // High priority for health changes, normal for resource changes
            return resources.getCurrentHealth() <= resources.getMaxHealth() * 0.2 ?
                NetworkSyncManager.SyncPriority.CRITICAL : NetworkSyncManager.SyncPriority.HIGH;
        }

        if (attachment instanceof PlayerCombatStatsAttachment) {
            return NetworkSyncManager.SyncPriority.HIGH; // Combat stats need fast sync
        }

        // Breakthrough events are important but not critical
        if (attachment instanceof PlayerBreakthroughAttachment) {
            return NetworkSyncManager.SyncPriority.HIGH;
        }

        // Cultivation progress is normal priority
        if (attachment instanceof PlayerCultivationAttachment) {
            PlayerCultivationAttachment cultivation = (PlayerCultivationAttachment) attachment;
            // Use change counter to determine if this is a burst of changes
            return cultivation.getChangeCounter() > 5 ?
                NetworkSyncManager.SyncPriority.HIGH : NetworkSyncManager.SyncPriority.NORMAL;
        }

        // Default to normal priority
        return NetworkSyncManager.SyncPriority.NORMAL;
    }

    /**
     * Invalidate cache for a specific player (used by ICModDimensionChangeEvent)
     */
    public static void invalidatePlayerCache(UUID playerId) {
        AttachmentCache cache = attachmentCache.get(playerId);
        if (cache != null) {
            cache.invalidate();
            ImmortalCultivation.LOGGER.debug("Invalidated cache for player: {}", playerId);
        }
    }
}