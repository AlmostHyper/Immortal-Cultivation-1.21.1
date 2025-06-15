package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.cultivation.ICultivationData;
import net.hyper.immortalcultivation.cultivation.IQiData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PlayerRealmAttachment {
    @Nullable
    private Player player;

    private CultivationRealm currentRealm;
    @Nullable
    private CultivationRealm previousRealm;

    // Breakthrough tracking
    private long breakthroughAttempts = 0;
    private long lastBreakthroughAttempt = 0; // Game time
    private boolean breakthroughCooldown = false;

    // Realm advancement tracking
    private long timeInCurrentRealm = 0;
    private long totalCultivationTime = 0;

    public PlayerRealmAttachment() {
        this.player = null;
        // Set default starting realm
        this.currentRealm = ICModCultivationRealms.MORTAL;
    }

    public PlayerRealmAttachment(Player player) {
        this.player = player;
        this.currentRealm = ICModCultivationRealms.MORTAL;
    }

    // === Player Management ===

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    // === Realm Management ===

    public CultivationRealm getCurrentRealm() {
        return currentRealm;
    }

    @Nullable
    public CultivationRealm getPreviousRealm() {
        return previousRealm;
    }

    public void setCurrentRealm(CultivationRealm realm) {
        setCurrentRealm(realm, true);
    }

    public void setCurrentRealm(CultivationRealm realm, boolean triggerEvents) {
        if (realm == null) {
            throw new IllegalArgumentException("Cultivation realm cannot be null");
        }

        CultivationRealm oldRealm = this.currentRealm;
        this.previousRealm = oldRealm;
        this.currentRealm = realm;
        this.timeInCurrentRealm = 0; // Reset time in realm

        if (triggerEvents && player != null) {
            ICultivationData cultivationData = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

            // Call exit event on old realm
            if (oldRealm != null) {
                oldRealm.onRealmExit(player, cultivationData, realm);
            }

            // Call advancement event on new realm
            realm.onRealmAdvancement(player, cultivationData, oldRealm);

            // Sync changes to client
            if (cultivationData instanceof PlayerQiAttachment qiComponent) {
                qiComponent.forceSync();
            }

            // Apply new realm modifiers
            realm.applyRealmModifiers(player, cultivationData);
        }
    }

    // === Breakthrough Logic ===

    /**
     * Check if player can attempt breakthrough
     * @return true if breakthrough attempt is possible
     */
    public boolean canAttemptBreakthrough() {
        if (player == null || currentRealm == null) return false;

        ICultivationData cultivationData = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivationData == null) return false;

        // Check cooldown
        if (breakthroughCooldown) {
            long currentTime = player.level().getGameTime();
            if (currentTime - lastBreakthroughAttempt < getBreakthroughCooldownTicks()) {
                return false;
            } else {
                breakthroughCooldown = false;
            }
        }

        return currentRealm.canBreakthrough(player, cultivationData);
    }

    /**
     * Check breakthrough requirements without cooldown consideration
     * @return true if player meets breakthrough requirements
     */
    public boolean meetsBreakthroughRequirements() {
        if (player == null || currentRealm == null) return false;

        ICultivationData cultivationData = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivationData == null) return false;

        return currentRealm.canBreakthrough(player, cultivationData);
    }

    /**
     * Attempt breakthrough to next realm
     * @return BreakthroughResult indicating success/failure and details
     */
    public BreakthroughResult attemptBreakthrough() {
        return attemptBreakthrough(BreakthroughType.MINOR);
    }

    /**
     * Attempt specific type of breakthrough
     * @param type Type of breakthrough to attempt
     * @return BreakthroughResult with details
     */
    public BreakthroughResult attemptBreakthrough(BreakthroughType type) {
        if (player == null || currentRealm == null) {
            return new BreakthroughResult(false, "Invalid player or realm state", null);
        }

        ICultivationData cultivationData = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivationData == null) {
            return new BreakthroughResult(false, "No Cultivation data found", null);
        }

        // Check if attempt is possible
        if (!canAttemptBreakthrough()) {
            if (breakthroughCooldown) {
                long remainingTicks = getBreakthroughCooldownTicks() -
                    (player.level().getGameTime() - lastBreakthroughAttempt);
                return new BreakthroughResult(false,
                    "Breakthrough cooldown: " + (remainingTicks / 20) + " seconds remaining", null);
            } else {
                return new BreakthroughResult(false, "Breakthrough requirements not met", null);
            }
        }

        // Record attempt
        breakthroughAttempts++;
        lastBreakthroughAttempt = player.level().getGameTime();

        // Determine target realm based on type
        CultivationRealm targetRealm = switch (type) {
            case MINOR -> currentRealm.getMinorBreakthrough(player, cultivationData);
            case MAJOR -> currentRealm.getMajorBreakthrough(player, cultivationData);
            case SPECIAL -> currentRealm.getSpecialBreakthrough(player, cultivationData);
            case BOUNDARY -> currentRealm.getBoundaryBreakthrough(player, cultivationData);
        };

        if (targetRealm == null) {
            return new BreakthroughResult(false, "No valid breakthrough path found", null);
        }

        // Calculate success chance (could be modified by realm, items, etc.)
        double successChance = calculateBreakthroughSuccessChance(type, targetRealm);
        boolean success = player.level().random.nextDouble() < successChance;

        if (success) {
            // Advance to new realm
            setCurrentRealm(targetRealm);

            return new BreakthroughResult(true,
                "Successfully advanced to " + targetRealm.getDisplayName(), targetRealm);
        } else {

            return new BreakthroughResult(false,
                "Breakthrough failed.", null);
        }
    }

    /**
     * Calculate success chance for breakthrough attempt
     * @param type Breakthrough type
     * @param targetRealm Target realm
     * @return Success chance (0.0 to 1.0)
     */
    private double calculateBreakthroughSuccessChance(BreakthroughType type, CultivationRealm targetRealm) {
        double baseChance = switch (type) {
            case MINOR -> 0.5;
            case MAJOR -> 0.3;
            case SPECIAL -> 0.2;
            case BOUNDARY -> 0.1;
        };

        // Modify based on current Cultivation percentage of realm max
        ICultivationData qiData = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        double cultivationPercentage = qiData.getCultivationPercentage();
        double qiBonus = (cultivationPercentage - 0.8) * 0.10;

        // Modify based on previous attempts (diminishing returns)
        double attemptBonus = Math.min(breakthroughAttempts * 0.05, 0.5);

        return Math.max(0.10, Math.min(1.0, baseChance + qiBonus - attemptBonus));
    }

    /**
     * Get breakthrough cooldown in ticks
     * @return Cooldown duration in ticks
     */
    private long getBreakthroughCooldownTicks() {
        return 20 * 30; // 30 seconds default
    }

    // === Realm Progression Tracking ===

    public long getBreakthroughAttempts() {
        return breakthroughAttempts;
    }

    public long getTimeInCurrentRealm() {
        return timeInCurrentRealm;
    }

    public long getTotalCultivationTime() {
        return totalCultivationTime;
    }

    public boolean isOnBreakthroughCooldown() {
        if (!breakthroughCooldown) return false;

        long currentTime = player != null ? player.level().getGameTime() : 0;
        return currentTime - lastBreakthroughAttempt < getBreakthroughCooldownTicks();
    }

    public long getRemainingCooldownTicks() {
        if (!isOnBreakthroughCooldown()) return 0;

        long currentTime = player != null ? player.level().getGameTime() : 0;
        return getBreakthroughCooldownTicks() - (currentTime - lastBreakthroughAttempt);
    }

    // === Utility Methods ===

    /**
     * Get all available breakthrough options
     * @return List of possible breakthrough realms with types
     */
    public List<BreakthroughOption> getAvailableBreakthroughs() {
        if (player == null || currentRealm == null) return Collections.emptyList();

        ICultivationData cultivationData = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivationData == null) return Collections.emptyList();

        List<BreakthroughOption> options = new ArrayList<>();

        CultivationRealm minor = currentRealm.getMinorBreakthrough(player, cultivationData);
        if (minor != null) {
            options.add(new BreakthroughOption(BreakthroughType.MINOR, minor,
                calculateBreakthroughSuccessChance(BreakthroughType.MINOR, minor)));
        }

        CultivationRealm major = currentRealm.getMajorBreakthrough(player, cultivationData);
        if (major != null) {
            options.add(new BreakthroughOption(BreakthroughType.SPECIAL, major,
                calculateBreakthroughSuccessChance(BreakthroughType.SPECIAL, major)));
        }

        CultivationRealm special = currentRealm.getSpecialBreakthrough(player, cultivationData);
        if (major != null) {
            options.add(new BreakthroughOption(BreakthroughType.SPECIAL, major,
                calculateBreakthroughSuccessChance(BreakthroughType.SPECIAL, major)));
        }

        CultivationRealm boundary = currentRealm.getBoundaryBreakthrough(player, cultivationData);
        if (boundary != null) {
            options.add(new BreakthroughOption(BreakthroughType.BOUNDARY, boundary,
                calculateBreakthroughSuccessChance(BreakthroughType.BOUNDARY, boundary)));
        }

        return options;
    }

    /**
     * Reset breakthrough attempts (for testing or special items)
     */
    public void resetBreakthroughAttempts() {
        this.breakthroughAttempts = 0;
        this.breakthroughCooldown = false;
    }

    // === Tick Method ===

    public void tick() {
        if (player == null) return;

        timeInCurrentRealm++;
        totalCultivationTime++;

        // Apply realm modifiers periodically
        if (timeInCurrentRealm % 100 == 0) { // Every 5 seconds
            ICultivationData cultivationData = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationData != null) {
                currentRealm.applyRealmModifiers(player, cultivationData);
            }
        }
    }

    // === Serialization ===

    public void deserializeNBT(CompoundTag tag) {
        String realmId = tag.getString("realm_id");
        this.currentRealm = ICModCultivationRealms.get(realmId);

        if (tag.contains("previous_realm_id")) {
            String prevRealmId = tag.getString("previous_realm_id");
            this.previousRealm = ICModCultivationRealms.get(prevRealmId);
        }

        this.breakthroughAttempts = tag.getLong("breakthrough_attempts");
        this.lastBreakthroughAttempt = tag.getLong("last_breakthrough_attempt");
        this.breakthroughCooldown = tag.getBoolean("breakthrough_cooldown");
        this.timeInCurrentRealm = tag.getLong("time_in_current_realm");
        this.totalCultivationTime = tag.getLong("total_cultivation_time");
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("realm_id", currentRealm.getId());

        if (previousRealm != null) {
            tag.putString("previous_realm_id", previousRealm.getId());
        }

        tag.putLong("breakthrough_attempts", breakthroughAttempts);
        tag.putLong("last_breakthrough_attempt", lastBreakthroughAttempt);
        tag.putBoolean("breakthrough_cooldown", breakthroughCooldown);
        tag.putLong("time_in_current_realm", timeInCurrentRealm);
        tag.putLong("total_cultivation_time", totalCultivationTime);

        return tag;
    }

    // === Codec ===

    public static final Codec<PlayerRealmAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("realm_id").forGetter(comp -> comp.currentRealm.getId()),
        Codec.STRING.optionalFieldOf("previous_realm_id").forGetter(comp ->
            comp.previousRealm != null ? Optional.of(comp.previousRealm.getId()) : Optional.empty()),
        Codec.LONG.optionalFieldOf("breakthrough_attempts", 0L).forGetter(comp -> comp.breakthroughAttempts),
        Codec.LONG.optionalFieldOf("time_in_current_realm", 0L).forGetter(comp -> comp.timeInCurrentRealm),
        Codec.LONG.optionalFieldOf("total_cultivation_time", 0L).forGetter(comp -> comp.totalCultivationTime)
    ).apply(instance, (realmId, prevRealmId, attempts, timeInRealm, totalTime) -> {
        PlayerRealmAttachment comp = new PlayerRealmAttachment();
        comp.setCurrentRealm(ICModCultivationRealms.get(realmId), false);
        prevRealmId.ifPresent(id -> comp.previousRealm = ICModCultivationRealms.get(id));
        comp.breakthroughAttempts = attempts;
        comp.timeInCurrentRealm = timeInRealm;
        comp.totalCultivationTime = totalTime;
        return comp;
    }));

    // === Helper Classes ===

    public enum BreakthroughType {
        MINOR,
        MAJOR,
        SPECIAL,
        BOUNDARY
    }

    public record BreakthroughResult(boolean success, String message, @Nullable CultivationRealm newRealm) {}

    public record BreakthroughOption(BreakthroughType type, CultivationRealm targetRealm, double successChance) {}
}
