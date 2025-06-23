package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
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
            RealmSyncPacket.ID,
            RealmSyncPacket.STREAM_CODEC,
            RealmSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            QiCultivationSyncPacket.ID,
            QiCultivationSyncPacket.STREAM_CODEC,
            QiCultivationSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            BodyCultivationSyncPacket.ID,
            BodyCultivationSyncPacket.STREAM_CODEC,
            BodyCultivationSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            CultivationFocusSyncPacket.ID,
            CultivationFocusSyncPacket.STREAM_CODEC,
            CultivationFocusSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            CultivationBonusSyncPacket.ID,
            CultivationBonusSyncPacket.STREAM_CODEC,
            CultivationBonusSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            CultivationBreakthroughSyncPacket.ID,
            CultivationBreakthroughSyncPacket.STREAM_CODEC,
            CultivationBreakthroughSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            HealthSyncPacket.ID,
            HealthSyncPacket.STREAM_CODEC,
            HealthSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            QiSyncPacket.ID,
            QiSyncPacket.STREAM_CODEC,
            QiSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            BodySyncPacket.ID,
            BodySyncPacket.STREAM_CODEC,
            BodySyncPacket::handleClient
        );

        REGISTRY.playToClient(
            SpiritualRootSyncPacket.ID,
            SpiritualRootSyncPacket.STREAM_CODEC,
            SpiritualRootSyncPacket::handleClient
        );

        REGISTRY.playToClient(
            PhysiqueSyncPacket.ID,
            PhysiqueSyncPacket.STREAM_CODEC,
            PhysiqueSyncPacket::handleClient
        );

        // === CLIENT -> SERVER === //

        REGISTRY.playToServer(
            RealmRequestPacket.ID,
            RealmRequestPacket.STREAM_CODEC,
            RealmRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            QiCultivationRequestPacket.ID,
            QiCultivationRequestPacket.STREAM_CODEC,
            QiCultivationRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            BodyCultivationRequestPacket.ID,
            BodyCultivationRequestPacket.STREAM_CODEC,
            BodyCultivationRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            CultivationFocusRequestPacket.ID,
            CultivationFocusRequestPacket.STREAM_CODEC,
            CultivationFocusRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            CultivationFocusTogglePacket.ID,
            CultivationFocusTogglePacket.STREAM_CODEC,
            CultivationFocusTogglePacket::handleServer
        );

        REGISTRY.playToServer(
            CultivationBonusRequestPacket.ID,
            CultivationBonusRequestPacket.STREAM_CODEC,
            CultivationBonusRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            CultivationBreakthroughRequestPacket.ID,
            CultivationBreakthroughRequestPacket.STREAM_CODEC,
            CultivationBreakthroughRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            HealthRequestPacket.ID,
            HealthRequestPacket.STREAM_CODEC,
            HealthRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            QiRequestPacket.ID,
            QiRequestPacket.STREAM_CODEC,
            QiRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            BodyRequestPacket.ID,
            BodyRequestPacket.STREAM_CODEC,
            BodyRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            SpiritualRootRequestPacket.ID,
            SpiritualRootRequestPacket.STREAM_CODEC,
            SpiritualRootRequestPacket::handleServer
        );

        REGISTRY.playToServer(
            PhysiqueRequestPacket.ID,
            PhysiqueRequestPacket.STREAM_CODEC,
            PhysiqueRequestPacket::handleServer
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
