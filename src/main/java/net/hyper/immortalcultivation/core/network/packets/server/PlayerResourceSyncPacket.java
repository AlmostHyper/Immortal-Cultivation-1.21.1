package net.hyper.immortalcultivation.core.network.packets.server;

import net.hyper.immortalcultivation.common.attachments.PlayerResourceAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record PlayerResourceSyncPacket(CompoundTag resourceData) implements ISyncPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("immortalcultivation", "player_resource_sync");
    public static final CustomPacketPayload.Type<PlayerResourceSyncPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, PlayerResourceSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeNbt(packet.resourceData()),
            (buf) -> new PlayerResourceSyncPacket(buf.readNbt())
    );

    // --- FIELD CONSTANTS ---
    public static final String KEY_CURRENT_HEALTH = "cur_h";
    public static final String KEY_PERM_HEALTH_BONUS = "p_h_b";
    public static final String KEY_PERM_HEALTH_REGEN = "p_h_r";
    public static final String KEY_CURRENT_QI = "cur_q";
    public static final String KEY_PERM_QI_BONUS = "p_q_b";
    public static final String KEY_PERM_QI_REGEN = "p_q_r";
    public static final String KEY_CURRENT_BODY = "cur_b";
    public static final String KEY_PERM_BODY_BONUS = "p_b_b";
    public static final String KEY_PERM_BODY_REGEN = "p_b_r";

    @Override
    public CustomPacketPayload.Type<PlayerResourceSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player != null) {
            PlayerResourceAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (attachment != null) {
                attachment.applySync(this.resourceData);
            }
        }
    }
}
