package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ICModScreens {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiLayersEvent event) {
        // Register custom health bar
        event.registerAboveAll(
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "custom_health_bar"),
            net.hyper.immortalcultivation.screen.CustomHealthBarScreen::renderXianxiaHealthBar
        );
    }
}
