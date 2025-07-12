package net.hyper.immortalcultivation.server.event;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;

/**
 * Unified event manager that delegates to specialized handlers
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class ICModEventManager {
    private static final Map<Class<? extends Event>, IEventProcessor> processors = new HashMap<>();

    static {
        processors.put(PlayerTickEvent.Post.class, new TickEventProcessor());
        processors.put(LivingIncomingDamageEvent.class, new DamageEventProcessor());
        // etc.
    }

    @SubscribeEvent
    public static void onEvent(Event event) {
        var processor = processors.get(event.getClass());
        if (processor != null) {
            processor.process(event);
        }
    }
}
