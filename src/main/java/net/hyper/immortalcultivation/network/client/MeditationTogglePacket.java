package net.hyper.immortalcultivation.network.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerResourceAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet for toggling meditation state.
 * Sent from client to server when player toggles meditation in the GUI.
 */
public record MeditationTogglePacket() implements CustomPacketPayload {

    public static final Type<MeditationTogglePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "meditation_toggle")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, MeditationTogglePacket> STREAM_CODEC =
        StreamCodec.unit(new MeditationTogglePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(MeditationTogglePacket packet, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    PlayerResourceAttachment resources = serverPlayer.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());

                    if (resources == null) {
                        serverPlayer.sendSystemMessage(
                            Component.literal("Error: Resource data not found!")
                                .withStyle(ChatFormatting.RED)
                        );
                        ImmortalCultivation.LOGGER.warn("Player {} attempted to toggle meditation but has no resource attachment",
                            serverPlayer.getName().getString());
                        return;
                    }

                    // Set player reference to ensure server-side methods work
                    resources.setPlayer(serverPlayer);
                }
            });
        }
    }
}