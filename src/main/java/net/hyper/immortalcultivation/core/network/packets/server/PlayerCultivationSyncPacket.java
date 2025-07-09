package net.hyper.immortalcultivation.core.network.packets.server;

import com.mojang.datafixers.types.Type;
import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.Optional;

/**
 * A dedicated, delta-aware packet that respects the generic structure of PlayerCultivationAttachment.
 */
public record PlayerCultivationSyncPacket(
    byte fieldMask, // Bitmask to track which high-level fields are included
    Optional<CultivationDataPayload> qiCultivationData,
    Optional<CultivationDataPayload> bodyCultivationData,
    Optional<PlayerCultivationAttachment.CultivationType> focus
) implements ISyncPacket<PlayerCultivationAttachment> {

    // Top-level field masks
    public static final byte QI_CULTIVATION_MASK = 1;
    public static final byte BODY_CULTIVATION_MASK = 2;
    public static final byte FOCUS_MASK = 4;

    /**
     * A nested record to hold the data for a single cultivation type.
     * This avoids duplicating fields for Qi and Body.
     */
    public record CultivationDataPayload(
        byte fieldMask, // Bitmask for fields within this payload
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

    public static final Type<PlayerCultivationSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "sync.cultivation")
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

    public static void handle(PlayerCultivationSyncPacket packet, IPayloadContext context) {
        packet.handleClient(context, player -> player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get()));
    }
}
