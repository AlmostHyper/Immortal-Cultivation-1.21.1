package net.hyper.immortalcultivation.core.network.packets.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record PlayerTalentSyncPacket(
        byte changedFields,
        SpiritualRootType spiritualRootType,
        boolean spiritualRootAssigned,
        PhysiqueType physiqueType,
        boolean physiqueAssigned
) implements ISyncPacket {

    public static final byte SPIRITUAL_ROOT_TYPE_MASK = 1;
    public static final byte SPIRITUAL_ROOT_ASSIGNED_MASK = 2;
    public static final byte PHYSIQUE_TYPE_MASK = 4;
    public static final byte PHYSIQUE_ASSIGNED_MASK = 8;

    public static final CustomPacketPayload.Type<PlayerTalentSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            new ResourceLocation(ImmortalCultivation.MOD_ID, "sync_talent")
    );

    public static final StreamCodec<ByteBuf, PlayerTalentSyncPacket> STREAM_CODEC = StreamCodec.of(
            PlayerTalentSyncPacket::encode,
            PlayerTalentSyncPacket::decode
    );

    private static void encode(ByteBuf buf, PlayerTalentSyncPacket packet) {
        buf.writeByte(packet.changedFields);
        if ((packet.changedFields & SPIRITUAL_ROOT_TYPE_MASK) != 0) {
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.spiritualRootType.name());
        }
        if ((packet.changedFields & SPIRITUAL_ROOT_ASSIGNED_MASK) != 0) {
            buf.writeBoolean(packet.spiritualRootAssigned);
        }
        if ((packet.changedFields & PHYSIQUE_TYPE_MASK) != 0) {
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.physiqueType.name());
        }
        if ((packet.changedFields & PHYSIQUE_ASSIGNED_MASK) != 0) {
            buf.writeBoolean(packet.physiqueAssigned);
        }
    }

    private static PlayerTalentSyncPacket decode(ByteBuf buf) {
        byte changedFields = buf.readByte();
        SpiritualRootType spiritualRootType = SpiritualRootType.MORTAL_ROOT;
        boolean spiritualRootAssigned = false;
        PhysiqueType physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
        boolean physiqueAssigned = false;

        if ((changedFields & SPIRITUAL_ROOT_TYPE_MASK) != 0) {
            spiritualRootType = SpiritualRootType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
        }
        if ((changedFields & SPIRITUAL_ROOT_ASSIGNED_MASK) != 0) {
            spiritualRootAssigned = buf.readBoolean();
        }
        if ((changedFields & PHYSIQUE_TYPE_MASK) != 0) {
            physiqueType = PhysiqueType.valueOf(ByteBufCodeodes.STRING_UTF8.decode(buf));
        }
        if ((changedFields & PHYSIQUE_ASSIGNED_MASK) != 0) {
            physiqueAssigned = buf.readBoolean();
        }
        return new PlayerTalentSyncPacket(changedFields, spiritualRootType, spiritualRootAssigned, physiqueType, physiqueAssigned);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player == null) return;
        PlayerTalentAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS);
        if (attachment == null) return;

        attachment.updateFromServer(spiritualRootType, spiritualRootAssigned, physiqueType, physiqueAssigned);
    }
}
