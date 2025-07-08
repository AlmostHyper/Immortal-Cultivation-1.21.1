package net.hyper.immortalcultivation.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.hyper.immortalcultivation.common.capability.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GetCultivationCommand {
    public GetCultivationCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("get").then(Commands.literal("cultivation")
            .executes(this::execute)));
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) {
                context.getSource().sendFailure(Component.literal("Failed to get cultivation data"));
                return 0;
            }

            // Header
            context.getSource().sendSuccess(() ->
                Component.literal("=== Cultivation Progress ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

            // Qi Cultivation
            long qiCurrent = cultivation.getCurrentQiCultivation();
            long qiMax = cultivation.getMaxQiCultivation();
            double qiPercent = cultivation.getQiProgress() * 100;

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Qi Cultivation: %d/%d (%.1f%%)", qiCurrent, qiMax, qiPercent))
                    .withStyle(ChatFormatting.AQUA), false);

            // Body Cultivation
            long bodyCurrent = cultivation.getCurrentBodyCultivation();
            long bodyMax = cultivation.getMaxBodyCultivation();
            double bodyPercent = cultivation.getBodyProgress() * 100;

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Body Cultivation: %d/%d (%.1f%%)", bodyCurrent, bodyMax, bodyPercent))
                    .withStyle(ChatFormatting.RED), false);

            // Current Focus
            String focus = cultivation.getCurrentFocus().getDisplayName();
            context.getSource().sendSuccess(() ->
                Component.literal("Current Focus: " + focus + " (2x rate)")
                    .withStyle(ChatFormatting.YELLOW), false);

            // Cultivation Rates
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Qi Rate: %.2f/sec | Body Rate: %.2f/sec",
                        cultivation.getEffectiveQiCultivationRate(),
                        cultivation.getEffectiveBodyCultivationRate()))
                    .withStyle(ChatFormatting.GREEN), false);

            // Bonuses (if any)
            double qiBonus = cultivation.getQiAdditiveBonus();
            double bodyBonus = cultivation.getBodyAdditiveBonus();

            if (qiBonus > 0 || bodyBonus > 0) {
                context.getSource().sendSuccess(() ->
                    Component.literal("=== Bonuses ===").withStyle(ChatFormatting.LIGHT_PURPLE), false);

                if (qiBonus > 0) {
                    context.getSource().sendSuccess(() ->
                        Component.literal(String.format("Qi Bonus: +%.3f/sec from pills", qiBonus))
                            .withStyle(ChatFormatting.AQUA), false);
                }

                if (bodyBonus > 0) {
                    context.getSource().sendSuccess(() ->
                        Component.literal(String.format("Body Bonus: +%.3f/sec from pills", bodyBonus))
                            .withStyle(ChatFormatting.RED), false);
                }
            }

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error retrieving cultivation: " + e.getMessage()));
            return 0;
        }
    }
}