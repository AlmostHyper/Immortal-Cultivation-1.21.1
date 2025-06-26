package net.hyper.immortalcultivation;

import net.hyper.immortalcultivation.handler.CultivationTickHandler;
import net.hyper.immortalcultivation.handler.ICModHealthHandler;
import net.hyper.immortalcultivation.handler.ICModImmunityHandler;
import net.hyper.immortalcultivation.registry.*;
import net.hyper.immortalcultivation.ui.menus.PillFurnaceMenu;
import net.hyper.immortalcultivation.ui.screens.PillFurnaceScreen;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ImmortalCultivation.MOD_ID)
public class ImmortalCultivation
{
    public static final String MOD_ID = "immortalcultivation";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ImmortalCultivation(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Immortal Cultivation mod...");

        NeoForge.EVENT_BUS.register(this);

        ICModBlocks.register(modEventBus);
        ICModItems.register(modEventBus);

        ICModBlockEntities.register(modEventBus);

        ICModTabs.register(modEventBus);
        ICModArmorMaterials.register(modEventBus);

        ICModAttachmentTypes.register(modEventBus);
        ICModMenuTypes.register(modEventBus);
        ICModRecipes.register(modEventBus);

        LOGGER.info("Registering Handlers...");

        NeoForge.EVENT_BUS.register(ICModImmunityHandler.class);
        NeoForge.EVENT_BUS.register(ICModHealthHandler.class);
        NeoForge.EVENT_BUS.register(CultivationTickHandler.class);

        LOGGER.info("Handlers registered successfully!");

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, ImmortalCultivationConfig.SPEC);

        LOGGER.info("Immortal Cultivation mod initialization complete.");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
            LOGGER.info("Registering menu screens...");

            event.register(ICModMenuTypes.PILL_FURNACE_MENU.get(), PillFurnaceScreen::new);

            LOGGER.info("Menu screens registered successfully!");
        }
    }
}