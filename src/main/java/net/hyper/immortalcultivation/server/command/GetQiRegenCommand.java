package net.hyper.immortalcultivation.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.hyper.immortalcultivation.common.attachments.PlayerResourceAttachment;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GetQiRegenCommand {
    public GetQiRegenCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("get").then(Commands.literal("qiregen")
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

            // Header
            context.getSource().sendSuccess(() ->
                Component.literal("=== Qi Energy Regeneration ===")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);

            // Current Qi Energy status
            long currentQi = resources.getCurrentQi();
            long maxQi = resources.getMaxQi();
            double qiPercent = maxQi > 0 ? (double) currentQi / maxQi * 100 : 0.0;


            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Current Qi Energy: %d/%d (%.1f%%)", currentQi, maxQi, qiPercent))
                    .withStyle(ChatFormatting.BLUE), false);

            // Base regeneration rate
            double baseRate = resources.getBaseQiRegen();
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Base Regeneration: %.3f/sec", baseRate))
                    .withStyle(ChatFormatting.GRAY), false);

            // Effective regeneration rate (with bonuses)
            double effectiveRate = resources.getEffectiveQiRegen();
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Effective Regeneration: %.3f/sec", effectiveRate))
                    .withStyle(ChatFormatting.AQUA), false);

            // Time to full calculation
            if (currentQi < maxQi && effectiveRate > 0) {
                double timeToFull = (maxQi - currentQi) / effectiveRate;
                int minutes = (int) (timeToFull / 60);
                int seconds = (int) (timeToFull % 60);

                context.getSource().sendSuccess(() ->
                    Component.literal(String.format("Time to Full: %dm %ds", minutes, seconds))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            } else if (currentQi >= maxQi) {
                context.getSource().sendSuccess(() ->
                    Component.literal("Qi Energy: Full")
                        .withStyle(ChatFormatting.GREEN), false);
            }

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error retrieving Qi regeneration: " + e.getMessage()));
            return 0;
        }
    }
}
