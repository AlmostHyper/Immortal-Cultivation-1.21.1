package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PhysiqueRequestPacket() implements CustomPacketPayload {
    public static final Type<PhysiqueRequestPacket> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "physique_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhysiqueRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new PhysiqueRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(PhysiqueRequestPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var realm = serverPlayer.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
                    if (realm != null) {
                        realm.forceSync();
                    }
                }
            });
        }
    }
}
