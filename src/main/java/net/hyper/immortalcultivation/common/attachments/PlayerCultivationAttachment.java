package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.common.interfaces.ITickable;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealmData;
import net.hyper.immortalcultivation.core.cultivation.realms.JsonCultivationRealm;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerCultivationSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class PlayerCultivationAttachment implements ICultivationData, ISyncable, ITickable {

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
        if (oldValue != newValue) this.markDirty();
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

    public void setRealm(CultivationType type, CultivationRealm realm) {
        if (realm != null && !realm.equals(this.currentRealms.get(type))) {
            this.previousRealms.put(type, this.currentRealms.get(type));
            this.currentRealms.put(type, realm);
            this.timeInCurrentRealm.put(type, 0L);
            this.markDirty();
        }
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
        PlayerTalentAttachment talent = p.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
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
        long pointsToAdd = (long) current;
        if (pointsToAdd > 0) {
            addCultivation(type, pointsToAdd);
            current -= pointsToAdd;
        }
        cultivationFraction.put(type, current);
        this.markDirty();
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

    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        if (player == null || player.level().isClientSide()) {
            return Optional.empty();
        }

        if (!isFullSync && lastSyncedState == null) {
            isFullSync = true;
        }

        byte qiPayloadMask = 0;
        Optional<String> qiRealmId = Optional.empty();
        Optional<Long> qiCurrent = Optional.empty();
        Optional<Long> qiMax = Optional.empty();

        if (isFullSync || !this.currentRealms.get(CultivationType.QI).getId().equals(lastSyncedState.currentRealms.get(CultivationType.QI).getId())) {
            qiPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.REALM_ID_MASK;
            qiRealmId = Optional.of(this.currentRealms.get(CultivationType.QI).getId());
        }
        if (isFullSync || !this.currentCultivation.get(CultivationType.QI).equals(lastSyncedState.currentCultivation.get(CultivationType.QI))) {
            qiPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.CURRENT_MASK;
            qiCurrent = Optional.of(this.currentCultivation.get(CultivationType.QI));
        }
        if (isFullSync || !this.maxCultivation.get(CultivationType.QI).equals(lastSyncedState.maxCultivation.get(CultivationType.QI))) {
            qiPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.MAX_MASK;
            qiMax = Optional.of(this.maxCultivation.get(CultivationType.QI));
        }

        Optional<PlayerCultivationSyncPacket.CultivationDataPayload> qiPayload = qiPayloadMask != 0
            ? Optional.of(new PlayerCultivationSyncPacket.CultivationDataPayload(qiPayloadMask, qiRealmId, qiCurrent, qiMax))
            : Optional.empty();

        byte bodyPayloadMask = 0;
        Optional<String> bodyRealmId = Optional.empty();
        Optional<Long> bodyCurrent = Optional.empty();
        Optional<Long> bodyMax = Optional.empty();

        if (isFullSync || !this.currentRealms.get(CultivationType.BODY).getId().equals(lastSyncedState.currentRealms.get(CultivationType.BODY).getId())) {
            bodyPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.REALM_ID_MASK;
            bodyRealmId = Optional.of(this.currentRealms.get(CultivationType.BODY).getId());
        }
        if (isFullSync || !this.currentCultivation.get(CultivationType.BODY).equals(lastSyncedState.currentCultivation.get(CultivationType.BODY))) {
            bodyPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.CURRENT_MASK;
            bodyCurrent = Optional.of(this.currentCultivation.get(CultivationType.BODY));
        }
        if (isFullSync || !this.maxCultivation.get(CultivationType.BODY).equals(lastSyncedState.maxCultivation.get(CultivationType.BODY))) {
            bodyPayloadMask |= PlayerCultivationSyncPacket.CultivationDataPayload.MAX_MASK;
            bodyMax = Optional.of(this.maxCultivation.get(CultivationType.BODY));
        }

        Optional<PlayerCultivationSyncPacket.CultivationDataPayload> bodyPayload = bodyPayloadMask != 0
            ? Optional.of(new PlayerCultivationSyncPacket.CultivationDataPayload(bodyPayloadMask, bodyRealmId, bodyCurrent, bodyMax))
            : Optional.empty();

        byte packetMask = 0;
        Optional<CultivationType> focus = Optional.empty();

        if (qiPayload.isPresent()) packetMask |= PlayerCultivationSyncPacket.QI_CULTIVATION_MASK;
        if (bodyPayload.isPresent()) packetMask |= PlayerCultivationSyncPacket.BODY_CULTIVATION_MASK;

        if (isFullSync || this.currentFocus != lastSyncedState.currentFocus) {
            packetMask |= PlayerCultivationSyncPacket.FOCUS_MASK;
            focus = Optional.of(this.currentFocus);
        }

        if (packetMask != 0) {
            this.lastSyncedState = this.copy();
            return Optional.of(new PlayerCultivationSyncPacket(packetMask, qiPayload, bodyPayload, focus));
        }

        return Optional.empty();
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
        var qiStats = new CultivationRealmData.QiStats(0L, 0.0, 0.0);
        var visualProperties = new CultivationRealmData.VisualProperties(8421504, "minecraft:cloud", java.util.Optional.empty());
        var progression = new CultivationRealmData.Progression(java.util.List.of(), java.util.List.of());
        var features = new CultivationRealmData.Features(java.util.List.of(), java.util.List.of(), java.util.List.of());
        var realmData = new CultivationRealmData(basicInfo, physicalStats, cultivationStats, qiStats, visualProperties, progression, features, java.util.Optional.empty(), java.util.Optional.empty());
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
