package net.hyper.immortalcultivation.network.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.attachment.PlayerRaceAttachment;
import net.hyper.immortalcultivation.cultivation.RaceType;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sync packet for player race data.
 */
public record RaceSyncPacket(
    String raceType,
    boolean raceAssigned
) implements CustomPacketPayload {

    public static final Type<RaceSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "race_data")
    );

    public static final StreamCodec<ByteBuf, RaceSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, RaceSyncPacket::raceType,
        ByteBufCodecs.BOOL, RaceSyncPacket::raceAssigned,
        RaceSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(RaceSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            PlayerRaceAttachment race = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());

            if (race != null) {
                try {
                    RaceType type = RaceType.valueOf(packet.raceType);
                    if (packet.raceAssigned) {
                        race.forceReassignRace(type);
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid race type, keep current
                }
            }
        });
    }
}