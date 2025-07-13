package net.hyper.immortalcultivation.common.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.command.ConfigCommand;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ICModCommands {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        // Existing commands
        new GetQiCommand(event.getDispatcher());
        new GetRealmCommand(event.getDispatcher());
        new SetCultivationCommand(event.getDispatcher());

        // NEW: Set commands with actual implementations
        new SetPhysiqueCommand(event.getDispatcher());
        new SetSpiritualRootCommand(event.getDispatcher());
        new SetRealmCommand(event.getDispatcher());

        // NEW: Random assignment commands
        new SetRandomPhysiqueCommand(event.getDispatcher());
        new SetRandomSpiritualRootCommand(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }
}