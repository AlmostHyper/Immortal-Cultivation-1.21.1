package net.hyper.immortalcultivation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Scanner;

public class SetCultivationCommand {
    public SetCultivationCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("set").then(Commands.literal("cultivation")
            .then(Commands.argument("amount", LongArgumentType.longArg(0)) // Add argument for cultivation amount
                .executes(this::execute))));
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            // Get the cultivation amount from the command argument
            long newCultivation = LongArgumentType.getLong(context, "amount");

            PlayerCultivationAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (attachment == null) {
                context.getSource().sendFailure(Component.literal("Failed to get cultivation data"));
                return 0;
            }

            // Set the cultivation (assuming setCurrentCultivation doesn't return the current value)
            attachment.setCurrentCultivation(newCultivation);

            context.getSource().sendSuccess(() ->
                Component.literal("Your cultivation has been set to: " + newCultivation), true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error setting cultivation: " + e.getMessage()));
            return 0;
        }
    }
}
