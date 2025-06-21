package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BodyCultivationRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BodyCultivationRequestPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "body_cultivation_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BodyCultivationRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new BodyCultivationRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(BodyCultivationRequestPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var bodyCultivation = serverPlayer.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
                    if (bodyCultivation != null) {
                        bodyCultivation.forceSync();
                    }
                }
            });
        }
    }
}
