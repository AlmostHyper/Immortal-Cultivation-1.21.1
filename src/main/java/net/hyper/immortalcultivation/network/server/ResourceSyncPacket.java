package net.hyper.immortalcultivation.network.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.attachment.PlayerResourceAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sync packet for unified resource data.
 * Replaces: HealthSyncPacket, QiSyncPacket, and BodySyncPacket
 */
public record ResourceSyncPacket(
    // Health
    long currentHealth,
    long maxHealth,

    // Qi Energy
    long currentQi,
    long maxQi,

    // Body Energy
    long currentBody,
    long maxBody,

    // Meditation state
    boolean isMeditating
) implements CustomPacketPayload {

    public static final Type<ResourceSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "resource_sync")
    );

    // Manual implementation due to parameter limits
    public static final StreamCodec<ByteBuf, ResourceSyncPacket> STREAM_CODEC = StreamCodec.of(
        ResourceSyncPacket::encode,
        ResourceSyncPacket::decode
    );

    private static void encode(ByteBuf buf, ResourceSyncPacket packet) {
        // Health
        ByteBufCodecs.VAR_LONG.encode(buf, packet.currentHealth);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.maxHealth);

        // Qi
        ByteBufCodecs.VAR_LONG.encode(buf, packet.currentQi);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.maxQi);

        // Body
        ByteBufCodecs.VAR_LONG.encode(buf, packet.currentBody);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.maxBody);

        // Meditation
        ByteBufCodecs.BOOL.encode(buf, packet.isMeditating);
    }

    private static ResourceSyncPacket decode(ByteBuf buf) {
        return new ResourceSyncPacket(
            // Health
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),

            // Qi
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),

            // Body
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),

            // Meditation
            ByteBufCodecs.BOOL.decode(buf)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(ResourceSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());

            if (resources != null) {
                // Apply health
                resources.setMaxHealth(packet.maxHealth);
                resources.setCurrentHealth(packet.currentHealth);

                // Apply Qi
                resources.setMaxQi(packet.maxQi);
                resources.setCurrentQi(packet.currentQi);

                // Apply Body
                resources.setMaxBody(packet.maxBody);
                resources.setCurrentBody(packet.currentBody);

                // Apply meditation state
                resources.setMeditating(packet.isMeditating);
            }
        });
    }
}