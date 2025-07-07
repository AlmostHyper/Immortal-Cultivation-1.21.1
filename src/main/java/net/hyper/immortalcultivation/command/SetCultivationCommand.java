package net.hyper.immortalcultivation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Enhanced SetCultivationCommand for the unified cultivation system.
 *
 * This command now supports setting cultivation values for both paths,
 * with clear specification of which cultivation type to modify.
 * It provides comprehensive feedback about the changes and their implications.
 *
 * Command structure:
 * /set cultivation qi <amount> - Sets Qi cultivation progress
 * /set cultivation body <amount> - Sets Body cultivation progress
 * /set cultivation focus <qi|body> - Changes cultivation focus
 */
public class SetCultivationCommand {
    public SetCultivationCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("set")
            .then(Commands.literal("cultivation")
                // Set Qi cultivation amount
                .then(Commands.literal("qi")
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                        .executes(this::executeSetQiCultivation)))
                // Set Body cultivation amount
                .then(Commands.literal("body")
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                        .executes(this::executeSetBodyCultivation)))
                // Set cultivation focus
                .then(Commands.literal("focus")
                    .then(Commands.argument("type", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("qi");
                            builder.suggest("body");
                            return builder.buildFuture();
                        })
                        .executes(this::executeSetCultivationFocus)))));
    }

    /**
     * Sets the player's Qi cultivation to the specified amount.
     * Includes safety checks and comprehensive feedback about the change.
     */
    private int executeSetQiCultivation(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            long newCultivation = LongArgumentType.getLong(context, "amount");

            // Get the unified cultivation attachment
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) {
                context.getSource().sendFailure(Component.literal("Failed to get cultivation data"));
                return 0;
            }

            // Store old value for comparison
            long oldCultivation = cultivation.getCurrentQiCultivation();
            long maxCultivation = cultivation.getMaxQiCultivation();

            // Validate the new amount against current realm limits
            if (newCultivation > maxCultivation) {
                context.getSource().sendFailure(Component.literal(
                    "Amount exceeds maximum for current Qi realm (" + maxCultivation + ")"));
                return 0;
            }

            // CRITICAL FIX: Update totalQiCultivationEarned and trigger milestone checks
            if (newCultivation > oldCultivation) {
                // Calculate the difference and add it properly
                long cultivationGained = newCultivation - oldCultivation;
                cultivation.addQiCultivation(cultivationGained);
            } else {
                // If setting to a lower value, just set directly (no milestones gained)
                cultivation.setCurrentQiCultivation(newCultivation);
            }

            // Provide comprehensive feedback
            context.getSource().sendSuccess(() ->
                Component.literal("Qi Cultivation set: " + oldCultivation + " → " + newCultivation)
                    .withStyle(ChatFormatting.AQUA), true);

            // Check if this change affects breakthrough readiness
            checkAndReportBreakthroughStatus(context.getSource(), cultivation, PlayerCultivationAttachment.CultivationType.QI);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error setting Qi cultivation: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Sets the player's Body cultivation to the specified amount.
     * Parallel implementation to Qi cultivation with Body-specific logic.
     */
    private int executeSetBodyCultivation(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            long newCultivation = LongArgumentType.getLong(context, "amount");

            // Get the unified cultivation attachment
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) {
                context.getSource().sendFailure(Component.literal("Failed to get cultivation data"));
                return 0;
            }

            // Store old value for comparison
            long oldCultivation = cultivation.getCurrentBodyCultivation();
            long maxCultivation = cultivation.getMaxBodyCultivation();

            // Validate the new amount against current realm limits
            if (newCultivation > maxCultivation) {
                context.getSource().sendFailure(Component.literal(
                    "Amount exceeds maximum for current Body realm (" + maxCultivation + ")"));
                return 0;
            }

            // CRITICAL FIX: Update totalBodyCultivationEarned and trigger milestone checks
            if (newCultivation > oldCultivation) {
                // Calculate the difference and add it properly
                long cultivationGained = newCultivation - oldCultivation;
                cultivation.addBodyCultivation(cultivationGained);
            } else {
                // If setting to a lower value, just set directly (no milestones gained)
                cultivation.setCurrentBodyCultivation(newCultivation);
            }

            // Provide comprehensive feedback
            context.getSource().sendSuccess(() ->
                Component.literal("Body Cultivation set: " + oldCultivation + " → " + newCultivation)
                    .withStyle(ChatFormatting.RED), true);

            // Check if this change affects breakthrough readiness
            checkAndReportBreakthroughStatus(context.getSource(), cultivation, PlayerCultivationAttachment.CultivationType.BODY);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error setting Body cultivation: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Changes the player's cultivation focus between Qi and Body cultivation.
     * This is a strategic command that affects advancement rates.
     */
    private int executeSetCultivationFocus(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            String focusType = StringArgumentType.getString(context, "type").toLowerCase();

            // Get the unified cultivation attachment
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) {
                context.getSource().sendFailure(Component.literal("Failed to get cultivation data"));
                return 0;
            }

            // Store old focus for comparison
            String oldFocus = cultivation.getCurrentFocus().getDisplayName();

            // Apply focus change using the toggle system or direct setting
            PlayerCultivationAttachment.CultivationType currentFocus = cultivation.getCurrentFocus();
            PlayerCultivationAttachment.CultivationType newFocus;

            switch (focusType) {
                case "qi" -> {
                    if (currentFocus == PlayerCultivationAttachment.CultivationType.QI) {
                        context.getSource().sendFailure(Component.literal("Already focused on Qi cultivation"));
                        return 0;
                    }
                    newFocus = PlayerCultivationAttachment.CultivationType.QI;
                }
                case "body" -> {
                    if (currentFocus == PlayerCultivationAttachment.CultivationType.BODY) {
                        context.getSource().sendFailure(Component.literal("Already focused on Body cultivation"));
                        return 0;
                    }
                    newFocus = PlayerCultivationAttachment.CultivationType.BODY;
                }
                default -> {
                    context.getSource().sendFailure(Component.literal("Invalid focus type. Use 'qi' or 'body'"));
                    return 0;
                }
            }

            // Attempt to toggle focus (this may have cooldown restrictions)
            boolean success = cultivation.toggleFocus();
            if (!success) {
                context.getSource().sendFailure(Component.literal("Focus switch failed - may be on cooldown"));
                return 0;
            }

            String newFocusName = cultivation.getCurrentFocus().getDisplayName();

            // Provide strategic feedback about the focus change
            context.getSource().sendSuccess(() ->
                Component.literal("Cultivation focus changed: " + oldFocus + " → " + newFocusName + " (2x rate)")
                    .withStyle(ChatFormatting.YELLOW), true);

            context.getSource().sendSuccess(() ->
                Component.literal("New focus will double advancement rate for " + newFocusName + " cultivation")
                    .withStyle(ChatFormatting.GREEN), false);

            // Show current rates
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Qi Rate: %.2f/sec | Body Rate: %.2f/sec",
                        cultivation.getEffectiveQiCultivationRate(),
                        cultivation.getEffectiveBodyCultivationRate()))
                    .withStyle(ChatFormatting.AQUA), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error setting cultivation focus: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Checks if a cultivation change affects breakthrough readiness and reports the status.
     * This provides immediate feedback about strategic implications of cultivation changes.
     */
    private void checkAndReportBreakthroughStatus(CommandSourceStack source, PlayerCultivationAttachment cultivation,
                                                  PlayerCultivationAttachment.CultivationType cultivationType) {
        try {
            boolean canBreakthrough;
            long current;
            long max;
            String pathName = cultivationType.getDisplayName();

            if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
                current = cultivation.getCurrentQiCultivation();
                max = cultivation.getMaxQiCultivation();
            } else {
                current = cultivation.getCurrentBodyCultivation();
                max = cultivation.getMaxBodyCultivation();
            }

            canBreakthrough = current >= max;

            if (canBreakthrough) {
                source.sendSuccess(() ->
                    Component.literal("✓ " + pathName + " breakthrough requirements now met!")
                        .withStyle(ChatFormatting.GREEN), false);
            } else {
                // Calculate how much more cultivation is needed
                long needed = max - current;

                source.sendSuccess(() ->
                    Component.literal("Need " + needed + " more " + pathName.toLowerCase() + " cultivation for breakthrough")
                        .withStyle(ChatFormatting.YELLOW), false);
            }
        } catch (Exception e) {
            // Silently ignore errors in status reporting - the main command succeeded
        }
    }
}