package net.hyper.immortalcultivation.server.events.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.client.gui.screens.CultivationBreakthroughScreen;
import net.hyper.immortalcultivation.client.gui.screens.CultivationStatsScreen;
import net.hyper.immortalcultivation.common.registry.ICModKeyBinds;
import net.hyper.immortalcultivation.core.network.NetworkRegistry;
import net.hyper.immortalcultivation.core.network.packets.client.UnifiedActionRequestPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, value = Dist.CLIENT)
public class KeyInputEvent {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null) {
            return;
        }

        // We only want to handle key presses, not releases
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (ICModKeyBinds.CULTIVATION_STATS_KEY.consumeClick()) {
            mc.setScreen(new CultivationStatsScreen());
        } else if (ICModKeyBinds.BREAKTHROUGH_SCREEN_KEY.consumeClick()) {
            mc.setScreen(new CultivationBreakthroughScreen());
        } else if (ICModKeyBinds.CULTIVATION_FOCUS_TOGGLE_KEY.consumeClick()) {
            NetworkRegistry.sendToServer(UnifiedActionRequestPacket.createFocusToggle());
        }
    }
}
