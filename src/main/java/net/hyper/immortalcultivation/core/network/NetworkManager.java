package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.core.attachments.ICultivationData;
import net.hyper.immortalcultivation.core.attachments.player.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.core.network.packets.CultivationPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NetworkManager {

    public static void handleCultivationPacket(CultivationPacket packet, IPayloadContext context) {
        ImmortalCultivation.LOGGER.info("NETWORK: Received packet with action {} on side {}",
            packet.action(), context.flow());

        if (context.flow() == PacketFlow.SERVERBOUND) {
            handleCultivationServerSide(packet, context);
        } else {
            handleCultivationClientSide(packet, context);
        }
    }

    private static void handleCultivationServerSide(CultivationPacket packet, IPayloadContext context) {
        ImmortalCultivation.LOGGER.info("CLIENT: Handling packet with action {}", packet.action());

        context.enqueueWork(() -> {
            switch (packet.action()) {
                case REQUEST_SYNC -> handleSyncRequest(packet, context);
                case ADD_CULTIVATION -> handleAddCultivation(packet, context);
                case REMOVE_CULTIVATION -> handleRemoveCultivation(packet, context);
                case SET_CULTIVATION -> handleSetCultivation(packet, context);
                default -> {}
            }
        });
    }

    private static void handleCultivationClientSide(CultivationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            switch (packet.action()) {
                case SYNC_DATA -> handleSyncData(packet, context);
                default -> {}
            }
        });
    }

    private static void handleSyncRequest(CultivationPacket packet, IPayloadContext context) {
        ServerPlayer serverPlayer = (ServerPlayer) context.player();
        PlayerCultivationAttachment attachment = serverPlayer.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());

        CompoundTag attachmentData = PlayerCultivationAttachment.serialize(attachment, serverPlayer.level().registryAccess());

        PacketDistributor.sendToPlayer(serverPlayer, new CultivationPacket(
            CultivationPacket.Action.SYNC_DATA,
            packet.cultivationType(),
            attachment.getCurrentCultivation(packet.cultivationType()),
            attachmentData
        ));
    }

    private static void handleSyncData(CultivationPacket packet, IPayloadContext context) {
        LocalPlayer localPlayer = (LocalPlayer) context.player();

        ImmortalCultivation.LOGGER.info("CLIENT: Received sync packet with data size: {}",
            packet.data() != null ? packet.data().size() : "null");

        PlayerCultivationAttachment newAttachment = PlayerCultivationAttachment.deserialize(
            localPlayer, packet.data(), localPlayer.level().registryAccess()
        );

        ImmortalCultivation.LOGGER.info("CLIENT: Deserialized attachment with QI: {}, BODY: {}",
            newAttachment.getCurrentCultivation(ICultivationData.CultivationType.QI),
            newAttachment.getCurrentCultivation(ICultivationData.CultivationType.BODY));

        localPlayer.setData(AttachmentRegistry.PLAYER_CULTIVATION.get(), newAttachment);

        PlayerCultivationAttachment verifyAttachment = localPlayer.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
        ImmortalCultivation.LOGGER.info("CLIENT: After setData - QI: {}, BODY: {}",
            verifyAttachment.getCurrentCultivation(ICultivationData.CultivationType.QI),
            verifyAttachment.getCurrentCultivation(ICultivationData.CultivationType.BODY));
    }

    private static void handleAddCultivation(CultivationPacket packet, IPayloadContext context) {
        ServerPlayer serverPlayer = (ServerPlayer) context.player();
        PlayerCultivationAttachment attachment = serverPlayer.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());

        attachment.addCultivation(packet.cultivationType(), packet.amount(), true);

        CompoundTag attachmentData = PlayerCultivationAttachment.serialize(attachment, serverPlayer.level().registryAccess());

        PacketDistributor.sendToPlayer(serverPlayer, new CultivationPacket(
            CultivationPacket.Action.SYNC_DATA,
            packet.cultivationType(),
            attachment.getCurrentCultivation(packet.cultivationType()),
            attachmentData
        ));
    }

    private static void handleRemoveCultivation(CultivationPacket packet, IPayloadContext context) {
        ServerPlayer serverPlayer = (ServerPlayer) context.player();
        PlayerCultivationAttachment attachment = serverPlayer.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());

        attachment.removeCultivation(packet.cultivationType(), packet.amount(), true);

        CompoundTag attachmentData = PlayerCultivationAttachment.serialize(attachment, serverPlayer.level().registryAccess());

        PacketDistributor.sendToPlayer(serverPlayer, new CultivationPacket(
            CultivationPacket.Action.SYNC_DATA,
            packet.cultivationType(),
            attachment.getCurrentCultivation(packet.cultivationType()),
            attachmentData
        ));
    }

    private static void handleSetCultivation(CultivationPacket packet, IPayloadContext context) {
        ServerPlayer serverPlayer = (ServerPlayer) context.player();
        PlayerCultivationAttachment attachment = serverPlayer.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());

        attachment.setCurrentCultivation(packet.cultivationType(), packet.amount());

        CompoundTag attachmentData = PlayerCultivationAttachment.serialize(attachment, serverPlayer.level().registryAccess());

        ImmortalCultivation.LOGGER.info("SERVER: Sending sync packet to player {} with data size: {}",
            serverPlayer.getName().getString(), attachmentData.size());

        PacketDistributor.sendToPlayer(serverPlayer, new CultivationPacket(
            CultivationPacket.Action.SYNC_DATA,
            packet.cultivationType(),
            attachment.getCurrentCultivation(packet.cultivationType()),
            attachmentData
        ));
    }
}