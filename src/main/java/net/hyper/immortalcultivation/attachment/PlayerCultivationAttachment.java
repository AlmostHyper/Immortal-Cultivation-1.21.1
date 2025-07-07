package net.hyper.immortalcultivation.attachment;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.interfaces.ICultivationData;
import net.hyper.immortalcultivation.interfaces.IDirtyTrackable;
import net.hyper.immortalcultivation.interfaces.ITickable;
import net.hyper.immortalcultivation.cultivation.CultivationRealm;
import net.hyper.immortalcultivation.cultivation.data.CultivationRealmData;
import net.hyper.immortalcultivation.cultivation.data.JsonCultivationRealm;
import net.hyper.immortalcultivation.network.server.CultivationSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.registry.ICModNetwork;
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
import java.util.EnumMap;
import java.util.Map;

/**
 * Unified cultivation data attachment implementing ICultivationData.
 * Consolidates: PlayerRealmAttachment, PlayerQiCultivationAttachment,
 * PlayerBodyCultivationAttachment, PlayerCultivationFocusAttachment,
 * and PlayerCultivationBonusAttachment into a single, generic system.
 */
public class PlayerCultivationAttachment implements ICultivationData, IDirtyTrackable, ITickable {

    // === Default Values ===
    private static final long DEFAULT_MAX_CULTIVATION = 100L;
    private static final String DEFAULT_MORTAL_REALM_ID = "mortal";
    private static final long MILESTONE_THRESHOLD = 1000L;
    private static final long FOCUS_CHANGE_COOLDOWN = 24000;

    // === Player Reference ===
    @Nullable
    private Player player;

    // === Unified Cultivation Data ===
    private final Map<CultivationType, CultivationRealm> currentRealms = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, CultivationRealm> previousRealms = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> currentCultivation = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> maxCultivation = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Double> cultivationFraction = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Double> accumulationRate = new EnumMap<>(CultivationType.class);

    // === Milestone Tracking ===
    private final Map<CultivationType, Long> totalCultivationEarned = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> lastMilestone = new EnumMap<>(CultivationType.class);

    // === Bonus System ===
    private final Map<CultivationType, Double> additiveBonus = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Double> multiplicativeBonus = new EnumMap<>(CultivationType.class);

    // === Time Tracking ===
    private final Map<CultivationType, Long> timeInCurrentRealm = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> totalCultivationTime = new EnumMap<>(CultivationType.class);

    // === Focus System ===
    private CultivationType currentFocus = CultivationType.QI;
    private long lastFocusChangeTime = -FOCUS_CHANGE_COOLDOWN;

    // === Sync Management ===
    private int tickCounter = 0;
    private static final int SYNC_INTERVAL = 20;
    private long changeCounter = 0;
    private boolean isDirty = false;

    // === CONSTRUCTORS ===

    public PlayerCultivationAttachment() {
        initializeDefaults();
    }

    private void initializeDefaults() {
        CultivationRealm defaultRealm = getDefaultMortalRealm();

        for (CultivationType type : CultivationType.values()) {
            currentRealms.put(type, defaultRealm);
            currentCultivation.put(type, 0L);
            maxCultivation.put(type, getSafeMaxCultivation(defaultRealm));
            cultivationFraction.put(type, 0.0);
            accumulationRate.put(type, 0.0);
            totalCultivationEarned.put(type, 0L);
            lastMilestone.put(type, 0L);
            additiveBonus.put(type, 0.0);
            multiplicativeBonus.put(type, 0.0);
            timeInCurrentRealm.put(type, 0L);
            totalCultivationTime.put(type, 0L);
        }
    }

    /**
     * Safely gets the mortal realm, handling cases where JSON isn't loaded yet
     */
    private CultivationRealm getDefaultMortalRealm() {
        CultivationRealm mortalRealm = ICModCultivationRealms.get(DEFAULT_MORTAL_REALM_ID);
        if (mortalRealm != null) {
            return mortalRealm;
        }

        ImmortalCultivation.LOGGER.warn("Mortal realm not found during initialization, using temporary default");
        return createTemporaryMortalRealm();
    }

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

    private CultivationRealm createTemporaryMortalRealm() {
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

    // === PLAYER REFERENCE MANAGEMENT ===

    public void setPlayer(Player player) {
        this.player = player;
        ImmortalCultivation.LOGGER.debug("Set player reference for PlayerCultivationAttachment: {}",
            player != null ? player.getName().getString() : "null");
    }

    @Nullable
    public Player getPlayerSafe() {
        return (player != null && player.isAlive()) ? player : null;
    }

    public void updateRealmsAfterJsonLoad() {
        CultivationRealm newMortalRealm = ICModCultivationRealms.get(DEFAULT_MORTAL_REALM_ID);
        if (newMortalRealm == null) return;

        for (CultivationType type : CultivationType.values()) {
            CultivationRealm currentRealm = currentRealms.get(type);
            if (currentRealm == null || DEFAULT_MORTAL_REALM_ID.equals(currentRealm.getId())) {
                ImmortalCultivation.LOGGER.info("Updating {} realm from temporary to loaded JSON realm", type.getDisplayName());
                currentRealms.put(type, newMortalRealm);
                maxCultivation.put(type, newMortalRealm.getBaseMaxCultivation());
            }
        }
    }

    // === ICultivationData IMPLEMENTATION ===

    @Override
    public long getBaseCultivation(CultivationType type) {
        return currentRealms.get(type).getBaseMaxCultivation();
    }

    @Override
    public long getCurrentCultivation(CultivationType type) {
        return currentCultivation.get(type);
    }

    @Override
    public void setCurrentCultivation(CultivationType type, long value) {
        long oldValue = currentCultivation.get(type);
        long newValue = Math.max(0, Math.min(value, maxCultivation.get(type)));
        currentCultivation.put(type, newValue);

        if (oldValue != newValue) {
            markDirty();
        }
    }

    @Override
    public long getMaxCultivation(CultivationType type) {
        return maxCultivation.get(type);
    }

    @Override
    public long addCultivation(CultivationType type, long amount) {
        if (amount <= 0) return 0;

        totalCultivationEarned.merge(type, amount, Long::sum);

        if (player != null) {
            ImmortalCultivation.LOGGER.info("DEBUG: Player {} gained {} {} cultivation points (direct). Total earned: {}",
                player.getName().getString(), amount, type.getDisplayName(), totalCultivationEarned.get(type));
        }

        long currentBefore = currentCultivation.get(type);
        setCurrentCultivation(type, currentBefore + amount);
        long actualAdded = currentCultivation.get(type) - currentBefore;

        checkMilestones(type);
        return actualAdded;
    }

    @Override
    public double getProgress(CultivationType type) {
        long max = maxCultivation.get(type);
        return max > 0 ? (double) currentCultivation.get(type) / max : 0.0;
    }

    @Override
    public double getBaseCultivationRate(CultivationType type) {
        return currentRealms.get(type).getBaseCultivationRate();
    }

    @Override
    public double getEffectiveCultivationRate(CultivationType type) {
        double baseRate = getBaseCultivationRate(type);
        Player player = getPlayerSafe();
        if (player == null) return baseRate;

        // Apply talent multipliers
        try {
            PlayerTalentAttachment talent = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talent != null) {
                baseRate *= (type == CultivationType.QI) ?
                    talent.getSpiritualRootMultiplier() : talent.getPhysiqueMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying talent multiplier", e);
        }

        // Apply race multipliers
        try {
            PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            if (race != null && race.hasRaceAssigned()) {
                baseRate *= (type == CultivationType.QI) ?
                    race.getQiMultiplier() : race.getBodyMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying race multiplier", e);
        }

        // Apply bonuses
        baseRate = applyCultivationBonuses(type, baseRate);

        // Apply focus multiplier
        if (currentFocus == type) {
            baseRate *= 2.0;
        }

        return baseRate;
    }

    @Override
    public double getRegenFraction(CultivationType type) {
        return cultivationFraction.get(type);
    }

    @Override
    public void setRegenFraction(CultivationType type, double fraction) {
        cultivationFraction.put(type, fraction);
    }

    @Override
    public void addRegenFraction(CultivationType type, double amount) {
        double current = cultivationFraction.get(type);
        cultivationFraction.put(type, current + amount);

        double wholePoints = cultivationFraction.get(type);
        if (wholePoints > 0) {
            cultivationFraction.put(type, cultivationFraction.get(type) - wholePoints);

            totalCultivationEarned.merge(type, (long) wholePoints, Long::sum);
            checkMilestones(type);
            markDirty();
        }
    }

    // === HELPER METHODS ===

    private double applyCultivationBonuses(CultivationType type, double baseRate) {
        return (baseRate + additiveBonus.get(type)) * (1.0 + multiplicativeBonus.get(type));
    }

    private void checkMilestones(CultivationType type) {
        long currentMilestone = totalCultivationEarned.get(type) / MILESTONE_THRESHOLD;
        long lastMilestoneValue = lastMilestone.get(type);

        if (currentMilestone > lastMilestoneValue) {
            long milestonesEarned = currentMilestone - lastMilestoneValue;
            lastMilestone.put(type, currentMilestone);

            if (player != null) {
                PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
                if (resources != null) {
                    if (type == CultivationType.QI) {
                        // Qi milestones: +2 Qi, +1 HP per milestone
                        resources.addPermanentMaxQiBonus(milestonesEarned * 2);
                        resources.addPermanentHealthBonus(milestonesEarned);

                        ImmortalCultivation.LOGGER.info("Player {} reached {} cultivation milestone {} - gained +{} Qi, +{} HP",
                            player.getName().getString(), type.getDisplayName(), currentMilestone,
                            milestonesEarned, milestonesEarned * 2);
                    } else if (type == CultivationType.BODY) {
                        // Body milestones: +1 Qi, +2 HP per milestone
                        resources.addPermanentMaxQiBonus(milestonesEarned);
                        resources.addPermanentHealthBonus(milestonesEarned * 2);

                        ImmortalCultivation.LOGGER.info("Player {} reached {} cultivation milestone {} - gained +{} Qi, +{} HP",
                            player.getName().getString(), type.getDisplayName(), currentMilestone,
                            milestonesEarned, milestonesEarned * 2);
                    }
                }
            }
        }
    }

    // === REALM MANAGEMENT ===

    public CultivationRealm getCurrentRealm(CultivationType type) {
        return currentRealms.get(type);
    }

    public void setCurrentRealm(CultivationType type, CultivationRealm realm) {
        CultivationRealm previousRealm = currentRealms.get(type);
        previousRealms.put(type, previousRealm);
        currentRealms.put(type, realm);

        applyPermanentBonusesIfAdvancement(previousRealm, realm, type);
        updateCultivationLimit(type);

        markDirty();
        forceSync();

        // Call realm advancement events using unified interface
        if (player != null && realm != null) {
            realm.onRealmAdvancement(player, this, previousRealm, type);
        }
    }

    private void updateCultivationLimit(CultivationType type) {
        long newMax = currentRealms.get(type).getBaseMaxCultivation();
        if (newMax != maxCultivation.get(type)) {
            maxCultivation.put(type, Math.max(1, newMax));
            if (currentCultivation.get(type) > newMax) {
                currentCultivation.put(type, newMax);
            }
        }
    }

    private void applyPermanentBonusesIfAdvancement(@Nullable CultivationRealm previousRealm,
                                                    CultivationRealm newRealm,
                                                    CultivationType cultivationType) {
        if (player == null || newRealm == null) return;

        try {
            boolean isAdvancement = previousRealm == null || newRealm.getRealmLevel() > previousRealm.getRealmLevel();
            if (!isAdvancement) return;

            // Get JSON-defined bonuses
            long healthBonus = newRealm.getBaseHealth();
            double healthRegenBonus = newRealm.getBaseHealthRegeneration();
            double attackBonus = newRealm.getBaseAttackDamage();
            double speedBonus = newRealm.getBaseMovementSpeed();
            double jumpBonus = newRealm.getBaseJumpHeight();
            long qiBonus = newRealm.getBaseMaxQi();
            double qiRegenBonus = newRealm.getBaseQiRegenerationRate();

            // Apply resource bonuses
            PlayerResourceAttachment resourceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resourceAttachment != null) {
                if (healthBonus > 0) {
                    resourceAttachment.addPermanentHealthBonus(healthBonus);
                    resourceAttachment.addPermanentMaxBodyBonus(healthBonus); // Body bonus derived from health
                }
                if (healthRegenBonus > 0) {
                    resourceAttachment.addPermanentHealthRegenBonus(healthRegenBonus);
                    resourceAttachment.addPermanentBodyRegenBonus(healthRegenBonus);
                }
                if (qiBonus > 0) {
                    resourceAttachment.addPermanentMaxQiBonus(qiBonus);
                }
                if (qiRegenBonus > 0) {
                    resourceAttachment.addPermanentQiRegenBonus(qiRegenBonus);
                }
            }

            // Apply combat bonuses
            PlayerCombatStatsAttachment combatAttachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatAttachment != null) {
                if (attackBonus > 0) combatAttachment.addPermanentAttackDamageBonus(attackBonus);
                if (speedBonus > 0) combatAttachment.addPermanentMovementSpeedBonus(speedBonus);
                if (jumpBonus > 0) combatAttachment.addPermanentJumpHeightBonus(jumpBonus);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying permanent bonuses for {} realm advancement to {}",
                cultivationType.getDisplayName(), newRealm.getDisplayName(), e);
        }
    }

    // === FOCUS MANAGEMENT ===

    public CultivationType getCurrentFocus() {
        return currentFocus;
    }

    public boolean toggleFocus() {
        if (player == null) return false;

        long currentTime = player.level().getGameTime();
        long timeSinceLastChange = currentTime - lastFocusChangeTime;

        if (timeSinceLastChange < FOCUS_CHANGE_COOLDOWN) {
            return false;
        }

        CultivationType oldFocus = currentFocus;
        currentFocus = (currentFocus == CultivationType.QI) ? CultivationType.BODY : CultivationType.QI;
        lastFocusChangeTime = currentTime;

        ImmortalCultivation.LOGGER.info("Focus changed from {} to {} for player {}",
            oldFocus.getDisplayName(), currentFocus.getDisplayName(),
            player.getName().getString());

        markDirty();
        forceSync();
        return true;
    }

    // === BONUS MANAGEMENT ===

    public void addBonus(CultivationType type, double additive, double multiplicative) {
        additiveBonus.merge(type, additive, Double::sum);
        multiplicativeBonus.merge(type, multiplicative, Double::sum);
        markDirty();
    }

    public void addBonusWithDiminishing(CultivationType type, double additive, double multiplicative, double effectivenessMultiplier) {
        additiveBonus.merge(type, additive * effectivenessMultiplier, Double::sum);
        multiplicativeBonus.merge(type, multiplicative * effectivenessMultiplier, Double::sum);
        markDirty();
    }

    public double getRawAdditiveBonus(CultivationType type) {
        return additiveBonus.get(type);
    }

    public double getRawMultiplicativeBonus(CultivationType type) {
        return multiplicativeBonus.get(type);
    }

    // === BREAKTHROUGH DELEGATION ===

    private PlayerBreakthroughAttachment getBreakthroughAttachment() {
        if (player == null) return null;

        PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
        if (breakthrough != null && breakthrough.getPlayer() == null) {
            breakthrough.setPlayer(player);
        }
        return breakthrough;
    }

    public PlayerBreakthroughAttachment.BreakthroughResult attemptBreakthrough(CultivationType type) {
        PlayerBreakthroughAttachment breakthrough = getBreakthroughAttachment();
        if (breakthrough == null) {
            return new PlayerBreakthroughAttachment.BreakthroughResult(false, "Breakthrough system unavailable", null,
                PlayerBreakthroughAttachment.BreakthroughType.MINOR, 0.0, 0L);
        }

        return (type == CultivationType.QI) ? breakthrough.attemptQiBreakthrough() : breakthrough.attemptBodyBreakthrough();
    }

    // === TICK PROCESSING ===

    @Override
    public void tick() {
        if (player == null) return;

        // Update time tracking
        for (CultivationType type : CultivationType.values()) {
            timeInCurrentRealm.merge(type, 1L, Long::sum);
            totalCultivationTime.merge(type, 1L, Long::sum);
        }

        // Update accumulation rates
        for (CultivationType type : CultivationType.values()) {
            accumulationRate.put(type, getEffectiveCultivationRate(type) / 20.0);
        }

        // Accumulate cultivation
        for (CultivationType type : CultivationType.values()) {
            double rate = accumulationRate.get(type);
            if (rate > 0 && getCurrentCultivation(type) < getMaxCultivation(type)) {
                addRegenFraction(type, rate);
            }
        }

        // Handle sync
        tickCounter++;
        if (tickCounter >= SYNC_INTERVAL) {
            tickCounter = 0;
            if (shouldSyncNow()) {
                forceSync();
            }
        }
    }

    // === SYNC MANAGEMENT ===

    @Override
    public boolean shouldSyncNow() {
        return isDirty;
    }

    @Override
    public void forceSync() {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        try {
            CultivationSyncPacket packet = new CultivationSyncPacket(
                currentRealms.get(CultivationType.QI).getId(),
                previousRealms.get(CultivationType.QI) != null ? previousRealms.get(CultivationType.QI).getId() : "",
                currentRealms.get(CultivationType.BODY).getId(),
                previousRealms.get(CultivationType.BODY) != null ? previousRealms.get(CultivationType.BODY).getId() : "",
                currentCultivation.get(CultivationType.QI),
                maxCultivation.get(CultivationType.QI),
                currentCultivation.get(CultivationType.BODY),
                maxCultivation.get(CultivationType.BODY),
                currentFocus,
                additiveBonus.get(CultivationType.QI),
                multiplicativeBonus.get(CultivationType.QI),
                additiveBonus.get(CultivationType.BODY),
                multiplicativeBonus.get(CultivationType.BODY)
            );

            ICModNetwork.sendToPlayer(packet, serverPlayer);
            clearDirty();

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error syncing cultivation data for player {}",
                serverPlayer.getName().getString(), e);
        }
    }

    @Override
    public void markDirty() {
        this.isDirty = true;
        incrementChangeCounter();
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }

    @Override
    public void clearDirty() {
        this.isDirty = false;
        resetChangeCounter();
    }

    @Override
    public long getChangeCounter() {
        return changeCounter;
    }

    @Override
    public void incrementChangeCounter() {
        this.changeCounter++;
    }

    @Override
    public void resetChangeCounter() {
        this.changeCounter = 0;
    }

    // === SERIALIZATION ===

    // Codec sub-records for organization
    private record RealmData(String currentQiRealmId, String previousQiRealmId,
                             String currentBodyRealmId, String previousBodyRealmId) {}

    private record CultivationProgress(
        long currentQi, long maxQi, double qiFraction,
        long currentBody, long maxBody, double bodyFraction,
        long totalQiEarned, long totalBodyEarned,
        long lastQiMilestone, long lastBodyMilestone
    ) {}

    private record BonusData(double qiAdditive, double qiMultiplicative,
                             double bodyAdditive, double bodyMultiplicative) {}

    private static final Codec<RealmData> REALM_DATA_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("current_qi_realm").forGetter(RealmData::currentQiRealmId),
            Codec.STRING.optionalFieldOf("previous_qi_realm", "").forGetter(RealmData::previousQiRealmId),
            Codec.STRING.fieldOf("current_body_realm").forGetter(RealmData::currentBodyRealmId),
            Codec.STRING.optionalFieldOf("previous_body_realm", "").forGetter(RealmData::previousBodyRealmId)
        ).apply(instance, RealmData::new)
    );

    private static final Codec<CultivationProgress> CULTIVATION_PROGRESS_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.optionalFieldOf("current_qi", 0L).forGetter(CultivationProgress::currentQi),
            Codec.LONG.optionalFieldOf("max_qi", 1000L).forGetter(CultivationProgress::maxQi),
            Codec.DOUBLE.optionalFieldOf("qi_fraction", 0.0).forGetter(CultivationProgress::qiFraction),
            Codec.LONG.optionalFieldOf("current_body", 0L).forGetter(CultivationProgress::currentBody),
            Codec.LONG.optionalFieldOf("max_body", 1000L).forGetter(CultivationProgress::maxBody),
            Codec.DOUBLE.optionalFieldOf("body_fraction", 0.0).forGetter(CultivationProgress::bodyFraction),
            Codec.LONG.optionalFieldOf("total_qi_earned", 0L).forGetter(CultivationProgress::totalQiEarned),
            Codec.LONG.optionalFieldOf("total_body_earned", 0L).forGetter(CultivationProgress::totalBodyEarned),
            Codec.LONG.optionalFieldOf("last_qi_milestone", 0L).forGetter(CultivationProgress::lastQiMilestone),
            Codec.LONG.optionalFieldOf("last_body_milestone", 0L).forGetter(CultivationProgress::lastBodyMilestone)
        ).apply(instance, CultivationProgress::new)
    );

    private static final Codec<BonusData> BONUS_DATA_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.optionalFieldOf("qi_additive", 0.0).forGetter(BonusData::qiAdditive),
            Codec.DOUBLE.optionalFieldOf("qi_multiplicative", 0.0).forGetter(BonusData::qiMultiplicative),
            Codec.DOUBLE.optionalFieldOf("body_additive", 0.0).forGetter(BonusData::bodyAdditive),
            Codec.DOUBLE.optionalFieldOf("body_multiplicative", 0.0).forGetter(BonusData::bodyMultiplicative)
        ).apply(instance, BonusData::new)
    );

    public static final Codec<PlayerCultivationAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            REALM_DATA_CODEC.fieldOf("realms").forGetter(PlayerCultivationAttachment::getRealmData),
            CULTIVATION_PROGRESS_CODEC.fieldOf("cultivation").forGetter(PlayerCultivationAttachment::getCultivationProgress),
            Codec.STRING.optionalFieldOf("focus", "QI").forGetter(attachment -> attachment.currentFocus.name()),
            Codec.LONG.optionalFieldOf("focus_change_time", -24000L).forGetter(attachment -> attachment.lastFocusChangeTime),
            Codec.LONG.optionalFieldOf("time_qi_realm", 0L).forGetter(attachment -> attachment.timeInCurrentRealm.get(CultivationType.QI)),
            Codec.LONG.optionalFieldOf("time_body_realm", 0L).forGetter(attachment -> attachment.timeInCurrentRealm.get(CultivationType.BODY)),
            Codec.LONG.optionalFieldOf("total_qi_time", 0L).forGetter(attachment -> attachment.totalCultivationTime.get(CultivationType.QI)),
            Codec.LONG.optionalFieldOf("total_body_time", 0L).forGetter(attachment -> attachment.totalCultivationTime.get(CultivationType.BODY)),
            BONUS_DATA_CODEC.fieldOf("bonuses").forGetter(PlayerCultivationAttachment::getBonusData)
        ).apply(instance, PlayerCultivationAttachment::decode)
    );

    // Helper methods for codec
    private RealmData getRealmData() {
        return new RealmData(
            currentRealms.get(CultivationType.QI).getId(),
            previousRealms.get(CultivationType.QI) != null ? previousRealms.get(CultivationType.QI).getId() : "",
            currentRealms.get(CultivationType.BODY).getId(),
            previousRealms.get(CultivationType.BODY) != null ? previousRealms.get(CultivationType.BODY).getId() : ""
        );
    }

    private CultivationProgress getCultivationProgress() {
        return new CultivationProgress(
            currentCultivation.get(CultivationType.QI), maxCultivation.get(CultivationType.QI), cultivationFraction.get(CultivationType.QI),
            currentCultivation.get(CultivationType.BODY), maxCultivation.get(CultivationType.BODY), cultivationFraction.get(CultivationType.BODY),
            totalCultivationEarned.get(CultivationType.QI), totalCultivationEarned.get(CultivationType.BODY),
            lastMilestone.get(CultivationType.QI), lastMilestone.get(CultivationType.BODY)
        );
    }

    private BonusData getBonusData() {
        return new BonusData(
            additiveBonus.get(CultivationType.QI), multiplicativeBonus.get(CultivationType.QI),
            additiveBonus.get(CultivationType.BODY), multiplicativeBonus.get(CultivationType.BODY)
        );
    }

    private static PlayerCultivationAttachment decode(
        RealmData realms, CultivationProgress progress, String focus, long focusTime,
        long timeQi, long timeBody, long totalQi, long totalBody, BonusData bonuses) {

        PlayerCultivationAttachment data = new PlayerCultivationAttachment();

        // Restore realms
        data.currentRealms.put(CultivationType.QI, ICModCultivationRealms.get(realms.currentQiRealmId));
        data.currentRealms.put(CultivationType.BODY, ICModCultivationRealms.get(realms.currentBodyRealmId));

        if (!realms.previousQiRealmId.isEmpty()) {
            data.previousRealms.put(CultivationType.QI, ICModCultivationRealms.get(realms.previousQiRealmId));
        }
        if (!realms.previousBodyRealmId.isEmpty()) {
            data.previousRealms.put(CultivationType.BODY, ICModCultivationRealms.get(realms.previousBodyRealmId));
        }

        // Restore cultivation progress
        data.currentCultivation.put(CultivationType.QI, progress.currentQi);
        data.maxCultivation.put(CultivationType.QI, progress.maxQi);
        data.cultivationFraction.put(CultivationType.QI, progress.qiFraction);
        data.currentCultivation.put(CultivationType.BODY, progress.currentBody);
        data.maxCultivation.put(CultivationType.BODY, progress.maxBody);
        data.cultivationFraction.put(CultivationType.BODY, progress.bodyFraction);
        data.totalCultivationEarned.put(CultivationType.QI, progress.totalQiEarned);
        data.totalCultivationEarned.put(CultivationType.BODY, progress.totalBodyEarned);
        data.lastMilestone.put(CultivationType.QI, progress.lastQiMilestone);
        data.lastMilestone.put(CultivationType.BODY, progress.lastBodyMilestone);

        // Restore focus
        try {
            data.currentFocus = CultivationType.valueOf(focus);
        } catch (IllegalArgumentException e) {
            data.currentFocus = CultivationType.QI;
        }
        data.lastFocusChangeTime = focusTime;

        // Restore time tracking
        data.timeInCurrentRealm.put(CultivationType.QI, timeQi);
        data.timeInCurrentRealm.put(CultivationType.BODY, timeBody);
        data.totalCultivationTime.put(CultivationType.QI, totalQi);
        data.totalCultivationTime.put(CultivationType.BODY, totalBody);

        // Restore bonuses
        data.additiveBonus.put(CultivationType.QI, bonuses.qiAdditive);
        data.multiplicativeBonus.put(CultivationType.QI, bonuses.qiMultiplicative);
        data.additiveBonus.put(CultivationType.BODY, bonuses.bodyAdditive);
        data.multiplicativeBonus.put(CultivationType.BODY, bonuses.bodyMultiplicative);

        return data;
    }

    // === SERIALIZER ===

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerCultivationAttachment> {
        @Override
        public PlayerCultivationAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerCultivationAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerCultivationAttachment: {}", error))
                .map(pair -> pair.getFirst())
                .orElse(new PlayerCultivationAttachment());

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerCultivationAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encode(attachment, provider.createSerializationContext(NbtOps.INSTANCE), new CompoundTag())
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerCultivationAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }
}