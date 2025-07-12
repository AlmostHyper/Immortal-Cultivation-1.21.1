package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class PlayerAttachment<T extends PlayerAttachment<T>> implements ISyncable, IAttachmentHolder {

    // Common fields
    @Nullable
    protected transient Player player;
    protected transient boolean isDirty = false;
    @Nullable
    protected transient T lastSyncedState = null;

    // === PLAYER REFERENCE MANAGEMENT ===

    public void setPlayer(Player player) {
        this.player = player;
        onPlayerSet(player);
    }

    public Player getPlayer() {
        return this.player;
    }

    protected void onPlayerSet(Player player) {
        // Override for specific behavior
    }

    // === DIRTY FLAG MANAGEMENT ===

    @Override
    public boolean isDirty() {
        // Automatic dirty checking by comparing with last synced state
        if (lastSyncedState == null) return true;
        return !this.isEqualTo(lastSyncedState);
    }

    @Override
    public void markDirty() {
        this.isDirty = true;

        // Auto-schedule sync if we have a server player
        if (this.player instanceof ServerPlayer serverPlayer) {
            // Could integrate with your sync system here
        }
    }

    @Override
    public void clearDirty() {
        this.isDirty = false;
        this.lastSyncedState = this.copy();
    }

    // === SERIALIZATION ===

    protected abstract Codec<T> getCodec();

    protected abstract ISyncPacket createSyncPacket(CompoundTag data);

    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        if (player == null) return Optional.empty();
        if (!isFullSync && !isDirty()) return Optional.empty();

        CompoundTag data = serialize();
        return data.isEmpty() ? Optional.empty() : Optional.of(createSyncPacket(data));
    }

    public CompoundTag serialize() {
        return (CompoundTag) getCodec()
            .encodeStart(NbtOps.INSTANCE, (T) this)
            .getOrThrow();
    }

    public void deserialize(CompoundTag tag) {
        getCodec().parse(NbtOps.INSTANCE, tag)
            .resultOrPartial(error ->
                ImmortalCultivation.LOGGER.error("Failed to decode {}: {}",
                    this.getClass().getSimpleName(), error))
            .ifPresent(decoded -> this.copyFrom(decoded));
    }

    public void applySync(CompoundTag data) {
        deserialize(data);
        clearDirty(); // Important: mark as clean after sync
    }

    // === COPY FUNCTIONALITY ===

    public abstract T copy();

    protected abstract void copyFrom(T other);

    protected abstract boolean isEqualTo(T other);

    // === COMMON SERIALIZER ===

    public static class BaseSerializer<T extends PlayerAttachment<T>> implements IAttachmentSerializer<CompoundTag, T> {
        private final Supplier<T> factory;
        private final Codec<T> codec;

        public BaseSerializer(Supplier<T> factory, Codec<T> codec) {
            this.factory = factory;
            this.codec = codec;
        }

        @Override
        public T read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            T attachment = factory.get();

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            attachment.deserialize(tag);
            return attachment;
        }

        @Override
        public CompoundTag write(T attachment, HolderLookup.Provider provider) {
            return attachment.serialize();
        }
    }
}
