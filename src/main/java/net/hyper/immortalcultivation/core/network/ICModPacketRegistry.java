package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.attachments.PlayerRaceAttachment;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.network.packets.client.*;
import net.hyper.immortalcultivation.core.network.packets.server.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ICModPacketRegistry {

    private static PayloadRegistrar REGISTRY;
    private static boolean DEBUG_PACKETS = true; // Toggle for packet debugging

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        ImmortalCultivation.LOGGER.info("Registering network packets...");

        REGISTRY = event.registrar(ImmortalCultivation.MOD_ID);

        // === SERVER -> CLIENT === //
        registerClientPacket(PlayerCultivationSyncPacket.TYPE, PlayerCultivationSyncPacket.STREAM_CODEC);
        registerClientPacket(PlayerTalentsSyncPacket.TYPE, PlayerTalentsSyncPacket.STREAM_CODEC);
        registerClientPacket(PlayerRaceSyncPacket.TYPE, PlayerRaceSyncPacket.STREAM_CODEC);
        registerClientPacket(PlayerPillConsumptionSyncPacket.TYPE, PlayerPillConsumptionSyncPacket.STREAM_CODEC);
        registerClientPacket(PlayerBreakthroughSyncPacket.TYPE, PlayerBreakthroughSyncPacket.STREAM_CODEC);

        REGISTRY.playToClient(
            OpenRaceSelectionSyncPacket.TYPE,
            OpenRaceSelectionSyncPacket.STREAM_CODEC,
            OpenRaceSelectionSyncPacket::handleClient
        );

        // === CLIENT -> SERVER === //
        REGISTRY.playToServer(
            UnifiedActionRequestPacket.TYPE,
            UnifiedActionRequestPacket.STREAM_CODEC,
            Clientbound::handleUnifiedAction
        );

        ImmortalCultivation.LOGGER.info("Network packets registered successfully!");
    }

    private static <T extends ISyncPacket> void registerClientPacket(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        REGISTRY.playToClient(type, codec, (packet, context) -> {
            if (DEBUG_PACKETS) {
                ImmortalCultivation.LOGGER.debug("Received {} packet on client for player: {}",
                    packet.type().id(),
                    context.player() != null ? context.player().getName().getString() : "null");
            }
            packet.handleOnClient(context.player());
        });
    }

    // Enhanced utility methods with logging
    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        try {
            player.connection.send(new ClientboundCustomPayloadPacket(packet));

            if (DEBUG_PACKETS) {
                ImmortalCultivation.LOGGER.debug("Sent {} packet to player: {} (UUID: {})",
                    packet.type().id(),
                    player.getName().getString(),
                    player.getUUID());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to send {} packet to player {}: {}",
                packet.type().id(),
                player.getName().getString(),
                e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendToServer(CustomPacketPayload packet) {
        try {
            Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(packet));

            if (DEBUG_PACKETS) {
                ImmortalCultivation.LOGGER.debug("Sent {} packet to server", packet.type().id());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to send {} packet to server: {}",
                packet.type().id(),
                e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendToAllPlayers(CustomPacketPayload packet) {
        int count = 0;
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            sendToPlayer(packet, player);
            count++;
        }

        if (DEBUG_PACKETS) {
            ImmortalCultivation.LOGGER.debug("Broadcast {} packet to {} players",
                packet.type().id(), count);
        }
    }

    // Add method to toggle debug logging
    public static void setDebugPackets(boolean debug) {
        DEBUG_PACKETS = debug;
        ImmortalCultivation.LOGGER.info("Packet debugging {}", debug ? "enabled" : "disabled");
    }

    public static class Clientbound {
        public static void handleUnifiedAction(UnifiedActionRequestPacket packet, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if (DEBUG_PACKETS) {
                ImmortalCultivation.LOGGER.debug("Handling {} action from player: {}",
                    packet.actionType(), player.getName().getString());
            }

            switch (packet.actionType()) {
                case BREAKTHROUGH_REQUEST -> handleBreakthroughRequest(player, packet);
                case CULTIVATION_FOCUS_TOGGLE -> handleFocusToggle(player);
                case SELECT_RACE -> handleRaceSelection(player, packet);
            }
        }

        private static void handleRaceSelection(ServerPlayer player, UnifiedActionRequestPacket packet) {
            var raceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            raceAttachment.setPlayer(player);

            if (raceAttachment.hasRaceAssigned()) {
                player.sendSystemMessage(Component.literal("You have already chosen a race.").withStyle(ChatFormatting.RED));
                return;
            }

            packet.stringParam().ifPresentOrElse(raceId -> {
                try {
                    var raceType = RaceType.valueOf(raceId.toUpperCase());
                    raceAttachment.assignRace(raceType);
                    player.sendSystemMessage(Component.literal("You are now a " + raceType.getDisplayName() + "!").withStyle(ChatFormatting.GOLD));
                    ICModNetworkManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_RACE::get, true);
                } catch (IllegalArgumentException e) {
                    player.sendSystemMessage(Component.literal("Invalid race selected.").withStyle(ChatFormatting.RED));
                    ImmortalCultivation.LOGGER.warn("Player {} tried to select invalid race: {}", player.getName().getString(), raceId);
                }
            }, () -> {
                player.sendSystemMessage(Component.literal("No race selected.").withStyle(ChatFormatting.RED));
                ImmortalCultivation.LOGGER.warn("Player {} sent a race selection packet with no race parameter.", player.getName().getString());
            });
        }

        private static void handleFocusToggle(ServerPlayer player) {
            var cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            cultivation.setPlayer(player);

            boolean success = cultivation.toggleFocus();
            if (success) {
                var newFocus = cultivation.getCurrentFocus();
                player.sendSystemMessage(Component.literal("✨ Cultivation focus shifted to ").append(Component.literal(newFocus.getDisplayName() + " Cultivation").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)).append("!").withStyle(ChatFormatting.GREEN));
                player.sendSystemMessage(Component.literal("» ").append(Component.literal("2x").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)).append(" cultivation speed | ").append(Component.literal("1.5x").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)).append(" energy regeneration").withStyle(ChatFormatting.GRAY));
                player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);
                ICModNetworkManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_CULTIVATION::get, false);
            } else {
                long remainingTicks = 24000 - (player.level().getGameTime() - cultivation.getLastFocusChangeTime());
                long remainingMinutes = remainingTicks / 1200;
                player.sendSystemMessage(Component.literal("⏳ Focus change on cooldown! ").append(Component.literal(remainingMinutes + " minutes").withStyle(ChatFormatting.YELLOW)).append(" remaining.").withStyle(ChatFormatting.RED));
                player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }

        private static void handleBreakthroughRequest(ServerPlayer player, UnifiedActionRequestPacket packet) {
            // Implementation details...
            ImmortalCultivation.LOGGER.debug("Processing breakthrough request for player: {}", player.getName().getString());
        }
    }
}