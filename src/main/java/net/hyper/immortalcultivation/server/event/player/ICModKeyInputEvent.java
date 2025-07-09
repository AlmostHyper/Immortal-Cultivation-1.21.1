package net.hyper.immortalcultivation.server.event.player;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.client.gui.screens.CultivationBreakthroughScreen;
import net.hyper.immortalcultivation.client.gui.screens.CultivationStatsScreen;
import net.hyper.immortalcultivation.common.registry.ICModKeyBinds;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.hyper.immortalcultivation.core.network.packets.client.UnifiedActionRequestPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, value = Dist.CLIENT)
public class ICModKeyInputEvent {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null) {
            return;
        }

        if (event.getAction() == InputEvent.Key.RELEASE) {
            return;
        }

        if (ICModKeyBinds.CULTIVATION_STATS_KEY.consumeClick()) {
            mc.setScreen(new CultivationStatsScreen());
        } else if (ICModKeyBinds.BREAKTHROUGH_SCREEN_KEY.consumeClick()) {
            mc.setScreen(new CultivationBreakthroughScreen());
        } else if (ICModKeyBinds.CULTIVATION_FOCUS_TOGGLE_KEY.consumeClick()) {
            ICModPacketRegistry.CHANNEL.sendToServer(UnifiedActionRequestPacket.createFocusToggle());
        }
    }
}
