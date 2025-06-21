package net.hyper.immortalcultivation.event;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.ui.screens.QiHUDScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiLayersEvent event) {
        // Register your Qi HUD overlay
        event.registerAboveAll(
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "qi_overlay"),
            QiHUDScreen::renderQiScreen
        );
    }
}


