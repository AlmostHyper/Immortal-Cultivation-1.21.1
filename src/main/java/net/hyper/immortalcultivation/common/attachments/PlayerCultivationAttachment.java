package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.*;
import net.hyper.immortalcultivation.core.cultivation.CultivationRealmRegistry;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerCultivationSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Comprehensive cultivation attachment that manages both Qi and Body cultivation paths
 * for player characters in the Xianxia system. Handles cultivation progression, realm advancement,
 * focus mechanics, and performance tracking with authentic cultivation features.
 * Extends PlayerAttachment to inherit debugging, synchronization, and performance monitoring capabilities.
 * Implements ICultivationData for unified cultivation management and ITickable for real-time progression.
 */
public class PlayerCultivationAttachment extends PlayerAttachment<PlayerCultivationAttachment> implements ICultivationData, ITickable {

    // === CONSTANTS === //

    private static final long DEFAULT_MAX_CULTIVATION = 100L;
    private static final String DEFAULT_MORTAL_REALM_ID = "mortal";
    private static final long MILESTONE_THRESHOLD = 1000L;
    private static final long FOCUS_CHANGE_COOLDOWN = 24000L; // 20 minutes at 20 TPS
    private static final double DEFAULT_ACCUMULATION_RATE = 1.0;
    private static final double DEFAULT_ADDITIVE_BONUS = 0.0;
    private static final double DEFAULT_MULTIPLICATIVE_BONUS = 1.0;
    private static final double MAX_MEDITATION_MULTIPLIER = 3.0;
    private static final int MEDITATION_RAMP_UP_TIME = 1200; // 60 seconds at 20 TPS
    private static final int MEDITATION_POSE_UPDATE_INTERVAL = 20; // Update pose every second

    // === CULTIVATION DATA FIELDS === //

    // --- Realm Data ---
    private final Map<CultivationType, CultivationRealm> currentRealms = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, CultivationRealm> previousRealms = new EnumMap<>(CultivationType.class);

    // --- Cultivation Progress Data ---
    private final Map<CultivationType, Long> currentCultivation = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> maxCultivation = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Double> cultivationFraction = new EnumMap<>(CultivationType.class);

    // --- Cultivation Rate Data ---
    private final Map<CultivationType, Double> accumulationRate = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Double> additiveBonus = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Double> multiplicativeBonus = new EnumMap<>(CultivationType.class);

    // --- Cultivation Statistics ---
    private final Map<CultivationType, Long> totalCultivationEarned = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> lastMilestone = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> timeInCurrentRealm = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> totalCultivationTime = new EnumMap<>(CultivationType.class);

    // === FOCUS SYSTEM FIELDS === //

    private CultivationType currentFocus = CultivationType.QI;
    private long lastFocusChangeTime = -FOCUS_CHANGE_COOLDOWN;

    // === MEDITATION SYSTEM FIELDS === //

    private boolean isMeditating = false;
    private int meditationTicks = 0;
    private double currentMeditationMultiplier = 1.0;
    private int lastPoseUpdateTick = 0;

    // === CONSTRUCTORS === //

    /**
     * Default constructor for creating a new PlayerCultivationAttachment with mortal base values.
     * Initializes all cultivation types with default mortal realm settings.
     */
    public PlayerCultivationAttachment() {
        super();
        initializeDefaults();
    }

    /**
     * Constructor with player reference for immediate association.
     * @param player The player to associate with this attachment
     */
    public PlayerCultivationAttachment(@Nullable Player player) {
        super();
        setPlayer(player);
        initializeDefaults();
    }

    // === INITIALIZATION === //

    /**
     * Initializes all cultivation data with default mortal values.
     * Sets up both QI and BODY cultivation paths with appropriate starting states.
     */
    private void initializeDefaults() {
        CultivationRealm mortalRealm = CultivationRealmRegistry.get(DEFAULT_MORTAL_REALM_ID);

        for (CultivationType type : CultivationType.values()) {
            // Realm initialization
            currentRealms.put(type, mortalRealm);
            previousRealms.put(type, mortalRealm);

            // Cultivation progress initialization
            currentCultivation.put(type, 0L);
            maxCultivation.put(type, DEFAULT_MAX_CULTIVATION);
            cultivationFraction.put(type, 0.0);

            // Rate initialization - FIX: Use realm's base cultivation rate
            double realmBaseCultivationRate = mortalRealm != null ?
                mortalRealm.getBaseCultivationRate() : DEFAULT_ACCUMULATION_RATE;
            accumulationRate.put(type, realmBaseCultivationRate);
            additiveBonus.put(type, DEFAULT_ADDITIVE_BONUS);
            multiplicativeBonus.put(type, DEFAULT_MULTIPLICATIVE_BONUS);

            // Statistics initialization
            totalCultivationEarned.put(type, 0L);
            lastMilestone.put(type, 0L);
            timeInCurrentRealm.put(type, 0L);
            totalCultivationTime.put(type, 0L);
        }

        if (debugEnabled()) {
            debugLog("Initialized PlayerCultivationAttachment with default mortal values");
        }
    }

    /**
     * Initializes any player-specific behavior when a player is set.
     * @param player The player that was set
     */
    @Override
    protected void onPlayerSet(Player player) {
        if (debugEnabled()) {
            debugLog("PlayerCultivationAttachment associated with player: {}",
                player != null ? player.getName().getString() : "NULL");
        }

        // Recalculate all cultivation-dependent values when player is set
        recalculateAllMaxCultivation();

        if (debugEnabled() && player != null) {
            debugLog("Cultivation attachment initialized for {}: QI={}/{}, BODY={}/{}",
                player.getName().getString(),
                getCurrentCultivation(CultivationType.QI), getMaxCultivation(CultivationType.QI),
                getCurrentCultivation(CultivationType.BODY), getMaxCultivation(CultivationType.BODY));
        }
    }

    /**
     * Gets the codec for serialization of this specific attachment type.
     * @return The codec for PlayerCultivationAttachment
     */
    @Override
    protected Codec<PlayerCultivationAttachment> getCodec() {
        return CODEC;
    }

    /**
     * Creates a sync packet with the current attachment data.
     * @param data The NBT data to include in the packet
     * @return The appropriate sync packet
     */
    @Override
    protected ISyncPacket createSyncPacket(CompoundTag data) {
        return new PlayerCultivationSyncPacket(data);
    }

    // === COPY FUNCTIONALITY === //

    /**
     * Creates a deep copy of this attachment for state comparison and sync management.
     * @return A new PlayerCultivationAttachment instance with identical data
     */
    @Override
    public PlayerCultivationAttachment copy() {
        PlayerCultivationAttachment copy = new PlayerCultivationAttachment();
        copy.copyFrom(this);
        return copy;
    }

    /**
     * Copies all data from another PlayerCultivationAttachment instance.
     * @param other The attachment to copy data from
     */
    @Override
    protected void copyFrom(PlayerCultivationAttachment other) {
        // Realm data
        this.currentRealms.clear();
        this.currentRealms.putAll(other.currentRealms);
        this.previousRealms.clear();
        this.previousRealms.putAll(other.previousRealms);

        // Cultivation progress data
        this.currentCultivation.clear();
        this.currentCultivation.putAll(other.currentCultivation);
        this.maxCultivation.clear();
        this.maxCultivation.putAll(other.maxCultivation);
        this.cultivationFraction.clear();
        this.cultivationFraction.putAll(other.cultivationFraction);

        // Cultivation rate data
        this.accumulationRate.clear();
        this.accumulationRate.putAll(other.accumulationRate);
        this.additiveBonus.clear();
        this.additiveBonus.putAll(other.additiveBonus);
        this.multiplicativeBonus.clear();
        this.multiplicativeBonus.putAll(other.multiplicativeBonus);

        // Cultivation statistics
        this.totalCultivationEarned.clear();
        this.totalCultivationEarned.putAll(other.totalCultivationEarned);
        this.lastMilestone.clear();
        this.lastMilestone.putAll(other.lastMilestone);
        this.timeInCurrentRealm.clear();
        this.timeInCurrentRealm.putAll(other.timeInCurrentRealm);
        this.totalCultivationTime.clear();
        this.totalCultivationTime.putAll(other.totalCultivationTime);

        // Focus system data
        this.currentFocus = other.currentFocus;
        this.lastFocusChangeTime = other.lastFocusChangeTime;
    }

    /**
     * Checks if this attachment is equal to another instance for change detection.
     * Used by the base class to determine if synchronization is needed.
     * @param other The attachment to compare with
     * @return True if all data fields are equal
     */
    @Override
    protected boolean isEqualTo(PlayerCultivationAttachment other) {
        if (other == null) return false;

        return this.currentRealms.equals(other.currentRealms) &&
            this.previousRealms.equals(other.previousRealms) &&
            this.currentCultivation.equals(other.currentCultivation) &&
            this.maxCultivation.equals(other.maxCultivation) &&
            this.cultivationFraction.equals(other.cultivationFraction) &&
            this.accumulationRate.equals(other.accumulationRate) &&
            this.additiveBonus.equals(other.additiveBonus) &&
            this.multiplicativeBonus.equals(other.multiplicativeBonus) &&
            this.totalCultivationEarned.equals(other.totalCultivationEarned) &&
            this.lastMilestone.equals(other.lastMilestone) &&
            this.timeInCurrentRealm.equals(other.timeInCurrentRealm) &&
            this.totalCultivationTime.equals(other.totalCultivationTime) &&
            this.currentFocus == other.currentFocus &&
            this.lastFocusChangeTime == other.lastFocusChangeTime;
    }

    // === ICULTIVATIONNDATA IMPLEMENTATION === //

    /**
     * Gets the base cultivation capacity for the specified cultivation type from the current realm.
     * @param type The cultivation type (QI or BODY)
     * @return The base maximum cultivation value from the realm
     */
    @Override
    public long getBaseCultivation(CultivationType type) {
        CultivationRealm realm = currentRealms.get(type);
        return realm != null ? realm.getBaseMaxCultivation() : DEFAULT_MAX_CULTIVATION;
    }

    /**
     * Gets the current cultivation progress for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return The current cultivation value
     */
    @Override
    public long getCurrentCultivation(CultivationType type) {
        return currentCultivation.getOrDefault(type, 0L);
    }

    /**
     * Enhanced setCurrentCultivation method that properly handles milestone recalculation
     * for both normal progression and set commands.
     *
     * @param type The cultivation type (QI or BODY)
     * @param value The new cultivation value
     */
    @Override
    public void setCurrentCultivation(CultivationType type, long value) {
        long oldValue = currentCultivation.get(type);
        long maxValue = maxCultivation.get(type);
        long newValue = Math.max(0, Math.min(value, maxValue));

        currentCultivation.put(type, newValue);

        if (oldValue != newValue) {
            // Update fractional progress
            double newFraction = maxValue > 0 ?
                ((double) newValue / maxValue) % 1.0 : 0.0;
            cultivationFraction.put(type, newFraction);

            markDirty();

            if (debugEnabled()) {
                debugLog("Cultivation changed for {}: {} {} -> {} (max: {})",
                    player != null ? player.getName().getString() : "unknown",
                    type, oldValue, newValue, maxValue);
            }

            // FIXED: Handle milestone processing correctly
            if (newValue > oldValue) {
                // Normal progression - use incremental milestone checking
                checkAndProcessMilestone(type, oldValue, newValue);
            } else {
                // Value decreased or set command - recalculate all milestones
                // This ensures consistency when using set commands
                recalculateMilestoneBonuses(type);
            }
        }
    }

    /**
     * Gets the maximum cultivation capacity for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return The maximum cultivation value
     */
    @Override
    public long getMaxCultivation(CultivationType type) {
        return maxCultivation.getOrDefault(type, DEFAULT_MAX_CULTIVATION);
    }

    /**
     * Sets the maximum cultivation for the specified cultivation type with validation.
     * Automatically adjusts current cultivation if it exceeds the new maximum.
     * @param type The cultivation type (QI or BODY)
     * @param value The new maximum cultivation value
     */
    @Override
    public void setMaxCultivation(CultivationType type, long value) {
        long newMax = Math.max(1, value);
        long oldMax = maxCultivation.get(type);

        if (newMax != oldMax) {
            maxCultivation.put(type, newMax);

            // Adjust current cultivation if it exceeds new maximum
            long current = currentCultivation.get(type);
            if (current > newMax) {
                currentCultivation.put(type, newMax);
                if (debugEnabled()) {
                    debugLog("Current cultivation capped to new maximum: {} -> {}", current, newMax);
                }
            }

            // Update fractional progress
            double newFraction = newMax > 0 ? (double) currentCultivation.get(type) / newMax : 0.0;
            cultivationFraction.put(type, newFraction);

            markDirty();

            if (debugEnabled()) {
                debugLog("Max cultivation changed for {}: {} {} -> {}",
                    player != null ? player.getName().getString() : "unknown",
                    type, oldMax, newMax);
            }
        }
    }

    /**
     * Gets the base cultivation rate for the specified cultivation type from the current realm.
     * @param type The cultivation type (QI or BODY)
     * @return The base cultivation rate per tick from the realm
     */
    public double getBaseCultivationRate(CultivationType type) {
        CultivationRealm realm = currentRealms.get(type);
        return realm != null ? realm.getBaseCultivationRate() : DEFAULT_ACCUMULATION_RATE;
    }

    /**
     * Gets the regeneration fraction for the specified cultivation type.
     * This represents partial progress towards the next whole cultivation point through passive regeneration.
     * @param type The cultivation type (QI or BODY)
     * @return Current regeneration fraction (0.0 to 0.999)
     */
    public double getRegenFraction(CultivationType type) {
        return cultivationFraction.getOrDefault(type, 0.0);
    }

    /**
     * Sets the regeneration fraction for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @param fraction The new regeneration fraction (will be clamped to 0.0-0.999)
     */
    public void setRegenFraction(CultivationType type, double fraction) {
        double oldFraction = cultivationFraction.get(type);
        double newFraction = Math.max(0.0, Math.min(0.999, fraction));

        cultivationFraction.put(type, newFraction);

        if (oldFraction != newFraction) {
            markDirty();

            if (debugEnabled()) {
                debugLog("Regen fraction changed for {}: {} {} -> {}",
                    player != null ? player.getName().getString() : "unknown",
                    type, oldFraction, newFraction);
            }
        }
    }

    /**
     * Adds to the regeneration fraction for the specified cultivation type.
     * Automatically handles overflow by converting to whole cultivation points.
     * @param type The cultivation type (QI or BODY)
     * @param amount The amount to add to the regeneration fraction
     */
    public void addRegenFraction(CultivationType type, double amount) {
        if (amount <= 0) return;

        double currentFraction = cultivationFraction.get(type);
        double newFraction = currentFraction + amount;

        // Convert overflow to whole cultivation points
        long wholeCultivation = (long) Math.floor(newFraction);
        newFraction -= wholeCultivation;

        cultivationFraction.put(type, newFraction);

        if (wholeCultivation > 0) {
            long currentValue = getCurrentCultivation(type);
            long maxValue = getMaxCultivation(type);
            long newTotal = Math.min(currentValue + wholeCultivation, maxValue);

            setCurrentCultivation(type, newTotal);

            if (debugEnabled()) {
                debugLog("Regen fraction overflow for {}: {} +{} whole, {:.3f} fractional -> {}",
                    player != null ? player.getName().getString() : "unknown",
                    type, wholeCultivation, newFraction, newTotal);
            }
        } else if (debugEnabled()) {
            debugLog("Added regen fraction for {}: {} +{:.3f} -> {:.3f} fractional",
                player != null ? player.getName().getString() : "unknown",
                type, amount, newFraction);
        }

        markDirty();
    }

    /**
     * Gets the cultivation progress as a percentage (0.0 to 1.0).
     * @param type The cultivation type (QI or BODY)
     * @return Progress as a decimal value between 0.0 and 1.0
     */
    @Override
    public double getProgress(CultivationType type) {
        long max = maxCultivation.get(type);
        return max > 0 ? (double) currentCultivation.get(type) / max : 0.0;
    }

    /**
     * Gets the cultivation progress as a percentage (0 to 100).
     * @param type The cultivation type (QI or BODY)
     * @return Progress as a percentage between 0 and 100
     */
    public double getProgressPercent(CultivationType type) {
        return getProgress(type) * 100.0;
    }

    // === CULTIVATION REALM MANAGEMENT === //

    /**
     * Gets the current cultivation realm for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return The current cultivation realm
     */
    public CultivationRealm getRealm(CultivationType type) {
        return currentRealms.getOrDefault(type, CultivationRealmRegistry.get(DEFAULT_MORTAL_REALM_ID));
    }

    /**
     * Sets the cultivation realm with optional silence for login reapplication.
     * @param type The cultivation type (QI or BODY)
     * @param realm The new cultivation realm
     * @param silent If true, suppresses advancement messages
     */
    public void setRealm(CultivationType type, CultivationRealm realm, boolean silent) {
        if (realm == null) {
            if (debugEnabled()) {
                debugWarn("Attempted to set null realm for cultivation type: {}", type);
            }
            return;
        }

        CultivationRealm oldRealm = currentRealms.get(type);
        currentRealms.put(type, realm);
        previousRealms.put(type, oldRealm);

        // Update maximum cultivation based on new realm
        long newMaxCultivation = realm.getBaseMaxCultivation();
        setMaxCultivation(type, newMaxCultivation);

        // Reset time in current realm only for actual advancement
        if (!silent) {
            timeInCurrentRealm.put(type, 0L);
        }

        markDirty();

        if (debugEnabled()) {
            debugLog("Realm {} for {}: {} {} -> {} (Max Cultivation: {})",
                silent ? "reapplied" : "changed",
                player != null ? player.getName().getString() : "unknown",
                type, oldRealm != null ? oldRealm.getId() : "NULL",
                realm.getId(), newMaxCultivation);
        }

        // === CONDITIONAL ADVANCEMENT MESSAGES === //
        // Only notify player of realm advancement for actual advancement, not login reapplication
        if (!silent && player instanceof ServerPlayer serverPlayer && oldRealm != null && !oldRealm.equals(realm)) {
            Component message = Component.literal("Congratulations! You have advanced to ")
                .append(Component.literal(realm.getDisplayName()).withStyle(ChatFormatting.GOLD))
                .append(" in " + type.getDisplayName() + " cultivation!")
                .withStyle(ChatFormatting.GREEN);
            serverPlayer.sendSystemMessage(message);
        }
    }

    /**
     * Gets the previous cultivation realm for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return The previous cultivation realm, or current realm if no previous exists
     */
    public CultivationRealm getPreviousRealm(CultivationType type) {
        return previousRealms.getOrDefault(type, currentRealms.get(type));
    }

    // === CULTIVATION ADVANCEMENT === //

    /**
     * Checks if the player can attempt a breakthrough for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return True if breakthrough can be attempted
     */
    public boolean canBreakthrough(CultivationType type) {
        CultivationRealm currentRealm = currentRealms.get(type);
        if (currentRealm == null) return false;

        // Check if at maximum cultivation for current realm
        long current = currentCultivation.get(type);
        long max = maxCultivation.get(type);

        // Must be at 100% cultivation and have next realm available
        return current >= max && !currentRealm.getNextCultivationRealms().isEmpty();
    }

    // === MILESTONE PROCESSING === //

    /**
     * Checks and processes cultivation milestones when cultivation increases.
     * @param type The cultivation type that changed
     * @param oldValue The previous cultivation value
     * @param newValue The new cultivation value
     */
    private void checkAndProcessMilestone(CultivationType type, long oldValue, long newValue) {
        if (newValue <= oldValue) return; // Only process increases

        long lastMilestoneValue = lastMilestone.get(type);
        long currentMilestone = (newValue / MILESTONE_THRESHOLD) * MILESTONE_THRESHOLD;

        if (currentMilestone > lastMilestoneValue) {
            lastMilestone.put(type, currentMilestone);

            // Update total cultivation earned
            long earned = totalCultivationEarned.get(type);
            totalCultivationEarned.put(type, earned + (newValue - oldValue));

            if (debugEnabled()) {
                debugLog("Milestone reached for {}: {} cultivation milestone {}",
                    player != null ? player.getName().getString() : "unknown",
                    type, currentMilestone);
            }

            // Apply milestone bonuses
            onMilestoneReached(type, currentMilestone);
        }
    }

    /**
     * Called when a cultivation milestone is reached. Applies permanent stat bonuses
     * based on the cultivation type and milestone value using source-based tracking.
     *
     * FIXED: Now uses source-based bonus system to prevent conflicts between cultivation paths
     *
     * @param type The cultivation type that reached the milestone
     * @param milestone The milestone value reached
     */
    protected void onMilestoneReached(CultivationType type, long milestone) {
        if (player == null) return;

        // Get the player's stats attachment to apply permanent bonuses
        PlayerStatsAttachment statsAttachment = player.getData(AttachmentRegistry.PLAYER_STATS.get());
        if (statsAttachment == null) {
            if (debugEnabled()) {
                debugWarn("Could not apply milestone bonuses: PlayerStatsAttachment not found");
            }
            return;
        }

        // Calculate current total milestones for this cultivation type
        long currentTotalMilestones = milestone / MILESTONE_THRESHOLD;
        long lastAppliedMilestone = lastMilestone.get(type);
        long previousTotalMilestones = lastAppliedMilestone / MILESTONE_THRESHOLD;

        // Calculate ONLY the new milestones to apply
        long newMilestonesToApply = currentTotalMilestones - previousTotalMilestones;

        if (newMilestonesToApply <= 0) {
            if (debugEnabled()) {
                debugWarn("No new milestones to apply for {}: current={}, previous={}",
                    type, currentTotalMilestones, previousTotalMilestones);
            }
            return;
        }

        if (debugEnabled()) {
            debugLog("Applying {} new milestone bonuses for {}: total milestones {} (was {})",
                newMilestonesToApply, type, currentTotalMilestones, previousTotalMilestones);
        }

        // Use source-based bonus system with separate sources for each cultivation type
        BonusSource milestoneSource = getMilestoneSourceForType(type);

        // Calculate TOTAL bonuses for this cultivation type (not incremental)
        switch (type) {
            case QI -> {
                // QI milestones: +1 health, +3 qi, +1 body per 1000 cultivation
                long totalHealthBonus = currentTotalMilestones * 1L;
                long totalQiBonus = currentTotalMilestones * 3L;
                long totalBodyBonus = currentTotalMilestones * 1L;

                statsAttachment.setSourcedStatBonus(IStatsData.StatType.HEALTH, milestoneSource, totalHealthBonus);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.QI, milestoneSource, totalQiBonus);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.BODY, milestoneSource, totalBodyBonus);

                if (debugEnabled()) {
                    debugLog("Applied {} QI milestone bonuses for {}: {} total health, {} total qi, {} total body",
                        newMilestonesToApply, player.getName().getString(),
                        totalHealthBonus, totalQiBonus, totalBodyBonus);
                }
            }
            case BODY -> {
                // BODY milestones: +2 health, +1 qi, +2 body per 1000 cultivation
                long totalHealthBonus = currentTotalMilestones * 2L;
                long totalQiBonus = currentTotalMilestones * 1L;
                long totalBodyBonus = currentTotalMilestones * 2L;

                statsAttachment.setSourcedStatBonus(IStatsData.StatType.HEALTH, milestoneSource, totalHealthBonus);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.QI, milestoneSource, totalQiBonus);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.BODY, milestoneSource, totalBodyBonus);

                if (debugEnabled()) {
                    debugLog("Applied {} BODY milestone bonuses for {}: {} total health, {} total qi, {} total body",
                        newMilestonesToApply, player.getName().getString(),
                        totalHealthBonus, totalQiBonus, totalBodyBonus);
                }
            }
        }

        // Mark stats attachment as dirty to trigger synchronization
        statsAttachment.markDirty();
    }

    /**
     * Gets the appropriate milestone bonus source for the given cultivation type.
     * This ensures QI and BODY milestone bonuses are tracked separately and can stack.
     *
     * @param type The cultivation type
     * @return The corresponding bonus source
     */
    private BonusSource getMilestoneSourceForType(CultivationType type) {
        return switch (type) {
            case QI -> BonusSource.QI_MILESTONES;
            case BODY -> BonusSource.BODY_MILESTONES;
        };
    }

    // === FOCUS SYSTEM === //

    /**
     * Toggles the cultivation focus between Qi and Body cultivation types.
     * This is a convenience method that switches to the opposite of the current focus.
     * Respects the focus change cooldown and leverages the existing setCurrentFocus infrastructure.
     *
     * @return True if focus was successfully toggled, false if on cooldown or other failure
     */
    public boolean toggleFocus() {
        // Determine the opposite focus type
        CultivationType newFocus = (currentFocus == CultivationType.QI) ?
            CultivationType.BODY : CultivationType.QI;

        // Use the existing setCurrentFocus method which handles all validation,
        // cooldowns, dirty marking, and player notifications
        boolean success = setCurrentFocus(newFocus);

        if (debugEnabled() && !success) {
            debugLog("Focus toggle failed for {}: cooldown or other restriction",
                player != null ? player.getName().getString() : "unknown");
        }

        return success;
    }

    /**
     * Gets the current cultivation focus type.
     * @return The currently focused cultivation type
     */
    public CultivationType getCurrentFocus() {
        return currentFocus;
    }

    /**
     * Sets the cultivation focus with cooldown validation.
     * Focus affects cultivation efficiency and breakthrough success rates.
     * @param focus The cultivation type to focus on
     * @return True if focus was changed successfully, false if on cooldown
     */
    public boolean setCurrentFocus(CultivationType focus) {
        if (focus == null || focus == currentFocus) {
            return false; // No change needed
        }

        // Check cooldown
        long currentTime = player != null && player.level() != null ?
            player.level().getGameTime() : System.currentTimeMillis() / 50; // Fallback to system time in ticks

        if (currentTime - lastFocusChangeTime < FOCUS_CHANGE_COOLDOWN) {
            if (debugEnabled()) {
                long remainingTicks = FOCUS_CHANGE_COOLDOWN - (currentTime - lastFocusChangeTime);
                debugLog("Focus change on cooldown for {}: {} ticks remaining",
                    player != null ? player.getName().getString() : "unknown", remainingTicks);
            }
            return false;
        }

        CultivationType oldFocus = currentFocus;
        currentFocus = focus;
        lastFocusChangeTime = currentTime;

        markDirty();

        if (debugEnabled()) {
            debugLog("Focus changed for {}: {} -> {}",
                player != null ? player.getName().getString() : "unknown",
                oldFocus, focus);
        }

        // Notify player of focus change
        if (player instanceof ServerPlayer serverPlayer) {
            Component message = Component.literal("Cultivation focus changed to ")
                .append(Component.literal(focus.getDisplayName()).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GREEN);
            serverPlayer.sendSystemMessage(message);
        }

        return true;
    }

    /**
     * Checks if focus change is currently on cooldown.
     * @return True if focus change is on cooldown
     */
    public boolean isFocusChangeOnCooldown() {
        if (player == null || player.level() == null) return false;

        long currentTime = player.level().getGameTime();
        return (currentTime - lastFocusChangeTime) < FOCUS_CHANGE_COOLDOWN;
    }

    /**
     * Gets the remaining cooldown time for focus changes in ticks.
     * @return Remaining cooldown time in ticks, or 0 if not on cooldown
     */
    public long getFocusChangeCooldownRemaining() {
        if (player == null || player.level() == null) return 0L;

        long currentTime = player.level().getGameTime();
        long elapsed = currentTime - lastFocusChangeTime;

        return Math.max(0L, FOCUS_CHANGE_COOLDOWN - elapsed);
    }

    // === MEDITATION SYSTEM === //

    /**
     * Toggles the meditation state for the player.
     * Meditation gradually increases cultivation rate up to 3x normal rate and forces sitting pose.
     *
     * @return True if now meditating, false if meditation stopped
     */
    public boolean toggleMeditation() {
        if (player == null) {
            if (debugEnabled()) {
                debugWarn("Cannot toggle meditation: no player reference");
            }
            return false;
        }

        isMeditating = !isMeditating;

        if (isMeditating) {
            startMeditation();
        } else {
            stopMeditation();
        }

        markDirty();

        if (debugEnabled()) {
            debugLog("Player {} {} meditation",
                player.getName().getString(),
                isMeditating ? "started" : "stopped");
        }

        return isMeditating;
    }

    /**
     * Starts meditation, initializing timers and effects.
     */
    private void startMeditation() {
        meditationTicks = 0;
        currentMeditationMultiplier = 1.0;
        lastPoseUpdateTick = 0;

        // Force player into sitting pose if on server
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.setPose(Pose.SITTING);
        }

        if (debugEnabled()) {
            debugLog("Meditation started for player: {}", player.getName().getString());
        }
    }

    /**
     * Stops meditation, resetting timers and restoring normal pose.
     */
    private void stopMeditation() {
        meditationTicks = 0;
        currentMeditationMultiplier = 1.0;

        // Restore normal pose if on server
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.setPose(Pose.STANDING);
        }

        if (debugEnabled()) {
            debugLog("Meditation stopped for player: {}", player.getName().getString());
        }
    }

    /**
     * Gets whether the player is currently meditating.
     *
     * @return True if player is meditating
     */
    public boolean isMeditating() {
        return isMeditating;
    }

    /**
     * Gets the current meditation cultivation rate multiplier.
     * This gradually increases from 1.0x to 3.0x over the ramp-up period.
     *
     * @return Current meditation multiplier (1.0 to 3.0)
     */
    public double getMeditationMultiplier() {
        return isMeditating ? currentMeditationMultiplier : 1.0;
    }

    /**
     * Gets the current meditation duration in ticks.
     *
     * @return Meditation duration in ticks
     */
    public int getMeditationTicks() {
        return meditationTicks;
    }

    /**
     * Gets the meditation progress as a percentage (0.0 to 1.0).
     *
     * @return Progress towards maximum meditation bonus
     */
    public double getMeditationProgress() {
        if (!isMeditating || MEDITATION_RAMP_UP_TIME <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) meditationTicks / MEDITATION_RAMP_UP_TIME);
    }

    /**
     * Processes meditation tick updates, called from the main tick method.
     */
    private void processMeditationTick() {
        if (!isMeditating) {
            return;
        }

        meditationTicks++;

        // Update meditation multiplier gradually
        updateMeditationMultiplier();

        // Maintain sitting pose
        maintainMeditationPose();

        // Check for meditation interruption conditions
        checkMeditationInterruption();
    }

    /**
     * Updates the meditation multiplier based on current progress.
     * Gradually increases from 1.0x to 3.0x over the ramp-up period.
     */
    private void updateMeditationMultiplier() {
        double progress = getMeditationProgress();

        // Smooth curve: starts slow, accelerates, then slows near the end
        double smoothProgress = 1.0 - Math.cos(progress * Math.PI / 2.0);

        currentMeditationMultiplier = 1.0 + (smoothProgress * (MAX_MEDITATION_MULTIPLIER - 1.0));
        currentMeditationMultiplier = Math.min(currentMeditationMultiplier, MAX_MEDITATION_MULTIPLIER);

        if (debugEnabled() && meditationTicks % 100 == 0) { // Debug every 5 seconds
            debugLog("Meditation progress: {:.1f}%, multiplier: {:.2f}x",
                progress * 100, currentMeditationMultiplier);
        }
    }

    /**
     * Maintains the player's sitting pose during meditation.
     */
    private void maintainMeditationPose() {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Update pose periodically
        if (meditationTicks - lastPoseUpdateTick >= MEDITATION_POSE_UPDATE_INTERVAL) {
            if (serverPlayer.getPose() != Pose.SITTING) {
                serverPlayer.setPose(Pose.SITTING);
            }
            lastPoseUpdateTick = meditationTicks;
        }
    }

    /**
     * Checks for conditions that should interrupt meditation.
     */
    private void checkMeditationInterruption() {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Interrupt meditation if player is moving, taking damage, or attacking
        boolean shouldInterrupt = false;

        // Check if player is moving (small threshold to account for floating point precision)
        if (serverPlayer.getDeltaMovement().lengthSqr() > 0.001) {
            shouldInterrupt = true;
        }

        // Check if player is not in sitting pose (forced out by external means)
        if (serverPlayer.getPose() != Pose.SITTING) {
            shouldInterrupt = true;
        }

        if (shouldInterrupt) {
            isMeditating = false;
            stopMeditation();
            markDirty();

            serverPlayer.sendSystemMessage(Component.literal("Your meditation was interrupted!")
                .withStyle(ChatFormatting.YELLOW));

            if (debugEnabled()) {
                debugLog("Meditation interrupted for player: {}", serverPlayer.getName().getString());
            }
        }
    }

    // === CULTIVATION STATISTICS === //

    /**
     * Gets the total cultivation earned for the specified cultivation type.
     * @param type The cultivation type to query
     * @return Total cultivation earned over time
     */
    public long getTotalCultivationEarned(CultivationType type) {
        return totalCultivationEarned.getOrDefault(type, 0L);
    }

    /**
     * Gets the time spent in the current realm for the specified cultivation type.
     * @param type The cultivation type to query
     * @return Time in current realm in ticks
     */
    public long getTimeInCurrentRealm(CultivationType type) {
        return timeInCurrentRealm.getOrDefault(type, 0L);
    }

    /**
     * Gets the total time spent cultivating for the specified cultivation type.
     * @param type The cultivation type to query
     * @return Total cultivation time in ticks
     */
    public long getTotalCultivationTime(CultivationType type) {
        return totalCultivationTime.getOrDefault(type, 0L);
    }

    /**
     * Gets the last milestone reached for the specified cultivation type.
     * @param type The cultivation type to query
     * @return Last milestone value reached
     */
    public long getLastMilestone(CultivationType type) {
        return lastMilestone.getOrDefault(type, 0L);
    }

    /**
     * Gets the current accumulation rate for the specified cultivation type.
     * @param type The cultivation type to query
     * @return Current accumulation rate per tick
     */
    public double getAccumulationRate(CultivationType type) {
        return accumulationRate.getOrDefault(type, DEFAULT_ACCUMULATION_RATE);
    }

    /**
     * Sets the accumulation rate for the specified cultivation type.
     * @param type The cultivation type to modify
     * @param rate The new accumulation rate per tick
     */
    public void setAccumulationRate(CultivationType type, double rate) {
        double oldRate = accumulationRate.get(type);
        accumulationRate.put(type, Math.max(0.0, rate));

        if (oldRate != rate) {
            markDirty();

            if (debugEnabled()) {
                debugLog("Accumulation rate changed for {}: {} {} -> {}",
                    player != null ? player.getName().getString() : "unknown",
                    type, oldRate, rate);
            }
        }
    }

    /**
     * Gets the additive bonus for the specified cultivation type.
     * @param type The cultivation type to query
     * @return Current additive bonus
     */
    public double getAdditiveBonus(CultivationType type) {
        return additiveBonus.getOrDefault(type, DEFAULT_ADDITIVE_BONUS);
    }

    /**
     * Sets the additive bonus for the specified cultivation type.
     * @param type The cultivation type to modify
     * @param bonus The new additive bonus
     */
    public void setAdditiveBonus(CultivationType type, double bonus) {
        double oldBonus = additiveBonus.get(type);
        additiveBonus.put(type, bonus);

        if (oldBonus != bonus) {
            markDirty();

            if (debugEnabled()) {
                debugLog("Additive bonus changed for {}: {} {} -> {}",
                    player != null ? player.getName().getString() : "unknown",
                    type, oldBonus, bonus);
            }
        }
    }

    /**
     * Gets the multiplicative bonus for the specified cultivation type.
     * @param type The cultivation type to query
     * @return Current multiplicative bonus
     */
    public double getMultiplicativeBonus(CultivationType type) {
        return multiplicativeBonus.getOrDefault(type, DEFAULT_MULTIPLICATIVE_BONUS);
    }

    /**
     * Sets the multiplicative bonus for the specified cultivation type.
     * @param type The cultivation type to modify
     * @param bonus The new multiplicative bonus
     */
    public void setMultiplicativeBonus(CultivationType type, double bonus) {
        double oldBonus = multiplicativeBonus.get(type);
        multiplicativeBonus.put(type, Math.max(0.0, bonus));

        if (oldBonus != bonus) {
            markDirty();

            if (debugEnabled()) {
                debugLog("Multiplicative bonus changed for {}: {} {} -> {}",
                    player != null ? player.getName().getString() : "unknown",
                    type, oldBonus, bonus);
            }
        }
    }

    /**
     * Adds a bonus to the existing additive bonus for the specified cultivation type.
     * This method accumulates bonuses rather than replacing them, making it suitable
     * for permanent consumable effects like pills. The "diminishing" aspect refers
     * to the fact that the bonus amount passed in has already been reduced by
     * consumption effectiveness calculations.
     *
     * @param type The cultivation type to modify
     * @param bonusAmount The bonus amount to add (already adjusted for effectiveness)
     * @param minValue Minimum allowed total bonus value (prevents negative bonuses)
     * @param maxValue Maximum allowed total bonus value (prevents excessive bonuses)
     */
    public void addBonusWithDiminishing(CultivationType type, double bonusAmount, double minValue, double maxValue) {
        if (bonusAmount == 0.0) return; // No change needed

        double currentBonus = getAdditiveBonus(type);
        double newBonus = currentBonus + bonusAmount;

        // Apply min/max constraints
        newBonus = Math.max(minValue, Math.min(maxValue, newBonus));

        // Only update if the value actually changed
        if (currentBonus != newBonus) {
            setAdditiveBonus(type, newBonus);

            if (debugEnabled()) {
                debugLog("Added bonus with diminishing for {}: {} +{} -> {} (clamped to [{}, {}])",
                    player != null ? player.getName().getString() : "unknown",
                    type, bonusAmount, newBonus, minValue, maxValue);
            }
        }
    }

    // === CULTIVATION EFFICIENCY === //

    /**
     * Gets the effective cultivation rate for a specific cultivation type.
     * Includes base rate, focus bonus, realm bonus, and meditation bonus.
     *
     * @param type The cultivation type to get the rate for
     * @return The effective cultivation rate per tick
     */
    public double getEffectiveCultivationRate(CultivationType type) {
        if (player == null) return 0.0;

        // Get base rate from current realm
        CultivationRealm realm = getRealm(type);
        double baseRate = realm != null ? realm.getBaseCultivationRate() : 0.0005;

        // Apply focus bonus (2x for focused type)
        double focusMultiplier = (type == currentFocus) ? 2.0 : 1.0;

        // Apply meditation bonus
        double meditationMultiplier = getMeditationMultiplier();

        // Combine all multipliers
        double effectiveRate = baseRate * focusMultiplier * meditationMultiplier;

        if (debugEnabled() && player.level().getGameTime() % 100 == 0) {
            debugLog("Effective {} rate: base={}, focus={}x, meditation={}x, total={}",
                type, baseRate, focusMultiplier, meditationMultiplier, effectiveRate);
        }

        return effectiveRate;
    }


    /**
     * Adds cultivation progress to the specified cultivation type.
     * Automatically handles overflow and milestone processing.
     * @param type The cultivation type to add cultivation to
     * @param amount The amount of cultivation to add
     * @param respectMaximum Whether to respect the maximum cultivation limit
     */
    @Override
    public void addCultivation(CultivationType type, long amount, boolean respectMaximum) {
        if (amount <= 0) return;

        long currentValue = getCurrentCultivation(type);
        long maxValue = getMaxCultivation(type);

        // Update fractional progress
        double currentFraction = cultivationFraction.get(type);
        double newFraction = currentFraction + amount;

        // Convert fractional progress to whole cultivation points
        long wholeCultivation = (long) Math.floor(newFraction);
        newFraction -= wholeCultivation;

        cultivationFraction.put(type, newFraction);

        // Update total cultivation time
        long totalTime = totalCultivationTime.get(type);
        totalCultivationTime.put(type, totalTime + 1); // Increment by 1 tick

        // Mark dirty since we've modified fractional progress and time tracking
        markDirty();

        if (wholeCultivation > 0) {
            long newTotal = currentValue + wholeCultivation;

            if (respectMaximum) {
                newTotal = Math.min(newTotal, maxValue);
            }

            setCurrentCultivation(type, newTotal); // This also marks dirty, but that's fine

            if (debugEnabled()) {
                debugLog("Added cultivation for {}: {} +{} -> {} ({:.3f} fractional)",
                    player != null ? player.getName().getString() : "unknown",
                    type, wholeCultivation, newTotal, newFraction);
            }
        } else if (debugEnabled()) {
            debugLog("Added fractional cultivation for {}: {} +{:.3f} -> {:.3f} fractional",
                player != null ? player.getName().getString() : "unknown",
                type, amount, newFraction);
        }
    }

    /**
     * Subtracts cultivation progress from the specified cultivation type.
     * Automatically handles underflow and maintains fractional progress.
     * @param type The cultivation type to subtract cultivation from
     * @param amount The amount of cultivation to subtract
     * @param respectMinimum Whether to respect the minimum cultivation limit (0)
     */
    @Override
    public void subtractCultivation(CultivationType type, long amount, boolean respectMinimum) {
        if (amount <= 0) return;

        long currentValue = getCurrentCultivation(type);

        // Update fractional progress
        double currentFraction = cultivationFraction.get(type);
        double newFraction = currentFraction - amount;

        // Handle negative fractional progress by borrowing from whole cultivation
        long wholeCultivationToSubtract = 0L;
        while (newFraction < 0.0 && (currentValue > 0 || !respectMinimum)) {
            newFraction += 1.0;
            wholeCultivationToSubtract++;
        }

        // Ensure fraction stays within bounds
        newFraction = Math.max(0.0, newFraction);
        cultivationFraction.put(type, newFraction);

        if (wholeCultivationToSubtract > 0) {
            long newTotal = currentValue - wholeCultivationToSubtract;

            if (respectMinimum) {
                newTotal = Math.max(0L, newTotal);
            }

            setCurrentCultivation(type, newTotal);

            if (debugEnabled()) {
                debugLog("Subtracted cultivation for {}: {} -{} -> {} ({:.3f} fractional)",
                    player != null ? player.getName().getString() : "unknown",
                    type, wholeCultivationToSubtract, newTotal, newFraction);
            }
        } else if (debugEnabled()) {
            debugLog("Subtracted fractional cultivation for {}: {} -{:.3f} -> {:.3f} fractional",
                player != null ? player.getName().getString() : "unknown",
                type, amount, newFraction);
        }

        // Mark dirty since we've modified fractional progress
        markDirty();
    }

    // === ITICKABLE IMPLEMENTATION === //

    /**
     * Ticks the cultivation attachment, updating time-based mechanics.
     * Called every game tick (20 times per second) for active players.
     */
    @Override
    public void tick() {
        if (player == null || !player.isAlive()) return;

        startPerformanceTracking("cultivationTick");

        try {
            // Update time in current realm for all cultivation types
            for (CultivationType type : CultivationType.values()) {
                long currentTime = timeInCurrentRealm.get(type);
                timeInCurrentRealm.put(type, currentTime + 1);
            }

            // Process meditation updates
            processMeditationTick(); // <-- ADD THIS LINE

            // Process automatic cultivation if player is in a suitable state
            if (shouldAutoCultivate()) {
                processAutomaticCultivation();
            }

            // Mark dirty periodically for sync (every 5 seconds)
            if (player.level().getGameTime() % 100 == 0) {
                markDirty();
            }

        } finally {
            endPerformanceTracking("cultivationTick", 2); // 2ms warning threshold
        }
    }

    /**
     * Checks if the player should automatically gain cultivation.
     * @return True if automatic cultivation should occur
     */
    private boolean shouldAutoCultivate() {
        if (player == null) return false;

        return !player.isSpectator();
    }

    /**
     * Processes automatic cultivation based on current focus and rates.
     * Uses fractional accumulation to handle small cultivation rates properly.
     */
    private void processAutomaticCultivation() {
        if (player == null) return;

        // Focus gets primary cultivation, other type gets reduced rate
        for (CultivationType type : CultivationType.values()) {
            double effectiveRate = getEffectiveCultivationRate(type);

            // Reduce rate for non-focused cultivation type
            if (type != currentFocus) {
                effectiveRate *= 0.5; // 50% rate when not focused
            }

            // Apply rate reduction based on current progress (harder to cultivate at higher levels)
            double progressPenalty = 1.0 - (getProgress(type) * 0.5); // Up to 50% reduction at max
            effectiveRate *= Math.max(0.1, progressPenalty);

            if (effectiveRate > 0) {
                // FIX: Use addRegenFraction for proper fractional accumulation
                addRegenFraction(type, effectiveRate);
            }
        }
    }

    // === UTILITY METHODS === //

    /**
     * Recalculates maximum cultivation values and accumulation rates for all cultivation types.
     * Should be called when realms change or player-specific modifiers are updated.
     */
    private void recalculateAllMaxCultivation() {
        for (CultivationType type : CultivationType.values()) {
            CultivationRealm realm = currentRealms.get(type);
            if (realm != null) {
                long baseMax = realm.getBaseMaxCultivation();
                double baseRate = realm.getBaseCultivationRate();

                // Apply any player-specific modifiers here if needed
                maxCultivation.put(type, baseMax);
                accumulationRate.put(type, baseRate); // FIX: Sync accumulation rate with realm
            }
        }

        if (debugEnabled()) {
            debugLog("Recalculated cultivation values - QI: {}/{} rate={}, BODY: {}/{} rate={}",
                maxCultivation.get(CultivationType.QI),
                getCurrentCultivation(CultivationType.QI),
                accumulationRate.get(CultivationType.QI),
                maxCultivation.get(CultivationType.BODY),
                getCurrentCultivation(CultivationType.BODY),
                accumulationRate.get(CultivationType.BODY));
        }
    }

    /**
     * Fixes milestone tracking when using set commands or loading data.
     * This method recalculates and reapplies all milestone bonuses from scratch
     * to ensure consistency.
     *
     * @param type The cultivation type to fix milestones for
     */
    public void recalculateMilestoneBonuses(CultivationType type) {
        if (player == null) return;

        PlayerStatsAttachment statsAttachment = player.getData(AttachmentRegistry.PLAYER_STATS.get());
        if (statsAttachment == null) return;

        long currentCultivation = getCurrentCultivation(type);
        long totalMilestones = currentCultivation / MILESTONE_THRESHOLD;

        if (totalMilestones <= 0) return;

        if (debugEnabled()) {
            debugLog("Recalculating milestone bonuses for {}: {} cultivation = {} milestones",
                type, currentCultivation, totalMilestones);
        }

        // Clear existing milestone bonuses for this cultivation type
        // (This requires using the sourced bonus system)
        BonusSource milestoneSource = BonusSource.MILESTONES;

        // Calculate what the bonuses SHOULD be for the total milestones
        switch (type) {
            case QI -> {
                long healthBonus = totalMilestones * 1L;
                long qiBonus = totalMilestones * 3L;
                long bodyBonus = totalMilestones * 1L;

                statsAttachment.setSourcedStatBonus(IStatsData.StatType.HEALTH, milestoneSource, healthBonus);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.QI, milestoneSource, qiBonus);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.BODY, milestoneSource, bodyBonus);
            }
            case BODY -> {
                long healthBonus = totalMilestones * 2L;
                long qiBonus = totalMilestones * 1L;
                long bodyBonus = totalMilestones * 2L;

                statsAttachment.setSourcedStatBonus(IStatsData.StatType.HEALTH, milestoneSource, healthBonus);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.QI, milestoneSource, qiBonus);
                statsAttachment.setSourcedStatBonus(IStatsData.StatType.BODY, milestoneSource, bodyBonus);
            }
        }

        // Update the last milestone tracker
        lastMilestone.put(type, totalMilestones * MILESTONE_THRESHOLD);
        markDirty();
    }

    /**
     * Gets a user-friendly display string for cultivation progress.
     * @param type The cultivation type to display
     * @return Formatted progress string
     */
    public String getProgressDisplayString(CultivationType type) {
        long current = getCurrentCultivation(type);
        long max = getMaxCultivation(type);
        double percent = getProgressPercent(type);

        return String.format("%,d / %,d (%.1f%%)", current, max, percent);
    }

    /**
     * Gets a user-friendly display string for cultivation realm.
     * @param type The cultivation type to display
     * @return Formatted realm string
     */
    public String getRealmDisplayString(CultivationType type) {
        CultivationRealm realm = getRealm(type);
        return realm != null ? realm.getDisplayName() : "Unknown Realm";
    }

    /**
     * Gets comprehensive cultivation information for debugging or display.
     * @param type The cultivation type to query
     * @return Map containing all cultivation data for the specified type
     */
    public Map<String, Object> getCultivationInfo(CultivationType type) {
        Map<String, Object> info = new HashMap<>();

        info.put("realm", getRealmDisplayString(type));
        info.put("progress", getProgressDisplayString(type));
        info.put("effectiveRate", String.format("%.3f/tick", getEffectiveCultivationRate(type)));
        info.put("totalEarned", String.format("%,d", getTotalCultivationEarned(type)));
        info.put("timeInRealm", String.format("%,d ticks", getTimeInCurrentRealm(type)));
        info.put("totalTime", String.format("%,d ticks", getTotalCultivationTime(type)));
        info.put("lastMilestone", String.format("%,d", getLastMilestone(type)));
        info.put("canBreakthrough", canBreakthrough(type));
        info.put("isFocused", currentFocus == type);

        return info;
    }

    /**
     * Resets all cultivation progress for the specified type (admin/debug use).
     * @param type The cultivation type to reset
     */
    public void resetCultivation(CultivationType type) {
        if (debugEnabled()) {
            debugLog("Resetting cultivation for {}: {}",
                player != null ? player.getName().getString() : "unknown", type);
        }

        // Reset progress
        currentCultivation.put(type, 0L);
        cultivationFraction.put(type, 0.0);

        // Reset statistics
        totalCultivationEarned.put(type, 0L);
        lastMilestone.put(type, 0L);
        timeInCurrentRealm.put(type, 0L);
        totalCultivationTime.put(type, 0L);

        // Reset bonuses
        additiveBonus.put(type, DEFAULT_ADDITIVE_BONUS);
        multiplicativeBonus.put(type, DEFAULT_MULTIPLICATIVE_BONUS);
        accumulationRate.put(type, DEFAULT_ACCUMULATION_RATE);

        // Reset to mortal realm
        CultivationRealm mortalRealm = CultivationRealmRegistry.get(DEFAULT_MORTAL_REALM_ID);
        currentRealms.put(type, mortalRealm);
        previousRealms.put(type, mortalRealm);
        maxCultivation.put(type, DEFAULT_MAX_CULTIVATION);

        markDirty();
    }

    /**
     * Resets all cultivation data (admin/debug use).
     */
    public void resetAllCultivation() {
        if (debugEnabled()) {
            debugLog("Resetting all cultivation for {}",
                player != null ? player.getName().getString() : "unknown");
        }

        for (CultivationType type : CultivationType.values()) {
            resetCultivation(type);
        }

        // Reset focus
        currentFocus = CultivationType.QI;
        lastFocusChangeTime = -FOCUS_CHANGE_COOLDOWN;

        markDirty();
    }

    /**
     * Gets the player reference safely, with null checking.
     * @return The player reference, or null if not set
     */
    @Nullable
    public Player getPlayerSafe() {
        return player;
    }

    /**
     * Gets the race cultivation multiplier for the specified cultivation type.
     * @param type The cultivation type to get multiplier for
     * @return Race multiplier (1.0 = no bonus, higher = better)
     */
    private double getRaceMultiplier(CultivationType type) {
        if (player == null) {
            if (debugEnabled()) {
                debugLog("Cannot get race multiplier: player is null");
            }
            return 1.0;
        }

        PlayerRaceAttachment raceAttachment = player.getData(AttachmentRegistry.PLAYER_RACE.get());
        if (raceAttachment == null || !raceAttachment.hasRaceAssigned()) {
            if (debugEnabled()) {
                debugLog("No race assigned or race attachment missing for player {}",
                    player.getName().getString());
            }
            return 1.0;
        }

        return raceAttachment.getCultivationMultiplier(type);
    }

    /**
     * Gets the talent cultivation multiplier for the specified cultivation type.
     * @param type The cultivation type to get multiplier for
     * @return Talent multiplier (1.0 = no bonus, higher = better)
     */
    private double getTalentMultiplier(CultivationType type) {
        if (player == null) {
            if (debugEnabled()) {
                debugLog("Cannot get talent multiplier: player is null");
            }
            return 1.0;
        }

        PlayerTalentsAttachment talentsAttachment = player.getData(AttachmentRegistry.PLAYER_TALENTS.get());
        if (talentsAttachment == null) {
            if (debugEnabled()) {
                debugLog("PlayerTalentsAttachment not found for player {}",
                    player.getName().getString());
            }
            return 1.0;
        }

        // Get the appropriate talent multiplier based on cultivation type
        PlayerTalentsAttachment.TalentType talentType = switch (type) {
            case QI -> PlayerTalentsAttachment.TalentType.SPIRITUAL_ROOT;
            case BODY -> PlayerTalentsAttachment.TalentType.PHYSIQUE;
        };

        if (!talentsAttachment.isTalentAssigned(talentType)) {
            if (debugEnabled()) {
                debugLog("Talent {} not assigned for player {}",
                    talentType.name(), player.getName().getString());
            }
            return 1.0;
        }

        return talentsAttachment.getTalentMultiplier(talentType);
    }

    // === DEBUG SUPPORT === //

    /**
     * Adds specific state debug information for PlayerCultivationAttachment.
     * @param info The map to add debug information to
     */
    @Override
    protected void addSpecificStateDebugInfo(Map<String, Object> info) {
        // Focus information
        info.put("currentFocus", currentFocus.name());
        info.put("focusChangeCooldown", getFocusChangeCooldownRemaining() + " ticks");

        // Cultivation information for each type
        for (CultivationType type : CultivationType.values()) {
            String prefix = type.name().toLowerCase();
            info.put(prefix + "Realm", getRealmDisplayString(type));
            info.put(prefix + "Progress", getProgressDisplayString(type));
            info.put(prefix + "EffectiveRate", String.format("%.3f/tick", getEffectiveCultivationRate(type)));
            info.put(prefix + "TotalEarned", getTotalCultivationEarned(type));
            info.put(prefix + "CanBreakthrough", canBreakthrough(type));
        }

        // Performance information
        info.put("autoCultivateEnabled", shouldAutoCultivate());
    }

    /**
     * Validates the internal state and returns any issues found.
     * @return Map of validation issues (empty if no issues)
     */
    @Override
    public Map<String, String> validateState() {
        Map<String, String> issues = new HashMap<>();

        // Validate each cultivation type
        for (CultivationType type : CultivationType.values()) {
            String prefix = type.name().toLowerCase();

            // Check cultivation values
            long current = getCurrentCultivation(type);
            long max = getMaxCultivation(type);
            if (current < 0) issues.put(prefix + "Current", "Negative cultivation: " + current);
            if (current > max) issues.put(prefix + "Current", "Exceeds maximum: " + current + " > " + max);
            if (max <= 0) issues.put(prefix + "Max", "Non-positive maximum: " + max);

            // Check fractional progress
            double fraction = cultivationFraction.get(type);
            if (fraction < 0 || fraction >= 1.0) {
                issues.put(prefix + "Fraction", "Invalid fraction: " + fraction);
            }

            // Check rates and bonuses
            double rate = getAccumulationRate(type);
            if (rate < 0) issues.put(prefix + "Rate", "Negative rate: " + rate);

            double mult = getMultiplicativeBonus(type);
            if (mult < 0) issues.put(prefix + "MultBonus", "Negative multiplier: " + mult);

            // Check realm
            CultivationRealm realm = getRealm(type);
            if (realm == null) issues.put(prefix + "Realm", "Null realm");
        }

        // Check focus
        if (currentFocus == null) issues.put("focus", "Null focus");

        return issues;
    }

    /**
     * Gets a hash of the current state for comparison.
     * @return State hash as a string
     */
    @Override
    public String getStateHash() {
        return String.valueOf(
            (getCurrentCultivation(CultivationType.QI) * 31L + getMaxCultivation(CultivationType.QI)) * 31L +
                (getCurrentCultivation(CultivationType.BODY) * 31L + getMaxCultivation(CultivationType.BODY)) * 31L +
                currentFocus.hashCode() +
                Double.hashCode(getEffectiveCultivationRate(CultivationType.QI)) +
                Double.hashCode(getEffectiveCultivationRate(CultivationType.BODY))
        );
    }

    // === CODEC FOR SERIALIZATION === //

    /**
     * Sub-codec for realm data
     */
    private static final Codec<PlayerCultivationAttachment> REALM_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("qi_realm").forGetter(d -> d.currentRealms.get(CultivationType.QI).getId()),
            Codec.STRING.fieldOf("body_realm").forGetter(d -> d.currentRealms.get(CultivationType.BODY).getId()),
            Codec.STRING.optionalFieldOf("qi_previous_realm").forGetter(d ->
                d.previousRealms.get(CultivationType.QI) != null ?
                    Optional.of(d.previousRealms.get(CultivationType.QI).getId()) : Optional.empty()),
            Codec.STRING.optionalFieldOf("body_previous_realm").forGetter(d ->
                d.previousRealms.get(CultivationType.BODY) != null ?
                    Optional.of(d.previousRealms.get(CultivationType.BODY).getId()) : Optional.empty())
        ).apply(instance, (qiRealm, bodyRealm, qiPrevious, bodyPrevious) -> {
            PlayerCultivationAttachment data = new PlayerCultivationAttachment();
            data.currentRealms.put(CultivationType.QI, CultivationRealmRegistry.get(qiRealm));
            data.currentRealms.put(CultivationType.BODY, CultivationRealmRegistry.get(bodyRealm));
            data.previousRealms.put(CultivationType.QI,
                qiPrevious.map(CultivationRealmRegistry::get).orElse(CultivationRealmRegistry.get(DEFAULT_MORTAL_REALM_ID)));
            data.previousRealms.put(CultivationType.BODY,
                bodyPrevious.map(CultivationRealmRegistry::get).orElse(CultivationRealmRegistry.get(DEFAULT_MORTAL_REALM_ID)));
            return data;
        })
    );

    /**
     * Sub-codec for cultivation progress data
     */
    private static final Codec<PlayerCultivationAttachment> PROGRESS_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("qi_current").forGetter(d -> d.currentCultivation.get(CultivationType.QI)),
            Codec.LONG.fieldOf("qi_max").forGetter(d -> d.maxCultivation.get(CultivationType.QI)),
            Codec.DOUBLE.optionalFieldOf("qi_fraction", 0.0).forGetter(d -> d.cultivationFraction.get(CultivationType.QI)),
            Codec.LONG.fieldOf("body_current").forGetter(d -> d.currentCultivation.get(CultivationType.BODY)),
            Codec.LONG.fieldOf("body_max").forGetter(d -> d.maxCultivation.get(CultivationType.BODY)),
            Codec.DOUBLE.optionalFieldOf("body_fraction", 0.0).forGetter(d -> d.cultivationFraction.get(CultivationType.BODY))
        ).apply(instance, (qiCurrent, qiMax, qiFraction, bodyCurrent, bodyMax, bodyFraction) -> {
            PlayerCultivationAttachment data = new PlayerCultivationAttachment();
            data.currentCultivation.put(CultivationType.QI, qiCurrent);
            data.maxCultivation.put(CultivationType.QI, qiMax);
            data.cultivationFraction.put(CultivationType.QI, qiFraction);
            data.currentCultivation.put(CultivationType.BODY, bodyCurrent);
            data.maxCultivation.put(CultivationType.BODY, bodyMax);
            data.cultivationFraction.put(CultivationType.BODY, bodyFraction);
            return data;
        })
    );

    /**
     * Sub-codec for cultivation rate data
     */
    private static final Codec<PlayerCultivationAttachment> RATE_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.optionalFieldOf("qi_accumulation_rate", DEFAULT_ACCUMULATION_RATE).forGetter(d -> d.accumulationRate.get(CultivationType.QI)),
            Codec.DOUBLE.optionalFieldOf("qi_additive_bonus", DEFAULT_ADDITIVE_BONUS).forGetter(d -> d.additiveBonus.get(CultivationType.QI)),
            Codec.DOUBLE.optionalFieldOf("qi_multiplicative_bonus", DEFAULT_MULTIPLICATIVE_BONUS).forGetter(d -> d.multiplicativeBonus.get(CultivationType.QI)),
            Codec.DOUBLE.optionalFieldOf("body_accumulation_rate", DEFAULT_ACCUMULATION_RATE).forGetter(d -> d.accumulationRate.get(CultivationType.BODY)),
            Codec.DOUBLE.optionalFieldOf("body_additive_bonus", DEFAULT_ADDITIVE_BONUS).forGetter(d -> d.additiveBonus.get(CultivationType.BODY)),
            Codec.DOUBLE.optionalFieldOf("body_multiplicative_bonus", DEFAULT_MULTIPLICATIVE_BONUS).forGetter(d -> d.multiplicativeBonus.get(CultivationType.BODY))
        ).apply(instance, (qiRate, qiAdd, qiMult, bodyRate, bodyAdd, bodyMult) -> {
            PlayerCultivationAttachment data = new PlayerCultivationAttachment();
            data.accumulationRate.put(CultivationType.QI, qiRate);
            data.additiveBonus.put(CultivationType.QI, qiAdd);
            data.multiplicativeBonus.put(CultivationType.QI, qiMult);
            data.accumulationRate.put(CultivationType.BODY, bodyRate);
            data.additiveBonus.put(CultivationType.BODY, bodyAdd);
            data.multiplicativeBonus.put(CultivationType.BODY, bodyMult);
            return data;
        })
    );

    /**
     * Sub-codec for cultivation statistics data
     */
    private static final Codec<PlayerCultivationAttachment> STATISTICS_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.optionalFieldOf("qi_total_earned", 0L).forGetter(d -> d.totalCultivationEarned.get(CultivationType.QI)),
            Codec.LONG.optionalFieldOf("qi_last_milestone", 0L).forGetter(d -> d.lastMilestone.get(CultivationType.QI)),
            Codec.LONG.optionalFieldOf("qi_time_in_realm", 0L).forGetter(d -> d.timeInCurrentRealm.get(CultivationType.QI)),
            Codec.LONG.optionalFieldOf("qi_total_time", 0L).forGetter(d -> d.totalCultivationTime.get(CultivationType.QI)),
            Codec.LONG.optionalFieldOf("body_total_earned", 0L).forGetter(d -> d.totalCultivationEarned.get(CultivationType.BODY)),
            Codec.LONG.optionalFieldOf("body_last_milestone", 0L).forGetter(d -> d.lastMilestone.get(CultivationType.BODY)),
            Codec.LONG.optionalFieldOf("body_time_in_realm", 0L).forGetter(d -> d.timeInCurrentRealm.get(CultivationType.BODY)),
            Codec.LONG.optionalFieldOf("body_total_time", 0L).forGetter(d -> d.totalCultivationTime.get(CultivationType.BODY))
        ).apply(instance, (qiEarned, qiMilestone, qiTimeRealm, qiTotalTime, bodyEarned, bodyMilestone, bodyTimeRealm, bodyTotalTime) -> {
            PlayerCultivationAttachment data = new PlayerCultivationAttachment();
            data.totalCultivationEarned.put(CultivationType.QI, qiEarned);
            data.lastMilestone.put(CultivationType.QI, qiMilestone);
            data.timeInCurrentRealm.put(CultivationType.QI, qiTimeRealm);
            data.totalCultivationTime.put(CultivationType.QI, qiTotalTime);
            data.totalCultivationEarned.put(CultivationType.BODY, bodyEarned);
            data.lastMilestone.put(CultivationType.BODY, bodyMilestone);
            data.timeInCurrentRealm.put(CultivationType.BODY, bodyTimeRealm);
            data.totalCultivationTime.put(CultivationType.BODY, bodyTotalTime);
            return data;
        })
    );

    /**
     * Sub-codec for focus system data
     */
    private static final Codec<PlayerCultivationAttachment> FOCUS_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("current_focus", "QI").forGetter(d -> d.currentFocus.name()),
            Codec.LONG.optionalFieldOf("last_focus_change", -FOCUS_CHANGE_COOLDOWN).forGetter(d -> d.lastFocusChangeTime)
        ).apply(instance, (focusName, lastChange) -> {
            PlayerCultivationAttachment data = new PlayerCultivationAttachment();
            try {
                data.currentFocus = CultivationType.valueOf(focusName);
            } catch (IllegalArgumentException e) {
                data.currentFocus = CultivationType.QI; // Default fallback
            }
            data.lastFocusChangeTime = lastChange;
            return data;
        })
    );

    /**
     * Sub-codec for focus system data
     */
    private static final Codec<PlayerCultivationAttachment> MEDITATION_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("isMeditating").forGetter(attachment -> attachment.isMeditating),
            Codec.INT.fieldOf("meditationTicks").forGetter(attachment -> attachment.meditationTicks),
            Codec.DOUBLE.fieldOf("currentMeditationMultiplier").forGetter(attachment -> attachment.currentMeditationMultiplier)
        ).apply(instance, (focusName, lastChange) -> {
            PlayerCultivationAttachment data = new PlayerCultivationAttachment();
            try {
                data.currentFocus = CultivationType.valueOf(focusName);
            } catch (IllegalArgumentException e) {
                data.currentFocus = CultivationType.QI; // Default fallback
            }
            data.last = lastChange;
            return data;
        })
    );

    /**
     * Main codec that combines all sub-codecs
     */
    public static final Codec<PlayerCultivationAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            REALM_CODEC.fieldOf("realm_data").forGetter(d -> d),
            PROGRESS_CODEC.fieldOf("progress_data").forGetter(d -> d),
            RATE_CODEC.fieldOf("rate_data").forGetter(d -> d),
            STATISTICS_CODEC.fieldOf("statistics_data").forGetter(d -> d),
            FOCUS_CODEC.fieldOf("focus_data").forGetter(d -> d)
        ).apply(instance, (realmData, progressData, rateData, statisticsData, focusData) -> {
            PlayerCultivationAttachment data = new PlayerCultivationAttachment();

            // Copy realm data
            data.currentRealms.putAll(realmData.currentRealms);
            data.previousRealms.putAll(realmData.previousRealms);

            // Copy progress data
            data.currentCultivation.putAll(progressData.currentCultivation);
            data.maxCultivation.putAll(progressData.maxCultivation);
            data.cultivationFraction.putAll(progressData.cultivationFraction);

            // Copy rate data
            data.accumulationRate.putAll(rateData.accumulationRate);
            data.additiveBonus.putAll(rateData.additiveBonus);
            data.multiplicativeBonus.putAll(rateData.multiplicativeBonus);

            // Copy statistics data
            data.totalCultivationEarned.putAll(statisticsData.totalCultivationEarned);
            data.lastMilestone.putAll(statisticsData.lastMilestone);
            data.timeInCurrentRealm.putAll(statisticsData.timeInCurrentRealm);
            data.totalCultivationTime.putAll(statisticsData.totalCultivationTime);

            // Copy focus data
            data.currentFocus = focusData.currentFocus;
            data.lastFocusChangeTime = focusData.lastFocusChangeTime;

            return data;
        })
    );

    // === SERIALIZER === //

    /**
     * Serializer for PlayerCultivationAttachment that handles NBT read/write operations.
     * Integrates with the NeoForge attachment system and provides debug support.
     */
    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerCultivationAttachment> {

        /**
         * Reads a PlayerCultivationAttachment from NBT data.
         * @param holder The attachment holder (typically a Player)
         * @param tag The NBT data to read from
         * @param provider The registry provider for context
         * @return The deserialized PlayerCultivationAttachment
         */
        @Override
        public PlayerCultivationAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerCultivationAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerCultivationAttachment: {}", error))
                .map(com.mojang.datafixers.util.Pair::getFirst)
                .orElseGet(PlayerCultivationAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            return attachment;
        }

        /**
         * Writes a PlayerCultivationAttachment to NBT data.
         * @param attachment The attachment to serialize
         * @param provider The registry provider for context
         * @return The NBT data, or null if serialization fails
         */
        @Override
        public @Nullable CompoundTag write(PlayerCultivationAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerCultivationAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }
}