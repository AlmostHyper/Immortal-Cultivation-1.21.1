package net.hyper.immortalcultivation.server.events.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.client.gui.screens.QiHUDScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GUIEvent {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiLayersEvent event) {
        // Register your Qi HUD overlay
        event.registerAboveAll(
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "qi_overlay"),
            QiHUDScreen::renderQiScreen
        );
    }

    // === GUI-RELATED PLAYER EVENTS === //

    /**
     * Subscribes to GAME bus for runtime client player events
     */
    @EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientPlayerEvents {

        /**
         * Handles player logout to clear GUI caches.
         * Prevents stale cultivation data from being displayed.
         *
         * @param event The client player logout event
         */
        @SubscribeEvent
        public static void onClientPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            LocalPlayer player = event.getPlayer();
            if (player != null) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[GUIEvent] Clearing HUD cache on player logout: {}",
                        player.getName().getString());
                }

                QiHUDScreen.invalidateCache();
            }
        }

        /**
         * Handles player respawn to refresh GUI data.
         * Ensures HUD displays correct data after respawn.
         *
         * @param event The client player respawn event
         */
        @SubscribeEvent
        public static void onClientPlayerRespawn(ClientPlayerNetworkEvent.Clone event) {
            LocalPlayer newPlayer = event.getNewPlayer();
            if (newPlayer != null) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[GUIEvent] Clearing HUD cache on player respawn: {}",
                        newPlayer.getName().getString());
                }

                QiHUDScreen.invalidateCache();
            }
        }

        /**
         * Handles dimension changes/login to refresh GUI.
         * Ensures HUD syncs properly when changing dimensions.
         *
         * @param event The client player login event
         */
        @SubscribeEvent
        public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            LocalPlayer player = event.getPlayer();
            if (player != null) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[GUIEvent] Clearing HUD cache on player login/dimension change: {}",
                        player.getName().getString());
                }

                QiHUDScreen.invalidateCache();
            }
        }
    }
}