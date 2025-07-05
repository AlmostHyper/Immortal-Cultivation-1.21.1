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
 * Enhanced sync packet for unified resource data including permanent health bonus and health regen bonus.
 * Replaces: HealthSyncPacket, QiSyncPacket, and BodySyncPacket
 */
public record ResourceSyncPacket(
    // Health
    long currentHealth,
    long maxHealth,
    long permanentHealthBonus,
    double permanentHealthRegenBonus,  // FIXED: Now includes permanent health regen bonus

    // Qi Energy
    long currentQi,
    long maxQi,

    // Body Energy
    long currentBody,
    long maxBody

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
        ByteBufCodecs.VAR_LONG.encode(buf, packet.permanentHealthBonus);
        ByteBufCodecs.DOUBLE.encode(buf, packet.permanentHealthRegenBonus); // FIXED: Now syncs health regen bonus

        // Qi
        ByteBufCodecs.VAR_LONG.encode(buf, packet.currentQi);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.maxQi);

        // Body
        ByteBufCodecs.VAR_LONG.encode(buf, packet.currentBody);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.maxBody);

    }

    private static ResourceSyncPacket decode(ByteBuf buf) {
        return new ResourceSyncPacket(
            // Health
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf), // FIXED: Now decodes health regen bonus

            // Qi
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),

            // Body
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf)

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
                // Apply health data including permanent bonuses
                resources.setPermanentHealthBonus(packet.permanentHealthBonus);
                resources.setPermanentHealthRegenBonus(packet.permanentHealthRegenBonus); // FIXED: Now sets health regen bonus
                resources.setMaxHealth(packet.maxHealth);
                resources.setCurrentHealth(packet.currentHealth);

                // Apply Qi
                resources.setMaxQi(packet.maxQi);
                resources.setCurrentQi(packet.currentQi);

                // Apply Body
                resources.setMaxBody(packet.maxBody);
                resources.setCurrentBody(packet.currentBody);

            }
        });
    }
}