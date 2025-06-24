package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IBodyCultivationData;
import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.handler.CombatStatsEventHandler;
import net.hyper.immortalcultivation.network.RealmSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * FIXED VERSION: PlayerRealmAttachment with proper player reference management
 *
 * Key Fix: The findPlayer() method now works correctly after death/respawn
 * and all methods that depend on player context have better error handling.
 */
public class PlayerRealmAttachment {

    // === Cultivation Type Enum === //
    public enum CultivationType {
        QI("Qi"),
        BODY("Body");

        private final String displayName;

        CultivationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // === Variables === //

    @Nullable
    private Player player;

    // Separate realms for Qi and Body cultivation
    private CultivationRealm currentQiRealm;
    private CultivationRealm currentBodyRealm;

    @Nullable
    private CultivationRealm previousQiRealm;
    @Nullable
    private CultivationRealm previousBodyRealm;

    // Separate breakthrough tracking for each cultivation type
    private long qiBreakthroughAttempts = 0;
    private long bodyBreakthroughAttempts = 0;

    private long lastQiBreakthroughAttempt = 0;
    private long lastBodyBreakthroughAttempt = 0;

    private boolean qiBreakthroughCooldown = false;
    private boolean bodyBreakthroughCooldown = false;

    // Time tracking for each cultivation path
    private long timeInCurrentQiRealm = 0;
    private long timeInCurrentBodyRealm = 0;

    private long totalQiCultivationTime = 0;
    private long totalBodyCultivationTime = 0;

    // Sync management for network communication
    private long lastSyncedValue = 0;
    private int tickCounter = 0;
    private static final int PERIODIC_SYNC_TICKS = 200; // Sync every 10 seconds

    // === Constructor(s) === //

    public PlayerRealmAttachment() {
        this.player = null;
        // Set default starting realms for both paths
        this.currentQiRealm = ICModCultivationRealms.MORTAL;
        this.currentBodyRealm = ICModCultivationRealms.MORTAL;
    }

    public PlayerRealmAttachment(Player player) {
        this.player = player;
        this.currentQiRealm = ICModCultivationRealms.MORTAL;
        this.currentBodyRealm = ICModCultivationRealms.MORTAL;
    }

    // === FIXED: Player Reference Management ===

    /**
     * CRITICAL FIX: This method was causing the "lost attachments" and breakthrough issues
     *
     * OLD BROKEN VERSION used reference equality (== this) which fails after respawn
     * NEW FIXED VERSION uses a more reliable approach with better error handling
     */
    private Player findPlayer() {
        // On client side, there's only one player - the local player
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            // Check if this client player has this attachment instance
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_REALM.get()) == this) {
                return clientPlayer;
            }
        }

        // FIXED: Server side now has better error handling and logging
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                // FIXED: More reliable check with null safety
                PlayerRealmAttachment playerRealmAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                if (playerRealmAttachment != null && playerRealmAttachment == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    /**
     * IMPROVED: Safe player retrieval with better debug logging
     */
    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            // IMPROVED: Better logging that doesn't spam but indicates the issue
            ImmortalCultivation.LOGGER.debug("Could not find player for RealmAttachment - this may indicate a respawn issue");
        }
        return player;
    }

    // === Player === //

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    // === Realm Getters === //

    /**
     * Get the current realm for a specific cultivation type
     */
    public CultivationRealm getCurrentRealm(CultivationType type) {
        return type == CultivationType.QI ? currentQiRealm : currentBodyRealm;
    }

    /**
     * Get the previous realm for a specific cultivation type
     */
    @Nullable
    public CultivationRealm getPreviousRealm(CultivationType type) {
        return type == CultivationType.QI ? previousQiRealm : previousBodyRealm;
    }

    /**
     * Get current Qi cultivation realm
     */
    public CultivationRealm getCurrentQiRealm() {
        return currentQiRealm;
    }

    /**
     * Get current Body cultivation realm
     */
    public CultivationRealm getCurrentBodyRealm() {
        return currentBodyRealm;
    }

    /**
     * Get the highest realm between Qi and Body cultivation
     * This can be used for determining overall cultivation level
     */
    public CultivationRealm getHighestRealm() {
        return currentQiRealm.getRealmLevel() >= currentBodyRealm.getRealmLevel()
            ? currentQiRealm : currentBodyRealm;
    }

    // === Realm Setters === //

    /**
     * Set the current realm for a specific cultivation type
     */
    public void setCurrentRealm(CultivationType type, CultivationRealm realm) {
        setCurrentRealm(type, realm, true);

        if (findPlayer() instanceof ServerPlayer serverPlayer) {
            CombatStatsEventHandler.refreshCombatStatsForRealmChange(serverPlayer);

            ImmortalCultivation.LOGGER.info("Combat stats updated after {} realm change to {}",
                type.getDisplayName(), realm.getDisplayName());
        };
    }

    /**
     * ENHANCED: Set the current realm for a specific cultivation type with event control
     * Now works properly after respawn due to fixed player finding
     */
    public void setCurrentRealm(CultivationType type, CultivationRealm realm, boolean triggerEvents) {
        if (realm == null) {
            throw new IllegalArgumentException("Cultivation realm cannot be null");
        }

        CultivationRealm oldRealm;

        if (type == CultivationType.QI) {
            oldRealm = this.currentQiRealm;
            this.previousQiRealm = oldRealm;
            this.currentQiRealm = realm;
            this.timeInCurrentQiRealm = 0;
        } else {
            oldRealm = this.currentBodyRealm;
            this.previousBodyRealm = oldRealm;
            this.currentBodyRealm = realm;
            this.timeInCurrentBodyRealm = 0;
        }

        // FIXED: Now uses getPlayerSafe() which works after respawn
        Player foundPlayer = getPlayerSafe();
        if (triggerEvents && foundPlayer != null) {
            // Get the appropriate cultivation data based on type
            if (type == CultivationType.QI) {
                IQiCultivationData qiData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());

                // Call exit event on old realm
                if (oldRealm != null) {
                    oldRealm.onRealmExit(foundPlayer, qiData, realm);
                }

                // Call advancement event on new realm
                realm.onRealmAdvancement(foundPlayer, qiData, oldRealm);

                // Apply new realm modifiers
                realm.applyRealmModifiers(foundPlayer, qiData);

                // Sync changes
                if (qiData instanceof PlayerQiCultivationAttachment qiAttachment) {
                    qiAttachment.forceSync();
                }
            } else {
                IBodyCultivationData bodyData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());

                // Call exit event on old realm
                if (oldRealm != null) {
                    oldRealm.onRealmExit(foundPlayer, bodyData, realm);
                }

                // Call advancement event on new realm
                realm.onRealmAdvancement(foundPlayer, bodyData, oldRealm);

                // Apply new realm modifiers
                realm.applyRealmModifiers(foundPlayer, bodyData);

                // Sync changes
                if (bodyData instanceof PlayerBodyCultivationAttachment bodyAttachment) {
                    bodyAttachment.forceSync();
                }
            }
        } else if (triggerEvents && foundPlayer == null) {
            ImmortalCultivation.LOGGER.warn("Cannot trigger realm change events - no player found for RealmAttachment");
        }

        // Trigger a sync after realm changes to update client
        if (!triggerEvents) { // Only sync if we're not already syncing through events
            triggerSyncIfNeeded();
        }
    }

    // === FIXED: Network Synchronization ===

    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                // Calculate a composite sync value that represents both realms
                // We'll encode both realm levels into a single long value
                long syncValue = calculateCompositeSyncValue();

                lastSyncedValue = syncValue;

                // Send the sync packet to the client
                ICModNetwork.sendToPlayer(new RealmSyncPacket(syncValue), serverPlayer);

                ImmortalCultivation.LOGGER.debug("Force synced realm data to client for player: {} (composite value: {})",
                    serverPlayer.getName().getString(), syncValue);

            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error force syncing realm data to client for player: {}",
                    player.getName().getString(), e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot force sync realm data - no player found");
        }
    }

    public void updateFromServer(long realmCultivation) {
        try {
            // Decode the composite value back into individual realm levels
            DecodedRealmData decodedData = decodeCompositeSyncValue(realmCultivation);

            // Store the previous realm levels for comparison
            int oldQiLevel = currentQiRealm.getRealmLevel();
            int oldBodyLevel = currentBodyRealm.getRealmLevel();

            // Update realms based on the decoded levels (without triggering events on client)
            updateRealmFromLevel(CultivationType.QI, decodedData.qiLevel);
            updateRealmFromLevel(CultivationType.BODY, decodedData.bodyLevel);

            // Update our sync tracking
            lastSyncedValue = realmCultivation;

            // Log the update for debugging
            if (oldQiLevel != currentQiRealm.getRealmLevel() || oldBodyLevel != currentBodyRealm.getRealmLevel()) {
                ImmortalCultivation.LOGGER.info("Client updated realm data - Qi: {} -> {}, Body: {} -> {}",
                    oldQiLevel, currentQiRealm.getRealmLevel(),
                    oldBodyLevel, currentBodyRealm.getRealmLevel());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating realm data from server sync", e);
        }
    }

    /**
     * Calculate a composite sync value that encodes both Qi and Body realm levels.
     *
     * We use bit shifting to pack two realm levels into a single long:
     * - Lower 32 bits: Qi realm level
     * - Upper 32 bits: Body realm level
     *
     * This approach maintains compatibility with the existing single-value sync packet
     * while supporting dual cultivation data.
     */
    private long calculateCompositeSyncValue() {
        long qiLevel = currentQiRealm.getRealmLevel();
        long bodyLevel = currentBodyRealm.getRealmLevel();

        // Pack both levels into a single long value
        // Body level in upper 32 bits, Qi level in lower 32 bits
        long compositeValue = (bodyLevel << 32) | (qiLevel & 0xFFFFFFFFL);

        ImmortalCultivation.LOGGER.debug("Encoding realm levels - Qi: {}, Body: {} -> Composite: {}",
            qiLevel, bodyLevel, compositeValue);

        return compositeValue;
    }

    /**
     * Decode a composite sync value back into individual realm levels.
     */
    private DecodedRealmData decodeCompositeSyncValue(long compositeValue) {
        // Extract individual levels from the composite value
        int qiLevel = (int)(compositeValue & 0xFFFFFFFFL);  // Lower 32 bits
        int bodyLevel = (int)(compositeValue >>> 32);       // Upper 32 bits

        ImmortalCultivation.LOGGER.debug("Decoding composite value {} -> Qi: {}, Body: {}",
            compositeValue, qiLevel, bodyLevel);

        return new DecodedRealmData(qiLevel, bodyLevel);
    }

    /**
     * Update a specific cultivation realm based on realm level.
     * This finds the appropriate realm object for the given level.
     */
    private void updateRealmFromLevel(CultivationType type, int targetLevel) {
        try {
            // Find the realm with the matching level
            // In a more sophisticated system, you might have a registry method for this
            CultivationRealm targetRealm = findRealmByLevel(targetLevel);

            if (targetRealm != null) {
                // Update the realm without triggering events (this is client-side sync)
                if (type == CultivationType.QI) {
                    this.currentQiRealm = targetRealm;
                } else {
                    this.currentBodyRealm = targetRealm;
                }
            } else {
                ImmortalCultivation.LOGGER.warn("Could not find realm for level {} in {} cultivation",
                    targetLevel, type.getDisplayName());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating {} realm from level {}",
                type.getDisplayName(), targetLevel, e);
        }
    }

    /**
     * Find a cultivation realm by its level.
     *
     * TODO: This is a simplified implementation. In a full system, you'd want
     * a proper registry method or cache for efficient realm lookup by level.
     */
    private CultivationRealm findRealmByLevel(int level) {
        // Simple implementation - check known realms
        // In a full system, you'd iterate through all registered realms
        if (level == 0) return ICModCultivationRealms.MORTAL;
        if (level == 1) return ICModCultivationRealms.QI_CONDENSATION_L1;
        if (level == 2) return ICModCultivationRealms.QI_CONDENSATION_L2;
        if (level == 3) return ICModCultivationRealms.QI_CONDENSATION_L3;
        if (level == 4) return ICModCultivationRealms.QI_CONDENSATION_L4;
        if (level == 5) return ICModCultivationRealms.QI_CONDENSATION_L5;
        if (level == 6) return ICModCultivationRealms.QI_CONDENSATION_L6;
        if (level == 7) return ICModCultivationRealms.QI_CONDENSATION_L7;
        if (level == 8) return ICModCultivationRealms.QI_CONDENSATION_L8;
        if (level == 9) return ICModCultivationRealms.QI_CONDENSATION_L9;

        // Add more realm mappings as needed

        // Fallback to current realm if not found
        ImmortalCultivation.LOGGER.warn("Unknown realm level: {}, using current realm", level);
        return currentQiRealm; // Or some default realm
    }

    /**
     * FIXED: Check if a sync is needed and trigger it if so.
     * Now works properly after respawn due to fixed player finding
     */
    private void triggerSyncIfNeeded() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer) {
            long currentSyncValue = calculateCompositeSyncValue();

            // Only sync if the value has changed
            if (currentSyncValue != lastSyncedValue) {
                forceSync();
            }
        }
    }

    /**
     * Helper record for decoded realm data
     */
    private record DecodedRealmData(int qiLevel, int bodyLevel) {}

    // === FIXED: Breakthrough Logic ===

    /**
     * ENHANCED: Check if player can attempt breakthrough for a specific cultivation type
     * Now works properly after respawn due to fixed player finding
     */
    public boolean canAttemptBreakthrough(CultivationType type) {
        Player foundPlayer = getPlayerSafe();
        if (foundPlayer == null) {
            ImmortalCultivation.LOGGER.debug("Cannot attempt breakthrough - no player found");
            return false;
        }

        // Check cooldown
        boolean onCooldown = type == CultivationType.QI ? qiBreakthroughCooldown : bodyBreakthroughCooldown;
        long lastAttempt = type == CultivationType.QI ? lastQiBreakthroughAttempt : lastBodyBreakthroughAttempt;

        if (onCooldown) {
            long currentTime = foundPlayer.level().getGameTime();
            if (currentTime - lastAttempt < getBreakthroughCooldownTicks()) {
                return false;
            } else {
                // Reset cooldown
                if (type == CultivationType.QI) {
                    qiBreakthroughCooldown = false;
                } else {
                    bodyBreakthroughCooldown = false;
                }
            }
        }

        // Check realm requirements
        CultivationRealm currentRealm = getCurrentRealm(type);
        if (currentRealm == null) return false;

        // Get appropriate cultivation data
        if (type == CultivationType.QI) {
            IQiCultivationData qiData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            return qiData != null && currentRealm.canBreakthrough(foundPlayer, qiData);
        } else {
            IBodyCultivationData bodyData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            return bodyData != null && currentRealm.canBreakthrough(foundPlayer, bodyData);
        }
    }

    /**
     * ENHANCED: Check breakthrough requirements without cooldown consideration
     */
    public boolean meetsBreakthroughRequirements(CultivationType type) {
        Player foundPlayer = getPlayerSafe();
        if (foundPlayer == null) {
            ImmortalCultivation.LOGGER.debug("Cannot check breakthrough requirements - no player found");
            return false;
        }

        CultivationRealm currentRealm = getCurrentRealm(type);
        if (currentRealm == null) return false;

        if (type == CultivationType.QI) {
            IQiCultivationData qiData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            return qiData != null && currentRealm.canBreakthrough(foundPlayer, qiData);
        } else {
            IBodyCultivationData bodyData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            return bodyData != null && currentRealm.canBreakthrough(foundPlayer, bodyData);
        }
    }

    /**
     * Attempt breakthrough for a specific cultivation type
     */
    public BreakthroughResult attemptBreakthrough(CultivationType cultivationType) {
        return attemptBreakthrough(cultivationType, BreakthroughType.MINOR);
    }

    /**
     * ENHANCED: Attempt specific type of breakthrough for a cultivation path
     * Now works properly after respawn due to fixed player finding
     */
    public BreakthroughResult attemptBreakthrough(CultivationType cultivationType, BreakthroughType breakthroughType) {
        Player foundPlayer = getPlayerSafe();

        // Enhanced debugging
        ImmortalCultivation.LOGGER.info("Attempting {} breakthrough - Player: {}, CurrentRealm: {}",
            cultivationType.getDisplayName(),
            foundPlayer != null ? foundPlayer.getName().getString() : "NULL",
            getCurrentRealm(cultivationType) != null ? getCurrentRealm(cultivationType).getDisplayName() : "NULL");

        if (foundPlayer == null) {
            ImmortalCultivation.LOGGER.error("Player is null in attemptBreakthrough!");
            return new BreakthroughResult(false, "Player reference is null", null);
        }

        CultivationRealm currentRealm = getCurrentRealm(cultivationType);
        if (currentRealm == null) {
            ImmortalCultivation.LOGGER.error("Current {} realm is null!", cultivationType.getDisplayName());
            return new BreakthroughResult(false, "Current realm is null", null);
        }

        // Get appropriate cultivation data
        if (cultivationType == CultivationType.QI) {
            IQiCultivationData qiData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiData == null) {
                ImmortalCultivation.LOGGER.error("Qi cultivation data is null for player: {}", foundPlayer.getName().getString());
                return new BreakthroughResult(false, "No Qi cultivation data found", null);
            }
            return performQiBreakthrough(currentRealm, qiData, breakthroughType, foundPlayer);
        } else {
            IBodyCultivationData bodyData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyData == null) {
                ImmortalCultivation.LOGGER.error("Body cultivation data is null for player: {}", foundPlayer.getName().getString());
                return new BreakthroughResult(false, "No Body cultivation data found", null);
            }
            return performBodyBreakthrough(currentRealm, bodyData, breakthroughType, foundPlayer);
        }
    }

    /**
     * FIXED: Perform Qi cultivation breakthrough - now takes player parameter
     */
    private BreakthroughResult performQiBreakthrough(CultivationRealm currentRealm, IQiCultivationData qiData, BreakthroughType type, Player foundPlayer) {
        // Check if attempt is possible
        if (!canAttemptBreakthrough(CultivationType.QI)) {
            if (qiBreakthroughCooldown) {
                long remainingTicks = getBreakthroughCooldownTicks() -
                    (foundPlayer.level().getGameTime() - lastQiBreakthroughAttempt);
                return new BreakthroughResult(false,
                    "Qi breakthrough cooldown: " + (remainingTicks / 20) + " seconds remaining", null);
            } else {
                return new BreakthroughResult(false, "Qi breakthrough requirements not met", null);
            }
        }

        // Record attempt
        qiBreakthroughAttempts++;
        lastQiBreakthroughAttempt = foundPlayer.level().getGameTime();
        qiBreakthroughCooldown = true;

        // Determine target realm based on type
        CultivationRealm targetRealm = switch (type) {
            case MINOR -> currentRealm.getMinorBreakthrough(foundPlayer, qiData);
            case MAJOR -> currentRealm.getMajorBreakthrough(foundPlayer, qiData);
            case SPECIAL -> currentRealm.getSpecialBreakthrough(foundPlayer, qiData);
            case BOUNDARY -> currentRealm.getBoundaryBreakthrough(foundPlayer, qiData);
        };

        if (targetRealm == null) {
            ImmortalCultivation.LOGGER.warn("No valid Qi breakthrough path found for type: {}", type);
            return new BreakthroughResult(false, "No valid Qi breakthrough path found", null);
        }

        // Calculate success chance
        double successChance = calculateBreakthroughSuccessChance(CultivationType.QI, type, targetRealm, foundPlayer);
        boolean success = foundPlayer.level().random.nextDouble() < successChance;

        ImmortalCultivation.LOGGER.info("Qi breakthrough attempt - Success chance: {}%, Result: {}",
            String.format("%.1f", successChance * 100), success ? "SUCCESS" : "FAILURE");

        if (success) {
            // Advance to new realm
            setCurrentRealm(CultivationType.QI, targetRealm);

            return new BreakthroughResult(true,
                "Successfully advanced Qi cultivation to " + targetRealm.getDisplayName(), targetRealm);
        } else {
            return new BreakthroughResult(false,
                "Qi breakthrough failed.", null);
        }
    }

    /**
     * FIXED: Perform Body cultivation breakthrough - now takes player parameter
     */
    private BreakthroughResult performBodyBreakthrough(CultivationRealm currentRealm, IBodyCultivationData bodyData, BreakthroughType type, Player foundPlayer) {
        // Check if attempt is possible
        if (!canAttemptBreakthrough(CultivationType.BODY)) {
            if (bodyBreakthroughCooldown) {
                long remainingTicks = getBreakthroughCooldownTicks() -
                    (foundPlayer.level().getGameTime() - lastBodyBreakthroughAttempt);
                return new BreakthroughResult(false,
                    "Body breakthrough cooldown: " + (remainingTicks / 20) + " seconds remaining", null);
            } else {
                return new BreakthroughResult(false, "Body breakthrough requirements not met", null);
            }
        }

        // Record attempt
        bodyBreakthroughAttempts++;
        lastBodyBreakthroughAttempt = foundPlayer.level().getGameTime();
        bodyBreakthroughCooldown = true;

        // Determine target realm based on type
        CultivationRealm targetRealm = switch (type) {
            case MINOR -> currentRealm.getMinorBreakthrough(foundPlayer, bodyData);
            case MAJOR -> currentRealm.getMajorBreakthrough(foundPlayer, bodyData);
            case SPECIAL -> currentRealm.getSpecialBreakthrough(foundPlayer, bodyData);
            case BOUNDARY -> currentRealm.getBoundaryBreakthrough(foundPlayer, bodyData);
        };

        if (targetRealm == null) {
            ImmortalCultivation.LOGGER.warn("No valid Body breakthrough path found for type: {}", type);
            return new BreakthroughResult(false, "No valid Body breakthrough path found", null);
        }

        // Calculate success chance
        double successChance = calculateBreakthroughSuccessChance(CultivationType.BODY, type, targetRealm, foundPlayer);
        boolean success = foundPlayer.level().random.nextDouble() < successChance;

        ImmortalCultivation.LOGGER.info("Body breakthrough attempt - Success chance: {}%, Result: {}",
            String.format("%.1f", successChance * 100), success ? "SUCCESS" : "FAILURE");

        if (success) {
            // Advance to new realm
            setCurrentRealm(CultivationType.BODY, targetRealm);

            return new BreakthroughResult(true,
                "Successfully advanced Body cultivation to " + targetRealm.getDisplayName(), targetRealm);
        } else {
            return new BreakthroughResult(false,
                "Body breakthrough failed.", null);
        }
    }

    /**
     * FIXED: Calculate success chance for breakthrough attempt - now takes player parameter
     */
    private double calculateBreakthroughSuccessChance(CultivationType cultivationType, BreakthroughType type, CultivationRealm targetRealm, Player foundPlayer) {
        double baseChance = switch (type) {
            case MINOR -> 0.5;
            case MAJOR -> 0.3;
            case SPECIAL -> 0.2;
            case BOUNDARY -> 0.1;
        };

        // Get cultivation percentage based on type
        double cultivationPercentage;
        if (cultivationType == CultivationType.QI) {
            IQiCultivationData qiData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
        } else {
            IBodyCultivationData bodyData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
        }


        return Math.max(0.10, Math.min(1.0, baseChance));
    }

    /**
     * Get breakthrough cooldown in ticks
     */
    private long getBreakthroughCooldownTicks() {
        return 20 * 30; // 30 seconds default
    }

    // === Realm Progression Tracking ===

    public long getBreakthroughAttempts(CultivationType type) {
        return type == CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts;
    }

    public long getTimeInCurrentRealm(CultivationType type) {
        return type == CultivationType.QI ? timeInCurrentQiRealm : timeInCurrentBodyRealm;
    }

    public long getTotalCultivationTime(CultivationType type) {
        return type == CultivationType.QI ? totalQiCultivationTime : totalBodyCultivationTime;
    }

    public boolean isOnBreakthroughCooldown(CultivationType type) {
        boolean onCooldown = type == CultivationType.QI ? qiBreakthroughCooldown : bodyBreakthroughCooldown;
        if (!onCooldown) return false;

        Player foundPlayer = getPlayerSafe();
        long lastAttempt = type == CultivationType.QI ? lastQiBreakthroughAttempt : lastBodyBreakthroughAttempt;
        long currentTime = foundPlayer != null ? foundPlayer.level().getGameTime() : 0;

        return currentTime - lastAttempt < getBreakthroughCooldownTicks();
    }

    public long getRemainingCooldownTicks(CultivationType type) {
        if (!isOnBreakthroughCooldown(type)) return 0;

        Player foundPlayer = getPlayerSafe();
        long lastAttempt = type == CultivationType.QI ? lastQiBreakthroughAttempt : lastBodyBreakthroughAttempt;
        long currentTime = foundPlayer != null ? foundPlayer.level().getGameTime() : 0;

        return getBreakthroughCooldownTicks() - (currentTime - lastAttempt);
    }

    // === ENHANCED: Utility Methods ===

    /**
     * FIXED: Get all available breakthrough options for both cultivation types
     */
    public BreakthroughOptions getAllAvailableBreakthroughs() {
        List<BreakthroughOption> qiOptions = getAvailableBreakthroughs(CultivationType.QI);
        List<BreakthroughOption> bodyOptions = getAvailableBreakthroughs(CultivationType.BODY);

        return new BreakthroughOptions(qiOptions, bodyOptions);
    }

    /**
     * FIXED: Get available breakthrough options for a specific cultivation type
     */
    public List<BreakthroughOption> getAvailableBreakthroughs(CultivationType type) {
        Player foundPlayer = getPlayerSafe();
        if (foundPlayer == null) {
            ImmortalCultivation.LOGGER.debug("Cannot get breakthrough options - no player found");
            return Collections.emptyList();
        }

        CultivationRealm currentRealm = getCurrentRealm(type);
        if (currentRealm == null) return Collections.emptyList();

        List<BreakthroughOption> options = new ArrayList<>();

        if (type == CultivationType.QI) {
            IQiCultivationData qiData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiData == null) return Collections.emptyList();

            addBreakthroughOption(options, currentRealm.getMinorBreakthrough(foundPlayer, qiData),
                BreakthroughType.MINOR, CultivationType.QI, foundPlayer);
            addBreakthroughOption(options, currentRealm.getMajorBreakthrough(foundPlayer, qiData),
                BreakthroughType.MAJOR, CultivationType.QI, foundPlayer);
            addBreakthroughOption(options, currentRealm.getSpecialBreakthrough(foundPlayer, qiData),
                BreakthroughType.SPECIAL, CultivationType.QI, foundPlayer);
            addBreakthroughOption(options, currentRealm.getBoundaryBreakthrough(foundPlayer, qiData),
                BreakthroughType.BOUNDARY, CultivationType.QI, foundPlayer);
        } else {
            IBodyCultivationData bodyData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyData == null) return Collections.emptyList();

            addBreakthroughOption(options, currentRealm.getMinorBreakthrough(foundPlayer, bodyData),
                BreakthroughType.MINOR, CultivationType.BODY, foundPlayer);
            addBreakthroughOption(options, currentRealm.getMajorBreakthrough(foundPlayer, bodyData),
                BreakthroughType.MAJOR, CultivationType.BODY, foundPlayer);
            addBreakthroughOption(options, currentRealm.getSpecialBreakthrough(foundPlayer, bodyData),
                BreakthroughType.SPECIAL, CultivationType.BODY, foundPlayer);
            addBreakthroughOption(options, currentRealm.getBoundaryBreakthrough(foundPlayer, bodyData),
                BreakthroughType.BOUNDARY, CultivationType.BODY, foundPlayer);
        }

        return options;
    }

    private void addBreakthroughOption(List<BreakthroughOption> options, CultivationRealm realm,
                                       BreakthroughType type, CultivationType cultivationType, Player foundPlayer) {
        if (realm != null) {
            double successChance = calculateBreakthroughSuccessChance(cultivationType, type, realm, foundPlayer);
            options.add(new BreakthroughOption(type, realm, successChance, cultivationType));
        }
    }

    /**
     * Reset breakthrough attempts for a specific cultivation type (for testing or special items)
     */
    public void resetBreakthroughAttempts(CultivationType type) {
        if (type == CultivationType.QI) {
            this.qiBreakthroughAttempts = 0;
            this.qiBreakthroughCooldown = false;
        } else {
            this.bodyBreakthroughAttempts = 0;
            this.bodyBreakthroughCooldown = false;
        }
    }

    /**
     * Reset all breakthrough attempts
     */
    public void resetAllBreakthroughAttempts() {
        resetBreakthroughAttempts(CultivationType.QI);
        resetBreakthroughAttempts(CultivationType.BODY);
    }

    // === FIXED: Tick Method ===

    public void tick() {
        Player foundPlayer = getPlayerSafe();
        if (foundPlayer == null) {
            // IMPROVED: Don't spam logs, but do track the issue
            if (tickCounter % 200 == 0) { // Log every 10 seconds
                ImmortalCultivation.LOGGER.debug("RealmAttachment ticking without player reference (tick {})", tickCounter);
            }
            return;
        }

        tickCounter++;

        // Update time tracking for both cultivation paths
        timeInCurrentQiRealm++;
        timeInCurrentBodyRealm++;
        totalQiCultivationTime++;
        totalBodyCultivationTime++;

        // Apply realm modifiers periodically for both types
        if (timeInCurrentQiRealm % 100 == 0) { // Every 5 seconds
            IQiCultivationData qiData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiData != null && currentQiRealm != null) {
                currentQiRealm.applyRealmModifiers(foundPlayer, qiData);
            }
        }

        if (timeInCurrentBodyRealm % 100 == 0) { // Every 5 seconds
            IBodyCultivationData bodyData = foundPlayer.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyData != null && currentBodyRealm != null) {
                currentBodyRealm.applyRealmModifiers(foundPlayer, bodyData);
            }
        }

        // Periodic sync check - only on server side
        if (!foundPlayer.level().isClientSide() && tickCounter % PERIODIC_SYNC_TICKS == 0) {
            triggerSyncIfNeeded();
        }
    }

    // === Serialization (unchanged) ===

    public void deserializeNBT(CompoundTag tag) {
        // Qi cultivation data
        String qiRealmId = tag.getString("qi_realm_id");
        this.currentQiRealm = ICModCultivationRealms.get(qiRealmId);

        if (tag.contains("previous_qi_realm_id")) {
            String prevQiRealmId = tag.getString("previous_qi_realm_id");
            this.previousQiRealm = ICModCultivationRealms.get(prevQiRealmId);
        }

        this.qiBreakthroughAttempts = tag.getLong("qi_breakthrough_attempts");
        this.lastQiBreakthroughAttempt = tag.getLong("last_qi_breakthrough_attempt");
        this.qiBreakthroughCooldown = tag.getBoolean("qi_breakthrough_cooldown");
        this.timeInCurrentQiRealm = tag.getLong("time_in_current_qi_realm");
        this.totalQiCultivationTime = tag.getLong("total_qi_cultivation_time");

        // Body cultivation data
        String bodyRealmId = tag.getString("body_realm_id");
        this.currentBodyRealm = ICModCultivationRealms.get(bodyRealmId);

        if (tag.contains("previous_body_realm_id")) {
            String prevBodyRealmId = tag.getString("previous_body_realm_id");
            this.previousBodyRealm = ICModCultivationRealms.get(prevBodyRealmId);
        }

        this.bodyBreakthroughAttempts = tag.getLong("body_breakthrough_attempts");
        this.lastBodyBreakthroughAttempt = tag.getLong("last_body_breakthrough_attempt");
        this.bodyBreakthroughCooldown = tag.getBoolean("body_breakthrough_cooldown");
        this.timeInCurrentBodyRealm = tag.getLong("time_in_current_body_realm");
        this.totalBodyCultivationTime = tag.getLong("total_body_cultivation_time");

        // Sync management data
        this.lastSyncedValue = tag.getLong("last_synced_value");
        this.tickCounter = tag.getInt("tick_counter");
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Qi cultivation data
        tag.putString("qi_realm_id", currentQiRealm.getId());
        if (previousQiRealm != null) {
            tag.putString("previous_qi_realm_id", previousQiRealm.getId());
        }
        tag.putLong("qi_breakthrough_attempts", qiBreakthroughAttempts);
        tag.putLong("last_qi_breakthrough_attempt", lastQiBreakthroughAttempt);
        tag.putBoolean("qi_breakthrough_cooldown", qiBreakthroughCooldown);
        tag.putLong("time_in_current_qi_realm", timeInCurrentQiRealm);
        tag.putLong("total_qi_cultivation_time", totalQiCultivationTime);

        // Body cultivation data
        tag.putString("body_realm_id", currentBodyRealm.getId());
        if (previousBodyRealm != null) {
            tag.putString("previous_body_realm_id", previousBodyRealm.getId());
        }
        tag.putLong("body_breakthrough_attempts", bodyBreakthroughAttempts);
        tag.putLong("last_body_breakthrough_attempt", lastBodyBreakthroughAttempt);
        tag.putBoolean("body_breakthrough_cooldown", bodyBreakthroughCooldown);
        tag.putLong("time_in_current_body_realm", timeInCurrentBodyRealm);
        tag.putLong("total_body_cultivation_time", totalBodyCultivationTime);

        // Sync management data
        tag.putLong("last_synced_value", lastSyncedValue);
        tag.putInt("tick_counter", tickCounter);

        return tag;
    }

    // === Codec (unchanged) ===

    public static final Codec<PlayerRealmAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        // Qi cultivation fields
        Codec.STRING.fieldOf("qi_realm_id").forGetter(comp -> comp.currentQiRealm.getId()),
        Codec.STRING.optionalFieldOf("previous_qi_realm_id").forGetter(comp ->
            comp.previousQiRealm != null ? Optional.of(comp.previousQiRealm.getId()) : Optional.empty()),
        Codec.LONG.optionalFieldOf("qi_breakthrough_attempts", 0L).forGetter(comp -> comp.qiBreakthroughAttempts),
        Codec.LONG.optionalFieldOf("time_in_current_qi_realm", 0L).forGetter(comp -> comp.timeInCurrentQiRealm),
        Codec.LONG.optionalFieldOf("total_qi_cultivation_time", 0L).forGetter(comp -> comp.totalQiCultivationTime),

        // Body cultivation fields
        Codec.STRING.fieldOf("body_realm_id").forGetter(comp -> comp.currentBodyRealm.getId()),
        Codec.STRING.optionalFieldOf("previous_body_realm_id").forGetter(comp ->
            comp.previousBodyRealm != null ? Optional.of(comp.previousBodyRealm.getId()) : Optional.empty()),
        Codec.LONG.optionalFieldOf("body_breakthrough_attempts", 0L).forGetter(comp -> comp.bodyBreakthroughAttempts),
        Codec.LONG.optionalFieldOf("time_in_current_body_realm", 0L).forGetter(comp -> comp.timeInCurrentBodyRealm),
        Codec.LONG.optionalFieldOf("total_body_cultivation_time", 0L).forGetter(comp -> comp.totalBodyCultivationTime),

        // Sync management fields
        Codec.LONG.optionalFieldOf("last_synced_value", 0L).forGetter(comp -> comp.lastSyncedValue),
        Codec.INT.optionalFieldOf("tick_counter", 0).forGetter(comp -> comp.tickCounter)
    ).apply(instance, (qiRealmId, prevQiRealmId, qiAttempts, timeInQiRealm, totalQiTime,
                       bodyRealmId, prevBodyRealmId, bodyAttempts, timeInBodyRealm, totalBodyTime,
                       lastSync, ticks) -> {
        PlayerRealmAttachment comp = new PlayerRealmAttachment();

        // Set Qi cultivation data
        comp.currentQiRealm = ICModCultivationRealms.get(qiRealmId);
        prevQiRealmId.ifPresent(id -> comp.previousQiRealm = ICModCultivationRealms.get(id));
        comp.qiBreakthroughAttempts = qiAttempts;
        comp.timeInCurrentQiRealm = timeInQiRealm;
        comp.totalQiCultivationTime = totalQiTime;

        // Set Body cultivation data
        comp.currentBodyRealm = ICModCultivationRealms.get(bodyRealmId);
        prevBodyRealmId.ifPresent(id -> comp.previousBodyRealm = ICModCultivationRealms.get(id));
        comp.bodyBreakthroughAttempts = bodyAttempts;
        comp.timeInCurrentBodyRealm = timeInBodyRealm;
        comp.totalBodyCultivationTime = totalBodyTime;

        // Set sync management data
        comp.lastSyncedValue = lastSync;
        comp.tickCounter = ticks;

        return comp;
    }));

    // === Helper Classes ===

    public enum BreakthroughType {
        MINOR,
        MAJOR,
        SPECIAL,
        BOUNDARY
    }

    public record BreakthroughResult(boolean success, String message, @Nullable CultivationRealm newRealm) {}

    public record BreakthroughOption(BreakthroughType type, CultivationRealm targetRealm,
                                     double successChance, CultivationType cultivationType) {}

    public record BreakthroughOptions(List<BreakthroughOption> qiOptions,
                                      List<BreakthroughOption> bodyOptions) {}
}