package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.*;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerResourcesSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.Optional;

public class PlayerResourceAttachment implements IHealthData, IQiData, IBodyData, ITickable, ISyncable {

    // === CONSTANTS === //

    private static final long MORTAL_BASE_HEALTH = 20L;
    private static final long MORTAL_BASE_QI = 0L;
    private static final long MORTAL_BASE_BODY = 0L;

    // === INSTANCES === //

    @Nullable
    private transient Player player;

    // --- Health Data ---
    private long permanentHealthBonus = 0L;
    private double permanentHealthRegenBonus = 0.0;
    private long currentHealth = MORTAL_BASE_HEALTH;
    private long maxHealth = MORTAL_BASE_HEALTH;
    private double healthRegenFraction = 0.0;

    // --- Qi Data ---
    private long permanentMaxQiBonus = 0L;
    private double permanentQiRegenBonus = 0.0;
    private long currentQi = MORTAL_BASE_QI;
    private long maxQi = MORTAL_BASE_QI;
    private double qiRegenFraction = 0.0;

    // --- Body Data ---
    private long permanentMaxBodyBonus = 0L;
    private double permanentBodyRegenBonus = 0.0;
    private long currentBody = MORTAL_BASE_BODY;
    private long maxBody = MORTAL_BASE_BODY;
    private double bodyRegenFraction = 0.0;

    // --- System Data ---
    private transient boolean isDirty = false;
    private transient PlayerResourceAttachment lastSyncedState = null;

    // === CONSTRUCTORS === //

    public PlayerResourceAttachment() {}

    // === GETTERS & SETTERS === //

    public void setPlayer(Player player) {
        this.player = player;
    }

    // --- Health Methods ---
    @Override
    public long getCurrentHealth() { return currentHealth; }

    @Override
    public void setCurrentHealth(long value) {
        long oldHealth = this.currentHealth;
        this.currentHealth = Math.max(0, Math.min(value, maxHealth));
        if (oldHealth != this.currentHealth) {
            updateVanillaHealthBar();
            markDirty();
        }
    }

    @Override
    public long getMaxHealth() { return maxHealth; }

    @Override
    public double getBaseHealthRegenerationRate() { return permanentHealthRegenBonus; }

    @Override
    public double getEffectiveHealthRegenerationRate() { return getBaseHealthRegenerationRate(); }

    @Override
    public double getRegenFraction() { return healthRegenFraction; }

    @Override
    public void setRegenFraction(double fraction) { this.healthRegenFraction = fraction; }

    @Override
    public void addRegenFraction(double amount) { this.healthRegenFraction += amount; }

    @Override
    public long getBaseMaxHealth() { return MORTAL_BASE_HEALTH + permanentHealthBonus; }

    public void addPermanentHealthBonus(long bonus) {
        this.permanentHealthBonus += bonus;
        recalculateMaxHealth();
        markDirty();
    }

    public void addPermanentHealthRegenBonus(double bonus) {
        this.permanentHealthRegenBonus += bonus;
        markDirty();
    }

    // --- Qi Methods ---
    @Override
    public long getCurrentQi() { return currentQi; }

    @Override
    public void setCurrentQi(long value) {
        long oldQi = this.currentQi;
        this.currentQi = Math.max(0, Math.min(value, maxQi));
        if (oldQi != this.currentQi) markDirty();
    }

    @Override
    public long getMaxQi() { return maxQi; }

    @Override
    public double getBaseQiRegenerationRate() { return permanentQiRegenBonus; }

    @Override
    public double getEffectiveQiRegenerationRate() { return getBaseQiRegenerationRate(); }

    @Override
    public long getBaseMaxQi() { return MORTAL_BASE_QI + permanentMaxQiBonus; }

    @Override
    public void consumeQi(long amount) { setCurrentQi(currentQi - amount); }

    public void addPermanentMaxQiBonus(long bonus) {
        this.permanentMaxQiBonus += bonus;
        recalculateMaxQi();
        markDirty();
    }

    public void addPermanentQiRegenBonus(double bonus) {
        this.permanentQiRegenBonus += bonus;
        markDirty();
    }

    // --- Body Methods ---
    @Override
    public long getCurrentBody() { return currentBody; }

    @Override
    public void setCurrentBody(long value) {
        long oldBody = this.currentBody;
        this.currentBody = Math.max(0, Math.min(value, maxBody));
        if (oldBody != this.currentBody) markDirty();
    }

    @Override
    public long getMaxBody() { return maxBody; }

    @Override
    public double getBaseBodyRegenerationRate() { return permanentBodyRegenBonus; }

    @Override
    public double getEffectiveBodyRegenerationRate() { return getBaseBodyRegenerationRate(); }

    @Override
    public long getBaseMaxBody() { return MORTAL_BASE_BODY + permanentMaxBodyBonus; }

    @Override
    public void consumeBody(long amount) { setCurrentBody(currentBody - amount); }

    public void addPermanentMaxBodyBonus(long bonus) {
        this.permanentMaxBodyBonus += bonus;
        recalculateMaxBody();
        markDirty();
    }

    public void addPermanentBodyRegenBonus(double bonus) {
        this.permanentBodyRegenBonus += bonus;
        markDirty();
    }

    // === IMPLEMENTATIONS === //

    @Override
    public void tick() {
        if (player == null) return;
        regenerate();
        updateVanillaHealthBar();
    }

    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        if (player == null || player.level().isClientSide() || !isDirty()) return Optional.empty();
        if (lastSyncedState == null) isFullSync = true;

        CompoundTag data = new CompoundTag();
        if (isFullSync || this.currentHealth != lastSyncedState.currentHealth) data.putLong(PlayerResourcesSyncPacket.KEY_CURRENT_HEALTH, this.currentHealth);
        if (isFullSync || this.permanentHealthBonus != lastSyncedState.permanentHealthBonus) data.putLong(PlayerResourcesSyncPacket.KEY_PERM_HEALTH_BONUS, this.permanentHealthBonus);
        if (isFullSync || this.permanentHealthRegenBonus != lastSyncedState.permanentHealthRegenBonus) data.putDouble(PlayerResourcesSyncPacket.KEY_PERM_HEALTH_REGEN, this.permanentHealthRegenBonus);
        if (isFullSync || this.currentQi != lastSyncedState.currentQi) data.putLong(PlayerResourcesSyncPacket.KEY_CURRENT_QI, this.currentQi);
        if (isFullSync || this.permanentMaxQiBonus != lastSyncedState.permanentMaxQiBonus) data.putLong(PlayerResourcesSyncPacket.KEY_PERM_QI_BONUS, this.permanentMaxQiBonus);
        if (isFullSync || this.permanentQiRegenBonus != lastSyncedState.permanentQiRegenBonus) data.putDouble(PlayerResourcesSyncPacket.KEY_PERM_QI_REGEN, this.permanentQiRegenBonus);
        if (isFullSync || this.currentBody != lastSyncedState.currentBody) data.putLong(PlayerResourcesSyncPacket.KEY_CURRENT_BODY, this.currentBody);
        if (isFullSync || this.permanentMaxBodyBonus != lastSyncedState.permanentMaxBodyBonus) data.putLong(PlayerResourcesSyncPacket.KEY_PERM_BODY_BONUS, this.permanentMaxBodyBonus);
        if (isFullSync || this.permanentBodyRegenBonus != lastSyncedState.permanentBodyRegenBonus) data.putDouble(PlayerResourcesSyncPacket.KEY_PERM_BODY_REGEN, this.permanentBodyRegenBonus);

        if (!data.isEmpty()) {
            this.lastSyncedState = this.copy();
            clearDirty();
            return Optional.of(new PlayerResourcesSyncPacket(data));
        }

        return Optional.empty();
    }

    @Override
    public boolean isDirty() { return isDirty; }

    @Override
    public void markDirty() { this.isDirty = true; }

    @Override
    public void clearDirty() { this.isDirty = false; }

    // === HELPER METHODS === //

    public void applySync(CompoundTag data) {
        if (data.contains(PlayerResourcesSyncPacket.KEY_CURRENT_HEALTH)) this.currentHealth = data.getLong(PlayerResourcesSyncPacket.KEY_CURRENT_HEALTH);
        if (data.contains(PlayerResourcesSyncPacket.KEY_PERM_HEALTH_BONUS)) this.permanentHealthBonus = data.getLong(PlayerResourcesSyncPacket.KEY_PERM_HEALTH_BONUS);
        if (data.contains(PlayerResourcesSyncPacket.KEY_PERM_HEALTH_REGEN)) this.permanentHealthRegenBonus = data.getDouble(PlayerResourcesSyncPacket.KEY_PERM_HEALTH_REGEN);
        if (data.contains(PlayerResourcesSyncPacket.KEY_CURRENT_QI)) this.currentQi = data.getLong(PlayerResourcesSyncPacket.KEY_CURRENT_QI);
        if (data.contains(PlayerResourcesSyncPacket.KEY_PERM_QI_BONUS)) this.permanentMaxQiBonus = data.getLong(PlayerResourcesSyncPacket.KEY_PERM_QI_BONUS);
        if (data.contains(PlayerResourcesSyncPacket.KEY_PERM_QI_REGEN)) this.permanentQiRegenBonus = data.getDouble(PlayerResourcesSyncPacket.KEY_PERM_QI_REGEN);
        if (data.contains(PlayerResourcesSyncPacket.KEY_CURRENT_BODY)) this.currentBody = data.getLong(PlayerResourcesSyncPacket.KEY_CURRENT_BODY);
        if (data.contains(PlayerResourcesSyncPacket.KEY_PERM_BODY_BONUS)) this.permanentMaxBodyBonus = data.getLong(PlayerResourcesSyncPacket.KEY_PERM_BODY_BONUS);
        if (data.contains(PlayerResourcesSyncPacket.KEY_PERM_BODY_REGEN)) this.permanentBodyRegenBonus = data.getDouble(PlayerResourcesSyncPacket.KEY_PERM_BODY_REGEN);
        recalculateAll();
    }

    private void regenerate() {
        regenerateHealth(getEffectiveHealthRegenerationRate() / 20.0);
        regenerateQi(getEffectiveQiRegenerationRate() / 20.0);
        regenerateBody(getEffectiveBodyRegenerationRate() / 20.0);
    }

    private void regenerateHealth(double rate) {
        if (currentHealth < maxHealth) {
            addRegenFraction(rate);
            long healing = (long) getRegenFraction();
            if (healing > 0) {
                setCurrentHealth(currentHealth + healing);
                setRegenFraction(getRegenFraction() - healing);
            }
        }
    }

    private void regenerateQi(double rate) {
        if (currentQi < maxQi) {
            qiRegenFraction += rate;
            long regen = (long) qiRegenFraction;
            if (regen > 0) {
                setCurrentQi(currentQi + regen);
                qiRegenFraction -= regen;
            }
        }
    }

    private void regenerateBody(double rate) {
        if (currentBody < maxBody) {
            bodyRegenFraction += rate;
            long regen = (long) bodyRegenFraction;
            if (regen > 0) {
                setCurrentBody(currentBody + regen);
                bodyRegenFraction -= regen;
            }
        }
    }

    private void updateVanillaHealthBar() {
        if (player != null && player.isAlive()) {
            float healthPercentage = maxHealth > 0 ? (float)currentHealth / (float)maxHealth : 0f;
            player.setHealth(Math.max(1.0f, player.getMaxHealth() * healthPercentage));
        }
    }
    
    public void recalculateAll() {
        recalculateMaxHealth();
        recalculateMaxQi();
        recalculateMaxBody();
    }

    private void recalculateMaxHealth() {
        this.maxHealth = getBaseMaxHealth();
        if (this.currentHealth > this.maxHealth) {
            this.currentHealth = this.maxHealth;
        }
        updateVanillaHealthBar();
    }

    private void recalculateMaxQi() {
        this.maxQi = getBaseMaxQi();
        if (this.currentQi > this.maxQi) {
            this.currentQi = this.maxQi;
        }
    }

    private void recalculateMaxBody() {
        this.maxBody = getBaseMaxBody();
        if (this.currentBody > this.maxBody) {
            this.currentBody = this.maxBody;
        }
    }

    public PlayerResourceAttachment copy() {
        PlayerResourceAttachment copy = new PlayerResourceAttachment();
        copy.currentHealth = this.currentHealth;
        copy.permanentHealthBonus = this.permanentHealthBonus;
        copy.permanentHealthRegenBonus = this.permanentHealthRegenBonus;
        copy.maxHealth = this.maxHealth;
        copy.currentQi = this.currentQi;
        copy.permanentMaxQiBonus = this.permanentMaxQiBonus;
        copy.permanentQiRegenBonus = this.permanentQiRegenBonus;
        copy.maxQi = this.maxQi;
        copy.currentBody = this.currentBody;
        copy.permanentMaxBodyBonus = this.permanentMaxBodyBonus;
        copy.permanentBodyRegenBonus = this.permanentBodyRegenBonus;
        copy.maxBody = this.maxBody;
        return copy;
    }

    // === SERIALIZATION === //

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerResourceAttachment> {
        @Override
        public PlayerResourceAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerResourceAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerResourceAttachment: {}", error))
                    .map(com.mojang.datafixers.util.Pair::getFirst)
                    .orElseGet(PlayerResourceAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }
            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerResourceAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerResourceAttachment: {}", error))
                    .orElse(new CompoundTag());
        }
    }

    // === CODEC === //
    
    public static final Codec<PlayerResourceAttachment> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.fieldOf("current_health").forGetter(d -> d.currentHealth),
                    Codec.LONG.fieldOf("permanent_health_bonus").forGetter(d -> d.permanentHealthBonus),
                    Codec.DOUBLE.fieldOf("permanent_health_regen_bonus").forGetter(d -> d.permanentHealthRegenBonus),
                    Codec.LONG.fieldOf("current_qi").forGetter(d -> d.currentQi),
                    Codec.LONG.fieldOf("permanent_max_qi_bonus").forGetter(d -> d.permanentMaxQiBonus),
                    Codec.DOUBLE.fieldOf("permanent_qi_regen_bonus").forGetter(d -> d.permanentQiRegenBonus),
                    Codec.LONG.fieldOf("current_body").forGetter(d -> d.currentBody),
                    Codec.LONG.fieldOf("permanent_max_body_bonus").forGetter(d -> d.permanentMaxBodyBonus),
                    Codec.DOUBLE.fieldOf("permanent_body_regen_bonus").forGetter(d -> d.permanentBodyRegenBonus)
            ).apply(instance, (curHealth, permHealthBonus, permHealthRegen, curQi, permMaxQi, permQiRegen, curBody, permMaxBody, permBodyRegen) -> {
                PlayerResourceAttachment data = new PlayerResourceAttachment();
                data.currentHealth = curHealth;
                data.permanentHealthBonus = permHealthBonus;
                data.permanentHealthRegenBonus = permHealthRegen;
                data.currentQi = curQi;
                data.permanentMaxQiBonus = permMaxQi;
                data.permanentQiRegenBonus = permQiRegen;
                data.currentBody = curBody;
                data.permanentMaxBodyBonus = permMaxBody;
                data.permanentBodyRegenBonus = permBodyRegen;
                data.recalculateAll();
                return data;
            })
    );
}
