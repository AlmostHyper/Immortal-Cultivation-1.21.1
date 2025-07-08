package net.hyper.immortalcultivation.common.capability.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.capability.interfaces.*;
import net.hyper.immortalcultivation.common.util.DirtyTrackerUtil;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;

/**
 * CORRECTED: Unified resource data attachment using ONLY permanent additive bonuses.
 * This system never calculates stats from "current realm" - only from accumulated permanent bonuses.
 * Each JSON realm defines the permanent bonus you GET when reaching it, not base stats while in it.
 */
public class PlayerResourceAttachment implements IHealthData, IQiData, IBodyData, ITickable, IDirtyTrackable {

    // === Player Reference ===
    @Nullable
    private Player player;

    // === MORTAL BASE VALUES (starting values for new players) ===
    private static final long MORTAL_BASE_HEALTH = 20L;
    private static final long MORTAL_BASE_QI = 0L;
    private static final long MORTAL_BASE_BODY = 0L;
    private static final double MORTAL_BASE_HEALTH_REGEN = 0.0;
    private static final double MORTAL_BASE_QI_REGEN = 0.0;
    private static final double MORTAL_BASE_BODY_REGEN = 0.0;

    // === PERMANENT ADDITIVE BONUSES (accumulated from realm breakthroughs) ===
    private long permanentHealthBonus = 0L;
    private double permanentHealthRegenBonus = 0.0;
    private long permanentMaxQiBonus = 0L;
    private double permanentQiRegenBonus = 0.0;
    private long permanentMaxBodyBonus = 0L;
    private double permanentBodyRegenBonus = 0.0;

    // === CURRENT RESOURCE VALUES ===
    private long currentHealth = MORTAL_BASE_HEALTH;
    private long currentQi = MORTAL_BASE_QI;
    private long currentBody = MORTAL_BASE_BODY;

    // === CALCULATED MAX VALUES (base + permanent bonuses) ===
    private long maxHealth = MORTAL_BASE_HEALTH;
    private long maxQi = MORTAL_BASE_QI;
    private long maxBody = MORTAL_BASE_BODY;

    // === REGENERATION FRACTIONS ===
    private double healthRegenFraction = 0.0;
    private double qiRegenFraction = 0.0;
    private double bodyRegenFraction = 0.0;

    // === SYNC MANAGEMENT ===
    private final DirtyTrackerUtil dirtyTrackerUtil = new DirtyTrackerUtil() {
        @Override
        public void forceSync() {
            PlayerResourceAttachment.this.forceSync();
        }

        @Override
        public boolean shouldSyncNow() {
            return shouldSync(1500, 1); // Fast sync for resources
        }
    };

    // === CONSTRUCTORS ===
    public PlayerResourceAttachment() {}

    // === PLAYER REFERENCE MANAGEMENT ===

    /**
     * CRITICAL: Set player reference for sync and calculation purposes.
     * Must be called after dimension changes and during initialization.
     */
    public void setPlayer(Player player) {
        this.player = player;
        ImmortalCultivation.LOGGER.debug("Set player reference for PlayerResourceAttachment: {}",
            player != null ? player.getName().getString() : "null");
    }

    @Nullable
    public Player getPlayerSafe() {
        if (player != null && player.isAlive()) {
            return player;
        }
        return null;
    }

    // === PERMANENT HEALTH BONUS SYSTEM ===

    public long addHealth(long amount) {
        if (amount <= 0) return 0;

        long actualIncrease = Math.min(amount, getRemainingHealthCapacity());
        if (actualIncrease > 0) {
            long oldHealth = currentHealth;
            currentHealth += actualIncrease;

            onHealthChanged(oldHealth, currentHealth);
            updateVanillaHealthBar();

            // UNIVERSAL DIRTY MARKING
            markDirty();
        }

        return actualIncrease;
    }

    /**
     * Adds a permanent health bonus from realm breakthrough.
     * This is the ONLY way health increases in your system.
     */
    public void addPermanentHealthBonus(long healthBonus) {
        if (healthBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive health bonus: {}", healthBonus);
            return;
        }

        long oldBonus = this.permanentHealthBonus;
        long oldMaxHealth = this.maxHealth;

        this.permanentHealthBonus += healthBonus;
        recalculateMaxHealth();

        if (this.maxHealth > oldMaxHealth) {
            this.currentHealth = this.maxHealth;
        }

        updateVanillaHealthBar();
        markDirty();

        ImmortalCultivation.LOGGER.info("Player {} gained +{} permanent health from realm breakthrough (total: +{}, max health: {})",
            player != null ? player.getName().getString() : "unknown",
            healthBonus, this.permanentHealthBonus, this.maxHealth);
    }

    public void addPermanentHealthRegenBonus(double healthRegenBonus) {
        if (healthRegenBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive health regen bonus: {}", healthRegenBonus);
            return;
        }

        this.permanentHealthRegenBonus += healthRegenBonus;
        markDirty();

        ImmortalCultivation.LOGGER.info("Player {} gained +{}/sec permanent health regen from realm breakthrough (total: +{}/sec)",
            player != null ? player.getName().getString() : "unknown",
            String.format("%.3f", healthRegenBonus),
            String.format("%.3f", this.permanentHealthRegenBonus));
    }

    /**
     * Recalculates max health using ONLY permanent bonuses.
     * Never uses "current realm" values because your system doesn't work that way.
     */
    public void recalculateMaxHealth() {
        // Max health = mortal base + all accumulated permanent bonuses
        this.maxHealth = MORTAL_BASE_HEALTH + permanentHealthBonus;

        // Ensure current health doesn't exceed new max
        if (this.currentHealth > this.maxHealth) {
            this.currentHealth = this.maxHealth;
        }

        ImmortalCultivation.LOGGER.debug("Recalculated max health: {} (base: {} + bonus: {})",
            this.maxHealth, MORTAL_BASE_HEALTH, permanentHealthBonus);
    }

    // === PERMANENT QI BONUS SYSTEM ===

    public void addPermanentMaxQiBonus(long qiBonus) {
        if (qiBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive max qi bonus: {}", qiBonus);
            return;
        }

        this.permanentMaxQiBonus += qiBonus;
        recalculateMaxQi();
        markDirty();

        ImmortalCultivation.LOGGER.info("Player {} gained +{} permanent max qi from realm breakthrough (total: +{}, max qi: {})",
            player != null ? player.getName().getString() : "unknown",
            qiBonus, this.permanentMaxQiBonus, this.maxQi);
    }

    public void addPermanentQiRegenBonus(double qiRegenBonus) {
        if (qiRegenBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive qi regen bonus: {}", qiRegenBonus);
            return;
        }

        this.permanentQiRegenBonus += qiRegenBonus;
        markDirty();

        ImmortalCultivation.LOGGER.info("Player {} gained +{}/sec permanent qi regen from realm breakthrough (total: +{}/sec)",
            player != null ? player.getName().getString() : "unknown",
            String.format("%.3f", qiRegenBonus),
            String.format("%.3f", this.permanentQiRegenBonus));
    }

    private void recalculateMaxQi() {
        this.maxQi = MORTAL_BASE_QI + permanentMaxQiBonus;

        if (this.currentQi > this.maxQi) {
            this.currentQi = this.maxQi;
        }
    }

    // === PERMANENT BODY BONUS SYSTEM ===

    public void addPermanentMaxBodyBonus(long bodyBonus) {
        if (bodyBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive max body bonus: {}", bodyBonus);
            return;
        }

        this.permanentMaxBodyBonus += bodyBonus;
        recalculateMaxBody();
        markDirty();

        ImmortalCultivation.LOGGER.info("Player {} gained +{} permanent max body from realm breakthrough (total: +{}, max body: {})",
            player != null ? player.getName().getString() : "unknown",
            bodyBonus, this.permanentMaxBodyBonus, this.maxBody);
    }

    public void addPermanentBodyRegenBonus(double bodyRegenBonus) {
        if (bodyRegenBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive body regen bonus: {}", bodyRegenBonus);
            return;
        }

        this.permanentBodyRegenBonus += bodyRegenBonus;
        markDirty();

        ImmortalCultivation.LOGGER.info("Player {} gained +{}/sec permanent body regen from realm breakthrough (total: +{}/sec)",
            player != null ? player.getName().getString() : "unknown",
            String.format("%.3f", bodyRegenBonus),
            String.format("%.3f", this.permanentBodyRegenBonus));
    }

    /**
     * Recalculates max body using ONLY permanent bonuses.
     * Never uses "current realm" values because your system doesn't work that way.
     */
    private void recalculateMaxBody() {
        // Max body = mortal base + all accumulated permanent bonuses
        this.maxBody = MORTAL_BASE_BODY + permanentMaxBodyBonus;

        // Ensure current body doesn't exceed new max
        if (this.currentBody > this.maxBody) {
            this.currentBody = this.maxBody;
        }

        ImmortalCultivation.LOGGER.debug("Recalculated max body: {} (base: {} + bonus: {})",
            this.maxBody, MORTAL_BASE_BODY, permanentMaxBodyBonus);
    }

    // === HEALTH INTERFACE IMPLEMENTATION ===

    @Override
    public long getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public void setCurrentHealth(long value) {
        long oldHealth = this.currentHealth;
        this.currentHealth = Math.max(0, Math.min(value, maxHealth));

        if (oldHealth != this.currentHealth) {
            updateVanillaHealthBar();
            markDirty();
        }
    }

    @Override
    public long getBaseMaxHealth() {
        return MORTAL_BASE_HEALTH + permanentHealthBonus;
    }

    public long consumeHealth(long amount) {
        if (amount <= 0) return 0;

        long actualConsumption = Math.min(amount, currentHealth);
        if (actualConsumption > 0) {
            long oldHealth = currentHealth;
            currentHealth -= actualConsumption;

            onHealthChanged(oldHealth, currentHealth);
            updateVanillaHealthBar();

            markDirty();
        }

        return actualConsumption;
    }

    @Override
    public long getMaxHealth() {
        return maxHealth; // Already calculated with permanent bonuses
    }

    @Override
    public double getBaseHealthRegenerationRate() {
        return MORTAL_BASE_HEALTH_REGEN + permanentHealthRegenBonus;
    }

    @Override
    public double getEffectiveHealthRegenerationRate() {
        double baseRate = getBaseHealthRegenerationRate();

        // Apply focus bonus if focusing on body cultivation
        Player player = getPlayerSafe();
        if (player != null) {
            try {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null && cultivation.getCurrentFocus() == PlayerCultivationAttachment.CultivationType.BODY) {
                    baseRate *= 1.5;
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error applying focus bonus to health regen", e);
            }
        }

        return baseRate;
    }

    @Override
    public double getRegenFraction() {
        return healthRegenFraction;
    }

    @Override
    public void setRegenFraction(double fraction) {
        this.healthRegenFraction = fraction;
    }

    @Override
    public void addRegenFraction(double amount) {
        this.healthRegenFraction += amount;
    }

    // === QI INTERFACE IMPLEMENTATION ===

    @Override
    public long getCurrentQi() {
        return currentQi;
    }

    @Override
    public void setCurrentQi(long value) {
        long oldQi = this.currentQi;
        this.currentQi = Math.max(0, Math.min(value, maxQi));

        if (oldQi != this.currentQi) {
            markDirty();
        }
    }

    @Override
    public long getMaxQi() {
        return maxQi;
    }

    @Override
    public long getBaseMaxQi() {
        return MORTAL_BASE_QI + permanentMaxQiBonus;
    }

    @Override
    public void consumeQi(long amount) {
        setCurrentQi(currentQi - amount);
    }

    @Override
    public double getBaseQiRegenerationRate() {
        return MORTAL_BASE_QI_REGEN + permanentQiRegenBonus;
    }

    @Override
    public double getEffectiveQiRegenerationRate() {
        double baseRate = getBaseQiRegenerationRate();

        Player player = getPlayerSafe();
        if (player != null) {
            try {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null) {
                    // Applies additive and multiplicative bonuses from realm definitions
                    double qiAdditive = cultivation.getRawAdditiveBonus(PlayerCultivationAttachment.CultivationType.QI);
                    double qiMultiplicative = cultivation.getRawMultiplicativeBonus(PlayerCultivationAttachment.CultivationType.QI);
                    double bodyAdditive = cultivation.getRawAdditiveBonus(PlayerCultivationAttachment.CultivationType.BODY) * 0.5;
                    double bodyMultiplicative = cultivation.getRawMultiplicativeBonus(PlayerCultivationAttachment.CultivationType.BODY) * 0.5;

                    // Combine bonuses from both cultivation types
                    double totalAdditiveBonus = qiAdditive + bodyAdditive;
                    double totalMultiplicativeBonus = qiMultiplicative + bodyMultiplicative;

                    // Apply combined bonuses: additive first, then multiplicative
                    baseRate += totalAdditiveBonus;
                    baseRate *= (1.0 + totalMultiplicativeBonus);
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error applying cultivation bonus to qi regen", e);
            }
        }

        return Math.max(0.0, baseRate);
    }


    // === BODY INTERFACE IMPLEMENTATION ===

    @Override
    public long getCurrentBody() {
        return currentBody;
    }

    @Override
    public void setCurrentBody(long value) {
        long oldBody = this.currentBody;
        this.currentBody = Math.max(0, Math.min(value, maxBody));

        if (oldBody != this.currentBody) {
            markDirty();
        }
    }

    @Override
    public long getMaxBody() {
        return maxBody;
    }

    @Override
    public long getBaseMaxBody() {
        return MORTAL_BASE_BODY + permanentMaxBodyBonus;
    }

    @Override
    public void consumeBody(long amount) {
        setCurrentBody(currentBody - amount);
    }

    @Override
    public double getBaseBodyRegenerationRate() {
        return MORTAL_BASE_BODY_REGEN + permanentBodyRegenBonus;
    }

    @Override
    public double getEffectiveBodyRegenerationRate() {
        double baseRate = getBaseBodyRegenerationRate();

        Player player = getPlayerSafe();
        if (player != null) {
            try {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null) {
                    // Applies additive and multiplicative bonuses from realm definitions
                    double qiAdditive = cultivation.getRawAdditiveBonus(PlayerCultivationAttachment.CultivationType.QI) * 0.5;
                    double qiMultiplicative = cultivation.getRawMultiplicativeBonus(PlayerCultivationAttachment.CultivationType.QI) * 0.5;
                    double bodyAdditive = cultivation.getRawAdditiveBonus(PlayerCultivationAttachment.CultivationType.BODY);
                    double bodyMultiplicative = cultivation.getRawMultiplicativeBonus(PlayerCultivationAttachment.CultivationType.BODY);

                    // Combine bonuses from both cultivation types
                    double totalAdditiveBonus = qiAdditive + bodyAdditive;
                    double totalMultiplicativeBonus = qiMultiplicative + bodyMultiplicative;

                    // Apply combined bonuses: additive first, then multiplicative
                    baseRate += totalAdditiveBonus;
                    baseRate *= (1.0 + totalMultiplicativeBonus);
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error applying cultivation bonus to body regen", e);
            }
        }

        return Math.max(0.0, baseRate);
    }

    // === UTILITY METHODS ===

    public long healHealth(long amount) {
        if (amount <= 0) return 0;

        long actualHealing = Math.min(amount, getRemainingHealthCapacity());
        if (actualHealing > 0) {
            long oldHealth = currentHealth;
            currentHealth += actualHealing;

            onHealthChanged(oldHealth, currentHealth);
            updateVanillaHealthBar();

            markDirty();
        }

        return actualHealing;
    }

    public void restoreQi(long amount) {
        if (amount > 0) {
            setCurrentQi(currentQi + amount); // setCurrentQi now calls markDirty()
        }
    }

    public void restoreBody(long amount) {
        if (amount > 0) {
            setCurrentBody(currentBody + amount); // setCurrentBody now calls markDirty()
        }
    }

    public boolean hasEnoughQi(long amount) {
        return currentQi >= amount;
    }

    public boolean hasEnoughBody(long amount) {
        return currentBody >= amount;
    }

    public double getHealthProgress() {
        return maxHealth > 0 ? (double) currentHealth / maxHealth : 0.0;
    }

    public double getQiProgress() {
        return maxQi > 0 ? (double) currentQi / maxQi : 0.0;
    }

    public double getBodyProgress() {
        return maxBody > 0 ? (double) currentBody / maxBody : 0.0;
    }

    // === REGENERATION METHODS ===

    public void regenerateHealth(double rate) {
        if (getCurrentHealth() >= getMaxHealth()) {
            healthRegenFraction = 0.0;
            return;
        }

        healthRegenFraction += rate;

        // Prevent fraction from getting too large
        if (healthRegenFraction > 1000.0) {
            long overflow = (long) healthRegenFraction;
            healthRegenFraction -= overflow;
            healHealth(overflow);
        } else {
            long wholePoints = (long) healthRegenFraction;
            healthRegenFraction -= wholePoints;

            if (wholePoints > 0) {
                healHealth(wholePoints);
            }
        }
    }


    public void regenerateQi(double rate) {
        if (getCurrentQi() >= getMaxQi()) {
            qiRegenFraction = 0.0;
            return;
        }

        qiRegenFraction += rate;

        // Prevent fraction from getting too large
        if (qiRegenFraction > 1000.0) {
            long overflow = (long) qiRegenFraction;
            qiRegenFraction -= overflow;
            setCurrentQi(overflow);
        } else {
            long wholePoints = (long) qiRegenFraction;
            qiRegenFraction -= wholePoints;

            if (wholePoints > 0) {
                setCurrentQi(wholePoints);
            }
        }
    }

    public void regenerateBody(double rate) {
        if (getCurrentQi() >= getMaxQi()) {
            bodyRegenFraction = 0.0;
            return;
        }

        bodyRegenFraction += rate;

        // Prevent fraction from getting too large
        if (bodyRegenFraction > 1000.0) {
            long overflow = (long) bodyRegenFraction;
            bodyRegenFraction -= overflow;
            setCurrentBody(overflow);
        } else {
            long wholePoints = (long) bodyRegenFraction;
            bodyRegenFraction -= wholePoints;

            if (wholePoints > 0) {
                setCurrentBody(wholePoints);
            }
        }
    }

    // === TICK PROCESSING ===

    public void tick() {
        if (player == null) return;

        // Apply regeneration for all three resources
        applyRegeneration();

        // CRITICAL: Update vanilla health bar every tick to ensure it stays synced
        updateVanillaHealthBar();
    }

    private void applyRegeneration() {
        // Health regeneration
        if (canRegenerateHealth()) {
            double healthRegenRate = getEffectiveHealthRegenerationRate();
            if (healthRegenRate > 0) {
                regenerateHealth(healthRegenRate / 20.0); // Convert per-second to per-tick
            }
        }

        // Qi regeneration
        if (canRegenerateQi()) {
            double qiRegenRate = getEffectiveQiRegenerationRate();
            if (qiRegenRate > 0) {
                regenerateQi(qiRegenRate / 20.0); // Convert per-second to per-tick
            }
        }

        // Body regeneration
        if (canRegenerateBody()) {
            double bodyRegenRate = getEffectiveBodyRegenerationRate();
            if (bodyRegenRate > 0) {
                regenerateBody(bodyRegenRate / 20.0); // Convert per-second to per-tick
            }
        }
    }

    // === UTILITY METHODS ===

    private void updateVanillaHealthBar() {
        Player player = getPlayerSafe();
        if (player != null) {
            float healthPercentage = (float) getHealthProgress();
            float vanillaHealth = player.getMaxHealth() * healthPercentage;
            player.setHealth(Math.max(1.0f, vanillaHealth));
        }
    }

    /**
     * Force recalculation of all max values from permanent bonuses.
     * Called during dimension changes to ensure consistency.
     */
    public void forceHealthRecalculation() {
        recalculateMaxHealth();
        recalculateMaxQi();
        recalculateMaxBody();
        updateVanillaHealthBar();
        forceSync();

        ImmortalCultivation.LOGGER.debug("Force recalculated all resource values for player: {}",
            player != null ? player.getName().getString() : "unknown");
    }

    // === SYNC MANAGEMENT ===

    @Override
    public boolean shouldSyncNow() {
        return dirtyTrackerUtil.shouldSync(1500, 1);
    }

    @Override
    public void forceSync() {
        if (getPlayerSafe() instanceof ServerPlayer serverPlayer) {
            try {
                NetworkSyncManager.scheduleSync(serverPlayer, NetworkSyncManager.SyncPriority.CRITICAL);

                ImmortalCultivation.LOGGER.debug("Force synced resource data for player: {}",
                    serverPlayer.getName().getString());
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Failed to force sync resource data for player: {}",
                    serverPlayer.getName().getString(), e);
            }
        }
    }

    @Override
    public void markDirty() {
        dirtyTrackerUtil.markDirty();
    }

    @Override
    public boolean isDirty() {
        return dirtyTrackerUtil.isDirty();
    }

    @Override
    public void clearDirty() {
        dirtyTrackerUtil.clearDirty();
    }

    @Override
    public long getChangeCounter() {
        return dirtyTrackerUtil.getChangeCounter();
    }

    @Override
    public void incrementChangeCounter() {
        dirtyTrackerUtil.incrementChangeCounter();
    }

    @Override
    public void resetChangeCounter() {
        dirtyTrackerUtil.resetChangeCounter();
    }

    // === GETTERS FOR PERMANENT BONUSES ===

    public long getPermanentHealthBonus() { return permanentHealthBonus; }
    public double getPermanentHealthRegenBonus() { return permanentHealthRegenBonus; }
    public long getPermanentMaxQiBonus() { return permanentMaxQiBonus; }
    public double getPermanentQiRegenBonus() { return permanentQiRegenBonus; }
    public long getPermanentMaxBodyBonus() { return permanentMaxBodyBonus; }
    public double getPermanentBodyRegenBonus() { return permanentBodyRegenBonus; }

    // === SETTERS FOR SYNC (called by sync packets) ===

    // === SETTERS FOR SYNC (called by sync packets) ===

    public void setPermanentHealthBonus(long bonus) {
        if (this.permanentHealthBonus != bonus) {
            this.permanentHealthBonus = Math.max(0, bonus);
            recalculateMaxHealth();
            markDirty();
        }
    }

    public void setPermanentHealthRegenBonus(double bonus) {
        if (this.permanentHealthRegenBonus != bonus) {
            this.permanentHealthRegenBonus = Math.max(0.0, bonus);
            markDirty();
        }
    }

    public void setPermanentMaxQiBonus(long bonus) {
        if (this.permanentMaxQiBonus != bonus) {
            this.permanentMaxQiBonus = Math.max(0, bonus);
            recalculateMaxQi();
            markDirty();
        }
    }

    public void setPermanentQiRegenBonus(double bonus) {
        if (this.permanentQiRegenBonus != bonus) {
            this.permanentQiRegenBonus = Math.max(0.0, bonus);
            markDirty();
        }
    }

    public void setPermanentMaxBodyBonus(long bonus) {
        if (this.permanentMaxBodyBonus != bonus) {
            this.permanentMaxBodyBonus = Math.max(0, bonus);
            recalculateMaxBody();
            markDirty();
        }
    }

    public void setPermanentBodyRegenBonus(double bonus) {
        if (this.permanentBodyRegenBonus != bonus) {
            this.permanentBodyRegenBonus = Math.max(0.0, bonus);
            markDirty();
        }
    }

    // === SERIALIZATION ===

    public static final Codec<PlayerResourceAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            // Permanent bonuses
            Codec.LONG.fieldOf("permanent_health_bonus").forGetter(d -> d.permanentHealthBonus),
            Codec.DOUBLE.fieldOf("permanent_health_regen_bonus").forGetter(d -> d.permanentHealthRegenBonus),
            Codec.LONG.fieldOf("permanent_max_qi_bonus").forGetter(d -> d.permanentMaxQiBonus),
            Codec.DOUBLE.fieldOf("permanent_qi_regen_bonus").forGetter(d -> d.permanentQiRegenBonus),
            Codec.LONG.fieldOf("permanent_max_body_bonus").forGetter(d -> d.permanentMaxBodyBonus),
            Codec.DOUBLE.fieldOf("permanent_body_regen_bonus").forGetter(d -> d.permanentBodyRegenBonus),
            // Current values
            Codec.LONG.fieldOf("current_health").forGetter(d -> d.currentHealth),
            Codec.LONG.fieldOf("current_qi").forGetter(d -> d.currentQi),
            Codec.LONG.fieldOf("current_body").forGetter(d -> d.currentBody),
            // Regeneration fractions
            Codec.DOUBLE.fieldOf("health_regen_fraction").forGetter(d -> d.healthRegenFraction),
            Codec.DOUBLE.fieldOf("qi_regen_fraction").forGetter(d -> d.qiRegenFraction),
            Codec.DOUBLE.fieldOf("body_regen_fraction").forGetter(d -> d.bodyRegenFraction)
        ).apply(instance, PlayerResourceAttachment::decode)
    );

    private static PlayerResourceAttachment decode(
        long permanentHealthBonus, double permanentHealthRegenBonus,
        long permanentMaxQiBonus, double permanentQiRegenBonus,
        long permanentMaxBodyBonus, double permanentBodyRegenBonus,
        long currentHealth, long currentQi, long currentBody,
        double healthFraction, double qiFraction, double bodyFraction) {

        PlayerResourceAttachment data = new PlayerResourceAttachment();

        // Set permanent bonuses
        data.permanentHealthBonus = permanentHealthBonus;
        data.permanentHealthRegenBonus = permanentHealthRegenBonus;
        data.permanentMaxQiBonus = permanentMaxQiBonus;
        data.permanentQiRegenBonus = permanentQiRegenBonus;
        data.permanentMaxBodyBonus = permanentMaxBodyBonus;
        data.permanentBodyRegenBonus = permanentBodyRegenBonus;

        // Calculate max values from permanent bonuses
        data.maxHealth = MORTAL_BASE_HEALTH + permanentHealthBonus;
        data.maxQi = MORTAL_BASE_QI + permanentMaxQiBonus;
        data.maxBody = MORTAL_BASE_BODY + permanentMaxBodyBonus;

        // Set current values (ensuring they don't exceed max)
        data.currentHealth = Math.min(currentHealth, data.maxHealth);
        data.currentQi = Math.min(currentQi, data.maxQi);
        data.currentBody = Math.min(currentBody, data.maxBody);

        // Set regeneration fractions
        data.healthRegenFraction = healthFraction;
        data.qiRegenFraction = qiFraction;
        data.bodyRegenFraction = bodyFraction;

        return data;
    }

    // === SERIALIZER ===

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerResourceAttachment> {
        @Override
        public PlayerResourceAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerResourceAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerResourceAttachment: {}", error))
                .map(pair -> pair.getFirst())
                .orElse(new PlayerResourceAttachment());

            // CRITICAL: Set player reference during deserialization
            if (holder instanceof Player player) {
                attachment.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Set player reference during resource attachment deserialization: {}",
                    player.getName().getString());
            }

            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerResourceAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encode(attachment, provider.createSerializationContext(NbtOps.INSTANCE), new CompoundTag())
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerResourceAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }
}