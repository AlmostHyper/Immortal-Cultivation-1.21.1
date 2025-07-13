package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.common.interfaces.ITickable;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerBreakthroughSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Player breakthrough attachment that manages cultivation breakthrough attempts, success rates,
 * and cooldown mechanics for both Qi and Body cultivation paths.
 * Extends PlayerAttachment to inherit debugging, synchronization, and performance monitoring capabilities.
 * Implements ITickable for cooldown management and time-based breakthrough mechanics.
 */
public class PlayerBreakthroughAttachment extends PlayerAttachment<PlayerBreakthroughAttachment> implements ITickable {

    // === CONSTANTS === //

    private static final long BREAKTHROUGH_COOLDOWN_TICKS = 600L; // 30 seconds at 20 TPS
    private static final double MAX_SUCCESS_RATE = 0.95; // 95% max
    private static final double MIN_SUCCESS_RATE = 0.05; // 5% min
    private static final double FAILURE_BONUS_PER_ATTEMPT = 0.05; // 5% per failed attempt

    // === BREAKTHROUGH DATA FIELDS === //

    // --- Qi Breakthrough Data ---
    private long qiBreakthroughAttempts = 0L;
    private boolean qiBreakthroughCooldown = false;
    private long lastQiBreakthroughAttempt = 0L;

    // --- Body Breakthrough Data ---
    private long bodyBreakthroughAttempts = 0L;
    private boolean bodyBreakthroughCooldown = false;
    private long lastBodyBreakthroughAttempt = 0L;

    // === CONSTRUCTORS === //

    /**
     * Default constructor for creating a new PlayerBreakthroughAttachment with default values.
     */
    public PlayerBreakthroughAttachment() {
        super();
    }

    /**
     * Constructor with player reference for immediate association.
     * @param player The player to associate with this attachment
     */
    public PlayerBreakthroughAttachment(@Nullable Player player) {
        super();
        setPlayer(player);
    }

    // === NESTED ENUMS AND RECORDS === //

    /**
     * Enum representing different types of breakthroughs with varying base success rates.
     */
    public enum BreakthroughType {
        MINOR(0.50, "Minor Breakthrough"),
        MAJOR(0.30, "Major Breakthrough"),
        SPECIAL(0.20, "Special Breakthrough"),
        BOUNDARY(0.10, "Boundary Breakthrough");

        private final double baseSuccessChance;
        private final String displayName;

        /**
         * Creates a breakthrough type with the specified base success chance and display name.
         * @param baseSuccessChance The base success rate (0.0 to 1.0)
         * @param displayName The human-readable name for this breakthrough type
         */
        BreakthroughType(double baseSuccessChance, String displayName) {
            this.baseSuccessChance = baseSuccessChance;
            this.displayName = displayName;
        }

        /**
         * Gets the base success chance for this breakthrough type.
         * @return Base success chance as a decimal value between 0.0 and 1.0
         */
        public double getBaseSuccessChance() {
            return baseSuccessChance;
        }

        /**
         * Gets the display name for this breakthrough type.
         * @return Human-readable name
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Record representing the result of a breakthrough attempt.
     * @param success Whether the breakthrough was successful
     * @param message Human-readable message describing the result
     * @param newRealm The new cultivation realm if successful, null otherwise
     * @param type The breakthrough type that was attempted
     * @param successChance The calculated success chance that was used
     * @param attemptNumber The attempt number (how many tries it took)
     */
    public record BreakthroughResult(
        boolean success,
        String message,
        @Nullable CultivationRealm newRealm,
        BreakthroughType type,
        double successChance,
        long attemptNumber
    ) {}

    /**
     * Record representing a breakthrough option available to the player.
     * @param cultivationType The cultivation type (QI or BODY)
     * @param type The breakthrough type
     * @param currentRealm The player's current cultivation realm
     * @param targetRealm The realm that would be achieved if successful
     * @param canAttempt Whether this breakthrough can currently be attempted
     * @param successChance The calculated success chance
     * @param attempts Number of previous attempts
     * @param cooldownRemaining Remaining cooldown time in ticks
     * @param unavailabilityReason Reason why breakthrough is unavailable (if applicable)
     */
    public record BreakthroughOption(
        PlayerCultivationAttachment.CultivationType cultivationType,
        BreakthroughType type,
        CultivationRealm currentRealm,
        CultivationRealm targetRealm,
        boolean canAttempt,
        double successChance,
        long attempts,
        long cooldownRemaining,
        @Nullable String unavailabilityReason
    ) {}

    /**
     * Record containing breakthrough options organized by cultivation type.
     * @param qiOptions List of available Qi cultivation breakthrough options
     * @param bodyOptions List of available Body cultivation breakthrough options
     */
    public record BreakthroughOptions(
        List<BreakthroughOption> qiOptions,
        List<BreakthroughOption> bodyOptions
    ) {}

    /**
     * Record providing a detailed breakdown of success chance calculation.
     * @param baseChance The base success chance from the breakthrough type
     * @param failureBonus Bonus from previous failed attempts
     * @param talentBonus Bonus from player's spiritual root or physique
     * @param focusBonus Bonus from focusing on the relevant cultivation type
     * @param totalChance The final calculated success chance
     * @param attempts Number of previous attempts
     */
    public record BreakthroughChanceBreakdown(
        double baseChance,
        double failureBonus,
        double talentBonus,
        double focusBonus,
        double totalChance,
        long attempts
    ) {}

    // === PLAYERATTACHMENT IMPLEMENTATION === //

    /**
     * Initializes any player-specific behavior when a player is set.
     * @param player The player that was set
     */
    @Override
    protected void onPlayerSet(Player player) {
        if (debugEnabled()) {
            debugLog("PlayerBreakthroughAttachment associated with player: {}",
                player != null ? player.getName().getString() : "NULL");
        }
        // No additional initialization needed for breakthrough data
    }

    /**
     * Gets the codec for serialization of this specific attachment type.
     * @return The codec for PlayerBreakthroughAttachment
     */
    @Override
    protected Codec<PlayerBreakthroughAttachment> getCodec() {
        return CODEC;
    }

    /**
     * Creates a sync packet with the current attachment data.
     * @param data The NBT data to include in the packet
     * @return The appropriate sync packet
     */
    @Override
    protected ISyncPacket createSyncPacket(CompoundTag data) {
        return new PlayerBreakthroughSyncPacket(data);
    }

    // === COPY FUNCTIONALITY === //

    /**
     * Creates a deep copy of this attachment for state comparison and sync management.
     * @return A new PlayerBreakthroughAttachment instance with identical data
     */
    @Override
    public PlayerBreakthroughAttachment copy() {
        PlayerBreakthroughAttachment copy = new PlayerBreakthroughAttachment();
        copy.copyFrom(this);
        return copy;
    }

    /**
     * Copies all data from another PlayerBreakthroughAttachment instance.
     * @param other The attachment to copy data from
     */
    @Override
    protected void copyFrom(PlayerBreakthroughAttachment other) {
        // Qi breakthrough data
        this.qiBreakthroughAttempts = other.qiBreakthroughAttempts;
        this.qiBreakthroughCooldown = other.qiBreakthroughCooldown;
        this.lastQiBreakthroughAttempt = other.lastQiBreakthroughAttempt;

        // Body breakthrough data
        this.bodyBreakthroughAttempts = other.bodyBreakthroughAttempts;
        this.bodyBreakthroughCooldown = other.bodyBreakthroughCooldown;
        this.lastBodyBreakthroughAttempt = other.lastBodyBreakthroughAttempt;
    }

    /**
     * Checks if this attachment is equal to another instance for change detection.
     * Used by the base class to determine if synchronization is needed.
     * @param other The attachment to compare with
     * @return True if all data fields are equal
     */
    @Override
    protected boolean isEqualTo(PlayerBreakthroughAttachment other) {
        if (other == null) return false;

        return this.qiBreakthroughAttempts == other.qiBreakthroughAttempts &&
            this.qiBreakthroughCooldown == other.qiBreakthroughCooldown &&
            this.lastQiBreakthroughAttempt == other.lastQiBreakthroughAttempt &&
            this.bodyBreakthroughAttempts == other.bodyBreakthroughAttempts &&
            this.bodyBreakthroughCooldown == other.bodyBreakthroughCooldown &&
            this.lastBodyBreakthroughAttempt == other.lastBodyBreakthroughAttempt;
    }

    // === GENERIC BREAKTHROUGH DATA METHODS === //

    /**
     * Gets the number of breakthrough attempts for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return Number of breakthrough attempts
     */
    public long getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType type) {
        return switch (type) {
            case QI -> qiBreakthroughAttempts;
            case BODY -> bodyBreakthroughAttempts;
        };
    }

    /**
     * Sets the number of breakthrough attempts for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @param attempts The new attempt count
     */
    public void setBreakthroughAttempts(PlayerCultivationAttachment.CultivationType type, long attempts) {
        long oldValue = getBreakthroughAttempts(type);
        if (oldValue != attempts) {
            switch (type) {
                case QI -> this.qiBreakthroughAttempts = attempts;
                case BODY -> this.bodyBreakthroughAttempts = attempts;
            }
            markDirty();
        }
    }

    /**
     * Checks if breakthrough is currently on cooldown for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return True if breakthrough is on cooldown
     */
    public boolean isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType type) {
        return switch (type) {
            case QI -> qiBreakthroughCooldown;
            case BODY -> bodyBreakthroughCooldown;
        };
    }

    /**
     * Sets the breakthrough cooldown status for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @param onCooldown The new cooldown status
     */
    public void setBreakthroughCooldown(PlayerCultivationAttachment.CultivationType type, boolean onCooldown) {
        boolean oldValue = isBreakthroughOnCooldown(type);
        if (oldValue != onCooldown) {
            switch (type) {
                case QI -> this.qiBreakthroughCooldown = onCooldown;
                case BODY -> this.bodyBreakthroughCooldown = onCooldown;
            }
            markDirty();
        }
    }

    /**
     * Gets the timestamp of the last breakthrough attempt for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return Last breakthrough attempt time in ticks
     */
    public long getLastBreakthroughAttempt(PlayerCultivationAttachment.CultivationType type) {
        return switch (type) {
            case QI -> lastQiBreakthroughAttempt;
            case BODY -> lastBodyBreakthroughAttempt;
        };
    }

    /**
     * Sets the timestamp of the last breakthrough attempt for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @param timestamp The new timestamp
     */
    public void setLastBreakthroughAttempt(PlayerCultivationAttachment.CultivationType type, long timestamp) {
        long oldValue = getLastBreakthroughAttempt(type);
        if (oldValue != timestamp) {
            switch (type) {
                case QI -> this.lastQiBreakthroughAttempt = timestamp;
                case BODY -> this.lastBodyBreakthroughAttempt = timestamp;
            }
            markDirty();
        }
    }

    // === CORE BREAKTHROUGH LOGIC === //

    /**
     * Attempts a breakthrough for the specified cultivation type and breakthrough type.
     * @param cultivationType The cultivation type (QI or BODY)
     * @param breakthroughType The breakthrough type (MINOR, MAJOR, etc.)
     * @return BreakthroughResult containing success status and details
     */
    public BreakthroughResult attemptTypedBreakthrough(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) {
            return new BreakthroughResult(false, "No player reference", null, breakthroughType, 0.0, 0L);
        }

        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) {
            return new BreakthroughResult(false, "Cultivation system unavailable", null, breakthroughType, 0.0, 0L);
        }

        if (!canAttemptTypedBreakthrough(cultivationType, breakthroughType)) {
            String reason = getUnavailabilityReason(cultivationType, breakthroughType);
            return new BreakthroughResult(false, reason, null, breakthroughType, 0.0, getBreakthroughAttempts(cultivationType));
        }

        double successChance = calculateBreakthroughSuccessChance(cultivationType, breakthroughType);
        long currentAttempts = getBreakthroughAttempts(cultivationType);

        // Increment attempts before rolling
        setBreakthroughAttempts(cultivationType, currentAttempts + 1);
        setLastBreakthroughAttempt(cultivationType, player.level().getGameTime());
        setBreakthroughCooldown(cultivationType, true);

        boolean success = player.level().getRandom().nextDouble() < successChance;

        if (success) {
            CultivationRealm nextRealm = cultivation.getRealm(cultivationType).getNextCultivationRealms().get(0);
            String message = String.format("%s %s breakthrough SUCCESS! Advanced to %s",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(), nextRealm.getDisplayName());

            ImmortalCultivation.LOGGER.info("{} {} breakthrough SUCCESS for {} (attempt {}, {}% chance)",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(),
                player.getName().getString(), currentAttempts + 1, (int) (successChance * 100));

            return new BreakthroughResult(true, message, nextRealm, breakthroughType, successChance, currentAttempts + 1);
        } else {
            double nextChance = calculateBreakthroughSuccessChance(cultivationType, breakthroughType);
            String message = String.format("%s %s breakthrough failed (attempt %d, %.0f%% chance). Next: %.0f%%",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(),
                currentAttempts + 1, successChance * 100, nextChance * 100);

            ImmortalCultivation.LOGGER.info("{} {} breakthrough FAILED for {} (attempt {}, {}% chance)",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(),
                player.getName().getString(), currentAttempts + 1, (int) (successChance * 100));

            return new BreakthroughResult(false, message, null, breakthroughType, successChance, currentAttempts + 1);
        }
    }

    /**
     * Calculates the success chance for a breakthrough attempt.
     * @param cultivationType The cultivation type (QI or BODY)
     * @param breakthroughType The breakthrough type
     * @return Success chance as a value between 0.0 and 1.0
     */
    public double calculateBreakthroughSuccessChance(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        double baseChance = breakthroughType.getBaseSuccessChance();
        long attempts = getBreakthroughAttempts(cultivationType);
        double failureBonus = attempts * FAILURE_BONUS_PER_ATTEMPT;
        double talentBonus = calculateTalentBonus(cultivationType);
        double focusBonus = calculateFocusBonus(cultivationType);

        return Math.min(MAX_SUCCESS_RATE, Math.max(MIN_SUCCESS_RATE,
            baseChance + failureBonus + talentBonus + focusBonus));
    }

    /**
     * Checks if a breakthrough attempt is possible for the given cultivation type and breakthrough type.
     * @param cultivationType The cultivation type (QI or BODY)
     * @param breakthroughType The breakthrough type
     * @return True if breakthrough can be attempted
     */
    public boolean canAttemptTypedBreakthrough(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return false;

        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null || !cultivation.canBreakthrough(cultivationType) || isBreakthroughOnCooldown(cultivationType)) {
            return false;
        }

        return !cultivation.getRealm(cultivationType).getNextCultivationRealms().isEmpty();
    }

    // === HELPER METHODS === //

    /**
     * Gets the PlayerCultivationAttachment for the current player.
     * @return The cultivation attachment, or null if unavailable
     */
    private PlayerCultivationAttachment getCultivationAttachment() {
        if (player == null) return null;
        return player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
    }

    /**
     * Calculates talent-based bonus for breakthrough success chance.
     * @param cultivationType The cultivation type
     * @return Talent bonus as a decimal value
     */
    private double calculateTalentBonus(PlayerCultivationAttachment.CultivationType cultivationType) {
        if (player == null) {
            if (debugEnabled()) {
                debugLog("Cannot calculate talent bonus: player is null");
            }
            return 0.0;
        }

        PlayerTalentsAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
        if (talents == null) {
            if (debugEnabled()) {
                debugLog("Cannot calculate talent bonus: PlayerTalentsAttachment not found for player {}",
                    player.getName().getString());
            }
            return 0.0;
        }

        double bonus = cultivationType == PlayerCultivationAttachment.CultivationType.QI ?
            talents.getSpiritualRootBreakthroughBonus() : talents.getPhysiqueBreakthroughBonus();

        if (debugEnabled() && bonus > 0.0) {
            debugLog("Calculated talent breakthrough bonus for {}: {:.2f}% from {}",
                cultivationType.name(), bonus * 100,
                cultivationType == PlayerCultivationAttachment.CultivationType.QI ?
                    talents.getSpiritualRootType().getDisplayName() :
                    talents.getPhysiqueType().getDisplayName());
        }

        return bonus;
    }

    /**
     * Calculates focus-based bonus for breakthrough success chance.
     * @param cultivationType The cultivation type
     * @return Focus bonus as a decimal value
     */
    private double calculateFocusBonus(PlayerCultivationAttachment.CultivationType cultivationType) {
        if (player == null) return 0.0;

        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) return 0.0;

        return cultivation.getCurrentFocus() == cultivationType ? 0.05 : 0.0;
    }

    /**
     * Gets the reason why a breakthrough attempt is not available.
     * @param cultivationType The cultivation type
     * @param breakthroughType The breakthrough type
     * @return Human-readable reason string
     */
    private String getUnavailabilityReason(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return "No player reference";

        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) return "Cultivation system unavailable";
        if (!cultivation.canBreakthrough(cultivationType)) return cultivationType.getDisplayName() + " cultivation not at maximum";
        if (isBreakthroughOnCooldown(cultivationType)) return cultivationType.getDisplayName() + " breakthrough on cooldown";
        if (cultivation.getRealm(cultivationType).getNextCultivationRealms().isEmpty()) return "No higher realm available";

        return "Requirements not met";
    }

    // === ITICKABLE IMPLEMENTATION === //

    /**
     * Called every tick to update breakthrough cooldowns and other time-based mechanics.
     */
    @Override
    public void tick() {
        updateCooldowns();
    }

    /**
     * Updates breakthrough cooldowns for both cultivation types.
     * Removes cooldown status when the cooldown period has elapsed.
     */
    private void updateCooldowns() {
        if (player == null) return;

        long currentTime = player.level().getGameTime();

        // Check Qi breakthrough cooldown
        if (isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType.QI)) {
            long timeSinceLastAttempt = currentTime - getLastBreakthroughAttempt(PlayerCultivationAttachment.CultivationType.QI);
            if (timeSinceLastAttempt >= BREAKTHROUGH_COOLDOWN_TICKS) {
                setBreakthroughCooldown(PlayerCultivationAttachment.CultivationType.QI, false);
                if (debugEnabled()) {
                    debugLog("Qi breakthrough cooldown expired for player: {}", player.getName().getString());
                }
            }
        }

        // Check Body breakthrough cooldown
        if (isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType.BODY)) {
            long timeSinceLastAttempt = currentTime - getLastBreakthroughAttempt(PlayerCultivationAttachment.CultivationType.BODY);
            if (timeSinceLastAttempt >= BREAKTHROUGH_COOLDOWN_TICKS) {
                setBreakthroughCooldown(PlayerCultivationAttachment.CultivationType.BODY, false);
                if (debugEnabled()) {
                    debugLog("Body breakthrough cooldown expired for player: {}", player.getName().getString());
                }
            }
        }
    }

    /**
     * Gets the remaining cooldown time in ticks for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return Remaining cooldown time in ticks, or 0 if not on cooldown
     */
    public long getRemainingCooldownTicks(PlayerCultivationAttachment.CultivationType type) {
        if (!isBreakthroughOnCooldown(type) || player == null) {
            return 0L;
        }

        long currentTime = player.level().getGameTime();
        long timeSinceLastAttempt = currentTime - getLastBreakthroughAttempt(type);
        long remaining = BREAKTHROUGH_COOLDOWN_TICKS - timeSinceLastAttempt;

        return Math.max(0L, remaining);
    }

    /**
     * Gets the remaining cooldown time in seconds for the specified cultivation type.
     * @param type The cultivation type (QI or BODY)
     * @return Remaining cooldown time in seconds, or 0 if not on cooldown
     */
    public double getRemainingCooldownSeconds(PlayerCultivationAttachment.CultivationType type) {
        return getRemainingCooldownTicks(type) / 20.0; // 20 ticks per second
    }

    /**
     * Manually starts a breakthrough cooldown for the specified cultivation type.
     * Useful for testing or special breakthrough mechanics.
     * @param type The cultivation type (QI or BODY)
     */
    public void startBreakthroughCooldown(PlayerCultivationAttachment.CultivationType type) {
        if (player != null) {
            setLastBreakthroughAttempt(type, player.level().getGameTime());
            setBreakthroughCooldown(type, true);

            if (debugEnabled()) {
                debugLog("Manual breakthrough cooldown started for {} cultivation", type.getDisplayName());
            }
        }
    }

    /**
     * Manually clears a breakthrough cooldown for the specified cultivation type.
     * Useful for testing or special breakthrough mechanics.
     * @param type The cultivation type (QI or BODY)
     */
    public void clearBreakthroughCooldown(PlayerCultivationAttachment.CultivationType type) {
        setBreakthroughCooldown(type, false);

        if (debugEnabled()) {
            debugLog("Manual breakthrough cooldown cleared for {} cultivation", type.getDisplayName());
        }
    }

    // === BREAKTHROUGH OPTIONS METHODS === //

    /**
     * Gets all available breakthrough options for both cultivation types.
     * @return BreakthroughOptions containing lists of available options for QI and BODY
     */
    public BreakthroughOptions getAllAvailableBreakthroughOptions() {
        List<BreakthroughOption> qiOptions = new ArrayList<>();
        List<BreakthroughOption> bodyOptions = new ArrayList<>();

        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) {
            if (debugEnabled()) {
                debugWarn("Cannot get breakthrough options: cultivation attachment unavailable");
            }
            return new BreakthroughOptions(qiOptions, bodyOptions);
        }

        qiOptions.addAll(generateBreakthroughOptionsForType(PlayerCultivationAttachment.CultivationType.QI, cultivation));
        bodyOptions.addAll(generateBreakthroughOptionsForType(PlayerCultivationAttachment.CultivationType.BODY, cultivation));

        return new BreakthroughOptions(qiOptions, bodyOptions);
    }

    /**
     * Generates breakthrough options for a specific cultivation type.
     * @param cultivationType The cultivation type (QI or BODY)
     * @param cultivation The player's cultivation attachment
     * @return List of available breakthrough options
     */
    private List<BreakthroughOption> generateBreakthroughOptionsForType(
        PlayerCultivationAttachment.CultivationType cultivationType,
        PlayerCultivationAttachment cultivation) {
        List<BreakthroughOption> options = new ArrayList<>();

        for (BreakthroughType breakthroughType : BreakthroughType.values()) {
            BreakthroughOption option = createBreakthroughOption(cultivationType, breakthroughType, cultivation);
            if (option != null) {
                options.add(option);
            }
        }

        return options;
    }

    /**
     * Creates a breakthrough option for a specific cultivation type and breakthrough type.
     * @param cultivationType The cultivation type (QI or BODY)
     * @param breakthroughType The breakthrough type
     * @param cultivation The player's cultivation attachment
     * @return BreakthroughOption if valid, null otherwise
     */
    private BreakthroughOption createBreakthroughOption(
        PlayerCultivationAttachment.CultivationType cultivationType,
        BreakthroughType breakthroughType,
        PlayerCultivationAttachment cultivation) {

        CultivationRealm currentRealm = cultivation.getRealm(cultivationType);
        if (currentRealm.getNextCultivationRealms().isEmpty()) {
            return null; // No next realm available
        }

        CultivationRealm nextRealm = currentRealm.getNextCultivationRealms().get(0);
        boolean canAttempt = canAttemptTypedBreakthrough(cultivationType, breakthroughType);
        double successChance = calculateBreakthroughSuccessChance(cultivationType, breakthroughType);
        long attempts = getBreakthroughAttempts(cultivationType);
        long cooldownRemaining = getRemainingCooldownTicks(cultivationType);
        String unavailabilityReason = canAttempt ? null : getUnavailabilityReason(cultivationType, breakthroughType);

        return new BreakthroughOption(
            cultivationType,
            breakthroughType,
            currentRealm,
            nextRealm,
            canAttempt,
            successChance,
            attempts,
            cooldownRemaining,
            unavailabilityReason
        );
    }

    /**
     * Gets breakthrough options for a specific cultivation type only.
     * @param cultivationType The cultivation type (QI or BODY)
     * @return List of breakthrough options for the specified type
     */
    public List<BreakthroughOption> getBreakthroughOptionsForType(PlayerCultivationAttachment.CultivationType cultivationType) {
        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) {
            if (debugEnabled()) {
                debugWarn("Cannot get breakthrough options for {}: cultivation attachment unavailable",
                    cultivationType.getDisplayName());
            }
            return new ArrayList<>();
        }

        return generateBreakthroughOptionsForType(cultivationType, cultivation);
    }

    /**
     * Gets the best available breakthrough option for a specific cultivation type.
     * @param cultivationType The cultivation type (QI or BODY)
     * @return The breakthrough option with the highest success chance, or null if none available
     */
    public BreakthroughOption getBestBreakthroughOption(PlayerCultivationAttachment.CultivationType cultivationType) {
        List<BreakthroughOption> options = getBreakthroughOptionsForType(cultivationType);

        return options.stream()
            .filter(BreakthroughOption::canAttempt)
            .max((o1, o2) -> Double.compare(o1.successChance(), o2.successChance()))
            .orElse(null);
    }

    /**
     * Checks if any breakthrough options are available for the specified cultivation type.
     * @param cultivationType The cultivation type (QI or BODY)
     * @return True if at least one breakthrough option is available
     */
    public boolean hasAvailableBreakthroughOptions(PlayerCultivationAttachment.CultivationType cultivationType) {
        return getBreakthroughOptionsForType(cultivationType).stream()
            .anyMatch(BreakthroughOption::canAttempt);
    }

    // === CODEC IMPLEMENTATION === //

    /**
     * Sub-codec for Qi breakthrough data
     */
    private static final Codec<PlayerBreakthroughAttachment> QI_BREAKTHROUGH_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.optionalFieldOf("qi_attempts", 0L).forGetter(d -> d.qiBreakthroughAttempts),
            Codec.BOOL.optionalFieldOf("qi_cooldown", false).forGetter(d -> d.qiBreakthroughCooldown),
            Codec.LONG.optionalFieldOf("last_qi_attempt", 0L).forGetter(d -> d.lastQiBreakthroughAttempt)
        ).apply(instance, (qiAttempts, qiCooldown, lastQiAttempt) -> {
            PlayerBreakthroughAttachment data = new PlayerBreakthroughAttachment();
            data.qiBreakthroughAttempts = qiAttempts;
            data.qiBreakthroughCooldown = qiCooldown;
            data.lastQiBreakthroughAttempt = lastQiAttempt;
            return data;
        })
    );

    /**
     * Sub-codec for Body breakthrough data
     */
    private static final Codec<PlayerBreakthroughAttachment> BODY_BREAKTHROUGH_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.optionalFieldOf("body_attempts", 0L).forGetter(d -> d.bodyBreakthroughAttempts),
            Codec.BOOL.optionalFieldOf("body_cooldown", false).forGetter(d -> d.bodyBreakthroughCooldown),
            Codec.LONG.optionalFieldOf("last_body_attempt", 0L).forGetter(d -> d.lastBodyBreakthroughAttempt)
        ).apply(instance, (bodyAttempts, bodyCooldown, lastBodyAttempt) -> {
            PlayerBreakthroughAttachment data = new PlayerBreakthroughAttachment();
            data.bodyBreakthroughAttempts = bodyAttempts;
            data.bodyBreakthroughCooldown = bodyCooldown;
            data.lastBodyBreakthroughAttempt = lastBodyAttempt;
            return data;
        })
    );

    /**
     * Main codec that combines all breakthrough data
     */
    public static final Codec<PlayerBreakthroughAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            QI_BREAKTHROUGH_CODEC.fieldOf("qi_breakthrough").forGetter(d -> d),
            BODY_BREAKTHROUGH_CODEC.fieldOf("body_breakthrough").forGetter(d -> d)
        ).apply(instance, (qiData, bodyData) -> {
            PlayerBreakthroughAttachment data = new PlayerBreakthroughAttachment();

            // Copy qi data
            data.qiBreakthroughAttempts = qiData.qiBreakthroughAttempts;
            data.qiBreakthroughCooldown = qiData.qiBreakthroughCooldown;
            data.lastQiBreakthroughAttempt = qiData.lastQiBreakthroughAttempt;

            // Copy body data
            data.bodyBreakthroughAttempts = bodyData.bodyBreakthroughAttempts;
            data.bodyBreakthroughCooldown = bodyData.bodyBreakthroughCooldown;
            data.lastBodyBreakthroughAttempt = bodyData.lastBodyBreakthroughAttempt;

            return data;
        })
    );

    // === SERIALIZER === //

    /**
     * Serializer for PlayerBreakthroughAttachment that handles NBT read/write operations.
     * Integrates with the NeoForge attachment system and provides debug support.
     */
    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerBreakthroughAttachment> {

        /**
         * Reads a PlayerBreakthroughAttachment from NBT data.
         * @param holder The attachment holder (typically a Player)
         * @param tag The NBT data to read from
         * @param provider The registry provider for context
         * @return The deserialized PlayerBreakthroughAttachment
         */
        @Override
        public PlayerBreakthroughAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerBreakthroughAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerBreakthroughAttachment: {}", error))
                .map(com.mojang.datafixers.util.Pair::getFirst)
                .orElseGet(PlayerBreakthroughAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            return attachment;
        }

        /**
         * Writes a PlayerBreakthroughAttachment to NBT data.
         * @param attachment The attachment to serialize
         * @param provider The registry provider for context
         * @return The NBT data, or null if serialization fails
         */
        @Override
        public @Nullable CompoundTag write(PlayerBreakthroughAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerBreakthroughAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }
}

