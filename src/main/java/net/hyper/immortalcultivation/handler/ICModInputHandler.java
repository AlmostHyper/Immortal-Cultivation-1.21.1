package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModKeyBinds;
import net.hyper.immortalcultivation.ui.screens.CultivationStatsScreen;
import net.hyper.immortalcultivation.ui.screens.CultivationBreakthroughScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Handles NEOFORGE BUS events related to player input during gameplay.
 *
 * This class is responsible for responding to input events that occur during
 * actual gameplay. It uses EventBusSubscriber.Bus.NEOFORGE because input events
 * are runtime events that happen continuously while the game is running.
 *
 * Key Learning Point: NeoForge bus events happen during gameplay - they respond
 * to player actions, world changes, entity behaviors, etc. These events do NOT
 * implement IModBusEvent and are called repeatedly during normal game operation.
 *
 * Why This Separation Matters:
 * - Mod bus: "Set up my keybinds" (happens once during loading)
 * - NeoForge bus: "The player pressed a key, respond to it" (happens during play)
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, value = Dist.CLIENT)
public class ICModInputHandler {

    /**
     * Handles key input events for cultivation interface shortcuts.
     *
     * This event handler responds to key presses and opens the appropriate interfaces.
     * InputEvent.Key is a NEOFORGE BUS event because it represents a runtime
     * action that the player is taking during gameplay.
     *
     * Think of it this way:
     * - The MOD BUS event said "Hey game, I want to register these keybinds"
     * - The NEOFORGE BUS event says "Hey, the player just pressed one of those keys!"
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();

        // Only process key presses (not releases) and only when in-game
        // This check ensures we don't interfere with menu navigation
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.screen != null) {
            return;
        }

        // Ensure player is available and in-world
        // These safety checks prevent crashes when the player isn't fully loaded
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        // Handle cultivation stats keybind
        // consumeClick() returns true if this keybind was pressed and "consumes" the press
        // This prevents other systems from also responding to the same keypress
        if (ICModKeyBinds.CULTIVATION_STATS_KEY.consumeClick()) {
            ImmortalCultivation.LOGGER.debug("Opening cultivation stats screen via keybind");
            minecraft.setScreen(new CultivationStatsScreen());
        }

        // Handle breakthrough analysis keybind
        if (ICModKeyBinds.BREAKTHROUGH_SCREEN_KEY.consumeClick()) {
            ImmortalCultivation.LOGGER.debug("Opening breakthrough analysis screen via keybind");
            minecraft.setScreen(new CultivationBreakthroughScreen());
        }
    }
}
