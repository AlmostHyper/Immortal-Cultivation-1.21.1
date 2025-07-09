package net.hyper.immortalcultivation.core.network.packets.server;

import net.hyper.immortalcultivation.common.attachments.PlayerCombatStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record PlayerCombatStatsSyncPacket(CompoundTag combatStatsData) implements ISyncPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("immortalcultivation", "player_combat_stats_sync");
    public static final CustomPacketPayload.Type<PlayerCombatStatsSyncPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, PlayerCombatStatsSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeNbt(packet.combatStatsData()),
            (buf) -> new PlayerCombatStatsSyncPacket(buf.readNbt())
    );

    @Override
    public CustomPacketPayload.Type<PlayerCombatStatsSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player != null) {
            PlayerCombatStatsAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (attachment != null) {
                attachment.applySync(this.combatStatsData);
            }
        }
    }
}
