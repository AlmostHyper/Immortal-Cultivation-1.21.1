package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerBodyAttachment;
import net.hyper.immortalcultivation.attachment.PlayerHealthAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HealthRequestPacket(Type requestType, long amount) implements CustomPacketPayload {

    public enum Type {
        SYNC_REQUEST,    // Request current health sync
        HEAL,           // Request healing
        DAMAGE,         // Request damage (for testing/admin purposes)
        SET_HEALTH      // Set specific health amount (admin)
    }

    public static final CustomPacketPayload.Type<HealthRequestPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "health_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HealthRequestPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(
                ordinal -> Type.values()[ordinal],
                Type::ordinal
            ), HealthRequestPacket::requestType,
            ByteBufCodecs.VAR_LONG, HealthRequestPacket::amount,
            HealthRequestPacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleServer(HealthRequestPacket packet, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    PlayerHealthAttachment healthAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());

                    if (healthAttachment != null) {
                        switch (packet.requestType()) {
                            case SYNC_REQUEST -> {
                                // Force sync current health to client
                                healthAttachment.forceSync();
                            }
                            case HEAL -> {
                                // Heal the player by the specified amount
                                healthAttachment.addHealth(packet.amount());
                            }
                            case DAMAGE -> {
                                // Damage the player (for testing/admin purposes)
                                if (serverPlayer.hasPermissions(2)) { // Only ops can damage
                                    healthAttachment.tryConsumeHealth(packet.amount());
                                }
                            }
                            case SET_HEALTH -> {
                                // Set specific health amount (admin only)
                                if (serverPlayer.hasPermissions(2)) { // Only ops can set health
                                    healthAttachment.setCurrentHealth(packet.amount());
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    // Convenience factory methods
    public static HealthRequestPacket syncRequest() {
        return new HealthRequestPacket(Type.SYNC_REQUEST, 0);
    }

    public static HealthRequestPacket heal(long amount) {
        return new HealthRequestPacket(Type.HEAL, amount);
    }

    public static HealthRequestPacket damage(long amount) {
        return new HealthRequestPacket(Type.DAMAGE, amount);
    }

    public static HealthRequestPacket setHealth(long amount) {
        return new HealthRequestPacket(Type.SET_HEALTH, amount);
    }
}
