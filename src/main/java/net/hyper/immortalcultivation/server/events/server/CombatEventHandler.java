package net.hyper.immortalcultivation.server.events.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.server.events.utils.CombatCalculationUtils;
import net.hyper.immortalcultivation.server.events.utils.CombatEventUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combat Event Handler for the Immortal Cultivation Mod
 *
 * Handles combat-related events following SOLID principles and DRY methodology.
 * Delegates business logic to utility classes while maintaining clean event handling.
 *
 * Key Features:
 * - Incoming damage processing with custom cultivation damage system
 * - Damage immunity tracking for sophisticated multi-entity combat
 * - Integration with PlayerStatsAttachment for health management
 * - Performance optimized with concurrent data structures
 * - Comprehensive debug logging for combat system monitoring
 *
 * Architecture:
 * - Handler manages event subscriptions and validation
 * - CombatEventUtils handles business logic and calculations
 * - CombatCalculationUtils provides mathematical operations
 * - Maintains thread safety through concurrent collections
 */
public class CombatEventHandler {

    // === CONSTANTS === //

    private static final String HANDLER_NAME = "CombatEventHandler";
    private static final int IMMUNITY_CLEANUP_INTERVAL = 6000; // Cleanup every 5 minutes

    // === TRACKING DATA === //

    /**
     * Tracks immunity per player per damage source for sophisticated multi-enemy combat.
     * Key: Player UUID, Value: Map of damage source keys to immunity expiration times
     */
    private static final Map<UUID, Map<String, Long>> playerImmunityMap = new ConcurrentHashMap<>();

    /**
     * Tracks last known vanilla health values for protection system compatibility.
     * Key: Player UUID, Value: Last recorded vanilla health
     */
    private static final Map<UUID, Float> lastKnownVanillaHealth = new ConcurrentHashMap<>();

    /**
     * Tracks last immunity cleanup time to prevent excessive cleanup operations
     */
    private static long lastCleanupTime = 0L;

    // === EVENT HANDLERS === //

    /**
     * Handles incoming damage events for players in the cultivation system.
     * Intercepts vanilla damage, converts to custom cultivation damage, and applies
     * immunity tracking for multi-entity combat scenarios.
     *
     * @param event The incoming damage event to process
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing damage event for entity: {}",
                HANDLER_NAME, event.getEntity().getClass().getSimpleName());
        }

        // Validate event and extract player
        Player player = validateAndExtractPlayer(event);
        if (player == null) {
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        UUID playerId = player.getUUID();
        DamageSource damageSource = event.getSource();
        float vanillaDamage = event.getAmount();

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing {} damage for player: {}",
                HANDLER_NAME, vanillaDamage, player.getName().getString());
        }

        try {
            // Check immunity using utility class
            String sourceKey = CombatCalculationUtils.generateDamageSourceKey(damageSource);
            if (CombatEventUtils.isPlayerImmune(playerId, sourceKey, playerImmunityMap)) {
                event.setCanceled(true);
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Player {} is immune to damage from source: {}",
                        HANDLER_NAME, player.getName().getString(), sourceKey);
                }
                return;
            }

            // Get player stats attachment
            PlayerStatsAttachment statsAttachment = getPlayerStatsAttachment(serverPlayer);
            if (statsAttachment == null) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] No stats attachment found for player: {}",
                        HANDLER_NAME, player.getName().getString());
                }
                return;
            }

            // Cancel vanilla damage processing
            event.setCanceled(true);

            // Process damage through utility class
            CombatEventUtils.processDamage(
                serverPlayer,
                statsAttachment,
                vanillaDamage,
                damageSource,
                sourceKey,
                playerImmunityMap,
                lastKnownVanillaHealth
            );

            // Perform periodic cleanup
            performPeriodicCleanup();

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error processing damage for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
            // Don't cancel the event on error to maintain vanilla fallback
        }
    }

    // === VALIDATION METHODS === //

    /**
     * Validates the damage event and extracts the player if applicable.
     *
     * @param event The damage event to validate
     * @return The player if valid, null otherwise
     */
    @Nullable
    private static Player validateAndExtractPlayer(LivingIncomingDamageEvent event) {
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

        return player;
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
            return player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
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
     * Performs periodic cleanup of expired immunity entries to prevent memory leaks.
     * Only runs cleanup at specified intervals to avoid performance impact.
     */
    private static void performPeriodicCleanup() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > IMMUNITY_CLEANUP_INTERVAL) {
            CombatEventUtils.cleanupExpiredImmunities(playerImmunityMap);
            lastCleanupTime = currentTime;

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Performed immunity cleanup", HANDLER_NAME);
            }
        }
    }

    // === GENERIC UTILITY METHODS === //

    /**
     * Generic method to safely get attachment data from any player.
     * Provides type safety and error handling for attachment retrieval.
     *
     * @param <T> The attachment type
     * @param player The player to get attachment from
     * @param attachmentSupplier Supplier for the attachment type
     * @param attachmentName Name for logging purposes
     * @return The attachment or null if not available
     */
    @Nullable
    public static <T> T getPlayerAttachment(ServerPlayer player,
                                            java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<T>> attachmentSupplier,
                                            String attachmentName) {
        try {
            return player.getData(attachmentSupplier.get());
        } catch (Exception e) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Failed to get {} attachment for player {}: {}",
                    HANDLER_NAME, attachmentName, player.getName().getString(), e.getMessage());
            }
            return null;
        }
    }

    // === DEBUG AND MONITORING === //

    /**
     * Gets current immunity statistics for debugging purposes.
     *
     * @return Map containing immunity statistics
     */
    public static Map<String, Object> getImmunityStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalPlayersWithImmunity", playerImmunityMap.size());
        stats.put("totalImmunityEntries", playerImmunityMap.values().stream()
            .mapToInt(Map::size)
            .sum());
        stats.put("lastCleanupTime", lastCleanupTime);
        return stats;
    }

    /**
     * Clears all immunity data for a specific player.
     * Useful for debugging or when a player logs out.
     *
     * @param playerId The player UUID to clear immunity for
     */
    public static void clearPlayerImmunity(UUID playerId) {
        if (playerImmunityMap.remove(playerId) != null) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Cleared all immunity for player: {}",
                    HANDLER_NAME, playerId);
            }
        }
    }

    /**
     * Forces cleanup of all expired immunity entries.
     * Useful for debugging or emergency cleanup.
     */
    public static void forceImmunityCleanup() {
        CombatEventUtils.cleanupExpiredImmunities(playerImmunityMap);
        lastCleanupTime = System.currentTimeMillis();

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Forced immunity cleanup completed", HANDLER_NAME);
        }
    }
}