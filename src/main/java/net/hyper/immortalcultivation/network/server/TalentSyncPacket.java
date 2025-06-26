package net.hyper.immortalcultivation.network.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.attachment.PlayerTalentAttachment;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.hyper.immortalcultivation.cultivation.PhysiqueType;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sync packet for unified talent data.
 * Replaces: SpiritualRootSyncPacket and PhysiqueSyncPacket
 */
public record TalentSyncPacket(
    String spiritualRootType,
    boolean spiritualRootAssigned,
    String physiqueType,
    boolean physiqueAssigned
) implements CustomPacketPayload {

    public static final Type<TalentSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "talent_data_sync")
    );

    public static final StreamCodec<ByteBuf, TalentSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, TalentSyncPacket::spiritualRootType,
        ByteBufCodecs.BOOL, TalentSyncPacket::spiritualRootAssigned,
        ByteBufCodecs.STRING_UTF8, TalentSyncPacket::physiqueType,
        ByteBufCodecs.BOOL, TalentSyncPacket::physiqueAssigned,
        TalentSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(TalentSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            PlayerTalentAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

            if (talents != null) {
                // Apply spiritual root
                try {
                    SpiritualRootType rootType = SpiritualRootType.valueOf(packet.spiritualRootType);
                    if (packet.spiritualRootAssigned) {
                        talents.forceReassignSpiritualRoot(rootType);
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid root type, keep current
                }

                // Apply physique
                try {
                    PhysiqueType physType = PhysiqueType.valueOf(packet.physiqueType);
                    if (packet.physiqueAssigned) {
                        talents.forceReassignPhysique(physType);
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid physique type, keep current
                }
            }
        });
    }
}