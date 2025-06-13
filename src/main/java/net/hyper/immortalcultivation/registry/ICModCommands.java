package net.hyper.immortalcultivation.registry;

import com.mojang.brigadier.CommandDispatcher;
import net.hyper.immortalcultivation.component.PlayerQiComponent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "immortalcultivation")
public class ICModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("qi")
            .requires(source -> source.getEntity() instanceof ServerPlayer) // Player-only
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                PlayerQiComponent qi = player.getData(ICModAttachments.PLAYER_QI.get());

                long qiAmount = qi.getCurrentQi();
                player.sendSystemMessage(Component.literal("Your current Qi: " + qiAmount));
                return 1;
            })
        );

        dispatcher.register(Commands.literal("qiRegen")
            .requires(source -> source.getEntity() instanceof ServerPlayer) // Player-only
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                PlayerQiComponent qi = player.getData(ICModAttachments.PLAYER_QI.get());

                double qiRegenerationRate = qi.getEffectiveQiRegenerationRate();
                player.sendSystemMessage(Component.literal("Your current Qi Regen: " + qiRegenerationRate));
                return 1;
            })
        );
    }
}
