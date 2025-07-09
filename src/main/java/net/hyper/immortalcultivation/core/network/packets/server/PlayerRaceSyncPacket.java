package net.hyper.immortalcultivation.core.network.packets.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerRaceAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record PlayerRaceSyncPacket(
        byte changedFields,
        RaceType raceType,
        boolean raceAssigned
) implements ISyncPacket {

    public static final byte RACE_TYPE_MASK = 1;
    public static final byte RACE_ASSIGNED_MASK = 2;

    public static final CustomPacketPayload.Type<PlayerRaceSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            new ResourceLocation(ImmortalCultivation.MOD_ID, "sync_race")
    );

    public static final StreamCodec<ByteBuf, PlayerRaceSyncPacket> STREAM_CODEC = StreamCodec.of(
            PlayerRaceSyncPacket::encode,
            PlayerRaceSyncPacket::decode
    );

    private static void encode(ByteBuf buf, PlayerRaceSyncPacket packet) {
        buf.writeByte(packet.changedFields);
        if ((packet.changedFields & RACE_TYPE_MASK) != 0) {
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.raceType.name());
        }
        if ((packet.changedFields & RACE_ASSIGNED_MASK) != 0) {
            buf.writeBoolean(packet.raceAssigned);
        }
    }

    private static PlayerRaceSyncPacket decode(ByteBuf buf) {
        byte changedFields = buf.readByte();
        RaceType raceType = RaceType.HUMAN;
        boolean raceAssigned = false;

        if ((changedFields & RACE_TYPE_MASK) != 0) {
            raceType = RaceType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
        }
        if ((changedFields & RACE_ASSIGNED_MASK) != 0) {
            raceAssigned = buf.readBoolean();
        }
        return new PlayerRaceSyncPacket(changedFields, raceType, raceAssigned);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player == null) return;
        PlayerRaceAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_RACE);
        if (attachment == null) return;

        attachment.updateFromServer(raceType, raceAssigned);
    }
}
