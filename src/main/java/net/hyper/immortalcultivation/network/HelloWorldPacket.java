package net.hyper.immortalcultivation.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HelloWorldPacket(String message) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HelloWorldPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("immortalcultivation", "hello_world"));

    public static final StreamCodec<FriendlyByteBuf, HelloWorldPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, HelloWorldPacket::message,
            HelloWorldPacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends  CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HelloWorldPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            System.out.println("🦊 PACKET RECEIVED: " + packet.message());

            // Client-side detection
            if (ctx.flow().isClientbound()) {
                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§6[DEBUG] Client received: " + packet.message())
                );
            } else {
                // Server-side detection
                System.out.println("🔧 [SERVER] Processing: " + packet.message());
            }
        });
    }
}
