package net.hyper.immortalcultivation.core.attachments;

import com.mojang.logging.LogUtils;
import net.hyper.immortalcultivation.core.cultivation.BonusSource;
import net.hyper.immortalcultivation.core.cultivation.CultivationRealmRegistry;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

@EventBusSubscriber
public class CultivationPlayerAttachment implements ICultivationData {

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    public static AttachmentType<CultivationPlayerAttachment> getAttachmentType() {return AttachmentRegistry.CULTIVATION.get();}
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("immortalcultivation", "cultivation");

    private static final long DEFAULT_MAX_CULTIVATION = 100L;
    private static final String DEFAULT_MORTAL_REALM_ID = "mortal";
    private static final long MILESTONE_THRESHOLD = 1000L;
    private static final long FOCUS_CHANGE_COOLDOWN = 24000L; // 20 minutes at 20 TPS
    private static final double DEFAULT_ACCUMULATION_RATE = 1.0;
    private static final double DEFAULT_ADDITIVE_BONUS = 0.0;
    private static final double DEFAULT_MULTIPLICATIVE_BONUS = 1.0;
    private static final double MAX_MEDITATION_MULTIPLIER = 3.0;
    private static final int MEDITATION_RAMP_UP_TIME = 1200; // 60 seconds at 20 TPS
    private static final int MEDITATION_POSE_UPDATE_INTERVAL = 20; // Update pose every second

    private CultivationType currentFocus = CultivationType.QI;
    private long lastFocusChangeTime = -FOCUS_CHANGE_COOLDOWN;

    private boolean isMeditating = false;
    private int meditationTicks = 0;
    private double currentMeditationMultiplier = 1.0;
    private int lastPoseUpdateTick = 0;

    public CultivationPlayerAttachment() {
        super();
        initializeDefaults();
    }

    // Composite object for cache
    public static class CultivationData {
        // Realm data
        CultivationRealm currentRealm, previousRealm;

        // Progress data
        long currentCultivation, maxCultivation;
        double cultivationFraction;

        // Rate data
        double accumulationRate, additiveBonus, multiplicativeBonus;

        // Statistics
        long totalCultivationEarned, lastMilestone;
        long timeInCurrentRealm, totalCultivationTime;

        // Constructor for easy initialization
        public CultivationData(CultivationRealm mortalRealm) {
            // Realm initialization
            this.currentRealm = mortalRealm;
            this.previousRealm = mortalRealm;

            // Progress initialization
            this.currentCultivation = 0L;
            this.maxCultivation = DEFAULT_MAX_CULTIVATION;
            this.cultivationFraction = 0.0;

            // Rate initialization
            double realmBaseCultivationRate = mortalRealm != null ?
                mortalRealm.getBaseCultivationRate() : DEFAULT_ACCUMULATION_RATE;
            this.accumulationRate = realmBaseCultivationRate;
            this.additiveBonus = DEFAULT_ADDITIVE_BONUS;
            this.multiplicativeBonus = DEFAULT_MULTIPLICATIVE_BONUS;

            // Statistics initialization
            this.totalCultivationEarned = 0L;
            this.lastMilestone = 0L;
            this.timeInCurrentRealm = 0L;
            this.totalCultivationTime = 0L;
        }
    }

    private final Map<CultivationType, CultivationData> cultivationData = new EnumMap<>(CultivationType.class);

    private void initializeDefaults() {
        CultivationRealm mortalRealm = CultivationRealmRegistry.get(DEFAULT_MORTAL_REALM_ID);

        for (CultivationType type : CultivationType.values()) {
            cultivationData.put(type, new CultivationData(mortalRealm));
        }
    }

    private CultivationData getOrCreateData(CultivationType type) {
        return cultivationData.computeIfAbsent(type,
            t -> new CultivationData(CultivationRealmRegistry.get(DEFAULT_MORTAL_REALM_ID)));
    }

    @Override
    public long getCurrentCultivation(CultivationType type) {return getOrCreateData(type).currentCultivation;}

    @Override
    public void setCurrentCultivation(CultivationType type, long value) {
        CultivationData data = getOrCreateData(type);
        data.currentCultivation = value;
    }

    @Override
    public long getMaxCultivation(CultivationType type) {return getOrCreateData(type).maxCultivation;}

    @Override
    public void setMaxCultivation(CultivationType type, long value) {
        CultivationData data = getOrCreateData(type);
        data.maxCultivation = value;
    }

    @Override
    public void addCultivation(CultivationType type, long amount, boolean respectMaximum) {
        if (amount <= 0) return;

        CultivationData data = getOrCreateData(type);
        long currentValue = getCurrentCultivation(type);
        long maxValue = getMaxCultivation(type);
        double currentFraction = data.cultivationFraction;
        double newFraction = currentFraction + amount;

        // Even the fractions matter
        long wholeCultivation = (long) Math.floor(newFraction);
        newFraction -= wholeCultivation;
        data.cultivationFraction = newFraction;

        if (wholeCultivation > 0) {
            long newTotal = currentValue + wholeCultivation;

            if (respectMaximum) {
                newTotal = Math.min(newTotal, maxValue);
            }

            setCurrentCultivation(type, newTotal);
        }
    }

    @Override
    public void removeCultivation(CultivationType type, long amount, boolean respectMinimum) {
        if (amount <= 0) return;

        CultivationData data = getOrCreateData(type);
        long currentValue = getCurrentCultivation(type);
        double currentFraction = data.cultivationFraction;
        double newFraction = currentFraction - amount;

        long wholeCultivationToSubtract = (long) Math.floor(newFraction);
        while (newFraction < 0.0 && (currentValue > 0 || !respectMinimum)) {
            newFraction += 1.0;
            wholeCultivationToSubtract++;
        }

        newFraction = Math.max(0.0, newFraction);
        data.cultivationFraction = newFraction;

        if (wholeCultivationToSubtract > 0) {
            long newTotal = currentValue - wholeCultivationToSubtract;

            if (respectMinimum) {
                newTotal = Math.max(newTotal, 0);
            }

            setCurrentCultivation(type, newTotal);
        }
    }

    @Override
    public double getProgress(CultivationType type) {
        CultivationData data = getOrCreateData(type);

        if (data.maxCultivation <= 0) {return  0.0;}

        return ((double) data.currentCultivation / data.maxCultivation) * 100.0;
    }

    @Override
    public CultivationRealm getPreviousRealm(CultivationType type) {
        CultivationData data = getOrCreateData(type);

        return data.previousRealm;
    }

    @Override
    public CultivationRealm getCurrentRealm(CultivationType type) {
        CultivationData data = getOrCreateData(type);

        return data.currentRealm;
    }

    @Override
    public void setCurrentRealm(CultivationType type, CultivationRealm realm, @Nullable Player player, boolean silent) {
        CultivationData data = getOrCreateData(type);
        data.previousRealm = data.currentRealm;
        data.currentRealm = realm;
        data.maxCultivation = realm.getBaseMaxCultivation();
        data.accumulationRate = realm.getBaseCultivationRate();
        data.timeInCurrentRealm = 0;

        if (!silent && player != null) {
            Component message = Component.literal("You have advanced to ")
                .append(Component.literal(realm.getDisplayName()).withStyle(ChatFormatting.GOLD))
                .append(" in " + type.getDisplayName() + " cultivation!")
                .withStyle(ChatFormatting.GREEN);
            player.sendSystemMessage(message);
        }
    }

    @Override
    public double getBaseCultivationRate(CultivationType type) {return getOrCreateData(type).accumulationRate;}

    @Override
    public double getEffectiveCultivationRate(CultivationType type) {
        CultivationRealm realm = getCurrentRealm(type);

        double baseRate = realm.getBaseCultivationRate();
        double focusMultiplier = (type == currentFocus) ? 2.0 : 1.0;
        double meditationMultiplier = getMeditationMultiplier();

        return baseRate * focusMultiplier * meditationMultiplier;
    }

    @Override
    public double getRegenFraction(CultivationType type) {return getOrCreateData(type).cultivationFraction;}

    @Override
    public void setRegenFraction(CultivationType type, double fraction) {
        CultivationData data = getOrCreateData(type);

        data.cultivationFraction = Math.max(0.0, Math.min(0.999, fraction));
    }

    @Override
    public void addRegenFraction(CultivationType type, double amount) {
        CultivationData data = getOrCreateData(type);

        double currentFraction = data.cultivationFraction;
        double newFraction = currentFraction + amount;

        long wholeCultivation = (long) Math.floor(newFraction);
        newFraction -= wholeCultivation;

        data.cultivationFraction = newFraction;

        if (wholeCultivation > 0) {
            long currentValue = getCurrentCultivation(type);
            long maxValue = getMaxCultivation(type);
            long newTotal = Math.min(currentValue + wholeCultivation, maxValue);

            setCurrentCultivation(type, newTotal);
        }
    }

    private BonusSource getMilestoneSourceForType(CultivationType type) {
        return switch (type) {
            case QI -> BonusSource.QI_MILESTONES;
            case BODY -> BonusSource.BODY_MILESTONES;
        };
    }

    private void checkMilestone(CultivationType type, long oldValue, long newValue) {
        CultivationData data = getOrCreateData(type);
        long lastMilestoneValue = data.lastMilestone;
        long currentMilestone = (newValue / MILESTONE_THRESHOLD) * MILESTONE_THRESHOLD;

        if (currentMilestone > lastMilestoneValue) {
            data.lastMilestone = currentMilestone;

            long earned = data.totalCultivationEarned;
            data.totalCultivationEarned = earned + (newValue - oldValue);

            onMilestoneReached(type, currentMilestone);
        }
    }

    private void onMilestoneReached(CultivationType type, long milestone) {
        // TODO: Add for stats attachment
    }

    public boolean toggleFocus() {
        CultivationType newFocus = (currentFocus == CultivationType.QI) ? CultivationType.BODY : CultivationType.QI;
        return setCurrentFocus(newFocus);
    }

    public CultivationType getCurrentFocus() {return currentFocus;}

    public boolean setCurrentFocus(CultivationType focus) {
        long currentTime = System.currentTimeMillis() / 50;
        CultivationType oldFocus = currentFocus;
        currentFocus = focus;
        lastFocusChangeTime = currentTime;

        // TODO: Check this logic
        if (currentTime - lastFocusChangeTime < FOCUS_CHANGE_COOLDOWN) {return false;}

        return true;
    }

    public boolean isFocusChangeOnCooldown() {
        long currentTime = System.currentTimeMillis() / 50;

        return (currentTime - lastFocusChangeTime) < FOCUS_CHANGE_COOLDOWN;
    }

    public long getFocusChangeCooldown() {
        long currentTime = System.currentTimeMillis() / 50;
        long elapsed = currentTime - lastFocusChangeTime;

        return Math.max(0, FOCUS_CHANGE_COOLDOWN - elapsed);
    }

    public boolean toggleMeditation() {
        isMeditating = !isMeditating;

        if (isMeditating) {
            startMeditation();
        } else {
            stopMeditation();
        }

        return isMeditating;
    }

    private void startMeditation() {
        meditationTicks = 0;
        currentMeditationMultiplier = 1.0;
        lastPoseUpdateTick = 0;

        if (player instanceof ServerPlayer serverPlayer) {serverPlayer.setPose(Pose.SITTING);}
    }

    private void stopMeditation() {
        meditationTicks = 0;
        currentMeditationMultiplier = 1.0;

        if (player instanceof ServerPlayer serverPlayer) {serverPlayer.setPose(Pose.STANDING);}
    }

    public boolean isMeditating() {return isMeditating;}

    public double getMeditationMultiplier() {return isMeditating ? currentMeditationMultiplier : 1.0;}

    public int getMeditationTicks() {return meditationTicks;}

    public double getMeditationProgress() {
        if (!isMeditating || MEDITATION_RAMP_UP_TIME <= 0) {return 0.0;}

        return Math.min(1.0, (double) meditationTicks / MEDITATION_RAMP_UP_TIME);
    }

    private void processMeditationTick() {
        if (!isMeditating) {return;}

        meditationTicks++;

        updateMeditationMultiplier();
        maintainMeditationPose();
        checkMeditationInterruption();
    }

    private void updateMeditationMultiplier() {
        double progress = getMeditationProgress();
        double smoothProgress = 1.0 - Math.cos(progress * Math.PI / 2.0);

        currentMeditationMultiplier = 1.0 + (smoothProgress * (MAX_MEDITATION_MULTIPLIER - 1.0));
        currentMeditationMultiplier = Math.min(currentMeditationMultiplier, MAX_MEDITATION_MULTIPLIER);
    }

    private void maintainMeditationPose(@Nullable ServerPlayer serverPlayer) {
        if (meditationTicks - lastPoseUpdateTick >= MEDITATION_POSE_UPDATE_INTERVAL) {
            if (serverPlayer.getPose() != Pose.SITTING) {serverPlayer.setPose(Pose.SITTING);}

            lastPoseUpdateTick = meditationTicks;
        }
    }

    private void checkMeditationInterruption(@Nullable ServerPlayer serverPlayer) {
        boolean shouldInterrupt = serverPlayer.getDeltaMovement().lengthSqr() > 0.001;

        if (serverPlayer.getPose() != Pose.SITTING) {shouldInterrupt = true;}

        if (shouldInterrupt) {
            isMeditating = false;
            stopMeditation();

            serverPlayer.sendSystemMessage(Component.literal("Meditation was interrupted!")
                .withStyle(ChatFormatting.YELLOW));
        }
    }
}