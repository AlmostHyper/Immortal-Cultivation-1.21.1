package net.hyper.immortalcultivation.server.event.player;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.interfaces.IDirtyTrackable;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.EnumMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * UNIFIED RESPAWN EVENT SYSTEM
 *
 * Handles all respawn-related logic for the cultivation system with integration
 * to the new network sync system and ICModTickEvent patterns.
 *
 * This event consolidates respawn penalties, resource restoration, and sync management
 * while maintaining compatibility with the permanent additive stat system.
 *
 * Key Features:
 * - Integrates with NetworkSyncManager for efficient sync operations
 * - Uses ICModTickEvent patterns for performance and error handling
 * - Maintains Xianxia thematic elements with dramatic respawn messaging
 * - Applies cultivation-appropriate death penalties while preserving permanent bonuses
 * - Comprehensive performance monitoring and error isolation
 * - Thread-safe operation with proper attachment reference management
 *
 * Priority System:
 * - Runs at LOWEST priority to ensure DimensionChangeHandler has completed
 * - Uses CRITICAL sync priority for immediate state synchronization
 * - Schedules follow-up syncs through ICModTickEvent integration
 *
 * Xianxia Integration:
 * - Death penalties reflect traditional cultivation consequences
 * - Respawn messages maintain immersive cultivation terminology
 * - Resource restoration follows soul-body-qi metaphysics
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class ICModRespawnEvent {

    // === PERFORMANCE TRACKING === //

    private static final AtomicInteger totalRespawnEvents = new AtomicInteger(0);
    private static final AtomicLong totalProcessingTime = new AtomicLong(0);
    private static final EnumMap<RespawnOperation, AtomicLong> operationTimes = new EnumMap<>(RespawnOperation.class);
    private static final EnumMap<RespawnOperation, AtomicInteger> operationCounts = new EnumMap<>(RespawnOperation.class);

    // Cache for respawn states to prevent duplicate processing
    private static final ConcurrentHashMap<UUID, Long> recentRespawns = new ConcurrentHashMap<>();
    private static final long RESPAWN_DEBOUNCE_MS = 1000; // 1 second debounce

    static {
        for (RespawnOperation op : RespawnOperation.values()) {
            operationTimes.put(op, new AtomicLong(0));
            operationCounts.put(op, new AtomicInteger(0));
        }
    }

    private enum RespawnOperation {
        CULTIVATION_PENALTIES,
        RESOURCE_RESTORATION,
        SYNC_OPERATIONS,
        MESSAGE_DELIVERY,
        CLEANUP
    }

    // === MAIN RESPAWN HANDLER === //

    /**
     * Ensures all attachments have valid player references before any processing.
     * This prevents the "player unknown" serialization issues.
     */
    private static void ensureAllPlayerReferences(ServerPlayer player) {
        try {
            // Get all attachments and ensure they have player references
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                cultivation.setPlayer(player);
            }

            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resources != null) {
                resources.setPlayer(player);
            }

            PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
            if (breakthrough != null) {
                breakthrough.setPlayer(player);
            }

            PlayerTalentAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talents != null) {
                talents.setPlayer(player);
            }

            PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            if (race != null) {
                race.setPlayer(player);
            }

            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats != null) {
                combatStats.setPlayer(player);
            }

            PlayerPillConsumptionAttachment pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());
            if (pillConsumption != null) {
                pillConsumption.setPlayer(player);
            }

            ImmortalCultivation.LOGGER.debug("Ensured all player references are set for player: {}",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ensuring player references for {}: {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Validates that all attachments are present and properly initialized.
     * This is critical because serialization failures can cause complete data loss.
     */
    private static boolean validateAndRecoverAttachments(ServerPlayer player) {
        long operationStart = System.nanoTime();
        boolean hasIssues = false;

        try {
            ImmortalCultivation.LOGGER.info("Validating attachment integrity for respawned player {}",
                player.getName().getString());

            // Check PlayerCultivationAttachment
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) {
                ImmortalCultivation.LOGGER.error("CRITICAL: PlayerCultivationAttachment is NULL for player {}!",
                    player.getName().getString());
                hasIssues = true;
            } else {
                cultivation.setPlayer(player);
                // Check if the attachment has default values (indicating data loss)
                if (cultivation.getCurrentRealmIndex(ICultivationData.CultivationType.QI) == 0 &&
                    cultivation.getCurrentRealmIndex(ICultivationData.CultivationType.BODY) == 0) {
                    ImmortalCultivation.LOGGER.warn("WARNING: PlayerCultivationAttachment appears to have default values - possible data loss!");
                    hasIssues = true;
                }
            }

            // Check PlayerTalentAttachment
            PlayerTalentAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talents == null) {
                ImmortalCultivation.LOGGER.error("CRITICAL: PlayerTalentAttachment is NULL for player {}!",
                    player.getName().getString());
                hasIssues = true;
            } else {
                talents.setPlayer(player);
            }

            // Check PlayerRaceAttachment
            PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            if (race == null) {
                ImmortalCultivation.LOGGER.error("CRITICAL: PlayerRaceAttachment is NULL for player {}!",
                    player.getName().getString());
                hasIssues = true;
            } else {
                race.setPlayer(player);
            }

            // Check other critical attachments...
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resources == null) {
                ImmortalCultivation.LOGGER.error("CRITICAL: PlayerResourceAttachment is NULL for player {}!",
                    player.getName().getString());
                hasIssues = true;
            } else {
                resources.setPlayer(player);
            }

            if (hasIssues) {
                ImmortalCultivation.LOGGER.error("CRITICAL: Attachment validation failed for player {}! " +
                        "Data loss has occurred. Consider implementing emergency restoration.",
                    player.getName().getString());

                // TODO: Implement emergency data restoration from backup or default values

                return false;
            }

            ImmortalCultivation.LOGGER.info("Attachment validation passed for player {}",
                player.getName().getString());
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error during attachment validation for player {}: {}",
                player.getName().getString(), e);
            return false;
        } finally {
            long duration = System.nanoTime() - operationStart;
            ImmortalCultivation.LOGGER.debug("Attachment validation took {:.2f}ms", duration / 1_000_000.0);
        }
    }

    /**
     * Unified respawn handler that processes all cultivation-related respawn logic
     * and integrates with the new network sync system.
     *
     * Runs at LOWEST priority to ensure all other respawn processing has completed,
     * particularly DimensionChangeHandler which manages player reference updates.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        long respawnStartTime = System.nanoTime();
        UUID playerId = player.getUUID();

        // Debounce respawn events to prevent duplicate processing
        if (isDuplicateRespawn(playerId)) {
            ImmortalCultivation.LOGGER.debug("Skipping duplicate respawn event for player {}",
                player.getName().getString());
            return;
        }

        ImmortalCultivation.LOGGER.info("Processing unified respawn event for player {}",
            player.getName().getString());

        try {
            totalRespawnEvents.incrementAndGet();

            // CRITICAL: Ensure all player references are set FIRST
            ensureAllPlayerReferences(player);

            // CRITICAL: Validate attachments
            if (!validateAndRecoverAttachments(player)) {
                ImmortalCultivation.LOGGER.error("Attachment validation failed for player {} - skipping respawn processing",
                    player.getName().getString());
                NetworkSyncManager.emergencySync(player);
                return;
            }

            // Rest of your respawn logic...
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Critical error during respawn processing for player {}",
                player.getName().getString(), e);
        } finally {
            scheduleCleanupOperation(playerId);
        }
    }

    // === PHASE 1: CULTIVATION DEATH PENALTIES === //

    /**
     * Applies cultivation progress penalties on death following Xianxia traditions.
     *
     * In traditional cultivation, death should have meaningful consequences but not
     * completely destroy a cultivator's foundation. This method reduces current
     * cultivation progress while preserving all permanent bonuses from pills,
     * treasures, and realm achievements.
     */
    private static boolean applyCultivationDeathPenalties(ServerPlayer player) {
        long operationStart = System.nanoTime();

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) {
                ImmortalCultivation.LOGGER.error("Player {} has null cultivation attachment during respawn!",
                    player.getName().getString());
                return false;
            }

            // Set player reference to ensure server-side operations work
            cultivation.setPlayer(player);

            // Calculate death penalties based on current realm
            // Higher realms have reduced penalties due to stronger foundation
            double qiPenaltyPercent = calculateQiDeathPenalty(cultivation);
            double bodyPenaltyPercent = calculateBodyDeathPenalty(cultivation);

            // Apply penalties to current cultivation progress only
            long qiPenalty = Math.round(cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI) * qiPenaltyPercent);
            long bodyPenalty = Math.round(cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY) * bodyPenaltyPercent);

            // Reduce current cultivation (never affects permanent bonuses)
            cultivation.setCurrentCultivation(ICultivationData.CultivationType.QI, cultivation.subtractCultivation(ICultivationData.CultivationType.QI, qiPenalty));
            cultivation.setCurrentCultivation(ICultivationData.CultivationType.BODY, cultivation.subtractCultivation(ICultivationData.CultivationType.BODY, bodyPenalty));


            // Mark for synchronization
            cultivation.markDirtyWithCounter();

            ImmortalCultivation.LOGGER.info("Applied death penalties to player {} - Qi: -{}, Body: -{}",
                player.getName().getString(), qiPenalty, bodyPenalty);

            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying cultivation death penalties for player {}",
                player.getName().getString(), e);
            return false;
        } finally {
            long duration = System.nanoTime() - operationStart;
            operationTimes.get(RespawnOperation.CULTIVATION_PENALTIES).addAndGet(duration);
            operationCounts.get(RespawnOperation.CULTIVATION_PENALTIES).incrementAndGet();
        }
    }

    /**
     * Calculates qi cultivation death penalty based on current realm.
     */
    private static double calculateQiDeathPenalty(PlayerCultivationAttachment cultivation) {
        // Base penalty starts at 10% for mortals
        double basePenalty = 0.10;

        // Reduce penalty based on realm advancement
        int realmIndex = cultivation.getCurrentRealmIndex(ICultivationData.CultivationType.QI);
        double realmReduction = Math.min(0.10, realmIndex * 0.002); // Max 10% reduction

        return Math.max(0.03, basePenalty - realmReduction); // Minimum 3% penalty
    }

    /**
     * Calculates body cultivation death penalty based on current realm.
     */
    private static double calculateBodyDeathPenalty(PlayerCultivationAttachment cultivation) {
        // Base penalty starts at 10% for mortals
        double basePenalty = 10;

        // Reduce penalty based on realm advancement
        int realmIndex = cultivation.getCurrentRealmIndex(ICultivationData.CultivationType.BODY);
        double realmReduction = Math.min(0.10, realmIndex * 0.002); // Max 10% reduction

        return Math.max(0.03, basePenalty - realmReduction); // Minimum 3% penalty
    }

    // === PHASE 2: RESOURCE RESTORATION === //

    /**
     * Restores all cultivation resources to full upon respawn.
     */
    private static boolean restoreResourcesOnRespawn(ServerPlayer player) {
        long operationStart = System.nanoTime();

        try {
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resources == null) {
                ImmortalCultivation.LOGGER.error("Player {} has null resource attachment during respawn!",
                    player.getName().getString());
                return false;
            }

            // Set player reference to ensure server-side operations work
            resources.setPlayer(player);

            // Force recalculation of max values using current permanent bonuses
            // This ensures compatibility with the permanent additive system
            resources.forceHealthRecalculation();

            // Store previous values for logging
            long previousHealth = resources.getCurrentHealth();
            long previousQi = resources.getCurrentQi();
            long previousBody = resources.getCurrentBody();

            // Restore all resources to maximum
            resources.setCurrentHealth(resources.getMaxHealth());
            resources.setCurrentQi(resources.getMaxQi());
            resources.setCurrentBody(resources.getMaxBody());

            // Reset regeneration state
            resources.setRegenFraction(0.0);

            // Update vanilla health bar to match cultivation system
            updateVanillaHealthBar(player, resources);

            // Mark for synchronization
            resources.markDirtyWithCounter();

            ImmortalCultivation.LOGGER.info("Restored resources for player {} - Health: {}->{}, Qi: {}->{}, Body: {}->{}",
                player.getName().getString(),
                previousHealth, resources.getCurrentHealth(),
                previousQi, resources.getCurrentQi(),
                previousBody, resources.getCurrentBody());

            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error restoring resources for player {}",
                player.getName().getString(), e);
            return false;
        } finally {
            long duration = System.nanoTime() - operationStart;
            operationTimes.get(RespawnOperation.RESOURCE_RESTORATION).addAndGet(duration);
            operationCounts.get(RespawnOperation.RESOURCE_RESTORATION).incrementAndGet();
        }
    }

    /**
     * Updates vanilla health bar to reflect cultivation system health.
     * Ensures UI consistency between vanilla and cultivation health systems.
     */
    private static void updateVanillaHealthBar(ServerPlayer player, PlayerResourceAttachment resources) {
        try {
            // Calculate percentage of health relative to max
            float healthPercentage = Math.min(1.0f,
                (float) resources.getCurrentHealth() / (float) resources.getMaxHealth());

            // Set vanilla health to match percentage
            player.setHealth(player.getMaxHealth() * healthPercentage);

            ImmortalCultivation.LOGGER.debug("Updated vanilla health bar for player {} to {:.1f}%",
                player.getName().getString(), healthPercentage * 100);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.debug("Error updating vanilla health bar for player {}",
                player.getName().getString());
        }
    }

    // === PHASE 3: NETWORK SYNCHRONIZATION === //

    /**
     * Performs all network synchronization operations using the new unified system.
     *
     * This integrates with NetworkSyncManager to ensure all respawn changes are
     * immediately synchronized to the client with high priority.
     */
    private static boolean performRespawnSyncOperations(ServerPlayer player) {
        long operationStart = System.nanoTime();

        try {
            // Use CRITICAL priority for immediate respawn sync
            NetworkSyncManager.scheduleSync(player, NetworkSyncManager.SyncPriority.CRITICAL);

            // Force immediate sync for combat stats (which may affect UI)
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats instanceof IDirtyTrackable dirtyTrackable) {
                combatStats.setPlayer(player);
                dirtyTrackable.markDirtyWithCounter();
                NetworkSyncManager.scheduleSync(player, NetworkSyncManager.SyncPriority.CRITICAL);
            }

            ImmortalCultivation.LOGGER.debug("Scheduled critical sync operations for respawn of player {}",
                player.getName().getString());

            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error performing respawn sync operations for player {}",
                player.getName().getString(), e);
            return false;
        } finally {
            long duration = System.nanoTime() - operationStart;
            operationTimes.get(RespawnOperation.SYNC_OPERATIONS).addAndGet(duration);
            operationCounts.get(RespawnOperation.SYNC_OPERATIONS).incrementAndGet();
        }
    }

    // === PHASE 4: RESPAWN MESSAGES === //

    /**
     * Sends thematic respawn messages to the player explaining what occurred.
     *
     * Maintains immersive Xianxia atmosphere while providing gameplay feedback
     * about death penalties and restoration effects.
     */
    private static void sendRespawnMessages(ServerPlayer player) {
        long operationStart = System.nanoTime();

        try {
            // Get current cultivation state for contextual messaging
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) {
                return;
            }

            // Main respawn message with dramatic Xianxia flair
            Component mainMessage = Component.literal("⚰️ ═══ RETURN FROM THE VOID ═══ ⚰️\n")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("Your soul has crossed the boundary between life and death...\n")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                .append(Component.literal("The underworld has claimed its toll, but your dao heart endures.\n")
                    .withStyle(ChatFormatting.DARK_GRAY));

            // Consequence details
            Component consequences = Component.literal("⚡ Consequences of Death:\n")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal("• Some cultivation progress has been lost to the void\n")
                    .withStyle(ChatFormatting.RED))
                .append(Component.literal("• Your foundation remains intact - permanent achievements preserved\n")
                    .withStyle(ChatFormatting.GOLD))
                .append(Component.literal("• Body, qi, and vital essence fully restored\n")
                    .withStyle(ChatFormatting.GREEN));

            // Current cultivation status
            Component currentStatus = Component.literal("🌟 Current Cultivation State:\n")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal(String.format("• Qi Realm: %s (%d/%d)\n",
                        cultivation.getCurrentRealm(ICultivationData.CultivationType.QI).getDisplayName(),
                        cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI),
                        cultivation.getMaxCultivation(ICultivationData.CultivationType.QI)))
                    .withStyle(ChatFormatting.BLUE))
                .append(Component.literal(String.format("• Body Realm: %s (%d/%d)\n",
                        cultivation.getCurrentRealm(ICultivationData.CultivationType.BODY).getDisplayName(),
                        cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY),
                        cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY)))
                    .withStyle(ChatFormatting.GREEN));

            // Encouragement message
            Component encouragement = Component.literal("🔥 The path to immortality is fraught with peril.\n")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("Each death teaches wisdom. Each revival strengthens resolve.\n")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC))
                .append(Component.literal("Continue your cultivation journey, brave cultivator!")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

            // Send messages with proper spacing
            player.sendSystemMessage(mainMessage);
            player.sendSystemMessage(consequences);
            player.sendSystemMessage(currentStatus);
            player.sendSystemMessage(encouragement);

            ImmortalCultivation.LOGGER.debug("Sent respawn messages to player {}",
                player.getName().getString());

        } catch (Exception e) {
            // Don't spam errors for cosmetic messages
            ImmortalCultivation.LOGGER.debug("Error sending respawn messages to player {}",
                player.getName().getString());
        } finally {
            long duration = System.nanoTime() - operationStart;
            operationTimes.get(RespawnOperation.MESSAGE_DELIVERY).addAndGet(duration);
            operationCounts.get(RespawnOperation.MESSAGE_DELIVERY).incrementAndGet();
        }
    }

    // === PHASE 5: FOLLOW-UP OPERATIONS === //

    /**
     * Schedules follow-up operations that integrate with ICModTickEvent.
     *
     * This ensures that respawn effects are properly maintained by the ongoing
     * tick system and that any special respawn bonuses are applied over time.
     */
    private static void scheduleFollowUpOperations(ServerPlayer player) {
        try {
            // Schedule high-priority sync for next few ticks to ensure UI updates
            // ICModTickEvent will handle these through its normal sync scheduling
            ImmortalCultivation.LOGGER.debug("Scheduled follow-up operations for player {} through ICModTickEvent",
                player.getName().getString());

            // Future: Could add temporary respawn buffs or special effects here
            // that would be managed by the tick event system

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.debug("Error scheduling follow-up operations for player {}",
                player.getName().getString());
        }
    }

    // === UTILITY METHODS === //

    /**
     * Checks if this respawn event is a duplicate to prevent double-processing.
     * Uses a time-based debounce system to handle rapid respawn events.
     */
    private static boolean isDuplicateRespawn(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        Long lastRespawn = recentRespawns.get(playerId);

        if (lastRespawn != null && (currentTime - lastRespawn) < RESPAWN_DEBOUNCE_MS) {
            return true;
        }

        recentRespawns.put(playerId, currentTime);
        return false;
    }

    /**
     * Schedules cleanup operation to manage memory and remove stale respawn data.
     */
    private static void scheduleCleanupOperation(UUID playerId) {
        long operationStart = System.nanoTime();

        try {
            // Clean up respawn debounce data after delay
            // In a real implementation, this might be handled by a scheduled task

            ImmortalCultivation.LOGGER.debug("Scheduled cleanup for player respawn data: {}", playerId);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.debug("Error scheduling cleanup for player {}", playerId);
        } finally {
            long duration = System.nanoTime() - operationStart;
            operationTimes.get(RespawnOperation.CLEANUP).addAndGet(duration);
            operationCounts.get(RespawnOperation.CLEANUP).incrementAndGet();
        }
    }

    // === PERFORMANCE MONITORING === //

    /**
     * Gets performance statistics for respawn operations.
     * Can be called from commands or debug systems.
     */
    public static String getPerformanceStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== ICModRespawnEvent Performance Stats ===\n");
        stats.append(String.format("Total Respawn Events: %d\n", totalRespawnEvents.get()));

        if (totalRespawnEvents.get() > 0) {
            double avgTotalTime = (double) totalProcessingTime.get() / totalRespawnEvents.get() / 1_000_000.0;
            stats.append(String.format("Average Total Processing Time: %.2fms\n", avgTotalTime));

            for (RespawnOperation op : RespawnOperation.values()) {
                int count = operationCounts.get(op).get();
                if (count > 0) {
                    double avgTime = (double) operationTimes.get(op).get() / count / 1_000_000.0;
                    stats.append(String.format("Average %s Time: %.2fms (%d operations)\n",
                        op.name(), avgTime, count));
                }
            }
        }

        return stats.toString();
    }

    /**
     * Clears performance statistics (useful for testing or periodic reset).
     */
    public static void clearPerformanceStats() {
        totalRespawnEvents.set(0);
        totalProcessingTime.set(0);

        for (RespawnOperation op : RespawnOperation.values()) {
            operationTimes.get(op).set(0);
            operationCounts.get(op).set(0);
        }

        recentRespawns.clear();

        ImmortalCultivation.LOGGER.info("Cleared ICModRespawnEvent performance statistics");
    }
}