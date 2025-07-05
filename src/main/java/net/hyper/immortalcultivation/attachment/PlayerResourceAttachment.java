package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IHealthData;
import net.hyper.immortalcultivation.cultivation.IQiData;
import net.hyper.immortalcultivation.cultivation.IBodyData;
import net.hyper.immortalcultivation.network.server.ResourceSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * CORRECTED: Unified resource data attachment using ONLY permanent additive bonuses.
 * This system never calculates stats from "current realm" - only from accumulated permanent bonuses.
 * Each JSON realm defines the permanent bonus you GET when reaching it, not base stats while in it.
 */
public class PlayerResourceAttachment implements IHealthData, IQiData, IBodyData {

    // === Player Reference ===
    @Nullable
    private Player player;

    // === MORTAL BASE VALUES (starting values for new players) ===
    private static final long MORTAL_BASE_HEALTH = 20L; // 1 heart
    private static final long MORTAL_BASE_QI = 100L;
    private static final long MORTAL_BASE_BODY = 100L;
    private static final double MORTAL_BASE_HEALTH_REGEN = 0.0;
    private static final double MORTAL_BASE_QI_REGEN = 1.0;
    private static final double MORTAL_BASE_BODY_REGEN = 1.0;

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
    private long lastSyncedHash = 0;
    private int tickCounter = 0;
    private static final int SYNC_INTERVAL = 10;

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
    protected Player getPlayerSafe() {
        if (player != null && player.isAlive()) {
            return player;
        }
        return null;
    }

    // === PERMANENT HEALTH BONUS SYSTEM ===

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

        // Heal to full when gaining permanent health (Xianxia tradition)
        if (this.maxHealth > oldMaxHealth) {
            this.currentHealth = this.maxHealth;
        }

        updateVanillaHealthBar();
        forceSync();

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
        forceSync();

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
        forceSync();

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
        forceSync();

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
        forceSync();

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
        forceSync();

        ImmortalCultivation.LOGGER.info("Player {} gained +{}/sec permanent body regen from realm breakthrough (total: +{}/sec)",
            player != null ? player.getName().getString() : "unknown",
            String.format("%.3f", bodyRegenBonus),
            String.format("%.3f", this.permanentBodyRegenBonus));
    }

    private void recalculateMaxBody() {
        this.maxBody = MORTAL_BASE_BODY + permanentMaxBodyBonus;

        if (this.currentBody > this.maxBody) {
            this.currentBody = this.maxBody;
        }
    }

    // === HEALTH INTERFACE IMPLEMENTATION ===

    @Override
    public long getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public void setCurrentHealth(long value) {
        this.currentHealth = Math.max(0, Math.min(value, maxHealth));
        updateVanillaHealthBar();
    }

    @Override
    public long getBaseMaxHealth() {
        return MORTAL_BASE_HEALTH + permanentHealthBonus;
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
    public double getBaseCirculatingHealthRegenerationRate() {
        return getBaseHealthRegenerationRate() * 3.0;
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
    public double getEffectiveCirculatingHealthRegenerationRate() {
        double baseCirculatingRate = getBaseCirculatingHealthRegenerationRate();

        Player player = getPlayerSafe();
        if (player != null) {
            try {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null && cultivation.getCurrentFocus() == PlayerCultivationAttachment.CultivationType.BODY) {
                    baseCirculatingRate *= 1.2;
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error applying focus bonus to circulating health regen", e);
            }
        }

        return baseCirculatingRate;
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
        this.currentQi = Math.max(0, Math.min(value, maxQi));
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
    public double getBaseCirculatingQiRegenerationRate() {
        return getBaseQiRegenerationRate() * 9.0;
    }

    @Override
    public double getEffectiveQiRegenerationRate() {
        double baseRate = getBaseQiRegenerationRate();

        Player player = getPlayerSafe();
        if (player != null) {
            try {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null) {
                    baseRate *= cultivation.getQiRegenerationBonus();
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error applying cultivation bonus to qi regen", e);
            }
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingQiRegenerationRate() {
        double baseCirculatingRate = getBaseCirculatingQiRegenerationRate();

        Player player = getPlayerSafe();
        if (player != null) {
            try {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null) {
                    baseCirculatingRate *= cultivation.getQiRegenerationBonus();
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error applying cultivation bonus to circulating qi regen", e);
            }
        }

        return baseCirculatingRate;
    }

    // === BODY INTERFACE IMPLEMENTATION ===

    @Override
    public long getCurrentBody() {
        return currentBody;
    }

    @Override
    public void setCurrentBody(long value) {
        this.currentBody = Math.max(0, Math.min(value, maxBody));
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
    public double getBaseCirculatingBodyRegenerationRate() {
        return getBaseBodyRegenerationRate() * 9.0;
    }

    @Override
    public double getEffectiveBodyRegenerationRate() {
        double baseRate = getBaseBodyRegenerationRate();

        Player player = getPlayerSafe();
        if (player != null) {
            try {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null) {
                    baseRate *= cultivation.getBodyRegenerationBonus();
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error applying cultivation bonus to body regen", e);
            }
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingBodyRegenerationRate() {
        double baseCirculatingRate = getBaseCirculatingBodyRegenerationRate();

        Player player = getPlayerSafe();
        if (player != null) {
            try {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null) {
                    baseCirculatingRate *= cultivation.getBodyRegenerationBonus();
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error applying cultivation bonus to circulating body regen", e);
            }
        }

        return baseCirculatingRate;
    }

    // === UTILITY METHODS ===

    public void healHealth(long amount) {
        setCurrentHealth(currentHealth + amount);
    }

    public void restoreQi(long amount) {
        setCurrentQi(currentQi + amount);
    }

    public void restoreBody(long amount) {
        setCurrentBody(currentBody + amount);
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

    public void regenerateHealth(double rate, boolean circulating) {
        if (getCurrentHealth() >= getMaxHealth()) return;

        healthRegenFraction += rate;
        long wholePoints = (long) healthRegenFraction;
        healthRegenFraction -= wholePoints;

        if (wholePoints > 0) {
            healHealth(wholePoints);
        }
    }

    public void regenerateQi(double rate, boolean circulating) {
        if (getCurrentQi() >= getMaxQi()) return;

        qiRegenFraction += rate;
        long wholePoints = (long) qiRegenFraction;
        qiRegenFraction -= wholePoints;

        if (wholePoints > 0) {
            restoreQi(wholePoints);
        }
    }

    public void regenerateBody(double rate, boolean circulating) {
        if (getCurrentBody() >= getMaxBody()) return;

        bodyRegenFraction += rate;
        long wholePoints = (long) bodyRegenFraction;
        bodyRegenFraction -= wholePoints;

        if (wholePoints > 0) {
            restoreBody(wholePoints);
        }
    }

    // === TICK PROCESSING ===

    public void tick() {
        if (player == null) return;

        // Handle sync
        tickCounter++;
        if (tickCounter >= SYNC_INTERVAL) {
            tickCounter = 0;
            syncIfNeeded();
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

    private void syncIfNeeded() {
        long currentHash = calculateStateHash();
        if (currentHash != lastSyncedHash) {
            lastSyncedHash = currentHash;
            syncToClient();
        }
    }

    private long calculateStateHash() {
        return (currentHealth << 42) | (currentQi << 21) | currentBody;
    }

    private void syncToClient() {
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                ResourceSyncPacket packet = new ResourceSyncPacket(
                    currentHealth, maxHealth,
                    permanentHealthBonus,
                    permanentHealthRegenBonus,
                    currentQi, maxQi,
                    currentBody, maxBody
                );
                PacketDistributor.sendToPlayer(serverPlayer, packet);

                ImmortalCultivation.LOGGER.debug("Synced resource data to client for player: {}",
                    serverPlayer.getName().getString());
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing resource data for player {}",
                    player.getName().getString(), e);
            }
        }
    }

    /**
     * CRITICAL: Force sync method for dimension changes and initialization.
     * This ensures the client receives updated data even if internal state hasn't changed.
     */
    public void forceSync() {
        try {
            lastSyncedHash = 0; // Reset hash to force sync
            syncToClient();
            ImmortalCultivation.LOGGER.debug("Force synced resource data for player: {}",
                player != null ? player.getName().getString() : "unknown");
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error force syncing resource data", e);
        }
    }

    // === GETTERS FOR PERMANENT BONUSES ===

    public long getPermanentHealthBonus() { return permanentHealthBonus; }
    public double getPermanentHealthRegenBonus() { return permanentHealthRegenBonus; }
    public long getPermanentMaxQiBonus() { return permanentMaxQiBonus; }
    public double getPermanentQiRegenBonus() { return permanentQiRegenBonus; }
    public long getPermanentMaxBodyBonus() { return permanentMaxBodyBonus; }
    public double getPermanentBodyRegenBonus() { return permanentBodyRegenBonus; }

    // === SETTERS FOR SYNC (called by sync packets) ===

    public void setPermanentHealthBonus(long bonus) {
        this.permanentHealthBonus = Math.max(0, bonus);
    }

    public void setPermanentHealthRegenBonus(double bonus) {
        this.permanentHealthRegenBonus = Math.max(0.0, bonus);
    }

    public void setPermanentMaxQiBonus(long bonus) {
        this.permanentMaxQiBonus = Math.max(0, bonus);
    }

    public void setPermanentQiRegenBonus(double bonus) {
        this.permanentQiRegenBonus = Math.max(0.0, bonus);
    }

    public void setPermanentMaxBodyBonus(long bonus) {
        this.permanentMaxBodyBonus = Math.max(0, bonus);
    }

    public void setPermanentBodyRegenBonus(double bonus) {
        this.permanentBodyRegenBonus = Math.max(0.0, bonus);
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

    // === LEGACY NBT METHODS (not used with Codec but required by interface) ===

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Not needed with Codec
    }

    @Override
    public CompoundTag serializeNBT() {
        // Not needed with Codec
        return new CompoundTag();
    }
}