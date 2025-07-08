package net.hyper.immortalcultivation.core.network.packets.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.common.capability.attachments.PlayerPillConsumptionAttachment;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sync packet for pill consumption data only.
 * Lightweight packet that only syncs when pills are consumed, reducing network overhead.
 */
public record PillConsumptionSyncPacket(
    CompoundTag pillConsumptionData
) implements CustomPacketPayload {

    public static final Type<PillConsumptionSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "pill_consumption_sync")
    );

    public static final StreamCodec<ByteBuf, PillConsumptionSyncPacket> STREAM_CODEC = StreamCodec.of(
        PillConsumptionSyncPacket::encode,
        PillConsumptionSyncPacket::decode
    );

    private static void encode(ByteBuf buf, PillConsumptionSyncPacket packet) {
        ByteBufCodecs.COMPOUND_TAG.encode(buf, packet.pillConsumptionData);
    }

    private static PillConsumptionSyncPacket decode(ByteBuf buf) {
        return new PillConsumptionSyncPacket(
            ByteBufCodecs.COMPOUND_TAG.decode(buf)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(PillConsumptionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            PlayerPillConsumptionAttachment pillConsumption = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

            if (pillConsumption != null) {
                pillConsumption.applySync(packet.pillConsumptionData);
            }
        });
    }
}