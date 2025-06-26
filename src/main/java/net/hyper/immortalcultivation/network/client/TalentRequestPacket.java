package net.hyper.immortalcultivation.network.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerTalentAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Request packet for syncing talent data from server to client.
 * Sent from client to server when client needs updated talent information.
 * This triggers the server to send a TalentSyncPacket back to the client.
 */
public record TalentRequestPacket() implements CustomPacketPayload {

    public static final Type<TalentRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "talent_request")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TalentRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new TalentRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(TalentRequestPacket packet, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    PlayerTalentAttachment talents = serverPlayer.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

                    if (talents != null) {
                        // Set player reference and force sync
                        talents.setPlayer(serverPlayer);
                        talents.forceSync();

                        ImmortalCultivation.LOGGER.debug("Forced talent sync for player: {}",
                            serverPlayer.getName().getString());
                    } else {
                        ImmortalCultivation.LOGGER.warn("Player {} requested talent sync but has no talent attachment",
                            serverPlayer.getName().getString());
                    }
                }
            });
        }
    }
}