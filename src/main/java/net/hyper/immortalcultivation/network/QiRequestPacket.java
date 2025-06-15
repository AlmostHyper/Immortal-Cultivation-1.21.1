package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QiRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QiRequestPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "qi_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QiRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new QiRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(QiRequestPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var qi = serverPlayer.getData(ICModAttachmentTypes.PLAYER_QI.get());
                    if (qi != null) {
                        qi.forceSync();
                    }
                }
            });
        }
    }
}
