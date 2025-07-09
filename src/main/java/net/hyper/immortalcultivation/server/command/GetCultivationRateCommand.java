package net.hyper.immortalcultivation.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GetCultivationRateCommand {
    public GetCultivationRateCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("get").then(Commands.literal("cultivationrate")
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
            PlayerTalentAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

            if (cultivation == null || talents == null) {
                context.getSource().sendFailure(Component.literal("Failed to get cultivation data"));
                return 0;
            }

            // Header
            context.getSource().sendSuccess(() ->
                Component.literal("=== Cultivation Rates ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

            // Current effective rates
            double qiRate = cultivation.getEffectiveQiCultivationRate();
            double bodyRate = cultivation.getEffectiveBodyCultivationRate();

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Current Qi Rate: %.3f/sec", qiRate))
                    .withStyle(ChatFormatting.AQUA), false);

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Current Body Rate: %.3f/sec", bodyRate))
                    .withStyle(ChatFormatting.RED), false);

            // Base rates from realms
            double qiBaseRate = cultivation.getBaseQiCultivationRate();
            double bodyBaseRate = cultivation.getBaseBodyCultivationRate();

            context.getSource().sendSuccess(() ->
                Component.literal("=== Base Rates (from realms) ===")
                    .withStyle(ChatFormatting.GRAY), false);

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Qi Base: %.3f/sec | Body Base: %.3f/sec", qiBaseRate, bodyBaseRate))
                    .withStyle(ChatFormatting.GRAY), false);

            // Talent multipliers
            float spiritRootMultiplier = talents.getSpiritualRootMultiplier();
            float physiqueMultiplier = talents.getPhysiqueMultiplier();
            float overallMultiplier = talents.getOverallCultivationMultiplier();

            context.getSource().sendSuccess(() ->
                Component.literal("=== Talent Multipliers ===")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Spiritual Root: %.1fx | Physique: %.1fx", spiritRootMultiplier, physiqueMultiplier))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Combined Talent Multiplier: %.2fx", overallMultiplier))
                    .withStyle(ChatFormatting.GREEN), false);

            // Focus bonus
            String currentFocus = cultivation.getCurrentFocus().getDisplayName();
            context.getSource().sendSuccess(() ->
                Component.literal("Focus: " + currentFocus + " (2x bonus for focused path)")
                    .withStyle(ChatFormatting.YELLOW), false);

            // Pill bonuses
            double qiBonus = cultivation.getQiAdditiveBonus();
            double bodyBonus = cultivation.getBodyAdditiveBonus();

            if (qiBonus > 0 || bodyBonus > 0) {
                context.getSource().sendSuccess(() ->
                    Component.literal("=== Pill Bonuses ===")
                        .withStyle(ChatFormatting.DARK_PURPLE), false);

                if (qiBonus > 0) {
                    context.getSource().sendSuccess(() ->
                        Component.literal(String.format("Qi Pills: +%.3f/sec", qiBonus))
                            .withStyle(ChatFormatting.AQUA), false);
                }

                if (bodyBonus > 0) {
                    context.getSource().sendSuccess(() ->
                        Component.literal(String.format("Body Pills: +%.3f/sec", bodyBonus))
                            .withStyle(ChatFormatting.RED), false);
                }
            }

            // Circulation rates (for meditation)
            double qiCirculationRate = cultivation.getEffectiveCirculatingQiCultivationRate();
            double bodyCirculationRate = cultivation.getEffectiveCirculatingBodyCultivationRate();

            context.getSource().sendSuccess(() ->
                Component.literal("=== Circulation Rates (during meditation) ===")
                    .withStyle(ChatFormatting.DARK_GREEN), false);

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Qi Circulation: %.3f/sec | Body Circulation: %.3f/sec",
                        qiCirculationRate, bodyCirculationRate))
                    .withStyle(ChatFormatting.DARK_GREEN), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error retrieving cultivation rates: " + e.getMessage()));
            return 0;
        }
    }
}