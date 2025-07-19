package net.hyper.immortalcultivation.core.network.packets.server;

import net.hyper.immortalcultivation.core.attachments.PlayerBreakthroughAttachment;
import net.hyper.immortalcultivation.core.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

public record PlayerBreakthroughSyncPacket(CompoundTag breakthroughData) implements ISyncPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("immortalcultivation", "player_breakthrough_sync");
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
    public void handleOnClient(Player player) {
        if (player != null) {
            PlayerBreakthroughAttachment attachment = player.getData(AttachmentRegistry.PLAYER_BREAKTHROUGH.get());
            if (attachment != null) {
                attachment.applySync(this.breakthroughData);

            }
        }
    }
}
