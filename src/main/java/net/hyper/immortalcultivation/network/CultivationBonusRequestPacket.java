package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CultivationBonusRequestPacket() implements CustomPacketPayload {
    public static final Type<CultivationBonusRequestPacket> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_bonus_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationBonusRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new CultivationBonusRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(CultivationBonusRequestPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var cultivationBonus = serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());
                    if (cultivationBonus != null) {
                        cultivationBonus.forceSync();
                    }
                }
            });
        }
    }
}
