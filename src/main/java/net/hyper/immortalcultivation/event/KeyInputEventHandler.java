package net.hyper.immortalcultivation.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

import static net.hyper.immortalcultivation.registry.ICModKeyMappings.cultivationKey;

@EventBusSubscriber(modid = "immortalcultivation", value = Dist.CLIENT)
public class KeyInputEventHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (cultivationKey.consumeClick()) {
            PacketHandler.sendToServer(new AccelerateCultivationPacket());
        }
    }
}
