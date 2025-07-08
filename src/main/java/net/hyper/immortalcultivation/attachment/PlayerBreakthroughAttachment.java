package net.hyper.immortalcultivation.attachment;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.helper.DirtyTrackerHelper;
import net.hyper.immortalcultivation.interfaces.ITickable;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.cultivation.CultivationRealm;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all breakthrough-related logic and state management for players.
 * Extracted from PlayerCultivationAttachment for better separation of concerns.
 *
 * Integrates with the Dirty Flag system for efficient synchronization.
 * Works with ICModTickEvent for consolidated processing.
 */
public class PlayerBreakthroughAttachment extends DirtyTrackerHelper implements ITickable {

    // === CONSTANTS === //
    private static final long BREAKTHROUGH_COOLDOWN_TICKS = 600L; // 30 seconds at 20 TPS
    private static final double MAX_SUCCESS_RATE = 0.95; // 95% max
    private static final double MIN_SUCCESS_RATE = 0.05; // 5% min
    private static final double FAILURE_BONUS_PER_ATTEMPT = 0.05; // 5% per failed attempt

    // === PLAYER REFERENCE === //
    private Player player;

    // === BREAKTHROUGH ATTEMPT TRACKING === //
    private long qiBreakthroughAttempts = 0L;
    private long bodyBreakthroughAttempts = 0L;

    // === COOLDOWN MANAGEMENT === //
    private boolean qiBreakthroughCooldown = false;
    private boolean bodyBreakthroughCooldown = false;
    private long lastQiBreakthroughAttempt = 0L;
    private long lastBodyBreakthroughAttempt = 0L;

    // === BREAKTHROUGH TYPE SYSTEM === //

    /**
     * Different types of breakthroughs with varying difficulty and success rates
     */
    public enum BreakthroughType {
        MINOR(0.50, "Minor Breakthrough"),      // 50% base chance - small realm advancement
        MAJOR(0.30, "Major Breakthrough"),      // 30% base chance - significant realm advancement
        SPECIAL(0.20, "Special Breakthrough"),  // 20% base chance - unique cultivation paths
        BOUNDARY(0.10, "Boundary Breakthrough"); // 10% base chance - major stage transitions

        private final double baseSuccessChance;
        private final String displayName;

        BreakthroughType(double baseSuccessChance, String displayName) {
            this.baseSuccessChance = baseSuccessChance;
            this.displayName = displayName;
        }

        public double getBaseSuccessChance() {
            return baseSuccessChance;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Breakthrough result with detailed information
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
     * Breakthrough option for UI display
     */
    public record BreakthroughOption(
        BreakthroughType type,
        CultivationRealm targetRealm,
        double successChance,
        PlayerCultivationAttachment.CultivationType cultivationType,
        boolean available,
        String unavailabilityReason
    ) {}

    /**
     * Container for all available breakthrough options
     */
    public record BreakthroughOptions(
        List<BreakthroughOption> qiOptions,
        List<BreakthroughOption> bodyOptions
    ) {}

    /**
     * Detailed breakdown of breakthrough success chance calculation
     */
    public record BreakthroughChanceBreakdown(
        double baseChance,
        double failureBonus,
        double talentBonus,
        double focusBonus,
        double totalChance,
        long attempts
    ) {}

    // === CONSTRUCTORS ===

    public PlayerBreakthroughAttachment() {
        // Default constructor for attachment system
    }

    public PlayerBreakthroughAttachment(Player player) {
        this.player = player;
    }

    // === PLAYER REFERENCE MANAGEMENT ===

    public void setPlayer(Player player) {
        this.player = player;
        ImmortalCultivation.LOGGER.debug("Set player reference for PlayerBreakthroughAttachment: {}",
            player != null ? player.getName().getString() : "null");
    }

    public Player getPlayer() {
        return player;
    }

    public Player getPlayerSafe() {
        return player;
    }

    // === GETTERS ===

    public long getQiBreakthroughAttempts() {
        return qiBreakthroughAttempts;
    }

    public long getBodyBreakthroughAttempts() {
        return bodyBreakthroughAttempts;
    }

    public boolean isQiBreakthroughOnCooldown() {
        return qiBreakthroughCooldown;
    }

    public boolean isBodyBreakthroughOnCooldown() {
        return bodyBreakthroughCooldown;
    }

    public boolean isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType type) {
        return (type == PlayerCultivationAttachment.CultivationType.QI) ? qiBreakthroughCooldown : bodyBreakthroughCooldown;
    }

    public long getTimeSinceLastBreakthroughAttempt(PlayerCultivationAttachment.CultivationType type) {
        if (player == null) return Long.MAX_VALUE;

        long lastAttempt = (type == PlayerCultivationAttachment.CultivationType.QI) ?
            lastQiBreakthroughAttempt : lastBodyBreakthroughAttempt;
        return player.level().getGameTime() - lastAttempt;
    }

    // === CULTIVATION ATTACHMENT INTEGRATION ===

    private PlayerCultivationAttachment getCultivationAttachment() {
        if (player == null) return null;

        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivation != null && cultivation.getPlayerSafe() == null) {
            cultivation.setPlayer(player);
        }
        return cultivation;
    }

    private PlayerTalentAttachment getTalentAttachment() {
        if (player == null) return null;
        return player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
    }

    // === BREAKTHROUGH SYSTEM IMPLEMENTATION ===

    /**
     * COMPATIBILITY: Simple breakthrough methods using MINOR type
     */
    public BreakthroughResult attemptQiBreakthrough() {
        return attemptTypedBreakthrough(PlayerCultivationAttachment.CultivationType.QI, BreakthroughType.MINOR);
    }

    public BreakthroughResult attemptBodyBreakthrough() {
        return attemptTypedBreakthrough(PlayerCultivationAttachment.CultivationType.BODY, BreakthroughType.MINOR);
    }

    /**
     * Main breakthrough logic with type support
     */
    public BreakthroughResult attemptTypedBreakthrough(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) {
            return new BreakthroughResult(false, "No player reference", null, breakthroughType, 0.0, 0L);
        }

        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) {
            return new BreakthroughResult(false, "Cultivation system unavailable", null, breakthroughType, 0.0, 0L);
        }

        // Check basic requirements
        if (!canAttemptTypedBreakthrough(cultivationType, breakthroughType)) {
            String reason = getUnavailabilityReason(cultivationType, breakthroughType);
            return new BreakthroughResult(false, reason, null, breakthroughType, 0.0,
                cultivationType == PlayerCultivationAttachment.CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts);
        }

        // Get current realm and next realms
        CultivationRealm currentRealm = cultivation.getCurrentRealm(cultivationType);
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();

        if (nextRealms.isEmpty()) {
            return new BreakthroughResult(false, "No higher realm available", null, breakthroughType, 0.0,
                cultivationType == PlayerCultivationAttachment.CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts);
        }

        // Get target realm (for now, just use the first available)
        CultivationRealm targetRealm = nextRealms.get(0);

        // Calculate success chance
        double successChance = calculateBreakthroughSuccessChance(cultivationType, breakthroughType);

        // Record attempt
        long currentAttempts;
        if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
            lastQiBreakthroughAttempt = player.level().getGameTime();
            currentAttempts = qiBreakthroughAttempts;
        } else {
            lastBodyBreakthroughAttempt = player.level().getGameTime();
            currentAttempts = bodyBreakthroughAttempts;
        }

        // Roll for success
        boolean success = player.getRandom().nextDouble() < successChance;

        if (success) {
            // SUCCESS: Advance to new realm and reset attempts
            cultivation.setCurrentRealm(cultivationType, targetRealm);

            if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
                qiBreakthroughAttempts = 0L;
            } else {
                bodyBreakthroughAttempts = 0L;
            }

            markDirty();
            forceSync();

            String message = String.format("%s %s breakthrough successful! (%d attempts, %.0f%% chance)",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(),
                currentAttempts, successChance * 100);

            ImmortalCultivation.LOGGER.info("{} {} breakthrough SUCCESS for {} after {} attempts ({}% chance)",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(),
                player.getName().getString(), currentAttempts, (int)(successChance * 100));

            return new BreakthroughResult(true, message, targetRealm, breakthroughType, successChance, currentAttempts);

        } else {
            // FAILURE: Increment attempts and set cooldown
            if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
                qiBreakthroughAttempts++;
                qiBreakthroughCooldown = true;
            } else {
                bodyBreakthroughAttempts++;
                bodyBreakthroughCooldown = true;
            }

            markDirty();
            forceSync();

            // Calculate next attempt chance for the message
            double nextChance = calculateBreakthroughSuccessChance(cultivationType, breakthroughType);

            String message = String.format("%s %s breakthrough failed (attempt %d, %.0f%% chance). Next: %.0f%%",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(),
                currentAttempts + 1, successChance * 100, nextChance * 100);

            ImmortalCultivation.LOGGER.info("{} {} breakthrough FAILED for {} (attempt {}, {}% chance)",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(),
                player.getName().getString(), currentAttempts + 1, (int)(successChance * 100));

            return new BreakthroughResult(false, message, null, breakthroughType, successChance, currentAttempts + 1);
        }
    }

    /**
     * Calculate breakthrough success chance with all bonuses
     */
    public double calculateBreakthroughSuccessChance(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        double baseChance = breakthroughType.getBaseSuccessChance();
        long attempts = (cultivationType == PlayerCultivationAttachment.CultivationType.QI) ?
            qiBreakthroughAttempts : bodyBreakthroughAttempts;

        double failureBonus = attempts * FAILURE_BONUS_PER_ATTEMPT;
        double talentBonus = calculateTalentBonus(cultivationType);
        double focusBonus = calculateFocusBonus(cultivationType);

        double totalChance = Math.min(MAX_SUCCESS_RATE, Math.max(MIN_SUCCESS_RATE,
            baseChance + failureBonus + talentBonus + focusBonus));

        return totalChance;
    }

    /**
     * Calculate talent-based bonus for breakthrough success
     */
    private double calculateTalentBonus(PlayerCultivationAttachment.CultivationType cultivationType) {
        PlayerTalentAttachment talents = getTalentAttachment();
        if (talents == null) return 0.0;

        // Apply talent bonuses - this would be implemented based on your talent system
        // For now, return 0.0 as placeholder
        return 0.0;
    }

    /**
     * Calculate focus-based bonus for breakthrough success
     */
    private double calculateFocusBonus(PlayerCultivationAttachment.CultivationType cultivationType) {
        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) return 0.0;

        PlayerCultivationAttachment.CultivationType currentFocus = cultivation.getCurrentFocus();
        return (currentFocus == cultivationType) ? 0.05 : 0.0; // 5% bonus for focused cultivation
    }

    /**
     * Check if a specific type of breakthrough can be attempted
     */
    public boolean canAttemptTypedBreakthrough(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return false;

        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) return false;

        // Check basic requirements
        if (!cultivation.canBreakthrough(cultivationType)) return false;

        // Check cooldown
        if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
            if (qiBreakthroughCooldown) return false;
        } else {
            if (bodyBreakthroughCooldown) return false;
        }

        // Check if there are available advancement paths
        CultivationRealm currentRealm = cultivation.getCurrentRealm(cultivationType);
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();

        if (nextRealms.isEmpty()) return false;

        // Additional type-specific requirements could be added here
        return true;
    }

    /**
     * Get the reason why a breakthrough type is not available
     */
    private String getUnavailabilityReason(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return "No player reference";

        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) return "Cultivation system unavailable";

        if (!cultivation.canBreakthrough(cultivationType)) {
            return cultivationType.getDisplayName() + " cultivation not at maximum";
        }

        if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
            if (qiBreakthroughCooldown) {
                return "Qi breakthrough on cooldown";
            }
        } else {
            if (bodyBreakthroughCooldown) {
                return "Body breakthrough on cooldown";
            }
        }

        CultivationRealm currentRealm = cultivation.getCurrentRealm(cultivationType);
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();

        if (nextRealms.isEmpty()) {
            return "No higher realm available";
        }

        return "Requirements not met";
    }

    /**
     * Get detailed breakdown of success rate calculation for UI display
     */
    public BreakthroughChanceBreakdown getBreakthroughChanceBreakdown(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        double baseChance = breakthroughType.getBaseSuccessChance();
        long attempts = (cultivationType == PlayerCultivationAttachment.CultivationType.QI) ?
            qiBreakthroughAttempts : bodyBreakthroughAttempts;

        double failureBonus = attempts * FAILURE_BONUS_PER_ATTEMPT;
        double talentBonus = calculateTalentBonus(cultivationType);
        double focusBonus = calculateFocusBonus(cultivationType);

        double totalChance = Math.min(MAX_SUCCESS_RATE, Math.max(MIN_SUCCESS_RATE,
            baseChance + failureBonus + talentBonus + focusBonus));

        return new BreakthroughChanceBreakdown(
            baseChance, failureBonus, talentBonus, focusBonus, totalChance, attempts
        );
    }

    /**
     * Calculate actual breakthrough success rate as used by the server.
     * This matches the exact calculation used in attemptQiBreakthrough() and attemptBodyBreakthrough().
     */
    public double calculateBreakthroughSuccessRate(PlayerCultivationAttachment.CultivationType type) {
        return calculateBreakthroughSuccessChance(type, BreakthroughType.MINOR);
    }

    public double calculateBreakthroughSuccessRate(PlayerCultivationAttachment.CultivationType type, BreakthroughType breakthroughType) {
        return calculateBreakthroughSuccessChance(type, breakthroughType);
    }

    // === COOLDOWN MANAGEMENT ===

    private void updateCooldowns() {
        if (player == null) return;

        long currentTime = player.level().getGameTime();

        if (qiBreakthroughCooldown && (currentTime - lastQiBreakthroughAttempt) >= BREAKTHROUGH_COOLDOWN_TICKS) {
            qiBreakthroughCooldown = false;
            markDirty();
        }

        if (bodyBreakthroughCooldown && (currentTime - lastBodyBreakthroughAttempt) >= BREAKTHROUGH_COOLDOWN_TICKS) {
            bodyBreakthroughCooldown = false;
            markDirty();
        }
    }

    // === ITICKABLE IMPLEMENTATION ===

    @Override
    public void tick() {
        if (player == null || !player.level().isClientSide()) {
            updateCooldowns();
        }
    }

    // === DIRTY FLAG IMPLEMENTATION === //

    @Override
    public boolean shouldSyncNow() {
        // Sync immediately after breakthrough attempts or when cooldown changes
        return isDirty() && (shouldSyncByTime(1000L) || shouldSyncByCount(1L));
    }

    @Override
    public void forceSync() {
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                // Create and send breakthrough sync packet
                // Note: This will need to be implemented when we create the specific sync packet
                markDirtyAndIncrement();
                clearDirty();

                ImmortalCultivation.LOGGER.debug("Synced breakthrough data for player: {}",
                    serverPlayer.getName().getString());
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Failed to sync breakthrough data for player: {}",
                    serverPlayer.getName().getString(), e);
            }
        }
    }

    // === SERIALIZATION === //

    private static PlayerBreakthroughAttachment decode(
        long qiAttempts, long bodyAttempts,
        boolean qiCooldown, boolean bodyCooldown,
        long lastQiAttempt, long lastBodyAttempt
    ) {
        PlayerBreakthroughAttachment attachment = new PlayerBreakthroughAttachment();
        attachment.qiBreakthroughAttempts = qiAttempts;
        attachment.bodyBreakthroughAttempts = bodyAttempts;
        attachment.qiBreakthroughCooldown = qiCooldown;
        attachment.bodyBreakthroughCooldown = bodyCooldown;
        attachment.lastQiBreakthroughAttempt = lastQiAttempt;
        attachment.lastBodyBreakthroughAttempt = lastBodyAttempt;
        attachment.markDirty(); // Always dirty after loading
        return attachment;
    }

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerBreakthroughAttachment> {
        @Override
        public PlayerBreakthroughAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerBreakthroughAttachment attachment = CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to deserialize PlayerBreakthroughAttachment: {}", error))
                .orElse(new PlayerBreakthroughAttachment());

            // Set player reference during deserialization
            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            return attachment;
        }

        @Override
        public CompoundTag write(PlayerBreakthroughAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, attachment)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to serialize PlayerBreakthroughAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }

    // === CODEC === //

    public static final Codec<PlayerBreakthroughAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.optionalFieldOf("qi_attempts", 0L).forGetter(a -> a.qiBreakthroughAttempts),
            Codec.LONG.optionalFieldOf("body_attempts", 0L).forGetter(a -> a.bodyBreakthroughAttempts),
            Codec.BOOL.optionalFieldOf("qi_cooldown", false).forGetter(a -> a.qiBreakthroughCooldown),
            Codec.BOOL.optionalFieldOf("body_cooldown", false).forGetter(a -> a.bodyBreakthroughCooldown),
            Codec.LONG.optionalFieldOf("last_qi_attempt", 0L).forGetter(a -> a.lastQiBreakthroughAttempt),
            Codec.LONG.optionalFieldOf("last_body_attempt", 0L).forGetter(a -> a.lastBodyBreakthroughAttempt)
        ).apply(instance, PlayerBreakthroughAttachment::decode)
    );
}