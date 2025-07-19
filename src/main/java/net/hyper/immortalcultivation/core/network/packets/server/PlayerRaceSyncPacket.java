package net.hyper.immortalcultivation.core.network.packets.server;

import net.hyper.immortalcultivation.core.attachments.PlayerRaceAttachment;
import net.hyper.immortalcultivation.core.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

public record PlayerRaceSyncPacket(CompoundTag raceData) implements ISyncPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("immortalcultivation", "player_race_sync");
    public static final CustomPacketPayload.Type<PlayerRaceSyncPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, PlayerRaceSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeNbt(packet.raceData()),
            (buf) -> new PlayerRaceSyncPacket(buf.readNbt())
    );

    @Override
    public CustomPacketPayload.Type<PlayerRaceSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player != null) {
            PlayerRaceAttachment attachment = player.getData(AttachmentRegistry.PLAYER_RACE.get());
            if (attachment != null) {
                attachment.applySync(this.raceData);

            }
        }
    }
}
