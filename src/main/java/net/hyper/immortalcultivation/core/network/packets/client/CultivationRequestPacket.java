package net.hyper.immortalcultivation.core.network.packets.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Request packet for syncing cultivation data from server to client.
 * Sent from client to server when client needs updated cultivation information.
 * This triggers the server to send a CultivationSyncPacket back to the client.
 */
public record CultivationRequestPacket() implements CustomPacketPayload {

    public static final Type<CultivationRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_request")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new CultivationRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(CultivationRequestPacket packet, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    PlayerCultivationAttachment cultivation = serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

                    if (cultivation != null) {
                        // Set player reference and force sync
                        cultivation.setPlayer(serverPlayer);
                        cultivation.forceSync();

                        ImmortalCultivation.LOGGER.debug("Forced cultivation sync for player: {}",
                            serverPlayer.getName().getString());
                    } else {
                        ImmortalCultivation.LOGGER.warn("Player {} requested cultivation sync but has no cultivation attachment",
                            serverPlayer.getName().getString());
                    }
                }
            });
        }
    }
}