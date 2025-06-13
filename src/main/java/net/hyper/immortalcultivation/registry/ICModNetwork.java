package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.network.QiRequestPacket;
import net.hyper.immortalcultivation.network.QiSyncPacket;
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
    public static final ResourceLocation QI_SYNC = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "qi_sync");
    public static final ResourceLocation QI_REQUEST = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "qi_request");

    private static PayloadRegistrar REGISTRY;

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        REGISTRY = event.registrar(ImmortalCultivation.MOD_ID);

        // Server to Client
        REGISTRY.playToClient(
            QiSyncPacket.ID,
            QiSyncPacket.STREAM_CODEC,
            QiSyncPacket::handleClient
        );

        // Client to Server
        REGISTRY.playToServer(
            QiRequestPacket.ID,
            QiRequestPacket.STREAM_CODEC,
            QiRequestPacket::handleServer
        );
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
