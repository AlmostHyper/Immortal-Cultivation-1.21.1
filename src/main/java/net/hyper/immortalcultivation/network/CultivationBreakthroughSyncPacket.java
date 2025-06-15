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

public record RealmSyncPacket(String realmId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RealmSyncPacket> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "realm_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RealmSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RealmSyncPacket::realmId,
            RealmSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handleClient(RealmSyncPacket packet, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player != null) {
                    var realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                    if (realmAttachment != null) {
                        realmAttachment.updateFromServer(packet.realmId());
                    }
                }

                // Optional debug message (remove later)
                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§6[DEBUG] Realm synced: " + packet.realmId()));
            });
        }
    }
}
