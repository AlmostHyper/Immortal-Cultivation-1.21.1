package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.network.HelloWorldPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ICModNetwork {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1")
            .executesOn(HandlerThread.NETWORK);

        registrar.playBidirectional(
            HelloWorldPacket.TYPE,
            HelloWorldPacket.STREAM_CODEC,
            HelloWorldPacket::handle
        );
    }
}
