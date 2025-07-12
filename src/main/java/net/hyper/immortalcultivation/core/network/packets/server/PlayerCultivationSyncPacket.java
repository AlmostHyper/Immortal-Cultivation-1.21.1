package net.hyper.immortalcultivation.core.network.packets.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.server.event.server.ICModAttachmentUpdateEvent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

public record PlayerCultivationSyncPacket(
        byte fieldMask,
        Optional<CultivationDataPayload> qiCultivationData,
        Optional<CultivationDataPayload> bodyCultivationData,
        Optional<PlayerCultivationAttachment.CultivationType> focus
) implements ISyncPacket {

    public static final byte QI_CULTIVATION_MASK = 1;
    public static final byte BODY_CULTIVATION_MASK = 2;
    public static final byte FOCUS_MASK = 4;

    public record CultivationDataPayload(
            byte fieldMask,
            Optional<String> realmId,
            Optional<Long> current,
            Optional<Long> max
    ) {
        public static final byte REALM_ID_MASK = 1;
        public static final byte CURRENT_MASK = 2;
        public static final byte MAX_MASK = 4;

        public static void encode(ByteBuf buf, CultivationDataPayload payload) {
            buf.writeByte(payload.fieldMask);
            payload.realmId.ifPresent(s -> ByteBufCodecs.STRING_UTF8.encode(buf, s));
            payload.current.ifPresent(l -> ByteBufCodecs.VAR_LONG.encode(buf, l));
            payload.max.ifPresent(l -> ByteBufCodecs.VAR_LONG.encode(buf, l));
        }

        public static CultivationDataPayload decode(ByteBuf buf) {
            byte fieldMask = buf.readByte();
            Optional<String> realmId = (fieldMask & REALM_ID_MASK) != 0 ? Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf)) : Optional.empty();
            Optional<Long> current = (fieldMask & CURRENT_MASK) != 0 ? Optional.of(ByteBufCodecs.VAR_LONG.decode(buf)) : Optional.empty();
            Optional<Long> max = (fieldMask & MAX_MASK) != 0 ? Optional.of(ByteBufCodecs.VAR_LONG.decode(buf)) : Optional.empty();
            return new CultivationDataPayload(fieldMask, realmId, current, max);
        }
    }

    public static final CustomPacketPayload.Type<PlayerCultivationSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "sync_cultivation")
    );

    public static final StreamCodec<ByteBuf, PlayerCultivationSyncPacket> STREAM_CODEC = StreamCodec.of(
            PlayerCultivationSyncPacket::encode,
            PlayerCultivationSyncPacket::decode
    );

    private static void encode(ByteBuf buf, PlayerCultivationSyncPacket packet) {
        buf.writeByte(packet.fieldMask);
        packet.qiCultivationData.ifPresent(payload -> CultivationDataPayload.encode(buf, payload));
        packet.bodyCultivationData.ifPresent(payload -> CultivationDataPayload.encode(buf, payload));
        packet.focus.ifPresent(f -> ByteBufCodecs.STRING_UTF8.encode(buf, f.name()));
    }

    private static PlayerCultivationSyncPacket decode(ByteBuf buf) {
        byte fieldMask = buf.readByte();
        Optional<CultivationDataPayload> qiData = (fieldMask & QI_CULTIVATION_MASK) != 0 ? Optional.of(CultivationDataPayload.decode(buf)) : Optional.empty();
        Optional<CultivationDataPayload> bodyData = (fieldMask & BODY_CULTIVATION_MASK) != 0 ? Optional.of(CultivationDataPayload.decode(buf)) : Optional.empty();
        Optional<PlayerCultivationAttachment.CultivationType> focus = (fieldMask & FOCUS_MASK) != 0 ? Optional.of(PlayerCultivationAttachment.CultivationType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf))) : Optional.empty();
        return new PlayerCultivationSyncPacket(fieldMask, qiData, bodyData, focus);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player == null) return;
        PlayerCultivationAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION);
        if (attachment == null) return;

        this.qiCultivationData.ifPresent(data -> {
            data.realmId().ifPresent(id -> attachment.setRealm(PlayerCultivationAttachment.CultivationType.QI, ICModCultivationRealms.get(id)));
            data.current().ifPresent(val -> attachment.setCurrentCultivation(PlayerCultivationAttachment.CultivationType.QI, val));
            data.max().ifPresent(val -> attachment.setMaxCultivation(PlayerCultivationAttachment.CultivationType.QI, val));
        });

        this.bodyCultivationData.ifPresent(data -> {
            data.realmId().ifPresent(id -> attachment.setRealm(PlayerCultivationAttachment.CultivationType.BODY, ICModCultivationRealms.get(id)));
            data.current().ifPresent(val -> attachment.setCurrentCultivation(PlayerCultivationAttachment.CultivationType.BODY, val));
            data.max().ifPresent(val -> attachment.setMaxCultivation(PlayerCultivationAttachment.CultivationType.BODY, val));
        });

        this.focus.ifPresent(attachment::setCurrentFocus);

        NeoForge.EVENT_BUS.post(new ICModAttachmentUpdateEvent(player, ICModAttachmentTypes.PLAYER_CULTIVATION.get()));

        ImmortalCultivation.LOGGER.debug("Applied cultivation sync packet on client.");
    }
}
