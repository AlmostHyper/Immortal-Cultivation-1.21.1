package net.hyper.immortalcultivation.network.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.network.NetworkCodecUtils;
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
 * Updated to work with the unified PlayerCultivationAttachment structure
 * and supports both Qi and Body cultivation paths.
 */
public record BreakthroughSyncPacket(PlayerCultivationAttachment.CultivationType cultivationType, String realmId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BreakthroughSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "breakthrough_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BreakthroughSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            // Use the improved NetworkCodecUtils for enum encoding
            NetworkCodecUtils.ordinalEnumCodec(PlayerCultivationAttachment.CultivationType.class),
            BreakthroughSyncPacket::cultivationType,
            ByteBufCodecs.STRING_UTF8,
            BreakthroughSyncPacket::realmId,
            BreakthroughSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(BreakthroughSyncPacket packet, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player != null) {
                    // Get the unified cultivation attachment
                    var cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    if (cultivationAttachment != null) {
                        // Get the new realm from the registry
                        var newRealm = ICModCultivationRealms.get(packet.realmId());
                        if (newRealm != null) {
                            // Update the appropriate realm on client side based on cultivation type
                            switch (packet.cultivationType()) {
                                case QI -> {
                                    cultivationAttachment.setCurrentQiRealm(newRealm);
                                    ImmortalCultivation.LOGGER.info("Client synchronized Qi realm to: {}",
                                        newRealm.getDisplayName());
                                }
                                case BODY -> {
                                    cultivationAttachment.setCurrentBodyRealm(newRealm);
                                    ImmortalCultivation.LOGGER.info("Client synchronized Body realm to: {}",
                                        newRealm.getDisplayName());
                                }
                            }

                            // Set the player reference to ensure client-side methods work
                            cultivationAttachment.setPlayer(player);

                            // Show success message with cultivation type context
                            String cultivationTypeName = packet.cultivationType().getDisplayName();
                            player.sendSystemMessage(
                                Component.literal("§6" + cultivationTypeName + " breakthrough successful! ").append(
                                    Component.literal("New realm: " + newRealm.getDisplayName()).withStyle(style ->
                                        style.withColor(0xFFD700)))); // Gold color

                            ImmortalCultivation.LOGGER.info("Client synchronized {} realm to: {}",
                                cultivationTypeName, newRealm.getDisplayName());
                        } else {
                            // Debug message if realm not found
                            ImmortalCultivation.LOGGER.warn("Failed to sync {} realm: Unknown realm ID {}",
                                packet.cultivationType().getDisplayName(), packet.realmId());
                        }
                    } else {
                        ImmortalCultivation.LOGGER.warn("Failed to sync realm: Player has no cultivation attachment");
                    }
                }
            });
        }
    }
}