package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.content.item.pills.PillConsumptionManager;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerPillConsumptionSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.Optional;

public class PlayerPillConsumptionAttachment implements ISyncable {

    // === INSTANCES === //
    @Nullable
    private transient Player player;
    private final PillConsumptionManager pillConsumptionManager;

    // === CONSTRUCTORS === //
    public PlayerPillConsumptionAttachment() {
        this.pillConsumptionManager = new PillConsumptionManager();
    }

    private PlayerPillConsumptionAttachment(PillConsumptionManager pillConsumptionManager) {
        this.pillConsumptionManager = pillConsumptionManager != null ? pillConsumptionManager : new PillConsumptionManager();
    }

    // === GETTERS & SETTERS === //
    public void setPlayer(Player player) {
        this.player = player;
    }

    public PillConsumptionManager getPillConsumptionManager() {
        return pillConsumptionManager;
    }

    public void applySync(CompoundTag pillConsumptionData) {
        if (pillConsumptionData != null && !pillConsumptionData.isEmpty()) {
            this.pillConsumptionManager.deserializeNBT(pillConsumptionData);
        }
    }

    // === IMPLEMENTATIONS === //
    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        // Full sync is ignored here as we always send the full data block
        if (player == null || player.level().isClientSide() || !isDirty()) {
            return Optional.empty();
        }
        // When a packet is created, we can clear the dirty flag.
        CompoundTag data = pillConsumptionManager.serializeNBT();
        clearDirty();
        return Optional.of(new PlayerPillConsumptionSyncPacket(data));
    }

    @Override
    public boolean isDirty() {
        return pillConsumptionManager.isDirty();
    }

    @Override
    public void markDirty() {
        // The manager handles its own dirty state.
    }

    @Override
    public void clearDirty() {
        pillConsumptionManager.clearDirty();
    }

    // === SERIALIZATION === //
    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerPillConsumptionAttachment> {
        @Override
        public PlayerPillConsumptionAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerPillConsumptionAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerPillConsumptionAttachment: {}", error))
                    .map(com.mojang.datafixers.util.Pair::getFirst)
                    .orElseGet(PlayerPillConsumptionAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }
            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerPillConsumptionAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerPillConsumptionAttachment: {}", error))
                    .orElse(new CompoundTag());
        }
    }

    // === CODEC === //
    public static final Codec<PlayerPillConsumptionAttachment> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CompoundTag.CODEC.optionalFieldOf("pill_consumption", new CompoundTag())
                            .forGetter(attachment -> attachment.pillConsumptionManager.serializeNBT())
            ).apply(instance, (pillConsumptionData) -> {
                PillConsumptionManager manager = new PillConsumptionManager();
                if (pillConsumptionData != null && !pillConsumptionData.isEmpty()) {
                    manager.deserializeNBT(pillConsumptionData);
                }
                return new PlayerPillConsumptionAttachment(manager);
            })
    );
}
