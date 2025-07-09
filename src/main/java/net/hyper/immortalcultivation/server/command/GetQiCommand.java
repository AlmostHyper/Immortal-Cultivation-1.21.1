package net.hyper.immortalcultivation.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.hyper.immortalcultivation.common.attachments.PlayerResourceAttachment;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GetQiCommand {
    public GetQiCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("get").then(Commands.literal("qi")
            .executes(this::execute)));
    }

    private int execute(CommandContext<CommandSourceStack> commandSourceStackCommandContext) {
        try {
            ServerPlayer player = commandSourceStackCommandContext.getSource().getPlayer();
            if (player == null) {
                commandSourceStackCommandContext.getSource().sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            PlayerResourceAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (attachment == null) {
                commandSourceStackCommandContext.getSource().sendFailure(Component.literal("Failed to get resource data"));
                return 0;
            }

            long currentQi = attachment.getCurrentQi();
            long maxQi = attachment.getMaxQi();

            commandSourceStackCommandContext.getSource().sendSuccess(() ->
                Component.literal("Your current Qi Energy is: " + currentQi + "/" + maxQi +
                    " (" + String.format("%.1f", attachment.getQiProgress() * 100) + "%)"), true);
            return 1;
        } catch (Exception e) {
            commandSourceStackCommandContext.getSource().sendFailure(Component.literal("Error retrieving Qi: " + e.getMessage()));
            return 0;
        }
    }
}