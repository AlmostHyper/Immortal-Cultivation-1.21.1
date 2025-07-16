package net.hyper.immortalcultivation.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeybindRegistry {

    public static final String CULTIVATION_CATEGORY = "key.categories." + ImmortalCultivation.MOD_ID + ".cultivation";

    public static final KeyMapping CULTIVATION_STATS_KEY = new KeyMapping(
        "key." + ImmortalCultivation.MOD_ID + ".cultivation_stats",
        GLFW.GLFW_KEY_C,
        CULTIVATION_CATEGORY
    );

    public static final KeyMapping BREAKTHROUGH_SCREEN_KEY = new KeyMapping(
        "key." + ImmortalCultivation.MOD_ID + ".breakthrough_screen",
        GLFW.GLFW_KEY_B,
        CULTIVATION_CATEGORY
    );

    public static final KeyMapping CULTIVATION_FOCUS_TOGGLE_KEY = new KeyMapping(
        "key." + ImmortalCultivation.MOD_ID + ".cultivation_focus_toggle",
        GLFW.GLFW_KEY_V,
        CULTIVATION_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeybinds(RegisterKeyMappingsEvent event) {
        ImmortalCultivation.LOGGER.info("Registering cultivation keybinds...");
        event.register(CULTIVATION_STATS_KEY);
        event.register(BREAKTHROUGH_SCREEN_KEY);
        event.register(CULTIVATION_FOCUS_TOGGLE_KEY);
        ImmortalCultivation.LOGGER.info("Cultivation keybinds registered successfully!");
    }
}
