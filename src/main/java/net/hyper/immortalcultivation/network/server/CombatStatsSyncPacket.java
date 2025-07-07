package net.hyper.immortalcultivation.network.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCombatStatsAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet for syncing combat statistics bonuses to the client.
 * Moved to server package to match the unified attachment structure.
 */
public record CombatStatsSyncPacket(
    double attackDamageBonus,
    double movementSpeedBonus,
    double jumpHeightBonus,
    // Add permanent values
    double permanentAttackBonus,
    double permanentSpeedBonus,
    double permanentJumpBonus
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CombatStatsSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "combat_stats_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CombatStatsSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE, CombatStatsSyncPacket::attackDamageBonus,
            ByteBufCodecs.DOUBLE, CombatStatsSyncPacket::movementSpeedBonus,
            ByteBufCodecs.DOUBLE, CombatStatsSyncPacket::jumpHeightBonus,
            ByteBufCodecs.DOUBLE, CombatStatsSyncPacket::permanentAttackBonus,
            ByteBufCodecs.DOUBLE, CombatStatsSyncPacket::permanentSpeedBonus,
            ByteBufCodecs.DOUBLE, CombatStatsSyncPacket::permanentJumpBonus,
            CombatStatsSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(CombatStatsSyncPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            ctx.enqueueWork(() -> {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    LocalPlayer player = mc.player;
                    if (player != null) {
                        PlayerCombatStatsAttachment combatStats = PlayerCombatStatsAttachment.getOrCreate(player);

                        // Log the received values for debugging
                        ImmortalCultivation.LOGGER.debug("Received combat stats sync - Attack: {}%, Speed: {}%, Jump: {}%",
                            String.format("%.1f", msg.attackDamageBonus() * 100),
                            String.format("%.1f", msg.movementSpeedBonus() * 100),
                            String.format("%.1f", msg.jumpHeightBonus() * 100));

                        // Update both display AND permanent values
                        combatStats.updateFromServer(
                            msg.attackDamageBonus(), msg.movementSpeedBonus(), msg.jumpHeightBonus(),
                            msg.permanentAttackBonus(), msg.permanentSpeedBonus(), msg.permanentJumpBonus()
                        );

                        ImmortalCultivation.LOGGER.debug("Combat stats updated on client for player: {}",
                            player.getName().getString());
                    } else {
                        ImmortalCultivation.LOGGER.warn("Received combat stats sync but player is null!");
                    }
                } catch (Exception e) {
                    ImmortalCultivation.LOGGER.error("Error handling combat stats sync", e);
                }
            });
        }
    }
}