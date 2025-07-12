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
            markDirty();
            onStatChanged(type, oldValue, newValue);
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
     * Sets the maximum value for the specified stat type and recalculates current if needed.
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
            // Clamp current stat to new maximum
            setCurrentStat(type, getCurrentStat(type));
            markDirty();
        }
    }

    /**
     * Gets the base regeneration rate per second for the specified stat type.
     * @param type The stat type to query
     * @return Base regeneration rate per second
     */
    @Override
    public double getBaseStatRegenerationRate(StatType type) {
        return switch (type) {
            case HEALTH -> 0.1 + permanentHealthRegenBonus;
            case QI -> 0.2 + permanentQiRegenBonus;
            case BODY -> 0.15 + permanentBodyRegenBonus;
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
     * Recalculates the maximum value for a specific stat type based on base values and permanent bonuses.
     * @param type The stat type to recalculate
     */
    private void recalculateStatMaximum(StatType type) {
        long baseValue = switch (type) {
            case HEALTH -> MORTAL_BASE_HEALTH;
            case QI -> MORTAL_BASE_QI;
            case BODY -> MORTAL_BASE_BODY;
        };
        setMaxStat(type, baseValue + getPermanentStatBonus(type));
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

                    if (currentFraction >= 1.0) {
                        long regenAmount = (long) currentFraction;
                        currentFraction -= regenAmount;

                        long actualAdded = addStat(type, regenAmount);
                        if (actualAdded > 0) {
                            anyRegenOccurred = true;
                            onStatAdded(type, actualAdded);
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

    // === SERIALIZATION METHODS === //

    /**
     * Serializes the attachment data to NBT format.
     * @return CompoundTag containing all attachment data
     */
    public CompoundTag serialize() {
        startPerformanceTracking("serialize");
        try {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this)
                .resultOrPartial(error -> {
                    onSyncError("Failed to serialize PlayerStatsAttachment: " + error, player);
                    ImmortalCultivation.LOGGER.error("Failed to encode PlayerStatsAttachment: {}", error);
                })
                .orElse(new CompoundTag());
        } finally {
            endPerformanceTracking("serialize", 5); // 5ms warning threshold
        }
    }

    /**
     * Deserializes attachment data from NBT format.
     * @param tag The CompoundTag containing the data
     */
    public void deserialize(CompoundTag tag) {
        startPerformanceTracking("deserialize");
        try {
            CODEC.decode(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> {
                    onSyncError("Failed to deserialize PlayerStatsAttachment: " + error, player);
                    ImmortalCultivation.LOGGER.error("Failed to decode PlayerStatsAttachment: {}", error);
                })
                .ifPresent(pair -> {
                    PlayerStatsAttachment decoded = pair.getFirst();
                    copyFrom(decoded);
                    recalculateAll();
                });
        } finally {
            endPerformanceTracking("deserialize", 5); // 5ms warning threshold
        }
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
     * Sub-codec for health data
     */
    private static final Codec<PlayerStatsAttachment> HEALTH_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("current_health").forGetter(d -> d.currentHealth),
            Codec.LONG.fieldOf("permanent_health_bonus").forGetter(d -> d.permanentHealthBonus),
            Codec.DOUBLE.fieldOf("permanent_health_regen_bonus").forGetter(d -> d.permanentHealthRegenBonus),
            Codec.DOUBLE.optionalFieldOf("health_regen_fraction", 0.0).forGetter(d -> d.healthRegenFraction)
        ).apply(instance, (curHealth, permHealthBonus, permHealthRegen, healthRegenFrac) -> {
            PlayerStatsAttachment data = new PlayerStatsAttachment();
            data.currentHealth = curHealth;
            data.permanentHealthBonus = permHealthBonus;
            data.permanentHealthRegenBonus = permHealthRegen;
            data.healthRegenFraction = healthRegenFrac;
            return data;
        })
    );

    /**
     * Sub-codec for qi data
     */
    private static final Codec<PlayerStatsAttachment> QI_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("current_qi").forGetter(d -> d.currentQi),
            Codec.LONG.fieldOf("permanent_qi_bonus").forGetter(d -> d.permanentQiBonus),
            Codec.DOUBLE.fieldOf("permanent_qi_regen_bonus").forGetter(d -> d.permanentQiRegenBonus),
            Codec.DOUBLE.optionalFieldOf("qi_regen_fraction", 0.0).forGetter(d -> d.qiRegenFraction)
        ).apply(instance, (curQi, permQiBonus, permQiRegen, qiRegenFrac) -> {
            PlayerStatsAttachment data = new PlayerStatsAttachment();
            data.currentQi = curQi;
            data.permanentQiBonus = permQiBonus;
            data.permanentQiRegenBonus = permQiRegen;
            data.qiRegenFraction = qiRegenFrac;
            return data;
        })
    );

    /**
     * Sub-codec for body data
     */
    private static final Codec<PlayerStatsAttachment> BODY_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("current_body").forGetter(d -> d.currentBody),
            Codec.LONG.fieldOf("permanent_body_bonus").forGetter(d -> d.permanentBodyBonus),
            Codec.DOUBLE.fieldOf("permanent_body_regen_bonus").forGetter(d -> d.permanentBodyRegenBonus),
            Codec.DOUBLE.optionalFieldOf("body_regen_fraction", 0.0).forGetter(d -> d.bodyRegenFraction)
        ).apply(instance, (curBody, permBodyBonus, permBodyRegen, bodyRegenFrac) -> {
            PlayerStatsAttachment data = new PlayerStatsAttachment();
            data.currentBody = curBody;
            data.permanentBodyBonus = permBodyBonus;
            data.permanentBodyRegenBonus = permBodyRegen;
            data.bodyRegenFraction = bodyRegenFrac;
            return data;
        })
    );

    /**
     * Sub-codec for combat stats data
     */
    private static final Codec<PlayerStatsAttachment> COMBAT_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.optionalFieldOf("permanent_attack_damage_bonus", 0.0).forGetter(d -> d.permanentAttackDamageBonus),
            Codec.DOUBLE.optionalFieldOf("permanent_movement_speed_bonus", 0.0).forGetter(d -> d.permanentMovementSpeedBonus),
            Codec.DOUBLE.optionalFieldOf("permanent_jump_height_bonus", 0.0).forGetter(d -> d.permanentJumpHeightBonus),
            Codec.DOUBLE.optionalFieldOf("attack_damage_bonus", 0.0).forGetter(d -> d.attackDamageBonus),
            Codec.DOUBLE.optionalFieldOf("movement_speed_bonus", 0.0).forGetter(d -> d.movementSpeedBonus),
            Codec.DOUBLE.optionalFieldOf("jump_height_bonus", 0.0).forGetter(d -> d.jumpHeightBonus)
        ).apply(instance, (permAttack, permSpeed, permJump, tempAttack, tempSpeed, tempJump) -> {
            PlayerStatsAttachment data = new PlayerStatsAttachment();
            data.permanentAttackDamageBonus = permAttack;
            data.permanentMovementSpeedBonus = permSpeed;
            data.permanentJumpHeightBonus = permJump;
            data.attackDamageBonus = tempAttack;
            data.movementSpeedBonus = tempSpeed;
            data.jumpHeightBonus = tempJump;
            return data;
        })
    );

    /**
     * Main codec that combines all sub-codecs
     */
    public static final Codec<PlayerStatsAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            HEALTH_CODEC.fieldOf("health_data").forGetter(d -> d),
            QI_CODEC.fieldOf("qi_data").forGetter(d -> d),
            BODY_CODEC.fieldOf("body_data").forGetter(d -> d),
            COMBAT_CODEC.fieldOf("combat_data").forGetter(d -> d)
        ).apply(instance, (healthData, qiData, bodyData, combatData) -> {
            PlayerStatsAttachment data = new PlayerStatsAttachment();

            // Copy health data
            data.currentHealth = healthData.currentHealth;
            data.permanentHealthBonus = healthData.permanentHealthBonus;
            data.permanentHealthRegenBonus = healthData.permanentHealthRegenBonus;
            data.healthRegenFraction = healthData.healthRegenFraction;

            // Copy qi data
            data.currentQi = qiData.currentQi;
            data.permanentQiBonus = qiData.permanentQiBonus;
            data.permanentQiRegenBonus = qiData.permanentQiRegenBonus;
            data.qiRegenFraction = qiData.qiRegenFraction;

            // Copy body data
            data.currentBody = bodyData.currentBody;
            data.permanentBodyBonus = bodyData.permanentBodyBonus;
            data.permanentBodyRegenBonus = bodyData.permanentBodyRegenBonus;
            data.bodyRegenFraction = bodyData.bodyRegenFraction;

            // Copy combat data
            data.permanentAttackDamageBonus = combatData.permanentAttackDamageBonus;
            data.permanentMovementSpeedBonus = combatData.permanentMovementSpeedBonus;
            data.permanentJumpHeightBonus = combatData.permanentJumpHeightBonus;
            data.attackDamageBonus = combatData.attackDamageBonus;
            data.movementSpeedBonus = combatData.movementSpeedBonus;
            data.jumpHeightBonus = combatData.jumpHeightBonus;

            data.recalculateAll();
            return data;
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
}