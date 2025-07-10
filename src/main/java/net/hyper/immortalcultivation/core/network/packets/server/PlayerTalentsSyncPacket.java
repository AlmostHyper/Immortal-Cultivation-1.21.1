package net.hyper.immortalcultivation.core.network.packets.server;

import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.server.event.world.ICModAttachmentUpdateEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

public record PlayerTalentsSyncPacket(CompoundTag talentData) implements ISyncPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("immortalcultivation", "player_talent_sync");
    public static final CustomPacketPayload.Type<PlayerTalentsSyncPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, PlayerTalentsSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeNbt(packet.talentData()),
            (buf) -> new PlayerTalentsSyncPacket(buf.readNbt())
    );

    @Override
    public CustomPacketPayload.Type<PlayerTalentsSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player != null) {
            PlayerTalentAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (attachment != null) {
                attachment.applySync(this.talentData);

                NeoForge.EVENT_BUS.post(new ICModAttachmentUpdateEvent(player, ICModAttachmentTypes.PLAYER_TALENTS.get()));
            }
        }
    }
}
