package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.interfaces.ICultivationData;
import net.hyper.immortalcultivation.network.server.UnifiedDeltaSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance network sync manager that handles delta syncing, batching,
 * and prioritization for all cultivation attachments.
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
     * Main sync method - analyzes all attachments and schedules appropriate syncs
     */
    public static void scheduleSync(ServerPlayer player, SyncPriority priority) {
        UUID playerId = player.getUUID();
        PlayerSyncState syncState = playerSyncStates.computeIfAbsent(playerId, k -> new PlayerSyncState());

        // Get current attachment states
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());

        // Detect changes and build delta packet
        UnifiedDeltaSyncPacket deltaPacket = buildDeltaPacket(player, syncState, cultivation, resources, combatStats);

        if (deltaPacket != null) {
            // Add to pending batch or send immediately based on priority
            if (priority == SyncPriority.CRITICAL) {
                sendImmediately(player, deltaPacket);
                updateSyncState(syncState, cultivation, resources, combatStats);
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

            batches.entrySet().removeIf(entry -> {
                UUID playerId = entry.getKey();
                PendingSyncBatch batch = entry.getValue();

                // Check if batch should be sent now
                if (shouldSendBatch(batch, priority, currentTime)) {
                    ServerPlayer player = getServerPlayer(playerId);
                    if (player != null) {
                        sendBatch(player, batch);
                        // Update sync state after sending batch
                        PlayerSyncState syncState = playerSyncStates.get(playerId);
                        if (syncState != null) {
                            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
                            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                            updateSyncState(syncState, cultivation, resources, combatStats);
                        }
                    }
                    return true; // Remove from pending
                }
                return false; // Keep in pending
            });
        }
    }

    /**
     * Force sync all data for a player (e.g., on login)
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

        // Build full sync packet
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());

        UnifiedDeltaSyncPacket packet = buildFullSyncPacket(cultivation, resources, combatStats);
        if (packet != null) {
            sendImmediately(player, packet);
            updateSyncState(syncState, cultivation, resources, combatStats);
        }
    }

    /**
     * Clean up sync state when player leaves
     */
    public static void cleanupPlayer(UUID playerId) {
        playerSyncStates.remove(playerId);
        lastFullSyncTime.remove(playerId);
        for (Map<UUID, PendingSyncBatch> batches : pendingSyncs.values()) {
            batches.remove(playerId);
        }
    }

    /**
     * Ensure all attachments have proper player references
     */
    public static void ensurePlayerReferences(ServerPlayer player) {
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());

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
    }

    /**
     * Emergency sync recovery for sync issues
     */
    public static void emergencySync(ServerPlayer player) {
        ImmortalCultivation.LOGGER.warn("Emergency sync triggered for player: {}",
            player.getName().getString());

        // Clear all sync state and force complete resync
        cleanupPlayer(player.getUUID());
        ensurePlayerReferences(player);
        forceSyncAll(player);
    }

    /**
     * Periodic cleanup - call this from tick handler occasionally
     */
    public static void performPeriodicCleanup() {
        long currentTime = System.currentTimeMillis();

        // Clean up old sync states (remove players offline for more than 1 hour)
        playerSyncStates.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            ServerPlayer player = getServerPlayer(playerId);
            return player == null; // Player is offline
        });

        // Clean up old full sync times
        lastFullSyncTime.entrySet().removeIf(entry ->
            (currentTime - entry.getValue()) > 3600000L); // 1 hour
    }

    // === PRIVATE HELPER METHODS ===

    /**
     * Build delta sync packet by comparing current state with cached state
     */
    private static UnifiedDeltaSyncPacket buildDeltaPacket(ServerPlayer player, PlayerSyncState syncState,
                                                           PlayerCultivationAttachment cultivation,
                                                           PlayerResourceAttachment resources,
                                                           PlayerCombatStatsAttachment combatStats) {

        byte attachmentMask = 0;
        short cultivationFieldMask = 0;
        short resourceFieldMask = 0;
        byte combatStatsFieldMask = 0;

        // Check cultivation changes
        Optional<String> currentQiRealmId = Optional.empty();
        Optional<String> currentBodyRealmId = Optional.empty();
        Optional<Long> currentQiCultivation = Optional.empty();
        Optional<Long> maxQiCultivation = Optional.empty();
        Optional<Long> currentBodyCultivation = Optional.empty();
        Optional<Long> maxBodyCultivation = Optional.empty();
        Optional<PlayerCultivationAttachment.CultivationType> currentFocus = Optional.empty();
        Optional<UnifiedDeltaSyncPacket.PackedBonuses> bonuses = Optional.empty();
        Optional<UnifiedDeltaSyncPacket.PackedBreakthroughData> breakthroughData = Optional.empty();

        if (cultivation != null && cultivation.isDirty()) {
            // Use generic realm access: getCurrentRealm(type).getId()
            String qiRealmId = cultivation.getCurrentRealm(ICultivationData.CultivationType.QI).getId();
            String bodyRealmId = cultivation.getCurrentRealm(ICultivationData.CultivationType.BODY).getId();

            if (!Objects.equals(qiRealmId, syncState.lastQiRealmId)) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_QI_REALM;
                currentQiRealmId = Optional.of(qiRealmId);
                syncState.lastQiRealmId = qiRealmId;
            }

            if (!Objects.equals(bodyRealmId, syncState.lastBodyRealmId)) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BODY_REALM;
                currentBodyRealmId = Optional.of(bodyRealmId);
                syncState.lastBodyRealmId = bodyRealmId;
            }

            // Use generic cultivation access methods from ICultivationData
            if (cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI) != syncState.lastCurrentQiCultivation) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_QI_CURRENT;
                currentQiCultivation = Optional.of(cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI));
                syncState.lastCurrentQiCultivation = cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI);
            }

            if (cultivation.getMaxCultivation(ICultivationData.CultivationType.QI) != syncState.lastMaxQiCultivation) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_QI_MAX;
                maxQiCultivation = Optional.of(cultivation.getMaxCultivation(ICultivationData.CultivationType.QI));
                syncState.lastMaxQiCultivation = cultivation.getMaxCultivation(ICultivationData.CultivationType.QI);
            }

            if (cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY) != syncState.lastCurrentBodyCultivation) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BODY_CURRENT;
                currentBodyCultivation = Optional.of(cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY));
                syncState.lastCurrentBodyCultivation = cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY);
            }

            if (cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY) != syncState.lastMaxBodyCultivation) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BODY_MAX;
                maxBodyCultivation = Optional.of(cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY));
                syncState.lastMaxBodyCultivation = cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY);
            }

            if (cultivation.getCurrentFocus() != syncState.lastCurrentFocus) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_FOCUS;
                currentFocus = Optional.of(cultivation.getCurrentFocus());
                syncState.lastCurrentFocus = cultivation.getCurrentFocus();
            }

            // Check bonuses (using correct method names with proper CultivationType)
            if (hasChanged(cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.QI), syncState.lastQiAdditiveBonus, 0.001) ||
                hasChanged(cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.QI), syncState.lastQiMultiplicativeBonus, 0.001) ||
                hasChanged(cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.BODY), syncState.lastBodyAdditiveBonus, 0.001) ||
                hasChanged(cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.BODY), syncState.lastBodyMultiplicativeBonus, 0.001)) {

                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BONUSES;
                bonuses = Optional.of(new UnifiedDeltaSyncPacket.PackedBonuses(
                    (float)cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.QI),
                    (float)cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.QI),
                    (float)cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.BODY),
                    (float)cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.BODY)
                ));

                syncState.lastQiAdditiveBonus = cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.QI);
                syncState.lastQiMultiplicativeBonus = cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.QI);
                syncState.lastBodyAdditiveBonus = cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.BODY);
                syncState.lastBodyMultiplicativeBonus = cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.BODY);
            }

            // Check breakthrough data (access through breakthrough attachment)
            PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
            if (breakthrough != null && breakthrough.isDirty()) {
                long qiAttempts = breakthrough.getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.QI);
                long bodyAttempts = breakthrough.getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.BODY);
                boolean qiCooldown = breakthrough.isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType.QI);
                boolean bodyCooldown = breakthrough.isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType.BODY);
                long lastQiAttempt = breakthrough.getTimeSinceLastBreakthroughAttempt(PlayerCultivationAttachment.CultivationType.QI);
                long lastBodyAttempt = breakthrough.getTimeSinceLastBreakthroughAttempt(PlayerCultivationAttachment.CultivationType.BODY);

                if (qiAttempts != syncState.lastQiBreakthroughAttempts ||
                    bodyAttempts != syncState.lastBodyBreakthroughAttempts ||
                    qiCooldown != syncState.lastQiBreakthroughCooldown ||
                    bodyCooldown != syncState.lastBodyBreakthroughCooldown ||
                    lastQiAttempt != syncState.lastQiBreakthroughAttempt ||
                    lastBodyAttempt != syncState.lastBodyBreakthroughAttempt) {

                    attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                    cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BREAKTHROUGH;
                    breakthroughData = Optional.of(UnifiedDeltaSyncPacket.PackedBreakthroughData.create(
                        qiAttempts, bodyAttempts, qiCooldown, bodyCooldown, lastQiAttempt, lastBodyAttempt
                    ));

                    syncState.lastQiBreakthroughAttempts = qiAttempts;
                    syncState.lastBodyBreakthroughAttempts = bodyAttempts;
                    syncState.lastQiBreakthroughCooldown = qiCooldown;
                    syncState.lastBodyBreakthroughCooldown = bodyCooldown;
                    syncState.lastQiBreakthroughAttempt = lastQiAttempt;
                    syncState.lastBodyBreakthroughAttempt = lastBodyAttempt;
                }
            }
        }

        // Check resource changes
        Optional<Long> currentHealth = Optional.empty();
        Optional<Long> currentQi = Optional.empty();
        Optional<Long> currentBody = Optional.empty();
        Optional<UnifiedDeltaSyncPacket.PackedPermanentBonuses> permanentBonuses = Optional.empty();

        if (resources != null && resources.isDirty()) {
            if (resources.getCurrentHealth() != syncState.lastCurrentHealth) {
                attachmentMask |= UnifiedDeltaSyncPacket.RESOURCE_MASK;
                resourceFieldMask |= UnifiedDeltaSyncPacket.RES_HEALTH;
                currentHealth = Optional.of(resources.getCurrentHealth());
                syncState.lastCurrentHealth = resources.getCurrentHealth();
            }

            if (resources.getCurrentQi() != syncState.lastCurrentQi) {
                attachmentMask |= UnifiedDeltaSyncPacket.RESOURCE_MASK;
                resourceFieldMask |= UnifiedDeltaSyncPacket.RES_QI;
                currentQi = Optional.of(resources.getCurrentQi());
                syncState.lastCurrentQi = resources.getCurrentQi();
            }

            if (resources.getCurrentBody() != syncState.lastCurrentBody) {
                attachmentMask |= UnifiedDeltaSyncPacket.RESOURCE_MASK;
                resourceFieldMask |= UnifiedDeltaSyncPacket.RES_BODY;
                currentBody = Optional.of(resources.getCurrentBody());
                syncState.lastCurrentBody = resources.getCurrentBody();
            }

            // Check permanent bonuses
            if (resources.getPermanentHealthBonus() != syncState.lastPermanentHealthBonus ||
                hasChanged(resources.getPermanentHealthRegenBonus(), syncState.lastPermanentHealthRegenBonus, 0.001) ||
                resources.getPermanentMaxQiBonus() != syncState.lastPermanentMaxQiBonus ||
                hasChanged(resources.getPermanentQiRegenBonus(), syncState.lastPermanentQiRegenBonus, 0.001) ||
                resources.getPermanentMaxBodyBonus() != syncState.lastPermanentMaxBodyBonus ||
                hasChanged(resources.getPermanentBodyRegenBonus(), syncState.lastPermanentBodyRegenBonus, 0.001)) {

                attachmentMask |= UnifiedDeltaSyncPacket.RESOURCE_MASK;
                resourceFieldMask |= UnifiedDeltaSyncPacket.RES_PERMANENT;
                permanentBonuses = Optional.of(new UnifiedDeltaSyncPacket.PackedPermanentBonuses(
                    resources.getPermanentHealthBonus(),
                    (float)resources.getPermanentHealthRegenBonus(),
                    resources.getPermanentMaxQiBonus(),
                    (float)resources.getPermanentQiRegenBonus(),
                    resources.getPermanentMaxBodyBonus(),
                    (float)resources.getPermanentBodyRegenBonus()
                ));

                syncState.lastPermanentHealthBonus = resources.getPermanentHealthBonus();
                syncState.lastPermanentHealthRegenBonus = resources.getPermanentHealthRegenBonus();
                syncState.lastPermanentMaxQiBonus = resources.getPermanentMaxQiBonus();
                syncState.lastPermanentQiRegenBonus = resources.getPermanentQiRegenBonus();
                syncState.lastPermanentMaxBodyBonus = resources.getPermanentMaxBodyBonus();
                syncState.lastPermanentBodyRegenBonus = resources.getPermanentBodyRegenBonus();
            }
        }

        // Check combat stats changes
        Optional<UnifiedDeltaSyncPacket.PackedCombatStats> combatStatsData = Optional.empty();

        if (combatStats != null && combatStats.isDirty()) {
            if (hasChanged(combatStats.getAttackDamageBonus(), syncState.lastAttackBonus, 0.001) ||
                hasChanged(combatStats.getMovementSpeedBonus(), syncState.lastSpeedBonus, 0.001) ||
                hasChanged(combatStats.getJumpHeightBonus(), syncState.lastJumpBonus, 0.001) ||
                hasChanged(combatStats.getPermanentAttackDamageBonus(), syncState.lastPermanentAttackBonus, 0.001) ||
                hasChanged(combatStats.getPermanentMovementSpeedBonus(), syncState.lastPermanentSpeedBonus, 0.001) ||
                hasChanged(combatStats.getPermanentJumpHeightBonus(), syncState.lastPermanentJumpBonus, 0.001)) {

                attachmentMask |= UnifiedDeltaSyncPacket.COMBAT_STATS_MASK;
                combatStatsFieldMask |= UnifiedDeltaSyncPacket.COMBAT_ALL;
                combatStatsData = Optional.of(new UnifiedDeltaSyncPacket.PackedCombatStats(
                    (float)combatStats.getAttackDamageBonus(),
                    (float)combatStats.getMovementSpeedBonus(),
                    (float)combatStats.getJumpHeightBonus(),
                    (float)combatStats.getPermanentAttackDamageBonus(),
                    (float)combatStats.getPermanentMovementSpeedBonus(),
                    (float)combatStats.getPermanentJumpHeightBonus()
                ));

                syncState.lastAttackBonus = combatStats.getAttackDamageBonus();
                syncState.lastSpeedBonus = combatStats.getMovementSpeedBonus();
                syncState.lastJumpBonus = combatStats.getJumpHeightBonus();
                syncState.lastPermanentAttackBonus = combatStats.getPermanentAttackDamageBonus();
                syncState.lastPermanentSpeedBonus = combatStats.getPermanentMovementSpeedBonus();
                syncState.lastPermanentJumpBonus = combatStats.getPermanentJumpHeightBonus();
            }
        }

        // Return packet only if there are changes
        if (attachmentMask != 0) {
            return new UnifiedDeltaSyncPacket(
                attachmentMask, cultivationFieldMask, resourceFieldMask, combatStatsFieldMask,
                currentQiRealmId, currentBodyRealmId, currentQiCultivation, maxQiCultivation,
                currentBodyCultivation, maxBodyCultivation, currentFocus, bonuses, breakthroughData,
                currentHealth, currentQi, currentBody, permanentBonuses, combatStatsData
            );
        }

        return null;
    }

    /**
     * Build full sync packet (for login)
     */
    private static UnifiedDeltaSyncPacket buildFullSyncPacket(PlayerCultivationAttachment cultivation,
                                                              PlayerResourceAttachment resources,
                                                              PlayerCombatStatsAttachment combatStats) {

        byte attachmentMask = 0;
        short cultivationFieldMask = 0;
        short resourceFieldMask = 0;
        byte combatStatsFieldMask = 0;

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

        // Get breakthrough data
        PlayerBreakthroughAttachment breakthrough = null;
        if (cultivation != null && cultivation.getPlayerSafe() != null) {
            breakthrough = cultivation.getPlayerSafe().getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
        }

        return new UnifiedDeltaSyncPacket(
            attachmentMask, cultivationFieldMask, resourceFieldMask, combatStatsFieldMask,
            cultivation != null ? Optional.of(cultivation.getCurrentRealm(ICultivationData.CultivationType.QI).getId()) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentRealm(ICultivationData.CultivationType.BODY).getId()) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI)) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getMaxCultivation(ICultivationData.CultivationType.QI)) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY)) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY)) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentFocus()) : Optional.empty(),
            cultivation != null ? Optional.of(new UnifiedDeltaSyncPacket.PackedBonuses(
                (float)cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.QI),
                (float)cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.QI),
                (float)cultivation.getRawAdditiveBonus(ICultivationData.CultivationType.BODY),
                (float)cultivation.getRawMultiplicativeBonus(ICultivationData.CultivationType.BODY)
            )) : Optional.empty(),
            breakthrough != null ? Optional.of(UnifiedDeltaSyncPacket.PackedBreakthroughData.create(
                breakthrough.getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.QI),
                breakthrough.getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.BODY),
                breakthrough.isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType.QI),
                breakthrough.isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType.BODY),
                breakthrough.getLastBreakthroughAttemptTime(PlayerCultivationAttachment.CultivationType.QI),
                breakthrough.getLastBreakthroughAttemptTime(PlayerCultivationAttachment.CultivationType.BODY)
            )) : Optional.empty(),
            resources != null ? Optional.of(resources.getCurrentHealth()) : Optional.empty(),
            resources != null ? Optional.of(resources.getCurrentQi()) : Optional.empty(),
            resources != null ? Optional.of(resources.getCurrentBody()) : Optional.empty(),
            resources != null ? Optional.of(new UnifiedDeltaSyncPacket.PackedPermanentBonuses(
                resources.getPermanentHealthBonus(),
                (float)resources.getPermanentHealthRegenBonus(),
                resources.getPermanentMaxQiBonus(),
                (float)resources.getPermanentQiRegenBonus(),
                resources.getPermanentMaxBodyBonus(),
                (float)resources.getPermanentBodyRegenBonus()
            )) : Optional.empty(),
            combatStats != null ? Optional.of(new UnifiedDeltaSyncPacket.PackedCombatStats(
                (float)combatStats.getAttackDamageBonus(),
                (float)combatStats.getMovementSpeedBonus(),
                (float)combatStats.getJumpHeightBonus(),
                (float)combatStats.getPermanentAttackDamageBonus(),
                (float)combatStats.getPermanentMovementSpeedBonus(),
                (float)combatStats.getPermanentJumpHeightBonus()
            )) : Optional.empty()
        );
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
            PacketDistributor.sendToPlayer(player, packet);

            // Clear dirty flags for all attachments after successful send
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());

            if (cultivation != null && cultivation.isDirty()) cultivation.clearDirty();
            if (resources != null && resources.isDirty()) resources.clearDirty();
            if (combatStats != null && combatStats.isDirty()) combatStats.clearDirty();
            if (breakthrough != null && breakthrough.isDirty()) breakthrough.clearDirty();

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
        return ServerLifecycleHooks.getCurrentServer()
            .getPlayerList()
            .getPlayer(playerId);
    }

    /**
     * Update sync state after successful sync
     */
    private static void updateSyncState(PlayerSyncState syncState, PlayerCultivationAttachment cultivation,
                                        PlayerResourceAttachment resources, PlayerCombatStatsAttachment combatStats) {
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

            resources.resetChangeCounter();
        }

        if (combatStats != null) {
            syncState.lastAttackBonus = combatStats.getAttackDamageBonus();
            syncState.lastSpeedBonus = combatStats.getMovementSpeedBonus();
            syncState.lastJumpBonus = combatStats.getJumpHeightBonus();
            syncState.lastPermanentAttackBonus = combatStats.getPermanentAttackDamageBonus();
            syncState.lastPermanentSpeedBonus = combatStats.getPermanentMovementSpeedBonus();
            syncState.lastPermanentJumpBonus = combatStats.getPermanentJumpHeightBonus();

            combatStats.resetChangeCounter();
        }
    }

    /**
     * Stores the last synced state for a player to enable delta detection
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
     * Send unified delta sync immediately without batching
     */
    public static void sendUnifiedDeltaSync(ServerPlayer player, UnifiedDeltaSyncPacket packet) {
        try {
            ICModNetwork.sendToPlayer(packet, player);
            ImmortalCultivation.LOGGER.debug("Sent unified delta sync packet to player: {}",
                player.getName().getString());
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to send unified sync packet to player {}: {}",
                player.getName().getString(), e.getMessage());
        }
    }
}