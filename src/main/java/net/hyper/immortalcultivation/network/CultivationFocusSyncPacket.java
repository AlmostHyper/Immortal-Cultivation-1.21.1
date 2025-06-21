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

public record CultivationFocusSyncPacket(long cultivationFocus) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CultivationFocusSyncPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_focus_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationFocusSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, CultivationFocusSyncPacket::cultivationFocus,
            CultivationFocusSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleClient(CultivationFocusSyncPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            ctx.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player != null) {
                    var cultivationFocus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
                    if (cultivationFocus != null) {
                        cultivationFocus.updateFromServer(msg.cultivationFocus);
                    }
                }

                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§6[DEBUG] [CultivationFocus] Client received: " + msg.cultivationFocus));

            });
        }
    }
}
