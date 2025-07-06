package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * OPTIMIZED: Handles all cultivation-related ticking for players with performance improvements.
 *
 * Key optimizations:
 * 1. Reduced tick frequency from every tick to every 4 ticks (5 times per second)
 * 2. Staggered operations to prevent synchronization spikes
 * 3. Player-specific tick offsets to distribute load
 * 4. Cached references to avoid repeated getData() calls
 * 5. Early returns for unnecessary operations
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class CultivationTickHandler {

    // Cache attachment references to avoid repeated getData() calls
    private static final Map<UUID, AttachmentCache> attachmentCache = new ConcurrentHashMap<>();

    // Performance constants
    private static final int CULTIVATION_TICK_INTERVAL = 4; // Process every 4 ticks instead of every tick
    private static final int RESOURCE_TICK_INTERVAL = 10;   // Resources every 10 ticks (0.5 seconds)
    private static final int COMBAT_STATS_TICK_INTERVAL = 20; // Combat stats every 20 ticks (1 second)
    private static final int CLEANUP_INTERVAL = 200;        // Clean cache every 10 seconds

    private static int globalTickCounter = 0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        globalTickCounter++;

        // Calculate player-specific tick offset to stagger operations
        int playerOffset = Math.abs(serverPlayer.getUUID().hashCode()) % 20;
        int adjustedTick = (serverPlayer.tickCount + playerOffset) % 100;

        // Get or create cached attachments
        AttachmentCache cache = getOrCreateCache(serverPlayer);

        // Process cultivation every 4 ticks (5 times per second instead of 20)
        if (adjustedTick % CULTIVATION_TICK_INTERVAL == 0) {
            tickCultivationAttachments(serverPlayer, cache);
        }

        // Process resources every 10 ticks (2 times per second instead of 20)
        if (adjustedTick % RESOURCE_TICK_INTERVAL == 0) {
            processResourceRegeneration(serverPlayer, cache);
        }

        // Process combat stats every 20 ticks (1 time per second instead of 20)
        if (adjustedTick % COMBAT_STATS_TICK_INTERVAL == 0) {
            tickCombatStats(serverPlayer, cache);
        }

        // Clean up cache periodically
        if (globalTickCounter % CLEANUP_INTERVAL == 0) {
            cleanupCache();
        }
    }

    /**
     * Cache for attachment references to avoid repeated getData() calls
     */
    private static class AttachmentCache {
        PlayerCultivationAttachment cultivation;
        PlayerResourceAttachment resources;
        PlayerCombatStatsAttachment combatStats;
        long lastUpdateTime;

        AttachmentCache(ServerPlayer player) {
            refresh(player);
        }

        void refresh(ServerPlayer player) {
            this.cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            this.resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            this.combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            this.lastUpdateTime = System.currentTimeMillis();
        }

        boolean isStale() {
            return System.currentTimeMillis() - lastUpdateTime > 30000; // 30 seconds
        }
    }

    private static AttachmentCache getOrCreateCache(ServerPlayer player) {
        UUID playerId = player.getUUID();
        AttachmentCache cache = attachmentCache.get(playerId);

        if (cache == null || cache.isStale()) {
            cache = new AttachmentCache(player);
            attachmentCache.put(playerId, cache);
        }

        return cache;
    }

    /**
     * OPTIMIZED: Tick cultivation attachment with reduced frequency
     */
    private static void tickCultivationAttachments(ServerPlayer player, AttachmentCache cache) {
        try {
            PlayerCultivationAttachment cultivation = cache.cultivation;
            if (cultivation != null) {
                // Only set player reference if not already set (avoid repeated calls)
                if (cultivation.getPlayerSafe() != player) {
                    cultivation.setPlayer(player);
                }
                cultivation.tick();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking cultivation attachments for player {}",
                player.getName().getString(), e);
            // Remove from cache to force refresh on next access
            attachmentCache.remove(player.getUUID());
        }
    }

    /**
     * OPTIMIZED: Process resource regeneration with reduced frequency
     */
    private static void processResourceRegeneration(ServerPlayer player, AttachmentCache cache) {
        try {
            PlayerResourceAttachment resources = cache.resources;
            if (resources != null) {
                // Only set player reference if not already set
                if (resources.getPlayerSafe() != player) {
                    resources.setPlayer(player);
                }

                // Since we're calling this less frequently, we need to adjust the regeneration rates
                // Call tick multiple times to compensate for reduced frequency
                int tickMultiplier = RESOURCE_TICK_INTERVAL;
                for (int i = 0; i < tickMultiplier; i++) {
                    resources.tick();
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing resource regeneration for player {}",
                player.getName().getString(), e);
            attachmentCache.remove(player.getUUID());
        }
    }

    /**
     * OPTIMIZED: Tick combat stats with reduced frequency and staggered sync
     */
    private static void tickCombatStats(ServerPlayer player, AttachmentCache cache) {
        try {
            PlayerCombatStatsAttachment combatStats = cache.combatStats;
            if (combatStats != null) {
                // Only set player reference if not already set
                if (combatStats.getPlayerSafe() != player) {
                    combatStats.setPlayer(player);
                }

                // Call tick multiple times to compensate for reduced frequency
                int tickMultiplier = COMBAT_STATS_TICK_INTERVAL;
                for (int i = 0; i < tickMultiplier; i++) {
                    combatStats.tick();
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking combat stats for player {}",
                player.getName().getString(), e);
            attachmentCache.remove(player.getUUID());
        }
    }

    /**
     * Clean up stale cache entries
     */
    private static void cleanupCache() {
        attachmentCache.entrySet().removeIf(entry -> entry.getValue().isStale());

        if (globalTickCounter % 1000 == 0) { // Every 50 seconds
            ImmortalCultivation.LOGGER.debug("Cleaned up attachment cache. Current size: {}",
                attachmentCache.size());
        }
    }

    /**
     * Clear cache entry for a specific player (call on logout/dimension change)
     */
    public static void clearPlayerCache(UUID playerId) {
        attachmentCache.remove(playerId);
    }

    /**
     * Clear all cache (call on server shutdown)
     */
    public static void clearAllCache() {
        attachmentCache.clear();
    }
}