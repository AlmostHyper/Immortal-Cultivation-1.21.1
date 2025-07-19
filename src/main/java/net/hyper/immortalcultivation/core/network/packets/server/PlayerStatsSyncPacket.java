package net.hyper.immortalcultivation.core.network.packets.server;

import net.hyper.immortalcultivation.core.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.core.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.server.events.server.AttachmentUpdateEventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Network packet for synchronizing player statistics data from server to client.
 * Handles health, qi, body resources, and combat stats including attack damage,
 * movement speed, and jump height bonuses.
 *
 * This packet is automatically sent when:
 * - Player stats change and need client-side updates
 * - Player logs in, respawns, or changes dimensions
 * - Manual synchronization is triggered
 */
public record PlayerStatsSyncPacket(CompoundTag statsData) implements ISyncPacket {

    /**
     * Unique identifier for this packet type in the network registry.
     */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("immortalcultivation", "player_stats_sync");

    /**
     * Packet type definition for NeoForge's network system.
     */
    public static final CustomPacketPayload.Type<PlayerStatsSyncPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    /**
     * Stream codec for encoding/decoding packet data over the network.
     * Handles serialization of the CompoundTag containing player stats.
     */
    public static final StreamCodec<FriendlyByteBuf, PlayerStatsSyncPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> buf.writeNbt(packet.statsData()),
        (buf) -> new PlayerStatsSyncPacket(buf.readNbt())
    );

    /**
     * Returns the packet type identifier for the network system.
     * @return The packet type for PlayerStatsSyncPacket
     */
    @Override
    public CustomPacketPayload.Type<PlayerStatsSyncPacket> type() {
        return TYPE;
    }

    /**
     * Handles the packet on the client side by applying the synchronized data
     * to the player's stats attachment and triggering update events.
     *
     * @param player The client-side player entity receiving the sync
     */
    @Override
    public void handleOnClient(Player player) {
        if (player != null) {
            PlayerStatsAttachment attachment = player.getData(AttachmentRegistry.PLAYER_STATS.get());
            if (attachment != null) {
                // Apply the synchronized data to the client-side attachment
                attachment.applySync(this.statsData);

                // Fire an update event to notify other systems of the change
                NeoForge.EVENT_BUS.post(new AttachmentUpdateEventHandler(player, AttachmentRegistry.PLAYER_STATS.get()));
            }
        }
    }
}