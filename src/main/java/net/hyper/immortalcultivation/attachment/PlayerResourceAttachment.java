package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IHealthData;
import net.hyper.immortalcultivation.cultivation.IQiData;
import net.hyper.immortalcultivation.cultivation.IBodyData;
import net.hyper.immortalcultivation.network.server.ResourceSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Unified resource data attachment.
 * Merges: PlayerHealthAttachment, PlayerQiAttachment, and PlayerBodyAttachment
 * Enhanced with automatic regeneration and meditation system integration.
 */
public class PlayerResourceAttachment implements IHealthData, IQiData, IBodyData {

    // === Player Reference ===
    @Nullable
    private Player player;

    // === Health Resource ===
    private long currentHealth = 20;
    private long maxHealth = 20;
    private double healthRegenFraction = 0.0;

    // === Qi Resource ===
    private long currentQi = 0;
    private long maxQi = 100;
    private double qiRegenFraction = 0.0;

    // === Body Resource ===
    private long currentBody = 0;
    private long maxBody = 100;
    private double bodyRegenFraction = 0.0;

    // === Meditation State ===
    private boolean isPlayerMeditating = false;
    private int meditationTicks = 0;
    private static final int MEDITATION_THRESHOLD = 60; // 3 seconds

    // === Sync Management ===
    private long lastSyncedHash = 0;
    private int tickCounter = 0;
    private static final int SYNC_INTERVAL = 10; // More frequent for resources

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

    // === Health Implementation ===

    @Override
    public long getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public void setCurrentHealth(long value) {
        this.currentHealth = Math.max(0, Math.min(value, maxHealth));

        Player player = getPlayerSafe();
        if (player != null) {
            updateVanillaHealthBar();
        }
    }

    @Override
    public long getBaseMaxHealth() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 20; // Default Minecraft health
        }

        try {
            PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationAttachment == null) return 20;

            // Health is COMBINED from both cultivation paths
            long qiRealmHealth = cultivationAttachment.getCurrentQiRealm().getBaseHealth();
            long bodyRealmHealth = cultivationAttachment.getCurrentBodyRealm().getBaseHealth();

            // If both are at mortal level (20 health each), don't double count the base
            if (qiRealmHealth == 20 && bodyRealmHealth == 20) {
                return 20; // Still just a mortal
            }

            // Otherwise, add the bonuses from both paths
            long totalBonus = Math.max(0, qiRealmHealth - 20) + Math.max(0, bodyRealmHealth - 20);
            return 20 + totalBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating base max health", e);
            return 20;
        }
    }

    @Override
    public long getMaxHealth() {
        // FIXED: Calculate max health properly, avoiding circular calls
        long baseMaxHealth = getBaseMaxHealth();

        Player player = getPlayerSafe();
        if (player == null) {
            return baseMaxHealth;
        }

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                // Small cultivation bonus to health
                long qiCultivationBonus = (long)(cultivation.getCurrentQiCultivation() * 0.005);
                long bodyCultivationBonus = (long)(cultivation.getCurrentBodyCultivation() * 0.01);
                return baseMaxHealth + qiCultivationBonus + bodyCultivationBonus;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating cultivation health bonus", e);
        }

        return baseMaxHealth;
    }

    public void setMaxHealth(long value) {
        this.maxHealth = Math.max(1, value);
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth;
        }
        updateVanillaHealthBar();
    }

    public void damageHealth(long amount) {
        setCurrentHealth(currentHealth - amount);
    }

    public void healHealth(long amount) {
        setCurrentHealth(currentHealth + amount);
    }

    public double getHealthProgress() {
        if (maxHealth == 0) return 0.0;
        return (double) currentHealth / maxHealth;
    }

    @Override
    public double getBaseHealthRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) return 0.1;

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) return 0.1;

            double qiRegen = cultivation.getCurrentQiRealm().getBaseHealthRegeneration();
            double bodyRegen = cultivation.getCurrentBodyRealm().getBaseHealthRegeneration();

            return qiRegen + bodyRegen;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting health regen rate", e);
            return 0.1;
        }
    }

    @Override
    public double getBaseCirculatingHealthRegenerationRate() {
        return getBaseHealthRegenerationRate() * 3.0;
    }

    @Override
    public double getEffectiveHealthRegenerationRate() {
        double baseRate = getBaseHealthRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseRate;

        // Apply focus bonus
        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null && cultivation.getCurrentFocus() == PlayerCultivationAttachment.CultivationType.BODY) {
                baseRate *= 1.5;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus to health", e);
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingHealthRegenerationRate() {
        double baseCirculatingRate = getBaseCirculatingHealthRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseCirculatingRate;

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null && cultivation.getCurrentFocus() == PlayerCultivationAttachment.CultivationType.BODY) {
                baseCirculatingRate *= 1.2;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus", e);
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

    // === Qi Implementation ===

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
        Player player = getPlayerSafe();
        if (player == null) {
            return 100; // Default base Qi
        }

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) return 100;

            // Base Qi from realm
            long baseQi = cultivation.getCurrentQiRealm().getBaseMaxQi();

            // Small cultivation bonus to max Qi
            long qiCultivationBonus = (long)(cultivation.getCurrentQiCultivation() * 0.01);
            long bodyCultivationBonus = (long)(cultivation.getCurrentBodyCultivation() * 0.005);

            return baseQi + qiCultivationBonus + bodyCultivationBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating base max Qi", e);
            return 100;
        }
    }

    public void setMaxQi(long value) {
        this.maxQi = Math.max(1, value);
        if (currentQi > maxQi) {
            currentQi = maxQi;
        }
    }

    @Override
    public void consumeQi(long amount) {
        setCurrentQi(currentQi - amount);
    }

    public void restoreQi(long amount) {
        setCurrentQi(currentQi + amount);
    }

    public boolean hasEnoughQi(long amount) {
        return currentQi >= amount;
    }

    public double getQiProgress() {
        if (maxQi == 0) return 0.0;
        return (double) currentQi / maxQi;
    }

    @Override
    public double getBaseQiRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) return 0.0;

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) return 0.0;

            return cultivation.getCurrentQiRealm().getBaseQiRegenerationRate();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base Qi regen rate", e);
            return 0.0;
        }
    }

    @Override
    public double getBaseCirculatingQiRegenerationRate() {
        return getBaseQiRegenerationRate() * 9.0;
    }

    @Override
    public double getEffectiveQiRegenerationRate() {
        double baseRate = getBaseQiRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseRate;

        // Apply focus bonus
        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                baseRate *= cultivation.getQiRegenerationBonus();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus to Qi regen", e);
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingQiRegenerationRate() {
        double baseCirculatingRate = getBaseCirculatingQiRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseCirculatingRate;

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                baseCirculatingRate *= cultivation.getQiRegenerationBonus();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus", e);
        }

        return baseCirculatingRate;
    }

    // === Body Implementation ===

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
        Player player = getPlayerSafe();
        if (player == null) {
            return 100; // Default base Body
        }

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) return 100;

            // Base Body from realm (using same as Qi)
            long baseBody = cultivation.getCurrentBodyRealm().getBaseMaxQi();

            // Small cultivation bonus to max Body
            long bodyCultivationBonus = (long)(cultivation.getCurrentBodyCultivation() * 0.01);
            long qiCultivationBonus = (long)(cultivation.getCurrentQiCultivation() * 0.005);

            return baseBody + bodyCultivationBonus + qiCultivationBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating base max Body", e);
            return 100;
        }
    }

    public void setMaxBody(long value) {
        this.maxBody = Math.max(1, value);
        if (currentBody > maxBody) {
            currentBody = maxBody;
        }
    }

    @Override
    public void consumeBody(long amount) {
        setCurrentBody(currentBody - amount);
    }

    public void restoreBody(long amount) {
        setCurrentBody(currentBody + amount);
    }

    public boolean hasEnoughBody(long amount) {
        return currentBody >= amount;
    }

    public double getBodyProgress() {
        if (maxBody == 0) return 0.0;
        return (double) currentBody / maxBody;
    }

    @Override
    public double getBaseBodyRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) return 0.0;

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) return 0.0;

            return cultivation.getCurrentBodyRealm().getBaseQiRegenerationRate(); // Body realms use same regen as Qi
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base Body regen rate", e);
            return 0.0;
        }
    }

    @Override
    public double getBaseCirculatingBodyRegenerationRate() {
        return getBaseBodyRegenerationRate() * 9.0;
    }

    @Override
    public double getEffectiveBodyRegenerationRate() {
        double baseRate = getBaseBodyRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseRate;

        // Apply focus bonus
        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                baseRate *= cultivation.getBodyRegenerationBonus();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus to Body regen", e);
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingBodyRegenerationRate() {
        double baseCirculatingRate = getBaseCirculatingBodyRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseCirculatingRate;

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                baseCirculatingRate *= cultivation.getBodyRegenerationBonus();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus", e);
        }

        return baseCirculatingRate;
    }

    // === Meditation System ===

    public boolean isPlayerMeditating() {
        return isPlayerMeditating;
    }

    public void setMeditating(boolean meditating) {
        this.isPlayerMeditating = meditating;
        if (!meditating) {
            this.meditationTicks = 0;
        }
    }

    public void updateMeditationState(boolean shouldBeMeditating) {
        if (shouldBeMeditating) {
            if (!isPlayerMeditating) {
                meditationTicks++;
                if (meditationTicks >= MEDITATION_THRESHOLD) {
                    setMeditating(true);
                }
            }
        } else {
            setMeditating(false);
            meditationTicks = 0;
        }
    }

    public void startMeditation() {
        // Called when player meets meditation conditions (crouching + stationary)
        // Uses threshold system to prevent instant meditation
        updateMeditationState(true);

        // Log meditation progress for debugging
        if (!isPlayerMeditating && meditationTicks > 0) {
            ImmortalCultivation.LOGGER.debug("Player building meditation focus: {}/{} ticks",
                meditationTicks, MEDITATION_THRESHOLD);
        }
    }

    public void stopMeditation() {
        // Called when player no longer meets meditation conditions
        // Immediately stops meditation and resets progress
        updateMeditationState(false);

        if (isPlayerMeditating) {
            ImmortalCultivation.LOGGER.debug("Player stopped meditating");
        }
    }

    /**
     * Get meditation progress as a percentage (0.0 to 1.0)
     */
    public double getMeditationProgress() {
        if (isPlayerMeditating) return 1.0;
        return Math.min(1.0, (double) meditationTicks / MEDITATION_THRESHOLD);
    }

    /**
     * Get ticks remaining until meditation starts
     */
    public int getTicksUntilMeditation() {
        if (isPlayerMeditating) return 0;
        return Math.max(0, MEDITATION_THRESHOLD - meditationTicks);
    }

    /**
     * Check if player is building up to meditation
     */
    public boolean isBuildingMeditation() {
        return !isPlayerMeditating && meditationTicks > 0;
    }

    // === Enhanced Tick Processing ===

    public void tick() {
        if (player == null) return;

        // Update max values based on current cultivation realms
        updateMaxValues();

        // Process regeneration based on meditation state
        processRegeneration();

        // Handle sync
        tickCounter++;
        if (tickCounter >= SYNC_INTERVAL) {
            tickCounter = 0;
            syncIfNeeded();
        }
    }

    /**
     * Updates max values from cultivation realms
     */
    private void updateMaxValues() {
        try {
            long newMaxHealth = getMaxHealth();
            long newMaxQi = getBaseMaxQi();
            long newMaxBody = getBaseMaxBody();

            // Update max values if they changed
            if (newMaxHealth != maxHealth) {
                setMaxHealth(newMaxHealth);
            }
            if (newMaxQi != maxQi) {
                setMaxQi(newMaxQi);
            }
            if (newMaxBody != maxBody) {
                setMaxBody(newMaxBody);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating max values for player {}",
                player != null ? player.getName().getString() : "null", e);
        }
    }

    /**
     * Process regeneration for all resource types based on meditation state
     */
    private void processRegeneration() {
        if (player == null) return;

        try {
            // Convert per-second rates to per-tick rates
            double healthRate = isPlayerMeditating ?
                getEffectiveCirculatingHealthRegenerationRate() / 20.0 :
                getEffectiveHealthRegenerationRate() / 20.0;

            double qiRate = isPlayerMeditating ?
                getEffectiveCirculatingQiRegenerationRate() / 20.0 :
                getEffectiveQiRegenerationRate() / 20.0;

            double bodyRate = isPlayerMeditating ?
                getEffectiveCirculatingBodyRegenerationRate() / 20.0 :
                getEffectiveBodyRegenerationRate() / 20.0;

            // Apply regeneration
            if (healthRate > 0 && getCurrentHealth() < getMaxHealth()) {
                regenerateHealth(healthRate, isPlayerMeditating);
            }

            if (qiRate > 0 && getCurrentQi() < getMaxQi()) {
                regenerateQi(qiRate, isPlayerMeditating);
            }

            if (bodyRate > 0 && getCurrentBody() < getMaxBody()) {
                regenerateBody(bodyRate, isPlayerMeditating);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing regeneration for player {}",
                player.getName().getString(), e);
        }
    }

    // === Enhanced Regeneration Methods ===

    public void regenerateHealth(double rate, boolean circulating) {
        if (getCurrentHealth() >= getMaxHealth()) return;

        healthRegenFraction += rate;
        long wholePoints = (long) healthRegenFraction;
        healthRegenFraction -= wholePoints;

        if (wholePoints > 0) {
            healHealth(wholePoints);

            // Debug logging for significant healing
            if (wholePoints >= 5) {
                ImmortalCultivation.LOGGER.debug("Player healed {} health ({})",
                    wholePoints, circulating ? "meditating" : "normal");
            }
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

    // === Utility Methods ===

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Not needed with Codec
    }

    @Override
    public CompoundTag serializeNBT() {
        // Not needed with Codec
        return new CompoundTag();
    }

    private void updateVanillaHealthBar() {
        Player player = getPlayerSafe();
        if (player != null) {
            float healthPercentage = (float) getHealthProgress();
            float vanillaHealth = player.getMaxHealth() * healthPercentage;
            player.setHealth(Math.max(1.0f, vanillaHealth));
        }
    }

    public void setRegenFraction(double health, double qi, double body) {
        this.healthRegenFraction = health;
        this.qiRegenFraction = qi;
        this.bodyRegenFraction = body;
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
        return (currentHealth << 42) | (currentQi << 21) | currentBody;
    }

    private void syncToClient() {
        if (player instanceof ServerPlayer serverPlayer) {
            ResourceSyncPacket packet = new ResourceSyncPacket(
                currentHealth, maxHealth,
                currentQi, maxQi,
                currentBody, maxBody,
                isPlayerMeditating
            );
            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }

    public void forceSync() {
        lastSyncedHash = 0;
        syncToClient();
    }

    // === Serialization ===

    public static final Codec<PlayerResourceAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("current_health").forGetter(d -> d.currentHealth),
            Codec.LONG.fieldOf("max_health").forGetter(d -> d.maxHealth),
            Codec.DOUBLE.fieldOf("health_regen_fraction").forGetter(d -> d.healthRegenFraction),
            Codec.LONG.fieldOf("current_qi").forGetter(d -> d.currentQi),
            Codec.LONG.fieldOf("max_qi").forGetter(d -> d.maxQi),
            Codec.DOUBLE.fieldOf("qi_regen_fraction").forGetter(d -> d.qiRegenFraction),
            Codec.LONG.fieldOf("current_body").forGetter(d -> d.currentBody),
            Codec.LONG.fieldOf("max_body").forGetter(d -> d.maxBody),
            Codec.DOUBLE.fieldOf("body_regen_fraction").forGetter(d -> d.bodyRegenFraction),
            Codec.BOOL.fieldOf("is_meditating").forGetter(d -> d.isPlayerMeditating)
        ).apply(instance, PlayerResourceAttachment::decode)
    );

    private static PlayerResourceAttachment decode(
        long currentHealth, long maxHealth, double healthFraction,
        long currentQi, long maxQi, double qiFraction,
        long currentBody, long maxBody, double bodyFraction,
        boolean meditating) {

        PlayerResourceAttachment data = new PlayerResourceAttachment();
        data.currentHealth = currentHealth;
        data.maxHealth = maxHealth;
        data.healthRegenFraction = healthFraction;
        data.currentQi = currentQi;
        data.maxQi = maxQi;
        data.qiRegenFraction = qiFraction;
        data.currentBody = currentBody;
        data.maxBody = maxBody;
        data.bodyRegenFraction = bodyFraction;
        data.isPlayerMeditating = meditating;

        return data;
    }
}