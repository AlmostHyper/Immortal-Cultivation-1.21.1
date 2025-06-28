package net.hyper.immortalcultivation.network.client;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerRaceAttachment;
import net.hyper.immortalcultivation.cultivation.RaceType;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client to server when a player selects their race.
 */
public record RaceSelectionRequestPacket(String raceType) implements CustomPacketPayload {

    public static final Type<RaceSelectionRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "race_selection")
    );

    public static final StreamCodec<ByteBuf, RaceSelectionRequestPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, RaceSelectionRequestPacket::raceType,
        RaceSelectionRequestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(RaceSelectionRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlayerRaceAttachment raceAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_RACE.get());

                if (raceAttachment != null && !raceAttachment.hasRaceAssigned()) {
                    try {
                        RaceType selectedRace = RaceType.valueOf(packet.raceType);
                        raceAttachment.assignRace(selectedRace);

                        ImmortalCultivation.LOGGER.info("Player {} selected race: {}",
                            serverPlayer.getName().getString(),
                            selectedRace.getDisplayName());

                        // Force sync to ensure client has the data
                        raceAttachment.forceSync();

                    } catch (IllegalArgumentException e) {
                        ImmortalCultivation.LOGGER.error("Invalid race type received: {}", packet.raceType);
                    }
                } else if (raceAttachment != null) {
                    ImmortalCultivation.LOGGER.warn("Player {} tried to select race but already has one assigned",
                        serverPlayer.getName().getString());
                }
            }
        });
    }
}