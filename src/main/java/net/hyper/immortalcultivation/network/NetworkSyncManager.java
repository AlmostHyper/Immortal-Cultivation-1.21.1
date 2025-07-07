package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.interfaces.ICultivationData;
import net.hyper.immortalcultivation.network.server.UnifiedDeltaSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.server.level.ServerPlayer;

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

        // Reset sync state to force full sync
        playerSyncStates.remove(playerId);
        PlayerSyncState syncState = new PlayerSyncState();
        playerSyncStates.put(playerId, syncState);

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
        for (Map<UUID, PendingSyncBatch> batches : pendingSyncs.values()) {
            batches.remove(playerId);
        }
    }

    // Helper method to build delta sync packet
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

        if (cultivation != null) {
            if (!Objects.equals(cultivation.getCurrentRealm(ICultivationData.CultivationType.QI), syncState.lastQiRealmId)) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_QI_REALM;
                currentQiRealmId = Optional.of(cultivation.getCurrentQiRealmId());
                syncState.lastQiRealmId = cultivation.getCurrentQiRealmId();
            }

            if (!Objects.equals(cultivation.getCurrentBodyRealmId(), syncState.lastBodyRealmId)) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BODY_REALM;
                currentBodyRealmId = Optional.of(cultivation.getCurrentBodyRealmId());
                syncState.lastBodyRealmId = cultivation.getCurrentBodyRealmId();
            }

            if (cultivation.getCurrentQiCultivation() != syncState.lastQiCultivation) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_QI_CURRENT;
                currentQiCultivation = Optional.of(cultivation.getCurrentQiCultivation());
                syncState.lastQiCultivation = cultivation.getCurrentQiCultivation();
            }

            if (cultivation.getMaxQiCultivation() != syncState.lastMaxQiCultivation) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_QI_MAX;
                maxQiCultivation = Optional.of(cultivation.getMaxQiCultivation());
                syncState.lastMaxQiCultivation = cultivation.getMaxQiCultivation();
            }

            if (cultivation.getCurrentBodyCultivation() != syncState.lastBodyCultivation) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BODY_CURRENT;
                currentBodyCultivation = Optional.of(cultivation.getCurrentBodyCultivation());
                syncState.lastBodyCultivation = cultivation.getCurrentBodyCultivation();
            }

            if (cultivation.getMaxBodyCultivation() != syncState.lastMaxBodyCultivation) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BODY_MAX;
                maxBodyCultivation = Optional.of(cultivation.getMaxBodyCultivation());
                syncState.lastMaxBodyCultivation = cultivation.getMaxBodyCultivation();
            }

            if (cultivation.getCurrentFocus() != syncState.lastFocus) {
                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_FOCUS;
                currentFocus = Optional.of(cultivation.getCurrentFocus());
                syncState.lastFocus = cultivation.getCurrentFocus();
            }

            // Check bonuses (compare with tolerance for floating point)
            if (hasChanged(cultivation.getQiAdditiveBonus(), syncState.lastQiAdditiveBonus, 0.001) ||
                hasChanged(cultivation.getQiMultiplicativeBonus(), syncState.lastQiMultiplicativeBonus, 0.001) ||
                hasChanged(cultivation.getBodyAdditiveBonus(), syncState.lastBodyAdditiveBonus, 0.001) ||
                hasChanged(cultivation.getBodyMultiplicativeBonus(), syncState.lastBodyMultiplicativeBonus, 0.001)) {

                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BONUSES;
                bonuses = Optional.of(new UnifiedDeltaSyncPacket.PackedBonuses(
                    (float)cultivation.getQiAdditiveBonus(),
                    (float)cultivation.getQiMultiplicativeBonus(),
                    (float)cultivation.getBodyAdditiveBonus(),
                    (float)cultivation.getBodyMultiplicativeBonus()
                ));

                syncState.lastQiAdditiveBonus = cultivation.getQiAdditiveBonus();
                syncState.lastQiMultiplicativeBonus = cultivation.getQiMultiplicativeBonus();
                syncState.lastBodyAdditiveBonus = cultivation.getBodyAdditiveBonus();
                syncState.lastBodyMultiplicativeBonus = cultivation.getBodyMultiplicativeBonus();
            }

            // Check breakthrough data
            if (cultivation.getQiBreakthroughAttempts() != syncState.lastQiBreakthroughAttempts ||
                cultivation.getBodyBreakthroughAttempts() != syncState.lastBodyBreakthroughAttempts ||
                cultivation.isQiBreakthroughCooldown() != syncState.lastQiBreakthroughCooldown ||
                cultivation.isBodyBreakthroughCooldown() != syncState.lastBodyBreakthroughCooldown ||
                cultivation.getLastQiBreakthroughAttempt() != syncState.lastQiBreakthroughAttempt ||
                cultivation.getLastBodyBreakthroughAttempt() != syncState.lastBodyBreakthroughAttempt) {

                attachmentMask |= UnifiedDeltaSyncPacket.CULTIVATION_MASK;
                cultivationFieldMask |= UnifiedDeltaSyncPacket.CULT_BREAKTHROUGH;
                breakthroughData = Optional.of(UnifiedDeltaSyncPacket.PackedBreakthroughData.create(
                    cultivation.getQiBreakthroughAttempts(),
                    cultivation.getBodyBreakthroughAttempts(),
                    cultivation.isQiBreakthroughCooldown(),
                    cultivation.isBodyBreakthroughCooldown(),
                    cultivation.getLastQiBreakthroughAttempt(),
                    cultivation.getLastBodyBreakthroughAttempt()
                ));

                syncState.lastQiBreakthroughAttempts = cultivation.getQiBreakthroughAttempts();
                syncState.lastBodyBreakthroughAttempts = cultivation.getBodyBreakthroughAttempts();
                syncState.lastQiBreakthroughCooldown = cultivation.isQiBreakthroughCooldown();
                syncState.lastBodyBreakthroughCooldown = cultivation.isBodyBreakthroughCooldown();
                syncState.lastQiBreakthroughAttempt = cultivation.getLastQiBreakthroughAttempt();
                syncState.lastBodyBreakthroughAttempt = cultivation.getLastBodyBreakthroughAttempt();
            }
        }

        // Check resource changes
        Optional<Long> currentHealth = Optional.empty();
        Optional<Long> currentQi = Optional.empty();
        Optional<Long> currentBody = Optional.empty();
        Optional<UnifiedDeltaSyncPacket.PackedPermanentBonuses> permanentBonuses = Optional.empty();

        if (resources != null) {
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

        if (combatStats != null) {
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

    // Helper method to build full sync packet (for login)
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

        return new UnifiedDeltaSyncPacket(
            attachmentMask, cultivationFieldMask, resourceFieldMask, combatStatsFieldMask,
            cultivation != null ? Optional.of(cultivation.getCurrentQiRealmId()) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentBodyRealmId()) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentQiCultivation()) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getMaxQiCultivation()) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentBodyCultivation()) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getMaxBodyCultivation()) : Optional.empty(),
            cultivation != null ? Optional.of(cultivation.getCurrentFocus()) : Optional.empty(),
            cultivation != null ? Optional.of(new UnifiedDeltaSyncPacket.PackedBonuses(
                (float)cultivation.getQiAdditiveBonus(),
                (float)cultivation.getQiMultiplicativeBonus(),
                (float)cultivation.getBodyAdditiveBonus(),
                (float)cultivation.getBodyMultiplicativeBonus()
            )) : Optional.empty(),
            cultivation != null ? Optional.of(UnifiedDeltaSyncPacket.PackedBreakthroughData.create(
                cultivation.getQiBreakthroughAttempts(),
                cultivation.getBodyBreakthroughAttempts(),
                cultivation.isQiBreakthroughCooldown(),
                cultivation.isBodyBreakthroughCooldown(),
                cultivation.getLastQiBreakthroughAttempt(),
                cultivation.getLastBodyBreakthroughAttempt()
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

    // Helper methods
    private static boolean hasChanged(double value1, double value2, double tolerance) {
        return Math.abs(value1 - value2) > tolerance;
    }

    private static void sendImmediately(ServerPlayer player, UnifiedDeltaSyncPacket packet) {
        ICModNetwork.sendToPlayer(packet, player);
    }

    private static void addToPendingBatch(ServerPlayer player, UnifiedDeltaSyncPacket packet, SyncPriority priority) {
        UUID playerId = player.getUUID();
        Map<UUID, PendingSyncBatch> batches = pendingSyncs.get(priority);

        batches.computeIfAbsent(playerId, k -> new PendingSyncBatch()).addPacket(packet);
    }

    private static boolean shouldSendBatch(PendingSyncBatch batch, SyncPriority priority, long currentTime) {
        return batch.getChangeCount() >= priority.maxChanges ||
            (currentTime - batch.getFirstChangeTime()) >= priority.maxDelayMs;
    }

    private static void sendBatch(ServerPlayer player, PendingSyncBatch batch) {
        // For now, send the most recent packet in the batch
        // In a more advanced implementation, you could merge multiple packets
        UnifiedDeltaSyncPacket latestPacket = batch.getLatestPacket();
        if (latestPacket != null) {
            ICModNetwork.sendToPlayer(latestPacket, player);
        }
    }

    private static ServerPlayer getServerPlayer(UUID playerId) {
        // Implementation depends on your server access pattern
        // This is a simplified version
        try {
            return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()
                .getPlayerList().getPlayer(playerId);
        } catch (Exception e) {
            return null;
        }
    }

    private static void updateSyncState(PlayerSyncState syncState, PlayerCultivationAttachment cultivation,
                                        PlayerResourceAttachment resources, PlayerCombatStatsAttachment combatStats) {
        if (cultivation != null) {
            syncState.lastQiRealmId = cultivation.getCurrentQiRealmId();
            syncState.lastBodyRealmId = cultivation.getCurrentBodyRealmId();
            syncState.lastQiCultivation = cultivation.getCurrentQiCultivation();
            syncState.lastMaxQiCultivation = cultivation.getMaxQiCultivation();
            syncState.lastBodyCultivation = cultivation.getCurrentBodyCultivation();
            syncState.lastMaxBodyCultivation = cultivation.getMaxBodyCultivation();
            syncState.lastFocus = cultivation.getCurrentFocus();
            syncState.lastQiAdditiveBonus = cultivation.getQiAdditiveBonus();
            syncState.lastQiMultiplicativeBonus = cultivation.getQiMultiplicativeBonus();
            syncState.lastBodyAdditiveBonus = cultivation.getBodyAdditiveBonus();
            syncState.lastBodyMultiplicativeBonus = cultivation.getBodyMultiplicativeBonus();
            syncState.lastQiBreakthroughAttempts = cultivation.getQiBreakthroughAttempts();
            syncState.lastBodyBreakthroughAttempts = cultivation.getBodyBreakthroughAttempts();
            syncState.lastQiBreakthroughCooldown = cultivation.isQiBreakthroughCooldown();
            syncState.lastBodyBreakthroughCooldown = cultivation.isBodyBreakthroughCooldown();
            syncState.lastQiBreakthroughAttempt = cultivation.getLastQiBreakthroughAttempt();
            syncState.lastBodyBreakthroughAttempt = cultivation.getLastBodyBreakthroughAttempt();
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
    }

    /**
     * Stores the last synced state for a player to enable delta detection
     */
    private static class PlayerSyncState {
        // Cultivation state
        String lastQiRealmId = "";
        String lastBodyRealmId = "";
        long lastQiCultivation = 0;
        long lastMaxQiCultivation = 0;
        long lastBodyCultivation = 0;
        long lastMaxBodyCultivation = 0;
        PlayerCultivationAttachment.CultivationType lastFocus = PlayerCultivationAttachment.CultivationType.QI;
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
}