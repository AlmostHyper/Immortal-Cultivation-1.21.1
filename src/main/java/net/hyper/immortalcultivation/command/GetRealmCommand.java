package net.hyper.immortalcultivation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Enhanced GetRealmCommand for the dual cultivation system.
 *
 * This command now provides comprehensive information about both cultivation paths,
 * helping players understand their complete cultivation state and the relationships
 * between different system components.
 *
 * The command serves both administrative and educational purposes:
 * - Shows current state of both Qi and Body cultivation paths
 * - Displays cultivation focus and its strategic implications
 * - Provides resource information (Health, Qi Energy, Body Energy)
 * - Helps players understand cultivation system interactions
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
     * Gathers all cultivation-related data from the player.
     * This creates a consistent snapshot for display purposes.
     */
    private CultivationDataSnapshot gatherPlayerCultivationData(ServerPlayer player) {
        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            PlayerQiCultivationAttachment qiCultivation = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            PlayerBodyCultivationAttachment bodyCultivation = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            PlayerHealthAttachment health = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            PlayerQiAttachment qi = player.getData(ICModAttachmentTypes.PLAYER_QI.get());
            PlayerBodyAttachment body = player.getData(ICModAttachmentTypes.PLAYER_BODY.get());
            PlayerSpiritualRootAttachment spiritualRoots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            PlayerPhysiqueAttachment physique = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());

            // Verify we have critical data
            if (realmAttachment == null || qiCultivation == null || bodyCultivation == null || focus == null) {
                return null;
            }

            return new CultivationDataSnapshot(realmAttachment, qiCultivation, bodyCultivation, focus,
                health, qi, body, spiritualRoots, physique);
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
        String focusName = snapshot.focus.getCurrentFocus().getDisplayName();
        source.sendSuccess(() -> Component.literal("Current Focus: " + focusName + " Cultivation (2x rate)")
            .withStyle(ChatFormatting.YELLOW), false);
    }

    /**
     * Sends detailed information about both cultivation realm states.
     * This helps players understand their position in both advancement paths.
     */
    private void sendRealmInformation(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        // Qi Realm Information
        String qiRealm = snapshot.realm.getCurrentQiRealm().getDisplayName();
        source.sendSuccess(() -> Component.literal("Qi Realm: " + qiRealm)
            .withStyle(ChatFormatting.AQUA), false);

        // Body Realm Information
        String bodyRealm = snapshot.realm.getCurrentBodyRealm().getDisplayName();
        source.sendSuccess(() -> Component.literal("Body Realm: " + bodyRealm)
            .withStyle(ChatFormatting.RED), false);

        // Overall cultivation level comparison
        String higherPath = snapshot.realm.getCurrentQiRealm().getRealmLevel() >=
            snapshot.realm.getCurrentBodyRealm().getRealmLevel() ? "Qi" : "Body";
        source.sendSuccess(() -> Component.literal("Highest Path: " + higherPath + " Cultivation")
            .withStyle(ChatFormatting.GREEN), false);
    }

    /**
     * Sends information about cultivation focus and its strategic implications.
     * This teaches players about the focus system's benefits and timing.
     */
    private void sendFocusInformation(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        // Focus switch availability
        if (snapshot.focus.canSwitchFocus()) {
            String switchTo = snapshot.focus.isFocusedOnQi() ? "Body" : "Qi";
            source.sendSuccess(() -> Component.literal("Focus Switch: Available (can switch to " + switchTo + ")")
                .withStyle(ChatFormatting.GREEN), false);
        } else {
            long remainingSeconds = snapshot.focus.getRemainingCooldownTicks() / 20;
            source.sendSuccess(() -> Component.literal("Focus Switch: Cooldown (" + remainingSeconds + "s remaining)")
                .withStyle(ChatFormatting.RED), false);
        }
    }

    /**
     * Sends detailed cultivation progress for both paths.
     * This shows specific numbers and percentages for advancement tracking.
     */
    private void sendDetailedCultivationProgress(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        source.sendSuccess(() -> Component.literal("=== Cultivation Progress ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        // Qi Cultivation Progress
        long qiCurrent = snapshot.qiCultivation.getCurrentQiCultivation();
        long qiMax = snapshot.qiCultivation.getMaxQiCultivation();
        double qiPercent = qiMax > 0 ? (double)qiCurrent / qiMax * 100 : 0;

        source.sendSuccess(() -> Component.literal(String.format("Qi Cultivation: %d/%d (%.1f%%)",
            qiCurrent, qiMax, qiPercent)).withStyle(ChatFormatting.AQUA), false);

        // Body Cultivation Progress
        long bodyCurrent = snapshot.bodyCultivation.getCurrentBodyCultivation();
        long bodyMax = snapshot.bodyCultivation.getMaxBodyCultivation();
        double bodyPercent = bodyMax > 0 ? (double)bodyCurrent / bodyMax * 100 : 0;

        source.sendSuccess(() -> Component.literal(String.format("Body Cultivation: %d/%d (%.1f%%)",
            bodyCurrent, bodyMax, bodyPercent)).withStyle(ChatFormatting.RED), false);
    }

    /**
     * Sends breakthrough readiness information for both cultivation paths.
     * This helps players understand when they can attempt advancement.
     */
    private void sendBreakthroughReadiness(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        // Qi Breakthrough Readiness
        boolean qiReady = snapshot.realm.meetsBreakthroughRequirements(PlayerRealmAttachment.CultivationType.QI);
        boolean qiOnCooldown = snapshot.realm.isOnBreakthroughCooldown(PlayerRealmAttachment.CultivationType.QI);

        String qiStatus = qiReady ? "Ready" : "Not Ready";
        if (qiOnCooldown) {
            long qiCooldown = snapshot.realm.getRemainingCooldownTicks(PlayerRealmAttachment.CultivationType.QI) / 20;
            qiStatus += " (Cooldown: " + qiCooldown + "s)";
        }

        ChatFormatting qiColor = qiReady && !qiOnCooldown ? ChatFormatting.GREEN : ChatFormatting.RED;
        String finalQiStatus = qiStatus;
        source.sendSuccess(() -> Component.literal("Qi Breakthrough: " + finalQiStatus).withStyle(qiColor), false);

        // Body Breakthrough Readiness
        boolean bodyReady = snapshot.realm.meetsBreakthroughRequirements(PlayerRealmAttachment.CultivationType.BODY);
        boolean bodyOnCooldown = snapshot.realm.isOnBreakthroughCooldown(PlayerRealmAttachment.CultivationType.BODY);

        String bodyStatus = bodyReady ? "Ready" : "Not Ready";
        if (bodyOnCooldown) {
            long bodyCooldown = snapshot.realm.getRemainingCooldownTicks(PlayerRealmAttachment.CultivationType.BODY) / 20;
            bodyStatus += " (Cooldown: " + bodyCooldown + "s)";
        }

        ChatFormatting bodyColor = bodyReady && !bodyOnCooldown ? ChatFormatting.GREEN : ChatFormatting.RED;
        String finalBodyStatus = bodyStatus;
        source.sendSuccess(() -> Component.literal("Body Breakthrough: " + finalBodyStatus).withStyle(bodyColor), false);
    }

    /**
     * Sends information about consumable resources (Health, Qi Energy, Body Energy).
     * This helps players distinguish between permanent cultivation and consumable resources.
     */
    private void sendResourceInformation(CommandSourceStack source, CultivationDataSnapshot snapshot) {
        source.sendSuccess(() -> Component.literal("=== Current Resources ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        // Health Information
        if (snapshot.health != null) {
            long healthCurrent = snapshot.health.getCurrentHealth();
            long healthMax = snapshot.health.getMaxHealth();
            double healthPercent = healthMax > 0 ? (double)healthCurrent / healthMax * 100 : 0;

            source.sendSuccess(() -> Component.literal(String.format("Health: %d/%d (%.1f%%)",
                healthCurrent, healthMax, healthPercent)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }

        // Qi Energy Information
        if (snapshot.qi != null) {
            long qiCurrent = snapshot.qi.getCurrentQi();
            long qiMax = snapshot.qi.getMaxQi();
            double qiPercent = qiMax > 0 ? (double)qiCurrent / qiMax * 100 : 0;

            source.sendSuccess(() -> Component.literal(String.format("Qi Energy: %d/%d (%.1f%%)",
                qiCurrent, qiMax, qiPercent)).withStyle(ChatFormatting.BLUE), false);
        }

        // Body Energy Information
        if (snapshot.body != null) {
            long bodyCurrent = snapshot.body.getCurrentBody();
            long bodyMax = snapshot.body.getMaxBody();
            double bodyPercent = bodyMax > 0 ? (double)bodyCurrent / bodyMax * 100 : 0;

            source.sendSuccess(() -> Component.literal(String.format("Body Energy: %d/%d (%.1f%%)",
                bodyCurrent, bodyMax, bodyPercent)).withStyle(ChatFormatting.DARK_RED), false);
        }
    }

    /**
     * Data snapshot containing all cultivation information for consistent display.
     */
    private record CultivationDataSnapshot(
        PlayerRealmAttachment realm,
        PlayerQiCultivationAttachment qiCultivation,
        PlayerBodyCultivationAttachment bodyCultivation,
        PlayerCultivationFocusAttachment focus,
        PlayerHealthAttachment health,
        PlayerQiAttachment qi,
        PlayerBodyAttachment body,
        PlayerSpiritualRootAttachment spiritualRoots,
        PlayerPhysiqueAttachment physique
    ) {}
}