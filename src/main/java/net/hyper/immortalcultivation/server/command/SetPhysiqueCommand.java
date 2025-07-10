package net.hyper.immortalcultivation.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public class SetPhysiqueCommand {

    private static final SuggestionProvider<CommandSourceStack> PHYSIQUE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            Arrays.stream(PhysiqueType.values())
                .map(type -> type.name().toLowerCase())
                .toArray(String[]::new),
            builder
        );
    };

    public SetPhysiqueCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("setphysique")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .then(Commands.argument("physique", StringArgumentType.word())
                    .suggests(PHYSIQUE_SUGGESTIONS)
                    .executes(context -> setPhysique(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> setPhysique(context, EntityArgument.getPlayer(context, "player")))
                    )
                )
        );
    }

    private static int setPhysique(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        String physiqueString = StringArgumentType.getString(context, "physique").toUpperCase();

        try {
            PhysiqueType physiqueType = PhysiqueType.valueOf(physiqueString);

            // Get the unified talent attachment
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talentAttachment == null) {
                context.getSource().sendFailure(Component.literal("Failed to get talent data for player"));
                return 0;
            }

            // Set the physique
            talentAttachment.assignPhysique(physiqueType);
            talentAttachment.markDirty();
            context.getSource().sendSuccess(() ->
                Component.literal("Set physique of " + player.getName().getString() +
                    " to " + physiqueType.name() + " (" +
                    String.format("%.1fx", talentAttachment.getPhysiqueMultiplier()) + " multiplier)"), true);

            // Send message to the target player if different from command source
            if (context.getSource().getEntity() != player) {
                player.sendSystemMessage(Component.literal("Your physique has been set to " +
                    physiqueType.name() + " (" +
                    String.format("%.1fx", talentAttachment.getPhysiqueMultiplier()) + " cultivation multiplier)"));
            }

            return 1;

        } catch (IllegalArgumentException e) {
            // Invalid physique type
            context.getSource().sendFailure(Component.literal("Invalid physique type: " + physiqueString));
            context.getSource().sendFailure(Component.literal("Available types: " +
                String.join(", ", Arrays.stream(PhysiqueType.values())
                    .map(type -> type.name().toLowerCase())
                    .toArray(String[]::new))));
            return 0;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error setting physique: " + e.getMessage()));
            return 0;
        }
    }
}
