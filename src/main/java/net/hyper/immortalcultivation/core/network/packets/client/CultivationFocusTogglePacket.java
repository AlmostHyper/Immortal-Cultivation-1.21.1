package net.hyper.immortalcultivation.core.network.packets.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.capability.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Request packet for toggling cultivation focus.
 * Sent from client to server when player wants to switch focus.
 * Moved to client package since it's a client->server request.
 */
public record CultivationFocusTogglePacket() implements CustomPacketPayload {

    public static final Type<CultivationFocusTogglePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_focus_toggle")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationFocusTogglePacket> STREAM_CODEC =
        StreamCodec.unit(new CultivationFocusTogglePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(CultivationFocusTogglePacket packet, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    PlayerCultivationAttachment cultivation = serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

                    if (cultivation == null) {
                        serverPlayer.sendSystemMessage(
                            Component.literal("Error: Cultivation data not found!")
                                .withStyle(ChatFormatting.RED)
                        );
                        ImmortalCultivation.LOGGER.warn("Player {} attempted to toggle focus but has no cultivation attachment",
                            serverPlayer.getName().getString());
                        return;
                    }

                    // Set player reference to ensure server-side methods work
                    cultivation.setPlayer(serverPlayer);

                    // Attempt to toggle focus
                    boolean success = cultivation.toggleFocus();

                    if (success) {
                        PlayerCultivationAttachment.CultivationType newFocus = cultivation.getCurrentFocus();

                        // Send success message
                        Component message = Component.literal("✨ Cultivation focus shifted to ")
                            .append(Component.literal(newFocus.getDisplayName() + " Cultivation")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                            .append("!")
                            .withStyle(ChatFormatting.GREEN);

                        serverPlayer.sendSystemMessage(message);

                        // Explain the bonus
                        Component bonusInfo = Component.literal("» ")
                            .append(Component.literal("2x")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                            .append(" cultivation speed | ")
                            .append(Component.literal("1.5x")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                            .append(" energy regeneration")
                            .withStyle(ChatFormatting.GRAY);

                        serverPlayer.sendSystemMessage(bonusInfo);

                        // Play sound effect
                        serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);

                        ImmortalCultivation.LOGGER.info("Player {} changed cultivation focus to {}",
                            serverPlayer.getName().getString(), newFocus.getDisplayName());

                    } else {
                        // On cooldown
                        long remainingTicks = 24000 - (serverPlayer.level().getGameTime() - cultivation.getLastFocusChangeTime());
                        long remainingMinutes = remainingTicks / 1200; // 20 ticks per second, 60 seconds per minute

                        Component message = Component.literal("⏳ Focus change on cooldown! ")
                            .append(Component.literal(remainingMinutes + " minutes")
                                .withStyle(ChatFormatting.YELLOW))
                            .append(" remaining.")
                            .withStyle(ChatFormatting.RED);

                        serverPlayer.sendSystemMessage(message);

                        // Play error sound
                        serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                            SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);

                        ImmortalCultivation.LOGGER.debug("Player {} attempted to change focus but is on cooldown ({}m remaining)",
                            serverPlayer.getName().getString(), remainingMinutes);
                    }
                }
            });
        }
    }
}