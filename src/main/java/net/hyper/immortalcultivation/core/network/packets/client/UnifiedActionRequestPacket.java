package net.hyper.immortalcultivation.core.network.packets.client;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.common.attachments.PlayerBreakthroughAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record UnifiedActionRequestPacket(
    ActionType actionType,
    Optional<PlayerCultivationAttachment.CultivationType> cultivationType,
    Optional<PlayerBreakthroughAttachment.BreakthroughType> breakthroughType,
    Optional<String> stringParam
) implements CustomPacketPayload {

    public static final Type<UnifiedActionRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "unified_action_request")
    );

    public enum ActionType {
        // --- Core Cultivation ---
        BREAKTHROUGH_REQUEST,
        CULTIVATION_FOCUS_TOGGLE,
        // --- Player Setup ---
        SELECT_RACE,
    }

    // --- Convenience Constructors ---

    public static UnifiedActionRequestPacket createDataRequest(ActionType type) {
        return new UnifiedActionRequestPacket(type, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static UnifiedActionRequestPacket createBreakthroughRequest(PlayerCultivationAttachment.CultivationType cType, PlayerBreakthroughAttachment.BreakthroughType bType) {
        return new UnifiedActionRequestPacket(ActionType.BREAKTHROUGH_REQUEST, Optional.of(cType), Optional.of(bType), Optional.empty());
    }

    public static UnifiedActionRequestPacket createFocusToggle() {
        return new UnifiedActionRequestPacket(ActionType.CULTIVATION_FOCUS_TOGGLE, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static UnifiedActionRequestPacket createRaceSelection(String raceId) {
        return new UnifiedActionRequestPacket(ActionType.SELECT_RACE, Optional.empty(), Optional.empty(), Optional.of(raceId));
    }

    // --- Serialization ---

    public static final StreamCodec<ByteBuf, UnifiedActionRequestPacket> STREAM_CODEC = StreamCodec.of(
        UnifiedActionRequestPacket::encode, UnifiedActionRequestPacket::decode
    );

    private static void encode(ByteBuf buf, UnifiedActionRequestPacket packet) {
        ByteBufCodecs.STRING_UTF8.encode(buf, packet.actionType.name());
        packet.cultivationType.ifPresentOrElse(
            val -> {
                buf.writeBoolean(true);
                ByteBufCodecs.STRING_UTF8.encode(buf, val.name());
            }, () -> buf.writeBoolean(false)
        );
        packet.breakthroughType.ifPresentOrElse(
            val -> {
                buf.writeBoolean(true);
                ByteBufCodecs.STRING_UTF8.encode(buf, val.name());
            }, () -> buf.writeBoolean(false)
        );
        packet.stringParam.ifPresentOrElse(
            val -> {
                buf.writeBoolean(true);
                ByteBufCodecs.STRING_UTF8.encode(buf, val);
            }, () -> buf.writeBoolean(false)
        );
    }

    private static UnifiedActionRequestPacket decode(ByteBuf buf) {
        ActionType actionType = ActionType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
        Optional<PlayerCultivationAttachment.CultivationType> cType = buf.readBoolean() ? Optional.of(PlayerCultivationAttachment.CultivationType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf))) : Optional.empty();
        Optional<PlayerBreakthroughAttachment.BreakthroughType> bType = buf.readBoolean() ? Optional.of(PlayerBreakthroughAttachment.BreakthroughType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf))) : Optional.empty();
        Optional<String> sParam = buf.readBoolean() ? Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf)) : Optional.empty();
        return new UnifiedActionRequestPacket(actionType, cType, bType, sParam);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
