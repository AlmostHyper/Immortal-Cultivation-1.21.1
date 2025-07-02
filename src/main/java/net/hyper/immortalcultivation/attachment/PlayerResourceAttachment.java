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
 * Unified resource data attachment with additive permanent health bonus system.
 * Merges: PlayerHealthAttachment, PlayerQiAttachment, and PlayerBodyAttachment
 * Enhanced with automatic regeneration and meditation system integration.
 */
public class PlayerResourceAttachment implements IHealthData, IQiData, IBodyData {

    // === Player Reference ===
    @Nullable
    private Player player;

    // === Health Resource with Permanent Bonuses ===
    private static final long MORTAL_BASE_HEALTH = 20L;

    // NEW: Permanent health bonuses from realm breakthroughs
    private long permanentHealthBonus = 0L;

    private long currentHealth = MORTAL_BASE_HEALTH;
    private long maxHealth = MORTAL_BASE_HEALTH;
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

    // === NEW: Permanent Health Bonus System ===

    /**
     * Adds a permanent health bonus (from realm breakthrough).
     * This bonus persists even if the player's realm changes.
     */
    public void addPermanentHealthBonus(long healthBonus) {
        if (healthBonus <= 0) return;

        this.permanentHealthBonus += healthBonus;

        // Recalculate max health with new bonus
        recalculateMaxHealth();

        // Heal to full when gaining permanent health
        this.currentHealth = this.maxHealth;

        updateVanillaHealthBar();

        ImmortalCultivation.LOGGER.info("Player gained +{} permanent health (total bonus: +{})",
            healthBonus, this.permanentHealthBonus);
    }

    /**
     * Gets the total permanent health bonus accumulated from breakthroughs
     */
    public long getPermanentHealthBonus() {
        return permanentHealthBonus;
    }

    /**
     * Recalculates max health based on permanent bonuses and cultivation
     */
    private void recalculateMaxHealth() {
        long newMaxHealth = getMaxHealth();

        // If max health increased, maintain health percentage
        if (newMaxHealth > this.maxHealth && this.maxHealth > 0) {
            double healthPercentage = (double) this.currentHealth / this.maxHealth;
            this.maxHealth = newMaxHealth;
            this.currentHealth = Math.min((long)(newMaxHealth * healthPercentage), newMaxHealth);
        } else {
            this.maxHealth = newMaxHealth;
            this.currentHealth = Math.min(this.currentHealth, this.maxHealth);
        }
    }

    // === Health Implementation ===

    @Override
    public long getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public void setCurrentHealth(long value) {
        this.currentHealth = Math.max(0, Math.min(value, getMaxHealth()));

        Player player = getPlayerSafe();
        if (player != null) {
            updateVanillaHealthBar();
        }
    }

    @Override
    public long getBaseMaxHealth() {
        Player player = getPlayerSafe();
        if (player == null) {
            return MORTAL_BASE_HEALTH + permanentHealthBonus;
        }

        try {
            PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationAttachment == null) return MORTAL_BASE_HEALTH + permanentHealthBonus;

            // Health is COMBINED from both cultivation paths (keeping your existing logic)
            long qiRealmHealth = cultivationAttachment.getCurrentQiRealm().getBaseHealth();
            long bodyRealmHealth = cultivationAttachment.getCurrentBodyRealm().getBaseHealth();

            // If both are at mortal level (20 health each), don't double count the base
            if (qiRealmHealth == 20 && bodyRealmHealth == 20) {
                return MORTAL_BASE_HEALTH + permanentHealthBonus; // Still just a mortal + permanent bonuses
            }

            // Otherwise, add the bonuses from both paths + permanent bonuses
            long totalBonus = Math.max(0, qiRealmHealth - 20) + Math.max(0, bodyRealmHealth - 20);
            return MORTAL_BASE_HEALTH + totalBonus + permanentHealthBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating base max health", e);
            return MORTAL_BASE_HEALTH + permanentHealthBonus;
        }
    }

    @Override
    public long getMaxHealth() {
        long baseMaxHealth = getBaseMaxHealth();

        Player player = getPlayerSafe();
        if (player == null) {
            return baseMaxHealth;
        }

        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                // Small cultivation bonus to health
                long qiCultivationBonus = (long)(cultivation.getCurrentQiCultivation() * 0.0005);
                long bodyCultivationBonus = (long)(cultivation.getCurrentBodyCultivation() * 0.001);
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

            // FIXED: Combine both realms instead of taking highest
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

            // FIXED: Combine both realms instead of just using Qi realm
            long qiRealmQi = cultivation.getCurrentQiRealm().getBaseMaxQi();
            long bodyRealmQi = cultivation.getCurrentBodyRealm().getBaseMaxQi();
            long baseQi = qiRealmQi + bodyRealmQi;

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

            // FIXED: Combine both realms instead of just using Qi realm
            double qiRealmRegen = cultivation.getCurrentQiRealm().getBaseQiRegenerationRate();
            double bodyRealmRegen = cultivation.getCurrentBodyRealm().getBaseQiRegenerationRate();

            return qiRealmRegen + bodyRealmRegen;
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

            // FIXED: Combine both realms instead of just using Body realm
            // Use the same Qi values from both realms for Body energy
            long qiRealmBody = cultivation.getCurrentQiRealm().getBaseMaxQi();
            long bodyRealmBody = cultivation.getCurrentBodyRealm().getBaseMaxQi();
            long baseBody = qiRealmBody + bodyRealmBody;

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

            // FIXED: Combine both realms instead of just using Body realm
            double qiRealmBodyRegen = cultivation.getCurrentQiRealm().getBaseQiRegenerationRate();
            double bodyRealmBodyRegen = cultivation.getCurrentBodyRealm().getBaseQiRegenerationRate();

            return qiRealmBodyRegen + bodyRealmBodyRegen;
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
            // NEW: Added permanent health bonus to codec
            Codec.LONG.fieldOf("permanent_health_bonus").forGetter(d -> d.permanentHealthBonus),
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
        long permanentHealthBonus,
        long currentHealth, long maxHealth, double healthFraction,
        long currentQi, long maxQi, double qiFraction,
        long currentBody, long maxBody, double bodyFraction,
        boolean meditating) {

        PlayerResourceAttachment data = new PlayerResourceAttachment();
        data.permanentHealthBonus = permanentHealthBonus; // NEW: Load permanent health bonus
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