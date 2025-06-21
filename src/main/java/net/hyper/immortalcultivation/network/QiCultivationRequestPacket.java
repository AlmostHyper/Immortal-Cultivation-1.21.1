package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QiCultivationRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QiCultivationRequestPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "qi_cultivation_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QiCultivationRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new QiCultivationRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(QiCultivationRequestPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var qiCultivation = serverPlayer.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
                    if (qiCultivation != null) {
                        qiCultivation.forceSync();
                    }
                }
            });
        }
    }
}
