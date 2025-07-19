package net.hyper.immortalcultivation.server.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class OnTickEvent {
    public static final int CULTIVATION_RATE = 20;
    public static final int REGENERATION = 10;

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent event) {}

    @SubscribeEvent
    static void onTick(LivingEvent.LivingTickEvent event) {}


}
