package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.attachment.PlayerQiCultivationAttachment;
import net.hyper.immortalcultivation.attachment.PlayerBodyCultivationAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Packet for requesting a cultivation breakthrough.
 * This packet includes both the cultivation type (Qi or Body) and the breakthrough type,
 * allowing players to attempt breakthroughs in either cultivation path.
 */
public record CultivationBreakthroughRequestPacket(
    PlayerRealmAttachment.CultivationType cultivationType,
    PlayerRealmAttachment.BreakthroughType breakthroughType
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CultivationBreakthroughRequestPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "breakthrough_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationBreakthroughRequestPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(
                ordinal -> PlayerRealmAttachment.CultivationType.values()[ordinal],
                PlayerRealmAttachment.CultivationType::ordinal
            ), CultivationBreakthroughRequestPacket::cultivationType,
            ByteBufCodecs.VAR_INT.map(
                ordinal -> PlayerRealmAttachment.BreakthroughType.values()[ordinal],
                PlayerRealmAttachment.BreakthroughType::ordinal
            ), CultivationBreakthroughRequestPacket::breakthroughType,
            CultivationBreakthroughRequestPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(CultivationBreakthroughRequestPacket packet, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    ImmortalCultivation.LOGGER.info("Processing {} breakthrough request from player: {}",
                        packet.cultivationType().getDisplayName(),
                        serverPlayer.getName().getString());

                    PlayerRealmAttachment realmAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_REALM.get());

                    if (realmAttachment == null) {
                        ImmortalCultivation.LOGGER.error("RealmAttachment is null for player: {}",
                            serverPlayer.getName().getString());
                        serverPlayer.sendSystemMessage(
                            Component.literal("Error: Could not access realm data")
                                .withStyle(ChatFormatting.RED));
                        return;
                    }

                    // Debug current state for the specific cultivation type
                    ImmortalCultivation.LOGGER.info("RealmAttachment found - {} Realm: {}, Player set: {}",
                        packet.cultivationType().getDisplayName(),
                        realmAttachment.getCurrentRealm(packet.cultivationType()).getDisplayName(),
                        realmAttachment.getPlayer() != null);

                    // Ensure player is set
                    realmAttachment.setPlayer(serverPlayer);

                    // Verify realms are set, if not set to default mortal realm
                    if (realmAttachment.getCurrentRealm(packet.cultivationType()) == null) {
                        ImmortalCultivation.LOGGER.warn("Current {} realm is null, setting to default mortal realm",
                            packet.cultivationType().getDisplayName());
                        realmAttachment.setCurrentRealm(packet.cultivationType(), ICModCultivationRealms.MORTAL, false);
                    }

                    // Attempt the breakthrough for the specified cultivation type
                    PlayerRealmAttachment.BreakthroughResult result = realmAttachment.attemptBreakthrough(
                        packet.cultivationType(),
                        packet.breakthroughType()
                    );

                    // Send result message to player
                    if (result.success() && result.newRealm() != null) {
                        serverPlayer.sendSystemMessage(
                            Component.literal("Breakthrough successful! ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(result.message())
                                    .withStyle(ChatFormatting.GOLD)));

                        // Sync the new realm to client with the correct cultivation type
                        syncBreakthroughToClient(serverPlayer, packet.cultivationType(), result.newRealm());

                        // Also sync the appropriate cultivation attachment
                        syncCultivationAttachment(serverPlayer, packet.cultivationType());

                    } else {
                        serverPlayer.sendSystemMessage(
                            Component.literal("Breakthrough failed: ")
                                .withStyle(ChatFormatting.RED)
                                .append(Component.literal(result.message())
                                    .withStyle(ChatFormatting.GRAY)));
                    }
                }
            });
        }
    }

    /**
     * Sync the successful breakthrough to the client.
     * Now properly includes the cultivation type so client knows which realm to update.
     */
    private static void syncBreakthroughToClient(ServerPlayer player,
                                                 PlayerRealmAttachment.CultivationType type,
                                                 net.hyper.immortalcultivation.cultivation.realms.CultivationRealm newRealm) {
        // Send a sync packet that includes both the cultivation type and realm ID
        ICModNetwork.sendToPlayer(
            new CultivationBreakthroughSyncPacket(
                type, // Send the actual cultivation type enum
                newRealm.getId()
            ),
            player
        );

        ImmortalCultivation.LOGGER.info("Syncing {} breakthrough to client: {}",
            type.getDisplayName(), newRealm.getDisplayName());
    }

    /**
     * Force sync the appropriate cultivation attachment after breakthrough.
     * This ensures the client has the latest cultivation data after realm advancement.
     */
    private static void syncCultivationAttachment(ServerPlayer player, PlayerRealmAttachment.CultivationType type) {
        if (type == PlayerRealmAttachment.CultivationType.QI) {
            PlayerQiCultivationAttachment qiCultivation = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiCultivation != null) {
                qiCultivation.forceSync();
                ImmortalCultivation.LOGGER.debug("Force synced Qi cultivation attachment for player: {}",
                    player.getName().getString());
            }
        } else {
            PlayerBodyCultivationAttachment bodyCultivation = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyCultivation != null) {
                bodyCultivation.forceSync();
                ImmortalCultivation.LOGGER.debug("Force synced Body cultivation attachment for player: {}",
                    player.getName().getString());
            }
        }
    }
}