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

    private int execute(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());

            if (resources == null) {
                context.getSource().sendFailure(Component.literal("Failed to get resource data"));
                return 0;
            }

            long currentQi = resources.getCurrentQi();
            long maxQi = resources.getMaxQi();
            double qiPercent = maxQi > 0 ? (double) currentQi / maxQi * 100 : 0.0;

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Qi Energy: %d/%d (%.1f%%)", currentQi, maxQi, qiPercent)),
                true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error retrieving Qi: " + e.getMessage()));
            return 0;
        }
    }
}
