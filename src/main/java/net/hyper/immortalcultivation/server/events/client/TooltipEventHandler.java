package net.hyper.immortalcultivation.server.events.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.server.events.utils.TooltipEventUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Tooltip Event Handler for the Immortal Cultivation Mod
 *
 * Handles tooltip enhancement events following SOLID principles and DRY methodology.
 * Adds cultivation-specific information to item tooltips, including stat bonuses,
 * enhancement levels, and player-specific effectiveness calculations.
 *
 * Key Features:
 * - Enhanced tooltips for cultivation equipment and items
 * - Player-specific effectiveness calculations for pills and spirit stones
 * - Equipment bonus display with aspect recognition
 * - Cultivation realm requirements and compatibility information
 * - Performance optimized with caching for frequently accessed tooltip data
 *
 * Architecture:
 * - Handler manages event subscriptions and validation
 * - TooltipEventUtils handles business logic and tooltip generation
 * - Client-side only using @EventBusSubscriber auto-registration
 * - Thread-safe caching for tooltip performance optimization
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class TooltipEventHandler {

    // === CONSTANTS === //

    private static final String HANDLER_NAME = "TooltipEventHandler";
    private static final long CACHE_CLEANUP_INTERVAL = 30000L; // 30 seconds
    private static final int MAX_CACHED_TOOLTIPS = 100;

    // === CACHING DATA === //

    /**
     * Cache for tooltip data to improve performance for frequently viewed items.
     * Key: Player UUID + Item Registry Name, Value: Cached tooltip timestamp
     */
    private static final Map<String, Long> tooltipCache = new ConcurrentHashMap<>();

    /**
     * Tracks last cache cleanup time to prevent excessive cleanup operations
     */
    private static long lastCacheCleanup = 0L;

    // === EVENT HANDLERS === //

    /**
     * Handles item tooltip events to add cultivation-specific information.
     * Processes tooltips at low priority to allow other mods to add their content first.
     *
     * @param event Item tooltip event containing stack and tooltip list
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing tooltip for item: {}",
                HANDLER_NAME, event.getItemStack().getItem().getClass().getSimpleName());
        }

        try {
            // Validate event components
            if (!validateTooltipEvent(event)) {
                return;
            }

            Player player = event.getEntity();
            ItemStack stack = event.getItemStack();

            // Check cache to avoid redundant processing
            String cacheKey = generateCacheKey(player, stack);
            if (isTooltipCached(cacheKey)) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Using cached tooltip for: {}",
                        HANDLER_NAME, stack.getItem().getClass().getSimpleName());
                }
                return;
            }

            // Get player attachments
            PlayerStatsAttachment statsAttachment = getPlayerStatsAttachment(player);
            PlayerCultivationAttachment cultivationAttachment = getPlayerCultivationAttachment(player);

            // Process tooltip through utility class
            boolean tooltipModified = TooltipEventUtils.enhanceItemTooltip(
                stack,
                event.getToolTip(),
                player,
                statsAttachment,
                cultivationAttachment
            );

            // Cache the result if tooltip was modified
            if (tooltipModified) {
                updateTooltipCache(cacheKey);
            }

            // Perform periodic cache cleanup
            performPeriodicCacheCleanup();

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error processing tooltip for item {}: {}",
                HANDLER_NAME, event.getItemStack().getItem().getClass().getSimpleName(), e.getMessage(), e);
            // Don't prevent other tooltip processing on error
        }
    }

    // === VALIDATION METHODS === //

    /**
     * Validates the tooltip event and its components.
     *
     * @param event The tooltip event to validate
     * @return True if the event is valid for processing
     */
    private static boolean validateTooltipEvent(ItemTooltipEvent event) {
        // Check if event has required components
        if (event == null || event.getItemStack().isEmpty() || event.getToolTip() == null) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Invalid tooltip event - missing components", HANDLER_NAME);
            }
            return false;
        }

        // Player can be null in some contexts (creative menu, etc.)
        // This is acceptable, we'll handle null player gracefully
        return true;
    }

    /**
     * Safely retrieves the PlayerStatsAttachment from a player.
     *
     * @param player The player to get attachment from (can be null)
     * @return The stats attachment or null if not available
     */
    @Nullable
    private static PlayerStatsAttachment getPlayerStatsAttachment(@Nullable Player player) {
        if (player == null) {
            return null;
        }

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

    /**
     * Safely retrieves the PlayerCultivationAttachment from a player.
     *
     * @param player The player to get attachment from (can be null)
     * @return The cultivation attachment or null if not available
     */
    @Nullable
    private static PlayerCultivationAttachment getPlayerCultivationAttachment(@Nullable Player player) {
        if (player == null) {
            return null;
        }

        try {
            return player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
        } catch (Exception e) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Failed to get cultivation attachment for player {}: {}",
                    HANDLER_NAME, player.getName().getString(), e.getMessage());
            }
            return null;
        }
    }

    // === CACHING METHODS === //

    /**
     * Generates a cache key for tooltip caching based on player and item.
     *
     * @param player The player viewing the tooltip (can be null)
     * @param stack The item stack
     * @return A unique cache key string
     */
    private static String generateCacheKey(@Nullable Player player, ItemStack stack) {
        String playerKey = player != null ? player.getUUID().toString() : "null";
        String itemKey = stack.getItem().builtInRegistryHolder().key().location().toString();
        return playerKey + ":" + itemKey;
    }

    /**
     * Checks if a tooltip is already cached and still valid.
     *
     * @param cacheKey The cache key to check
     * @return True if the tooltip is cached and valid
     */
    private static boolean isTooltipCached(String cacheKey) {
        Long cachedTime = tooltipCache.get(cacheKey);
        if (cachedTime == null) {
            return false;
        }

        // Check if cache entry is still valid (within cache interval)
        long currentTime = System.currentTimeMillis();
        return (currentTime - cachedTime) < CACHE_CLEANUP_INTERVAL;
    }

    /**
     * Updates the tooltip cache with a new entry.
     *
     * @param cacheKey The cache key to update
     */
    private static void updateTooltipCache(String cacheKey) {
        // Prevent cache from growing too large
        if (tooltipCache.size() >= MAX_CACHED_TOOLTIPS) {
            // Remove oldest entries
            long currentTime = System.currentTimeMillis();
            tooltipCache.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > CACHE_CLEANUP_INTERVAL);
        }

        tooltipCache.put(cacheKey, System.currentTimeMillis());
    }

    /**
     * Performs periodic cleanup of expired cache entries to prevent memory leaks.
     * Only runs cleanup at specified intervals to avoid performance impact.
     */
    private static void performPeriodicCacheCleanup() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastCacheCleanup >= CACHE_CLEANUP_INTERVAL) {
            int sizeBefore = tooltipCache.size();

            // Remove expired entries
            tooltipCache.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > CACHE_CLEANUP_INTERVAL);

            lastCacheCleanup = currentTime;

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                int sizeAfter = tooltipCache.size();
                ImmortalCultivation.LOGGER.debug("[{}] Cache cleanup: {} -> {} entries",
                    HANDLER_NAME, sizeBefore, sizeAfter);
            }
        }
    }

    // === UTILITY METHODS === //

    /**
     * Clears all cached tooltip data.
     * Used for debugging or when major changes occur that invalidate cached data.
     */
    public static void clearTooltipCache() {
        int clearedEntries = tooltipCache.size();
        tooltipCache.clear();
        lastCacheCleanup = System.currentTimeMillis();

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Cleared {} cached tooltip entries",
                HANDLER_NAME, clearedEntries);
        }
    }

    /**
     * Gets current cache statistics for debugging purposes.
     *
     * @return A formatted string with cache statistics
     */
    public static String getCacheStatistics() {
        long currentTime = System.currentTimeMillis();
        int totalEntries = tooltipCache.size();
        long validEntries = tooltipCache.values().stream()
            .mapToLong(time -> (currentTime - time) < CACHE_CLEANUP_INTERVAL ? 1 : 0)
            .sum();

        return String.format("[%s] Cache: %d total, %d valid, last cleanup: %dms ago",
            HANDLER_NAME, totalEntries, validEntries, currentTime - lastCacheCleanup);
    }
}