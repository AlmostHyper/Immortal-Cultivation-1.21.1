package net.hyper.immortalcultivation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.hyper.immortalcultivation.attachment.PlayerBodyCultivationAttachment;
import net.hyper.immortalcultivation.attachment.PlayerCultivationFocusAttachment;
import net.hyper.immortalcultivation.attachment.PlayerQiCultivationAttachment;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Enhanced SetCultivationCommand for the dual cultivation system.
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

            // Get the Qi cultivation attachment
            PlayerQiCultivationAttachment qiAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiAttachment == null) {
                context.getSource().sendFailure(Component.literal("Failed to get Qi cultivation data"));
                return 0;
            }

            // Store old value for comparison
            long oldCultivation = qiAttachment.getCurrentQiCultivation();
            long maxCultivation = qiAttachment.getMaxQiCultivation();

            // Validate the new amount against current realm limits
            if (newCultivation > maxCultivation) {
                context.getSource().sendFailure(Component.literal(
                    "Amount exceeds maximum for current Qi realm (" + maxCultivation + ")"));
                return 0;
            }

            // Apply the change
            qiAttachment.setCurrentQiCultivation(newCultivation);

            // Provide comprehensive feedback
            context.getSource().sendSuccess(() ->
                Component.literal("Qi Cultivation set: " + oldCultivation + " → " + newCultivation)
                    .withStyle(ChatFormatting.AQUA), true);

            // Check if this change affects breakthrough readiness
            checkAndReportBreakthroughStatus(context.getSource(), player, PlayerRealmAttachment.CultivationType.QI);

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

            // Get the Body cultivation attachment
            PlayerBodyCultivationAttachment bodyAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyAttachment == null) {
                context.getSource().sendFailure(Component.literal("Failed to get Body cultivation data"));
                return 0;
            }

            // Store old value for comparison
            long oldCultivation = bodyAttachment.getCurrentBodyCultivation();
            long maxCultivation = bodyAttachment.getMaxBodyCultivation();

            // Validate the new amount against current realm limits
            if (newCultivation > maxCultivation) {
                context.getSource().sendFailure(Component.literal(
                    "Amount exceeds maximum for current Body realm (" + maxCultivation + ")"));
                return 0;
            }

            // Apply the change
            bodyAttachment.setCurrentBodyCultivation(newCultivation);

            // Provide comprehensive feedback
            context.getSource().sendSuccess(() ->
                Component.literal("Body Cultivation set: " + oldCultivation + " → " + newCultivation)
                    .withStyle(ChatFormatting.RED), true);

            // Check if this change affects breakthrough readiness
            checkAndReportBreakthroughStatus(context.getSource(), player, PlayerRealmAttachment.CultivationType.BODY);

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

            // Get the cultivation focus attachment
            PlayerCultivationFocusAttachment focusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focusAttachment == null) {
                context.getSource().sendFailure(Component.literal("Failed to get cultivation focus data"));
                return 0;
            }

            // Check if focus switch is available
            if (!focusAttachment.canSwitchFocus()) {
                long remainingSeconds = focusAttachment.getRemainingCooldownTicks() / 20;
                context.getSource().sendFailure(Component.literal(
                    "Focus switch on cooldown: " + remainingSeconds + " seconds remaining"));
                return 0;
            }

            // Store old focus for comparison
            String oldFocus = focusAttachment.getCurrentFocus().getDisplayName();

            // Apply focus change
            switch (focusType) {
                case "qi" -> {
                    if (focusAttachment.isFocusedOnQi()) {
                        context.getSource().sendFailure(Component.literal("Already focused on Qi cultivation"));
                        return 0;
                    }
                    focusAttachment.focusOnQi();
                }
                case "body" -> {
                    if (focusAttachment.isFocusedOnBody()) {
                        context.getSource().sendFailure(Component.literal("Already focused on Body cultivation"));
                        return 0;
                    }
                    focusAttachment.focusOnBody();
                }
                default -> {
                    context.getSource().sendFailure(Component.literal("Invalid focus type. Use 'qi' or 'body'"));
                    return 0;
                }
            }

            String newFocus = focusAttachment.getCurrentFocus().getDisplayName();

            // Provide strategic feedback about the focus change
            context.getSource().sendSuccess(() ->
                Component.literal("Cultivation focus changed: " + oldFocus + " → " + newFocus + " (2x rate)")
                    .withStyle(ChatFormatting.YELLOW), true);

            context.getSource().sendSuccess(() ->
                Component.literal("New focus will double advancement rate for " + newFocus + " cultivation")
                    .withStyle(ChatFormatting.GREEN), false);

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
    private void checkAndReportBreakthroughStatus(CommandSourceStack source, ServerPlayer player,
                                                  PlayerRealmAttachment.CultivationType cultivationType) {
        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return;

            boolean canBreakthrough = realmAttachment.meetsBreakthroughRequirements(cultivationType);
            String pathName = cultivationType.getDisplayName();

            if (canBreakthrough) {
                source.sendSuccess(() ->
                    Component.literal("✓ " + pathName + " breakthrough requirements now met!")
                        .withStyle(ChatFormatting.GREEN), false);
            } else {
                // Calculate how much more cultivation is needed
                long current = cultivationType == PlayerRealmAttachment.CultivationType.QI ?
                    player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get()).getCurrentQiCultivation() :
                    player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get()).getCurrentBodyCultivation();

                long threshold = realmAttachment.getCurrentRealm(cultivationType).getBreakthroughCultivationThreshold();
                long needed = threshold - current;

                source.sendSuccess(() ->
                    Component.literal("Need " + needed + " more " + pathName.toLowerCase() + " cultivation for breakthrough")
                        .withStyle(ChatFormatting.YELLOW), false);
            }
        } catch (Exception e) {
            // Silently ignore errors in status reporting - the main command succeeded
        }
    }
}