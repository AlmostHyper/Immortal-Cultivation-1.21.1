package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.network.client.*;
import net.hyper.immortalcultivation.network.server.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ICModNetwork {

    private static PayloadRegistrar REGISTRY;

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        ImmortalCultivation.LOGGER.info("Registering network packets...");

        REGISTRY = event.registrar(ImmortalCultivation.MOD_ID);

        // === SERVER -> CLIENT === //

        REGISTRY.playToClient(
            RaceSyncPacket.TYPE,
            RaceSyncPacket.STREAM_CODEC,
            RaceSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            OpenRaceSelectionSyncPacket.TYPE,
            OpenRaceSelectionSyncPacket.STREAM_CODEC,
            OpenRaceSelectionSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            CultivationSyncPacket.TYPE,
            CultivationSyncPacket.STREAM_CODEC,
            CultivationSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            ResourceSyncPacket.TYPE,
            ResourceSyncPacket.STREAM_CODEC,
            ResourceSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            TalentSyncPacket.TYPE,
            TalentSyncPacket.STREAM_CODEC,
            TalentSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            BreakthroughSyncPacket.TYPE,
            BreakthroughSyncPacket.STREAM_CODEC,
            BreakthroughSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            CombatStatsSyncPacket.TYPE,
            CombatStatsSyncPacket.STREAM_CODEC,
            CombatStatsSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            DamageFeedbackSyncPacket.TYPE,
            DamageFeedbackSyncPacket.STREAM_CODEC,
            DamageFeedbackSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            PillConsumptionSyncPacket.TYPE,
            PillConsumptionSyncPacket.STREAM_CODEC,
            PillConsumptionSyncPacket::handleClient
        );

        // === CLIENT -> SERVER === //

        REGISTRY.playToServer(
            RaceSelectionRequestPacket.TYPE,
            RaceSelectionRequestPacket.STREAM_CODEC,
            RaceSelectionRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            CultivationRequestPacket.TYPE,
            CultivationRequestPacket.STREAM_CODEC,
            CultivationRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            ResourceRequestPacket.TYPE,
            ResourceRequestPacket.STREAM_CODEC,
            ResourceRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            TalentRequestPacket.TYPE,
            TalentRequestPacket.STREAM_CODEC,
            TalentRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            CultivationFocusTogglePacket.TYPE,
            CultivationFocusTogglePacket.STREAM_CODEC,
            CultivationFocusTogglePacket::handleServer
        );

        REGISTRY.playToServer(
            BreakthroughRequestPacket.TYPE,
            BreakthroughRequestPacket.STREAM_CODEC,
            BreakthroughRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            CombatStatsRequestPacket.TYPE,
            CombatStatsRequestPacket.STREAM_CODEC,
            CombatStatsRequestPacket::handleServer
        );

        ImmortalCultivation.LOGGER.info("Network packets registered successfully!");
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
