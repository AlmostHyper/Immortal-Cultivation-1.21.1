package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.network.packets.BreakthroughPacket;
import net.hyper.immortalcultivation.core.network.packets.CultivationPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkRegistry {

    public enum Packets {
        // Client -> Server
        BREAKTHROUGH_ATTEMPT(BreakthroughPacket.class, BreakthroughPacket.STREAM_CODEC),

        // Bidirectional


        // Server -> Client
        CULTIVATION_SYNC(CultivationPacket.class, CultivationPacket.STREAM_CODEC);

        private final CustomPacketPayload.Type<?> type;
        private final StreamCodec<? super RegistryFriendlyByteBuf, ?> codec;

        <T extends CustomPacketPayload> Packets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            this.type = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, name().toLowerCase())
            );
            this.codec = codec;
        }

        @SuppressWarnings("unchecked")
        public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
            return (CustomPacketPayload.Type<T>) type;
        }

        @SuppressWarnings("unchecked")
        public <T extends CustomPacketPayload> StreamCodec<? super RegistryFriendlyByteBuf, T> getCodec() {
            return (StreamCodec<? super RegistryFriendlyByteBuf, T>) codec;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ImmortalCultivation.MOD_ID);

        // Client -> Server
        registrar.playToServer(Packets.CULTIVATION_SYNC.getType(), Packets.CULTIVATION_SYNC.getCodec(),
            (packet, context) -> packet.handle(context));
        //registrar.playToServer(Packets.BREAKTHROUGH_ATTEMPT.getType(), Packets.BREAKTHROUGH_ATTEMPT.getCodec(),
            //(packet, context) -> packet.handle(context));

        // Bidirectional

        // Server -> Client
        registrar.playToClient(Packets.CULTIVATION_SYNC.getType(), Packets.CULTIVATION_SYNC.getCodec(),
            (packet, context) -> packet.handle(context));
    }
}
