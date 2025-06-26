package net.hyper.immortalcultivation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.hyper.immortalcultivation.attachment.PlayerTalentAttachment;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public class SetSpiritualRootCommand {

    private static final SuggestionProvider<CommandSourceStack> SPIRITUAL_ROOT_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            Arrays.stream(SpiritualRootType.values())
                .map(type -> type.name().toLowerCase())
                .toArray(String[]::new),
            builder
        );
    };

    public SetSpiritualRootCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("setspiritualroot")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .then(Commands.argument("root_type", StringArgumentType.word())
                    .suggests(SPIRITUAL_ROOT_SUGGESTIONS)
                    .executes(context -> setSpiritualRoot(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> setSpiritualRoot(context, EntityArgument.getPlayer(context, "player")))
                    )
                )
        );
    }

    private static int setSpiritualRoot(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        String rootString = StringArgumentType.getString(context, "root_type").toUpperCase();

        try {
            SpiritualRootType rootType = SpiritualRootType.valueOf(rootString);

            // Get the unified talent attachment
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talentAttachment == null) {
                context.getSource().sendFailure(Component.literal("Failed to get talent data for player"));
                return 0;
            }

            // Set the spiritual root using the unified system
            if (talentAttachment.hasSpiritualRootAssigned()) {
                // Force reassignment (admin override)
                talentAttachment.forceReassignSpiritualRoot(rootType);
                context.getSource().sendSuccess(() ->
                    Component.literal("Forcibly reassigned spiritual root of " + player.getName().getString() +
                        " to " + rootType.getDisplayName() + " (" +
                        String.format("%.1fx", rootType.getCultivationMultiplier()) + " multiplier)"), true);
            } else {
                // Normal assignment
                talentAttachment.assignSpiritualRoot(rootType);
                context.getSource().sendSuccess(() ->
                    Component.literal("Set spiritual root of " + player.getName().getString() +
                        " to " + rootType.getDisplayName() + " (" +
                        String.format("%.1fx", rootType.getCultivationMultiplier()) + " multiplier)"), true);
            }

            // Send message to the target player if different from command source
            if (!context.getSource().getEntity().equals(player)) {
                player.sendSystemMessage(Component.literal("Your spiritual root has been set to " +
                    rootType.getDisplayName() + " (" +
                    String.format("%.1fx", rootType.getCultivationMultiplier()) + " cultivation multiplier)"));
            }

            // Show combined talent status if both talents are assigned
            if (talentAttachment.hasSpiritualRootAssigned() && talentAttachment.hasPhysiqueAssigned()) {
                String combinedDesc = talentAttachment.getCombinedTalentDescription();
                float overallMultiplier = talentAttachment.getOverallCultivationMultiplier();

                context.getSource().sendSuccess(() ->
                    Component.literal("Combined Talent Status: " + combinedDesc +
                        " (Overall: " + String.format("%.2fx", overallMultiplier) + " multiplier)"), false);

                // Check for Divine Dao Destiny
                if (talentAttachment.hasDivineDaoDestiny()) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("🌟 DIVINE DAO DESTINY ACHIEVED! 🌟")
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.BOLD), false);
                }
            }

            return 1;

        } catch (IllegalArgumentException e) {
            // Invalid spiritual root type
            context.getSource().sendFailure(Component.literal("Invalid spiritual root type: " + rootString));
            context.getSource().sendFailure(Component.literal("Available types: " +
                String.join(", ", Arrays.stream(SpiritualRootType.values())
                    .map(type -> type.name().toLowerCase())
                    .toArray(String[]::new))));
            return 0;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error setting spiritual root: " + e.getMessage()));
            return 0;
        }
    }
}