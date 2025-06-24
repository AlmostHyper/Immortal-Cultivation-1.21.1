package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CombatStatsRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CombatStatsRequestPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "combat_stats_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CombatStatsRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new CombatStatsRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(CombatStatsRequestPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer serverPlayer) {
                    var combatStats = serverPlayer.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                    if (combatStats != null) {
                        combatStats.forceSync();
                    }
                }
            });
        }
    }
}