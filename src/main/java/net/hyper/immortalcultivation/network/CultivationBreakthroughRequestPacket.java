package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.ByteBufCodecs;


import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CultivationBreakthroughRequestPacket(PlayerRealmAttachment.BreakthroughType breakthroughType) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CultivationBreakthroughRequestPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "breakthrough_attempt"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationBreakthroughRequestPacket> STREAM_CODEC =
        StreamCodec.composite(
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
                    PlayerRealmAttachment realmAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_REALM.get());

                    if (realmAttachment != null) {
                        PlayerRealmAttachment.BreakthroughResult result = realmAttachment.attemptBreakthrough(packet.breakthroughType());

                        // Send result message to player
                        if (result.success()) {
                            serverPlayer.sendSystemMessage(Component.literal("Breakthrough successful! ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(result.message()).withStyle(ChatFormatting.GOLD)));

                            // SYNC: Send realm update to client
                            ICModNetwork.sendToPlayer(new RealmSyncPacket(result.newRealm().getId()), serverPlayer);

                            // Also sync cultivation attachment in case max cultivation changed
                            PlayerCultivationAttachment cultivation = serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                            if (cultivation != null) {
                                cultivation.forceSync();
                            }

                        } else {
                            serverPlayer.sendSystemMessage(Component.literal("Breakthrough failed: ")
                                .withStyle(ChatFormatting.RED)
                                .append(Component.literal(result.message()).withStyle(ChatFormatting.GRAY)));
                        }
                    }
                }
            });
        }
    }
}
