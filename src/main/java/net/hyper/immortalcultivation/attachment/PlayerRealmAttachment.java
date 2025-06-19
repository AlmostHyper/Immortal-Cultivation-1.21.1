package net.hyper.immortalcultivation.attachment;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IBodyCultivationData;
import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
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

    // === Cultivation Type Enum === //
    public enum CultivationType {
        QI("Qi"),
        BODY("Body");

        private final String displayName;

        CultivationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // === Variables === //

    @Nullable
    private Player player;

    // Separate realms for Qi and Body cultivation
    private CultivationRealm currentQiRealm;
    private CultivationRealm currentBodyRealm;

    @Nullable
    private CultivationRealm previousQiRealm;
    @Nullable
    private CultivationRealm previousBodyRealm;

    // Separate breakthrough tracking for each cultivation type
    private long qiBreakthroughAttempts = 0;
    private long bodyBreakthroughAttempts = 0;

    private long lastQiBreakthroughAttempt = 0;
    private long lastBodyBreakthroughAttempt = 0;

    private boolean qiBreakthroughCooldown = false;
    private boolean bodyBreakthroughCooldown = false;

    // Time tracking for each cultivation path
    private long timeInCurrentQiRealm = 0;
    private long timeInCurrentBodyRealm = 0;

    private long totalQiCultivationTime = 0;
    private long totalBodyCultivationTime = 0;

    // === Constructor(s) === //

    public PlayerRealmAttachment() {
        this.player = null;
        // Set default starting realms for both paths
        this.currentQiRealm = ICModCultivationRealms.MORTAL;
        this.currentBodyRealm = ICModCultivationRealms.MORTAL;
    }

    public PlayerRealmAttachment(Player player) {
        this.player = player;
        this.currentQiRealm = ICModCultivationRealms.MORTAL;
        this.currentBodyRealm = ICModCultivationRealms.MORTAL;
    }

    // === Player === //

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    // === Realm Getters === //

    /**
     * Get the current realm for a specific cultivation type
     */
    public CultivationRealm getCurrentRealm(CultivationType type) {
        return type == CultivationType.QI ? currentQiRealm : currentBodyRealm;
    }

    /**
     * Get the previous realm for a specific cultivation type
     */
    @Nullable
    public CultivationRealm getPreviousRealm(CultivationType type) {
        return type == CultivationType.QI ? previousQiRealm : previousBodyRealm;
    }

    /**
     * Get current Qi cultivation realm
     */
    public CultivationRealm getCurrentQiRealm() {
        return currentQiRealm;
    }

    /**
     * Get current Body cultivation realm
     */
    public CultivationRealm getCurrentBodyRealm() {
        return currentBodyRealm;
    }

    /**
     * Get the highest realm between Qi and Body cultivation
     * This can be used for determining overall cultivation level
     */
    public CultivationRealm getHighestRealm() {
        return currentQiRealm.getRealmLevel() >= currentBodyRealm.getRealmLevel()
            ? currentQiRealm : currentBodyRealm;
    }

    // === Realm Setters === //

    /**
     * Set the current realm for a specific cultivation type
     */
    public void setCurrentRealm(CultivationType type, CultivationRealm realm) {
        setCurrentRealm(type, realm, true);
    }

    /**
     * Set the current realm for a specific cultivation type with event control
     */
    public void setCurrentRealm(CultivationType type, CultivationRealm realm, boolean triggerEvents) {
        if (realm == null) {
            throw new IllegalArgumentException("Cultivation realm cannot be null");
        }

        CultivationRealm oldRealm;

        if (type == CultivationType.QI) {
            oldRealm = this.currentQiRealm;
            this.previousQiRealm = oldRealm;
            this.currentQiRealm = realm;
            this.timeInCurrentQiRealm = 0;
        } else {
            oldRealm = this.currentBodyRealm;
            this.previousBodyRealm = oldRealm;
            this.currentBodyRealm = realm;
            this.timeInCurrentBodyRealm = 0;
        }

        if (triggerEvents && player != null) {
            // Get the appropriate cultivation data based on type
            if (type == CultivationType.QI) {
                IQiCultivationData qiData = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());

                // Call exit event on old realm
                if (oldRealm != null) {
                    oldRealm.onRealmExit(player, qiData, realm);
                }

                // Call advancement event on new realm
                realm.onRealmAdvancement(player, qiData, oldRealm);

                // Apply new realm modifiers
                realm.applyRealmModifiers(player, qiData);

                // Sync changes
                if (qiData instanceof PlayerQiCultivationAttachment qiAttachment) {
                    qiAttachment.forceSync();
                }
            } else {
                IBodyCultivationData bodyData = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());

                // Call exit event on old realm
                if (oldRealm != null) {
                    oldRealm.onRealmExit(player, bodyData, realm);
                }

                // Call advancement event on new realm
                realm.onRealmAdvancement(player, bodyData, oldRealm);

                // Apply new realm modifiers
                realm.applyRealmModifiers(player, bodyData);

                // Sync changes
                if (bodyData instanceof PlayerBodyCultivationAttachment bodyAttachment) {
                    bodyAttachment.forceSync();
                }
            }
        }
    }

    // === Breakthrough Logic === //

    /**
     * Check if player can attempt breakthrough for a specific cultivation type
     */
    public boolean canAttemptBreakthrough(CultivationType type) {
        if (player == null) return false;

        // Check cooldown
        boolean onCooldown = type == CultivationType.QI ? qiBreakthroughCooldown : bodyBreakthroughCooldown;
        long lastAttempt = type == CultivationType.QI ? lastQiBreakthroughAttempt : lastBodyBreakthroughAttempt;

        if (onCooldown) {
            long currentTime = player.level().getGameTime();
            if (currentTime - lastAttempt < getBreakthroughCooldownTicks()) {
                return false;
            } else {
                // Reset cooldown
                if (type == CultivationType.QI) {
                    qiBreakthroughCooldown = false;
                } else {
                    bodyBreakthroughCooldown = false;
                }
            }
        }

        // Check realm requirements
        CultivationRealm currentRealm = getCurrentRealm(type);
        if (currentRealm == null) return false;

        // Get appropriate cultivation data
        if (type == CultivationType.QI) {
            IQiCultivationData qiData = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            return qiData != null && currentRealm.canBreakthrough(player, qiData);
        } else {
            IBodyCultivationData bodyData = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            return bodyData != null && currentRealm.canBreakthrough(player, bodyData);
        }
    }

    /**
     * Check breakthrough requirements without cooldown consideration
     */
    public boolean meetsBreakthroughRequirements(CultivationType type) {
        if (player == null) return false;

        CultivationRealm currentRealm = getCurrentRealm(type);
        if (currentRealm == null) return false;

        if (type == CultivationType.QI) {
            IQiCultivationData qiData = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            return qiData != null && currentRealm.canBreakthrough(player, qiData);
        } else {
            IBodyCultivationData bodyData = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            return bodyData != null && currentRealm.canBreakthrough(player, bodyData);
        }
    }

    /**
     * Attempt breakthrough for a specific cultivation type
     */
    public BreakthroughResult attemptBreakthrough(CultivationType cultivationType) {
        return attemptBreakthrough(cultivationType, BreakthroughType.MINOR);
    }

    /**
     * Attempt specific type of breakthrough for a cultivation path
     */
    public BreakthroughResult attemptBreakthrough(CultivationType cultivationType, BreakthroughType breakthroughType) {
        // Enhanced debugging
        ImmortalCultivation.LOGGER.info("Attempting {} breakthrough - Player: {}, CurrentRealm: {}",
            cultivationType.getDisplayName(),
            player != null ? player.getName().getString() : "NULL",
            getCurrentRealm(cultivationType) != null ? getCurrentRealm(cultivationType).getDisplayName() : "NULL");

        if (player == null) {
            ImmortalCultivation.LOGGER.error("Player is null in attemptBreakthrough!");
            return new BreakthroughResult(false, "Player reference is null", null);
        }

        CultivationRealm currentRealm = getCurrentRealm(cultivationType);
        if (currentRealm == null) {
            ImmortalCultivation.LOGGER.error("Current {} realm is null!", cultivationType.getDisplayName());
            return new BreakthroughResult(false, "Current realm is null", null);
        }

        // Get appropriate cultivation data
        if (cultivationType == CultivationType.QI) {
            IQiCultivationData qiData = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiData == null) {
                ImmortalCultivation.LOGGER.error("Qi cultivation data is null for player: {}", player.getName().getString());
                return new BreakthroughResult(false, "No Qi cultivation data found", null);
            }
            return performQiBreakthrough(currentRealm, qiData, breakthroughType);
        } else {
            IBodyCultivationData bodyData = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyData == null) {
                ImmortalCultivation.LOGGER.error("Body cultivation data is null for player: {}", player.getName().getString());
                return new BreakthroughResult(false, "No Body cultivation data found", null);
            }
            return performBodyBreakthrough(currentRealm, bodyData, breakthroughType);
        }
    }

    /**
     * Perform Qi cultivation breakthrough
     */
    private BreakthroughResult performQiBreakthrough(CultivationRealm currentRealm, IQiCultivationData qiData, BreakthroughType type) {
        // Check if attempt is possible
        if (!canAttemptBreakthrough(CultivationType.QI)) {
            if (qiBreakthroughCooldown) {
                long remainingTicks = getBreakthroughCooldownTicks() -
                    (player.level().getGameTime() - lastQiBreakthroughAttempt);
                return new BreakthroughResult(false,
                    "Qi breakthrough cooldown: " + (remainingTicks / 20) + " seconds remaining", null);
            } else {
                return new BreakthroughResult(false, "Qi breakthrough requirements not met", null);
            }
        }

        // Record attempt
        qiBreakthroughAttempts++;
        lastQiBreakthroughAttempt = player.level().getGameTime();
        qiBreakthroughCooldown = true;

        // Determine target realm based on type
        CultivationRealm targetRealm = switch (type) {
            case MINOR -> currentRealm.getMinorBreakthrough(player, qiData);
            case MAJOR -> currentRealm.getMajorBreakthrough(player, qiData);
            case SPECIAL -> currentRealm.getSpecialBreakthrough(player, qiData);
            case BOUNDARY -> currentRealm.getBoundaryBreakthrough(player, qiData);
        };

        if (targetRealm == null) {
            ImmortalCultivation.LOGGER.warn("No valid Qi breakthrough path found for type: {}", type);
            return new BreakthroughResult(false, "No valid Qi breakthrough path found", null);
        }

        // Calculate success chance
        double successChance = calculateBreakthroughSuccessChance(CultivationType.QI, type, targetRealm);
        boolean success = player.level().random.nextDouble() < successChance;

        ImmortalCultivation.LOGGER.info("Qi breakthrough attempt - Success chance: {}%, Result: {}",
            String.format("%.1f", successChance * 100), success ? "SUCCESS" : "FAILURE");

        if (success) {
            // Advance to new realm
            setCurrentRealm(CultivationType.QI, targetRealm);

            return new BreakthroughResult(true,
                "Successfully advanced Qi cultivation to " + targetRealm.getDisplayName(), targetRealm);
        } else {
            return new BreakthroughResult(false,
                "Qi breakthrough failed.", null);
        }
    }

    /**
     * Perform Body cultivation breakthrough
     */
    private BreakthroughResult performBodyBreakthrough(CultivationRealm currentRealm, IBodyCultivationData bodyData, BreakthroughType type) {
        // Check if attempt is possible
        if (!canAttemptBreakthrough(CultivationType.BODY)) {
            if (bodyBreakthroughCooldown) {
                long remainingTicks = getBreakthroughCooldownTicks() -
                    (player.level().getGameTime() - lastBodyBreakthroughAttempt);
                return new BreakthroughResult(false,
                    "Body breakthrough cooldown: " + (remainingTicks / 20) + " seconds remaining", null);
            } else {
                return new BreakthroughResult(false, "Body breakthrough requirements not met", null);
            }
        }

        // Record attempt
        bodyBreakthroughAttempts++;
        lastBodyBreakthroughAttempt = player.level().getGameTime();
        bodyBreakthroughCooldown = true;

        // Determine target realm based on type
        CultivationRealm targetRealm = switch (type) {
            case MINOR -> currentRealm.getMinorBreakthrough(player, bodyData);
            case MAJOR -> currentRealm.getMajorBreakthrough(player, bodyData);
            case SPECIAL -> currentRealm.getSpecialBreakthrough(player, bodyData);
            case BOUNDARY -> currentRealm.getBoundaryBreakthrough(player, bodyData);
        };

        if (targetRealm == null) {
            ImmortalCultivation.LOGGER.warn("No valid Body breakthrough path found for type: {}", type);
            return new BreakthroughResult(false, "No valid Body breakthrough path found", null);
        }

        // Calculate success chance
        double successChance = calculateBreakthroughSuccessChance(CultivationType.BODY, type, targetRealm);
        boolean success = player.level().random.nextDouble() < successChance;

        ImmortalCultivation.LOGGER.info("Body breakthrough attempt - Success chance: {}%, Result: {}",
            String.format("%.1f", successChance * 100), success ? "SUCCESS" : "FAILURE");

        if (success) {
            // Advance to new realm
            setCurrentRealm(CultivationType.BODY, targetRealm);

            return new BreakthroughResult(true,
                "Successfully advanced Body cultivation to " + targetRealm.getDisplayName(), targetRealm);
        } else {
            return new BreakthroughResult(false,
                "Body breakthrough failed.", null);
        }
    }

    /**
     * Calculate success chance for breakthrough attempt
     */
    private double calculateBreakthroughSuccessChance(CultivationType cultivationType, BreakthroughType type, CultivationRealm targetRealm) {
        double baseChance = switch (type) {
            case MINOR -> 0.5;
            case MAJOR -> 0.3;
            case SPECIAL -> 0.2;
            case BOUNDARY -> 0.1;
        };

        // Get cultivation percentage based on type
        double cultivationPercentage;
        if (cultivationType == CultivationType.QI) {
            IQiCultivationData qiData = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            cultivationPercentage = qiData != null ? qiData.getQiCultivationPercentage() : 0.0;
        } else {
            IBodyCultivationData bodyData = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            cultivationPercentage = bodyData != null ? bodyData.getBodyCultivationPercentage() : 0.0;
        }

        // Bonus for high cultivation percentage (only if above 80%)
        double cultivationBonus = Math.max(0, (cultivationPercentage - 0.8) * 0.20);

        // Penalty based on previous attempts (diminishing returns)
        long attempts = cultivationType == CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts;
        double attemptPenalty = Math.min(attempts * 0.05, 0.3);

        return Math.max(0.10, Math.min(1.0, baseChance + cultivationBonus - attemptPenalty));
    }

    /**
     * Get breakthrough cooldown in ticks
     */
    private long getBreakthroughCooldownTicks() {
        return 20 * 30; // 30 seconds default
    }

    // === Realm Progression Tracking ===

    public long getBreakthroughAttempts(CultivationType type) {
        return type == CultivationType.QI ? qiBreakthroughAttempts : bodyBreakthroughAttempts;
    }

    public long getTimeInCurrentRealm(CultivationType type) {
        return type == CultivationType.QI ? timeInCurrentQiRealm : timeInCurrentBodyRealm;
    }

    public long getTotalCultivationTime(CultivationType type) {
        return type == CultivationType.QI ? totalQiCultivationTime : totalBodyCultivationTime;
    }

    public boolean isOnBreakthroughCooldown(CultivationType type) {
        boolean onCooldown = type == CultivationType.QI ? qiBreakthroughCooldown : bodyBreakthroughCooldown;
        if (!onCooldown) return false;

        long lastAttempt = type == CultivationType.QI ? lastQiBreakthroughAttempt : lastBodyBreakthroughAttempt;
        long currentTime = player != null ? player.level().getGameTime() : 0;

        return currentTime - lastAttempt < getBreakthroughCooldownTicks();
    }

    public long getRemainingCooldownTicks(CultivationType type) {
        if (!isOnBreakthroughCooldown(type)) return 0;

        long lastAttempt = type == CultivationType.QI ? lastQiBreakthroughAttempt : lastBodyBreakthroughAttempt;
        long currentTime = player != null ? player.level().getGameTime() : 0;

        return getBreakthroughCooldownTicks() - (currentTime - lastAttempt);
    }

    // === Utility Methods ===

    /**
     * Get all available breakthrough options for both cultivation types
     */
    public BreakthroughOptions getAllAvailableBreakthroughs() {
        List<BreakthroughOption> qiOptions = getAvailableBreakthroughs(CultivationType.QI);
        List<BreakthroughOption> bodyOptions = getAvailableBreakthroughs(CultivationType.BODY);

        return new BreakthroughOptions(qiOptions, bodyOptions);
    }

    /**
     * Get available breakthrough options for a specific cultivation type
     */
    public List<BreakthroughOption> getAvailableBreakthroughs(CultivationType type) {
        if (player == null) return Collections.emptyList();

        CultivationRealm currentRealm = getCurrentRealm(type);
        if (currentRealm == null) return Collections.emptyList();

        List<BreakthroughOption> options = new ArrayList<>();

        if (type == CultivationType.QI) {
            IQiCultivationData qiData = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiData == null) return Collections.emptyList();

            addBreakthroughOption(options, currentRealm.getMinorBreakthrough(player, qiData),
                BreakthroughType.MINOR, CultivationType.QI);
            addBreakthroughOption(options, currentRealm.getMajorBreakthrough(player, qiData),
                BreakthroughType.MAJOR, CultivationType.QI);
            addBreakthroughOption(options, currentRealm.getSpecialBreakthrough(player, qiData),
                BreakthroughType.SPECIAL, CultivationType.QI);
            addBreakthroughOption(options, currentRealm.getBoundaryBreakthrough(player, qiData),
                BreakthroughType.BOUNDARY, CultivationType.QI);
        } else {
            IBodyCultivationData bodyData = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyData == null) return Collections.emptyList();

            addBreakthroughOption(options, currentRealm.getMinorBreakthrough(player, bodyData),
                BreakthroughType.MINOR, CultivationType.BODY);
            addBreakthroughOption(options, currentRealm.getMajorBreakthrough(player, bodyData),
                BreakthroughType.MAJOR, CultivationType.BODY);
            addBreakthroughOption(options, currentRealm.getSpecialBreakthrough(player, bodyData),
                BreakthroughType.SPECIAL, CultivationType.BODY);
            addBreakthroughOption(options, currentRealm.getBoundaryBreakthrough(player, bodyData),
                BreakthroughType.BOUNDARY, CultivationType.BODY);
        }

        return options;
    }

    private void addBreakthroughOption(List<BreakthroughOption> options, CultivationRealm realm,
                                       BreakthroughType type, CultivationType cultivationType) {
        if (realm != null) {
            double successChance = calculateBreakthroughSuccessChance(cultivationType, type, realm);
            options.add(new BreakthroughOption(type, realm, successChance, cultivationType));
        }
    }

    /**
     * Reset breakthrough attempts for a specific cultivation type (for testing or special items)
     */
    public void resetBreakthroughAttempts(CultivationType type) {
        if (type == CultivationType.QI) {
            this.qiBreakthroughAttempts = 0;
            this.qiBreakthroughCooldown = false;
        } else {
            this.bodyBreakthroughAttempts = 0;
            this.bodyBreakthroughCooldown = false;
        }
    }

    /**
     * Reset all breakthrough attempts
     */
    public void resetAllBreakthroughAttempts() {
        resetBreakthroughAttempts(CultivationType.QI);
        resetBreakthroughAttempts(CultivationType.BODY);
    }

    // === Tick Method ===

    public void tick() {
        if (player == null) return;

        // Update time tracking for both cultivation paths
        timeInCurrentQiRealm++;
        timeInCurrentBodyRealm++;
        totalQiCultivationTime++;
        totalBodyCultivationTime++;

        // Apply realm modifiers periodically for both types
        if (timeInCurrentQiRealm % 100 == 0) { // Every 5 seconds
            IQiCultivationData qiData = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiData != null && currentQiRealm != null) {
                currentQiRealm.applyRealmModifiers(player, qiData);
            }
        }

        if (timeInCurrentBodyRealm % 100 == 0) { // Every 5 seconds
            IBodyCultivationData bodyData = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyData != null && currentBodyRealm != null) {
                currentBodyRealm.applyRealmModifiers(player, bodyData);
            }
        }
    }

    // === Serialization ===

    public void deserializeNBT(CompoundTag tag) {
        // Qi cultivation data
        String qiRealmId = tag.getString("qi_realm_id");
        this.currentQiRealm = ICModCultivationRealms.get(qiRealmId);

        if (tag.contains("previous_qi_realm_id")) {
            String prevQiRealmId = tag.getString("previous_qi_realm_id");
            this.previousQiRealm = ICModCultivationRealms.get(prevQiRealmId);
        }

        this.qiBreakthroughAttempts = tag.getLong("qi_breakthrough_attempts");
        this.lastQiBreakthroughAttempt = tag.getLong("last_qi_breakthrough_attempt");
        this.qiBreakthroughCooldown = tag.getBoolean("qi_breakthrough_cooldown");
        this.timeInCurrentQiRealm = tag.getLong("time_in_current_qi_realm");
        this.totalQiCultivationTime = tag.getLong("total_qi_cultivation_time");

        // Body cultivation data
        String bodyRealmId = tag.getString("body_realm_id");
        this.currentBodyRealm = ICModCultivationRealms.get(bodyRealmId);

        if (tag.contains("previous_body_realm_id")) {
            String prevBodyRealmId = tag.getString("previous_body_realm_id");
            this.previousBodyRealm = ICModCultivationRealms.get(prevBodyRealmId);
        }

        this.bodyBreakthroughAttempts = tag.getLong("body_breakthrough_attempts");
        this.lastBodyBreakthroughAttempt = tag.getLong("last_body_breakthrough_attempt");
        this.bodyBreakthroughCooldown = tag.getBoolean("body_breakthrough_cooldown");
        this.timeInCurrentBodyRealm = tag.getLong("time_in_current_body_realm");
        this.totalBodyCultivationTime = tag.getLong("total_body_cultivation_time");
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Qi cultivation data
        tag.putString("qi_realm_id", currentQiRealm.getId());
        if (previousQiRealm != null) {
            tag.putString("previous_qi_realm_id", previousQiRealm.getId());
        }
        tag.putLong("qi_breakthrough_attempts", qiBreakthroughAttempts);
        tag.putLong("last_qi_breakthrough_attempt", lastQiBreakthroughAttempt);
        tag.putBoolean("qi_breakthrough_cooldown", qiBreakthroughCooldown);
        tag.putLong("time_in_current_qi_realm", timeInCurrentQiRealm);
        tag.putLong("total_qi_cultivation_time", totalQiCultivationTime);

        // Body cultivation data
        tag.putString("body_realm_id", currentBodyRealm.getId());
        if (previousBodyRealm != null) {
            tag.putString("previous_body_realm_id", previousBodyRealm.getId());
        }
        tag.putLong("body_breakthrough_attempts", bodyBreakthroughAttempts);
        tag.putLong("last_body_breakthrough_attempt", lastBodyBreakthroughAttempt);
        tag.putBoolean("body_breakthrough_cooldown", bodyBreakthroughCooldown);
        tag.putLong("time_in_current_body_realm", timeInCurrentBodyRealm);
        tag.putLong("total_body_cultivation_time", totalBodyCultivationTime);

        return tag;
    }

    public void updateFromServer(long newRealm) {
        long oldRealm = this.getCurrentRealm(b);
        this.currentQiCultivation = newCultivation;
        this.lastSyncedCultivation = newCultivation;

        if (oldCultivation != newCultivation) {
            onQiCultivationChanged(oldCultivation, newCultivation);
        }
    }

    // === Codec ===

    public static final Codec<PlayerRealmAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        // Qi cultivation fields
        Codec.STRING.fieldOf("qi_realm_id").forGetter(comp -> comp.currentQiRealm.getId()),
        Codec.STRING.optionalFieldOf("previous_qi_realm_id").forGetter(comp ->
            comp.previousQiRealm != null ? Optional.of(comp.previousQiRealm.getId()) : Optional.empty()),
        Codec.LONG.optionalFieldOf("qi_breakthrough_attempts", 0L).forGetter(comp -> comp.qiBreakthroughAttempts),
        Codec.LONG.optionalFieldOf("time_in_current_qi_realm", 0L).forGetter(comp -> comp.timeInCurrentQiRealm),
        Codec.LONG.optionalFieldOf("total_qi_cultivation_time", 0L).forGetter(comp -> comp.totalQiCultivationTime),

        // Body cultivation fields
        Codec.STRING.fieldOf("body_realm_id").forGetter(comp -> comp.currentBodyRealm.getId()),
        Codec.STRING.optionalFieldOf("previous_body_realm_id").forGetter(comp ->
            comp.previousBodyRealm != null ? Optional.of(comp.previousBodyRealm.getId()) : Optional.empty()),
        Codec.LONG.optionalFieldOf("body_breakthrough_attempts", 0L).forGetter(comp -> comp.bodyBreakthroughAttempts),
        Codec.LONG.optionalFieldOf("time_in_current_body_realm", 0L).forGetter(comp -> comp.timeInCurrentBodyRealm),
        Codec.LONG.optionalFieldOf("total_body_cultivation_time", 0L).forGetter(comp -> comp.totalBodyCultivationTime)
    ).apply(instance, (qiRealmId, prevQiRealmId, qiAttempts, timeInQiRealm, totalQiTime,
                       bodyRealmId, prevBodyRealmId, bodyAttempts, timeInBodyRealm, totalBodyTime) -> {
        PlayerRealmAttachment comp = new PlayerRealmAttachment();

        // Set Qi cultivation data
        comp.currentQiRealm = ICModCultivationRealms.get(qiRealmId);
        prevQiRealmId.ifPresent(id -> comp.previousQiRealm = ICModCultivationRealms.get(id));
        comp.qiBreakthroughAttempts = qiAttempts;
        comp.timeInCurrentQiRealm = timeInQiRealm;
        comp.totalQiCultivationTime = totalQiTime;

        // Set Body cultivation data
        comp.currentBodyRealm = ICModCultivationRealms.get(bodyRealmId);
        prevBodyRealmId.ifPresent(id -> comp.previousBodyRealm = ICModCultivationRealms.get(id));
        comp.bodyBreakthroughAttempts = bodyAttempts;
        comp.timeInCurrentBodyRealm = timeInBodyRealm;
        comp.totalBodyCultivationTime = totalBodyTime;

        return comp;
    }));

    // === Helper Classes === //

    public enum BreakthroughType {
        MINOR,
        MAJOR,
        SPECIAL,
        BOUNDARY
    }

    public record BreakthroughResult(boolean success, String message, @Nullable CultivationRealm newRealm) {}

    public record BreakthroughOption(BreakthroughType type, CultivationRealm targetRealm,
                                     double successChance, CultivationType cultivationType) {}

    public record BreakthroughOptions(List<BreakthroughOption> qiOptions,
                                      List<BreakthroughOption> bodyOptions) {}
}
