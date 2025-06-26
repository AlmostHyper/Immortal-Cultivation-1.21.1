package net.hyper.immortalcultivation.network.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.network.NetworkCodecUtils;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Request packet for consuming cultivation pills.
 * Sent from client to server when player consumes a pill item.
 * Updated to use improved NetworkCodecUtils and unified cultivation attachment.
 */
public record ConsumePillRequestPacket(
    PlayerCultivationAttachment.CultivationType cultivationType,
    double additiveBonus,
    double multiplicativeBonus,
    String pillName
) implements CustomPacketPayload {

    public static final Type<ConsumePillRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "consume_pill_request")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ConsumePillRequestPacket> STREAM_CODEC =
        StreamCodec.composite(
            // Use improved NetworkCodecUtils for enum encoding
            NetworkCodecUtils.ordinalEnumCodec(PlayerCultivationAttachment.CultivationType.class),
            ConsumePillRequestPacket::cultivationType,
            ByteBufCodecs.DOUBLE,
            ConsumePillRequestPacket::additiveBonus,
            ByteBufCodecs.DOUBLE,
            ConsumePillRequestPacket::multiplicativeBonus,
            ByteBufCodecs.STRING_UTF8,
            ConsumePillRequestPacket::pillName,
            ConsumePillRequestPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(ConsumePillRequestPacket packet, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    PlayerCultivationAttachment cultivation = serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

                    if (cultivation == null) {
                        serverPlayer.sendSystemMessage(
                            Component.literal("Error: Cultivation data not found!")
                                .withStyle(ChatFormatting.RED)
                        );
                        ImmortalCultivation.LOGGER.warn("Player {} attempted to consume pill but has no cultivation attachment",
                            serverPlayer.getName().getString());
                        return;
                    }

                    // Set player reference to ensure server-side methods work
                    cultivation.setPlayer(serverPlayer);

                    // Apply the pill bonus based on cultivation type
                    switch (packet.cultivationType()) {
                        case QI -> {
                            cultivation.addQiBonus(packet.additiveBonus(), packet.multiplicativeBonus());

                            // Send success message for Qi cultivation
                            Component message = Component.literal("✨ Consumed ")
                                .append(Component.literal(packet.pillName())
                                    .withStyle(ChatFormatting.AQUA))
                                .append("!")
                                .withStyle(ChatFormatting.GREEN);
                            serverPlayer.sendSystemMessage(message);

                            // Show bonus details
                            if (packet.additiveBonus() > 0) {
                                Component bonusMsg = Component.literal("» Qi cultivation rate increased by ")
                                    .append(Component.literal(String.format("+%.2f/sec", packet.additiveBonus()))
                                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                                    .withStyle(ChatFormatting.GRAY);
                                serverPlayer.sendSystemMessage(bonusMsg);
                            }

                            if (packet.multiplicativeBonus() > 0) {
                                Component multMsg = Component.literal("» Qi cultivation multiplier increased by ")
                                    .append(Component.literal(String.format("+%.0f%%", packet.multiplicativeBonus() * 100))
                                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                                    .withStyle(ChatFormatting.GRAY);
                                serverPlayer.sendSystemMessage(multMsg);
                            }
                        }
                        case BODY -> {
                            cultivation.addBodyBonus(packet.additiveBonus(), packet.multiplicativeBonus());

                            // Send success message for Body cultivation
                            Component message = Component.literal("💪 Consumed ")
                                .append(Component.literal(packet.pillName())
                                    .withStyle(ChatFormatting.RED))
                                .append("!")
                                .withStyle(ChatFormatting.GREEN);
                            serverPlayer.sendSystemMessage(message);

                            // Show bonus details
                            if (packet.additiveBonus() > 0) {
                                Component bonusMsg = Component.literal("» Body cultivation rate increased by ")
                                    .append(Component.literal(String.format("+%.2f/sec", packet.additiveBonus()))
                                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                                    .withStyle(ChatFormatting.GRAY);
                                serverPlayer.sendSystemMessage(bonusMsg);
                            }

                            if (packet.multiplicativeBonus() > 0) {
                                Component multMsg = Component.literal("» Body cultivation multiplier increased by ")
                                    .append(Component.literal(String.format("+%.0f%%", packet.multiplicativeBonus() * 100))
                                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                                    .withStyle(ChatFormatting.GRAY);
                                serverPlayer.sendSystemMessage(multMsg);
                            }
                        }
                    }

                    // Play consumption sound
                    serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                        SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0f, 1.0f);

                    // Force sync to update client with new bonus values
                    cultivation.forceSync();

                    ImmortalCultivation.LOGGER.info("Player {} consumed {} pill: {} additive, {} multiplicative bonus",
                        serverPlayer.getName().getString(),
                        packet.cultivationType().getDisplayName(),
                        packet.additiveBonus(),
                        packet.multiplicativeBonus());
                }
            });
        }
    }
}