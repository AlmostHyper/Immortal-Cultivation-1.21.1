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
 * FIXED: Complete sync packet for unified resource data including ALL permanent bonuses.
 *
 * This packet now properly syncs all 6 permanent bonuses instead of just 2:
 * - permanentHealthBonus ✓
 * - permanentHealthRegenBonus ✓
 * - permanentMaxQiBonus ✓ (was missing!)
 * - permanentQiRegenBonus ✓ (was missing!)
 * - permanentMaxBodyBonus ✓ (was missing!)
 * - permanentBodyRegenBonus ✓ (was missing!)
 *
 * Compatible with the permanent additive stat system.
 * Replaces: HealthSyncPacket, QiSyncPacket, and BodySyncPacket
 */
public record ResourceSyncPacket(
    // Current resource values
    long currentHealth,
    long currentQi,
    long currentBody,

    // All permanent bonuses (the critical part that was incomplete)
    long permanentHealthBonus,
    double permanentHealthRegenBonus,
    long permanentMaxQiBonus,
    double permanentQiRegenBonus,
    long permanentMaxBodyBonus,
    double permanentBodyRegenBonus

) implements CustomPacketPayload {

    public static final Type<ResourceSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "resource_sync")
    );

    // Manual implementation due to high parameter count
    public static final StreamCodec<ByteBuf, ResourceSyncPacket> STREAM_CODEC = StreamCodec.of(
        ResourceSyncPacket::encode,
        ResourceSyncPacket::decode
    );

    private static void encode(ByteBuf buf, ResourceSyncPacket packet) {
        // Current resource values
        ByteBufCodecs.VAR_LONG.encode(buf, packet.currentHealth);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.currentQi);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.currentBody);

        // ALL permanent bonuses (this was the missing part!)
        ByteBufCodecs.VAR_LONG.encode(buf, packet.permanentHealthBonus);
        ByteBufCodecs.DOUBLE.encode(buf, packet.permanentHealthRegenBonus);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.permanentMaxQiBonus);
        ByteBufCodecs.DOUBLE.encode(buf, packet.permanentQiRegenBonus);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.permanentMaxBodyBonus);
        ByteBufCodecs.DOUBLE.encode(buf, packet.permanentBodyRegenBonus);
    }

    private static ResourceSyncPacket decode(ByteBuf buf) {
        return new ResourceSyncPacket(
            // Current resource values
            ByteBufCodecs.VAR_LONG.decode(buf), // currentHealth
            ByteBufCodecs.VAR_LONG.decode(buf), // currentQi
            ByteBufCodecs.VAR_LONG.decode(buf), // currentBody

            // ALL permanent bonuses
            ByteBufCodecs.VAR_LONG.decode(buf), // permanentHealthBonus
            ByteBufCodecs.DOUBLE.decode(buf),   // permanentHealthRegenBonus
            ByteBufCodecs.VAR_LONG.decode(buf), // permanentMaxQiBonus
            ByteBufCodecs.DOUBLE.decode(buf),   // permanentQiRegenBonus
            ByteBufCodecs.VAR_LONG.decode(buf), // permanentMaxBodyBonus
            ByteBufCodecs.DOUBLE.decode(buf)    // permanentBodyRegenBonus
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * FIXED: Handle client-side sync with proper permanent additive system compatibility.
     * This ensures the client's resource attachment matches the server exactly.
     */
    public static void handleClient(ResourceSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());

            if (resources != null) {
                // Step 1: Set ALL permanent bonuses first
                resources.setPermanentHealthBonus(packet.permanentHealthBonus);
                resources.setPermanentHealthRegenBonus(packet.permanentHealthRegenBonus);
                resources.setPermanentMaxQiBonus(packet.permanentMaxQiBonus);
                resources.setPermanentQiRegenBonus(packet.permanentQiRegenBonus);
                resources.setPermanentMaxBodyBonus(packet.permanentMaxBodyBonus);
                resources.setPermanentBodyRegenBonus(packet.permanentBodyRegenBonus);

                // Step 2: Force recalculation of max values from permanent bonuses
                // This is critical because setPermanent methods don't auto-recalculate
                resources.forceHealthRecalculation();

                // Step 3: Set current values (after max values are correct)
                resources.setCurrentHealth(packet.currentHealth);
                resources.setCurrentQi(packet.currentQi);
                resources.setCurrentBody(packet.currentBody);

                // Note: No direct setMaxHealth/setMaxQi/setMaxBody methods exist
                // Max values are calculated automatically from base + permanent bonuses
            }
        });
    }
}