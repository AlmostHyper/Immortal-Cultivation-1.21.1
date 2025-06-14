package net.hyper.immortalcultivation.event;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.network.HelloWorldPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ICModClientEvent {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() == GLFW.GLFW_KEY_P && event.getAction() == GLFW.GLFW_PRESS) {
            // Debug: Send a test packet!
            PacketDistributor.sendToServer(new HelloWorldPacket("Test from client!"));
            System.out.println("🚀 [DEBUG] Packet sent from client!");
        }
    }
}
