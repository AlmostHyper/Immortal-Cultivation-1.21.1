package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.command.GetQiCommand;
import net.hyper.immortalcultivation.command.GetRealmCommand;
import net.hyper.immortalcultivation.command.SetCultivationCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.command.ConfigCommand;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ICModCommands {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        new GetQiCommand(event.getDispatcher());
        new GetRealmCommand(event.getDispatcher());
        new SetCultivationCommand(event.getDispatcher());


        ConfigCommand.register(event.getDispatcher());
    }
}
