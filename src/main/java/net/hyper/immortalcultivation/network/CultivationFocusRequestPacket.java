package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CultivationFocusRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CultivationFocusRequestPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_focus_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationFocusRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new CultivationFocusRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(CultivationFocusRequestPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var cultivationFocus = serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
                    if (cultivationFocus != null) {
                        cultivationFocus.forceSync();
                    }
                }
            });
        }
    }
}
