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

public record SpiritualRootSyncPacket(int spiritualRoot, boolean hasBeenAssigned) implements CustomPacketPayload {
    public static final Type<SpiritualRootSyncPacket> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "spiritual_root_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpiritualRootSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpiritualRootSyncPacket::spiritualRoot,
            spiritualRoot1 -> new SpiritualRootSyncPacket(spiritualRoot1, false)
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleClient(SpiritualRootSyncPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            ctx.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player != null) {
                    var spiritualRoot = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
                    if (spiritualRoot != null) {
                        spiritualRoot.updateFromServer(msg.spiritualRoot, spiritualRoot.hasBeenAssigned());
                    }
                }

                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§6[DEBUG] Client received: " + msg.spiritualRoot));

            });
        }
    }
}
