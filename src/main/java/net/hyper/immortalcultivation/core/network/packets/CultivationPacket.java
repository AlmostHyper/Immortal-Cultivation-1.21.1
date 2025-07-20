package net.hyper.immortalcultivation.core.network.packets;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.core.attachments.CultivationPlayerAttachment;
import net.hyper.immortalcultivation.core.attachments.ICultivationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

    public void handle(IPayloadContext context) {
        switch (action) {
            case REQUEST_SYNC -> handleSyncRequest(context);
            case SYNC_DATA -> handleSyncData(context);
            case ADD_CULTIVATION -> handleAddCultivation(context);
            case SET_CULTIVATION -> handleSetCultivation(context);
        }
    }

    private void handleSyncRequest (IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            ServerPlayer player = (ServerPlayer) context.player();
            CultivationPlayerAttachment attachment = player.getData(AttachmentRegistry.CULTIVATION.get());

            PacketDistributor.sendToPlayer(player, new CultivationPacket(Action.SYNC_DATA,
                cultivationType, attachment.getCurrentCultivation(cultivationType, attachment.serialize())));
        }
    }


}
