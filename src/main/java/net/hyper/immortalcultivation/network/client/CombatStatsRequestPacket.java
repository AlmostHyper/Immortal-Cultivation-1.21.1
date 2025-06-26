package net.hyper.immortalcultivation.network.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCombatStatsAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Request packet for syncing combat stats data from server to client.
 * Sent from client to server when client needs updated combat statistics.
 * This triggers the server to send a CombatStatsSyncPacket back to the client.
 */
public record CombatStatsRequestPacket() implements CustomPacketPayload {

    public static final Type<CombatStatsRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "combat_stats_request")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CombatStatsRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new CombatStatsRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(CombatStatsRequestPacket packet, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    PlayerCombatStatsAttachment combatStats = PlayerCombatStatsAttachment.getOrCreate(serverPlayer);

                    // Force sync to update client with latest combat statistics
                    combatStats.forceSync();

                    ImmortalCultivation.LOGGER.debug("Forced combat stats sync for player: {}",
                        serverPlayer.getName().getString());
                }
            });
        }
    }
}