package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerTalentSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.Optional;

public class PlayerTalentAttachment implements ISyncable {

    // === INSTANCES === //

    @Nullable
    private transient Player player;

    // --- Talent Data ---
    private SpiritualRootType spiritualRootType = SpiritualRootType.MORTAL_ROOT;
    private boolean spiritualRootAssigned = false;
    private PhysiqueType physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
    private boolean physiqueAssigned = false;

    // --- System Data ---
    private transient boolean isDirty = false;
    private transient PlayerTalentAttachment lastSyncedState = null;

    // === CONSTRUCTORS === //

    public PlayerTalentAttachment() {}

    // === GETTERS & SETTERS === //

    public void setPlayer(Player player) {
        this.player = player;
    }

    public SpiritualRootType getSpiritualRootType() {
        return spiritualRootType;
    }

    public void assignSpiritualRoot(SpiritualRootType type) {
        if (!spiritualRootAssigned) {
            this.spiritualRootType = type;
            this.spiritualRootAssigned = true;
            markDirty();
        }
    }

    public boolean hasSpiritualRootAssigned() {
        return spiritualRootAssigned;
    }

    public float getSpiritualRootMultiplier() {
        return spiritualRootType.getCultivationMultiplier();
    }

    public PhysiqueType getPhysiqueType() {
        return physiqueType;
    }

    public void assignPhysique(PhysiqueType type) {
        if (!physiqueAssigned) {
            this.physiqueType = type;
            this.physiqueAssigned = true;
            markDirty();
        }
    }

    public boolean hasPhysiqueAssigned() {
        return physiqueAssigned;
    }

    public float getPhysiqueMultiplier() {
        return physiqueType.getCultivationMultiplier();
    }
    
    public void updateFromServer(SpiritualRootType spiritualRootType, boolean spiritualRootAssigned,
                                 PhysiqueType physiqueType, boolean physiqueAssigned) {
        this.spiritualRootType = spiritualRootType;
        this.spiritualRootAssigned = spiritualRootAssigned;
        this.physiqueType = physiqueType;
        this.physiqueAssigned = physiqueAssigned;
    }

    // === IMPLEMENTATIONS === //

    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        if (player == null || player.level().isClientSide()) {
            return Optional.empty();
        }

        if (lastSyncedState == null) {
            isFullSync = true;
        }

        byte changedFields = 0;
        if (isFullSync || this.spiritualRootType != lastSyncedState.spiritualRootType) {
            changedFields |= PlayerTalentSyncPacket.SPIRITUAL_ROOT_TYPE_MASK;
        }
        if (isFullSync || this.spiritualRootAssigned != lastSyncedState.spiritualRootAssigned) {
            changedFields |= PlayerTalentSyncPacket.SPIRITUAL_ROOT_ASSIGNED_MASK;
        }
        if (isFullSync || this.physiqueType != lastSyncedState.physiqueType) {
            changedFields |= PlayerTalentSyncPacket.PHYSIQUE_TYPE_MASK;
        }
        if (isFullSync || this.physiqueAssigned != lastSyncedState.physiqueAssigned) {
            changedFields |= PlayerTalentSyncPacket.PHYSIQUE_ASSIGNED_MASK;
        }

        if (changedFields > 0) {
            this.lastSyncedState = this.copy();
            return Optional.of(new PlayerTalentSyncPacket(changedFields, spiritualRootType, spiritualRootAssigned, physiqueType, physiqueAssigned));
        }

        return Optional.empty();
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }

    @Override
    public void markDirty() {
        this.isDirty = true;
    }

    @Override
    public void clearDirty() {
        this.isDirty = false;
    }

    // === HELPER METHODS === //

    public PlayerTalentAttachment copy() {
        PlayerTalentAttachment copy = new PlayerTalentAttachment();
        copy.spiritualRootType = this.spiritualRootType;
        copy.spiritualRootAssigned = this.spiritualRootAssigned;
        copy.physiqueType = this.physiqueType;
        copy.physiqueAssigned = this.physiqueAssigned;
        return copy;
    }

    // === SERIALIZATION === //

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerTalentAttachment> {
        @Override
        public PlayerTalentAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerTalentAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerTalentAttachment: {}", error))
                    .map(com.mojang.datafixers.util.Pair::getFirst)
                    .orElseGet(PlayerTalentAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }
            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerTalentAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerTalentAttachment: {}", error))
                    .orElse(new CompoundTag());
        }
    }

    // === CODEC === //

    public static final Codec<PlayerTalentAttachment> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("spiritual_root_type").forGetter(d -> d.spiritualRootType.name()),
                    Codec.BOOL.fieldOf("spiritual_root_assigned").forGetter(d -> d.spiritualRootAssigned),
                    Codec.STRING.fieldOf("physique_type").forGetter(d -> d.physiqueType.name()),
                    Codec.BOOL.fieldOf("physique_assigned").forGetter(d -> d.physiqueAssigned)
            ).apply(instance, (rootType, rootAssigned, physiqueType, physiqueAssigned) -> {
                PlayerTalentAttachment data = new PlayerTalentAttachment();
                try {
                    data.spiritualRootType = SpiritualRootType.valueOf(rootType);
                } catch (IllegalArgumentException e) {
                    data.spiritualRootType = SpiritualRootType.MORTAL_ROOT;
                }
                data.spiritualRootAssigned = rootAssigned;
                try {
                    data.physiqueType = PhysiqueType.valueOf(physiqueType);
                } catch (IllegalArgumentException e) {
                    data.physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
                }
                data.physiqueAssigned = physiqueAssigned;
                return data;
            })
    );
}
