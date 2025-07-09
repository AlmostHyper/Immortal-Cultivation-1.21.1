package net.hyper.immortalcultivation.core.network.packets.server;

import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record PlayerTalentSyncPacket(CompoundTag talentData) implements ISyncPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("immortalcultivation", "player_talent_sync");
    public static final CustomPacketPayload.Type<PlayerTalentSyncPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, PlayerTalentSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeNbt(packet.talentData()),
            (buf) -> new PlayerTalentSyncPacket(buf.readNbt())
    );

    @Override
    public CustomPacketPayload.Type<PlayerTalentSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player != null) {
            PlayerTalentAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (attachment != null) {
                attachment.applySync(this.talentData);
            }
        }
    }
}
