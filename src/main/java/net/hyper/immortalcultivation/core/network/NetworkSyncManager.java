package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.hyper.immortalcultivation.core.network.packets.server.UnifiedDeltaSyncPacket;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance network sync manager that handles delta syncing, batching,
 * and prioritization for all cultivation attachments including race, talent, and pill consumption data.
 */
public class NetworkSyncManager {

    // Priority levels for sync operations
    public enum SyncPriority {
        CRITICAL(100, 500),    // Sync within 500ms, max changes: 100
        HIGH(50, 1000),        // Sync within 1s, max changes: 50
        NORMAL(20, 3000),      // Sync within 3s, max changes: 20
        LOW(10, 5000);         // Sync within 5s, max changes: 10

        public final int maxChanges;
        public final long maxDelayMs;

        SyncPriority(int maxChanges, long maxDelayMs) {
            this.maxChanges = maxChanges;
            this.maxDelayMs = maxDelayMs;
        }
    }

    // Player sync states for delta detection
    private static final Map<UUID, PlayerSyncState> playerSyncStates = new ConcurrentHashMap<>();

    // Pending sync batches by priority
    private static final Map<SyncPriority, Map<UUID, PendingSyncBatch>> pendingSyncs = new EnumMap<>(SyncPriority.class);

    // Performance tracking
    private static final Map<UUID, Long> lastFullSyncTime = new ConcurrentHashMap<>();

    static {
        for (SyncPriority priority : SyncPriority.values()) {
            pendingSyncs.put(priority, new ConcurrentHashMap<>());
        }
    }

    /**
     * Main sync method - analyzes all attachments and schedules appropriate syncs.
     * Now includes support for race, talent, and pill consumption attachments.
     */
    public static void scheduleSync(ServerPlayer player, SyncPriority priority) {
        UUID playerId = player.getUUID();
        PlayerSyncState syncState = playerSyncStates.computeIfAbsent(playerId, k -> new PlayerSyncState());

        // Get current attachment states
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
        PlayerTalentAttachment talent = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
        PlayerPillConsumptionAttachment pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

        // Detect changes and build delta packet
        UnifiedDeltaSyncPacket deltaPacket = buildDeltaPacket(player, syncState, cultivation, resources, combatStats,
            race, talent, pillConsumption);

        if (deltaPacket != null) {
            // Add to pending batch or send immediately based on priority
            if (priority == SyncPriority.CRITICAL) {
                sendImmediately(player, deltaPacket);
                updateSyncState(syncState, cultivation, resources, combatStats, race, talent, pillConsumption);
            } else {
                addToPendingBatch(player, deltaPacket, priority);
            }
        }
    }

    /**
     * Process all pending sync batches - call this from your tick handler
     */
    public static void processPendingSyncs() {
        long currentTime = System.currentTimeMillis();

        for (SyncPriority priority : SyncPriority.values()) {
            Map<UUID, PendingSyncBatch> batches = pendingSyncs.get(priority);
            Iterator<Map.Entry<UUID, PendingSyncBatch>> iterator = batches.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, PendingSyncBatch> entry = iterator.next();
                UUID playerId = entry.getKey();
                PendingSyncBatch batch = entry.getValue();

                if (shouldSendBatch(batch, priority, currentTime)) {
                    ServerPlayer player = getServerPlayer(playerId);
                    if (player != null) {
                        sendBatch(player, batch);
                        PlayerSyncState syncState = playerSyncStates.get(playerId);
                        if (syncState != null) {
                            updateSyncStateFromLatestPacket(player, syncState);
                        }
                    }
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Build delta packet with support for all attachment types including the new ones.
     * Performs comprehensive change detection and only includes changed data in the packet.
     */
    private static UnifiedDeltaSyncPacket buildDeltaPacket(ServerPlayer player, PlayerSyncState syncState,
                                                           PlayerCultivationAttachment cultivation,
                                                           PlayerResourceAttachment resources,
                                                           PlayerCombatStatsAttachment combatStats,
                                                           PlayerRaceAttachment race,
                                                           PlayerTalentAttachment talent,
                                                           PlayerPillConsumptionAttachment pillConsumption) {

        byte attachmentMask = 0;
        short cultivationFieldMask = 0;
        short resourceFieldMask = 0;
        byte combatStatsFieldMask = 0;
        byte raceFieldMask = 0;
        byte talentFieldMask = 0;
        byte pillConsumptionFieldMask = 0;

        boolean hasChanges = false;

        // === CULTIVATION CHANGES ===
        Optional<String> currentQiRealmId = Optional.empty();
        Optional<String> currentBodyRealmId = Optional.empty();
        Optional<Long> currentQiCultivation = Optional.empty();
        Optional<Long> maxQiCultivation = Optional.empty();
        Optional<Long> currentBodyCultivation = Optional.empty();
        Optional<Long> maxBodyCultivation = Optional.empty();
        Optional<PlayerCultivationAttachment.CultivationType> currentFocus = Optional.empty();
        Optional<UnifiedDeltaSyncPacket.PackedBonuses> bonuses = Optional.empty();
        Optional<UnifiedDeltaSyncPacket.PackedBreakthroughData> breakthroughData = Optional.empty();

        if (cultivation != null) {
            String qiRealmId = cultivation.getCurrentRealm(ICultivationData.CultivationType.QI).getId();
            String bodyRealmId = cultivation.getCurrentRealm(ICultivationData.CultivationType.BODY).getId();
            long qiCultivationVal = cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI);
            long maxQiCultivationVal = cultivation.getMaxCultivation(ICultivationData.CultivationType.QI);
            long bodyCultivationVal = cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY);
            long maxBodyCultivationVal = cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY);
            PlayerCultivationAttachment.CultivationType focus = cultivation.getCurrentFocus();

            if (!qiRealmId.equals(syncState.lastQiRealmId)) {
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_QI_REALM;
                currentQiRealmId = Optional.of(qiRealmId);
                hasChanges = true;
            }
            if (!bodyRealmId.equals(syncState.lastBodyRealmId)) {
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BODY_REALM;
                currentBodyRealmId = Optional.of(bodyRealmId);
                hasChanges = true;
            }
            if (qiCultivationVal != syncState.lastCurrentQiCultivation) {
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_QI_CURRENT;
                currentQiCultivation = Optional.of(qiCultivationVal);
                hasChanges = true;
            }
            if (maxQiCultivationVal != syncState.lastMaxQiCultivation) {
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_QI_MAX;
                maxQiCultivation = Optional.of(maxQiCultivationVal);
                hasChanges = true;
            }
            if (bodyCultivationVal != syncState.lastCurrentBodyCultivation) {
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BODY_CURRENT;
                currentBodyCultivation = Optional.of(bodyCultivationVal);
                hasChanges = true;
            }
            if (maxBodyCultivationVal != syncState.lastMaxBodyCultivation) {
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BODY_MAX;
                maxBodyCultivation = Optional.of(maxBodyCultivationVal);
                hasChanges = true;
            }
            if (focus != syncState.lastCurrentFocus) {
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_FOCUS;
                currentFocus = Optional.of(focus);
                hasChanges = true;
            }

            // Check bonuses
            double qiAdditiveBonus = cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.QI);
            double qiMultiplicativeBonus = cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.QI);
            double bodyAdditiveBonus = cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.BODY);
            double bodyMultiplicativeBonus = cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.BODY);

            if (hasChanged(qiAdditiveBonus, syncState.lastQiAdditiveBonus, 0.001) ||
                hasChanged(qiMultiplicativeBonus, syncState.lastQiMultiplicativeBonus, 0.001) ||
                hasChanged(bodyAdditiveBonus, syncState.lastBodyAdditiveBonus, 0.001) ||
                hasChanged(bodyMultiplicativeBonus, syncState.lastBodyMultiplicativeBonus, 0.001)) {

                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BONUSES;
                bonuses = Optional.of(new UnifiedDeltaSyncPacket.PackedBonuses(
                    qiAdditiveBonus, qiMultiplicativeBonus, bodyAdditiveBonus, bodyMultiplicativeBonus));
                hasChanges = true;
            }

            // Check breakthrough data
            PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
            if (breakthrough != null) {
                long qiBreakthroughAttempts = breakthrough.getBreakthroughAttempts(ICultivationData.CultivationType.QI);
                long bodyBreakthroughAttempts = breakthrough.getBreakthroughAttempts(ICultivationData.CultivationType.BODY);
                boolean qiBreakthroughCooldown = breakthrough.isBreakthroughOnCooldown(ICultivationData.CultivationType.QI);
                boolean bodyBreakthroughCooldown = breakthrough.isBreakthroughOnCooldown(ICultivationData.CultivationType.BODY);
                long qiBreakthroughAttempt = breakthrough.getLastBreakthroughAttemptTime(ICultivationData.CultivationType.QI);
                long bodyBreakthroughAttempt = breakthrough.getLastBreakthroughAttemptTime(ICultivationData.CultivationType.BODY);

                if (qiBreakthroughAttempts != syncState.lastQiBreakthroughAttempts ||
                    bodyBreakthroughAttempts != syncState.lastBodyBreakthroughAttempts ||
                    qiBreakthroughCooldown != syncState.lastQiBreakthroughCooldown ||
                    bodyBreakthroughCooldown != syncState.lastBodyBreakthroughCooldown ||
                    qiBreakthroughAttempt != syncState.lastQiBreakthroughAttempt ||
                    bodyBreakthroughAttempt != syncState.lastBodyBreakthroughAttempt) {

                    cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BREAKTHROUGH;
                    breakthroughData = Optional.of(new UnifiedDeltaSyncPacket.PackedBreakthroughData(
                        qiBreakthroughAttempts, bodyBreakthroughAttempts,
                        qiBreakthroughCooldown, bodyBreakthroughCooldown,
                        qiBreakthroughAttempt, bodyBreakthroughAttempt));
                    hasChanges = true;
                }
            }

            if (cultivationFieldMask != 0) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
            }
        }

        // === RESOURCE CHANGES ===
        Optional<Long> currentHealth = Optional.empty();
        Optional<Long> currentQi = Optional.empty();
        Optional<Long> currentBody = Optional.empty();
        Optional<UnifiedDeltaSyncPacket.PackedPermanentBonuses> permanentBonuses = Optional.empty();

        if (resources != null) {
            long healthVal = resources.getCurrentHealth();
            long qiVal = resources.getCurrentQi();
            long bodyVal = resources.getCurrentBody();

            if (healthVal != syncState.lastCurrentHealth) {
                resourceFieldMask |= UnifiedDeltaSyncPacket.RES_HEALTH;
                currentHealth = Optional.of(healthVal);
                hasChanges = true;
            }
            if (qiVal != syncState.lastCurrentQi) {
                resourceFieldMask |= UnifiedDeltaSyncPacket.RES_QI;
                currentQi = Optional.of(qiVal);
                hasChanges = true;
            }
            if (bodyVal != syncState.lastCurrentBody) {
                resourceFieldMask |= UnifiedDeltaSyncPacket.RES_BODY;
                currentBody = Optional.of(bodyVal);
                hasChanges = true;
            }

            // Check permanent bonuses
            long permHealthBonus = resources.getPermanentHealthBonus();
            double permHealthRegenBonus = resources.getPermanentHealthRegenBonus();
            long permMaxQiBonus = resources.getPermanentMaxQiBonus();
            double permQiRegenBonus = resources.getPermanentQiRegenBonus();
            long permMaxBodyBonus = resources.getPermanentMaxBodyBonus();
            double permBodyRegenBonus = resources.getPermanentBodyRegenBonus();

            if (permHealthBonus != syncState.lastPermanentHealthBonus ||
                hasChanged(permHealthRegenBonus, syncState.lastPermanentHealthRegenBonus, 0.001) ||
                permMaxQiBonus != syncState.lastPermanentMaxQiBonus ||
                hasChanged(permQiRegenBonus, syncState.lastPermanentQiRegenBonus, 0.001) ||
                permMaxBodyBonus != syncState.lastPermanentMaxBodyBonus ||
                hasChanged(permBodyRegenBonus, syncState.lastPermanentBodyRegenBonus, 0.001)) {

                resourceFieldMask |= UnifiedDeltaSyncPacket.RES_PERMANENT;
                permanentBonuses = Optional.of(new UnifiedDeltaSyncPacket.PackedPermanentBonuses(
                    permHealthBonus, permHealthRegenBonus, permMaxQiBonus,
                    permQiRegenBonus, permMaxBodyBonus, permBodyRegenBonus));
                hasChanges = true;
            }

            if (resourceFieldMask != 0) {
                attachmentMask |= UnifiedDeltaSyncPacket.RESOURCE_MASK;
            }
        }

        // === COMBAT STATS CHANGES ===
        Optional<UnifiedDeltaSyncPacket.PackedCombatStats> combatStatsData = Optional.empty();

        if (combatStats != null) {
            double attackBonus = combatStats.getAttackDamageBonus();
            double speedBonus = combatStats.getMovementSpeedBonus();
            double jumpBonus = combatStats.getJumpHeightBonus();
            double permAttackBonus = combatStats.getPermanentAttackDamageBonus();
            double permSpeedBonus = combatStats.getPermanentMovementSpeedBonus();
            double permJumpBonus = combatStats.getPermanentJumpHeightBonus();

            if (hasChanged(attackBonus, syncState.lastAttackBonus, 0.001) ||
                hasChanged(speedBonus, syncState.lastSpeedBonus, 0.001) ||
                hasChanged(jumpBonus, syncState.lastJumpBonus, 0.001) ||
                hasChanged(permAttackBonus, syncState.lastPermanentAttackBonus, 0.001) ||
                hasChanged(permSpeedBonus, syncState.lastPermanentSpeedBonus, 0.001) ||
                hasChanged(permJumpBonus, syncState.lastPermanentJumpBonus, 0.001)) {

                combatStatsFieldMask |= UnifiedDeltaSyncPacket.COMBAT_ALL;
                combatStatsData = Optional.of(new UnifiedDeltaSyncPacket.PackedCombatStats(
                    attackBonus, speedBonus, jumpBonus, permAttackBonus, permSpeedBonus, permJumpBonus));
                attachmentMask |= UnifiedDeltaSyncPacket.COMBAT_STATS_MASK;
                hasChanges = true;
            }
        }

        // === RACE CHANGES ===
        Optional<String> raceType = Optional.empty();
        Optional<Boolean> raceAssigned = Optional.empty();

        if (race != null) {
            RaceType currentRaceType = race.getRaceType();
            boolean currentRaceAssigned = race.hasRaceAssigned();

            if (!currentRaceType.equals(syncState.lastRaceType)) {
                raceFieldMask |= UnifiedDeltaSyncPacket.RACE_TYPE;
                raceType = Optional.of(currentRaceType.name());
                hasChanges = true;
            }
            if (currentRaceAssigned != syncState.lastRaceAssigned) {
                raceFieldMask |= UnifiedDeltaSyncPacket.RACE_ASSIGNED;
                raceAssigned = Optional.of(currentRaceAssigned);
                hasChanges = true;
            }

            if (raceFieldMask != 0) {
                attachmentMask |= UnifiedDeltaSyncPacket.RACE_MASK;
            }
        }

        // === TALENT CHANGES ===
        Optional<String> spiritualRootType = Optional.empty();
        Optional<Boolean> spiritualRootAssigned = Optional.empty();
        Optional<String> physiqueType = Optional.empty();
        Optional<Boolean> physiqueAssigned = Optional.empty();

        if (talent != null) {
            SpiritualRootType currentSpiritualRootType = talent.getSpiritualRootType();
            boolean currentSpiritualRootAssigned = talent.hasSpiritualRootAssigned();
            PhysiqueType currentPhysiqueType = talent.getPhysiqueType();
            boolean currentPhysiqueAssigned = talent.hasPhysiqueAssigned();

            if (!currentSpiritualRootType.equals(syncState.lastSpiritualRootType)) {
                talentFieldMask |= UnifiedDeltaSyncPacket.TALENT_SPIRITUAL_ROOT_TYPE;
                spiritualRootType = Optional.of(currentSpiritualRootType.name());
                hasChanges = true;
            }
            if (currentSpiritualRootAssigned != syncState.lastSpiritualRootAssigned) {
                talentFieldMask |= UnifiedDeltaSyncPacket.TALENT_SPIRITUAL_ROOT_ASSIGNED;
                spiritualRootAssigned = Optional.of(currentSpiritualRootAssigned);
                hasChanges = true;
            }
            if (!currentPhysiqueType.equals(syncState.lastPhysiqueType)) {
                talentFieldMask |= UnifiedDeltaSyncPacket.TALENT_PHYSIQUE_TYPE;
                physiqueType = Optional.of(currentPhysiqueType.name());
                hasChanges = true;
            }
            if (currentPhysiqueAssigned != syncState.lastPhysiqueAssigned) {
                talentFieldMask |= UnifiedDeltaSyncPacket.TALENT_PHYSIQUE_ASSIGNED;
                physiqueAssigned = Optional.of(currentPhysiqueAssigned);
                hasChanges = true;
            }

            if (talentFieldMask != 0) {
                attachmentMask |= UnifiedDeltaSyncPacket.TALENT_MASK;
            }
        }

        // === PILL CONSUMPTION CHANGES ===
        Optional<CompoundTag> pillConsumptionData = Optional.empty();

        if (pillConsumption != null) {
            CompoundTag currentPillData = pillConsumption.getPillConsumptionManager().serializeNBT();

            // For pill consumption, we compare the serialized data hash since it's complex data
            int currentPillDataHash = currentPillData.hashCode();
            if (currentPillDataHash != syncState.lastPillConsumptionDataHash) {
                pillConsumptionFieldMask |= UnifiedDeltaSyncPacket.PILL_CONSUMPTION_DATA;
                pillConsumptionData = Optional.of(currentPillData);
                attachmentMask |= UnifiedDeltaSyncPacket.PILL_CONSUMPTION_MASK;
                hasChanges = true;
            }
        }

        // Build and return packet if there are changes
        if (hasChanges) {
            return new UnifiedDeltaSyncPacket(
                attachmentMask, cultivationFieldMask, resourceFieldMask, combatStatsFieldMask,
                raceFieldMask, talentFieldMask, pillConsumptionFieldMask,
                currentQiRealmId, currentBodyRealmId, currentQiCultivation, maxQiCultivation,
                currentBodyCultivation, maxBodyCultivation, currentFocus, bonuses, breakthroughData,
                currentHealth, currentQi, currentBody, permanentBonuses, combatStatsData,
                Optional.empty(), Optional.empty(), // damage feedback (not changed)
                raceType, raceAssigned,
                spiritualRootType, spiritualRootAssigned, physiqueType, physiqueAssigned,
                pillConsumptionData
            );
        }

        return null;
    }

    /**
     * Update sync state after successful sync to track changes for future delta detection.
     * Now includes the new attachment types.
     */
    private static void updateSyncState(PlayerSyncState syncState, PlayerCultivationAttachment cultivation,
                                        PlayerResourceAttachment resources, PlayerCombatStatsAttachment combatStats,
                                        PlayerRaceAttachment race, PlayerTalentAttachment talent,
                                        PlayerPillConsumptionAttachment pillConsumption) {
        if (cultivation != null) {
            syncState.lastQiRealmId = cultivation.getCurrentRealm(ICultivationData.CultivationType.QI).getId();
            syncState.lastBodyRealmId = cultivation.getCurrentRealm(ICultivationData.CultivationType.BODY).getId();
            syncState.lastCurrentQiCultivation = cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI);
            syncState.lastMaxQiCultivation = cultivation.getMaxCultivation(ICultivationData.CultivationType.QI);
            syncState.lastCurrentBodyCultivation = cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY);
            syncState.lastMaxBodyCultivation = cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY);
            syncState.lastCurrentFocus = cultivation.getCurrentFocus();
            syncState.lastQiAdditiveBonus = cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.QI);
            syncState.lastQiMultiplicativeBonus = cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.QI);
            syncState.lastBodyAdditiveBonus = cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.BODY);
            syncState.lastBodyMultiplicativeBonus = cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.BODY);

            // Update breakthrough data if available
            if (cultivation.getPlayerSafe() != null) {
                PlayerBreakthroughAttachment breakthrough = cultivation.getPlayerSafe().getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
                if (breakthrough != null) {
                    syncState.lastQiBreakthroughAttempts = breakthrough.getBreakthroughAttempts(ICultivationData.CultivationType.QI);
                    syncState.lastBodyBreakthroughAttempts = breakthrough.getBreakthroughAttempts(ICultivationData.CultivationType.BODY);
                    syncState.lastQiBreakthroughCooldown = breakthrough.isBreakthroughOnCooldown(ICultivationData.CultivationType.QI);
                    syncState.lastBodyBreakthroughCooldown = breakthrough.isBreakthroughOnCooldown(ICultivationData.CultivationType.BODY);
                    syncState.lastQiBreakthroughAttempt = breakthrough.getLastBreakthroughAttemptTime(ICultivationData.CultivationType.QI);
                    syncState.lastBodyBreakthroughAttempt = breakthrough.getLastBreakthroughAttemptTime(ICultivationData.CultivationType.BODY);
                }
            }

            // Reset change counters for processed attachments
            cultivation.resetChangeCounter();
        }

        if (resources != null) {
            syncState.lastCurrentHealth = resources.getCurrentHealth();
            syncState.lastCurrentQi = resources.getCurrentQi();
            syncState.lastCurrentBody = resources.getCurrentBody();
            syncState.lastPermanentHealthBonus = resources.getPermanentHealthBonus();
            syncState.lastPermanentHealthRegenBonus = resources.getPermanentHealthRegenBonus();
            syncState.lastPermanentMaxQiBonus = resources.getPermanentMaxQiBonus();
            syncState.lastPermanentQiRegenBonus = resources.getPermanentQiRegenBonus();
            syncState.lastPermanentMaxBodyBonus = resources.getPermanentMaxBodyBonus();
            syncState.lastPermanentBodyRegenBonus = resources.getPermanentBodyRegenBonus();
        }

        if (combatStats != null) {
            syncState.lastAttackBonus = combatStats.getAttackDamageBonus();
            syncState.lastSpeedBonus = combatStats.getMovementSpeedBonus();
            syncState.lastJumpBonus = combatStats.getJumpHeightBonus();
            syncState.lastPermanentAttackBonus = combatStats.getPermanentAttackDamageBonus();
            syncState.lastPermanentSpeedBonus = combatStats.getPermanentMovementSpeedBonus();
            syncState.lastPermanentJumpBonus = combatStats.getPermanentJumpHeightBonus();
        }

        // Update state for new attachment types
        if (race != null) {
            syncState.lastRaceType = race.getRaceType();
            syncState.lastRaceAssigned = race.hasRaceAssigned();
        }

        if (talent != null) {
            syncState.lastSpiritualRootType = talent.getSpiritualRootType();
            syncState.lastSpiritualRootAssigned = talent.hasSpiritualRootAssigned();
            syncState.lastPhysiqueType = talent.getPhysiqueType();
            syncState.lastPhysiqueAssigned = talent.hasPhysiqueAssigned();
        }

        if (pillConsumption != null) {
            CompoundTag pillData = pillConsumption.getPillConsumptionManager().serializeNBT();
            syncState.lastPillConsumptionDataHash = pillData.hashCode();
        }
    }

    /**
     * Update sync state from the latest packet data - used when processing batched syncs
     */
    private static void updateSyncStateFromLatestPacket(ServerPlayer player, PlayerSyncState syncState) {
        // Get all attachments and update sync state
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
        PlayerTalentAttachment talent = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
        PlayerPillConsumptionAttachment pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

        updateSyncState(syncState, cultivation, resources, combatStats, race, talent, pillConsumption);
    }

    // === UTILITY METHODS ===

    /**
     * Helper method to check if floating point values have changed
     */
    private static boolean hasChanged(double value1, double value2, double tolerance) {
        return Math.abs(value1 - value2) > tolerance;
    }

    /**
     * Send packet immediately with proper error handling
     */
    private static void sendImmediately(ServerPlayer player, UnifiedDeltaSyncPacket packet) {
        try {
            ICModPacketRegistry.sendToPlayer(packet, player);

            ImmortalCultivation.LOGGER.debug("Sent unified delta sync packet to player: {}",
                player.getName().getString());
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to send unified sync packet to player {}: {}",
                player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Add packet to pending batch
     */
    private static void addToPendingBatch(ServerPlayer player, UnifiedDeltaSyncPacket packet, SyncPriority priority) {
        UUID playerId = player.getUUID();
        Map<UUID, PendingSyncBatch> batches = pendingSyncs.get(priority);
        batches.computeIfAbsent(playerId, k -> new PendingSyncBatch()).addPacket(packet);
    }

    /**
     * Check if batch should be sent now
     */
    private static boolean shouldSendBatch(PendingSyncBatch batch, SyncPriority priority, long currentTime) {
        return batch.getChangeCount() >= priority.maxChanges ||
            (currentTime - batch.getFirstChangeTime()) >= priority.maxDelayMs;
    }

    /**
     * Send batch to player
     */
    private static void sendBatch(ServerPlayer player, PendingSyncBatch batch) {
        // For now, send the most recent packet in the batch
        // In a more advanced implementation, you could merge multiple packets
        UnifiedDeltaSyncPacket latestPacket = batch.getLatestPacket();
        if (latestPacket != null) {
            sendImmediately(player, latestPacket);
        }
    }

    /**
     * Get server player by UUID
     */
    private static ServerPlayer getServerPlayer(UUID playerId) {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()
            .getPlayerList()
            .getPlayer(playerId);
    }

    /**
     * Send unified delta sync immediately without batching
     */
    public static void sendUnifiedDeltaSync(ServerPlayer player, UnifiedDeltaSyncPacket packet) {
        try {
            ICModPacketRegistry.sendToPlayer(packet, player);
            ImmortalCultivation.LOGGER.debug("Sent unified delta sync packet to player: {}",
                player.getName().getString());
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to send unified sync packet to player {}: {}",
                player.getName().getString(), e.getMessage());
        }
    }

    // === SYNC STATE CLASSES ===

    /**
     * Tracks the last known state of all attachments for a player to enable delta detection.
     * Extended to include race, talent, and pill consumption data.
     */
    private static class PlayerSyncState {
        // Cultivation state
        String lastQiRealmId = "";
        String lastBodyRealmId = "";
        long lastCurrentQiCultivation = 0;
        long lastMaxQiCultivation = 0;
        long lastCurrentBodyCultivation = 0;
        long lastMaxBodyCultivation = 0;
        PlayerCultivationAttachment.CultivationType lastCurrentFocus = PlayerCultivationAttachment.CultivationType.QI;
        double lastQiAdditiveBonus = 0.0;
        double lastQiMultiplicativeBonus = 0.0;
        double lastBodyAdditiveBonus = 0.0;
        double lastBodyMultiplicativeBonus = 0.0;
        long lastQiBreakthroughAttempts = 0;
        long lastBodyBreakthroughAttempts = 0;
        boolean lastQiBreakthroughCooldown = false;
        boolean lastBodyBreakthroughCooldown = false;
        long lastQiBreakthroughAttempt = 0;
        long lastBodyBreakthroughAttempt = 0;

        // Resource state
        long lastCurrentHealth = 0;
        long lastCurrentQi = 0;
        long lastCurrentBody = 0;
        long lastPermanentHealthBonus = 0;
        double lastPermanentHealthRegenBonus = 0.0;
        long lastPermanentMaxQiBonus = 0;
        double lastPermanentQiRegenBonus = 0.0;
        long lastPermanentMaxBodyBonus = 0;
        double lastPermanentBodyRegenBonus = 0.0;

        // Combat stats state
        double lastAttackBonus = 0.0;
        double lastSpeedBonus = 0.0;
        double lastJumpBonus = 0.0;
        double lastPermanentAttackBonus = 0.0;
        double lastPermanentSpeedBonus = 0.0;
        double lastPermanentJumpBonus = 0.0;

        // Race state
        RaceType lastRaceType = RaceType.HUMAN;
        boolean lastRaceAssigned = false;

        // Talent state
        SpiritualRootType lastSpiritualRootType = SpiritualRootType.MORTAL_ROOT;
        boolean lastSpiritualRootAssigned = false;
        PhysiqueType lastPhysiqueType = PhysiqueType.MORTAL_PHYSIQUE;
        boolean lastPhysiqueAssigned = false;

        // Pill consumption state
        int lastPillConsumptionDataHash = 0;
    }

    /**
     * Represents a batch of pending sync operations
     */
    private static class PendingSyncBatch {
        private final List<UnifiedDeltaSyncPacket> packets = new ArrayList<>();
        private final long firstChangeTime = System.currentTimeMillis();

        public void addPacket(UnifiedDeltaSyncPacket packet) {
            packets.add(packet);
        }

        public int getChangeCount() {
            return packets.size();
        }

        public long getFirstChangeTime() {
            return firstChangeTime;
        }

        public UnifiedDeltaSyncPacket getLatestPacket() {
            return packets.isEmpty() ? null : packets.get(packets.size() - 1);
        }
    }

    /**
     * Ensure all attachments have proper player references
     * Updated to include race, talent, and pill consumption attachments
     */
    public static void ensurePlayerReferences(ServerPlayer player) {
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
        PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
        PlayerTalentAttachment talent = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
        PlayerPillConsumptionAttachment pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

        if (cultivation != null && cultivation.getPlayerSafe() == null) {
            cultivation.setPlayer(player);
        }
        if (resources != null && resources.getPlayerSafe() == null) {
            resources.setPlayer(player);
        }
        if (combatStats != null && combatStats.getPlayerSafe() == null) {
            combatStats.setPlayer(player);
        }
        if (breakthrough != null && breakthrough.getPlayerSafe() == null) {
            breakthrough.setPlayer(player);
        }
        if (race != null && race.getPlayerSafe() == null) {
            race.setPlayer(player);
        }
        if (talent != null && talent.getPlayerSafe() == null) {
            talent.setPlayer(player);
        }
        if (pillConsumption != null && pillConsumption.getPlayerSafe() == null) {
            pillConsumption.setPlayer(player);
        }
    }

    /**
     * Force sync all data for a player (e.g., on login)
     * Updated to include race, talent, and pill consumption attachments
     */
    public static void forceSyncAll(ServerPlayer player) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        // Prevent force sync spam (max once per 5 seconds)
        Long lastSync = lastFullSyncTime.get(playerId);
        if (lastSync != null && (currentTime - lastSync) < 5000L) {
            ImmortalCultivation.LOGGER.debug("Skipping force sync for {} - too recent",
                player.getName().getString());
            return;
        }

        lastFullSyncTime.put(playerId, currentTime);

        // Reset sync state to force full sync
        playerSyncStates.remove(playerId);
        PlayerSyncState syncState = new PlayerSyncState();
        playerSyncStates.put(playerId, syncState);

        // Ensure player references are set
        ensurePlayerReferences(player);

        // Get all attachments
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
        PlayerTalentAttachment talent = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
        PlayerPillConsumptionAttachment pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

        // Build full sync packet with all attachment types
        UnifiedDeltaSyncPacket packet = buildFullSyncPacket(cultivation, resources, combatStats, race, talent, pillConsumption);
        if (packet != null) {
            sendImmediately(player, packet);
            updateSyncState(syncState, cultivation, resources, combatStats, race, talent, pillConsumption);
        }
    }

    /**
     * Build full sync packet (for login) - Updated to include all attachment types
     */
    private static UnifiedDeltaSyncPacket buildFullSyncPacket(PlayerCultivationAttachment cultivation,
                                                              PlayerResourceAttachment resources,
                                                              PlayerCombatStatsAttachment combatStats,
                                                              PlayerRaceAttachment race,
                                                              PlayerTalentAttachment talent,
                                                              PlayerPillConsumptionAttachment pillConsumption) {

        byte attachmentMask = 0;
        short cultivationFieldMask = 0;
        short resourceFieldMask = 0;
        byte combatStatsFieldMask = 0;
        byte raceFieldMask = 0;
        byte talentFieldMask = 0;
        byte pillConsumptionFieldMask = 0;

        // Include all cultivation data
        if (cultivation != null) {
            attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
            cultivationFieldMask = UnifiedDeltaSyncPacket.CULT_QI_REALM | UnifiedDeltaSyncPacket.CULT_BODY_REALM |
                UnifiedDeltaSyncPacket.CULT_QI_CURRENT | UnifiedDeltaSyncPacket.CULT_QI_MAX |
                UnifiedDeltaSyncPacket.CULT_BODY_CURRENT | UnifiedDeltaSyncPacket.CULT_BODY_MAX |
                UnifiedDeltaSyncPacket.CULT_FOCUS | UnifiedDeltaSyncPacket.CULT_BONUSES |
                UnifiedDeltaSyncPacket.CULT_BREAKTHROUGH;
        }

        // Include all resource data
        if (resources != null) {
            attachmentMask |= UnifiedDeltaSyncPacket.RESOURCE_MASK;
            resourceFieldMask = UnifiedDeltaSyncPacket.RES_HEALTH | UnifiedDeltaSyncPacket.RES_QI |
                UnifiedDeltaSyncPacket.RES_BODY | UnifiedDeltaSyncPacket.RES_PERMANENT;
        }

        // Include all combat stats data
        if (combatStats != null) {
            attachmentMask |= UnifiedDeltaSyncPacket.COMBAT_STATS_MASK;
            combatStatsFieldMask = UnifiedDeltaSyncPacket.COMBAT_ALL;
        }

        // Include all race data
        if (race != null) {
            attachmentMask |= UnifiedDeltaSyncPacket.RACE_MASK;
            raceFieldMask = UnifiedDeltaSyncPacket.RACE_ALL;
        }

        // Include all talent data
        if (talent != null) {
            attachmentMask |= UnifiedDeltaSyncPacket.TALENT_MASK;
            talentFieldMask = UnifiedDeltaSyncPacket.TALENT_ALL;
        }

        // Include all pill consumption data
        if (pillConsumption != null) {
            attachmentMask |= UnifiedDeltaSyncPacket.PILL_CONSUMPTION_MASK;
            pillConsumptionFieldMask = UnifiedDeltaSyncPacket.PILL_CONSUMPTION_DATA;
        }

        // Get breakthrough data
        PlayerBreakthroughAttachment breakthrough = null;
        if (cultivation != null && cultivation.getPlayerSafe() != null) {
            breakthrough = cultivation.getPlayerSafe().getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
        }

        return new UnifiedDeltaSyncPacket(
            attachmentMask, cultivationFieldMask, resourceFieldMask, combatStatsFieldMask,
            raceFieldMask, talentFieldMask, pillConsumptionFieldMask,

            // Cultivation data
            cultivation != null ? Optional.of(cultivation.getCurrentRealm(ICultivationData.CultivationType.QI).getId()) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentRealm(ICultivationData.CultivationType.BODY).getId()) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI)) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getMaxCultivation(ICultivationData.CultivationType.QI)) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY)) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY)) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentFocus()) : Optional.empty(),
            cultivation != null ? Optional.of(new UnifiedDeltaSyncPacket.PackedBonuses(
                cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.QI),
                cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.QI),
                cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.BODY),
                cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.BODY)
            )) : Optional.empty(),
            breakthrough != null ? Optional.of(new UnifiedDeltaSyncPacket.PackedBreakthroughData(
                breakthrough.getBreakthroughAttempts(ICultivationData.CultivationType.QI),
                breakthrough.getBreakthroughAttempts(ICultivationData.CultivationType.BODY),
                breakthrough.isBreakthroughOnCooldown(ICultivationData.CultivationType.QI),
                breakthrough.isBreakthroughOnCooldown(ICultivationData.CultivationType.BODY),
                breakthrough.getLastBreakthroughAttemptTime(ICultivationData.CultivationType.QI),
                breakthrough.getLastBreakthroughAttemptTime(ICultivationData.CultivationType.BODY)
            )) : Optional.empty(),

            // Resource data
            resources != null ? Optional.of(resources.getCurrentHealth()) : Optional.empty(),
            resources != null ? Optional.of(resources.getCurrentQi()) : Optional.empty(),
            resources != null ? Optional.of(resources.getCurrentBody()) : Optional.empty(),
            resources != null ? Optional.of(new UnifiedDeltaSyncPacket.PackedPermanentBonuses(
                resources.getPermanentHealthBonus(),
                resources.getPermanentHealthRegenBonus(),
                resources.getPermanentMaxQiBonus(),
                resources.getPermanentQiRegenBonus(),
                resources.getPermanentMaxBodyBonus(),
                resources.getPermanentBodyRegenBonus()
            )) : Optional.empty(),

            // Combat stats data
            combatStats != null ? Optional.of(new UnifiedDeltaSyncPacket.PackedCombatStats(
                combatStats.getAttackDamageBonus(),
                combatStats.getMovementSpeedBonus(),
                combatStats.getJumpHeightBonus(),
                combatStats.getPermanentAttackDamageBonus(),
                combatStats.getPermanentMovementSpeedBonus(),
                combatStats.getPermanentJumpHeightBonus()
            )) : Optional.empty(),

            // Damage feedback data (empty for full sync)
            Optional.empty(), Optional.empty(),

            // Race data
            race != null ? Optional.of(race.getRaceType().name()) : Optional.empty(),
            race != null ? Optional.of(race.hasRaceAssigned()) : Optional.empty(),

            // Talent data
            talent != null ? Optional.of(talent.getSpiritualRootType().name()) : Optional.empty(),
            talent != null ? Optional.of(talent.hasSpiritualRootAssigned()) : Optional.empty(),
            talent != null ? Optional.of(talent.getPhysiqueType().name()) : Optional.empty(),
            talent != null ? Optional.of(talent.hasPhysiqueAssigned()) : Optional.empty(),

            // Pill consumption data
            pillConsumption != null ? Optional.of(pillConsumption.getPillConsumptionManager().serializeNBT()) : Optional.empty()
        );
    }
}