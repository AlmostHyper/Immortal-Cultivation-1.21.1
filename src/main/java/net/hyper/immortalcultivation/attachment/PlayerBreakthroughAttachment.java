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
     * Cultivation types for breakthrough system
     */
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
        CultivationType cultivationType,
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

    public boolean isBreakthroughOnCooldown(CultivationType type) {
        return (type == CultivationType.QI) ? qiBreakthroughCooldown : bodyBreakthroughCooldown;
    }

    public long getTimeSinceLastBreakthroughAttempt(CultivationType type) {
        if (player == null) return Long.MAX_VALUE;

        long lastAttempt = (type == CultivationType.QI) ? lastQiBreakthroughAttempt : lastBodyBreakthroughAttempt;
        return player.level().getGameTime() - lastAttempt;
    }

    // === PLACEHOLDER METHODS ===
    // === Breakthrough System Implementation ===

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
     * Get the number of failed Qi breakthrough attempts for this realm.
     * Used for calculating breakthrough success rate bonus.
     */
    public long getQiBreakthroughAttempts() {
        return qiBreakthroughAttempts;
    }

    /**
     * Get the number of failed Body breakthrough attempts for this realm.
     * Used for calculating breakthrough success rate bonus.
     */
    public long getBodyBreakthroughAttempts() {
        return bodyBreakthroughAttempts;
    }

    /**
     * Calculate actual breakthrough success rate as used by the server.
     * This matches the exact calculation used in attemptQiBreakthrough() and attemptBodyBreakthrough().
     *
     * @param type The cultivation type to calculate for
     * @return The success rate as a value between 0.0 and 1.0
     */
    public double calculateBreakthroughSuccessRate(PlayerCultivationAttachment.CultivationType type) {
        // Use MINOR breakthrough type for the standard calculation
        return calculateBreakthroughSuccessChance(type, BreakthroughType.MINOR);
    }

    /**
     * Get success rate for a specific breakthrough type
     */
    public double calculateBreakthroughSuccessRate(PlayerCultivationAttachment.CultivationType type, BreakthroughType breakthroughType) {
        return calculateBreakthroughSuccessChance(type, breakthroughType);
    }

    /**
     * Check if breakthrough is on cooldown for the specified type.
     */
    public boolean isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType type) {
        return (type == PlayerCultivationAttachment.CultivationType.QI) ? qiBreakthroughCooldown : bodyBreakthroughCooldown;
    }

    /**
     * Get time since last breakthrough attempt for the specified type.
     */
    public long getTimeSinceLastBreakthroughAttempt(PlayerCultivationAttachment.CultivationType type) {
        if (player == null) return Long.MAX_VALUE;

        long lastAttempt = (type == PlayerCultivationAttachment.CultivationType.QI) ? lastQiBreakthroughAttempt : lastBodyBreakthroughAttempt;
        return player.level().getGameTime() - lastAttempt;
    }

    /**
     * Attempt breakthrough with specific type for better control
     */
    public BreakthroughResult attemptTypedBreakthrough(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) {
            return new BreakthroughResult(false, "No player reference", null, breakthroughType, 0.0, 0L);
        }

        // Check basic requirements first
        if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
            if (currentQiCultivation < maxQiCultivation) {
                return new BreakthroughResult(false, "Qi cultivation not at maximum", null, breakthroughType, 0.0, qiBreakthroughAttempts);
            }
            if (qiBreakthroughCooldown) {
                return new BreakthroughResult(false, "Qi breakthrough on cooldown", null, breakthroughType, 0.0, qiBreakthroughAttempts);
            }
        } else {
            if (currentBodyCultivation < maxBodyCultivation) {
                return new BreakthroughResult(false, "Body cultivation not at maximum", null, breakthroughType, 0.0, bodyBreakthroughAttempts);
            }
            if (bodyBreakthroughCooldown) {
                return new BreakthroughResult(false, "Body breakthrough on cooldown", null, breakthroughType, 0.0, bodyBreakthroughAttempts);
            }
        }

        // Get current realm and check for available breakthrough paths
        CultivationRealm currentRealm = cultivationType == PlayerCultivationAttachment.CultivationType.QI ? currentQiRealm : currentBodyRealm;
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();

        if (nextRealms.isEmpty()) {
            return new BreakthroughResult(false, "No higher realm available", null, breakthroughType, 0.0,
                cultivationType == PlayerCultivationAttachment.CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts);
        }

        // Get target realm (for now, just use the first available - can be enhanced later)
        CultivationRealm targetRealm = nextRealms.get(0);

        // Calculate success chance combining base type chance with failure bonus
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
            if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
                setCurrentQiRealm(targetRealm); // This resets attempts to 0
            } else {
                setCurrentBodyRealm(targetRealm); // This resets attempts to 0
            }

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

            forceSync();

            // Calculate next attempt chance for the message
            double nextChance = calculateBreakthroughSuccessChance(cultivationType, breakthroughType);

            String message = String.format("%s %s breakthrough failed (attempt %d, %.0f%% chance). Next attempt: %.0f%% chance",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(),
                cultivationType == PlayerCultivationAttachment.CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts,
                successChance * 100, nextChance * 100);

            ImmortalCultivation.LOGGER.info("{} {} breakthrough FAILED for {} - attempt {} ({}% chance, next: {}%)",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(),
                player.getName().getString(),
                cultivationType == PlayerCultivationAttachment.CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts,
                (int)(successChance * 100), (int)(nextChance * 100));

            return new BreakthroughResult(false, message, null, breakthroughType, successChance,
                cultivationType == PlayerCultivationAttachment.CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts);
        }
    }

    /**
     * Calculate success chance combining breakthrough type with failure bonus
     */
    private double calculateBreakthroughSuccessChance(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        // Base chance from breakthrough type
        double baseChance = breakthroughType.getBaseSuccessChance();

        // Add failure bonus (5% per failed attempt)
        long attempts = (cultivationType == PlayerCultivationAttachment.CultivationType.QI) ? qiBreakthroughAttempts : bodyBreakthroughAttempts;
        double failureBonus = attempts * 0.05;

        // Calculate total chance
        double totalChance = baseChance + failureBonus;

        // Apply talent bonuses if available
        totalChance = applyTalentBonuses(cultivationType, totalChance);

        // Apply focus bonus
        totalChance = applyFocusBonus(cultivationType, totalChance);

        // Cap at 95% max success rate
        return Math.min(0.95, Math.max(0.05, totalChance)); // Min 5%, Max 95%
    }

    /**
     * Apply talent-based bonuses to breakthrough success rate
     */
    private double applyTalentBonuses(PlayerCultivationAttachment.CultivationType cultivationType, double currentChance) {
        if (player == null) return currentChance;

        try {
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talentAttachment != null) {
                if (cultivationType == PlayerCultivationAttachment.CultivationType.QI && talentAttachment.hasSpiritualRootAssigned()) {
                    float multiplier = talentAttachment.getSpiritualRootMultiplier();
                    // Each 0.1x talent adds 2% breakthrough success rate
                    double bonus = (multiplier - 1.0) * 0.2;
                    return currentChance + bonus;
                } else if (cultivationType == PlayerCultivationAttachment.CultivationType.BODY && talentAttachment.hasPhysiqueAssigned()) {
                    float multiplier = talentAttachment.getPhysiqueMultiplier();
                    // Each 0.1x talent adds 2% breakthrough success rate
                    double bonus = (multiplier - 1.0) * 0.2;
                    return currentChance + bonus;
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying talent bonuses to breakthrough", e);
        }

        return currentChance;
    }

    /**
     * Apply focus bonus to breakthrough success rate
     */
    private double applyFocusBonus(PlayerCultivationAttachment.CultivationType cultivationType, double currentChance) {
        // If player is focused on this cultivation type, add 5% bonus
        if (currentFocus == cultivationType) {
            return currentChance + 0.05;
        }
        return currentChance;
    }

    /**
     * Get all available breakthrough options for UI display
     */
    public BreakthroughOptions getAllAvailableBreakthroughOptions() {
        List<BreakthroughOption> qiOptions = getBreakthroughOptionsForType(PlayerCultivationAttachment.CultivationType.QI);
        List<BreakthroughOption> bodyOptions = getBreakthroughOptionsForType(PlayerCultivationAttachment.CultivationType.BODY);

        return new BreakthroughOptions(qiOptions, bodyOptions);
    }

    /**
     * Get breakthrough options for a specific cultivation type
     */
    public List<BreakthroughOption> getBreakthroughOptionsForType(PlayerCultivationAttachment.CultivationType cultivationType) {
        List<BreakthroughOption> options = new ArrayList<>();

        if (player == null) {
            return options;
        }

        CultivationRealm currentRealm = (cultivationType == PlayerCultivationAttachment.CultivationType.QI) ? currentQiRealm : currentBodyRealm;
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();

        if (nextRealms.isEmpty()) {
            return options;
        }

        // For now, use the first available realm for all breakthrough types
        // In a more advanced system, different breakthrough types could lead to different realms
        CultivationRealm targetRealm = nextRealms.get(0);

        for (BreakthroughType type : BreakthroughType.values()) {
            boolean available = canAttemptTypedBreakthrough(cultivationType, type);
            String unavailabilityReason = available ? "" : getUnavailabilityReason(cultivationType, type);
            double successChance = available ? calculateBreakthroughSuccessChance(cultivationType, type) : 0.0;

            options.add(new BreakthroughOption(
                type,
                targetRealm,
                successChance,
                cultivationType,
                available,
                unavailabilityReason
            ));
        }

        return options;
    }

    /**
     * Check if a specific type of breakthrough can be attempted
     */
    public boolean canAttemptTypedBreakthrough(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return false;

        // Check basic requirements
        if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
            if (currentQiCultivation < maxQiCultivation) return false;
            if (qiBreakthroughCooldown) return false;
        } else {
            if (currentBodyCultivation < maxBodyCultivation) return false;
            if (bodyBreakthroughCooldown) return false;
        }

        // Check if there are available advancement paths
        CultivationRealm currentRealm = (cultivationType == PlayerCultivationAttachment.CultivationType.QI) ? currentQiRealm : currentBodyRealm;
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();

        if (nextRealms.isEmpty()) return false;

        // Additional type-specific requirements could be added here
        // For example, SPECIAL breakthroughs might require certain items or conditions
        switch (breakthroughType) {
            case SPECIAL:
                // Could require special cultivation techniques or items
                return true; // For now, allow if basic requirements are met
            case BOUNDARY:
                // Could require specific realm level or major stage transitions
                return true; // For now, allow if basic requirements are met
            default:
                return true;
        }
    }

    /**
     * Get the reason why a breakthrough type is not available
     */
    private String getUnavailabilityReason(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return "No player reference";

        if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
            if (currentQiCultivation < maxQiCultivation) {
                return "Qi cultivation not at maximum";
            }
            if (qiBreakthroughCooldown) {
                return "Qi breakthrough on cooldown";
            }
        } else {
            if (currentBodyCultivation < maxBodyCultivation) {
                return "Body cultivation not at maximum";
            }
            if (bodyBreakthroughCooldown) {
                return "Body breakthrough on cooldown";
            }
        }

        CultivationRealm currentRealm = (cultivationType == PlayerCultivationAttachment.CultivationType.QI) ? currentQiRealm : currentBodyRealm;
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
        long attempts = (cultivationType == PlayerCultivationAttachment.CultivationType.QI) ? qiBreakthroughAttempts : bodyBreakthroughAttempts;
        double failureBonus = attempts * 0.05;
        double talentBonus = applyTalentBonuses(cultivationType, 0.0); // Apply to 0 to get just the bonus
        double focusBonus = (currentFocus == cultivationType) ? 0.05 : 0.0;

        double totalChance = Math.min(0.95, Math.max(0.05, baseChance + failureBonus + talentBonus + focusBonus));

        return new BreakthroughChanceBreakdown(
            baseChance, failureBonus, talentBonus, focusBonus, totalChance, attempts
        );
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
            return CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to deserialize PlayerBreakthroughAttachment: {}", error))
                .orElse(new PlayerBreakthroughAttachment());
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