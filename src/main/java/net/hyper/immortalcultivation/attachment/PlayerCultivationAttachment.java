package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IBodyCultivationData;
import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.handler.CombatStatsEventHandler;
import net.hyper.immortalcultivation.network.server.CultivationSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Unified cultivation data attachment.
 * Merges: PlayerRealmAttachment, PlayerQiCultivationAttachment,
 * PlayerBodyCultivationAttachment, PlayerCultivationFocusAttachment,
 * and PlayerCultivationBonusAttachment
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
    private double qiAccumulationRate = 0.0;

    // === Body Cultivation Data (from PlayerBodyCultivationAttachment) ===
    private long currentBodyCultivation = 0;
    private long maxBodyCultivation = 1000; // Start with default max
    private double bodyCultivationFraction = 0.0;
    private double bodyAccumulationRate = 0.0;

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
    private long lastFocusChangeTime = 0;
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
            PlayerTalentAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (roots != null) {
                baseRate *= roots.getSpiritualRootMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying spiritual roots", e);
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
            PlayerTalentAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (roots != null) {
                baseCirculatingRate *= roots.getSpiritualRootMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying spiritual roots", e);
        }

        baseCirculatingRate = applyQiCultivationBonuses(baseCirculatingRate);

        if (currentFocus == CultivationType.QI) {
            baseCirculatingRate *= 2.0;
        }

        return baseCirculatingRate;
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
        this.qiCultivationFraction += amount;
    }

    @Override
    public long getBaseQiCultivation() {
        return currentQiRealm.getBaseMaxCultivation();
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

    public boolean toggleFocus() {
        if (player == null) return false;

        long currentTime = player.level().getGameTime();
        if (currentTime - lastFocusChangeTime < FOCUS_CHANGE_COOLDOWN) {
            return false;
        }

        currentFocus = (currentFocus == CultivationType.QI) ? CultivationType.BODY : CultivationType.QI;
        lastFocusChangeTime = currentTime;

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

    // === Breakthrough System ===

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
            return new BreakthroughResult(false, "Qi breakthrough failed", null);
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
            return new BreakthroughResult(false, "Body breakthrough failed", null);
        }
    }

    // === Pill System ===

    /**
     * Attempt to consume a Qi cultivation pill
     * @param tier The tier of pill being consumed
     * @return true if successful, false if failed
     */
    public boolean consumeQiCultivationPill(PillTier tier) {
        // Add the pill's additive bonus
        double bonusToAdd = tier.getQiAdditiveBonus();
        qiCultivationAdditiveBonus += bonusToAdd;

        // Log and sync
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.info("Player {} consumed {} Qi cultivation pill. Added: {} rate/sec (Total additive: {})",
                player.getName().getString(), tier.name(),
                String.format("%.3f", bonusToAdd),
                String.format("%.3f", qiCultivationAdditiveBonus));

            if (!player.level().isClientSide()) {
                forceSync();
            }
        }

        return true;
    }

    /**
     * Attempt to consume a Body cultivation pill
     * @param tier The tier of pill being consumed
     * @return true if successful, false if failed
     */
    public boolean consumeBodyCultivationPill(PillTier tier) {
        // Add the pill's additive bonus
        double bonusToAdd = tier.getBodyAdditiveBonus();
        bodyCultivationAdditiveBonus += bonusToAdd;

        // Log and sync
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.info("Player {} consumed {} Body cultivation pill. Added: {} rate/sec (Total additive: {})",
                player.getName().getString(), tier.name(),
                String.format("%.3f", bonusToAdd),
                String.format("%.3f", bodyCultivationAdditiveBonus));

            if (!player.level().isClientSide()) {
                forceSync();
            }
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

    // === Pill Tier Enum ===

    public enum PillTier {
        LOW_QUALITY(0.005, 0.005),
        MEDIUM_QUALITY(0.02, 0.02),
        HIGH_QUALITY(0.1, 0.1),
        TOP_QUALITY(1.0, 1.0);

        private final double qiAdditiveBonus;
        private final double bodyAdditiveBonus;

        PillTier(double qiAdditiveBonus, double bodyAdditiveBonus) {
            this.qiAdditiveBonus = qiAdditiveBonus;
            this.bodyAdditiveBonus = bodyAdditiveBonus;
        }

        public double getQiAdditiveBonus() {
            return qiAdditiveBonus;
        }

        public double getBodyAdditiveBonus() {
            return bodyAdditiveBonus;
        }

        public String getDisplayName() {
            return switch (this) {
                case LOW_QUALITY -> "Low Quality";
                case MEDIUM_QUALITY -> "Medium Quality";
                case HIGH_QUALITY -> "High Quality";
                case TOP_QUALITY -> "Top Quality";
            };
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

    // === Tick Processing ===

    public void tick() {
        if (player == null) return;

        // Update time tracking
        timeInCurrentQiRealm++;
        timeInCurrentBodyRealm++;
        totalQiCultivationTime++;
        totalBodyCultivationTime++;

        // Accumulate cultivation
        if (qiAccumulationRate > 0) {
            addQiCultivation(qiAccumulationRate);
        }
        if (bodyAccumulationRate > 0) {
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
            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }

    public void forceSync() {
        lastSyncedHash = 0;
        syncToClient();
    }

    // === Additional Getters for Network/UI ===

    public long getLastFocusChangeTime() {
        return lastFocusChangeTime;
    }

    public long getQiBreakthroughAttempts() {
        return qiBreakthroughAttempts;
    }

    public long getBodyBreakthroughAttempts() {
        return bodyBreakthroughAttempts;
    }

    public double getQiAdditiveBonus() {
        return qiCultivationAdditiveBonus;
    }

    public double getBodyAdditiveBonus() {
        return bodyCultivationAdditiveBonus;
    }

    public double getQiCultivationFraction() {
        return qiCultivationFraction;
    }

    public double getBodyCultivationFraction() {
        return bodyCultivationFraction;
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

        // Update last synced hash to prevent immediate re-sync
        this.lastSyncedHash = calculateStateHash();
    }

    // === Fixed Serialization (Split into sub-codecs due to 16 parameter limit) ===

    // Sub-codec for realm data
    private static final Codec<RealmData> REALM_DATA_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("current_qi_realm").forGetter(d -> d.currentQiRealmId),
            Codec.STRING.optionalFieldOf("previous_qi_realm", "").forGetter(d -> d.previousQiRealmId),
            Codec.STRING.fieldOf("current_body_realm").forGetter(d -> d.currentBodyRealmId),
            Codec.STRING.optionalFieldOf("previous_body_realm", "").forGetter(d -> d.previousBodyRealmId)
        ).apply(instance, RealmData::new)
    );

    // Sub-codec for cultivation progress
    private static final Codec<CultivationProgress> CULTIVATION_PROGRESS_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.optionalFieldOf("current_qi", 0L).forGetter(d -> d.currentQi),
            Codec.LONG.optionalFieldOf("max_qi", 1000L).forGetter(d -> d.maxQi),
            Codec.DOUBLE.optionalFieldOf("qi_fraction", 0.0).forGetter(d -> d.qiFraction),
            Codec.LONG.optionalFieldOf("current_body", 0L).forGetter(d -> d.currentBody),
            Codec.LONG.optionalFieldOf("max_body", 1000L).forGetter(d -> d.maxBody),
            Codec.DOUBLE.optionalFieldOf("body_fraction", 0.0).forGetter(d -> d.bodyFraction)
        ).apply(instance, CultivationProgress::new)
    );

    // Sub-codec for breakthrough data
    private static final Codec<BreakthroughData> BREAKTHROUGH_DATA_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.optionalFieldOf("qi_attempts", 0L).forGetter(d -> d.qiAttempts),
            Codec.LONG.optionalFieldOf("body_attempts", 0L).forGetter(d -> d.bodyAttempts),
            Codec.LONG.optionalFieldOf("last_qi_attempt", 0L).forGetter(d -> d.lastQiAttempt),
            Codec.LONG.optionalFieldOf("last_body_attempt", 0L).forGetter(d -> d.lastBodyAttempt),
            Codec.BOOL.optionalFieldOf("qi_cooldown", false).forGetter(d -> d.qiCooldown),
            Codec.BOOL.optionalFieldOf("body_cooldown", false).forGetter(d -> d.bodyCooldown)
        ).apply(instance, BreakthroughData::new)
    );

    // Sub-codec for bonus data
    private static final Codec<BonusData> BONUS_DATA_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.optionalFieldOf("qi_additive", 0.0).forGetter(d -> d.qiAdditive),
            Codec.DOUBLE.optionalFieldOf("qi_multiplicative", 0.0).forGetter(d -> d.qiMultiplicative),
            Codec.DOUBLE.optionalFieldOf("body_additive", 0.0).forGetter(d -> d.bodyAdditive),
            Codec.DOUBLE.optionalFieldOf("body_multiplicative", 0.0).forGetter(d -> d.bodyMultiplicative)
        ).apply(instance, BonusData::new)
    );

    // Main codec using sub-codecs
    public static final Codec<PlayerCultivationAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            REALM_DATA_CODEC.fieldOf("realms").forGetter(PlayerCultivationAttachment::getRealmData),
            CULTIVATION_PROGRESS_CODEC.fieldOf("progress").forGetter(PlayerCultivationAttachment::getCultivationProgress),
            Codec.STRING.optionalFieldOf("focus", "QI").forGetter(d -> d.currentFocus.name()),
            Codec.LONG.optionalFieldOf("focus_change_time", 0L).forGetter(d -> d.lastFocusChangeTime),
            BREAKTHROUGH_DATA_CODEC.fieldOf("breakthrough").forGetter(PlayerCultivationAttachment::getBreakthroughData),
            Codec.LONG.optionalFieldOf("time_qi_realm", 0L).forGetter(d -> d.timeInCurrentQiRealm),
            Codec.LONG.optionalFieldOf("time_body_realm", 0L).forGetter(d -> d.timeInCurrentBodyRealm),
            Codec.LONG.optionalFieldOf("total_qi_time", 0L).forGetter(d -> d.totalQiCultivationTime),
            Codec.LONG.optionalFieldOf("total_body_time", 0L).forGetter(d -> d.totalBodyCultivationTime),
            BONUS_DATA_CODEC.fieldOf("bonuses").forGetter(PlayerCultivationAttachment::getBonusData)
        ).apply(instance, PlayerCultivationAttachment::decode)
    );

    // Helper records for codec structure
    private record RealmData(String currentQiRealmId, String previousQiRealmId,
                             String currentBodyRealmId, String previousBodyRealmId) {}

    private record CultivationProgress(long currentQi, long maxQi, double qiFraction,
                                       long currentBody, long maxBody, double bodyFraction) {}

    private record BreakthroughData(long qiAttempts, long bodyAttempts,
                                    long lastQiAttempt, long lastBodyAttempt,
                                    boolean qiCooldown, boolean bodyCooldown) {}

    private record BonusData(double qiAdditive, double qiMultiplicative,
                             double bodyAdditive, double bodyMultiplicative) {}

    // Getter methods for codec
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

    // Decode method
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
}