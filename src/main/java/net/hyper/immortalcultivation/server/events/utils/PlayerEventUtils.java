package net.hyper.immortalcultivation.server.events.utils;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.*;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Utility class for player event-related operations in the cultivation system.
 *
 * This util class contains shared business logic for player lifecycle management,
 * attachment initialization, synchronization, and state management. It provides
 * the foundation for player cultivation data handling across login, respawn,
 * and dimension change events.
 *
 * Key Responsibilities:
 * - Initialize and manage player attachments
 * - Handle player synchronization with clients
 * - Apply and manage cultivation realm modifiers
 * - Restore player stats during respawn
 * - Update vanilla health display integration
 * - Provide utility methods for player data operations
 *
 * Architecture:
 * - Util Pattern: Contains shared business logic for player operations
 * - Integrates with PlayerLifecycleEventHandler through public static methods
 * - Uses generic methods for different attachment types
 * - Follows DRY principles for player data management
 */
public class PlayerEventUtils {

    // === CONSTANTS === //
    private static final float MIN_VANILLA_HEALTH_ALIVE = 0.1f;
    private static final long SYNC_RETRY_DELAY_MS = 100L;
    private static final int MAX_SYNC_RETRIES = 3;

    // === ATTACHMENT MANAGEMENT === //
    private static final List<Supplier<? extends AttachmentType<?>>> MANAGED_ATTACHMENTS = List.of(
        ICModAttachmentTypes.PLAYER_CULTIVATION,
        ICModAttachmentTypes.PLAYER_STATS,
        ICModAttachmentTypes.PLAYER_BREAKTHROUGH,
        ICModAttachmentTypes.PLAYER_TALENTS,
        ICModAttachmentTypes.PLAYER_RACE,
        ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION
    );

    // === TRACKING MAPS === //
    private static final Map<UUID, Float> lastKnownVanillaHealth = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSyncAttempts = new ConcurrentHashMap<>();
    private static final Map<String, Integer> initializationFailures = new ConcurrentHashMap<>();

    // === PLAYER INITIALIZATION === //

    /**
     * Initializes all managed attachments for a player.
     * This method ensures all cultivation-related attachments are properly created
     * and initialized for a player during login or other lifecycle events.
     *
     * @param player The server player to initialize attachments for
     */
    public static void initializePlayerAttachments(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("[PlayerEventUtils] Cannot initialize attachments for null player");
            return;
        }

        String playerId = player.getUUID().toString();

        try {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Initializing {} attachment types for player: {}",
                    MANAGED_ATTACHMENTS.size(), player.getName().getString());
            }

            for (Supplier<? extends AttachmentType<?>> attachmentSupplier : MANAGED_ATTACHMENTS) {
                initializeAttachment(player, attachmentSupplier);
            }

            // Clear any previous initialization failures for this player
            initializationFailures.remove(playerId);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Successfully initialized all attachments for player: {}",
                    player.getName().getString());
            }

        } catch (Exception e) {
            int failureCount = initializationFailures.merge(playerId, 1, Integer::sum);
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error initializing attachments for player {} (attempt {}): {}",
                player.getName().getString(), failureCount, e.getMessage(), e);
        }
    }

    /**
     * Initializes a single attachment for a player.
     * This method handles the creation and setup of individual attachment types.
     *
     * @param player The server player
     * @param attachmentSupplier The supplier for the attachment type
     */
    private static void initializeAttachment(ServerPlayer player, Supplier<? extends AttachmentType<?>> attachmentSupplier) {
        try {
            AttachmentType<?> attachmentType = attachmentSupplier.get();
            String attachmentName = attachmentType.toString();

            // Create attachment if it doesn't exist
            if (!player.hasData(attachmentType)) {
                player.getData(attachmentType); // This creates with default values

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Created new attachment: {} for player: {}",
                        attachmentName, player.getName().getString());
                }
            }

            // Initialize if the attachment supports it
            Object attachment = player.getExistingData(attachmentType).orElse(null);
            if (attachment != null) {
                // Set player reference if supported
                setPlayerReferenceIfSupported(attachment, player);

                // Initialize if supported
                if (attachment instanceof IPlayerInitializable initializable) {
                    initializable.onPlayerLogin(player);

                    if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                        ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Initialized attachment: {} for player: {}",
                            attachmentName, player.getName().getString());
                    }
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error initializing single attachment for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Sets player reference on attachment if it supports it.
     * This ensures attachments have proper player context for packet creation and operations.
     *
     * @param attachment The attachment object
     * @param player The player to set as reference
     */
    public static void setPlayerReferenceIfSupported(Object attachment, ServerPlayer player) {
        try {
            if (attachment instanceof PlayerTalentsAttachment talents) {
                talents.setPlayer(player);
            } else if (attachment instanceof PlayerStatsAttachment stats) {
                stats.setPlayer(player);
            } else if (attachment instanceof PlayerCultivationAttachment cultivation) {
                cultivation.setPlayer(player);
            } else if (attachment instanceof PlayerBreakthroughAttachment breakthrough) {
                breakthrough.setPlayer(player);
            } else if (attachment instanceof PlayerRaceAttachment race) {
                race.setPlayer(player);
            } else if (attachment instanceof PlayerPillConsumptionAttachment pills) {
                pills.setPlayer(player);
            }
            // Add other attachment types as needed when you create new attachments

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error setting player reference: {}", e.getMessage(), e);
        }
    }

    // === SYNCHRONIZATION === //

    /**
     * Synchronizes all player data to the client.
     * This method handles sending all attachment data to the client for proper
     * display and interaction with the cultivation system.
     *
     * @param player The server player to sync data for
     */
    public static void synchronizeAllPlayerData(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("[PlayerEventUtils] Cannot sync data for null player");
            return;
        }

        try {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Synchronizing all player data for: {}",
                    player.getName().getString());
            }

            int syncCount = 0;
            int failCount = 0;

            for (Supplier<? extends AttachmentType<?>> attachmentSupplier : MANAGED_ATTACHMENTS) {
                if (synchronizeAttachment(player, attachmentSupplier)) {
                    syncCount++;
                } else {
                    failCount++;
                }
            }

            lastSyncAttempts.put(player.getUUID(), System.currentTimeMillis());

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Sync complete for {}: {} successful, {} failed",
                    player.getName().getString(), syncCount, failCount);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error synchronizing player data for {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Synchronizes a single attachment for a player.
     *
     * @param player The server player
     * @param attachmentSupplier The supplier for the attachment type
     * @return true if synchronization was successful
     */
    private static boolean synchronizeAttachment(ServerPlayer player, Supplier<? extends AttachmentType<?>> attachmentSupplier) {
        try {
            AttachmentType<?> attachmentType = attachmentSupplier.get();
            Object attachment = player.getExistingData(attachmentType).orElse(null);

            if (attachment instanceof ISyncable syncable) {
                // Ensure player reference is set
                setPlayerReferenceIfSupported(attachment, player);

                // Create and send sync packet
                Optional<ISyncPacket> packetOptional = syncable.createPacket(true); // true for full sync
                if (packetOptional.isPresent()) {
                    ICModPacketRegistry.sendToPlayer(packetOptional.get(), player);
                    syncable.clearDirty();

                    if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                        ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Synced {} for player {}",
                            attachmentType.toString(), player.getName().getString());
                    }
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error syncing single attachment for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
            return false;
        }
    }

    // === REALM MODIFIER MANAGEMENT === //

    /**
     * Applies current realm modifiers for all cultivation types.
     * This method clears existing REALM bonuses and reapplies them to prevent
     * accumulation of bonuses on player login/respawn, while preserving
     * bonuses from other sources like pills.
     *
     * @param player The server player to apply modifiers for
     */
    public static void applyCurrentRealmModifiers(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("[PlayerEventUtils] Cannot apply realm modifiers for null player");
            return;
        }

        try {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Applying current realm modifiers for player: {}",
                    player.getName().getString());
            }

            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            PlayerStatsAttachment stats = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());

            if (cultivation == null || stats == null) {
                ImmortalCultivation.LOGGER.warn("[PlayerEventUtils] Missing attachments for player {}: cultivation={}, stats={}",
                    player.getName().getString(), cultivation != null, stats != null);
                return;
            }

            // Clear only REALM bonuses, preserving pill and other bonuses
            stats.clearBonusesFromSource(BonusSource.REALM);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Cleared REALM bonuses, preserved other sources");
            }

            // Apply cumulative realm bonuses for each cultivation type
            for (ICultivationData.CultivationType type : ICultivationData.CultivationType.values()) {
                applyRealmModifiersForType(player, type, cultivation, stats);
            }

            // Refresh stats to apply changes (using AttributeModifierUtils)
            AttributeModifierUtils.refreshPlayerAttributes(player);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Realm modifier application complete for player: {}",
                    player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error applying realm modifiers for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Applies realm modifiers for a specific cultivation type.
     *
     * @param player The server player
     * @param type The cultivation type
     * @param cultivation The cultivation attachment
     * @param stats The stats attachment
     */
    private static void applyRealmModifiersForType(ServerPlayer player, ICultivationData.CultivationType type,
                                                   PlayerCultivationAttachment cultivation, PlayerStatsAttachment stats) {
        try {
            CultivationRealm realm = cultivation.getRealm(type);
            if (realm == null) {
                return;
            }

            // Apply cumulative bonuses from mortal to current realm
            applyCumulativeRealmBonuses(player, type, realm, cultivation, stats);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Applied cumulative {} realm modifiers up to: {}",
                    type.getDisplayName(), realm.getDisplayName());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error applying realm modifiers for type {} on player {}: {}",
                type.getDisplayName(), player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Applies cumulative realm bonuses from mortal realm to the current realm.
     * This method adds up all bonuses from each realm level to provide the total
     * cultivation benefit the player should have.
     *
     * @param player The server player
     * @param type The cultivation type
     * @param currentRealm The player's current realm
     * @param cultivation The cultivation attachment
     * @param stats The stats attachment
     */
    private static void applyCumulativeRealmBonuses(ServerPlayer player, ICultivationData.CultivationType type,
                                                    CultivationRealm currentRealm, PlayerCultivationAttachment cultivation,
                                                    PlayerStatsAttachment stats) {
        try {
            Collection<CultivationRealm> realmPath = getRealmProgressionPath(type, currentRealm);

            for (CultivationRealm realm : realmPath) {
                // Apply stat bonuses from this realm
                applyRealmStatBonuses(realm, stats);

                // Apply attribute bonuses from this realm
                applyRealmAttributeBonuses(realm, stats);
            }

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Applied bonuses from {} realms for {} cultivation",
                    realmPath.size(), type.getDisplayName());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error applying cumulative realm bonuses: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets the complete progression path from mortal realm to the target realm.
     *
     * @param type The cultivation type
     * @param targetRealm The target realm
     * @return Collection of realms in progression order
     */
    private static Collection<CultivationRealm> getRealmProgressionPath(ICultivationData.CultivationType type, CultivationRealm targetRealm) {
        List<CultivationRealm> path = new ArrayList<>();

        try {
            // Get all realms for this cultivation type from the registry
            Collection<CultivationRealm> allRealms = ICModCultivationRealms.getAllRealms();

            // Sort by realm level and filter up to target realm
            allRealms.stream()
                .filter(realm -> realm.getRealmLevel() <= targetRealm.getRealmLevel())
                .sorted(Comparator.comparingInt(CultivationRealm::getRealmLevel))
                .forEach(path::add);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error getting realm progression path: {}", e.getMessage(), e);
        }

        return path;
    }

    /**
     * Applies stat bonuses from a specific realm.
     *
     * @param realm The realm providing bonuses
     * @param stats The stats attachment to apply bonuses to
     */
    private static void applyRealmStatBonuses(CultivationRealm realm, PlayerStatsAttachment stats) {
        try {
            // Apply health bonus
            long healthBonus = realm.getBaseMaxHealth();
            if (healthBonus > 0) {
                long currentBonus = stats.getSourcedStatBonus(IStatsData.StatType.HEALTH, BonusSource.REALM);
                stats.setSourcedStatBonus(IStatsData.StatType.HEALTH, BonusSource.REALM, currentBonus + healthBonus);
            }

            // Apply qi bonus
            long qiBonus = realm.getBaseMaxQi();
            if (qiBonus > 0) {
                long currentBonus = stats.getSourcedStatBonus(IStatsData.StatType.QI, BonusSource.REALM);
                stats.setSourcedStatBonus(IStatsData.StatType.QI, BonusSource.REALM, currentBonus + qiBonus);
            }

            // Apply body bonus
            long bodyBonus = realm.getBaseMaxBody();
            if (bodyBonus > 0) {
                long currentBonus = stats.getSourcedStatBonus(IStatsData.StatType.BODY, BonusSource.REALM);
                stats.setSourcedStatBonus(IStatsData.StatType.BODY, BonusSource.REALM, currentBonus + bodyBonus);
            }

            // Apply regeneration bonuses
            double healthRegenBonus = realm.getBaseHealthRegenRate();
            if (healthRegenBonus > 0.0) {
                double currentRegenBonus = stats.getSourcedStatRegenBonus(IStatsData.StatType.HEALTH, BonusSource.REALM);
                stats.setSourcedStatRegenBonus(IStatsData.StatType.HEALTH, BonusSource.REALM, currentRegenBonus + healthRegenBonus);
            }

            double qiRegenBonus = realm.getBaseQiRegenRate();
            if (qiRegenBonus > 0.0) {
                double currentRegenBonus = stats.getSourcedStatRegenBonus(IStatsData.StatType.QI, BonusSource.REALM);
                stats.setSourcedStatRegenBonus(IStatsData.StatType.QI, BonusSource.REALM, currentRegenBonus + qiRegenBonus);
            }

            double bodyRegenBonus = realm.getBaseBodyRegenRate();
            if (bodyRegenBonus > 0.0) {
                double currentRegenBonus = stats.getSourcedStatRegenBonus(IStatsData.StatType.BODY, BonusSource.REALM);
                stats.setSourcedStatRegenBonus(IStatsData.StatType.BODY, BonusSource.REALM, currentRegenBonus + bodyRegenBonus);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error applying realm stat bonuses: {}", e.getMessage(), e);
        }
    }

    /**
     * Applies attribute bonuses from a specific realm.
     *
     * @param realm The realm providing bonuses
     * @param stats The stats attachment to apply bonuses to
     */
    private static void applyRealmAttributeBonuses(CultivationRealm realm, PlayerStatsAttachment stats) {
        try {
            // Apply attack damage bonus
            double attackBonus = realm.getBaseAttackDamage();
            if (attackBonus > 0.0) {
                double currentBonus = stats.getSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.ATTACK_DAMAGE, BonusSource.REALM);
                stats.setSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.ATTACK_DAMAGE, BonusSource.REALM, currentBonus + attackBonus);
            }

            // Apply movement speed bonus
            double speedBonus = realm.getBaseMovementSpeed();
            if (speedBonus > 0.0) {
                double currentBonus = stats.getSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.MOVEMENT_SPEED, BonusSource.REALM);
                stats.setSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.MOVEMENT_SPEED, BonusSource.REALM, currentBonus + speedBonus);
            }

            // Apply jump height bonus
            double jumpBonus = realm.getBaseJumpHeight();
            if (jumpBonus > 0.0) {
                double currentBonus = stats.getSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.JUMP_HEIGHT, BonusSource.REALM);
                stats.setSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.JUMP_HEIGHT, BonusSource.REALM, currentBonus + jumpBonus);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error applying realm attribute bonuses: {}", e.getMessage(), e);
        }
    }

    // === HEALTH MANAGEMENT === //

    /**
     * Updates vanilla health display to match custom health and handles death when appropriate.
     * This method bridges the custom cultivation health system with Minecraft's vanilla health display.
     *
     * @param player The player whose health display should be updated
     * @param statsAttachment The stats attachment containing custom health values
     */
    public static void updateVanillaHealthDisplay(Player player, PlayerStatsAttachment statsAttachment) {
        if (player == null || statsAttachment == null) {
            return;
        }

        try {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Updating vanilla health for {}: customHealth={}/{}",
                    player.getName().getString(),
                    statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getMaxStat(IStatsData.StatType.HEALTH));
            }

            long currentCustomHealth = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);
            long maxCustomHealth = statsAttachment.getMaxStat(IStatsData.StatType.HEALTH);

            // Check if player should die
            if (currentCustomHealth <= 0) {
                handlePlayerDeath(player);
                return;
            }

            // Calculate health percentage and update vanilla display
            float percentage = (float) currentCustomHealth / maxCustomHealth;
            float vanillaHealth = percentage * player.getMaxHealth();

            // Ensure minimum health when alive
            float adjustedHealth = Math.max(MIN_VANILLA_HEALTH_ALIVE, vanillaHealth);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Setting vanilla health to {} (percentage: {})",
                    adjustedHealth, percentage);
            }

            player.setHealth(adjustedHealth);
            lastKnownVanillaHealth.put(player.getUUID(), adjustedHealth);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error updating vanilla health display for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Handles player death when custom health reaches zero.
     *
     * @param player The player who should die
     */
    private static void handlePlayerDeath(Player player) {
        try {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Player {} custom health reached 0, triggering death",
                    player.getName().getString());
            }

            // Set vanilla health to 0 to trigger death
            player.setHealth(0.0f);
            lastKnownVanillaHealth.put(player.getUUID(), 0.0f);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error handling player death for {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Restores all player stats (Health, Qi, Body) to their maximum values.
     * Used during respawn to give players a fresh start with full resources.
     *
     * @param player The server player whose stats should be restored
     * @param statsAttachment The player's stats attachment
     */
    public static void restoreStatsToFull(ServerPlayer player, PlayerStatsAttachment statsAttachment) {
        if (player == null || statsAttachment == null) {
            return;
        }

        try {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Restoring stats to full for {}: Before - H:{}/{} Q:{}/{} B:{}/{}",
                    player.getName().getString(),
                    statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                    statsAttachment.getMaxStat(IStatsData.StatType.QI),
                    statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                    statsAttachment.getMaxStat(IStatsData.StatType.BODY));
            }

            // Restore each stat type to its maximum value
            restoreStatToFull(statsAttachment, IStatsData.StatType.HEALTH);
            restoreStatToFull(statsAttachment, IStatsData.StatType.QI);
            restoreStatToFull(statsAttachment, IStatsData.StatType.BODY);

            // Update vanilla health display to match restored custom health
            updateVanillaHealthDisplay(player, statsAttachment);

            // Mark for synchronization
            statsAttachment.markDirty();

            ImmortalCultivation.LOGGER.info("[PlayerEventUtils] Successfully restored all stats to full for player: {} - H:{}/{} Q:{}/{} B:{}/{}",
                player.getName().getString(),
                statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                statsAttachment.getMaxStat(IStatsData.StatType.QI),
                statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                statsAttachment.getMaxStat(IStatsData.StatType.BODY));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error restoring stats to full for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Restores a single stat type to its maximum value.
     *
     * @param statsAttachment The stats attachment
     * @param statType The stat type to restore
     */
    private static void restoreStatToFull(PlayerStatsAttachment statsAttachment, IStatsData.StatType statType) {
        try {
            long maxValue = statsAttachment.getMaxStat(statType);
            statsAttachment.setCurrentStat(statType, maxValue);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error restoring {} stat to full: {}",
                statType.getDisplayName(), e.getMessage(), e);
        }
    }

    // === UTILITY METHODS === //

    /**
     * Clears cached data for a player (used during logout or cleanup).
     *
     * @param playerId The player's UUID
     */
    public static void clearPlayerCache(UUID playerId) {
        try {
            lastKnownVanillaHealth.remove(playerId);
            lastSyncAttempts.remove(playerId);
            initializationFailures.remove(playerId.toString());

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[PlayerEventUtils] Cleared cache for player: {}", playerId);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[PlayerEventUtils] Error clearing player cache: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets debug information about player event utilities.
     *
     * @return Debug information string
     */
    public static String getDebugInfo() {
        return String.format("PlayerEventUtils - Managed Attachments: %d, Cached Players: %d, Sync Attempts: %d",
            MANAGED_ATTACHMENTS.size(), lastKnownVanillaHealth.size(), lastSyncAttempts.size());
    }
}