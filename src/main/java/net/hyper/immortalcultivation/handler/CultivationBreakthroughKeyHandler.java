package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.screen.BreakthroughScreen;
import net.hyper.immortalcultivation.screen.CultivationBreakthroughScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CultivationBreakthroughKeyHandler {

    public static final KeyMapping BREAKTHROUGH_KEY = new KeyMapping(
        "key.immortalcultivation.breakthrough",
        GLFW.GLFW_KEY_B,
        "key.categories.immortalcultivation"
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(BREAKTHROUGH_KEY);
    }

    @EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, value = Dist.CLIENT)
    public static class ClientKeyEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (BREAKTHROUGH_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new CultivationBreakthroughScreen());
            }
        }
    }
}
