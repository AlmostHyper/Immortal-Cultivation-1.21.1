package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BodyRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BodyRequestPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "body_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BodyRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new BodyRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(BodyRequestPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var body = serverPlayer.getData(ICModAttachmentTypes.PLAYER_BODY.get());
                    if (body != null) {
                        body.forceSync();
                    }
                }
            });
        }
    }
}
