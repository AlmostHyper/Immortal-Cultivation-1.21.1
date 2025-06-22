package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpiritualRootRequestPacket() implements CustomPacketPayload {
    public static final Type<SpiritualRootRequestPacket> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "spiritual_root_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpiritualRootRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new SpiritualRootRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(SpiritualRootRequestPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var realm = serverPlayer.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
                    if (realm != null) {
                        realm.forceSync();
                    }
                }
            });
        }
    }
}
