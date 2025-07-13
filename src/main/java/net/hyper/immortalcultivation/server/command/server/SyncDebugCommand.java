package net.hyper.immortalcultivation.server.command.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.ICModNetworkManager;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class SyncDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("immortalcultivation")
            .then(Commands.literal("debug")
                .then(Commands.literal("sync")
                    .executes(context -> checkSyncStatus(context.getSource()))
                    .then(Commands.literal("enable")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(context -> toggleDebugLogging(
                                context.getSource(),
                                BoolArgumentType.getBool(context, "enabled")
                            ))
                        )
                    )
                    .then(Commands.literal("force")
                        .executes(context -> forceSyncAll(context.getSource()))
                    )
                    .then(Commands.literal("mark_dirty")
                        .executes(context -> markAllDirty(context.getSource()))
                    )
                )
            )
        );
    }

    private static int checkSyncStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("=== Sync Status Report ===").withStyle(ChatFormatting.GOLD), false);

        // Check each attachment
        Map<String, ISyncable> attachments = new HashMap<>();
        attachments.put("Cultivation", player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get()));
        attachments.put("Talents", player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get()));
        attachments.put("Race", player.getData(ICModAttachmentTypes.PLAYER_RACE.get()));
        attachments.put("Stats", player.getData(ICModAttachmentTypes.PLAYER_STATS.get()));
        attachments.put("Breakthrough", player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get()));
        attachments.put("Pill Consumption", player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get()));

        int dirtyCount = 0;
        for (Map.Entry<String, ISyncable> entry : attachments.entrySet()) {
            ISyncable attachment = entry.getValue();
            if (attachment != null) {
                boolean isDirty = attachment.isDirty();
                if (isDirty) dirtyCount++;

                ChatFormatting color = isDirty ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
                String status = isDirty ? "DIRTY" : "CLEAN";

                source.sendSuccess(() -> Component.literal("• " + entry.getKey() + ": ")
                    .append(Component.literal(status).withStyle(color)), false);
            } else {
                source.sendSuccess(() -> Component.literal("• " + entry.getKey() + ": ")
                    .append(Component.literal("NULL").withStyle(ChatFormatting.RED)), false);
            }
        }

        int finalDirtyCount = dirtyCount;
        source.sendSuccess(() -> Component.literal("\nTotal dirty attachments: " + finalDirtyCount)
            .withStyle(finalDirtyCount > 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false);

        int pendingSyncs = ICModNetworkManager.INSTANCE.getPendingSyncCount();
        source.sendSuccess(() -> Component.literal("Pending syncs in NetworkSyncManager: " + pendingSyncs)
            .withStyle(pendingSyncs > 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false);

        return 1;
    }

    private static int toggleDebugLogging(CommandSourceStack source, boolean enabled) {
        ICModPacketRegistry.setDebugPackets(enabled);
        ICModNetworkManager.setDebugSync(enabled);

        source.sendSuccess(() -> Component.literal("Sync debug logging " + (enabled ? "enabled" : "disabled"))
            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);

        if (enabled) {
            source.sendSuccess(() -> Component.literal("Check logs for detailed sync information")
                .withStyle(ChatFormatting.GRAY), false);
        }

        return 1;
    }

    private static int forceSyncAll(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Forcing sync of all attachments...").withStyle(ChatFormatting.YELLOW), false);

        // Schedule full sync for all attachments
        ICModNetworkManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_CULTIVATION::get, true);
        ICModNetworkManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_TALENTS::get, true);
        ICModNetworkManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_RACE::get, true);
        ICModNetworkManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_STATS::get, true);
        ICModNetworkManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_BREAKTHROUGH::get, true);
        ICModNetworkManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION::get, true);

        source.sendSuccess(() -> Component.literal("All attachments scheduled for sync!")
            .withStyle(ChatFormatting.GREEN), false);

        return 1;
    }

    private static int markAllDirty(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Marking all attachments as dirty...").withStyle(ChatFormatting.YELLOW), false);

        // Mark all attachments as dirty
        player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get()).markDirty();
        player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get()).markDirty();
        player.getData(ICModAttachmentTypes.PLAYER_RACE.get()).markDirty();
        player.getData(ICModAttachmentTypes.PLAYER_STATS.get()).markDirty();
        player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get()).markDirty();
        player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get()).markDirty();

        source.sendSuccess(() -> Component.literal("All attachments marked as dirty!")
            .withStyle(ChatFormatting.GREEN), false);

        return 1;
    }
}