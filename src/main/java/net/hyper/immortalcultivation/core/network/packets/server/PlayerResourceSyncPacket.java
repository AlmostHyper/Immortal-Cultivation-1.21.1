package net.hyper.immortalcultivation.core.network.packets.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerResourceAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record PlayerResourceSyncPacket(
        short changedFields,
        long currentHealth, long permanentHealthBonus, double permanentHealthRegenBonus,
        long currentQi, long permanentMaxQiBonus, double permanentQiRegenBonus,
        long currentBody, long permanentMaxBodyBonus, double permanentBodyRegenBonus
) implements ISyncPacket {

    public static final short CURRENT_HEALTH_MASK = 1;
    public static final short PERMANENT_HEALTH_BONUS_MASK = 2;
    public static final short PERMANENT_HEALTH_REGEN_BONUS_MASK = 4;
    public static final short CURRENT_QI_MASK = 8;
    public static final short PERMANENT_MAX_QI_BONUS_MASK = 16;
    public static final short PERMANENT_QI_REGEN_BONUS_MASK = 32;
    public static final short CURRENT_BODY_MASK = 64;
    public static final short PERMANENT_MAX_BODY_BONUS_MASK = 128;
    public static final short PERMANENT_BODY_REGEN_BONUS_MASK = 256;

    public static final CustomPacketPayload.Type<PlayerResourceSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            new ResourceLocation(ImmortalCultivation.MOD_ID, "sync_resource")
    );

    public static final StreamCodec<ByteBuf, PlayerResourceSyncPacket> STREAM_CODEC = StreamCodec.of(
            PlayerResourceSyncPacket::encode,
            PlayerResourceSyncPacket::decode
    );

    private static void encode(ByteBuf buf, PlayerResourceSyncPacket packet) {
        buf.writeShort(packet.changedFields);
        if ((packet.changedFields & CURRENT_HEALTH_MASK) != 0) buf.writeLong(packet.currentHealth);
        if ((packet.changedFields & PERMANENT_HEALTH_BONUS_MASK) != 0) buf.writeLong(packet.permanentHealthBonus);
        if ((packet.changedFields & PERMANENT_HEALTH_REGEN_BONUS_MASK) != 0) buf.writeDouble(packet.permanentHealthRegenBonus);
        if ((packet.changedFields & CURRENT_QI_MASK) != 0) buf.writeLong(packet.currentQi);
        if ((packet.changedFields & PERMANENT_MAX_QI_BONUS_MASK) != 0) buf.writeLong(packet.permanentMaxQiBonus);
        if ((packet.changedFields & PERMANENT_QI_REGEN_BONUS_MASK) != 0) buf.writeDouble(packet.permanentQiRegenBonus);
        if ((packet.changedFields & CURRENT_BODY_MASK) != 0) buf.writeLong(packet.currentBody);
        if ((packet.changedFields & PERMANENT_MAX_BODY_BONUS_MASK) != 0) buf.writeLong(packet.permanentMaxBodyBonus);
        if ((packet.changedFields & PERMANENT_BODY_REGEN_BONUS_MASK) != 0) buf.writeDouble(packet.permanentBodyRegenBonus);
    }

    private static PlayerResourceSyncPacket decode(ByteBuf buf) {
        short changedFields = buf.readShort();
        long currentHealth = (changedFields & CURRENT_HEALTH_MASK) != 0 ? buf.readLong() : 0;
        long permanentHealthBonus = (changedFields & PERMANENT_HEALTH_BONUS_MASK) != 0 ? buf.readLong() : 0;
        double permanentHealthRegenBonus = (changedFields & PERMANENT_HEALTH_REGEN_BONUS_MASK) != 0 ? buf.readDouble() : 0;
        long currentQi = (changedFields & CURRENT_QI_MASK) != 0 ? buf.readLong() : 0;
        long permanentMaxQiBonus = (changedFields & PERMANENT_MAX_QI_BONUS_MASK) != 0 ? buf.readLong() : 0;
        double permanentQiRegenBonus = (changedFields & PERMANENT_QI_REGEN_BONUS_MASK) != 0 ? buf.readDouble() : 0;
        long currentBody = (changedFields & CURRENT_BODY_MASK) != 0 ? buf.readLong() : 0;
        long permanentMaxBodyBonus = (changedFields & PERMANENT_MAX_BODY_BONUS_MASK) != 0 ? buf.readLong() : 0;
        double permanentBodyRegenBonus = (changedFields & PERMANENT_BODY_REGEN_BONUS_MASK) != 0 ? buf.readDouble() : 0;

        return new PlayerResourceSyncPacket(changedFields, currentHealth, permanentHealthBonus, permanentHealthRegenBonus,
                currentQi, permanentMaxQiBonus, permanentQiRegenBonus,
                currentBody, permanentMaxBodyBonus, permanentBodyRegenBonus);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player == null) return;
        PlayerResourceAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES);
        if (attachment == null) return;

        attachment.updateFromServer(currentHealth, permanentHealthBonus, permanentHealthRegenBonus,
                currentQi, permanentMaxQiBonus, permanentQiRegenBonus,
                currentBody, permanentMaxBodyBonus, permanentBodyRegenBonus);
    }
}
