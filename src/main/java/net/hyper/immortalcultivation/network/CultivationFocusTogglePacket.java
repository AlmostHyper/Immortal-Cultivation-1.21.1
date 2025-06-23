package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CultivationFocusTogglePacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CultivationFocusTogglePacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_focus_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationFocusTogglePacket> STREAM_CODEC =
        StreamCodec.unit(new CultivationFocusTogglePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(CultivationFocusTogglePacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var focusAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
                    if (focusAttachment != null) {
                        ImmortalCultivation.LOGGER.info("Server received focus toggle request from player {}",
                            serverPlayer.getName().getString());

                        // Toggle focus on the SERVER side (this is the key fix!)
                        focusAttachment.toggleFocus();

                        ImmortalCultivation.LOGGER.info("Server toggled focus to: {} cultivation",
                            focusAttachment.getCurrentFocus().getDisplayName());
                    }
                }
            });
        }
    }
}