package net.hyper.immortalcultivation.core.network.packets.client;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
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
 * Enhanced request packet for attempting specific types of cultivation breakthroughs.
 * Supports Minor, Major, Special, and Boundary breakthrough types with different success rates.
 */
public record BreakthroughRequestPacket(
    PlayerCultivationAttachment.CultivationType cultivationType,
    PlayerCultivationAttachment.BreakthroughType breakthroughType
) implements CustomPacketPayload {

    public static final Type<BreakthroughRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "enhanced_breakthrough_request")
    );

    // Custom codec for enums using ByteBufCodecs
    public static final StreamCodec<ByteBuf, BreakthroughRequestPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.cultivationType.name());
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.breakthroughType.name());
        },
        buf -> {
            try {
                PlayerCultivationAttachment.CultivationType cultType =
                    PlayerCultivationAttachment.CultivationType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
                PlayerCultivationAttachment.BreakthroughType breakType =
                    PlayerCultivationAttachment.BreakthroughType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
                return new BreakthroughRequestPacket(cultType, breakType);
            } catch (IllegalArgumentException e) {
                return new BreakthroughRequestPacket(
                    PlayerCultivationAttachment.CultivationType.QI,
                    PlayerCultivationAttachment.BreakthroughType.MINOR
                );
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

                // Ensure player reference is set
                cultivation.setPlayer(serverPlayer);

                // Attempt the specific type of breakthrough
                PlayerCultivationAttachment.BreakthroughResult result =
                    cultivation.attemptTypedBreakthrough(packet.cultivationType, packet.breakthroughType);

                // Send detailed result message
                ChatFormatting color = result.success() ? ChatFormatting.GREEN : ChatFormatting.RED;
                Component message = Component.literal(result.message()).withStyle(color);

                // Add breakthrough type information
                Component typeInfo = Component.literal(" [" + packet.breakthroughType.getDisplayName() + "]")
                    .withStyle(ChatFormatting.GRAY);

                serverPlayer.sendSystemMessage(message.copy().append(typeInfo));

                // Play effects based on result
                Level level = serverPlayer.level();
                if (result.success()) {
                    // Success effects - vary by breakthrough type
                    playSuccessEffects(level, serverPlayer, packet.breakthroughType);

                    // Announce major breakthroughs to server
                    if (result.newRealm() != null && result.newRealm().isMajorRealm()) {
                        Component announcement = Component.literal("🌟 ")
                            .append(serverPlayer.getDisplayName())
                            .append(" has achieved a ")
                            .append(packet.breakthroughType.getDisplayName())
                            .append(" breakthrough to ")
                            .append(result.newRealm().getDisplayName())
                            .append("!")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

                        serverPlayer.server.getPlayerList().broadcastSystemMessage(announcement, false);
                    }

                    // Force sync after successful breakthrough
                    cultivation.forceSync();

                } else {
                    // Failure effects - vary by breakthrough type
                    playFailureEffects(level, serverPlayer, packet.breakthroughType);

                    // Send encouragement message with next attempt info
                    if (result.attemptNumber() > 0) {
                        double nextChance = cultivation.calculateBreakthroughSuccessRate(
                            packet.cultivationType, packet.breakthroughType);

                        Component encouragement = Component.literal(
                                String.format("Next %s attempt will have %.0f%% success rate. Keep cultivating!",
                                    packet.breakthroughType.getDisplayName(), nextChance * 100))
                            .withStyle(ChatFormatting.YELLOW);

                        serverPlayer.sendSystemMessage(encouragement);
                    }

                    // Force sync after failed attempt to update attempt count
                    cultivation.forceSync();
                }
            }
        });
    }

    /**
     * Play success effects based on breakthrough type
     */
    private static void playSuccessEffects(Level level, ServerPlayer player, PlayerCultivationAttachment.BreakthroughType type) {
        switch (type) {
            case MINOR:
                level.playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.0f);
                break;
            case MAJOR:
                level.playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.5f, 1.5f);
                break;
            case SPECIAL:
                level.playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;
            case BOUNDARY:
                level.playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2f, 2.0f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0f, 2.0f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.3f, 1.0f);
                break;
        }
    }

    /**
     * Play failure effects based on breakthrough type
     */
    private static void playFailureEffects(Level level, ServerPlayer player, PlayerCultivationAttachment.BreakthroughType type) {
        switch (type) {
            case MINOR:
                level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 0.8f);
                break;
            case MAJOR:
                level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
                break;
            case SPECIAL:
                level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 0.5f);
                break;
            case BOUNDARY:
                level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 0.3f, 0.5f);
                break;
        }
    }
}