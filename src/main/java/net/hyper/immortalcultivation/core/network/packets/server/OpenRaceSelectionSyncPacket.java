package net.hyper.immortalcultivation.core.network.packets.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.client.gui.screens.RaceSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from server to client to open the race selection screen.
 */
public record OpenRaceSelectionSyncPacket() implements CustomPacketPayload {

    public static final Type<OpenRaceSelectionSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "open_race_selection")
    );

    public static final StreamCodec<ByteBuf, OpenRaceSelectionSyncPacket> STREAM_CODEC = StreamCodec.unit(
        new OpenRaceSelectionSyncPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(OpenRaceSelectionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                minecraft.setScreen(new RaceSelectionScreen());
            });
        });
    }
}