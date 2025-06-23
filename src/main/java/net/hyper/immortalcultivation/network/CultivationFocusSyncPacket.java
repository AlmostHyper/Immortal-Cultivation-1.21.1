package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.ui.screens.CultivationStatsScreen;
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
                        boolean oldFocus = cultivationFocus.isFocusedOnQi();

                        // Update focus from server
                        cultivationFocus.updateFromServer(msg.cultivationFocus);

                        boolean newFocus = cultivationFocus.isFocusedOnQi();

                        ImmortalCultivation.LOGGER.info("Client received focus update: {} cultivation",
                            newFocus ? "Qi" : "Body");

                        // CRITICAL: Refresh GUI if focus changed and CultivationStatsScreen is open
                        if (oldFocus != newFocus && mc.screen instanceof CultivationStatsScreen statsScreen) {
                            ImmortalCultivation.LOGGER.info("Refreshing CultivationStatsScreen after focus change");
                            statsScreen.init(); // This will rebuild the GUI with new focus state
                        }
                    }
                }
            });
        }
    }
}
