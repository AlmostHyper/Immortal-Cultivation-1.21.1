package net.hyper.immortalcultivation.core.network.packets;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.attachments.ICultivationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CultivationPacket(Action action, ICultivationData.CultivationType cultivationType, long amount, CompoundTag data)
    implements CustomPacketPayload {

    public enum Action {
        REQUEST_SYNC,
        SYNC_DATA,
        ADD_CULTIVATION,
        REMOVE_CULTIVATION,
        SET_CULTIVATION
    }

    public static final Type<CultivationPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT.map(i -> Action.values()[i], Enum::ordinal), CultivationPacket::action,
            ByteBufCodecs.INT.map(i -> ICultivationData.CultivationType.values()[i], Enum::ordinal), CultivationPacket::cultivationType,
            ByteBufCodecs.VAR_LONG, CultivationPacket::amount,
            ByteBufCodecs.COMPOUND_TAG, CultivationPacket::data,
            CultivationPacket::new
        );

    @Override
    public Type<CultivationPacket> type() {
        return TYPE;
    }
}
