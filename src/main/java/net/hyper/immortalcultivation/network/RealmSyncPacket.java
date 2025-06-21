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

public record RealmSyncPacket(long realmCultivation) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RealmSyncPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "realm_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RealmSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, RealmSyncPacket::realmCultivation,
            RealmSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleClient(RealmSyncPacket msg, IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            ctx.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player != null) {
                    var realm = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                    if (realm != null) {
                        realm.updateFromServer(msg.realmCultivation);
                    }
                }

                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§6[DEBUG] Client received: " + msg.realmCultivation));

            });
        }
    }
}
