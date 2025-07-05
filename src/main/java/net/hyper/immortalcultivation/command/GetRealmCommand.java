package net.hyper.immortalcultivation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Enhanced GetRealmCommand for the unified cultivation system.
 *
 * This command now provides comprehensive information about both cultivation paths,
 * helping players understand their complete cultivation state and the relationships
 * between different system components using the new unified attachment system.
 */
public class GetRealmCommand {
    public GetRealmCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register the enhanced command with clear, descriptive structure
        dispatcher.register(Commands.literal("get")
            .then(Commands.literal("realm")
                .executes(this::executeRealmInfo)) // Show comprehensive cultivation info
            .then(Commands.literal("cultivation")
                .executes(this::executeCultivationInfo)) // Show detailed cultivation progress
            .then(Commands.literal("resources")
                .executes(this::executeResourceInfo))); // Show current resource levels
    }

    /**
     * Displays comprehensive realm and cultivation information.
     * This is the main command that gives players a complete overview of their cultivation state.
     */
    private int executeRealmInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            // Gather all cultivation data for comprehensive display
            CultivationDataSnapshot snapshot = gatherPlayerCultivationData(player);
            if (snapshot == null) {
                context.getSource().sendFailure(Component.literal("Failed to retrieve cultivation data"));
                return 0;
            }

            // Send comprehensive cultivation state information
            sendCultivationOverview(context.getSource(), snapshot);
            sendRealmInformation(context.getSource(), snapshot);
            sendFocusInformation(context.getSource(), snapshot);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error retrieving realm info: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Displays detailed cultivation progress information.
     * This focuses specifically on cultivation advancement and breakthrough readiness.
     */
    private int executeCultivationInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            CultivationDataSnapshot snapshot = gatherPlayerCultivationData(player);
            if (snapshot == null) {
                context.getSource().sendFailure(Component.literal("Failed to retrieve cultivation data"));
                return 0;
            }

            sendDetailedCultivationProgress(context.getSource(), snapshot);
            sendBreakthroughReadiness(context.getSource(), snapshot);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error retrieving cultivation info: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Displays current resource levels (Health, Qi Energy, Body Energy).
     * This helps players understand their consumable resources versus permanent cultivation.
     */
    private int executeResourceInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            CultivationDataSnapshot snapshot = gatherPlayerCultivationData(player);
            if (snapshot == null) {
                context.getSource().sendFailure(Component.literal("Failed to retrieve cultivation data"));
                return 0;
            }

            sendResourceInformation(context.getSource(), snapshot);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error retrieving resource info: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Gathers all cultivation-related data from the player using unified attachments.
     * This creates a consistent snapshot for display purposes.
     */
    private CultivationDataSnapshot gatherPlayerCultivationData(ServerPlayer player) {
        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            PlayerTalentAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());

            // Verify we have critical data
            if (cultivation == null || resources == null || talents == null) {
                return null;
            }

            return new CultivationDataSnapshot(cultivation, resources, talents, combatStats);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Sends a high-level overview of the player's cultivation state.
     * This provides the "big picture" of their advancement in both paths.
     */
    private void sendCultivationOverview(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        source.sendSuccess(() -> Component.literal("=== Dual Cultivation Overview ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        // Current focus with strategic context
        String focusName = snapshot.cultivation.getCurrentFocus().getDisplayName();
        source.sendSuccess(() -> Component.literal("Current Focus: " + focusName + " Cultivation (2x rate)")
            .withStyle(ChatFormatting.YELLOW), false);

        // Talent information
        String talentDesc = snapshot.talents.getCombinedTalentDescription();
        source.sendSuccess(() -> Component.literal("Talent Level: " + talentDesc)
            .withStyle(ChatFormatting.LIGHT_PURPLE), false);

        // Overall multiplier
        float overallMultiplier = snapshot.talents.getOverallCultivationMultiplier();
        source.sendSuccess(() -> Component.literal("Overall Talent Multiplier: " +
                String.format("%.2fx", overallMultiplier))
            .withStyle(ChatFormatting.GREEN), false);
    }

    /**
     * Sends detailed information about both cultivation realm states.
     * This helps players understand their position in both advancement paths.
     */
    private void sendRealmInformation(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        // Qi Realm Information
        String qiRealm = snapshot.cultivation.getCurrentQiRealm().getDisplayName();
        source.sendSuccess(() -> Component.literal("Qi Realm: " + qiRealm)
            .withStyle(ChatFormatting.AQUA), false);

        // Body Realm Information
        String bodyRealm = snapshot.cultivation.getCurrentBodyRealm().getDisplayName();
        source.sendSuccess(() -> Component.literal("Body Realm: " + bodyRealm)
            .withStyle(ChatFormatting.RED), false);

        // Overall cultivation level comparison
        String higherPath = snapshot.cultivation.getCurrentQiRealm().getRealmLevel() >=
            snapshot.cultivation.getCurrentBodyRealm().getRealmLevel() ? "Qi" : "Body";
        source.sendSuccess(() -> Component.literal("Highest Path: " + higherPath + " Cultivation")
            .withStyle(ChatFormatting.GREEN), false);
    }

    /**
     * Sends information about cultivation focus and its strategic implications.
     * This teaches players about the focus system's benefits and timing.
     */
    private void sendFocusInformation(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        // Focus switch availability (simplified check)
        String currentFocus = snapshot.cultivation.getCurrentFocus().getDisplayName();
        String switchTo = snapshot.cultivation.getCurrentFocus() == PlayerCultivationAttachment.CultivationType.QI ? "Body" : "Qi";

        source.sendSuccess(() -> Component.literal("Focus Switch: Can switch to " + switchTo)
            .withStyle(ChatFormatting.GREEN), false);

        // Show current bonuses
        source.sendSuccess(() -> Component.literal("Qi Cultivation Rate: " +
                String.format("%.2f/sec", snapshot.cultivation.getEffectiveQiCultivationRate()))
            .withStyle(ChatFormatting.AQUA), false);

        source.sendSuccess(() -> Component.literal("Body Cultivation Rate: " +
                String.format("%.2f/sec", snapshot.cultivation.getEffectiveBodyCultivationRate()))
            .withStyle(ChatFormatting.RED), false);
    }

    /**
     * Sends detailed cultivation progress for both paths.
     * This shows specific numbers and percentages for advancement tracking.
     */
    private void sendDetailedCultivationProgress(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        source.sendSuccess(() -> Component.literal("=== Cultivation Progress ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        // Qi Cultivation Progress
        long qiCurrent = snapshot.cultivation.getCurrentQiCultivation();
        long qiMax = snapshot.cultivation.getMaxQiCultivation();
        double qiPercent = snapshot.cultivation.getQiProgress() * 100;

        source.sendSuccess(() -> Component.literal(String.format("Qi Cultivation: %d/%d (%.1f%%)",
            qiCurrent, qiMax, qiPercent)).withStyle(ChatFormatting.AQUA), false);

        // Body Cultivation Progress
        long bodyCurrent = snapshot.cultivation.getCurrentBodyCultivation();
        long bodyMax = snapshot.cultivation.getMaxBodyCultivation();
        double bodyPercent = snapshot.cultivation.getBodyProgress() * 100;

        source.sendSuccess(() -> Component.literal(String.format("Body Cultivation: %d/%d (%.1f%%)",
            bodyCurrent, bodyMax, bodyPercent)).withStyle(ChatFormatting.RED), false);

        // Show bonuses if any
        double qiBonus = snapshot.cultivation.getQiAdditiveBonus();
        double bodyBonus = snapshot.cultivation.getBodyAdditiveBonus();

        if (qiBonus > 0) {
            source.sendSuccess(() -> Component.literal(String.format("Qi Bonus: +%.3f/sec from pills",
                qiBonus)).withStyle(ChatFormatting.YELLOW), false);
        }

        if (bodyBonus > 0) {
            source.sendSuccess(() -> Component.literal(String.format("Body Bonus: +%.3f/sec from pills",
                bodyBonus)).withStyle(ChatFormatting.YELLOW), false);
        }
    }

    /**
     * Sends breakthrough readiness information for both cultivation paths.
     * This helps players understand when they can attempt advancement.
     */
    private void sendBreakthroughReadiness(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        // Qi Breakthrough Readiness
        boolean qiReady = snapshot.cultivation.getCurrentQiCultivation() >= snapshot.cultivation.getMaxQiCultivation();
        String qiStatus = qiReady ? "Ready" : "Not Ready";
        ChatFormatting qiColor = qiReady ? ChatFormatting.GREEN : ChatFormatting.RED;

        source.sendSuccess(() -> Component.literal("Qi Breakthrough: " + qiStatus).withStyle(qiColor), false);

        // Body Breakthrough Readiness
        boolean bodyReady = snapshot.cultivation.getCurrentBodyCultivation() >= snapshot.cultivation.getMaxBodyCultivation();
        String bodyStatus = bodyReady ? "Ready" : "Not Ready";
        ChatFormatting bodyColor = bodyReady ? ChatFormatting.GREEN : ChatFormatting.RED;

        source.sendSuccess(() -> Component.literal("Body Breakthrough: " + bodyStatus).withStyle(bodyColor), false);

        // Show what's needed if not ready
        if (!qiReady) {
            long needed = snapshot.cultivation.getMaxQiCultivation() - snapshot.cultivation.getCurrentQiCultivation();
            source.sendSuccess(() -> Component.literal("Need " + needed + " more Qi cultivation for breakthrough")
                .withStyle(ChatFormatting.YELLOW), false);
        }

        if (!bodyReady) {
            long needed = snapshot.cultivation.getMaxBodyCultivation() - snapshot.cultivation.getCurrentBodyCultivation();
            source.sendSuccess(() -> Component.literal("Need " + needed + " more Body cultivation for breakthrough")
                .withStyle(ChatFormatting.YELLOW), false);
        }
    }

    /**
     * Sends information about consumable resources (Health, Qi Energy, Body Energy).
     * This helps players distinguish between permanent cultivation and consumable resources.
     */
    private void sendResourceInformation(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        source.sendSuccess(() -> Component.literal("=== Current Resources ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        // Health Information
        long healthCurrent = snapshot.resources.getCurrentHealth();
        long healthMax = snapshot.resources.getMaxHealth();
        double healthPercent = snapshot.resources.getHealthProgress() * 100;

        source.sendSuccess(() -> Component.literal(String.format("Health: %d/%d (%.1f%%)",
            healthCurrent, healthMax, healthPercent)).withStyle(ChatFormatting.LIGHT_PURPLE), false);

        // Qi Energy Information
        long qiCurrent = snapshot.resources.getCurrentQi();
        long qiMax = snapshot.resources.getMaxQi();
        double qiPercent = snapshot.resources.getQiProgress() * 100;

        source.sendSuccess(() -> Component.literal(String.format("Qi Energy: %d/%d (%.1f%%)",
            qiCurrent, qiMax, qiPercent)).withStyle(ChatFormatting.BLUE), false);

        // Body Energy Information
        long bodyCurrent = snapshot.resources.getCurrentBody();
        long bodyMax = snapshot.resources.getMaxBody();
        double bodyPercent = snapshot.resources.getBodyProgress() * 100;

        source.sendSuccess(() -> Component.literal(String.format("Body Energy: %d/%d (%.1f%%)",
            bodyCurrent, bodyMax, bodyPercent)).withStyle(ChatFormatting.DARK_RED), false);

    }

    /**
     * Data snapshot containing all cultivation information for consistent display using unified attachments.
     */
    private record CultivationDataSnapshot(
        PlayerCultivationAttachment cultivation,
        PlayerResourceAttachment resources,
        PlayerTalentAttachment talents,
        PlayerCombatStatsAttachment combatStats
    ) {}
}