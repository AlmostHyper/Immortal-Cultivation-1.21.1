package net.hyper.immortalcultivation.network.client;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Request packet for attempting cultivation breakthroughs.
 * Sent from client to server when player attempts breakthrough.
 */
public record BreakthroughRequestPacket(
    PlayerCultivationAttachment.CultivationType cultivationType
) implements CustomPacketPayload {

    public static final Type<BreakthroughRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "breakthrough_request")
    );

    // Custom codec for enum using ByteBufCodecs
    public static final StreamCodec<ByteBuf, BreakthroughRequestPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> ByteBufCodecs.STRING_UTF8.encode(buf, packet.cultivationType.name()),
        buf -> {
            try {
                return new BreakthroughRequestPacket(
                    PlayerCultivationAttachment.CultivationType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf))
                );
            } catch (IllegalArgumentException e) {
                return new BreakthroughRequestPacket(PlayerCultivationAttachment.CultivationType.QI);
            }
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(BreakthroughRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlayerCultivationAttachment cultivation = serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

                if (cultivation == null) {
                    serverPlayer.sendSystemMessage(
                        Component.literal("Error: Cultivation data not found!")
                            .withStyle(ChatFormatting.RED)
                    );
                    return;
                }

                // Attempt the breakthrough
                PlayerCultivationAttachment.BreakthroughResult result;
                if (packet.cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
                    result = cultivation.attemptQiBreakthrough();
                } else {
                    result = cultivation.attemptBodyBreakthrough();
                }

                // Send result message
                ChatFormatting color = result.success() ? ChatFormatting.GREEN : ChatFormatting.RED;
                serverPlayer.sendSystemMessage(
                    Component.literal(result.message())
                        .withStyle(color)
                );

                // Play effects based on result
                Level level = serverPlayer.level();
                if (result.success()) {
                    // Success effects
                    level.playSound(null, serverPlayer.blockPosition(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);

                    // Announce major breakthroughs to server
                    if (result.newRealm() != null && result.newRealm().isMajorRealm()) {
                        Component announcement = Component.literal("🌟 ")
                            .append(serverPlayer.getDisplayName())
                            .append(" has broken through to ")
                            .append(result.newRealm().getDisplayName())
                            .append("!")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

                        serverPlayer.server.getPlayerList().broadcastSystemMessage(announcement, false);
                    }

                    // Force sync after successful breakthrough
                    cultivation.forceSync();
                } else {
                    // Failure effects
                    level.playSound(null, serverPlayer.blockPosition(),
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
                }
            }
        });
    }
}