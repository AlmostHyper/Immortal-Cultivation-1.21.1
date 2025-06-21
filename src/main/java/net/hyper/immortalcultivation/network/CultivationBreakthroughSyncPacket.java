package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet for syncing cultivation realm breakthroughs to the client.
 * Now supports both Qi and Body cultivation paths by including the cultivation type.
 */
public record CultivationBreakthroughSyncPacket(
    PlayerRealmAttachment.CultivationType cultivationType,
    String realmId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CultivationBreakthroughSyncPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "breakthrough_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationBreakthroughSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            // Encode/decode cultivation type as ordinal
            ByteBufCodecs.VAR_INT.map(
                ordinal -> PlayerRealmAttachment.CultivationType.values()[ordinal],
                PlayerRealmAttachment.CultivationType::ordinal
            ), CultivationBreakthroughSyncPacket::cultivationType,
            ByteBufCodecs.STRING_UTF8, CultivationBreakthroughSyncPacket::realmId,
            CultivationBreakthroughSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleClient(CultivationBreakthroughSyncPacket packet, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player != null) {
                    var realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                    if (realmAttachment != null) {
                        // Get the new realm from the registry
                        var newRealm = ICModCultivationRealms.get(packet.realmId());
                        if (newRealm != null) {
                            // Update the appropriate realm on client side (don't trigger events)
                            realmAttachment.setCurrentRealm(packet.cultivationType(), newRealm, false);

                            // Set the player reference to ensure client-side methods work
                            realmAttachment.setPlayer(player);

                            // Show success message with cultivation type context
                            String cultivationTypeName = packet.cultivationType().getDisplayName();
                            player.sendSystemMessage(
                                Component.literal("§6" + cultivationTypeName + " realm synchronized: " + newRealm.getDisplayName()));

                            ImmortalCultivation.LOGGER.info("Client synchronized {} realm to: {}",
                                cultivationTypeName, newRealm.getDisplayName());
                        } else {
                            // Debug message if realm not found
                            ImmortalCultivation.LOGGER.warn("Failed to sync {} realm: Unknown realm ID {}",
                                packet.cultivationType().getDisplayName(), packet.realmId());
                        }
                    } else {
                        ImmortalCultivation.LOGGER.warn("Failed to sync realm: Player has no realm attachment");
                    }
                }
            });
        }
    }
}