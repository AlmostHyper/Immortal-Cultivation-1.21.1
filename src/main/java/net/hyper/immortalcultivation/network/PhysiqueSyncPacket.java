package net.hyper.immortalcultivation.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PhysiqueSyncPacket(int physique, boolean hasBeenAssigned) implements CustomPacketPayload {
    public static final Type<PhysiqueSyncPacket> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "physique_data"));

    // FIXED: Now properly encodes/decodes both parameters
    public static final StreamCodec<RegistryFriendlyByteBuf, PhysiqueSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PhysiqueSyncPacket::physique,
            ByteBufCodecs.BOOL, PhysiqueSyncPacket::hasBeenAssigned,
            PhysiqueSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleClient(PhysiqueSyncPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            ctx.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player != null) {
                    var physique = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
                    if (physique != null) {
                        // FIXED: Now uses the hasBeenAssigned from the packet, not the current client state
                        physique.updateFromServer(msg.physique, msg.hasBeenAssigned);

                        ImmortalCultivation.LOGGER.debug("Client updated physique: ordinal={}, assigned={}",
                            msg.physique, msg.hasBeenAssigned);
                    }
                }
            });
        }
    }
}