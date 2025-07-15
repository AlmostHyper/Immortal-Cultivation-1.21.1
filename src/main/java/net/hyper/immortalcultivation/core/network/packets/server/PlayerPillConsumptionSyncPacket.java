package net.hyper.immortalcultivation.core.network.packets.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerPillConsumptionAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.server.events.server.AttachmentUpdateEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

public record PlayerPillConsumptionSyncPacket(
        CompoundTag pillConsumptionData
) implements ISyncPacket {

    public static final CustomPacketPayload.Type<PlayerPillConsumptionSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "sync_pill_consumption")
    );

    public static final StreamCodec<ByteBuf, PlayerPillConsumptionSyncPacket> STREAM_CODEC = StreamCodec.of(
            PlayerPillConsumptionSyncPacket::encode,
            PlayerPillConsumptionSyncPacket::decode
    );

    private static void encode(ByteBuf buf, PlayerPillConsumptionSyncPacket packet) {
        ByteBufCodecs.COMPOUND_TAG.encode(buf, packet.pillConsumptionData);
    }

    private static PlayerPillConsumptionSyncPacket decode(ByteBuf buf) {
        return new PlayerPillConsumptionSyncPacket(ByteBufCodecs.COMPOUND_TAG.decode(buf));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player == null) return;
        PlayerPillConsumptionAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION);
        if (attachment == null) return;

        attachment.applySync(pillConsumptionData);

        NeoForge.EVENT_BUS.post(new AttachmentUpdateEvent(player, ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get()));
    }
}
