package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record CultivationBreakthroughSyncPacket(String realmId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CultivationBreakthroughSyncPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "breakthrough_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationBreakthroughSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
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
                            // Update the realm on client side (don't trigger events)
                            realmAttachment.setCurrentRealm(newRealm, false);

                            // Set the player reference to ensure client-side methods work
                            realmAttachment.setPlayer(player);

                            // Optional: Show success message on client
                            player.sendSystemMessage(
                                Component.literal("§6Realm synchronized: " + newRealm.getDisplayName()));
                        } else {
                            // Debug message if realm not found
                            ImmortalCultivation.LOGGER.warn("Failed to sync realm: Unknown realm ID {}", packet.realmId());
                        }
                    } else {
                        ImmortalCultivation.LOGGER.warn("Failed to sync realm: Player has no realm attachment");
                    }
                }
            });
        }
    }
}
