package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerBreakthroughAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.helpers.BreakthroughFeedbackHelper;
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

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        ImmortalCultivation.LOGGER.info("Registering network packets...");

        REGISTRY = event.registrar(ImmortalCultivation.MOD_ID);

        // === SERVER -> CLIENT === //
        registerClientPacket(PlayerCultivationSyncPacket.TYPE, PlayerCultivationSyncPacket.STREAM_CODEC);
        registerClientPacket(PlayerTalentSyncPacket.TYPE, PlayerTalentSyncPacket.STREAM_CODEC);
        registerClientPacket(PlayerRaceSyncPacket.TYPE, PlayerRaceSyncPacket.STREAM_CODEC);
        registerClientPacket(PlayerResourceSyncPacket.TYPE, PlayerResourceSyncPacket.STREAM_CODEC);
        registerClientPacket(PlayerPillConsumptionSyncPacket.TYPE, PlayerPillConsumptionSyncPacket.STREAM_CODEC);
        registerClientPacket(PlayerBreakthroughSyncPacket.TYPE, PlayerBreakthroughSyncPacket.STREAM_CODEC);
        registerClientPacket(PlayerCombatStatsSyncPacket.TYPE, PlayerCombatStatsSyncPacket.STREAM_CODEC);

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
        REGISTRY.playToClient(type, codec, (packet, context) -> packet.handleOnClient(context.player()));
    }

    // Utility methods for sending packets
    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        player.connection.send(new ClientboundCustomPayloadPacket(packet));
    }

    public static void sendToServer(CustomPacketPayload packet) {
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(packet));
    }

    public static void sendToAllPlayers(CustomPacketPayload packet) {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            sendToPlayer(packet, player);
        }
    }

    public static class Clientbound {
        public static void handleUnifiedAction(UnifiedActionRequestPacket packet, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) return;

            switch (packet.actionType()) {
                case BREAKTHROUGH_REQUEST -> handleBreakthroughRequest(player, packet);
                case CULTIVATION_FOCUS_TOGGLE -> handleFocusToggle(player);
                // Other cases for race selection etc. would go here
            }
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
            } else {
                long remainingTicks = 24000 - (player.level().getGameTime() - cultivation.getLastFocusChangeTime());
                long remainingMinutes = remainingTicks / 1200;
                player.sendSystemMessage(Component.literal("⏳ Focus change on cooldown! ").append(Component.literal(remainingMinutes + " minutes").withStyle(ChatFormatting.YELLOW)).append(" remaining.").withStyle(ChatFormatting.RED));
                player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            NetworkSyncManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_CULTIVATION::get, false);
        }

        private static void handleBreakthroughRequest(ServerPlayer player, UnifiedActionRequestPacket packet) {
            if (packet.cultivationType().isEmpty() || packet.breakthroughType().isEmpty()) return;

            var breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
            breakthrough.setPlayer(player);
            var result = breakthrough.attemptTypedBreakthrough(packet.cultivationType().get(), packet.breakthroughType().get());

            BreakthroughFeedbackHelper.sendBreakthroughFeedback(player, result, packet.breakthroughType().get());
            NetworkSyncManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_BREAKTHROUGH::get, true);
        }
    }
}
