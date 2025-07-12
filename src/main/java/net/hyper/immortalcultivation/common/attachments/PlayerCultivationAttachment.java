package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.*;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealmData;
import net.hyper.immortalcultivation.core.cultivation.realms.JsonCultivationRealm;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerCultivationSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class PlayerCultivationAttachment extends PlayerAttachment<PlayerCultivationAttachment> implements ICultivationData, ISyncable, ITickable {

    // === CONSTANTS === //

    private static final long DEFAULT_MAX_CULTIVATION = 100L;
    private static final String DEFAULT_MORTAL_REALM_ID = "mortal";
    private static final long MILESTONE_THRESHOLD = 1000L;
    private static final long FOCUS_CHANGE_COOLDOWN = 24000;

    // === INSTANCES === //

    @Nullable
    private transient Player player;

    // --- Cultivation Data ---
    private final Map<CultivationType, CultivationRealm> currentRealms = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, CultivationRealm> previousRealms = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> currentCultivation = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> maxCultivation = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Double> cultivationFraction = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Double> accumulationRate = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> totalCultivationEarned = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> lastMilestone = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Double> additiveBonus = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Double> multiplicativeBonus = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> timeInCurrentRealm = new EnumMap<>(CultivationType.class);
    private final Map<CultivationType, Long> totalCultivationTime = new EnumMap<>(CultivationType.class);

    // --- System Data ---
    private CultivationType currentFocus = CultivationType.QI;
    private long lastFocusChangeTime = -FOCUS_CHANGE_COOLDOWN;
    private transient boolean isDirty = false;
    private transient PlayerCultivationAttachment lastSyncedState = null;

    // === CONSTRUCTORS === //

    public PlayerCultivationAttachment() {
        initializeDefaults();
    }

    // === GETTERS & SETTERS === //

    public void setPlayer(Player player) { this.player = player; }

    @Nullable
    public Player getPlayerSafe() {
        return player;
    }

    @Override
    public long getBaseCultivation(CultivationType type) { return currentRealms.get(type).getBaseMaxCultivation(); }

    @Override
    public long getCurrentCultivation(CultivationType type) { return currentCultivation.get(type); }

    @Override
    public void setCurrentCultivation(CultivationType type, long value) {
        long oldValue = currentCultivation.get(type);
        long newValue = Math.max(0, Math.min(value, maxCultivation.get(type)));
        currentCultivation.put(type, newValue);
        if (oldValue != newValue) {
            ImmortalCultivation.LOGGER.debug("Cultivation changed for {}: {} {} -> {}",
                player != null ? player.getName().getString() : "unknown",
                type, oldValue, newValue);
            this.markDirty();
        }
    }

    @Override
    public long getMaxCultivation(CultivationType type) { return maxCultivation.get(type); }

    @Override
    public void setMaxCultivation(CultivationType type, long value) {
        long newMax = Math.max(1, value);
        if (newMax != maxCultivation.get(type)) {
            maxCultivation.put(type, newMax);
            if (currentCultivation.get(type) > newMax) {
                currentCultivation.put(type, newMax);
            }
            this.markDirty();
        }
    }

    @Override
    public double getProgress(CultivationType type) {
        long max = maxCultivation.get(type);
        return max > 0 ? (double) currentCultivation.get(type) / max : 0.0;
    }

    @Override
    public double getBaseCultivationRate(CultivationType type) { return currentRealms.get(type).getBaseCultivationRate(); }

    @Override
    public double getRegenFraction(CultivationType type) { return cultivationFraction.get(type); }

    @Override
    public void setRegenFraction(CultivationType type, double fraction) { cultivationFraction.put(type, fraction); }
    
    public double getAdditiveBonus(CultivationType type) {
        return this.additiveBonus.getOrDefault(type, 0.0);
    }

    public double getMultiplicativeBonus(CultivationType type) {
        return this.multiplicativeBonus.getOrDefault(type, 0.0);
    }

    public CultivationRealm getRealm(CultivationType type) {
        return currentRealms.get(type);
    }

    // Replace your existing setRealm method in PlayerCultivationAttachment with this updated version:

    public void setRealm(CultivationType type, CultivationRealm realm) {
        if (realm != null && !realm.equals(this.currentRealms.get(type))) {
            CultivationRealm previousRealm = this.currentRealms.get(type);

            // Update the realm
            this.previousRealms.put(type, previousRealm);
            this.currentRealms.put(type, realm);
            this.timeInCurrentRealm.put(type, 0L);
            this.markDirty();

            // Handle realm transition for ServerPlayer
            if (player instanceof ServerPlayer serverPlayer) {
                try {
                    boolean isAdvancement = previousRealm == null || realm.getRealmLevel() > previousRealm.getRealmLevel();

                    if (isAdvancement) {
                        // Normal advancement - apply new realm bonuses additively
                        realm.applyRealmModifiers(serverPlayer, this, type);
                    } else {
                        // Manual realm setting (like commands) or realm downgrade
                        // Recalculate total bonuses from scratch to ensure accuracy
                        recalculateAllRealmBonuses(serverPlayer, type);

                        // Send notification message
                        serverPlayer.sendSystemMessage(
                            Component.literal("Your " + type.getDisplayName() + " cultivation realm has been set to " + realm.getDisplayName())
                                .withStyle(ChatFormatting.AQUA)
                        );
                    }

                    // Force network sync to client
                    this.markDirty();
                    PlayerStatsAttachment stats = serverPlayer.getData(ICModAttachmentTypes.PLAYER_STATS.get());
                    if (stats != null) {
                        stats.markDirty();
                    }

                    ImmortalCultivation.LOGGER.info("Successfully applied realm change for player {} - {} cultivation set to {} (level {})",
                        serverPlayer.getName().getString(), type.getDisplayName(), realm.getDisplayName(), realm.getRealmLevel());

                } catch (Exception e) {
                    ImmortalCultivation.LOGGER.error("Error applying realm modifiers during realm change for player {}",
                        serverPlayer.getName().getString(), e);
                }
            }
        }
    }

    /**
     * Recalculates cumulative bonuses from all realms up to the current level.
     * This is used when manually setting realms via commands.
     */
    public void recalculateAllRealmBonuses(ServerPlayer player, CultivationType type) {
        try {
            PlayerStatsAttachment stats = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());

            if (stats == null) {
                ImmortalCultivation.LOGGER.warn("Missing attachments for player {}", player.getName().getString());
                return;
            }

            CultivationRealm currentRealm = this.currentRealms.get(type);
            if (currentRealm == null) {
                ImmortalCultivation.LOGGER.warn("No current realm found for type {} for player {}", type, player.getName().getString());
                return;
            }

            ImmortalCultivation.LOGGER.info("Recalculating cumulative bonuses for {} {} cultivation up to level {} ({})",
                player.getName().getString(), type.getDisplayName(), currentRealm.getRealmLevel(), currentRealm.getDisplayName());

            // Calculate cumulative bonuses from level 1 to current level (excluding mortal level 0)
            long totalHealthBonus = 0;
            double totalHealthRegenBonus = 0.0;
            long totalQiBonus = 0;
            double totalQiRegenBonus = 0.0;
            long totalBodyBonus = 0;
            double totalBodyRegenBonus = 0.0;
            double totalAttackBonus = 0.0;
            double totalSpeedBonus = 0.0;
            double totalJumpBonus = 0.0;

            // Iterate through each realm level from 1 to current level
            for (int level = 1; level <= currentRealm.getRealmLevel(); level++) {
                CultivationRealm realm = findRealmByLevel(level);
                if (realm != null) {
                    // Add this realm's bonuses to the totals
                    totalHealthBonus += realm.getBaseMaxHealth();
                    totalHealthRegenBonus += realm.getBaseHealthRegenRate();
                    totalQiBonus += realm.getBaseMaxQi();
                    totalQiRegenBonus += realm.getBaseQiRegenRate();
                    totalBodyBonus += realm.getBaseMaxBody();
                    totalBodyRegenBonus += realm.getBaseBodyRegenRate();
                    totalAttackBonus += realm.getBaseAttackDamage();
                    totalSpeedBonus += realm.getBaseMovementSpeed();
                    totalJumpBonus += realm.getBaseJumpHeight();

                    ImmortalCultivation.LOGGER.debug("Added bonuses from {} (Level {}): Health +{}, Qi +{}, Body +{}",
                        realm.getDisplayName(), level, realm.getBaseMaxHealth(), realm.getBaseMaxQi(), realm.getBaseMaxBody());
                }
            }

            // Apply cumulative bonuses to attachments
            // For resources, we SET the permanent bonuses to the cumulative totals
            stats.setPermanentStatBonus(IStatsData.StatType.HEALTH, totalHealthBonus);
            stats.setPermanentStatRegenBonus(IStatsData.StatType.HEALTH, totalHealthRegenBonus);
            stats.setPermanentStatBonus(IStatsData.StatType.QI, totalQiBonus);
            stats.setPermanentStatRegenBonus(IStatsData.StatType.QI, totalQiRegenBonus);
            stats.setPermanentStatBonus(IStatsData.StatType.BODY, totalBodyBonus);
            stats.setPermanentStatRegenBonus(IStatsData.StatType.BODY, totalBodyRegenBonus);

            // For combat stats, we SET the permanent bonuses to the cumulative totals
            stats.setPermanentAttackDamageBonus(totalAttackBonus);
            stats.setPermanentMovementSpeedBonus(totalSpeedBonus);
            stats.setPermanentJumpHeightBonus(totalJumpBonus);

            // Recalculate derived values and force sync
            stats.recalculateAll();
            stats.markDirty();

            // Refresh combat stats to apply the changes immediately
            ICModCombatEvent.refreshCombatStats(player);

            ImmortalCultivation.LOGGER.info("Applied cumulative bonuses for {}: Health +{}, Qi +{}, Body +{}, ATK +{}%, SPD +{}%, JUMP +{}%",
                player.getName().getString(), totalHealthBonus, totalQiBonus, totalBodyBonus,
                String.format("%.1f", totalAttackBonus * 100),
                String.format("%.1f", totalSpeedBonus * 100),
                String.format("%.1f", totalJumpBonus * 100));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error recalculating realm bonuses for player {}", player.getName().getString(), e);
        }
    }

    private void clearAllBonuses(PlayerStatsAttachment stats) {
        // Clear all permanent bonuses to recalculate from scratch
        stats.setPermanentStatBonus(IStatsData.StatType.HEALTH, 0L);
        stats.setPermanentStatRegenBonus(IStatsData.StatType.HEALTH,0.0);
        stats.setPermanentStatBonus(IStatsData.StatType.QI, 0L);
        stats.setPermanentStatRegenBonus(IStatsData.StatType.QI,0.0);
        stats.setPermanentStatBonus(IStatsData.StatType.BODY, 0L);
        stats.setPermanentStatRegenBonus(IStatsData.StatType.BODY,0.0);
        stats.setPermanentAttackDamageBonus(0.0);
        stats.setPermanentMovementSpeedBonus(0.0);
        stats.setPermanentJumpHeightBonus(0.0);
    }

    /**
     * Helper method to find a realm by its level.
     * This searches through all registered realms to find one with the specified level.
     */
    private CultivationRealm findRealmByLevel(int level) {
        return ICModCultivationRealms.getAllRealms().stream()
            .filter(realm -> realm.getRealmLevel() == level)
            .findFirst()
            .orElse(null);
    }
    
    // === FOCUS METHODS === //

    public CultivationType getCurrentFocus() { return this.currentFocus; }

    public void setCurrentFocus(CultivationType focus) {
        if (this.currentFocus != focus) {
            this.currentFocus = focus;
            this.markDirty();
        }
    }

    public long getLastFocusChangeTime() {
        return this.lastFocusChangeTime;
    }

    public boolean toggleFocus() {
        if (player == null) return false;
        long currentTime = player.level().getGameTime();
        if (currentTime - lastFocusChangeTime < FOCUS_CHANGE_COOLDOWN) {
            return false; // Cooldown active
        }
        this.currentFocus = (currentFocus == CultivationType.QI) ? CultivationType.BODY : CultivationType.QI;
        this.lastFocusChangeTime = currentTime;
        markDirty();
        return true;
    }


    // === CORE LOGIC === //

    private void initializeDefaults() {
        CultivationRealm defaultRealm = getDefaultMortalRealm();
        for (CultivationType type : CultivationType.values()) {
            currentRealms.put(type, defaultRealm);
            currentCultivation.put(type, 0L);
            maxCultivation.put(type, getSafeMaxCultivation(defaultRealm));
            cultivationFraction.put(type, 0.0);
            accumulationRate.put(type, 0.0);
            totalCultivationEarned.put(type, 0L);
            lastMilestone.put(type, 0L);
            additiveBonus.put(type, 0.0);
            multiplicativeBonus.put(type, 0.0);
            timeInCurrentRealm.put(type, 0L);
            totalCultivationTime.put(type, 0L);
        }
    }

    @Override
    public long addCultivation(CultivationType type, long amount) {
        if (amount <= 0) return 0;
        totalCultivationEarned.merge(type, amount, Long::sum);
        long currentBefore = currentCultivation.get(type);
        setCurrentCultivation(type, currentBefore + amount);
        return currentCultivation.get(type) - currentBefore;
    }

    @Override
    public long subtractCultivation(CultivationType type, long amount) {
        if (amount <= 0) return 0;
        long currentBefore = currentCultivation.get(type);
        setCurrentCultivation(type, currentBefore - amount);
        return currentBefore - currentCultivation.get(type);
    }

    @Override
    public double getEffectiveCultivationRate(CultivationType type) {
        double baseRate = getBaseCultivationRate(type);
        Player p = (player != null && player.isAlive()) ? player : null;
        if (p == null) return baseRate;
        PlayerTalentsAttachment talent = p.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
        if (talent != null) baseRate *= (type == CultivationType.QI) ? talent.getSpiritualRootMultiplier() : talent.getPhysiqueMultiplier();
        PlayerRaceAttachment race = p.getData(ICModAttachmentTypes.PLAYER_RACE.get());
        if (race != null && race.hasRaceAssigned()) baseRate *= (type == CultivationType.QI) ? race.getQiMultiplier() : race.getBodyMultiplier();
        baseRate = (baseRate + additiveBonus.get(type)) * (1.0 + multiplicativeBonus.get(type));
        if (currentFocus == type) baseRate *= 2.0;
        return baseRate;
    }
    
    public void addBonusWithDiminishing(CultivationType type, double bonusAmount, double minBonus, double maxBonus) {
        if (bonusAmount <= 0) return;

        // Get the current bonus and apply the new one, capping at the max
        double currentBonus = this.additiveBonus.getOrDefault(type, 0.0);
        double newBonus = Math.min(currentBonus + bonusAmount, maxBonus);

        // Ensure the bonus doesn't fall below the minimum
        newBonus = Math.max(newBonus, minBonus);

        this.additiveBonus.put(type, newBonus);
        this.markDirty();
    }

    @Override
    public void addRegenFraction(CultivationType type, double amount) {
        double current = cultivationFraction.get(type) + amount;

        // Only convert to long if the fraction is >= 1.0
        if (current >= 1.0) {
            long pointsToAdd = (long) current;
            if (pointsToAdd > 0) {
                addCultivation(type, pointsToAdd); // This will call markDirty() if values change
                current -= pointsToAdd;
                // markDirty() is called by addCultivation() -> setCurrentCultivation()
            }
        }

        cultivationFraction.put(type, current);
        // Don't mark dirty here - only mark dirty when actual cultivation points are added
    }

    public boolean canBreakthrough(CultivationType type) {
        return currentCultivation.get(type) >= maxCultivation.get(type);
    }

    public PlayerCultivationAttachment copy() {
        PlayerCultivationAttachment copy = new PlayerCultivationAttachment();
        copy.currentRealms.putAll(this.currentRealms);
        copy.currentCultivation.putAll(this.currentCultivation);
        copy.maxCultivation.putAll(this.maxCultivation);
        copy.currentFocus = this.currentFocus;
        return copy;
    }

    // === IMPLEMENTATIONS === //

    // Example fix for PlayerCultivationAttachment.createPacket()
// This pattern should be applied to ALL attachment classes

    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        if (player == null) {
            return Optional.empty();
        }

        if (!isFullSync && !isDirty()) {
            return Optional.empty();
        }

        if (!isFullSync && lastSyncedState == null) {
            isFullSync = true;
            ImmortalCultivation.LOGGER.debug("Forcing full sync for {} - no lastSyncedState", player.getName().getString());
        }

        byte fieldMask = 0;
        Optional<PlayerCultivationSyncPacket.CultivationDataPayload> qiPayload = Optional.empty();
        Optional<PlayerCultivationSyncPacket.CultivationDataPayload> bodyPayload = Optional.empty();
        Optional<CultivationType> focusPayload = Optional.empty();

        // Build Qi cultivation payload
        byte qiPayloadMask = 0;
        Optional<String> qiRealmId = Optional.empty();
        Optional<Long> qiCurrent = Optional.empty();
        Optional<Long> qiMax = Optional.empty();

        if (isFullSync || lastSyncedState == null ||
            !currentRealms.get(CultivationType.QI).getId().equals(lastSyncedState.currentRealms.get(CultivationType.QI).getId())) {
            qiRealmId = Optional.of(currentRealms.get(CultivationType.QI).getId());
            qiPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.REALM_ID_MASK;
            ImmortalCultivation.LOGGER.debug("Adding QI realm to sync: {}", qiRealmId.get());
        }

        // Fix: Use .equals() for Long comparison instead of !=
        if (isFullSync || lastSyncedState == null ||
            !currentCultivation.get(CultivationType.QI).equals(lastSyncedState.currentCultivation.get(CultivationType.QI))) {
            qiCurrent = Optional.of(currentCultivation.get(CultivationType.QI));
            qiPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.CURRENT_MASK;
            ImmortalCultivation.LOGGER.debug("Adding QI current to sync: {}", qiCurrent.get());
        }

        if (isFullSync || lastSyncedState == null ||
            !maxCultivation.get(CultivationType.QI).equals(lastSyncedState.maxCultivation.get(CultivationType.QI))) {
            qiMax = Optional.of(maxCultivation.get(CultivationType.QI));
            qiPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.MAX_MASK;
            ImmortalCultivation.LOGGER.debug("Adding QI max to sync: {}", qiMax.get());
        }

        if (qiPayloadMask != 0) {
            qiPayload = Optional.of(new PlayerCultivationSyncPacket.CultivationDataPayload(qiPayloadMask, qiRealmId, qiCurrent, qiMax));
            fieldMask |= PlayerCultivationSyncPacket.QI_CULTIVATION_MASK;
        }

        // Build Body cultivation payload (similar pattern with .equals())
        byte bodyPayloadMask = 0;
        Optional<String> bodyRealmId = Optional.empty();
        Optional<Long> bodyCurrent = Optional.empty();
        Optional<Long> bodyMax = Optional.empty();

        if (isFullSync || lastSyncedState == null ||
            !currentRealms.get(CultivationType.BODY).getId().equals(lastSyncedState.currentRealms.get(CultivationType.BODY).getId())) {
            bodyRealmId = Optional.of(currentRealms.get(CultivationType.BODY).getId());
            bodyPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.REALM_ID_MASK;
        }

        if (isFullSync || lastSyncedState == null ||
            !currentCultivation.get(CultivationType.BODY).equals(lastSyncedState.currentCultivation.get(CultivationType.BODY))) {
            bodyCurrent = Optional.of(currentCultivation.get(CultivationType.BODY));
            bodyPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.CURRENT_MASK;
        }

        if (isFullSync || lastSyncedState == null ||
            !maxCultivation.get(CultivationType.BODY).equals(lastSyncedState.maxCultivation.get(CultivationType.BODY))) {
            bodyMax = Optional.of(maxCultivation.get(CultivationType.BODY));
            bodyPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.MAX_MASK;
        }

        if (bodyPayloadMask != 0) {
            bodyPayload = Optional.of(new PlayerCultivationSyncPacket.CultivationDataPayload(bodyPayloadMask, bodyRealmId, bodyCurrent, bodyMax));
            fieldMask |= PlayerCultivationSyncPacket.BODY_CULTIVATION_MASK;
        }

        // Build focus payload
        if (isFullSync || lastSyncedState == null || currentFocus != lastSyncedState.currentFocus) {
            focusPayload = Optional.of(currentFocus);
            fieldMask |= PlayerCultivationSyncPacket.FOCUS_MASK;
        }

        ImmortalCultivation.LOGGER.debug("PlayerCultivation createPacket - fieldMask: {}, isFullSync: {}, isDirty: {}",
            fieldMask, isFullSync, isDirty());

        if (fieldMask == 0) {
            ImmortalCultivation.LOGGER.debug("No cultivation changes detected for player {}", player.getName().getString());
            return Optional.empty();
        }

        // Update last synced state ONLY when we have changes to send
        this.lastSyncedState = this.copy();

        return Optional.of(new PlayerCultivationSyncPacket(fieldMask, qiPayload, bodyPayload, focusPayload));
    }

    @Override
    public void tick() {
        if (player == null) return;
        for (CultivationType type : CultivationType.values()) {
            addRegenFraction(type, getEffectiveCultivationRate(type) / 20.0);
        }
    }

    @Override
    public boolean isDirty() { return this.isDirty; }

    @Override
    public void markDirty() { this.isDirty = true; }

    @Override
    public void clearDirty() { this.isDirty = false; }

    // === HELPER METHODS === //

    private CultivationRealm getDefaultMortalRealm() {
        CultivationRealm mortalRealm = ICModCultivationRealms.get(DEFAULT_MORTAL_REALM_ID);
        return mortalRealm != null ? mortalRealm : createTemporaryMortalRealm();
    }

    private long getSafeMaxCultivation(CultivationRealm realm) {
        if (realm != null) {
            try {
                return realm.getBaseMaxCultivation();
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.warn("Error getting max cultivation from realm, using default", e);
            }
        }
        return DEFAULT_MAX_CULTIVATION;
    }

    private CultivationRealm createTemporaryMortalRealm() {
        var basicInfo = new CultivationRealmData.BasicInfo(DEFAULT_MORTAL_REALM_ID, "Mortal", 0, "Mortal", "Temp");
        var physicalStats = new CultivationRealmData.PhysicalStats(20L, 0.0, 0.0, 0.0, 0.0);
        var cultivationStats = new CultivationRealmData.CultivationStats(DEFAULT_MAX_CULTIVATION, 0.005);
        var qiStats = new CultivationRealmData.QiStats(0L, 0.0);
        var bodyStats = new CultivationRealmData.BodyStats(0L, 0.0);
        var visualProperties = new CultivationRealmData.VisualProperties(8421504, "minecraft:cloud", java.util.Optional.empty());
        var progression = new CultivationRealmData.Progression(java.util.List.of(), java.util.List.of());
        var features = new CultivationRealmData.Features(java.util.List.of(), java.util.List.of(), java.util.List.of());
        var realmData = new CultivationRealmData(basicInfo, physicalStats, cultivationStats, qiStats, bodyStats, visualProperties, progression, features, java.util.Optional.empty(), java.util.Optional.empty());  // UPDATE THIS LINE
        return new JsonCultivationRealm(realmData);
    }


    // === SERIALIZATION === //

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerCultivationAttachment> {
        @Override
        public PlayerCultivationAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerCultivationAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .result()
                .map(com.mojang.datafixers.util.Pair::getFirst)
                .orElseGet(PlayerCultivationAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }
            return attachment;
        }

        @Override
        public CompoundTag write(PlayerCultivationAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment).getOrThrow();
        }
    }

    // === CODEC === //

    public static final Codec<PlayerCultivationAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("qi_realm").forGetter(att -> att.currentRealms.get(CultivationType.QI).getId()),
            Codec.LONG.fieldOf("qi_current").forGetter(att -> att.currentCultivation.get(CultivationType.QI)),
            Codec.LONG.fieldOf("qi_max").forGetter(att -> att.maxCultivation.get(CultivationType.QI)),
            Codec.STRING.fieldOf("body_realm").forGetter(att -> att.currentRealms.get(CultivationType.BODY).getId()),
            Codec.LONG.fieldOf("body_current").forGetter(att -> att.currentCultivation.get(CultivationType.BODY)),
            Codec.LONG.fieldOf("body_max").forGetter(att -> att.maxCultivation.get(CultivationType.BODY)),
            Codec.STRING.fieldOf("focus").forGetter(att -> att.currentFocus.name())
        ).apply(instance, PlayerCultivationAttachment::decode)
    );
    private static PlayerCultivationAttachment decode(String qiRealm, long qiCurrent, long qiMax, String bodyRealm, long bodyCurrent, long bodyMax, String focus) {
        PlayerCultivationAttachment att = new PlayerCultivationAttachment();
        att.currentRealms.put(CultivationType.QI, ICModCultivationRealms.get(qiRealm));
        att.currentCultivation.put(CultivationType.QI, qiCurrent);
        att.maxCultivation.put(CultivationType.QI, qiMax);
        att.currentRealms.put(CultivationType.BODY, ICModCultivationRealms.get(bodyRealm));
        att.currentCultivation.put(CultivationType.BODY, bodyCurrent);
        att.maxCultivation.put(CultivationType.BODY, bodyMax);
        att.currentFocus = CultivationType.valueOf(focus);
        return att;
    }
}
