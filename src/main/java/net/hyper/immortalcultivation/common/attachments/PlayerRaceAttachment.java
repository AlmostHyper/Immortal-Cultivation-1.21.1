package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.IPlayerInitializable;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.hyper.immortalcultivation.core.network.packets.server.OpenRaceSelectionSyncPacket;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerRaceSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.Optional;

public class PlayerRaceAttachment implements ISyncable, IPlayerInitializable {

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

    // === IMPLEMENTATIONS === //

    @Override
    public void onPlayerLogin(ServerPlayer player) {
        if (!hasRaceAssigned()) {
            ICModPacketRegistry.sendToPlayer(new OpenRaceSelectionSyncPacket(), player);
            player.sendSystemMessage(Component.literal("Welcome, young cultivator! Choose your race to begin your journey.").withStyle(ChatFormatting.GOLD));
            ImmortalCultivation.LOGGER.info("Sent race selection screen to new player {}", player.getName().getString());
        } else {
            ImmortalCultivation.LOGGER.debug("Player {} logged in with race: {}", player.getName().getString(), getRaceType().getDisplayName());
        }
    }

    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        if (player == null) {
            return Optional.empty();
        }

        if (!isFullSync && !isDirty()) {
            return Optional.empty();
        }

        CompoundTag data = (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this)
            .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerRaceAttachment: {}", error))
            .orElse(new CompoundTag());

        return Optional.of(new PlayerRaceSyncPacket(data));
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

    public void applySync(CompoundTag data) {
        CODEC.decode(NbtOps.INSTANCE, data)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerRaceAttachment from sync: {}", error))
                .ifPresent(pair -> {
                    PlayerRaceAttachment decoded = pair.getFirst();
                    this.raceType = decoded.raceType;
                    this.raceAssigned = decoded.raceAssigned;
                });
    }

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
