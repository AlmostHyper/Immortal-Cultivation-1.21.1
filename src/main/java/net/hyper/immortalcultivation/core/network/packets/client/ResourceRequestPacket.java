package net.hyper.immortalcultivation.core.network.packets.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.capability.attachments.PlayerResourceAttachment;
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
public record ResourceRequestPacket() implements CustomPacketPayload {

    public static final Type<ResourceRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "resource_request")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new ResourceRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(ResourceRequestPacket packet, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    PlayerResourceAttachment resourceAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());

                    if (resourceAttachment != null) {
                        // Set player reference and force sync
                        resourceAttachment.setPlayer(serverPlayer);
                        resourceAttachment.forceSync();

                        ImmortalCultivation.LOGGER.debug("Forced resource sync for player: {}",
                            serverPlayer.getName().getString());
                    } else {
                        ImmortalCultivation.LOGGER.warn("Player {} requested resource sync but has no resource attachment",
                            serverPlayer.getName().getString());
                    }
                }
            });
        }
    }
}