package net.hyper.immortalcultivation.registry;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Handles MOD BUS events related to keybind registration.
 *
 * This class is responsible for registering keybinds during mod initialization.
 * It only contains events that occur during the mod loading phase, which is why
 * it uses EventBusSubscriber.Bus.MOD.
 *
 * Key Learning Point: Mod bus events happen during initialization - they set up
 * your mod's content like keybinds, blocks, items, etc. These events implement
 * IModBusEvent and are called once during mod loading.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ICModKeyBinds {

    // Keybind category for organization in controls menu
    public static final String CULTIVATION_CATEGORY = "key.categories." + ImmortalCultivation.MOD_ID + ".cultivation";

    // Primary keybind for general cultivation monitoring
    public static final KeyMapping CULTIVATION_STATS_KEY = new KeyMapping(
        "key." + ImmortalCultivation.MOD_ID + ".cultivation_stats",    // Translation key
        GLFW.GLFW_KEY_C,                                               // Default key: C
        CULTIVATION_CATEGORY                                           // Category for organization
    );

    // Secondary keybind for breakthrough analysis (less frequent use)
    public static final KeyMapping BREAKTHROUGH_SCREEN_KEY = new KeyMapping(
        "key." + ImmortalCultivation.MOD_ID + ".breakthrough_screen",  // Translation key
        GLFW.GLFW_KEY_B,                                               // Default key: B
        CULTIVATION_CATEGORY                                           // Category for organization
    );

    /**
     * Registers cultivation-related keybinds with the game system.
     *
     * This event is called during mod initialization to set up keyboard shortcuts.
     * RegisterKeyMappingsEvent is a MOD BUS event because it happens during the
     * setup phase, not during gameplay.
     */
    @SubscribeEvent
    public static void registerKeybinds(RegisterKeyMappingsEvent event) {
        ImmortalCultivation.LOGGER.info("Registering cultivation keybinds...");

        // Register the general stats keybind (primary interface)
        event.register(CULTIVATION_STATS_KEY);

        // Register the breakthrough analysis keybind (specialized interface)
        event.register(BREAKTHROUGH_SCREEN_KEY);

        ImmortalCultivation.LOGGER.info("Cultivation keybinds registered successfully!");
    }
}