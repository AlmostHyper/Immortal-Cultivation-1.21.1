package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RealmRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RealmRequestPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "realm_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RealmRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new RealmRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(RealmRequestPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var realm = serverPlayer.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                    if (realm != null) {
                        realm.forceSync();
                    }
                }
            });
        }
    }
}
