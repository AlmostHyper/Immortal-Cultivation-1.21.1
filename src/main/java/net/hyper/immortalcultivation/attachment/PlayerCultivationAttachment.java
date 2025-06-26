package net.hyper.immortalcultivation.attachment;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IBodyCultivationData;
import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.handler.CombatStatsEventHandler;
import net.hyper.immortalcultivation.network.server.CultivationSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;

/**
 * Unified cultivation data attachment.
 * Merges: PlayerRealmAttachment, PlayerQiCultivationAttachment,
 * PlayerBodyCultivationAttachment, PlayerCultivationFocusAttachment,
 * and PlayerCultivationBonusAttachment
 * Enhanced with comprehensive pill system integration.
 */
public class PlayerCultivationAttachment implements IQiCultivationData, IBodyCultivationData {

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
    private long lastFocusChangeTime = -72000; // CRITICAL FIX: Initialize to allow immediate first switch
    private static final long FOCUS_CHANGE_COOLDOWN = 72000; // 1 hour in ticks

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

    // === Pill System ===
    public enum PillTier {
        LOW_QUALITY(0.005, 0.005, "Low Quality"),
        MEDIUM_QUALITY(0.02, 0.02, "Medium Quality"),
        HIGH_QUALITY(0.1, 0.1, "High Quality"),
        TOP_QUALITY(1.0, 1.0, "Top Quality");

        private final double qiAdditiveBonus;
        private final double bodyAdditiveBonus;
        private final String displayName;

        PillTier(double qiAdditiveBonus, double bodyAdditiveBonus, String displayName) {
            this.qiAdditiveBonus = qiAdditiveBonus;
            this.bodyAdditiveBonus = bodyAdditiveBonus;
            this.displayName = displayName;
        }

        public double getQiAdditiveBonus() {
            return qiAdditiveBonus;
        }

        public double getBodyAdditiveBonus() {
            return bodyAdditiveBonus;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Get a user-friendly description of the Qi bonus amount
         */
        public String getQiBonusDescription() {
            if (qiAdditiveBonus >= 1.0) {
                return "+" + String.format("%.1f", qiAdditiveBonus) + "/sec";
            } else if (qiAdditiveBonus >= 0.1) {
                return "+" + String.format("%.1f", qiAdditiveBonus) + "/sec";
            } else if (qiAdditiveBonus >= 0.01) {
                return "+" + String.format("%.2f", qiAdditiveBonus) + "/sec";
            } else {
                return "+" + String.format("%.3f", qiAdditiveBonus) + "/sec";
            }
        }

        /**
         * Get a user-friendly description of the Body bonus amount
         */
        public String getBodyBonusDescription() {
            if (bodyAdditiveBonus >= 1.0) {
                return "+" + String.format("%.1f", bodyAdditiveBonus) + "/sec";
            } else if (bodyAdditiveBonus >= 0.1) {
                return "+" + String.format("%.1f", bodyAdditiveBonus) + "/sec";
            } else if (bodyAdditiveBonus >= 0.01) {
                return "+" + String.format("%.2f", bodyAdditiveBonus) + "/sec";
            } else {
                return "+" + String.format("%.3f", bodyAdditiveBonus) + "/sec";
            }
        }
    }

    // === Constructor (FIXED - removed void) ===
    public PlayerCultivationAttachment() {
        this.currentQiRealm = ICModCultivationRealms.MORTAL;
        this.currentBodyRealm = ICModCultivationRealms.MORTAL;
        // Initialize max cultivation based on realms
        this.maxQiCultivation = currentQiRealm.getBaseMaxCultivation();
        this.maxBodyCultivation = currentBodyRealm.getBaseMaxCultivation();
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
        return qiCultivationFraction;
    }

    @Override
    public void setRegenFraction(double fraction) {
        this.qiCultivationFraction = fraction;
    }

    @Override
    public void addRegenFraction(double amount) {
        IQiCultivationData.super.addRegenFraction(amount);
        IBodyCultivationData.super.addRegenFraction(amount);
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

    public void setCurrentQiRealm(CultivationRealm realm) {
        this.previousQiRealm = this.currentQiRealm;
        this.currentQiRealm = realm;
        this.maxQiCultivation = realm.getBaseMaxCultivation();
        this.currentQiCultivation = 0;
        this.timeInCurrentQiRealm = 0;
        this.qiBreakthroughAttempts = 0;
        this.qiBreakthroughCooldown = false;

        if (player instanceof ServerPlayer serverPlayer) {
            CombatStatsEventHandler.refreshCombatStats(serverPlayer);
        }
    }

    public void setCurrentBodyRealm(CultivationRealm realm) {
        this.previousBodyRealm = this.currentBodyRealm;
        this.currentBodyRealm = realm;
        this.maxBodyCultivation = realm.getBaseMaxCultivation();
        this.currentBodyCultivation = 0;
        this.timeInCurrentBodyRealm = 0;
        this.bodyBreakthroughAttempts = 0;
        this.bodyBreakthroughCooldown = false;

        if (player instanceof ServerPlayer serverPlayer) {
            CombatStatsEventHandler.refreshCombatStats(serverPlayer);
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

    // === Pill System Implementation ===

    /**
     * Consume a Qi cultivation pill of the specified tier (called by QiCultivationPillItem)
     * @param tier The quality tier of the pill
     * @return true if successful, false if failed
     */
    public boolean consumeQiCultivationPill(PlayerCultivationAttachment.PillTier tier) {
        return consumeQiPill(tier, tier.getDisplayName() + " Qi Cultivation Pill");
    }

    /**
     * Consume a Body cultivation pill of the specified tier (called by BodyCultivationPillItem)
     * @param tier The quality tier of the pill
     * @return true if successful, false if failed
     */
    public boolean consumeBodyCultivationPill(PlayerCultivationAttachment.PillTier tier) {
        return consumeBodyPill(tier, tier.getDisplayName() + " Body Cultivation Pill");
    }

    /**
     * Consume a Qi cultivation pill of the specified tier
     * @param tier The quality tier of the pill
     * @param pillName The name of the pill for logging
     * @return true if successful, false if failed
     */
    public boolean consumeQiPill(PillTier tier, String pillName) {
        if (player == null) return false;

        try {
            double bonusToAdd = tier.getQiAdditiveBonus();

            // Apply the bonus
            this.qiCultivationAdditiveBonus += bonusToAdd;

            ImmortalCultivation.LOGGER.info("Player {} consumed {} Qi pill ({}). Added: {} rate/sec (Total additive: {})",
                player.getName().getString(), tier.name(), pillName,
                String.format("%.3f", bonusToAdd),
                String.format("%.3f", qiCultivationAdditiveBonus));

            // Force sync to update client
            if (!player.level().isClientSide()) {
                forceSync();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error consuming Qi pill for player {}",
                player.getName().getString(), e);
            return false;
        }

        return true;
    }

    /**
     * Consume a Body cultivation pill of the specified tier
     * @param tier The quality tier of the pill
     * @param pillName The name of the pill for logging
     * @return true if successful, false if failed
     */
    public boolean consumeBodyPill(PillTier tier, String pillName) {
        if (player == null) return false;

        try {
            double bonusToAdd = tier.getBodyAdditiveBonus();

            // Apply the bonus
            this.bodyCultivationAdditiveBonus += bonusToAdd;

            ImmortalCultivation.LOGGER.info("Player {} consumed {} Body pill ({}). Added: {} rate/sec (Total additive: {})",
                player.getName().getString(), tier.name(), pillName,
                String.format("%.3f", bonusToAdd),
                String.format("%.3f", bodyCultivationAdditiveBonus));

            // Force sync to update client
            if (!player.level().isClientSide()) {
                forceSync();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error consuming Body pill for player {}",
                player.getName().getString(), e);
            return false;
        }

        return true;
    }

    /**
     * Consume a dual cultivation pill that affects both Qi and Body
     * @param tier The quality tier of the pill
     * @param pillName The name of the pill for logging
     * @return true if successful, false if failed
     */
    public boolean consumeDualPill(PillTier tier, String pillName) {
        if (player == null) return false;

        try {
            double qiBonusToAdd = tier.getQiAdditiveBonus();
            double bodyBonusToAdd = tier.getBodyAdditiveBonus();

            // Apply both bonuses
            this.qiCultivationAdditiveBonus += qiBonusToAdd;
            this.bodyCultivationAdditiveBonus += bodyBonusToAdd;

            ImmortalCultivation.LOGGER.info("Player {} consumed {} Dual pill ({}). Added: Qi {} + Body {} rate/sec",
                player.getName().getString(), tier.name(), pillName,
                String.format("%.3f", qiBonusToAdd),
                String.format("%.3f", bodyBonusToAdd));

            // Force sync to update client
            if (!player.level().isClientSide()) {
                forceSync();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error consuming Dual pill for player {}",
                player.getName().getString(), e);
            return false;
        }

        return true;
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

    private long calculateStateHash() {
        return Objects.hash(
            currentQiRealm.getId(),
            currentQiCultivation,
            currentBodyRealm.getId(),
            currentBodyCultivation,
            currentFocus,
            qiCultivationAdditiveBonus,
            bodyCultivationAdditiveBonus
        );
    }

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
                bodyCultivationMultiplicativeBonus
            );
            ICModNetwork.sendToPlayer(packet, serverPlayer);
        }
    }

    public void forceSync() {
        lastSyncedHash = 0; // Force sync on next check
        syncToClient();
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

    // === Sync Application Method ===

    public void applySync(String qiRealmId, String prevQiRealmId, String bodyRealmId, String prevBodyRealmId,
                          long qiCurrent, long qiMax, long bodyCurrent, long bodyMax,
                          CultivationType focus, double qiAdd, double qiMult, double bodyAdd, double bodyMult) {

        // Update realms
        this.currentQiRealm = ICModCultivationRealms.get(qiRealmId);
        this.previousQiRealm = prevQiRealmId.isEmpty() ? null : ICModCultivationRealms.get(prevQiRealmId);
        this.currentBodyRealm = ICModCultivationRealms.get(bodyRealmId);
        this.previousBodyRealm = prevBodyRealmId.isEmpty() ? null : ICModCultivationRealms.get(prevBodyRealmId);

        // Update cultivation progress
        this.currentQiCultivation = qiCurrent;
        this.maxQiCultivation = qiMax;
        this.currentBodyCultivation = bodyCurrent;
        this.maxBodyCultivation = bodyMax;

        // Update focus
        this.currentFocus = focus;

        // Update bonuses
        this.qiCultivationAdditiveBonus = qiAdd;
        this.qiCultivationMultiplicativeBonus = qiMult;
        this.bodyCultivationAdditiveBonus = bodyAdd;
        this.bodyCultivationMultiplicativeBonus = bodyMult;

        ImmortalCultivation.LOGGER.debug("Applied cultivation sync - Qi: {}/{}, Body: {}/{}, Focus: {}",
            qiCurrent, qiMax, bodyCurrent, bodyMax, focus.getDisplayName());
    }

    // === Additional Getters for Network/UI ===

    public double getQiMultiplicativeBonus() {
        return qiCultivationMultiplicativeBonus;
    }

    public double getBodyMultiplicativeBonus() {
        return bodyCultivationMultiplicativeBonus;
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

    public record BreakthroughResult(boolean success, String message, @Nullable CultivationRealm newRealm) {}

    public BreakthroughResult attemptQiBreakthrough() {
        if (currentQiCultivation < maxQiCultivation) {
            return new BreakthroughResult(false, "Qi cultivation not at maximum", null);
        }

        if (qiBreakthroughCooldown) {
            return new BreakthroughResult(false, "Breakthrough on cooldown", null);
        }

        List<CultivationRealm> nextRealms = currentQiRealm.getNextCultivationRealms();
        if (nextRealms.isEmpty()) {
            return new BreakthroughResult(false, "No higher realm available", null);
        }

        double chance = 0.1 + (qiBreakthroughAttempts * 0.05);
        chance = Math.min(chance, 0.95);

        qiBreakthroughAttempts++;
        lastQiBreakthroughAttempt = player.level().getGameTime();

        if (player.getRandom().nextDouble() < chance) {
            CultivationRealm newRealm = nextRealms.get(0);
            setCurrentQiRealm(newRealm);
            return new BreakthroughResult(true, "Qi breakthrough successful!", newRealm);
        } else {
            qiBreakthroughCooldown = true;
            return new BreakthroughResult(false, "Breakthrough failed", null);
        }
    }

    public BreakthroughResult attemptBodyBreakthrough() {
        if (currentBodyCultivation < maxBodyCultivation) {
            return new BreakthroughResult(false, "Body cultivation not at maximum", null);
        }

        if (bodyBreakthroughCooldown) {
            return new BreakthroughResult(false, "Breakthrough on cooldown", null);
        }

        List<CultivationRealm> nextRealms = currentBodyRealm.getNextCultivationRealms();
        if (nextRealms.isEmpty()) {
            return new BreakthroughResult(false, "No higher realm available", null);
        }

        double chance = 0.1 + (bodyBreakthroughAttempts * 0.05);
        chance = Math.min(chance, 0.95);

        bodyBreakthroughAttempts++;
        lastBodyBreakthroughAttempt = player.level().getGameTime();

        if (player.getRandom().nextDouble() < chance) {
            CultivationRealm newRealm = nextRealms.get(0);
            setCurrentBodyRealm(newRealm);
            return new BreakthroughResult(true, "Body breakthrough successful!", newRealm);
        } else {
            bodyBreakthroughCooldown = true;
            return new BreakthroughResult(false, "Breakthrough failed", null);
        }
    }
}