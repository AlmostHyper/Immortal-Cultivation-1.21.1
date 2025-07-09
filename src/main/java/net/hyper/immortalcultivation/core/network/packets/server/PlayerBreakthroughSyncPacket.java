package net.hyper.immortalcultivation.core.network.packets.server;

import net.hyper.immortalcultivation.common.attachments.PlayerBreakthroughAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerBreakthroughSyncPacket(CompoundTag breakthroughData) implements ISyncPacket {
    public static final ResourceLocation ID = new ResourceLocation("immortalcultivation", "player_breakthrough_sync");
    public static final CustomPacketPayload.Type<PlayerBreakthroughSyncPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, PlayerBreakthroughSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeNbt(packet.breakthroughData()),
            (buf) -> new PlayerBreakthroughSyncPacket(buf.readNbt())
    );

    @Override
    public CustomPacketPayload.Type<PlayerBreakthroughSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            PlayerBreakthroughAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
            if (attachment != null) {
                attachment.applySync(this.breakthroughData);
            }
        }
    }
}
