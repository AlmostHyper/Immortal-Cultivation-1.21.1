package net.hyper.immortalcultivation.core.network.packets.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Packet for sending damage feedback information from server to client.
 * Used to display damage numbers, effects, or other combat feedback on the client side.
 */
public record DamageFeedbackSyncPacket(
    float damageAmount,
    String damageSourceId
) implements ISyncPacket {

    // === CONSTANTS === //

    public static final CustomPacketPayload.Type<DamageFeedbackSyncPacket> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "damage_feedback_sync")
    );

    public static final StreamCodec<ByteBuf, DamageFeedbackSyncPacket> STREAM_CODEC = StreamCodec.of(
        DamageFeedbackSyncPacket::encode,
        DamageFeedbackSyncPacket::decode
    );

    // === CONSTRUCTORS === //

    /**
     * Creates a damage feedback packet for client display.
     *
     * @param damageAmount The amount of damage dealt
     * @param damageSourceId The identifier for the damage source
     * @return A new damage feedback packet
     */
    public static DamageFeedbackSyncPacket create(float damageAmount, String damageSourceId) {
        return new DamageFeedbackSyncPacket(damageAmount, damageSourceId);
    }

    // === SERIALIZATION === //

    private static void encode(ByteBuf buf, DamageFeedbackSyncPacket packet) {
        ByteBufCodecs.FLOAT.encode(buf, packet.damageAmount);
        ByteBufCodecs.STRING_UTF8.encode(buf, packet.damageSourceId);
    }

    private static DamageFeedbackSyncPacket decode(ByteBuf buf) {
        float damageAmount = ByteBufCodecs.FLOAT.decode(buf);
        String damageSourceId = ByteBufCodecs.STRING_UTF8.decode(buf);
        return new DamageFeedbackSyncPacket(damageAmount, damageSourceId);
    }

    // === IMPLEMENTATIONS === //

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("Received damage feedback packet with null player");
            return;
        }

        // Handle damage feedback on client side
        // This could trigger damage number displays, screen effects, etc.
        ImmortalCultivation.LOGGER.debug("Player {} received damage feedback: {} from {}",
            player.getName().getString(), damageAmount, damageSourceId);

        // TODO: Add client-side damage feedback rendering here
        // Examples:
        // - Floating damage numbers
        // - Screen shake effects
        // - Blood/impact particle effects
        // - Sound effects
        // - UI damage indicators
    }
}