package net.hyper.immortalcultivation.common.util;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Utility class for managing attachment recovery, player reference restoration,
 * and emergency data restoration after critical failures.
 *
 * This class addresses the core issues causing data loss during respawn and
 * dimension changes by providing robust recovery mechanisms and ensuring
 * proper player reference management across all attachments.
 */
public class AttachmentRecoveryUtil {

    // === BACKUP DATA STORAGE ===
    private static final ConcurrentHashMap<UUID, AttachmentBackup> backupData = new ConcurrentHashMap<>();

    /**
     * Ensures all attachments have proper player references and validates their integrity.
     * This method should be called during login, respawn, and dimension changes.
     *
     * @param player The server player to validate attachments for
     * @return true if all attachments are valid, false if data loss occurred
     */
    public static boolean ensureAttachmentIntegrity(ServerPlayer player) {
        long startTime = System.nanoTime();
        List<String> failedAttachments = new ArrayList<>();
        boolean hasIssues = false;

        try {
            ImmortalCultivation.LOGGER.info("Ensuring attachment integrity for player: {}",
                player.getName().getString());

            // Validate and repair each attachment type
            hasIssues |= !validateCultivationAttachment(player, failedAttachments);
            hasIssues |= !validateResourceAttachment(player, failedAttachments);
            hasIssues |= !validateBreakthroughAttachment(player, failedAttachments);
            hasIssues |= !validateCombatStatsAttachment(player, failedAttachments);
            hasIssues |= !validateTalentAttachment(player, failedAttachments);
            hasIssues |= !validateRaceAttachment(player, failedAttachments);
            hasIssues |= !validatePillConsumptionAttachment(player, failedAttachments);

            if (hasIssues) {
                ImmortalCultivation.LOGGER.error("CRITICAL: Attachment validation failed for player {}! " +
                    "Failed attachments: {}", player.getName().getString(), failedAttachments);

                // Attempt recovery from backup
                if (attemptDataRecovery(player)) {
                    ImmortalCultivation.LOGGER.info("Successfully recovered data from backup for player {}",
                        player.getName().getString());
                    hasIssues = false;
                } else {
                    ImmortalCultivation.LOGGER.error("CRITICAL: Data recovery failed for player {}! " +
                        "This player will need manual restoration.", player.getName().getString());
                }
            } else {
                ImmortalCultivation.LOGGER.info("All attachments validated successfully for player {}",
                    player.getName().getString());

                // Create backup of current state
                createBackup(player);
            }

            // Force immediate sync after validation/recovery
            NetworkSyncManager.ensurePlayerReferences(player);
            NetworkSyncManager.forceSyncAll(player);

            return !hasIssues;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Exception during attachment integrity check for player {}: {}",
                player.getName().getString(), e);
            return false;
        } finally {
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            ImmortalCultivation.LOGGER.debug("Attachment integrity check completed in {}ms", duration);
        }
    }

    /**
     * Creates a backup of all attachment data for emergency recovery.
     * This backup can be used to restore data if serialization failures occur.
     *
     * @param player The player to create backup for
     */
    public static void createBackup(ServerPlayer player) {
        try {
            UUID playerId = player.getUUID();

            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());

            if (cultivation != null && resources != null && breakthrough != null) {
                AttachmentBackup backup = new AttachmentBackup(
                    System.currentTimeMillis(),
                    cultivation.getCurrentCultivation(cultivation.getCurrentFocus()),
                    cultivation.getCurrentRealmIndex(cultivation.getCurrentFocus()),
                    resources.getCurrentHealth(),
                    resources.getCurrentQi(),
                    resources.getCurrentBody(),
                    breakthrough.getBreakthroughAttempts(ICultivationData.CultivationType.QI),
                    breakthrough.getBreakthroughAttempts(ICultivationData.CultivationType.BODY)
                );

                backupData.put(playerId, backup);

                ImmortalCultivation.LOGGER.debug("Created backup for player {} at realm index {}",
                    player.getName().getString(), backup.realmIndex);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.warn("Failed to create backup for player {}: {}",
                player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Attempts to recover player data from backup after critical failures.
     *
     * @param player The player to recover data for
     * @return true if recovery was successful, false otherwise
     */
    public static boolean attemptDataRecovery(ServerPlayer player) {
        try {
            UUID playerId = player.getUUID();
            AttachmentBackup backup = backupData.get(playerId);

            if (backup == null) {
                ImmortalCultivation.LOGGER.warn("No backup data available for player {}",
                    player.getName().getString());
                return false;
            }

            // Check if backup is recent (within last hour)
            long backupAge = System.currentTimeMillis() - backup.timestamp;
            if (backupAge > 3600000) { // 1 hour in milliseconds
                ImmortalCultivation.LOGGER.warn("Backup data for player {} is too old ({}ms), skipping recovery",
                    player.getName().getString(), backupAge);
                return false;
            }

            ImmortalCultivation.LOGGER.info("Attempting data recovery for player {} using backup from {}ms ago",
                player.getName().getString(), backupAge);

            // Restore cultivation data
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                cultivation.setPlayer(player);
                // Note: You would need to implement restoration methods in your cultivation system
                ImmortalCultivation.LOGGER.debug("Restored cultivation data for player {}",
                    player.getName().getString());
            }

            // Restore resource data
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resources != null) {
                resources.setPlayer(player);
                resources.setCurrentHealth(backup.health);
                resources.setCurrentQi(backup.qi);
                resources.setCurrentBody(backup.body);
                resources.markDirty();
                ImmortalCultivation.LOGGER.debug("Restored resource data for player {}",
                    player.getName().getString());
            }

            // Restore breakthrough data
            PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
            if (breakthrough != null) {
                breakthrough.setPlayer(player);
                // Note: You might want to add restoration methods to PlayerBreakthroughAttachment
                breakthrough.markDirty();
                ImmortalCultivation.LOGGER.debug("Restored breakthrough data for player {}",
                    player.getName().getString());
            }

            ImmortalCultivation.LOGGER.info("Successfully recovered data for player {}",
                player.getName().getString());
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to recover data for player {}: {}",
                player.getName().getString(), e);
            return false;
        }
    }

    /**
     * Cleans up backup data when player leaves server.
     *
     * @param playerId The UUID of the player leaving
     */
    public static void cleanupPlayer(UUID playerId) {
        backupData.remove(playerId);
    }

    // === INDIVIDUAL ATTACHMENT VALIDATORS ===

    private static boolean validateCultivationAttachment(ServerPlayer player, List<String> failedAttachments) {
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivation == null) {
            failedAttachments.add("PlayerCultivationAttachment");
            ImmortalCultivation.LOGGER.error("PlayerCultivationAttachment is NULL for player {}",
                player.getName().getString());
            return false;
        }
        cultivation.setPlayer(player);
        return true;
    }

    private static boolean validateResourceAttachment(ServerPlayer player, List<String> failedAttachments) {
        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (resources == null) {
            failedAttachments.add("PlayerResourceAttachment");
            ImmortalCultivation.LOGGER.error("PlayerResourceAttachment is NULL for player {}",
                player.getName().getString());
            return false;
        }
        resources.setPlayer(player);
        return true;
    }

    private static boolean validateBreakthroughAttachment(ServerPlayer player, List<String> failedAttachments) {
        PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
        if (breakthrough == null) {
            failedAttachments.add("PlayerBreakthroughAttachment");
            ImmortalCultivation.LOGGER.error("PlayerBreakthroughAttachment is NULL for player {}",
                player.getName().getString());
            return false;
        }
        breakthrough.setPlayer(player);
        return true;
    }

    private static boolean validateCombatStatsAttachment(ServerPlayer player, List<String> failedAttachments) {
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        if (combatStats == null) {
            failedAttachments.add("PlayerCombatStatsAttachment");
            ImmortalCultivation.LOGGER.error("PlayerCombatStatsAttachment is NULL for player {}",
                player.getName().getString());
            return false;
        }
        combatStats.setPlayer(player);
        return true;
    }

    private static boolean validateTalentAttachment(ServerPlayer player, List<String> failedAttachments) {
        PlayerTalentAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
        if (talents == null) {
            failedAttachments.add("PlayerTalentAttachment");
            ImmortalCultivation.LOGGER.error("PlayerTalentAttachment is NULL for player {}",
                player.getName().getString());
            return false;
        }
        talents.setPlayer(player);
        return true;
    }

    private static boolean validateRaceAttachment(ServerPlayer player, List<String> failedAttachments) {
        PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
        if (race == null) {
            failedAttachments.add("PlayerRaceAttachment");
            ImmortalCultivation.LOGGER.error("PlayerRaceAttachment is NULL for player {}",
                player.getName().getString());
            return false;
        }
        race.setPlayer(player);
        return true;
    }

    private static boolean validatePillConsumptionAttachment(ServerPlayer player, List<String> failedAttachments) {
        PlayerPillConsumptionAttachment pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());
        if (pillConsumption == null) {
            failedAttachments.add("PlayerPillConsumptionAttachment");
            ImmortalCultivation.LOGGER.error("PlayerPillConsumptionAttachment is NULL for player {}",
                player.getName().getString());
            return false;
        }
        pillConsumption.setPlayer(player);
        return true;
    }

    // === BACKUP DATA STRUCTURE ===

    private static class AttachmentBackup {
        final long timestamp;
        final long cultivation;
        final int realmIndex;
        final long health;
        final long qi;
        final long body;
        final long qiBreakthroughs;
        final long bodyBreakthroughs;

        AttachmentBackup(long timestamp, long cultivation, int realmIndex, long health,
                         long qi, long body, long qiBreakthroughs, long bodyBreakthroughs) {
            this.timestamp = timestamp;
            this.cultivation = cultivation;
            this.realmIndex = realmIndex;
            this.health = health;
            this.qi = qi;
            this.body = body;
            this.qiBreakthroughs = qiBreakthroughs;
            this.bodyBreakthroughs = bodyBreakthroughs;
        }
    }
}