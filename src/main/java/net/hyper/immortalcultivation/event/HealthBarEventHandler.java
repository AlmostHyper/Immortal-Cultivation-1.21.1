package net.hyper.immortalcultivation.event;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class HealthBarEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player != null && player.getMaxHealth() > 20.0f && !player.isCreative()) {
            // Try to identify and cancel health-related rendering
            event.setCanceled(false); // Don't cancel everything, just modify behavior
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null) return;

        LayeredDraw.Layer layer = event.getLayer();

        // Try multiple layer identifiers
        if (player.getMaxHealth() > 20.0f && !player.isCreative()) {
            if (layer.equals(VanillaGuiLayers.PLAYER_HEALTH) ||
                layer.toString().contains("health") ||
                layer.toString().contains("hearts")) {
                event.setCanceled(true);
            }
        }
    }

}