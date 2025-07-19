package net.hyper.immortalcultivation.core.network.packets.server;

import net.hyper.immortalcultivation.core.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.core.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Network packet for synchronizing player cultivation data from server to client.
 * Handles Qi and Body cultivation realms, progress, rates, focus mechanics, and statistics.
 *
 * This packet is automatically sent when:
 * - Cultivation progress changes and needs client-side updates
 * - Player advances realms or changes cultivation focus
 * - Player logs in, respawns, or changes dimensions
 * - Manual synchronization is triggered for cultivation data
 */
public record PlayerCultivationSyncPacket(CompoundTag cultivationData) implements ISyncPacket {

    /**
     * Unique identifier for this packet type in the network registry.
     */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("immortalcultivation", "player_cultivation_sync");

    /**
     * Packet type definition for NeoForge's network system.
     */
    public static final CustomPacketPayload.Type<PlayerCultivationSyncPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    /**
     * Stream codec for encoding/decoding packet data over the network.
     * Handles serialization of the CompoundTag containing player cultivation data.
     */
    public static final StreamCodec<FriendlyByteBuf, PlayerCultivationSyncPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> buf.writeNbt(packet.cultivationData()),
        (buf) -> new PlayerCultivationSyncPacket(buf.readNbt())
    );

    /**
     * Returns the packet type identifier for the network system.
     * @return The packet type for PlayerCultivationSyncPacket
     */
    @Override
    public CustomPacketPayload.Type<PlayerCultivationSyncPacket> type() {
        return TYPE;
    }

    /**
     * Handles the packet on the client side by applying the synchronized data
     * to the player's cultivation attachment and triggering update events.
     *
     * @param player The client-side player entity receiving the sync
     */
    @Override
    public void handleOnClient(Player player) {
        if (player != null) {
            PlayerCultivationAttachment attachment = player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
            if (attachment != null) {
                // Apply the synchronized data to the client-side attachment
                attachment.applySync(this.cultivationData);

                // Fire an update event to notify other systems of the change
            }
        }
    }
}