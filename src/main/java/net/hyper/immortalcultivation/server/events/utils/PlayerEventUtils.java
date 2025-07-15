package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.*;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class PlayerEventUtils {

    /**
     * List of all attachment types managed by the initialization system
     */
    private static final List<Supplier<? extends AttachmentType<?>>> MANAGED_ATTACHMENTS = List.of(
        ICModAttachmentTypes.PLAYER_CULTIVATION,
        ICModAttachmentTypes.PLAYER_STATS,
        ICModAttachmentTypes.PLAYER_BREAKTHROUGH,
        ICModAttachmentTypes.PLAYER_TALENTS,
        ICModAttachmentTypes.PLAYER_RACE,
        ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION
    );

    /**
     * Initializes all managed attachments for a player
     *
     * @param player The player to initialize
     */
    private static void initializePlayerAttachments(ServerPlayer player) {
        for (Supplier<? extends AttachmentType<?>> attachmentSupplier : MANAGED_ATTACHMENTS) {
            AttachmentType<?> attachmentType = attachmentSupplier.get();

            if (!player.hasData(attachmentType)) {
                // This will create the attachment with default values
                player.getData(attachmentType);
            }

            // Initialize if the attachment supports it
            Object attachment = player.getExistingData(attachmentType).orElse(null);
            if (attachment instanceof IPlayerInitializable initializable) {
                initializable.onPlayerLogin(player);
            }
        }
    }

    /**
     * Synchronizes all player data to the client
     *
     * @param player The player to sync data for
     */
    private static void synchronizeAllPlayerData(ServerPlayer player) {
        for (Supplier<? extends AttachmentType<?>> attachmentSupplier : MANAGED_ATTACHMENTS) {
            Object attachment = player.getExistingData(attachmentSupplier.get()).orElse(null);
            if (attachment instanceof ISyncable syncable) {
                try {
                    // Ensure player reference is set if the attachment supports it
                    setPlayerReferenceIfSupported(attachment, player);

                    // Create and send sync packet
                    Optional<ISyncPacket> packetOptional = syncable.createPacket(true); // true for full sync
                    if (packetOptional.isPresent()) {
                        ICModPacketRegistry.sendToPlayer(packetOptional.get(), player);
                        syncable.clearDirty();

                        ImmortalCultivation.LOGGER.debug("Synced {} for player {}",
                            attachmentSupplier.get().toString(), player.getName().getString());
                    }
                } catch (Exception e) {
                    ImmortalCultivation.LOGGER.error("Error syncing attachment {} for player {}: {}",
                        attachmentSupplier.get().toString(), player.getName().getString(), e.getMessage());
                }
            }
        }
    }

    /**
     * Applies current realm modifiers for all cultivation types.
     * This method clears existing REALM bonuses and reapplies them to prevent
     * accumulation of bonuses on player login/respawn, while preserving
     * bonuses from other sources like pills.
     *
     * @param player The player to apply modifiers for
     */
    private static void applyCurrentRealmModifiers(ServerPlayer player) {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Silently reapplying current realm modifiers for player: {}",
                player.getName().getString());
        }

        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        PlayerStatsAttachment stats = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());

        if (cultivation == null || stats == null) {
            ImmortalCultivation.LOGGER.warn("Missing attachments for player {}: cultivation={}, stats={}",
                player.getName().getString(), cultivation != null, stats != null);
            return;
        }

        // CRITICAL FIX: Clear only REALM bonuses, preserving pill and other bonuses
        stats.clearBonusesFromSource(BonusSource.REALM);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Cleared REALM bonuses, preserved other sources (pills, equipment, etc.)");
        }

        // Apply CUMULATIVE realm bonuses for each cultivation type IN SILENT MODE
        for (ICultivationData.CultivationType type : ICultivationData.CultivationType.values()) {
            CultivationRealm realm = cultivation.getRealm(type);
            if (realm != null) {
                // Apply cumulative bonuses from mortal to current realm SILENTLY
                applyCumulativeRealmBonuses(player, type, realm, cultivation);

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("Silently reapplied cumulative {} realm modifiers up to: {}",
                        type.getDisplayName(), realm.getDisplayName());
                }
            }
        }

        // Refresh stats to apply changes
        refreshStats(player);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Silent cumulative realm modifier reapplication complete for player: {}",
                player.getName().getString());
        }
    }

    /**
     * Updates vanilla health display to match custom health and handles death when appropriate
     *
     * @param player The player
     * @param statsAttachment The stats attachment
     */
    private static void updateVanillaHealthDisplay(Player player, PlayerStatsAttachment statsAttachment) {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Updating vanilla health for {}: customHealth={}/{}",
                player.getName().getString(),
                statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                statsAttachment.getMaxStat(IStatsData.StatType.HEALTH));
        }

        long currentCustomHealth = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);
        long maxCustomHealth = statsAttachment.getMaxStat(IStatsData.StatType.HEALTH);

        // Check if player should die
        if (currentCustomHealth <= 0) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Player {} custom health reached 0, triggering death",
                    player.getName().getString());
            }

            // Set vanilla health to 0 to trigger death
            player.setHealth(0.0f);
            lastKnownVanillaHealth.put(player.getUUID(), 0.0f);
            return;
        }

        // Calculate health percentage and update vanilla display
        float percentage = (float) currentCustomHealth / maxCustomHealth;
        float vanillaHealth = percentage * player.getMaxHealth();

        // Ensure minimum of 0.1f when alive (but allow 0.0f for death)
        float adjustedHealth = Math.max(0.1f, vanillaHealth);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Setting vanilla health to {} (percentage: {}, vanillaHealth: {})",
                adjustedHealth, percentage, vanillaHealth);
        }

        player.setHealth(adjustedHealth);
        lastKnownVanillaHealth.put(player.getUUID(), adjustedHealth);
    }

    /**
     * Restores all player stats (Health, Qi, Body) to their maximum values
     * Used during respawn to give players a fresh start
     *
     * @param player The player whose stats should be restored
     * @param statsAttachment The player's stats attachment
     */
    private static void restoreStatsToFull(ServerPlayer player, PlayerStatsAttachment statsAttachment) {
        try {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Restoring stats to full for {}: Before - H:{}/{} Q:{}/{} B:{}/{}",
                    player.getName().getString(),
                    statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                    statsAttachment.getMaxStat(IStatsData.StatType.QI),
                    statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                    statsAttachment.getMaxStat(IStatsData.StatType.BODY));
            }

            // Restore Health to maximum using the correct method name
            long maxHealth = statsAttachment.getMaxStat(IStatsData.StatType.HEALTH);
            statsAttachment.setCurrentStat(IStatsData.StatType.HEALTH, maxHealth);

            // Restore Qi to maximum
            long maxQi = statsAttachment.getMaxStat(IStatsData.StatType.QI);
            statsAttachment.setCurrentStat(IStatsData.StatType.QI, maxQi);

            // Restore Body to maximum
            long maxBody = statsAttachment.getMaxStat(IStatsData.StatType.BODY);
            statsAttachment.setCurrentStat(IStatsData.StatType.BODY, maxBody);

            // Update vanilla health display to match restored custom health
            updateVanillaHealthDisplay(player, statsAttachment);

            // Mark for synchronization
            statsAttachment.markDirty();

            ImmortalCultivation.LOGGER.info("Successfully restored all stats to full for player: {} - H:{}/{} Q:{}/{} B:{}/{}",
                player.getName().getString(),
                statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                statsAttachment.getMaxStat(IStatsData.StatType.QI),
                statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                statsAttachment.getMaxStat(IStatsData.StatType.BODY));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error restoring stats to full for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Gets the complete progression path from mortal realm to the target realm.
     * This uses the realm level system to include ALL realms that should be traversed,
     * including different stages and special realms nested between regular realms.
     *
     * @param targetRealm The target realm
     * @return List of realms in progression order (including target)
     */
    private static List<CultivationRealm> getRealmProgressionPath(CultivationRealm targetRealm) {
        List<CultivationRealm> path = new ArrayList<>();

        try {
            int targetLevel = targetRealm.getRealmLevel();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Building progression path to realm: {} (Level {})",
                    targetRealm.getId(), targetLevel);
            }

            // Get all available realms as a collection
            Collection<CultivationRealm> allRealms = ICModCultivationRealms.getAllRealms();

            // Create a list of all realms with levels from 1 to target level
            List<CultivationRealm> candidateRealms = new ArrayList<>();
            for (CultivationRealm realm : allRealms) {
                int realmLevel = realm.getRealmLevel();
                if (realmLevel > 0 && realmLevel <= targetLevel) {
                    candidateRealms.add(realm);
                    if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                        ImmortalCultivation.LOGGER.debug("Added candidate realm: {} (Level {})",
                            realm.getId(), realmLevel);
                    }
                }
            }

            // Sort by realm level to ensure proper progression order
            candidateRealms.sort((a, b) -> Integer.compare(a.getRealmLevel(), b.getRealmLevel()));

            // Add all realms in order
            path.addAll(candidateRealms);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Generated complete progression path with {} realms for target: {} (Level {})",
                    path.size(), targetRealm.getId(), targetLevel);

                // Log the complete path for debugging
                StringBuilder pathStr = new StringBuilder("Progression path: ");
                for (int i = 0; i < path.size(); i++) {
                    CultivationRealm realm = path.get(i);
                    pathStr.append(realm.getId()).append("(L").append(realm.getRealmLevel()).append(")");
                    if (i < path.size() - 1) pathStr.append(" -> ");
                }
                ImmortalCultivation.LOGGER.debug(pathStr.toString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error building progression path for realm {}: {}",
                targetRealm.getId(), e.getMessage(), e);
            // Fallback: just add the target realm
            path.add(targetRealm);
        }

        return path;
    }
}
