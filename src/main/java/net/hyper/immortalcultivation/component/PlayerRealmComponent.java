package net.hyper.immortalcultivation.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.cultivation.PlayerQiData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachments;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PlayerRealmComponent {
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

    public PlayerRealmComponent() {
        this.player = null;
        // Set default starting realm
        this.currentRealm = ICModCultivationRealms.QI_CONDENSATION_L1;
    }

    public PlayerRealmComponent(Player player) {
        this.player = player;
        this.currentRealm = ICModCultivationRealms.QI_CONDENSATION_L1;
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
            PlayerQiData qiData = player.getData(ICModAttachments.PLAYER_QI.get());

            // Call exit event on old realm
            if (oldRealm != null) {
                oldRealm.onRealmExit(player, qiData, realm);
            }

            // Call advancement event on new realm
            realm.onRealmAdvancement(player, qiData, oldRealm);

            // Sync changes to client
            if (qiData instanceof PlayerQiComponent qiComponent) {
                qiComponent.forceSync();
            }

            // Apply new realm modifiers
            realm.applyRealmModifiers(player, qiData);
        }
    }

    // === Breakthrough Logic ===

    /**
     * Check if player can attempt breakthrough
     * @return true if breakthrough attempt is possible
     */
    public boolean canAttemptBreakthrough() {
        if (player == null || currentRealm == null) return false;

        PlayerQiData qiData = player.getData(ICModAttachments.PLAYER_QI.get());
        if (qiData == null) return false;

        // Check cooldown
        if (breakthroughCooldown) {
            long currentTime = player.level().getGameTime();
            if (currentTime - lastBreakthroughAttempt < getBreakthroughCooldownTicks()) {
                return false;
            } else {
                breakthroughCooldown = false;
            }
        }

        return currentRealm.canBreakthrough(player, qiData);
    }

    /**
     * Check breakthrough requirements without cooldown consideration
     * @return true if player meets breakthrough requirements
     */
    public boolean meetsBreakthroughRequirements() {
        if (player == null || currentRealm == null) return false;

        PlayerQiData qiData = player.getData(ICModAttachments.PLAYER_QI.get());
        if (qiData == null) return false;

        return currentRealm.canBreakthrough(player, qiData);
    }

    /**
     * Attempt breakthrough to next realm
     * @return BreakthroughResult indicating success/failure and details
     */
    public BreakthroughResult attemptBreakthrough() {
        return attemptBreakthrough(BreakthroughType.STANDARD);
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

        PlayerQiData qiData = player.getData(ICModAttachments.PLAYER_QI.get());
        if (qiData == null) {
            return new BreakthroughResult(false, "No Qi data found", null);
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
            case STANDARD -> currentRealm.getStandardBreakthrough(player, qiData);
            case SPECIAL -> currentRealm.getSpecialBreakthrough(player, qiData);
            case BOUNDARY -> currentRealm.getBoundaryBreakthrough(player, qiData);
            case BEST -> currentRealm.getBestBreakthrough(player, qiData);
        };

        if (targetRealm == null) {
            return new BreakthroughResult(false, "No valid breakthrough path found", null);
        }

        // Calculate success chance (could be modified by realm, items, etc.)
        double successChance = calculateBreakthroughSuccessChance(type, targetRealm);
        boolean success = player.level().random.nextDouble() < successChance;

        if (success) {
            // Consume breakthrough cost
            long cost = currentRealm.getBreakthroughQiCost();
            if (!qiData.consumeQi(cost)) {
                return new BreakthroughResult(false, "Insufficient Qi for breakthrough", null);
            }

            // Advance to new realm
            setCurrentRealm(targetRealm);

            return new BreakthroughResult(true,
                "Successfully advanced to " + targetRealm.getDisplayName(), targetRealm);
        } else {
            // Failed breakthrough - apply cooldown and possibly consume some Qi
            breakthroughCooldown = true;
            long failureCost = currentRealm.getBreakthroughQiCost() / 4; // 25% cost on failure
            qiData.consumeQi(failureCost);

            return new BreakthroughResult(false,
                "Breakthrough failed. Qi slightly depleted.", null);
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
            case STANDARD -> 0.95; // 95% for standard
            case SPECIAL -> 0.70;  // 70% for special
            case BOUNDARY -> 0.50; // 50% for boundary
            case BEST -> 0.85;     // 85% for best choice
        };

        // Modify based on current Qi percentage
        PlayerQiData qiData = player.getData(ICModAttachments.PLAYER_QI.get());
        double qiPercentage = qiData.getQiPercentage();
        double qiBonus = (qiPercentage - 0.8) * 0.5; // Bonus for having Qi above 80%

        // Modify based on previous attempts (diminishing returns)
        double attemptPenalty = Math.min(breakthroughAttempts * 0.02, 0.10); // Max 10% penalty

        return Math.max(0.10, Math.min(1.0, baseChance + qiBonus - attemptPenalty));
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

        PlayerQiData qiData = player.getData(ICModAttachments.PLAYER_QI.get());
        if (qiData == null) return Collections.emptyList();

        List<BreakthroughOption> options = new ArrayList<>();

        CultivationRealm standard = currentRealm.getStandardBreakthrough(player, qiData);
        if (standard != null) {
            options.add(new BreakthroughOption(BreakthroughType.STANDARD, standard,
                calculateBreakthroughSuccessChance(BreakthroughType.STANDARD, standard)));
        }

        CultivationRealm special = currentRealm.getSpecialBreakthrough(player, qiData);
        if (special != null) {
            options.add(new BreakthroughOption(BreakthroughType.SPECIAL, special,
                calculateBreakthroughSuccessChance(BreakthroughType.SPECIAL, special)));
        }

        CultivationRealm boundary = currentRealm.getBoundaryBreakthrough(player, qiData);
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
            PlayerQiData qiData = player.getData(ICModAttachments.PLAYER_QI.get());
            if (qiData != null) {
                currentRealm.applyRealmModifiers(player, qiData);
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

    public static final Codec<PlayerRealmComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("realm_id").forGetter(comp -> comp.currentRealm.getId()),
        Codec.STRING.optionalFieldOf("previous_realm_id").forGetter(comp ->
            comp.previousRealm != null ? Optional.of(comp.previousRealm.getId()) : Optional.empty()),
        Codec.LONG.optionalFieldOf("breakthrough_attempts", 0L).forGetter(comp -> comp.breakthroughAttempts),
        Codec.LONG.optionalFieldOf("time_in_current_realm", 0L).forGetter(comp -> comp.timeInCurrentRealm),
        Codec.LONG.optionalFieldOf("total_cultivation_time", 0L).forGetter(comp -> comp.totalCultivationTime)
    ).apply(instance, (realmId, prevRealmId, attempts, timeInRealm, totalTime) -> {
        PlayerRealmComponent comp = new PlayerRealmComponent();
        comp.setCurrentRealm(ICModCultivationRealms.get(realmId), false);
        prevRealmId.ifPresent(id -> comp.previousRealm = ICModCultivationRealms.get(id));
        comp.breakthroughAttempts = attempts;
        comp.timeInCurrentRealm = timeInRealm;
        comp.totalCultivationTime = totalTime;
        return comp;
    }));

    // === Helper Classes ===

    public enum BreakthroughType {
        STANDARD,   // Normal advancement
        SPECIAL,    // Special conditions met
        BOUNDARY,   // Major stage crossing
        BEST        // Best available option
    }

    public record BreakthroughResult(boolean success, String message, @Nullable CultivationRealm newRealm) {}

    public record BreakthroughOption(BreakthroughType type, CultivationRealm targetRealm, double successChance) {}
}
