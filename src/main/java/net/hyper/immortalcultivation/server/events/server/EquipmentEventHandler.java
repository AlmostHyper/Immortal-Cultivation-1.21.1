package net.hyper.immortalcultivation.server.events.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.server.events.utils.EquipmentEventUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Equipment Event Handler for the Immortal Cultivation Mod
 *
 * Handles equipment change events following SOLID principles and DRY methodology.
 * Delegates business logic to utility classes while maintaining clean event handling.
 * Integrates with the sophisticated bonus source system for equipment-based stat modifications.
 *
 * Key Features:
 * - Equipment change processing with aspected gear recognition
 * - Integration with BonusSource.EQUIPMENT for proper bonus tracking
 * - Attribute recalculation for vanilla compatibility
 * - Performance optimized with concurrent data structures
 * - Comprehensive debug logging for equipment system monitoring
 *
 * Architecture:
 * - Handler manages event subscriptions and validation
 * - EquipmentEventUtils handles business logic and calculations
 * - AttributeModifierUtils provides attribute management
 * - Maintains thread safety through concurrent collections
 */
public class EquipmentEventHandler {

    // === CONSTANTS === //

    private static final String HANDLER_NAME = "EquipmentEventHandler";
    private static final int EQUIPMENT_PROCESSING_TIMEOUT_MS = 100; // Max time to process equipment changes

    // === TRACKING DATA === //

    /**
     * Tracks last equipment processing time per player to prevent spam processing.
     * Key: Player UUID, Value: Last processing timestamp
     */
    private static final Map<UUID, Long> lastProcessingTime = new ConcurrentHashMap<>();

    /**
     * Tracks current equipment state per player for efficient change detection.
     * Key: Player UUID, Value: Equipment state snapshot
     */
    private static final Map<UUID, EquipmentSnapshot> equipmentSnapshots = new ConcurrentHashMap<>();

    /**
     * Minimum time between equipment processing operations (in milliseconds)
     */
    private static final long MIN_PROCESSING_INTERVAL = 50L; // 50ms

    // === EVENT HANDLERS === //

    /**
     * Handles equipment change events for players in the cultivation system.
     * Processes equipment bonuses, attribute modifications, and stat recalculations
     * while maintaining compatibility with vanilla attribute systems.
     *
     * @param event The equipment change event to process
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing equipment change for entity: {}",
                HANDLER_NAME, event.getEntity().getClass().getSimpleName());
        }

        // Validate event and extract player
        ServerPlayer player = validateAndExtractPlayer(event);
        if (player == null) {
            return;
        }

        UUID playerId = player.getUUID();
        EquipmentSlot slot = event.getSlot();
        ItemStack oldStack = event.getFrom();
        ItemStack newStack = event.getTo();

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing equipment change for player: {} in slot: {} (old: {}, new: {})",
                HANDLER_NAME, player.getName().getString(), slot.getName(),
                oldStack.getDisplayName().getString(), newStack.getDisplayName().getString());
        }

        try {
            // Check processing throttling to prevent spam
            if (!canProcessEquipmentChange(playerId)) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Throttling equipment processing for player: {}",
                        HANDLER_NAME, player.getName().getString());
                }
                return;
            }

            // Get player stats attachment
            PlayerStatsAttachment statsAttachment = getPlayerStatsAttachment(player);
            if (statsAttachment == null) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] No stats attachment found for player: {}",
                        HANDLER_NAME, player.getName().getString());
                }
                return;
            }

            // Update processing timestamp
            lastProcessingTime.put(playerId, System.currentTimeMillis());

            // Process equipment change through utility class
            EquipmentEventUtils.processEquipmentChange(
                player,
                statsAttachment,
                slot,
                oldStack,
                newStack,
                equipmentSnapshots
            );

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error processing equipment change for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === VALIDATION METHODS === //

    /**
     * Validates the equipment change event and extracts the player if applicable.
     *
     * @param event The equipment change event to validate
     * @return The server player if valid, null otherwise
     */
    @Nullable
    private static ServerPlayer validateAndExtractPlayer(LivingEquipmentChangeEvent event) {
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

    /**
     * Checks if equipment processing can proceed for a player (throttling).
     *
     * @param playerId The player's UUID
     * @return True if processing can proceed, false if throttled
     */
    private static boolean canProcessEquipmentChange(UUID playerId) {
        Long lastTime = lastProcessingTime.get(playerId);
        if (lastTime == null) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        return (currentTime - lastTime) >= MIN_PROCESSING_INTERVAL;
    }

    // === EQUIPMENT SNAPSHOT MANAGEMENT === //

    /**
     * Inner class representing a snapshot of a player's equipment state.
     * Used for efficient change detection and processing optimization.
     */
    public static class EquipmentSnapshot {
        private final Map<EquipmentSlot, ItemStack> equipment;
        private final long timestamp;

        public EquipmentSnapshot(ServerPlayer player) {
            this.equipment = new ConcurrentHashMap<>();
            this.timestamp = System.currentTimeMillis();

            // Capture current equipment state
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                this.equipment.put(slot, player.getItemBySlot(slot).copy());
            }
        }

        /**
         * Gets the equipment item for a specific slot.
         *
         * @param slot The equipment slot
         * @return The item stack (or empty if not present)
         */
        public ItemStack getItem(EquipmentSlot slot) {
            return equipment.getOrDefault(slot, ItemStack.EMPTY);
        }

        /**
         * Gets the timestamp when this snapshot was created.
         *
         * @return The creation timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }

        /**
         * Checks if this snapshot has a specific item in a slot.
         *
         * @param slot The equipment slot to check
         * @param item The item to compare against
         * @return True if the item matches, false otherwise
         */
        public boolean hasItem(EquipmentSlot slot, ItemStack item) {
            ItemStack slotItem = equipment.get(slot);
            if (slotItem == null) {
                return item.isEmpty();
            }
            return ItemStack.isSameItem(slotItem, item);
        }
    }

    // === UTILITY METHODS === //

    /**
     * Creates or updates an equipment snapshot for a player.
     *
     * @param player The server player
     * @return The updated equipment snapshot
     */
    public static EquipmentSnapshot updateEquipmentSnapshot(ServerPlayer player) {
        EquipmentSnapshot snapshot = new EquipmentSnapshot(player);
        equipmentSnapshots.put(player.getUUID(), snapshot);
        return snapshot;
    }

    /**
     * Gets the current equipment snapshot for a player.
     *
     * @param playerId The player's UUID
     * @return The equipment snapshot or null if not available
     */
    @Nullable
    public static EquipmentSnapshot getEquipmentSnapshot(UUID playerId) {
        return equipmentSnapshots.get(playerId);
    }

    /**
     * Removes equipment snapshot data for a player (cleanup).
     *
     * @param playerId The player's UUID
     */
    public static void clearPlayerEquipmentData(UUID playerId) {
        equipmentSnapshots.remove(playerId);
        lastProcessingTime.remove(playerId);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Cleared equipment data for player: {}",
                HANDLER_NAME, playerId);
        }
    }

    /**
     * Forces a complete equipment recalculation for a player.
     * Useful for initialization or when system state becomes inconsistent.
     *
     * @param player The server player
     */
    public static void forceEquipmentRecalculation(ServerPlayer player) {
        try {
            PlayerStatsAttachment statsAttachment = getPlayerStatsAttachment(player);
            if (statsAttachment == null) {
                ImmortalCultivation.LOGGER.warn("[{}] Cannot force recalculation: no stats attachment for player {}",
                    HANDLER_NAME, player.getName().getString());
                return;
            }

            // Clear equipment-based bonuses and recalculate
            EquipmentEventUtils.forceFullEquipmentRecalculation(player, statsAttachment, equipmentSnapshots);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Forced equipment recalculation for player: {}",
                    HANDLER_NAME, player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during forced equipment recalculation for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
        }
    }

    // === DEBUG AND MONITORING === //

    /**
     * Gets current equipment processing statistics for debugging purposes.
     *
     * @return Map containing equipment processing statistics
     */
    public static Map<String, Object> getEquipmentProcessingStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("playersWithSnapshots", equipmentSnapshots.size());
        stats.put("playersWithProcessingHistory", lastProcessingTime.size());
        stats.put("minProcessingInterval", MIN_PROCESSING_INTERVAL);
        return stats;
    }

    /**
     * Performs cleanup of stale equipment data for players who have logged out.
     *
     * @param maxAge Maximum age in milliseconds for data to be considered stale
     */
    public static void cleanupStaleEquipmentData(long maxAge) {
        long currentTime = System.currentTimeMillis();
        AtomicInteger removedSnapshots = new AtomicInteger();
        AtomicInteger removedProcessingTimes = new AtomicInteger();

        // Clean up stale snapshots
        equipmentSnapshots.entrySet().removeIf(entry -> {
            boolean isStale = (currentTime - entry.getValue().getTimestamp()) > maxAge;
            if (isStale) removedSnapshots.getAndIncrement();
            return isStale;
        });

        // Clean up stale processing times
        lastProcessingTime.entrySet().removeIf(entry -> {
            boolean isStale = (currentTime - entry.getValue()) > maxAge;
            if (isStale) removedProcessingTimes.getAndIncrement();
            return isStale;
        });

        if (ImmortalCultivation.LOGGER.isDebugEnabled() && (removedSnapshots.get() > 0 || removedProcessingTimes.get() > 0)) {
            ImmortalCultivation.LOGGER.debug("[{}] Cleanup removed {} stale snapshots and {} processing times",
                HANDLER_NAME, removedSnapshots, removedProcessingTimes);
        }
    }
}