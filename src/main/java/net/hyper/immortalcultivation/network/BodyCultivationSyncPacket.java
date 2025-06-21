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

public record BodyCultivationSyncPacket(long bodyCultivation) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BodyCultivationSyncPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "body_cultivation_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BodyCultivationSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, BodyCultivationSyncPacket::bodyCultivation,
            BodyCultivationSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleClient(BodyCultivationSyncPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            ctx.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player != null) {
                    var bodyCultivation = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
                    if (bodyCultivation != null) {
                        bodyCultivation.updateFromServer(msg.bodyCultivation);
                    }
                }

                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§6[DEBUG] [BodyCultivation] Client received: " + msg.bodyCultivation));

            });
        }
    }
}
