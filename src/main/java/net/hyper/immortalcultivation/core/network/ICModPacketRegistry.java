package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.core.network.packets.client.*;
import net.hyper.immortalcultivation.core.network.packets.server.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
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
                UnifiedActionRequestPacket::handleServer
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
}