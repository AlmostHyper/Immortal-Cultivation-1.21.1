package net.hyper.immortalcultivation.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
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

            // Set the spiritual root
            talentAttachment.setSpiritualRootType(rootType);
            context.getSource().sendSuccess(() ->
                Component.literal("Set spiritual root of " + player.getName().getString() +
                    " to " + rootType.name() + " (" +
                    String.format("%.1fx", talentAttachment.getSpiritualRootMultiplier()) + " multiplier)"), true);

            // Send message to the target player if different from command source
            if (context.getSource().getEntity() != player) {
                player.sendSystemMessage(Component.literal("Your spiritual root has been set to " +
                    rootType.name() + " (" +
                    String.format("%.1fx", talentAttachment.getSpiritualRootMultiplier()) + " cultivation multiplier)"));
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
