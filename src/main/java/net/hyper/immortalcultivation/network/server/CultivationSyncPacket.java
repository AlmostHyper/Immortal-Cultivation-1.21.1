package net.hyper.immortalcultivation.network.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sync packet for unified cultivation data.
 * Replaces: RealmSyncPacket, QiCultivationSyncPacket, BodyCultivationSyncPacket,
 * CultivationFocusSyncPacket, and CultivationBonusSyncPacket
 */
public record CultivationSyncPacket(
    // Realm data
    String currentQiRealmId,
    String previousQiRealmId,
    String currentBodyRealmId,
    String previousBodyRealmId,

    // Cultivation progress
    long currentQiCultivation,
    long maxQiCultivation,
    long currentBodyCultivation,
    long maxBodyCultivation,

    // Focus
    PlayerCultivationAttachment.CultivationType currentFocus,

    // Bonuses
    double qiAdditiveBonus,
    double qiMultiplicativeBonus,
    double bodyAdditiveBonus,
    double bodyMultiplicativeBonus
) implements CustomPacketPayload {

    public static final Type<CultivationSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "cultivation_sync")
    );

    // Custom codec for enum - write as string using ByteBufCodecs
    private static final StreamCodec<ByteBuf, PlayerCultivationAttachment.CultivationType> CULTIVATION_TYPE_CODEC =
        StreamCodec.of(
            (buf, type) -> ByteBufCodecs.STRING_UTF8.encode(buf, type.name()),
            buf -> {
                try {
                    return PlayerCultivationAttachment.CultivationType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
                } catch (IllegalArgumentException e) {
                    return PlayerCultivationAttachment.CultivationType.QI; // Default fallback
                }
            }
        );

    // Manual implementation due to parameter limits
    public static final StreamCodec<ByteBuf, CultivationSyncPacket> STREAM_CODEC = StreamCodec.of(
        CultivationSyncPacket::encode,
        CultivationSyncPacket::decode
    );

    private static void encode(ByteBuf buf, CultivationSyncPacket packet) {
        // Realm data
        ByteBufCodecs.STRING_UTF8.encode(buf, packet.currentQiRealmId);
        ByteBufCodecs.STRING_UTF8.encode(buf, packet.previousQiRealmId);
        ByteBufCodecs.STRING_UTF8.encode(buf, packet.currentBodyRealmId);
        ByteBufCodecs.STRING_UTF8.encode(buf, packet.previousBodyRealmId);

        // Cultivation progress
        ByteBufCodecs.VAR_LONG.encode(buf, packet.currentQiCultivation);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.maxQiCultivation);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.currentBodyCultivation);
        ByteBufCodecs.VAR_LONG.encode(buf, packet.maxBodyCultivation);

        // Focus
        CULTIVATION_TYPE_CODEC.encode(buf, packet.currentFocus);

        // Bonuses
        ByteBufCodecs.DOUBLE.encode(buf, packet.qiAdditiveBonus);
        ByteBufCodecs.DOUBLE.encode(buf, packet.qiMultiplicativeBonus);
        ByteBufCodecs.DOUBLE.encode(buf, packet.bodyAdditiveBonus);
        ByteBufCodecs.DOUBLE.encode(buf, packet.bodyMultiplicativeBonus);
    }

    private static CultivationSyncPacket decode(ByteBuf buf) {
        return new CultivationSyncPacket(
            // Realm data
            ByteBufCodecs.STRING_UTF8.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf),

            // Cultivation progress
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),

            // Focus
            CULTIVATION_TYPE_CODEC.decode(buf),

            // Bonuses
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(CultivationSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

            if (cultivation != null) {
                // Apply all synced data
                cultivation.applySync(
                    packet.currentQiRealmId,
                    packet.previousQiRealmId,
                    packet.currentBodyRealmId,
                    packet.previousBodyRealmId,
                    packet.currentQiCultivation,
                    packet.maxQiCultivation,
                    packet.currentBodyCultivation,
                    packet.maxBodyCultivation,
                    packet.currentFocus,
                    packet.qiAdditiveBonus,
                    packet.qiMultiplicativeBonus,
                    packet.bodyAdditiveBonus,
                    packet.bodyMultiplicativeBonus
                );
            }
        });
    }
}