package net.hyper.immortalcultivation.attachment;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IBodyCultivationData;
import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.cultivation.realms.data.CultivationRealmData;
import net.hyper.immortalcultivation.cultivation.realms.data.JsonCultivationRealm;
import net.hyper.immortalcultivation.network.server.CultivationSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Unified cultivation data attachment.
 * Merges: PlayerRealmAttachment, PlayerQiCultivationAttachment,
 * PlayerBodyCultivationAttachment, PlayerCultivationFocusAttachment,
 * and PlayerCultivationBonusAttachment
 * Enhanced with comprehensive pill system integration and breakthrough types.
 */
public class PlayerCultivationAttachment implements IQiCultivationData, IBodyCultivationData {

    // === Default Values (fallback when JSON isn't loaded yet) ===
    private static final long DEFAULT_MAX_CULTIVATION = 100L;
    private static final String DEFAULT_MORTAL_REALM_ID = "mortal";

    // === Player Reference ===
    @Nullable
    private Player player;

    // === Realm Data (from PlayerRealmAttachment) ===
    private CultivationRealm currentQiRealm;
    private CultivationRealm currentBodyRealm;
    @Nullable
    private CultivationRealm previousQiRealm;
    @Nullable
    private CultivationRealm previousBodyRealm;

    // === Qi Cultivation Data (from PlayerQiCultivationAttachment) ===
    private long currentQiCultivation = 0;
    private long maxQiCultivation = 1000; // Start with default max
    private double qiCultivationFraction = 0.0;
    private double qiAccumulationRate = 0.0; // CRITICAL: This needs to be updated each tick

    // === Body Cultivation Data (from PlayerBodyCultivationAttachment) ===
    private long currentBodyCultivation = 0;
    private long maxBodyCultivation = 1000; // Start with default max
    private double bodyCultivationFraction = 0.0;
    private double bodyAccumulationRate = 0.0; // CRITICAL: This needs to be updated each tick

    public void startMeditation() {

    }

    public void stopMeditation() {
    }

    // === Focus System (from PlayerCultivationFocusAttachment) ===
    public enum CultivationType {
        QI("Qi"),
        BODY("Body");

        private final String displayName;

        CultivationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    private CultivationType currentFocus = CultivationType.QI;
    private long lastFocusChangeTime = -24000;
    private static final long FOCUS_CHANGE_COOLDOWN = 24000;

    // === Breakthrough Tracking ===
    private long qiBreakthroughAttempts = 0;
    private long bodyBreakthroughAttempts = 0;
    private long lastQiBreakthroughAttempt = 0;
    private long lastBodyBreakthroughAttempt = 0;
    private boolean qiBreakthroughCooldown = false;
    private boolean bodyBreakthroughCooldown = false;

    // === Time Tracking ===
    private long timeInCurrentQiRealm = 0;
    private long timeInCurrentBodyRealm = 0;
    private long totalQiCultivationTime = 0;
    private long totalBodyCultivationTime = 0;

    // === Bonus System (from PlayerCultivationBonusAttachment) ===
    private double qiCultivationAdditiveBonus = 0.0;
    private double qiCultivationMultiplicativeBonus = 0.0;
    private double bodyCultivationAdditiveBonus = 0.0;
    private double bodyCultivationMultiplicativeBonus = 0.0;

    // === Sync Management ===
    private long lastSyncedHash = 0;
    private int tickCounter = 0;
    private static final int SYNC_INTERVAL = 20;

    // === Breakthrough Type System ===

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
    ) {
        // Compatibility constructor for simple results
        public BreakthroughResult(boolean success, String message, @Nullable CultivationRealm newRealm) {
            this(success, message, newRealm, BreakthroughType.MINOR, 0.0, 0L);
        }
    }

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

    /**
     * Detailed breakdown of success chance calculation for UI
     */
    public record BreakthroughChanceBreakdown(
        double baseChance,
        double failureBonus,
        double talentBonus,
        double focusBonus,
        double totalChance,
        long attempts
    ) {
        public String getFormattedBreakdown() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Base: %.0f%%", baseChance * 100));
            if (failureBonus > 0) sb.append(String.format(" + Attempts: %.0f%%", failureBonus * 100));
            if (talentBonus > 0) sb.append(String.format(" + Talent: %.0f%%", talentBonus * 100));
            if (focusBonus > 0) sb.append(String.format(" + Focus: %.0f%%", focusBonus * 100));
            sb.append(String.format(" = %.0f%%", totalChance * 100));
            return sb.toString();
        }
    }

    // === Constructor ===
    public PlayerCultivationAttachment() {
        // Use safe initialization that handles JSON not being loaded yet
        this.currentQiRealm = getDefaultMortalRealm();
        this.currentBodyRealm = getDefaultMortalRealm();

        // Initialize max cultivation with safe defaults
        this.maxQiCultivation = getSafeMaxCultivation(currentQiRealm);
        this.maxBodyCultivation = getSafeMaxCultivation(currentBodyRealm);
    }

    /**
     * Safely gets the mortal realm, handling cases where JSON isn't loaded yet
     */
    private CultivationRealm getDefaultMortalRealm() {
        // First try to get from the registry
        CultivationRealm mortalRealm = ICModCultivationRealms.get(DEFAULT_MORTAL_REALM_ID);

        if (mortalRealm != null) {
            return mortalRealm;
        }

        // If still null, create a temporary default realm
        ImmortalCultivation.LOGGER.warn("Mortal realm not found during initialization, using temporary default");
        return createTemporaryMortalRealm();
    }

    /**
     * Safely gets max cultivation from a realm, with fallback for null realms
     */
    private long getSafeMaxCultivation(CultivationRealm realm) {
        if (realm != null) {
            try {
                return realm.getBaseMaxCultivation();
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.warn("Error getting max cultivation from realm, using default", e);
            }
        }
        return DEFAULT_MAX_CULTIVATION;
    }

    /**
     * Creates a temporary mortal realm for use during early initialization
     */
    private CultivationRealm createTemporaryMortalRealm() {
        // Create basic mortal realm data
        var basicInfo = new CultivationRealmData.BasicInfo(
            DEFAULT_MORTAL_REALM_ID, "Mortal", 0, "Mortal",
            "Temporary mortal realm used during initialization"
        );

        var physicalStats = new CultivationRealmData.PhysicalStats(
            20L, 0.0, 0.0, 0.0, 0.0
        );

        var cultivationStats = new CultivationRealmData.CultivationStats(
            DEFAULT_MAX_CULTIVATION, 0.005
        );

        var qiStats = new CultivationRealmData.QiStats(
            0L, 0.0, 0.0
        );

        var visualProperties = new CultivationRealmData.VisualProperties(
            8421504, "minecraft:cloud", java.util.Optional.empty()
        );

        var progression = new CultivationRealmData.Progression(
            java.util.List.of(), java.util.List.of()
        );

        var features = new CultivationRealmData.Features(
            java.util.List.of(), java.util.List.of(), java.util.List.of()
        );

        var realmData = new CultivationRealmData(
            basicInfo, physicalStats, cultivationStats, qiStats,
            visualProperties, progression, features,
            java.util.Optional.empty(), java.util.Optional.empty()
        );

        return new JsonCultivationRealm(realmData);
    }

    /**
     * Updates realms after JSON loading is complete.
     * Call this method after the JSON realm system has been initialized.
     */
    public void updateRealmsAfterJsonLoad() {
        if (currentQiRealm == null || DEFAULT_MORTAL_REALM_ID.equals(currentQiRealm.getId())) {
            CultivationRealm newMortalRealm = ICModCultivationRealms.get(DEFAULT_MORTAL_REALM_ID);
            if (newMortalRealm != null && newMortalRealm != currentQiRealm) {
                ImmortalCultivation.LOGGER.info("Updating Qi realm from temporary to loaded JSON realm");
                this.currentQiRealm = newMortalRealm;
                this.maxQiCultivation = currentQiRealm.getBaseMaxCultivation();
            }
        }

        if (currentBodyRealm == null || DEFAULT_MORTAL_REALM_ID.equals(currentBodyRealm.getId())) {
            CultivationRealm newMortalRealm = ICModCultivationRealms.get(DEFAULT_MORTAL_REALM_ID);
            if (newMortalRealm != null && newMortalRealm != currentBodyRealm) {
                ImmortalCultivation.LOGGER.info("Updating Body realm from temporary to loaded JSON realm");
                this.currentBodyRealm = newMortalRealm;
                this.maxBodyCultivation = currentBodyRealm.getBaseMaxCultivation();
            }
        }
    }

    // === Player Reference Management ===

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Nullable
    protected Player getPlayerSafe() {
        if (player != null && player.isAlive()) {
            return player;
        }
        return null;
    }

    // === Qi Cultivation Implementation ===

    @Override
    public long getCurrentQiCultivation() {
        return currentQiCultivation;
    }

    @Override
    public void setCurrentQiCultivation(long value) {
        this.currentQiCultivation = Math.max(0, Math.min(value, maxQiCultivation));
    }

    @Override
    public long getMaxQiCultivation() {
        return maxQiCultivation;
    }

    public void setMaxQiCultivation(long value) {
        this.maxQiCultivation = Math.max(1, value);
        if (currentQiCultivation > maxQiCultivation) {
            currentQiCultivation = maxQiCultivation;
        }
    }

    public void addQiCultivation(double amount) {
        qiCultivationFraction += amount;

        long wholePoints = (long) qiCultivationFraction;
        qiCultivationFraction -= wholePoints;

        setCurrentQiCultivation(currentQiCultivation + wholePoints);
    }

    public double getQiProgress() {
        if (maxQiCultivation == 0) return 0.0;
        return (double) currentQiCultivation / maxQiCultivation;
    }

    @Override
    public double getBaseQiCultivationRate() {
        return currentQiRealm.getBaseCultivationRate();
    }

    @Override
    public double getBaseCirculatingQiCultivationRate() {
        return getBaseQiCultivationRate() * 10;
    }

    @Override
    public double getEffectiveQiCultivationRate() {
        double baseRate = getBaseQiCultivationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseRate;

        // Apply spiritual root multiplier
        try {
            PlayerTalentAttachment spiritualRoot = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (spiritualRoot != null) {
                baseRate *= spiritualRoot.getSpiritualRootMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying spiritual root", e);
        }

        // Apply race multiplier for Qi cultivation
        try {
            PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            if (race != null && race.hasRaceAssigned()) {
                baseRate *= race.getQiMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying race Qi multiplier", e);
        }

        // Apply bonuses
        baseRate = applyQiCultivationBonuses(baseRate);

        // Apply focus multiplier
        if (currentFocus == CultivationType.QI) {
            baseRate *= 2.0;
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingQiCultivationRate() {
        double baseCirculatingRate = getBaseCirculatingQiCultivationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseCirculatingRate;

        // Apply same multipliers as normal cultivation
        try {
            PlayerTalentAttachment spiritualRoot = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (spiritualRoot != null) {
                baseCirculatingRate *= spiritualRoot.getSpiritualRootMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying spiritual root", e);
        }

        baseCirculatingRate = applyQiCultivationBonuses(baseCirculatingRate);

        if (currentFocus == CultivationType.QI) {
            baseCirculatingRate *= 2.0;
        }

        return baseCirculatingRate;
    }

    @Override
    public long getBaseQiCultivation() {
        return currentQiRealm.getBaseMaxCultivation();
    }

    @Override
    public double getRegenFraction() {
        // Return the appropriate fraction based on current focus
        return currentFocus == CultivationType.QI ? qiCultivationFraction : bodyCultivationFraction;
    }

    @Override
    public void setRegenFraction(double fraction) {
        // Set the appropriate fraction based on current focus
        if (currentFocus == CultivationType.QI) {
            this.qiCultivationFraction = fraction;
        } else {
            this.bodyCultivationFraction = fraction;
        }
    }

    @Override
    public void addQiRegenFraction(double amount) {
        IQiCultivationData.super.addQiRegenFraction(amount);
    }

    @Override
    public void addBodyRegenFraction(double amount) {
        IBodyCultivationData.super.addBodyRegenFraction(amount);
    }

    // === Body Cultivation Implementation ===

    @Override
    public long getCurrentBodyCultivation() {
        return currentBodyCultivation;
    }

    @Override
    public void setCurrentBodyCultivation(long value) {
        this.currentBodyCultivation = Math.max(0, Math.min(value, maxBodyCultivation));
    }

    @Override
    public long getMaxBodyCultivation() {
        return maxBodyCultivation;
    }

    public void setMaxBodyCultivation(long value) {
        this.maxBodyCultivation = Math.max(1, value);
        if (currentBodyCultivation > maxBodyCultivation) {
            currentBodyCultivation = maxBodyCultivation;
        }
    }

    public void addBodyCultivation(double amount) {
        bodyCultivationFraction += amount;

        long wholePoints = (long) bodyCultivationFraction;
        bodyCultivationFraction -= wholePoints;

        setCurrentBodyCultivation(currentBodyCultivation + wholePoints);
    }

    public double getBodyProgress() {
        if (maxBodyCultivation == 0) return 0.0;
        return (double) currentBodyCultivation / maxBodyCultivation;
    }

    @Override
    public double getBaseBodyCultivationRate() {
        return currentBodyRealm.getBaseCultivationRate();
    }

    @Override
    public double getBaseCirculatingBodyCultivationRate() {
        return getBaseBodyCultivationRate() * 10;
    }

    @Override
    public double getEffectiveBodyCultivationRate() {
        double baseRate = getBaseBodyCultivationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseRate;

        // Apply physique multiplier
        try {
            PlayerTalentAttachment physique = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (physique != null) {
                baseRate *= physique.getPhysiqueMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying physique", e);
        }

        // Apply race multiplier for Body cultivation
        try {
            PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            if (race != null && race.hasRaceAssigned()) {
                baseRate *= race.getBodyMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying race Body multiplier", e);
        }

        // Apply bonuses
        baseRate = applyBodyCultivationBonuses(baseRate);

        // Apply focus multiplier
        if (currentFocus == CultivationType.BODY) {
            baseRate *= 2.0;
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingBodyCultivationRate() {
        double baseCirculatingRate = getBaseCirculatingBodyCultivationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseCirculatingRate;

        // Apply same multipliers as normal cultivation
        try {
            PlayerTalentAttachment physique = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (physique != null) {
                baseCirculatingRate *= physique.getPhysiqueMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying physique", e);
        }

        baseCirculatingRate = applyBodyCultivationBonuses(baseCirculatingRate);

        if (currentFocus == CultivationType.BODY) {
            baseCirculatingRate *= 2.0;
        }

        return baseCirculatingRate;
    }

    @Override
    public long getBaseBodyCultivation() {
        return currentBodyRealm.getBaseMaxCultivation();
    }

    // === Realm Management ===

    public CultivationRealm getCurrentQiRealm() {
        return currentQiRealm;
    }

    public CultivationRealm getCurrentBodyRealm() {
        return currentBodyRealm;
    }


    /**
     * Updates the max cultivation limits based on current realms
     */
    private void updateCultivationLimits() {
        // Update max Qi cultivation based on current Qi realm
        long newMaxQiCultivation = currentQiRealm.getBaseMaxCultivation();
        if (newMaxQiCultivation != maxQiCultivation) {
            setMaxQiCultivation(newMaxQiCultivation);
        }

        // Update max Body cultivation based on current Body realm
        long newMaxBodyCultivation = currentBodyRealm.getBaseMaxCultivation();
        if (newMaxBodyCultivation != maxBodyCultivation) {
            setMaxBodyCultivation(newMaxBodyCultivation);
        }
    }

    /**
     * Sets the current Qi realm and applies permanent health bonus if this is an advancement
     */
    public void setCurrentQiRealm(CultivationRealm realm) {
        CultivationRealm previousRealm = this.currentQiRealm;
        this.previousQiRealm = previousRealm;
        this.currentQiRealm = realm;

        // Reset breakthrough attempts when successfully advancing
        this.qiBreakthroughAttempts = 0;

        // Apply permanent bonuses if this is an advancement
        applyPermanentBonusesIfAdvancement(previousRealm, realm, CultivationType.QI);

        // Update cultivation limits based on new realm
        updateCultivationLimits();

        forceSync();

        // Call realm advancement events
        if (player != null && realm != null) {
            realm.onRealmAdvancement(player, (IQiCultivationData) this, previousRealm);
        }
    }

    /**
     * Sets the current Body realm and applies permanent health bonus if this is an advancement
     */
    public void setCurrentBodyRealm(CultivationRealm realm) {
        CultivationRealm previousRealm = this.currentBodyRealm;
        this.previousBodyRealm = previousRealm;
        this.currentBodyRealm = realm;

        // Reset breakthrough attempts when successfully advancing
        this.bodyBreakthroughAttempts = 0;

        // Apply permanent bonuses if this is an advancement
        applyPermanentBonusesIfAdvancement(previousRealm, realm, CultivationType.BODY);

        // Update cultivation limits based on new realm
        updateCultivationLimits();

        forceSync();

        // Call realm advancement events
        if (player != null && realm != null) {
            realm.onRealmAdvancement(player, (IBodyCultivationData) this, previousRealm);
        }
    }

    /**
     * Applies permanent health bonus when advancing to a higher realm level.
     * This ensures that special realm bonuses stick around permanently.
     */
    private void applyPermanentBonusesIfAdvancement(@Nullable CultivationRealm previousRealm,
                                                    CultivationRealm newRealm,
                                                    CultivationType cultivationType) {
        if (player == null || newRealm == null) return;

        try {
            // Only apply bonus if this is a genuine advancement (higher realm level)
            boolean isAdvancement = previousRealm == null || newRealm.getRealmLevel() > previousRealm.getRealmLevel();

            if (isAdvancement) {
                // Apply permanent health bonus
                long healthBonus = newRealm.getBaseHealth();
                if (healthBonus > 0) {
                    PlayerResourceAttachment resourceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
                    if (resourceAttachment != null) {
                        resourceAttachment.addPermanentHealthBonus(healthBonus);

                        ImmortalCultivation.LOGGER.info("Player {} gained +{} permanent health from {} realm advancement: {} -> {}",
                            player.getName().getString(),
                            healthBonus,
                            cultivationType.getDisplayName(),
                            previousRealm != null ? previousRealm.getDisplayName() : "None",
                            newRealm.getDisplayName());
                    }
                }

                // Apply permanent attack damage bonus
                double attackBonus = newRealm.getBaseAttackDamage();
                if (attackBonus > 0) {
                    PlayerCombatStatsAttachment combatStatsAttachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                    if (combatStatsAttachment != null) {
                        combatStatsAttachment.addPermanentAttackDamageBonus(attackBonus);

                        ImmortalCultivation.LOGGER.info("Player {} gained +{} permanent attack damage from {} realm advancement: {} -> {}",
                            player.getName().getString(),
                            attackBonus,
                            cultivationType.getDisplayName(),
                            previousRealm != null ? previousRealm.getDisplayName() : "None",
                            newRealm.getDisplayName());
                    }
                }

                // Apply permanent movement speed bonus
                double speedBonus = newRealm.getBaseMovementSpeed();
                if (speedBonus > 0) {
                    PlayerCombatStatsAttachment combatStatsAttachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                    if (combatStatsAttachment != null) {
                        combatStatsAttachment.addPermanentMovementSpeedBonus(speedBonus);

                        ImmortalCultivation.LOGGER.info("Player {} gained +{} permanent movement speed from {} realm advancement: {} -> {}",
                            player.getName().getString(),
                            speedBonus,
                            cultivationType.getDisplayName(),
                            previousRealm != null ? previousRealm.getDisplayName() : "None",
                            newRealm.getDisplayName());
                    }
                }

                // Apply permanent jump height bonus
                double jumpBonus = newRealm.getBaseJumpHeight();
                if (jumpBonus > 0) {
                    PlayerCombatStatsAttachment combatStatsAttachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                    if (combatStatsAttachment != null) {
                        combatStatsAttachment.addPermanentJumpHeightBonus(jumpBonus);

                        ImmortalCultivation.LOGGER.info("Player {} gained +{} permanent jump height from {} realm advancement: {} -> {}",
                            player.getName().getString(),
                            jumpBonus,
                            cultivationType.getDisplayName(),
                            previousRealm != null ? previousRealm.getDisplayName() : "None",
                            newRealm.getDisplayName());
                    }
                }

                // Send comprehensive notification to player about all bonuses gained
                if (player instanceof ServerPlayer serverPlayer) {
                    StringBuilder bonusMessage = new StringBuilder("§6Permanent Bonuses Gained: §r");
                    boolean hasBonus = false;

                    if (healthBonus > 0) {
                        bonusMessage.append("§a+").append(healthBonus).append(" Health§r");
                        hasBonus = true;
                    }

                    if (attackBonus > 0) {
                        if (hasBonus) bonusMessage.append(", ");
                        bonusMessage.append("§c+").append(String.format("%.1f", attackBonus * 100)).append("% Attack§r");
                        hasBonus = true;
                    }

                    if (speedBonus > 0) {
                        if (hasBonus) bonusMessage.append(", ");
                        bonusMessage.append("§b+").append(String.format("%.1f", speedBonus * 100)).append("% Speed§r");
                        hasBonus = true;
                    }

                    if (jumpBonus > 0) {
                        if (hasBonus) bonusMessage.append(", ");
                        bonusMessage.append("§e+").append(String.format("%.1f", jumpBonus * 100)).append("% Jump§r");
                        hasBonus = true;
                    }

                    if (hasBonus) {
                        serverPlayer.sendSystemMessage(Component.literal(bonusMessage.toString()));
                    }
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying permanent bonuses for player {}",
                player.getName().getString(), e);
        }
    }

    // === Focus Management ===

    public CultivationType getCurrentFocus() {
        return currentFocus;
    }

    public long getLastFocusChangeTime() {
        return lastFocusChangeTime;
    }

    public boolean toggleFocus() {
        if (player == null) return false;

        long currentTime = player.level().getGameTime();

        // CRITICAL FIX: Better cooldown logic with proper logging
        long timeSinceLastChange = currentTime - lastFocusChangeTime;
        if (timeSinceLastChange < FOCUS_CHANGE_COOLDOWN) {
            ImmortalCultivation.LOGGER.debug("Focus change rejected - cooldown active. Time since last: {}, Required: {}",
                timeSinceLastChange, FOCUS_CHANGE_COOLDOWN);
            return false;
        }

        CultivationType oldFocus = currentFocus;
        currentFocus = (currentFocus == CultivationType.QI) ? CultivationType.BODY : CultivationType.QI;
        lastFocusChangeTime = currentTime;

        ImmortalCultivation.LOGGER.info("Focus changed from {} to {} for player {}",
            oldFocus.getDisplayName(), currentFocus.getDisplayName(),
            player.getName().getString());

        forceSync();
        return true;
    }

    public double getQiCultivationMultiplier() {
        return currentFocus == CultivationType.QI ? 2.0 : 1.0;
    }

    public double getBodyCultivationMultiplier() {
        return currentFocus == CultivationType.BODY ? 2.0 : 1.0;
    }

    public double getQiRegenerationBonus() {
        return currentFocus == CultivationType.QI ? 1.5 : 1.0;
    }

    public double getBodyRegenerationBonus() {
        return currentFocus == CultivationType.BODY ? 1.5 : 1.0;
    }

    // === Bonus Management ===

    public double applyQiCultivationBonuses(double baseRate) {
        return (baseRate + qiCultivationAdditiveBonus) * (1.0 + qiCultivationMultiplicativeBonus);
    }

    public double applyBodyCultivationBonuses(double baseRate) {
        return (baseRate + bodyCultivationAdditiveBonus) * (1.0 + bodyCultivationMultiplicativeBonus);
    }

    public void addQiBonus(double additive, double multiplicative) {
        this.qiCultivationAdditiveBonus += additive;
        this.qiCultivationMultiplicativeBonus += multiplicative;
    }

    public void addBodyBonus(double additive, double multiplicative) {
        this.bodyCultivationAdditiveBonus += additive;
        this.bodyCultivationMultiplicativeBonus += multiplicative;
    }

    /**
     * Get the raw Qi additive bonus for display
     */
    public double getQiCultivationRawAdditiveBonus() {
        return qiCultivationAdditiveBonus;
    }

    /**
     * Get the raw Body additive bonus for display
     */
    public double getBodyCultivationRawAdditiveBonus() {
        return bodyCultivationAdditiveBonus;
    }

    /**
     * Get the raw Qi multiplicative bonus percentage for display
     */
    public double getQiCultivationRawMultiplicativeBonus() {
        return qiCultivationMultiplicativeBonus;
    }

    /**
     * Get the raw Body multiplicative bonus percentage for display
     */
    public double getBodyCultivationRawMultiplicativeBonus() {
        return bodyCultivationMultiplicativeBonus;
    }

    // === Getters for GUI display ===
    public double getQiAdditiveBonus() {
        return qiCultivationAdditiveBonus;
    }

    public double getBodyAdditiveBonus() {
        return bodyCultivationAdditiveBonus;
    }

    // === Tick Processing ===

    public void tick() {
        if (player == null) return;

        // Update time tracking
        timeInCurrentQiRealm++;
        timeInCurrentBodyRealm++;
        totalQiCultivationTime++;
        totalBodyCultivationTime++;

        // CRITICAL FIX: Update accumulation rates from effective rates each tick
        this.qiAccumulationRate = getEffectiveQiCultivationRate() / 20.0; // Convert per-second to per-tick
        this.bodyAccumulationRate = getEffectiveBodyCultivationRate() / 20.0; // Convert per-second to per-tick

        // Accumulate cultivation using the updated rates
        if (qiAccumulationRate > 0 && getCurrentQiCultivation() < getMaxQiCultivation()) {
            addQiCultivation(qiAccumulationRate);
        }
        if (bodyAccumulationRate > 0 && getCurrentBodyCultivation() < getMaxBodyCultivation()) {
            addBodyCultivation(bodyAccumulationRate);
        }

        // Handle sync
        tickCounter++;
        if (tickCounter >= SYNC_INTERVAL) {
            tickCounter = 0;
            syncIfNeeded();
        }

        // Clear breakthrough cooldowns after time
        if (qiBreakthroughCooldown && player.level().getGameTime() - lastQiBreakthroughAttempt > 6000) {
            qiBreakthroughCooldown = false;
        }
        if (bodyBreakthroughCooldown && player.level().getGameTime() - lastBodyBreakthroughAttempt > 6000) {
            bodyBreakthroughCooldown = false;
        }
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Not needed with Codec
    }

    @Override
    public CompoundTag serializeNBT() {
        // Not needed with Codec
        return new CompoundTag();
    }

    // === Sync Management ===

    private void syncIfNeeded() {
        long currentHash = calculateStateHash();
        if (currentHash != lastSyncedHash) {
            lastSyncedHash = currentHash;
            syncToClient();
        }
    }

    /**
     * UPDATED: Calculate state hash including breakthrough data
     */
    private long calculateStateHash() {
        return Objects.hash(
            currentQiRealm.getId(),
            currentQiCultivation,
            currentBodyRealm.getId(),
            currentBodyCultivation,
            currentFocus,
            qiCultivationAdditiveBonus,
            bodyCultivationAdditiveBonus,
            // ADDED: Include breakthrough data in hash
            qiBreakthroughAttempts,
            bodyBreakthroughAttempts,
            qiBreakthroughCooldown,
            bodyBreakthroughCooldown
        );
    }

    /**
     * UPDATED: Sync to client including breakthrough data
     */
    private void syncToClient() {
        if (player instanceof ServerPlayer serverPlayer) {
            CultivationSyncPacket packet = new CultivationSyncPacket(
                currentQiRealm.getId(),
                previousQiRealm != null ? previousQiRealm.getId() : "",
                currentBodyRealm.getId(),
                previousBodyRealm != null ? previousBodyRealm.getId() : "",
                currentQiCultivation,
                maxQiCultivation,
                currentBodyCultivation,
                maxBodyCultivation,
                currentFocus,
                qiCultivationAdditiveBonus,
                qiCultivationMultiplicativeBonus,
                bodyCultivationAdditiveBonus,
                bodyCultivationMultiplicativeBonus,
                // ADDED: Include breakthrough data in sync packet
                qiBreakthroughAttempts,
                bodyBreakthroughAttempts,
                qiBreakthroughCooldown,
                bodyBreakthroughCooldown,
                lastQiBreakthroughAttempt,
                lastBodyBreakthroughAttempt
            );

            ICModNetwork.sendToPlayer(packet, serverPlayer);

            ImmortalCultivation.LOGGER.debug("Synced cultivation data to client - Qi attempts: {}, Body attempts: {}",
                qiBreakthroughAttempts, bodyBreakthroughAttempts);
        }
    }

    /**
     * UPDATED: Force sync method with better logging
     */
    public void forceSync() {
        if (player instanceof ServerPlayer) {
            lastSyncedHash = 0; // Force a sync by invalidating hash
            syncToClient();
            ImmortalCultivation.LOGGER.debug("Forced cultivation sync for player {}",
                player.getName().getString());
        }
    }

    // === Utility Methods ===

    public void setQiAccumulationRate(double rate) {
        this.qiAccumulationRate = rate;
    }

    public void setBodyAccumulationRate(double rate) {
        this.bodyAccumulationRate = rate;
    }

    public double getQiAccumulationRate() {
        return qiAccumulationRate;
    }

    public double getBodyAccumulationRate() {
        return bodyAccumulationRate;
    }

    // === Sync Application Methods ===

    /**
     * UPDATED: Apply sync data from server including breakthrough data
     */
    public void applySync(String currentQiRealmId, String previousQiRealmId,
                          String currentBodyRealmId, String previousBodyRealmId,
                          long currentQiCultivation, long maxQiCultivation,
                          long currentBodyCultivation, long maxBodyCultivation,
                          CultivationType currentFocus,
                          double qiAdditiveBonus, double qiMultiplicativeBonus,
                          double bodyAdditiveBonus, double bodyMultiplicativeBonus,
                          // ADDED: Breakthrough data parameters
                          long qiBreakthroughAttempts, long bodyBreakthroughAttempts,
                          boolean qiBreakthroughCooldown, boolean bodyBreakthroughCooldown,
                          long lastQiBreakthroughAttempt, long lastBodyBreakthroughAttempt) {

        // Apply realm data
        this.currentQiRealm = ICModCultivationRealms.get(currentQiRealmId);
        if (!previousQiRealmId.isEmpty()) {
            this.previousQiRealm = ICModCultivationRealms.get(previousQiRealmId);
        }
        this.currentBodyRealm = ICModCultivationRealms.get(currentBodyRealmId);
        if (!previousBodyRealmId.isEmpty()) {
            this.previousBodyRealm = ICModCultivationRealms.get(previousBodyRealmId);
        }

        // Apply cultivation progress
        this.currentQiCultivation = currentQiCultivation;
        this.maxQiCultivation = maxQiCultivation;
        this.currentBodyCultivation = currentBodyCultivation;
        this.maxBodyCultivation = maxBodyCultivation;

        // Apply focus
        this.currentFocus = currentFocus;

        // Apply bonuses
        this.qiCultivationAdditiveBonus = qiAdditiveBonus;
        this.qiCultivationMultiplicativeBonus = qiMultiplicativeBonus;
        this.bodyCultivationAdditiveBonus = bodyAdditiveBonus;
        this.bodyCultivationMultiplicativeBonus = bodyMultiplicativeBonus;

        // ADDED: Apply breakthrough data
        this.qiBreakthroughAttempts = qiBreakthroughAttempts;
        this.bodyBreakthroughAttempts = bodyBreakthroughAttempts;
        this.qiBreakthroughCooldown = qiBreakthroughCooldown;
        this.bodyBreakthroughCooldown = bodyBreakthroughCooldown;
        this.lastQiBreakthroughAttempt = lastQiBreakthroughAttempt;
        this.lastBodyBreakthroughAttempt = lastBodyBreakthroughAttempt;

        ImmortalCultivation.LOGGER.debug("Applied cultivation sync - Qi attempts: {}, Body attempts: {}",
            qiBreakthroughAttempts, bodyBreakthroughAttempts);
    }

    /**
     * COMPATIBILITY: Old applySync method signature for backward compatibility
     */
    public void applySync(String qiRealmId, String prevQiRealmId, String bodyRealmId, String prevBodyRealmId,
                          long qiCurrent, long qiMax, long bodyCurrent, long bodyMax,
                          CultivationType focus, double qiAdd, double qiMult, double bodyAdd, double bodyMult) {

        // Call the new method with default breakthrough data
        applySync(qiRealmId, prevQiRealmId, bodyRealmId, prevBodyRealmId,
            qiCurrent, qiMax, bodyCurrent, bodyMax,
            focus, qiAdd, qiMult, bodyAdd, bodyMult,
            // Default breakthrough data
            0L, 0L, false, false, 0L, 0L);
    }

    // === Additional Getters for Network/UI ===

    public double getQiMultiplicativeBonus() {
        return qiCultivationMultiplicativeBonus;
    }

    public double getBodyMultiplicativeBonus() {
        return bodyCultivationMultiplicativeBonus;
    }

    // === Breakthrough System Implementation ===

    /**
     * COMPATIBILITY: Simple breakthrough methods using MINOR type
     */
    public BreakthroughResult attemptQiBreakthrough() {
        return attemptTypedBreakthrough(CultivationType.QI, BreakthroughType.MINOR);
    }

    public BreakthroughResult attemptBodyBreakthrough() {
        return attemptTypedBreakthrough(CultivationType.BODY, BreakthroughType.MINOR);
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
    public double calculateBreakthroughSuccessRate(CultivationType type) {
        // Use MINOR breakthrough type for the standard calculation
        return calculateBreakthroughSuccessChance(type, BreakthroughType.MINOR);
    }

    /**
     * Get success rate for a specific breakthrough type
     */
    public double calculateBreakthroughSuccessRate(CultivationType type, BreakthroughType breakthroughType) {
        return calculateBreakthroughSuccessChance(type, breakthroughType);
    }

    /**
     * Check if breakthrough is on cooldown for the specified type.
     */
    public boolean isBreakthroughOnCooldown(CultivationType type) {
        return (type == CultivationType.QI) ? qiBreakthroughCooldown : bodyBreakthroughCooldown;
    }

    /**
     * Get time since last breakthrough attempt for the specified type.
     */
    public long getTimeSinceLastBreakthroughAttempt(CultivationType type) {
        if (player == null) return Long.MAX_VALUE;

        long lastAttempt = (type == CultivationType.QI) ? lastQiBreakthroughAttempt : lastBodyBreakthroughAttempt;
        return player.level().getGameTime() - lastAttempt;
    }

    /**
     * Attempt breakthrough with specific type for better control
     */
    public BreakthroughResult attemptTypedBreakthrough(CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) {
            return new BreakthroughResult(false, "No player reference", null, breakthroughType, 0.0, 0L);
        }

        // Check basic requirements first
        if (cultivationType == CultivationType.QI) {
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
        CultivationRealm currentRealm = cultivationType == CultivationType.QI ? currentQiRealm : currentBodyRealm;
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();

        if (nextRealms.isEmpty()) {
            return new BreakthroughResult(false, "No higher realm available", null, breakthroughType, 0.0,
                cultivationType == CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts);
        }

        // Get target realm (for now, just use the first available - can be enhanced later)
        CultivationRealm targetRealm = nextRealms.get(0);

        // Calculate success chance combining base type chance with failure bonus
        double successChance = calculateBreakthroughSuccessChance(cultivationType, breakthroughType);

        // Record attempt
        long currentAttempts;
        if (cultivationType == CultivationType.QI) {
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
            if (cultivationType == CultivationType.QI) {
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
            if (cultivationType == CultivationType.QI) {
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
                cultivationType == CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts,
                successChance * 100, nextChance * 100);

            ImmortalCultivation.LOGGER.info("{} {} breakthrough FAILED for {} - attempt {} ({}% chance, next: {}%)",
                cultivationType.getDisplayName(), breakthroughType.getDisplayName(),
                player.getName().getString(),
                cultivationType == CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts,
                (int)(successChance * 100), (int)(nextChance * 100));

            return new BreakthroughResult(false, message, null, breakthroughType, successChance,
                cultivationType == CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts);
        }
    }

    /**
     * Calculate success chance combining breakthrough type with failure bonus
     */
    private double calculateBreakthroughSuccessChance(CultivationType cultivationType, BreakthroughType breakthroughType) {
        // Base chance from breakthrough type
        double baseChance = breakthroughType.getBaseSuccessChance();

        // Add failure bonus (5% per failed attempt)
        long attempts = (cultivationType == CultivationType.QI) ? qiBreakthroughAttempts : bodyBreakthroughAttempts;
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
    private double applyTalentBonuses(CultivationType cultivationType, double currentChance) {
        if (player == null) return currentChance;

        try {
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talentAttachment != null) {
                if (cultivationType == CultivationType.QI && talentAttachment.hasSpiritualRootAssigned()) {
                    float multiplier = talentAttachment.getSpiritualRootMultiplier();
                    // Each 0.1x talent adds 2% breakthrough success rate
                    double bonus = (multiplier - 1.0) * 0.2;
                    return currentChance + bonus;
                } else if (cultivationType == CultivationType.BODY && talentAttachment.hasPhysiqueAssigned()) {
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
    private double applyFocusBonus(CultivationType cultivationType, double currentChance) {
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
        List<BreakthroughOption> qiOptions = getBreakthroughOptionsForType(CultivationType.QI);
        List<BreakthroughOption> bodyOptions = getBreakthroughOptionsForType(CultivationType.BODY);

        return new BreakthroughOptions(qiOptions, bodyOptions);
    }

    /**
     * Get breakthrough options for a specific cultivation type
     */
    public List<BreakthroughOption> getBreakthroughOptionsForType(CultivationType cultivationType) {
        List<BreakthroughOption> options = new ArrayList<>();

        if (player == null) {
            return options;
        }

        CultivationRealm currentRealm = (cultivationType == CultivationType.QI) ? currentQiRealm : currentBodyRealm;
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
    public boolean canAttemptTypedBreakthrough(CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return false;

        // Check basic requirements
        if (cultivationType == CultivationType.QI) {
            if (currentQiCultivation < maxQiCultivation) return false;
            if (qiBreakthroughCooldown) return false;
        } else {
            if (currentBodyCultivation < maxBodyCultivation) return false;
            if (bodyBreakthroughCooldown) return false;
        }

        // Check if there are available advancement paths
        CultivationRealm currentRealm = (cultivationType == CultivationType.QI) ? currentQiRealm : currentBodyRealm;
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
    private String getUnavailabilityReason(CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return "No player reference";

        if (cultivationType == CultivationType.QI) {
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

        CultivationRealm currentRealm = (cultivationType == CultivationType.QI) ? currentQiRealm : currentBodyRealm;
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();

        if (nextRealms.isEmpty()) {
            return "No higher realm available";
        }

        return "Requirements not met";
    }

    /**
     * Get detailed breakdown of success rate calculation for UI display
     */
    public BreakthroughChanceBreakdown getBreakthroughChanceBreakdown(CultivationType cultivationType, BreakthroughType breakthroughType) {
        double baseChance = breakthroughType.getBaseSuccessChance();
        long attempts = (cultivationType == CultivationType.QI) ? qiBreakthroughAttempts : bodyBreakthroughAttempts;
        double failureBonus = attempts * 0.05;
        double talentBonus = applyTalentBonuses(cultivationType, 0.0); // Apply to 0 to get just the bonus
        double focusBonus = (currentFocus == cultivationType) ? 0.05 : 0.0;

        double totalChance = Math.min(0.95, Math.max(0.05, baseChance + failureBonus + talentBonus + focusBonus));

        return new BreakthroughChanceBreakdown(
            baseChance, failureBonus, talentBonus, focusBonus, totalChance, attempts
        );
    }

    // === Serialization with Codec ===

    // Sub-codec for realm data
    private record RealmData(String currentQiRealmId, String previousQiRealmId,
                             String currentBodyRealmId, String previousBodyRealmId) {}

    private static final Codec<RealmData> REALM_DATA_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("current_qi_realm").forGetter(RealmData::currentQiRealmId),
            Codec.STRING.optionalFieldOf("previous_qi_realm", "").forGetter(RealmData::previousQiRealmId),
            Codec.STRING.fieldOf("current_body_realm").forGetter(RealmData::currentBodyRealmId),
            Codec.STRING.optionalFieldOf("previous_body_realm", "").forGetter(RealmData::previousBodyRealmId)
        ).apply(instance, RealmData::new)
    );

    // Sub-codec for cultivation progress
    private record CultivationProgress(long currentQi, long maxQi, double qiFraction,
                                       long currentBody, long maxBody, double bodyFraction) {}

    private static final Codec<CultivationProgress> CULTIVATION_PROGRESS_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.optionalFieldOf("current_qi", 0L).forGetter(CultivationProgress::currentQi),
            Codec.LONG.optionalFieldOf("max_qi", 1000L).forGetter(CultivationProgress::maxQi),
            Codec.DOUBLE.optionalFieldOf("qi_fraction", 0.0).forGetter(CultivationProgress::qiFraction),
            Codec.LONG.optionalFieldOf("current_body", 0L).forGetter(CultivationProgress::currentBody),
            Codec.LONG.optionalFieldOf("max_body", 1000L).forGetter(CultivationProgress::maxBody),
            Codec.DOUBLE.optionalFieldOf("body_fraction", 0.0).forGetter(CultivationProgress::bodyFraction)
        ).apply(instance, CultivationProgress::new)
    );

    // Sub-codec for breakthrough data
    private record BreakthroughData(long qiAttempts, long bodyAttempts, long lastQiAttempt,
                                    long lastBodyAttempt, boolean qiCooldown, boolean bodyCooldown) {}

    private static final Codec<BreakthroughData> BREAKTHROUGH_DATA_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.optionalFieldOf("qi_attempts", 0L).forGetter(BreakthroughData::qiAttempts),
            Codec.LONG.optionalFieldOf("body_attempts", 0L).forGetter(BreakthroughData::bodyAttempts),
            Codec.LONG.optionalFieldOf("last_qi_attempt", 0L).forGetter(BreakthroughData::lastQiAttempt),
            Codec.LONG.optionalFieldOf("last_body_attempt", 0L).forGetter(BreakthroughData::lastBodyAttempt),
            Codec.BOOL.optionalFieldOf("qi_cooldown", false).forGetter(BreakthroughData::qiCooldown),
            Codec.BOOL.optionalFieldOf("body_cooldown", false).forGetter(BreakthroughData::bodyCooldown)
        ).apply(instance, BreakthroughData::new)
    );

    // Sub-codec for bonus data
    private record BonusData(double qiAdditive, double qiMultiplicative,
                             double bodyAdditive, double bodyMultiplicative) {}

    private static final Codec<BonusData> BONUS_DATA_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.optionalFieldOf("qi_additive", 0.0).forGetter(BonusData::qiAdditive),
            Codec.DOUBLE.optionalFieldOf("qi_multiplicative", 0.0).forGetter(BonusData::qiMultiplicative),
            Codec.DOUBLE.optionalFieldOf("body_additive", 0.0).forGetter(BonusData::bodyAdditive),
            Codec.DOUBLE.optionalFieldOf("body_multiplicative", 0.0).forGetter(BonusData::bodyMultiplicative)
        ).apply(instance, BonusData::new)
    );

    // Main codec using sub-codecs
    public static final Codec<PlayerCultivationAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            REALM_DATA_CODEC.fieldOf("realms").forGetter(PlayerCultivationAttachment::getRealmData),
            CULTIVATION_PROGRESS_CODEC.fieldOf("cultivation").forGetter(PlayerCultivationAttachment::getCultivationProgress),
            Codec.STRING.optionalFieldOf("focus", "QI").forGetter(attachment -> attachment.currentFocus.name()),
            Codec.LONG.optionalFieldOf("focus_change_time", -72000L).forGetter(attachment -> attachment.lastFocusChangeTime),
            BREAKTHROUGH_DATA_CODEC.fieldOf("breakthrough").forGetter(PlayerCultivationAttachment::getBreakthroughData),
            Codec.LONG.optionalFieldOf("time_qi_realm", 0L).forGetter(attachment -> attachment.timeInCurrentQiRealm),
            Codec.LONG.optionalFieldOf("time_body_realm", 0L).forGetter(attachment -> attachment.timeInCurrentBodyRealm),
            Codec.LONG.optionalFieldOf("total_qi_time", 0L).forGetter(attachment -> attachment.totalQiCultivationTime),
            Codec.LONG.optionalFieldOf("total_body_time", 0L).forGetter(attachment -> attachment.totalBodyCultivationTime),
            BONUS_DATA_CODEC.fieldOf("bonuses").forGetter(PlayerCultivationAttachment::getBonusData)
        ).apply(instance, PlayerCultivationAttachment::decode)
    );

    // Helper methods for codec
    private RealmData getRealmData() {
        return new RealmData(
            currentQiRealm.getId(),
            previousQiRealm != null ? previousQiRealm.getId() : "",
            currentBodyRealm.getId(),
            previousBodyRealm != null ? previousBodyRealm.getId() : ""
        );
    }

    private CultivationProgress getCultivationProgress() {
        return new CultivationProgress(
            currentQiCultivation, maxQiCultivation, qiCultivationFraction,
            currentBodyCultivation, maxBodyCultivation, bodyCultivationFraction
        );
    }

    private BreakthroughData getBreakthroughData() {
        return new BreakthroughData(
            qiBreakthroughAttempts, bodyBreakthroughAttempts,
            lastQiBreakthroughAttempt, lastBodyBreakthroughAttempt,
            qiBreakthroughCooldown, bodyBreakthroughCooldown
        );
    }

    private BonusData getBonusData() {
        return new BonusData(
            qiCultivationAdditiveBonus, qiCultivationMultiplicativeBonus,
            bodyCultivationAdditiveBonus, bodyCultivationMultiplicativeBonus
        );
    }

    // Decode method for creating instances from saved data
    private static PlayerCultivationAttachment decode(
        RealmData realms, CultivationProgress progress, String focus, long focusTime,
        BreakthroughData breakthrough, long timeQi, long timeBody,
        long totalQi, long totalBody, BonusData bonuses) {

        PlayerCultivationAttachment data = new PlayerCultivationAttachment();

        // Restore realms
        data.currentQiRealm = ICModCultivationRealms.get(realms.currentQiRealmId);
        if (!realms.previousQiRealmId.isEmpty()) {
            data.previousQiRealm = ICModCultivationRealms.get(realms.previousQiRealmId);
        }
        data.currentBodyRealm = ICModCultivationRealms.get(realms.currentBodyRealmId);
        if (!realms.previousBodyRealmId.isEmpty()) {
            data.previousBodyRealm = ICModCultivationRealms.get(realms.previousBodyRealmId);
        }

        // Restore progress
        data.currentQiCultivation = progress.currentQi;
        data.maxQiCultivation = progress.maxQi;
        data.qiCultivationFraction = progress.qiFraction;
        data.currentBodyCultivation = progress.currentBody;
        data.maxBodyCultivation = progress.maxBody;
        data.bodyCultivationFraction = progress.bodyFraction;

        // Restore focus
        try {
            data.currentFocus = CultivationType.valueOf(focus);
        } catch (IllegalArgumentException e) {
            data.currentFocus = CultivationType.QI;
        }
        data.lastFocusChangeTime = focusTime;

        // Restore breakthrough data
        data.qiBreakthroughAttempts = breakthrough.qiAttempts;
        data.bodyBreakthroughAttempts = breakthrough.bodyAttempts;
        data.lastQiBreakthroughAttempt = breakthrough.lastQiAttempt;
        data.lastBodyBreakthroughAttempt = breakthrough.lastBodyAttempt;
        data.qiBreakthroughCooldown = breakthrough.qiCooldown;
        data.bodyBreakthroughCooldown = breakthrough.bodyCooldown;

        // Restore time tracking
        data.timeInCurrentQiRealm = timeQi;
        data.timeInCurrentBodyRealm = timeBody;
        data.totalQiCultivationTime = totalQi;
        data.totalBodyCultivationTime = totalBody;

        // Restore bonuses
        data.qiCultivationAdditiveBonus = bonuses.qiAdditive;
        data.qiCultivationMultiplicativeBonus = bonuses.qiMultiplicative;
        data.bodyCultivationAdditiveBonus = bonuses.bodyAdditive;
        data.bodyCultivationMultiplicativeBonus = bonuses.bodyMultiplicative;

        return data;
    }
}