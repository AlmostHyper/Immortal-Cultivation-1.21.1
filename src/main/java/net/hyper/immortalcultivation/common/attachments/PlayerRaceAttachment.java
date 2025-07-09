package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerRaceSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.Optional;

public class PlayerRaceAttachment implements ISyncable {

    // === INSTANCES === //

    @Nullable
    private transient Player player;

    // --- Race Data ---
    private RaceType raceType = RaceType.HUMAN;
    private boolean raceAssigned = false;

    // --- System Data ---
    private transient boolean isDirty = false;
    private transient PlayerRaceAttachment lastSyncedState = null;

    // === CONSTRUCTORS === //

    public PlayerRaceAttachment() {}

    // === GETTERS & SETTERS === //

    public void setPlayer(Player player) {
        this.player = player;
    }

    public RaceType getRaceType() {
        return raceType;
    }

    public void assignRace(RaceType type) {
        if (!raceAssigned) {
            this.raceType = type;
            this.raceAssigned = true;
            markDirty();
        }
    }

    public boolean hasRaceAssigned() {
        return raceAssigned;
    }

    public float getQiMultiplier() {
        return raceType.getQiMultiplier();
    }

    public float getBodyMultiplier() {
        return raceType.getBodyMultiplier();
    }

    public void updateFromServer(RaceType raceType, boolean raceAssigned) {
        this.raceType = raceType;
        this.raceAssigned = raceAssigned;
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
        if (isFullSync || this.raceType != lastSyncedState.raceType) {
            changedFields |= PlayerRaceSyncPacket.RACE_TYPE_MASK;
        }
        if (isFullSync || this.raceAssigned != lastSyncedState.raceAssigned) {
            changedFields |= PlayerRaceSyncPacket.RACE_ASSIGNED_MASK;
        }

        if (changedFields > 0) {
            this.lastSyncedState = this.copy();
            return Optional.of(new PlayerRaceSyncPacket(changedFields, raceType, raceAssigned));
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

    public PlayerRaceAttachment copy() {
        PlayerRaceAttachment copy = new PlayerRaceAttachment();
        copy.raceType = this.raceType;
        copy.raceAssigned = this.raceAssigned;
        return copy;
    }

    // === SERIALIZATION === //

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerRaceAttachment> {
        @Override
        public PlayerRaceAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerRaceAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerRaceAttachment: {}", error))
                    .map(com.mojang.datafixers.util.Pair::getFirst)
                    .orElseGet(PlayerRaceAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }
            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerRaceAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerRaceAttachment: {}", error))
                    .orElse(new CompoundTag());
        }
    }

    // === CODEC === //
    
    public static final Codec<PlayerRaceAttachment> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("race_type").forGetter(d -> d.raceType.name()),
                    Codec.BOOL.fieldOf("race_assigned").forGetter(d -> d.raceAssigned)
            ).apply(instance, (raceType, raceAssigned) -> {
                PlayerRaceAttachment data = new PlayerRaceAttachment();
                try {
                    data.raceType = RaceType.valueOf(raceType);
                } catch (IllegalArgumentException e) {
                    data.raceType = RaceType.HUMAN;
                }
                data.raceAssigned = raceAssigned;
                return data;
            })
    );
}
