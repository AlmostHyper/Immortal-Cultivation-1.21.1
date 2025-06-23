package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CultivationBonusSyncPacket(long cultivationBonus) implements CustomPacketPayload {
    public static final Type<CultivationBonusSyncPacket> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_bonus_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationBonusSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, CultivationBonusSyncPacket::cultivationBonus,
            CultivationBonusSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleClient(CultivationBonusSyncPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            ctx.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player != null) {
                    var cultivationBonus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());
                    if (cultivationBonus != null) {
                        cultivationBonus.updateFromServer(msg.cultivationBonus);
                    }
                }

                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§6[DEBUG] [CultivationBonus] Client received: " + msg.cultivationBonus));

            });
        }
    }
}
