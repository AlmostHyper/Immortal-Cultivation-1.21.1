package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.*;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerStatsSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Unified player stats attachment that combines resource management (Health, Qi, Body)
 * and combat statistics (Attack, Movement, Jump) in a single cohesive system.
 * Extends PlayerAttachment to inherit debugging, synchronization, and performance monitoring capabilities.
 * Implements IStatsData for unified stat management following the ICultivationData pattern.
 */
public class PlayerStatsAttachment extends PlayerAttachment<PlayerStatsAttachment> implements IStatsData, ITickable {

    // === CONSTANTS === //

    private static final long MORTAL_BASE_HEALTH = 20L;
    private static final long MORTAL_BASE_QI = 0L;
    private static final long MORTAL_BASE_BODY = 0L;

    // === RESOURCE FIELDS === //

    // --- Health Data ---
    private long permanentHealthBonus = 0L;
    private double permanentHealthRegenBonus = 0.0;
    private long currentHealth = MORTAL_BASE_HEALTH;
    private long maxHealth = MORTAL_BASE_HEALTH;
    private double healthRegenFraction = 0.0;

    // --- Qi Data ---
    private long permanentQiBonus = 0L;
    private double permanentQiRegenBonus = 0.0;
    private long currentQi = MORTAL_BASE_QI;
    private long maxQi = MORTAL_BASE_QI;
    private double qiRegenFraction = 0.0;

    // --- Body Data ---
    private long permanentBodyBonus = 0L;
    private double permanentBodyRegenBonus = 0.0;
    private long currentBody = MORTAL_BASE_BODY;
    private long maxBody = MORTAL_BASE_BODY;
    private double bodyRegenFraction = 0.0;

    // === COMBAT STATS FIELDS === //

    // --- Permanent Combat Bonuses ---
    private double permanentAttackDamageBonus = 0.0;
    private double permanentMovementSpeedBonus = 0.0;
    private double permanentJumpHeightBonus = 0.0;

    // --- Temporary Combat Bonuses ---
    private double attackDamageBonus = 0.0;
    private double movementSpeedBonus = 0.0;
    private double jumpHeightBonus = 0.0;

    // === CONSTRUCTORS === //

    /**
     * Default constructor for creating a new PlayerStatsAttachment with mortal base values.
     */
    public PlayerStatsAttachment() {
        super();
    }

    /**
     * Constructor with player reference for immediate association.
     * @param player The player to associate with this attachment
     */
    public PlayerStatsAttachment(@Nullable Player player) {
        super();
        setPlayer(player);
    }

    // === ISTATSDATA IMPLEMENTATION === //

    /**
     * Gets the current value for the specified stat type.
     * @param type The stat type to query
     * @return Current stat value
     */
    @Override
    public long getCurrentStat(StatType type) {
        return switch (type) {
            case HEALTH -> currentHealth;
            case QI -> currentQi;
            case BODY -> currentBody;
        };
    }

    /**
     * Sets the current value for the specified stat type, clamping to valid range and triggering sync.
     * @param type The stat type to modify
     * @param value The new stat value
     */
    @Override
    public void setCurrentStat(StatType type, long value) {
        long oldValue = getCurrentStat(type);
        long maxValue = getMaxStat(type);
        long newValue = Math.max(0, Math.min(value, maxValue));

        switch (type) {
            case HEALTH -> this.currentHealth = newValue;
            case QI -> this.currentQi = newValue;
            case BODY -> this.currentBody = newValue;
        }

        if (oldValue != newValue) {
            onStatChanged(type, oldValue, newValue);
            markDirty();
        }
    }

    /**
     * Gets the maximum value for the specified stat type.
     * @param type The stat type to query
     * @return Maximum stat value
     */
    @Override
    public long getMaxStat(StatType type) {
        return switch (type) {
            case HEALTH -> maxHealth;
            case QI -> maxQi;
            case BODY -> maxBody;
        };
    }

    /**
     * Sets the maximum value for the specified stat type and clamps current if needed.
     * Fixed to only clamp downward, never restore health to max.
     * @param type The stat type to modify
     * @param value The new maximum value
     */
    @Override
    public void setMaxStat(StatType type, long value) {
        long oldMax = getMaxStat(type);
        long newMax = Math.max(0, value);

        switch (type) {
            case HEALTH -> this.maxHealth = newMax;
            case QI -> this.maxQi = newMax;
            case BODY -> this.maxBody = newMax;
        }

        if (oldMax != newMax) {
            long currentValue = getCurrentStat(type);

            // Only clamp current stat DOWN if it exceeds the new maximum
            // Never increase current stat - this prevents health restoration bugs
            if (currentValue > newMax) {
                setCurrentStat(type, newMax);

                if (debugEnabled()) {
                    debugLog("Clamped {} from {} to {} due to max change ({} -> {})",
                        type.getDisplayName(), currentValue, newMax, oldMax, newMax);
                }
            } else if (debugEnabled()) {
                debugLog("Max {} changed from {} to {}, current {} unchanged",
                    type.getDisplayName(), oldMax, newMax, currentValue);
            }

            markDirty();
        }
    }

    /**
     * Gets the base regeneration rate per second for the specified stat type.
     * @param type The stat type to query
     * @return Base regeneration rate per second
     */
    @Override
    public double getStatRegenRate(StatType type) {
        return switch (type) {
            case HEALTH -> 0.0 + permanentHealthRegenBonus;
            case QI -> 0.0 + permanentQiRegenBonus;
            case BODY -> 0.0 + permanentBodyRegenBonus;
        };
    }

    /**
     * Gets the current regeneration fraction for the specified stat type.
     * @param type The stat type to query
     * @return Current regeneration fraction
     */
    @Override
    public double getRegenFraction(StatType type) {
        return switch (type) {
            case HEALTH -> healthRegenFraction;
            case QI -> qiRegenFraction;
            case BODY -> bodyRegenFraction;
        };
    }

    /**
     * Sets the regeneration fraction for the specified stat type.
     * @param type The stat type to modify
     * @param fraction The new fractional regeneration amount
     */
    @Override
    public void setRegenFraction(StatType type, double fraction) {
        switch (type) {
            case HEALTH -> this.healthRegenFraction = fraction;
            case QI -> this.qiRegenFraction = fraction;
            case BODY -> this.bodyRegenFraction = fraction;
        }
    }

    /**
     * Gets the permanent bonus that increases the maximum for the specified stat type.
     * @param type The stat type to query
     * @return Permanent bonus amount
     */
    @Override
    public long getPermanentStatBonus(StatType type) {
        return switch (type) {
            case HEALTH -> permanentHealthBonus;
            case QI -> permanentQiBonus;
            case BODY -> permanentBodyBonus;
        };
    }

    /**
     * Sets the permanent bonus for the specified stat type and recalculates maximums.
     * @param type The stat type to modify
     * @param bonus The new permanent bonus amount
     */
    @Override
    public void setPermanentStatBonus(StatType type, long bonus) {
        long oldBonus = getPermanentStatBonus(type);

        switch (type) {
            case HEALTH -> this.permanentHealthBonus = bonus;
            case QI -> this.permanentQiBonus = bonus;
            case BODY -> this.permanentBodyBonus = bonus;
        }

        if (oldBonus != bonus) {
            recalculateStatMaximum(type);
            markDirty();
        }
    }

    /**
     * Gets the permanent regeneration bonus multiplier for the specified stat type.
     * @param type The stat type to query
     * @return Permanent regeneration bonus multiplier
     */
    @Override
    public double getPermanentStatRegenBonus(StatType type) {
        return switch (type) {
            case HEALTH -> permanentHealthRegenBonus;
            case QI -> permanentQiRegenBonus;
            case BODY -> permanentBodyRegenBonus;
        };
    }

    /**
     * Sets the permanent regeneration bonus for the specified stat type.
     * @param type The stat type to modify
     * @param bonus The new regeneration bonus multiplier
     */
    @Override
    public void setPermanentStatRegenBonus(StatType type, double bonus) {
        double oldBonus = getPermanentStatRegenBonus(type);

        switch (type) {
            case HEALTH -> this.permanentHealthRegenBonus = bonus;
            case QI -> this.permanentQiRegenBonus = bonus;
            case BODY -> this.permanentBodyRegenBonus = bonus;
        }

        if (oldBonus != bonus) {
            markDirty();
        }
    }

    /**
     * Source-based permanent stat bonuses for Health, organized by bonus source.
     * Allows selective clearing and management of bonuses from different sources.
     */
    private final Map<BonusSource, Long> healthBonusBySource = new EnumMap<>(BonusSource.class);

    /**
     * Source-based permanent stat bonuses for Qi, organized by bonus source.
     */
    private final Map<BonusSource, Long> qiBonusBySource = new EnumMap<>(BonusSource.class);

    /**
     * Source-based permanent stat bonuses for Body, organized by bonus source.
     */
    private final Map<BonusSource, Long> bodyBonusBySource = new EnumMap<>(BonusSource.class);

    /**
     * Source-based permanent regeneration bonuses for Health, organized by bonus source.
     */
    private final Map<BonusSource, Double> healthRegenBonusBySource = new EnumMap<>(BonusSource.class);

    /**
     * Source-based permanent regeneration bonuses for Qi, organized by bonus source.
     */
    private final Map<BonusSource, Double> qiRegenBonusBySource = new EnumMap<>(BonusSource.class);

    /**
     * Source-based permanent regeneration bonuses for Body, organized by bonus source.
     */
    private final Map<BonusSource, Double> bodyRegenBonusBySource = new EnumMap<>(BonusSource.class);

    /**
     * Source-based permanent attack damage bonuses, organized by bonus source.
     */
    private final Map<BonusSource, Double> attackDamageBonusBySource = new EnumMap<>(BonusSource.class);

    /**
     * Source-based permanent movement speed bonuses, organized by bonus source.
     */
    private final Map<BonusSource, Double> movementSpeedBonusBySource = new EnumMap<>(BonusSource.class);

    /**
     * Source-based permanent jump height bonuses, organized by bonus source.
     */
    private final Map<BonusSource, Double> jumpHeightBonusBySource = new EnumMap<>(BonusSource.class);

// === SOURCE-BASED STAT BONUS MANAGEMENT === //

    /**
     * Sets a permanent stat bonus from a specific source.
     * This allows precise control over which bonuses to clear or modify.
     *
     * @param type The stat type to modify
     * @param source The source providing this bonus
     * @param bonus The bonus amount
     */
    public void setSourcedStatBonus(IStatsData.StatType type, BonusSource source, long bonus) {
        Map<BonusSource, Long> sourceMap = getStatBonusSourceMap(type);
        Long oldBonus = sourceMap.get(source);

        if (bonus == 0) {
            sourceMap.remove(source);
        } else {
            sourceMap.put(source, bonus);
        }

        // Recalculate total bonus if changed
        if (!java.util.Objects.equals(oldBonus, bonus == 0 ? null : bonus)) {
            recalculateStatBonusFromSources(type);
            markDirty();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Source stat bonus changed for {} from {}: {} -> {} (total: {})",
                    type, source.getDisplayName(), oldBonus, bonus, getPermanentStatBonus(type));
            }
        }
    }

    /**
     * Gets a permanent stat bonus from a specific source.
     *
     * @param type The stat type to query
     * @param source The source to check
     * @return The bonus amount from this source (0 if no bonus)
     */
    public long getSourcedStatBonus(IStatsData.StatType type, BonusSource source) {
        Map<BonusSource, Long> sourceMap = getStatBonusSourceMap(type);
        return sourceMap.getOrDefault(source, 0L);
    }

    /**
     * Clears all bonuses from a specific source for all stat types.
     * This is useful for realm reapplication or equipment changes.
     *
     * @param source The source to clear bonuses from
     */
    public void clearBonusesFromSource(BonusSource source) {
        boolean anyChanged = false;

        // Clear stat bonuses
        anyChanged |= healthBonusBySource.remove(source) != null;
        anyChanged |= qiBonusBySource.remove(source) != null;
        anyChanged |= bodyBonusBySource.remove(source) != null;

        // Clear regen bonuses
        anyChanged |= healthRegenBonusBySource.remove(source) != null;
        anyChanged |= qiRegenBonusBySource.remove(source) != null;
        anyChanged |= bodyRegenBonusBySource.remove(source) != null;

        // Clear combat bonuses
        anyChanged |= attackDamageBonusBySource.remove(source) != null;
        anyChanged |= movementSpeedBonusBySource.remove(source) != null;
        anyChanged |= jumpHeightBonusBySource.remove(source) != null;

        if (anyChanged) {
            // Recalculate all totals
            recalculateAllBonusesFromSources();
            markDirty();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Cleared all bonuses from source: {}", source.getDisplayName());
            }
        }
    }

    /**
     * Sets a permanent regeneration bonus from a specific source.
     *
     * @param type The stat type to modify
     * @param source The source providing this bonus
     * @param bonus The regeneration bonus multiplier
     */
    public void setSourcedStatRegenBonus(IStatsData.StatType type, BonusSource source, double bonus) {
        Map<BonusSource, Double> sourceMap = getStatRegenBonusSourceMap(type);
        Double oldBonus = sourceMap.get(source);

        if (bonus == 0.0) {
            sourceMap.remove(source);
        } else {
            sourceMap.put(source, bonus);
        }

        // Recalculate total bonus if changed
        if (!java.util.Objects.equals(oldBonus, bonus == 0.0 ? null : bonus)) {
            recalculateStatRegenBonusFromSources(type);
            markDirty();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Source regen bonus changed for {} from {}: {} -> {} (total: {})",
                    type, source.getDisplayName(), oldBonus, bonus, getPermanentStatRegenBonus(type));
            }
        }
    }

    /**
     * Gets a permanent regeneration bonus from a specific source.
     *
     * @param type The stat type to query
     * @param source The source to check
     * @return The regeneration bonus from this source (0.0 if no bonus)
     */
    public double getSourcedStatRegenBonus(IStatsData.StatType type, BonusSource source) {
        Map<BonusSource, Double> sourceMap = getStatRegenBonusSourceMap(type);
        return sourceMap.getOrDefault(source, 0.0);
    }

    /**
     * Sets a permanent combat bonus from a specific source.
     *
     * @param bonusType The type of combat bonus (attack, speed, jump)
     * @param source The source providing this bonus
     * @param bonus The bonus value
     */
    public void setSourcedCombatBonus(CombatBonusType bonusType, BonusSource source, double bonus) {
        Map<BonusSource, Double> sourceMap = getCombatBonusSourceMap(bonusType);
        Double oldBonus = sourceMap.get(source);

        if (bonus == 0.0) {
            sourceMap.remove(source);
        } else {
            sourceMap.put(source, bonus);
        }

        // Recalculate total bonus if changed
        if (!java.util.Objects.equals(oldBonus, bonus == 0.0 ? null : bonus)) {
            recalculateCombatBonusFromSources(bonusType);
            markDirty();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Source combat bonus changed for {} from {}: {} -> {} (total: {})",
                    bonusType, source.getDisplayName(), oldBonus, bonus, getCurrentCombatBonus(bonusType));
            }
        }
    }

    /**
     * Gets a permanent combat bonus from a specific source.
     *
     * @param bonusType The type of combat bonus to query
     * @param source The source to check
     * @return The combat bonus from this source (0.0 if no bonus)
     */
    public double getSourcedCombatBonus(CombatBonusType bonusType, BonusSource source) {
        Map<BonusSource, Double> sourceMap = getCombatBonusSourceMap(bonusType);
        return sourceMap.getOrDefault(source, 0.0);
    }

    // === COMBAT STATS MANAGEMENT === //

    /**
     * Gets the permanent attack damage bonus.
     * @return Permanent attack damage bonus as a multiplier
     */
    public double getPermanentAttackDamageBonus() {
        return permanentAttackDamageBonus;
    }

    /**
     * Sets the permanent attack damage bonus and triggers sync if changed.
     * @param value The new permanent attack damage bonus
     */
    public void setPermanentAttackDamageBonus(double value) {
        if (this.permanentAttackDamageBonus != value) {
            this.permanentAttackDamageBonus = value;
            markDirty();
        }
    }

    /**
     * Gets the permanent movement speed bonus.
     * @return Permanent movement speed bonus as a modifier
     */
    public double getPermanentMovementSpeedBonus() {
        return permanentMovementSpeedBonus;
    }

    /**
     * Sets the permanent movement speed bonus and triggers sync if changed.
     * @param value The new permanent movement speed bonus
     */
    public void setPermanentMovementSpeedBonus(double value) {
        if (this.permanentMovementSpeedBonus != value) {
            this.permanentMovementSpeedBonus = value;
            markDirty();
        }
    }

    /**
     * Gets the permanent jump height bonus.
     * @return Permanent jump height bonus as a modifier
     */
    public double getPermanentJumpHeightBonus() {
        return permanentJumpHeightBonus;
    }

    /**
     * Sets the permanent jump height bonus and triggers sync if changed.
     * @param value The new permanent jump height bonus
     */
    public void setPermanentJumpHeightBonus(double value) {
        if (this.permanentJumpHeightBonus != value) {
            this.permanentJumpHeightBonus = value;
            markDirty();
        }
    }

    /**
     * Gets the temporary attack damage bonus.
     * @return Temporary attack damage bonus as a multiplier
     */
    public double getAttackDamageBonus() {
        return attackDamageBonus;
    }

    /**
     * Sets the temporary attack damage bonus and triggers sync if changed.
     * @param value The new temporary attack damage bonus
     */
    public void setAttackDamageBonus(double value) {
        if (this.attackDamageBonus != value) {
            this.attackDamageBonus = value;
            markDirty();
        }
    }

    /**
     * Gets the temporary movement speed bonus.
     * @return Temporary movement speed bonus as a modifier
     */
    public double getMovementSpeedBonus() {
        return movementSpeedBonus;
    }

    /**
     * Sets the temporary movement speed bonus and triggers sync if changed.
     * @param value The new temporary movement speed bonus
     */
    public void setMovementSpeedBonus(double value) {
        if (this.movementSpeedBonus != value) {
            this.movementSpeedBonus = value;
            markDirty();
        }
    }

    /**
     * Gets the temporary jump height bonus.
     * @return Temporary jump height bonus as a modifier
     */
    public double getJumpHeightBonus() {
        return jumpHeightBonus;
    }

    /**
     * Sets the temporary jump height bonus and triggers sync if changed.
     * @param value The new temporary jump height bonus
     */
    public void setJumpHeightBonus(double value) {
        if (this.jumpHeightBonus != value) {
            this.jumpHeightBonus = value;
            markDirty();
        }
    }

    // === COMBAT STATS CALCULATIONS === //

    /**
     * Calculates the effective attack damage multiplier combining permanent and temporary bonuses.
     * @return Effective attack damage multiplier (1.0 + bonuses)
     */
    public double getEffectiveAttackDamageMultiplier() {
        return 1.0 + permanentAttackDamageBonus + attackDamageBonus;
    }

    /**
     * Calculates the effective movement speed multiplier combining permanent and temporary bonuses.
     * @return Effective movement speed multiplier
     */
    public double getEffectiveMovementSpeedMultiplier() {
        return permanentMovementSpeedBonus + movementSpeedBonus;
    }

    /**
     * Calculates the effective jump height multiplier combining permanent and temporary bonuses.
     * @return Effective jump height multiplier
     */
    public double getEffectiveJumpHeightMultiplier() {
        return permanentJumpHeightBonus + jumpHeightBonus;
    }

    // === UTILITY METHODS === //

    /**
     * Fixed recalculateStatMaximum to prevent health restoration during combat.
     * @param type The stat type to recalculate
     */
    private void recalculateStatMaximum(StatType type) {
        long oldMax = getMaxStat(type);

        long baseValue = switch (type) {
            case HEALTH -> MORTAL_BASE_HEALTH;
            case QI -> MORTAL_BASE_QI;
            case BODY -> MORTAL_BASE_BODY;
        };

        long newMax = baseValue + getPermanentStatBonus(type);

        if (debugEnabled()) {
            debugLog("Recalculating {} max: {} -> {} (base: {}, bonus: {})",
                type.getDisplayName(), oldMax, newMax, baseValue, getPermanentStatBonus(type));
        }

        setMaxStat(type, newMax);
    }

    /**
     * Recalculates all maximum values based on current bonuses.
     * Called after deserialization or major state changes.
     */
    public void recalculateAll() {
        for (StatType type : StatType.values()) {
            recalculateStatMaximum(type);
        }
    }

    /**
     * Override to add debugging for stat changes - helps track unexpected healing.
     */
    @Override
    public void onStatChanged(StatType type, long oldValue, long newValue) {
        if (type == StatType.HEALTH && debugEnabled()) {
            debugLog("Health changed: {} -> {} (diff: {})", oldValue, newValue, newValue - oldValue);

            // Log stack trace for unexpected large heals to help debug
            if (newValue - oldValue > 5) {
                debugWarn("Large health change detected! Stack trace:");
                Thread.dumpStack();
            }
        }
    }

    /**
     * Override to add debugging for stat additions - helps track regeneration.
     */
    @Override
    public void onStatAdded(StatType type, long amount) {
        if (type == StatType.HEALTH && debugEnabled()) {
            debugLog("Health added: +{} points", amount);
        }
    }

    /**
     * Override to add debugging for stat consumption - helps track damage.
     */
    @Override
    public void onStatConsumed(StatType type, long amount) {
        if (type == StatType.HEALTH && debugEnabled()) {
            debugLog("Health consumed: -{} points", amount);
        }
    }

    // === TICKABLE IMPLEMENTATION === //

    /**
     * Handles per-tick processing for resource regeneration using the unified stat system.
     * Called every server tick to update Health, Qi, and Body regeneration.
     */
    @Override
    public void tick() {
        if (player == null || player.level().isClientSide) {
            return;
        }

        startPerformanceTracking("tick");
        try {
            boolean anyRegenOccurred = false;

            // Process regeneration for all stat types
            for (StatType type : StatType.values()) {
                if (canRegenerateStat(type)) {
                    double regenRate = getEffectiveStatRegenerationRate(type);
                    double currentFraction = getRegenFraction(type);

                    // Add regeneration per tick (divide by 20 for per-second rate)
                    currentFraction += regenRate / 20.0;

                    // Cap the maximum regeneration per tick to prevent instant healing exploits
                    double maxRegenPerTick = getMaxStat(type) * 0.05; // Max 5% of total health per tick

                    if (currentFraction >= 1.0) {
                        long regenAmount = Math.min((long) currentFraction, (long) maxRegenPerTick);
                        currentFraction -= regenAmount;

                        // Additional safety check: prevent healing more than 1 point per tick for health
                        if (type == StatType.HEALTH) {
                            regenAmount = Math.min(regenAmount, 1L);
                        }

                        long actualAdded = addStat(type, regenAmount);
                        if (actualAdded > 0) {
                            anyRegenOccurred = true;
                            onStatAdded(type, actualAdded);

                            // Debug log for health regeneration to track the issue
                            if (type == StatType.HEALTH && debugEnabled()) {
                                debugLog("Health regeneration: +{} (rate: {}, fraction: {})",
                                    actualAdded, regenRate, currentFraction);
                            }
                        }
                    }

                    setRegenFraction(type, currentFraction);
                }
            }

            // Debug logging for regeneration
            if (anyRegenOccurred && debugEnabled()) {
                debugLog("Regeneration occurred - H:{}/{} Q:{}/{} B:{}/{}",
                    getCurrentStat(StatType.HEALTH), getMaxStat(StatType.HEALTH),
                    getCurrentStat(StatType.QI), getMaxStat(StatType.QI),
                    getCurrentStat(StatType.BODY), getMaxStat(StatType.BODY));
            }
        } finally {
            endPerformanceTracking("tick", 2); // 2ms warning threshold
        }
    }

    // === PLAYERATTACHMENT OVERRIDES === //

    /**
     * Called when a player is set on this attachment.
     * Initializes any player-specific behavior.
     * @param player The player that was set
     */
    @Override
    protected void onPlayerSet(Player player) {
        if (debugEnabled()) {
            debugLog("PlayerStatsAttachment associated with player: {}",
                player != null ? player.getName().getString() : "NULL");
        }

        // Ensure all calculations are up to date when player is set
        recalculateAll();
    }

    /**
     * Gets the codec for serialization of this specific attachment type.
     * @return The codec for PlayerStatsAttachment
     */
    @Override
    protected Codec<PlayerStatsAttachment> getCodec() {
        return CODEC;
    }

    /**
     * Creates a sync packet with the current attachment data.
     * @param data The NBT data to include in the packet
     * @return The appropriate sync packet
     */
    @Override
    protected ISyncPacket createSyncPacket(CompoundTag data) {

        return new PlayerStatsSyncPacket(data);
    }

    // === HELPER METHODS === //

    /**
     * Gets the appropriate source map for stat bonuses based on stat type.
     */
    private Map<BonusSource, Long> getStatBonusSourceMap(IStatsData.StatType type) {
        return switch (type) {
            case HEALTH -> healthBonusBySource;
            case QI -> qiBonusBySource;
            case BODY -> bodyBonusBySource;
        };
    }

    /**
     * Gets the appropriate source map for regeneration bonuses based on stat type.
     */
    private Map<BonusSource, Double> getStatRegenBonusSourceMap(IStatsData.StatType type) {
        return switch (type) {
            case HEALTH -> healthRegenBonusBySource;
            case QI -> qiRegenBonusBySource;
            case BODY -> bodyRegenBonusBySource;
        };
    }

    /**
     * Gets the appropriate source map for combat bonuses based on bonus type.
     */
    private Map<BonusSource, Double> getCombatBonusSourceMap(CombatBonusType bonusType) {
        return switch (bonusType) {
            case ATTACK_DAMAGE -> attackDamageBonusBySource;
            case MOVEMENT_SPEED -> movementSpeedBonusBySource;
            case JUMP_HEIGHT -> jumpHeightBonusBySource;
        };
    }

    /**
     * Recalculates the total permanent stat bonus from all sources.
     */
    private void recalculateStatBonusFromSources(IStatsData.StatType type) {
        Map<BonusSource, Long> sourceMap = getStatBonusSourceMap(type);
        long total = sourceMap.values().stream().mapToLong(Long::longValue).sum();

        // Update the legacy field to maintain compatibility
        switch (type) {
            case HEALTH -> this.permanentHealthBonus = total;
            case QI -> this.permanentQiBonus = total;
            case BODY -> this.permanentBodyBonus = total;
        }

        recalculateStatMaximum(type);
    }

    /**
     * Recalculates the total permanent regeneration bonus from all sources.
     */
    private void recalculateStatRegenBonusFromSources(IStatsData.StatType type) {
        Map<BonusSource, Double> sourceMap = getStatRegenBonusSourceMap(type);
        double total = sourceMap.values().stream().mapToDouble(Double::doubleValue).sum();

        // Update the legacy field to maintain compatibility
        switch (type) {
            case HEALTH -> this.permanentHealthRegenBonus = total;
            case QI -> this.permanentQiRegenBonus = total;
            case BODY -> this.permanentBodyRegenBonus = total;
        }
    }

    /**
     * Recalculates the total permanent combat bonus from all sources.
     */
    private void recalculateCombatBonusFromSources(CombatBonusType bonusType) {
        Map<BonusSource, Double> sourceMap = getCombatBonusSourceMap(bonusType);
        double total = sourceMap.values().stream().mapToDouble(Double::doubleValue).sum();

        // Update the legacy field to maintain compatibility
        switch (bonusType) {
            case ATTACK_DAMAGE -> this.permanentAttackDamageBonus = total;
            case MOVEMENT_SPEED -> this.permanentMovementSpeedBonus = total;
            case JUMP_HEIGHT -> this.permanentJumpHeightBonus = total;
        }
    }

    /**
     * Recalculates all bonuses from all sources.
     * Call this after clearing a source to update all totals.
     */
    private void recalculateAllBonusesFromSources() {
        // Recalculate stat bonuses
        for (IStatsData.StatType type : IStatsData.StatType.values()) {
            recalculateStatBonusFromSources(type);
            recalculateStatRegenBonusFromSources(type);
        }

        // Recalculate combat bonuses
        for (CombatBonusType type : CombatBonusType.values()) {
            recalculateCombatBonusFromSources(type);
        }
    }

    /**
     * Gets the current total for a combat bonus type (for compatibility).
     */
    private double getCurrentCombatBonus(CombatBonusType bonusType) {
        return switch (bonusType) {
            case ATTACK_DAMAGE -> this.permanentAttackDamageBonus;
            case MOVEMENT_SPEED -> this.permanentMovementSpeedBonus;
            case JUMP_HEIGHT -> this.permanentJumpHeightBonus;
        };
    }

    /**
     * Enumeration for different types of combat bonuses.
     */
    public enum CombatBonusType {
        ATTACK_DAMAGE,
        MOVEMENT_SPEED,
        JUMP_HEIGHT
    }

    // === COPY FUNCTIONALITY === //

    /**
     * Creates a deep copy of this attachment for state comparison and sync management.
     * @return A new PlayerStatsAttachment instance with identical data
     */
    @Override
    public PlayerStatsAttachment copy() {
        PlayerStatsAttachment copy = new PlayerStatsAttachment();
        copy.copyFrom(this);
        return copy;
    }

    /**
     * Copies all data from another PlayerStatsAttachment instance.
     * @param other The attachment to copy data from
     */
    @Override
    protected void copyFrom(PlayerStatsAttachment other) {
        // Resource data
        this.permanentHealthBonus = other.permanentHealthBonus;
        this.permanentHealthRegenBonus = other.permanentHealthRegenBonus;
        this.currentHealth = other.currentHealth;
        this.maxHealth = other.maxHealth;
        this.healthRegenFraction = other.healthRegenFraction;

        this.permanentQiBonus = other.permanentQiBonus;
        this.permanentQiRegenBonus = other.permanentQiRegenBonus;
        this.currentQi = other.currentQi;
        this.maxQi = other.maxQi;
        this.qiRegenFraction = other.qiRegenFraction;

        this.permanentBodyBonus = other.permanentBodyBonus;
        this.permanentBodyRegenBonus = other.permanentBodyRegenBonus;
        this.currentBody = other.currentBody;
        this.maxBody = other.maxBody;
        this.bodyRegenFraction = other.bodyRegenFraction;

        // Combat stats data
        this.permanentAttackDamageBonus = other.permanentAttackDamageBonus;
        this.permanentMovementSpeedBonus = other.permanentMovementSpeedBonus;
        this.permanentJumpHeightBonus = other.permanentJumpHeightBonus;
        this.attackDamageBonus = other.attackDamageBonus;
        this.movementSpeedBonus = other.movementSpeedBonus;
        this.jumpHeightBonus = other.jumpHeightBonus;
    }

    /**
     * Checks if this attachment is equal to another instance for change detection.
     * @param other The attachment to compare with
     * @return True if all data fields are equal
     */
    @Override
    protected boolean isEqualTo(PlayerStatsAttachment other) {
        if (other == null) return false;

        return this.permanentHealthBonus == other.permanentHealthBonus &&
            this.permanentHealthRegenBonus == other.permanentHealthRegenBonus &&
            this.currentHealth == other.currentHealth &&
            this.maxHealth == other.maxHealth &&
            this.healthRegenFraction == other.healthRegenFraction &&

            this.permanentQiBonus == other.permanentQiBonus &&
            this.permanentQiRegenBonus == other.permanentQiRegenBonus &&
            this.currentQi == other.currentQi &&
            this.maxQi == other.maxQi &&
            this.qiRegenFraction == other.qiRegenFraction &&

            this.permanentBodyBonus == other.permanentBodyBonus &&
            this.permanentBodyRegenBonus == other.permanentBodyRegenBonus &&
            this.currentBody == other.currentBody &&
            this.maxBody == other.maxBody &&
            this.bodyRegenFraction == other.bodyRegenFraction &&

            this.permanentAttackDamageBonus == other.permanentAttackDamageBonus &&
            this.permanentMovementSpeedBonus == other.permanentMovementSpeedBonus &&
            this.permanentJumpHeightBonus == other.permanentJumpHeightBonus &&
            this.attackDamageBonus == other.attackDamageBonus &&
            this.movementSpeedBonus == other.movementSpeedBonus &&
            this.jumpHeightBonus == other.jumpHeightBonus;
    }

    // === SYNC PACKET METHODS === //

    /**
     * Creates a sync packet containing the current attachment data.
     * @param isFullSync Whether this is a full synchronization
     * @return Optional containing the sync packet if successful
     */
    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        if (player == null) {
            if (debugEnabled()) {
                debugWarn("Cannot create sync packet: no player reference");
            }
            return Optional.empty();
        }

        startPerformanceTracking("createPacket");
        try {
            CompoundTag data = serialize();
            ISyncPacket packet = createSyncPacket(data);

            onSyncPacketCreated(data, isFullSync, player);
            return Optional.of(packet);
        } finally {
            endPerformanceTracking("createPacket", 3); // 3ms warning threshold
        }
    }

    /**
     * Applies synchronized data from a received packet.
     * @param data The NBT data from the sync packet
     */
    public void applySync(CompoundTag data) {
        startPerformanceTracking("applySync");
        try {
            deserialize(data);
            clearDirty(); // Important: mark as clean after sync
            onSyncPacketApplied(data, player);
        } finally {
            endPerformanceTracking("applySync", 3); // 3ms warning threshold
        }
    }

    // === DEBUG SUPPORT === //

    /**
     * Adds specific state debug information for PlayerStatsAttachment.
     * @param info The map to add debug information to
     */
    @Override
    protected void addSpecificStateDebugInfo(Map<String, Object> info) {
        // Resource information using the unified interface
        for (StatType type : StatType.values()) {
            String name = type.getDisplayName().toLowerCase();
            info.put(name, getCurrentStat(type) + "/" + getMaxStat(type) + " (+" + getPermanentStatBonus(type) + ")");
            info.put(name + "Regen", getPermanentStatRegenBonus(type) + " (" + getRegenFraction(type) + ")");
        }

        // Combat stats information
        info.put("attackDamage", permanentAttackDamageBonus + "+" + attackDamageBonus);
        info.put("movementSpeed", permanentMovementSpeedBonus + "+" + movementSpeedBonus);
        info.put("jumpHeight", permanentJumpHeightBonus + "+" + jumpHeightBonus);
    }

    /**
     * Validates the internal state and returns any issues found.
     * @return Map of validation issues (empty if no issues)
     */
    @Override
    public Map<String, String> validateState() {
        Map<String, String> issues = new HashMap<>();

        // Validate all stat types using the unified interface
        for (StatType type : StatType.values()) {
            String name = type.getDisplayName();
            long current = getCurrentStat(type);
            long max = getMaxStat(type);
            double regenFrac = getRegenFraction(type);

            if (current < 0) issues.put("current" + name, "Negative value: " + current);
            if (current > max) issues.put("current" + name, "Exceeds maximum: " + current + " > " + max);
            if (type == StatType.HEALTH && max <= 0) issues.put("max" + name, "Non-positive value: " + max);
            if (type != StatType.HEALTH && max < 0) issues.put("max" + name, "Negative value: " + max);
            if (regenFrac < 0) issues.put(name.toLowerCase() + "RegenFraction", "Negative value: " + regenFrac);
        }

        return issues;
    }

    /**
     * Gets a hash of the current state for comparison.
     * @return State hash as a string
     */
    @Override
    public String getStateHash() {
        return String.valueOf(
            (currentHealth * 31L + maxHealth) * 31L +
                (currentQi * 31L + maxQi) * 31L +
                (currentBody * 31L + maxBody) * 31L +
                Double.hashCode(permanentAttackDamageBonus) +
                Double.hashCode(attackDamageBonus)
        );
    }

    // === CODEC FOR SERIALIZATION === //

    /**
     * Sub-codec for current stat values (6 fields)
     */
    private static final Codec<PlayerStatsAttachment> CURRENT_STATS_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("currentHealth").forGetter(d -> d.currentHealth),
            Codec.LONG.fieldOf("currentQi").forGetter(d -> d.currentQi),
            Codec.LONG.fieldOf("currentBody").forGetter(d -> d.currentBody),
            Codec.LONG.fieldOf("maxHealth").forGetter(d -> d.maxHealth),
            Codec.LONG.fieldOf("maxQi").forGetter(d -> d.maxQi),
            Codec.LONG.fieldOf("maxBody").forGetter(d -> d.maxBody)
        ).apply(instance, (currentHealth, currentQi, currentBody, maxHealth, maxQi, maxBody) -> {
            PlayerStatsAttachment attachment = new PlayerStatsAttachment();
            attachment.currentHealth = currentHealth;
            attachment.currentQi = currentQi;
            attachment.currentBody = currentBody;
            attachment.maxHealth = maxHealth;
            attachment.maxQi = maxQi;
            attachment.maxBody = maxBody;
            return attachment;
        })
    );

    /**
     * Sub-codec for regeneration fractions (3 fields)
     */
    private static final Codec<PlayerStatsAttachment> REGEN_FRACTIONS_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("healthRegenFraction").forGetter(d -> d.healthRegenFraction),
            Codec.DOUBLE.fieldOf("qiRegenFraction").forGetter(d -> d.qiRegenFraction),
            Codec.DOUBLE.fieldOf("bodyRegenFraction").forGetter(d -> d.bodyRegenFraction)
        ).apply(instance, (healthRegenFraction, qiRegenFraction, bodyRegenFraction) -> {
            PlayerStatsAttachment attachment = new PlayerStatsAttachment();
            attachment.healthRegenFraction = healthRegenFraction;
            attachment.qiRegenFraction = qiRegenFraction;
            attachment.bodyRegenFraction = bodyRegenFraction;
            return attachment;
        })
    );

    /**
     * Sub-codec for temporary bonuses (3 fields)
     */
    private static final Codec<PlayerStatsAttachment> TEMPORARY_BONUSES_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("attackDamageBonus").forGetter(d -> d.attackDamageBonus),
            Codec.DOUBLE.fieldOf("movementSpeedBonus").forGetter(d -> d.movementSpeedBonus),
            Codec.DOUBLE.fieldOf("jumpHeightBonus").forGetter(d -> d.jumpHeightBonus)
        ).apply(instance, (attackDamageBonus, movementSpeedBonus, jumpHeightBonus) -> {
            PlayerStatsAttachment attachment = new PlayerStatsAttachment();
            attachment.attackDamageBonus = attackDamageBonus;
            attachment.movementSpeedBonus = movementSpeedBonus;
            attachment.jumpHeightBonus = jumpHeightBonus;
            return attachment;
        })
    );

    /**
     * Sub-codec for source-based stat bonuses (3 fields)
     */
    private static final Codec<PlayerStatsAttachment> SOURCE_STAT_BONUSES_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(BonusSource.CODEC, Codec.LONG)
                .optionalFieldOf("healthBonusBySource", new HashMap<>())
                .forGetter(d -> d.healthBonusBySource),
            Codec.unboundedMap(BonusSource.CODEC, Codec.LONG)
                .optionalFieldOf("qiBonusBySource", new HashMap<>())
                .forGetter(d -> d.qiBonusBySource),
            Codec.unboundedMap(BonusSource.CODEC, Codec.LONG)
                .optionalFieldOf("bodyBonusBySource", new HashMap<>())
                .forGetter(d -> d.bodyBonusBySource)
        ).apply(instance, (healthBonusBySource, qiBonusBySource, bodyBonusBySource) -> {
            PlayerStatsAttachment attachment = new PlayerStatsAttachment();
            attachment.healthBonusBySource.putAll(healthBonusBySource);
            attachment.qiBonusBySource.putAll(qiBonusBySource);
            attachment.bodyBonusBySource.putAll(bodyBonusBySource);
            return attachment;
        })
    );

    /**
     * Sub-codec for source-based regeneration bonuses (3 fields)
     */
    private static final Codec<PlayerStatsAttachment> SOURCE_REGEN_BONUSES_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(BonusSource.CODEC, Codec.DOUBLE)
                .optionalFieldOf("healthRegenBonusBySource", new HashMap<>())
                .forGetter(d -> d.healthRegenBonusBySource),
            Codec.unboundedMap(BonusSource.CODEC, Codec.DOUBLE)
                .optionalFieldOf("qiRegenBonusBySource", new HashMap<>())
                .forGetter(d -> d.qiRegenBonusBySource),
            Codec.unboundedMap(BonusSource.CODEC, Codec.DOUBLE)
                .optionalFieldOf("bodyRegenBonusBySource", new HashMap<>())
                .forGetter(d -> d.bodyRegenBonusBySource)
        ).apply(instance, (healthRegenBonusBySource, qiRegenBonusBySource, bodyRegenBonusBySource) -> {
            PlayerStatsAttachment attachment = new PlayerStatsAttachment();
            attachment.healthRegenBonusBySource.putAll(healthRegenBonusBySource);
            attachment.qiRegenBonusBySource.putAll(qiRegenBonusBySource);
            attachment.bodyRegenBonusBySource.putAll(bodyRegenBonusBySource);
            return attachment;
        })
    );

    /**
     * Sub-codec for source-based combat bonuses (3 fields)
     */
    private static final Codec<PlayerStatsAttachment> SOURCE_COMBAT_BONUSES_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(BonusSource.CODEC, Codec.DOUBLE)
                .optionalFieldOf("attackDamageBonusBySource", new HashMap<>())
                .forGetter(d -> d.attackDamageBonusBySource),
            Codec.unboundedMap(BonusSource.CODEC, Codec.DOUBLE)
                .optionalFieldOf("movementSpeedBonusBySource", new HashMap<>())
                .forGetter(d -> d.movementSpeedBonusBySource),
            Codec.unboundedMap(BonusSource.CODEC, Codec.DOUBLE)
                .optionalFieldOf("jumpHeightBonusBySource", new HashMap<>())
                .forGetter(d -> d.jumpHeightBonusBySource)
        ).apply(instance, (attackDamageBonusBySource, movementSpeedBonusBySource, jumpHeightBonusBySource) -> {
            PlayerStatsAttachment attachment = new PlayerStatsAttachment();
            attachment.attackDamageBonusBySource.putAll(attackDamageBonusBySource);
            attachment.movementSpeedBonusBySource.putAll(movementSpeedBonusBySource);
            attachment.jumpHeightBonusBySource.putAll(jumpHeightBonusBySource);
            return attachment;
        })
    );

// === MAIN CODEC (6 fields - well under 13 limit) === //

    /**
     * Main codec that combines all sub-codecs for PlayerStatsAttachment.
     * Each field represents a logical group of related data.
     */
    public static final Codec<PlayerStatsAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            CURRENT_STATS_CODEC.fieldOf("current_stats").forGetter(d -> d),
            REGEN_FRACTIONS_CODEC.fieldOf("regen_fractions").forGetter(d -> d),
            TEMPORARY_BONUSES_CODEC.fieldOf("temporary_bonuses").forGetter(d -> d),
            SOURCE_STAT_BONUSES_CODEC.fieldOf("source_stat_bonuses").forGetter(d -> d),
            SOURCE_REGEN_BONUSES_CODEC.fieldOf("source_regen_bonuses").forGetter(d -> d),
            SOURCE_COMBAT_BONUSES_CODEC.fieldOf("source_combat_bonuses").forGetter(d -> d)
        ).apply(instance, (currentStats, regenFractions, temporaryBonuses, sourceStatBonuses, sourceRegenBonuses, sourceCombatBonuses) -> {
            PlayerStatsAttachment attachment = new PlayerStatsAttachment();

            // === MERGE DATA FROM ALL SUB-CODECS === //

            // Current stats
            attachment.currentHealth = currentStats.currentHealth;
            attachment.currentQi = currentStats.currentQi;
            attachment.currentBody = currentStats.currentBody;
            attachment.maxHealth = currentStats.maxHealth;
            attachment.maxQi = currentStats.maxQi;
            attachment.maxBody = currentStats.maxBody;

            // Regen fractions
            attachment.healthRegenFraction = regenFractions.healthRegenFraction;
            attachment.qiRegenFraction = regenFractions.qiRegenFraction;
            attachment.bodyRegenFraction = regenFractions.bodyRegenFraction;

            // Temporary bonuses
            attachment.attackDamageBonus = temporaryBonuses.attackDamageBonus;
            attachment.movementSpeedBonus = temporaryBonuses.movementSpeedBonus;
            attachment.jumpHeightBonus = temporaryBonuses.jumpHeightBonus;

            // Source-based stat bonuses
            attachment.healthBonusBySource.putAll(sourceStatBonuses.healthBonusBySource);
            attachment.qiBonusBySource.putAll(sourceStatBonuses.qiBonusBySource);
            attachment.bodyBonusBySource.putAll(sourceStatBonuses.bodyBonusBySource);

            // Source-based regen bonuses
            attachment.healthRegenBonusBySource.putAll(sourceRegenBonuses.healthRegenBonusBySource);
            attachment.qiRegenBonusBySource.putAll(sourceRegenBonuses.qiRegenBonusBySource);
            attachment.bodyRegenBonusBySource.putAll(sourceRegenBonuses.bodyRegenBonusBySource);

            // Source-based combat bonuses
            attachment.attackDamageBonusBySource.putAll(sourceCombatBonuses.attackDamageBonusBySource);
            attachment.movementSpeedBonusBySource.putAll(sourceCombatBonuses.movementSpeedBonusBySource);
            attachment.jumpHeightBonusBySource.putAll(sourceCombatBonuses.jumpHeightBonusBySource);

            // === RECALCULATE LEGACY TOTALS FROM SOURCES === //
            attachment.recalculateAllBonusesFromSources();

            return attachment;
        })
    );

    // === SERIALIZER === //

    /**
     * Serializer for PlayerStatsAttachment that handles NBT read/write operations.
     * Integrates with the NeoForge attachment system and provides debug support.
     */
    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerStatsAttachment> {

        /**
         * Reads a PlayerStatsAttachment from NBT data.
         * @param holder The attachment holder (typically a Player)
         * @param tag The NBT data to read from
         * @param provider The registry provider for context
         * @return The deserialized PlayerStatsAttachment
         */
        @Override
        public PlayerStatsAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerStatsAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerStatsAttachment: {}", error))
                .map(com.mojang.datafixers.util.Pair::getFirst)
                .orElseGet(PlayerStatsAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            return attachment;
        }

        /**
         * Writes a PlayerStatsAttachment to NBT data.
         * @param attachment The attachment to serialize
         * @param provider The registry provider for context
         * @return The NBT data, or null if serialization fails
         */
        @Override
        public @Nullable CompoundTag write(PlayerStatsAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerStatsAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }

    // === SERIALIZATION UPDATES === //

    /**
     * Serializes this attachment to NBT using source-based bonus tracking.
     * This method completely replaces legacy serialization.
     */
    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        try {
            // === CURRENT STAT VALUES === //
            tag.putLong("currentHealth", currentHealth);
            tag.putLong("currentQi", currentQi);
            tag.putLong("currentBody", currentBody);
            tag.putLong("maxHealth", maxHealth);
            tag.putLong("maxQi", maxQi);
            tag.putLong("maxBody", maxBody);

            // === REGENERATION FRACTIONS === //
            tag.putDouble("healthRegenFraction", healthRegenFraction);
            tag.putDouble("qiRegenFraction", qiRegenFraction);
            tag.putDouble("bodyRegenFraction", bodyRegenFraction);

            // === TEMPORARY BONUSES === //
            tag.putDouble("attackDamageBonus", attackDamageBonus);
            tag.putDouble("movementSpeedBonus", movementSpeedBonus);
            tag.putDouble("jumpHeightBonus", jumpHeightBonus);

            // === SOURCE-BASED STAT BONUSES === //
            CompoundTag sourceBonusesTag = new CompoundTag();
            serializeSourceMap(sourceBonusesTag, "health", healthBonusBySource);
            serializeSourceMap(sourceBonusesTag, "qi", qiBonusBySource);
            serializeSourceMap(sourceBonusesTag, "body", bodyBonusBySource);
            if (!sourceBonusesTag.isEmpty()) {
                tag.put("sourcedStatBonuses", sourceBonusesTag);
            }

            // === SOURCE-BASED REGEN BONUSES === //
            CompoundTag sourceRegenTag = new CompoundTag();
            serializeSourceMapDouble(sourceRegenTag, "health", healthRegenBonusBySource);
            serializeSourceMapDouble(sourceRegenTag, "qi", qiRegenBonusBySource);
            serializeSourceMapDouble(sourceRegenTag, "body", bodyRegenBonusBySource);
            if (!sourceRegenTag.isEmpty()) {
                tag.put("sourcedRegenBonuses", sourceRegenTag);
            }

            // === SOURCE-BASED COMBAT BONUSES === //
            CompoundTag sourceCombatTag = new CompoundTag();
            serializeSourceMapDouble(sourceCombatTag, "attack", attackDamageBonusBySource);
            serializeSourceMapDouble(sourceCombatTag, "speed", movementSpeedBonusBySource);
            serializeSourceMapDouble(sourceCombatTag, "jump", jumpHeightBonusBySource);
            if (!sourceCombatTag.isEmpty()) {
                tag.put("sourcedCombatBonuses", sourceCombatTag);
            }

            // === VERSION FOR FUTURE COMPATIBILITY === //
            tag.putInt("sourceBonusVersion", 1);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Serialized PlayerStatsAttachment with source-based bonus system");
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error serializing PlayerStatsAttachment: {}", e.getMessage(), e);
            // Return basic tag to prevent total failure
            tag = new CompoundTag();
            tag.putLong("currentHealth", currentHealth);
            tag.putLong("currentQi", currentQi);
            tag.putLong("currentBody", currentBody);
        }

        return tag;
    }

    /**
     * Deserializes NBT data into this attachment using source-based bonus tracking.
     * This method completely replaces legacy deserialization.
     */
    @Override
    public void deserialize(CompoundTag tag) {
        try {
            // === CURRENT STAT VALUES === //
            currentHealth = tag.getLong("currentHealth");
            currentQi = tag.getLong("currentQi");
            currentBody = tag.getLong("currentBody");
            maxHealth = tag.getLong("maxHealth");
            maxQi = tag.getLong("maxQi");
            maxBody = tag.getLong("maxBody");

            // === REGENERATION FRACTIONS === //
            healthRegenFraction = tag.getDouble("healthRegenFraction");
            qiRegenFraction = tag.getDouble("qiRegenFraction");
            bodyRegenFraction = tag.getDouble("bodyRegenFraction");

            // === TEMPORARY BONUSES === //
            attackDamageBonus = tag.getDouble("attackDamageBonus");
            movementSpeedBonus = tag.getDouble("movementSpeedBonus");
            jumpHeightBonus = tag.getDouble("jumpHeightBonus");

            // === CLEAR AND LOAD SOURCE-BASED DATA === //
            clearAllSourceMaps();

            // Load source-based stat bonuses
            if (tag.contains("sourcedStatBonuses")) {
                CompoundTag sourceBonusesTag = tag.getCompound("sourcedStatBonuses");
                deserializeSourceMap(sourceBonusesTag, "health", healthBonusBySource);
                deserializeSourceMap(sourceBonusesTag, "qi", qiBonusBySource);
                deserializeSourceMap(sourceBonusesTag, "body", bodyBonusBySource);
            }

            // Load source-based regen bonuses
            if (tag.contains("sourcedRegenBonuses")) {
                CompoundTag sourceRegenTag = tag.getCompound("sourcedRegenBonuses");
                deserializeSourceMapDouble(sourceRegenTag, "health", healthRegenBonusBySource);
                deserializeSourceMapDouble(sourceRegenTag, "qi", qiRegenBonusBySource);
                deserializeSourceMapDouble(sourceRegenTag, "body", bodyRegenBonusBySource);
            }

            // Load source-based combat bonuses
            if (tag.contains("sourcedCombatBonuses")) {
                CompoundTag sourceCombatTag = tag.getCompound("sourcedCombatBonuses");
                deserializeSourceMapDouble(sourceCombatTag, "attack", attackDamageBonusBySource);
                deserializeSourceMapDouble(sourceCombatTag, "speed", movementSpeedBonusBySource);
                deserializeSourceMapDouble(sourceCombatTag, "jump", jumpHeightBonusBySource);
            }

            // === RECALCULATE TOTALS FROM SOURCES === //
            recalculateAllBonusesFromSources();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Deserialized PlayerStatsAttachment with source-based bonus system");
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error deserializing PlayerStatsAttachment: {}", e.getMessage(), e);
            // Initialize safe defaults on error
            initializeSafeDefaults();
        }
    }

// === SERIALIZATION HELPER METHODS === //

    /**
     * Serializes a source map with long values to NBT.
     */
    private void serializeSourceMap(CompoundTag parentTag, String key, Map<BonusSource, Long> sourceMap) {
        if (!sourceMap.isEmpty()) {
            CompoundTag mapTag = new CompoundTag();
            for (Map.Entry<BonusSource, Long> entry : sourceMap.entrySet()) {
                if (entry.getValue() != 0) { // Only serialize non-zero values
                    mapTag.putLong(entry.getKey().name(), entry.getValue());
                }
            }
            if (!mapTag.isEmpty()) {
                parentTag.put(key, mapTag);
            }
        }
    }

    /**
     * Serializes a source map with double values to NBT.
     */
    private void serializeSourceMapDouble(CompoundTag parentTag, String key, Map<BonusSource, Double> sourceMap) {
        if (!sourceMap.isEmpty()) {
            CompoundTag mapTag = new CompoundTag();
            for (Map.Entry<BonusSource, Double> entry : sourceMap.entrySet()) {
                if (entry.getValue() != 0.0) { // Only serialize non-zero values
                    mapTag.putDouble(entry.getKey().name(), entry.getValue());
                }
            }
            if (!mapTag.isEmpty()) {
                parentTag.put(key, mapTag);
            }
        }
    }

    /**
     * Deserializes a source map with long values from NBT.
     */
    private void deserializeSourceMap(CompoundTag parentTag, String key, Map<BonusSource, Long> sourceMap) {
        if (parentTag.contains(key)) {
            CompoundTag mapTag = parentTag.getCompound(key);
            for (String sourceKey : mapTag.getAllKeys()) {
                try {
                    BonusSource source = BonusSource.valueOf(sourceKey);
                    long value = mapTag.getLong(sourceKey);
                    if (value != 0) {
                        sourceMap.put(source, value);
                    }
                } catch (IllegalArgumentException e) {
                    ImmortalCultivation.LOGGER.warn("Unknown bonus source in NBT: {}", sourceKey);
                }
            }
        }
    }

    /**
     * Deserializes a source map with double values from NBT.
     */
    private void deserializeSourceMapDouble(CompoundTag parentTag, String key, Map<BonusSource, Double> sourceMap) {
        if (parentTag.contains(key)) {
            CompoundTag mapTag = parentTag.getCompound(key);
            for (String sourceKey : mapTag.getAllKeys()) {
                try {
                    BonusSource source = BonusSource.valueOf(sourceKey);
                    double value = mapTag.getDouble(sourceKey);
                    if (value != 0.0) {
                        sourceMap.put(source, value);
                    }
                } catch (IllegalArgumentException e) {
                    ImmortalCultivation.LOGGER.warn("Unknown bonus source in NBT: {}", sourceKey);
                }
            }
        }
    }

// === UTILITY METHODS === //

    /**
     * Clears all source-based bonus maps.
     */
    private void clearAllSourceMaps() {
        healthBonusBySource.clear();
        qiBonusBySource.clear();
        bodyBonusBySource.clear();
        healthRegenBonusBySource.clear();
        qiRegenBonusBySource.clear();
        bodyRegenBonusBySource.clear();
        attackDamageBonusBySource.clear();
        movementSpeedBonusBySource.clear();
        jumpHeightBonusBySource.clear();
    }

    /**
     * Initializes safe default values in case of deserialization errors.
     */
    private void initializeSafeDefaults() {
        currentHealth = Math.max(currentHealth, 20L);
        currentQi = Math.max(currentQi, 0L);
        currentBody = Math.max(currentBody, 0L);
        maxHealth = Math.max(maxHealth, 20L);
        maxQi = Math.max(maxQi, 0L);
        maxBody = Math.max(maxBody, 0L);

        // Clear all source maps and totals
        clearAllSourceMaps();

        // Reset legacy fields to match empty source maps
        permanentHealthBonus = 0;
        permanentQiBonus = 0;
        permanentBodyBonus = 0;
        permanentHealthRegenBonus = 0.0;
        permanentQiRegenBonus = 0.0;
        permanentBodyRegenBonus = 0.0;
        permanentAttackDamageBonus = 0.0;
        permanentMovementSpeedBonus = 0.0;
        permanentJumpHeightBonus = 0.0;

        // Reset temporary bonuses
        attackDamageBonus = 0.0;
        movementSpeedBonus = 0.0;
        jumpHeightBonus = 0.0;

        // Reset regen fractions
        healthRegenFraction = 0.0;
        qiRegenFraction = 0.0;
        bodyRegenFraction = 0.0;

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Initialized safe defaults after deserialization error");
        }
    }
}