package net.hyper.immortalcultivation.server.events.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.client.gui.overlays.CultivationOverlay;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class GuiEvent {

    public static final CultivationOverlay CULTIVATION_OVERLAY = new CultivationOverlay();

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_overlay"),
            (guiGraphics, partialTick) -> {
                int screenWidth = guiGraphics.guiWidth();
                int screenHeight = guiGraphics.guiHeight();
                CULTIVATION_OVERLAY.render(guiGraphics, partialTick, screenWidth, screenHeight);
            }
        );
    }
}
