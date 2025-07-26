package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.core.attachments.CultivationPlayerAttachment;
import net.hyper.immortalcultivation.core.network.packets.CultivationPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NetworkManager {

    public static void handleCultivationPacket(CultivationPacket packet, IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            handleCultivationServerSide(packet, context);
        } else {
            handleCultivationClientSide(packet, context);
        }
    }

    private static void handleCultivationServerSide(CultivationPacket packet, IPayloadContext context) {
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
        if (context.player() instanceof ServerPlayer serverPlayer) {
            CultivationPlayerAttachment attachment = serverPlayer.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());

            // Serialize current attachment data using write method
            CompoundTag attachmentData = (CompoundTag) attachment.write(attachment, serverPlayer.level().registryAccess());

            // Send sync response back to client
            PacketDistributor.sendToPlayer(serverPlayer, new CultivationPacket(
                CultivationPacket.Action.SYNC_DATA,
                packet.cultivationType(),
                attachment.getCurrentCultivation(packet.cultivationType()),
                attachmentData
            ));
        }
    }

    private static void handleSyncData(CultivationPacket packet, IPayloadContext context) {
        if (context.player() instanceof LocalPlayer localPlayer) {
            CultivationPlayerAttachment attachment = localPlayer.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());

            // Apply synced data from server using read method
            // Note: The read method creates a new attachment, so we need to copy the data
            CultivationPlayerAttachment newAttachment = (CultivationPlayerAttachment) attachment.read(
                localPlayer, packet.data(), localPlayer.level().registryAccess()
            );

            // Update current cultivation for the specific type
            attachment.setCurrentCultivation(packet.cultivationType(), packet.amount());
        }
    }

    private static void handleAddCultivation(CultivationPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            CultivationPlayerAttachment attachment = serverPlayer.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());

            // Add cultivation points using the correct method signature
            attachment.addCultivation(packet.cultivationType(), packet.amount(), true);

            // Serialize updated data and sync back to client
            CompoundTag attachmentData = (CompoundTag) attachment.write(attachment, serverPlayer.level().registryAccess());

            PacketDistributor.sendToPlayer(serverPlayer, new CultivationPacket(
                CultivationPacket.Action.SYNC_DATA,
                packet.cultivationType(),
                attachment.getCurrentCultivation(packet.cultivationType()),
                attachmentData
            ));
        }
    }

    private static void handleRemoveCultivation(CultivationPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            CultivationPlayerAttachment attachment = serverPlayer.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());

            // Remove cultivation points using the correct method signature
            attachment.removeCultivation(packet.cultivationType(), packet.amount(), true);

            // Serialize updated data and sync back to client
            CompoundTag attachmentData = (CompoundTag) attachment.write(attachment, serverPlayer.level().registryAccess());

            PacketDistributor.sendToPlayer(serverPlayer, new CultivationPacket(
                CultivationPacket.Action.SYNC_DATA,
                packet.cultivationType(),
                attachment.getCurrentCultivation(packet.cultivationType()),
                attachmentData
            ));
        }
    }

    private static void handleSetCultivation(CultivationPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            CultivationPlayerAttachment attachment = serverPlayer.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());

            // Set cultivation to specific value
            attachment.setCurrentCultivation(packet.cultivationType(), packet.amount());

            // Serialize updated data and sync back to client
            CompoundTag attachmentData = (CompoundTag) attachment.write(attachment, serverPlayer.level().registryAccess());

            PacketDistributor.sendToPlayer(serverPlayer, new CultivationPacket(
                CultivationPacket.Action.SYNC_DATA,
                packet.cultivationType(),
                attachment.getCurrentCultivation(packet.cultivationType()),
                attachmentData
            ));
        }
    }
}