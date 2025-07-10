package net.hyper.immortalcultivation.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

public class SetRandomPhysiqueCommand {

    public SetRandomPhysiqueCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("setrandomphysique")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .executes(context -> setRandomPhysique(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> setRandomPhysique(context, EntityArgument.getPlayer(context, "player")))
                )
        );
    }

    private static int setRandomPhysique(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        try {
            // Use the weighted random generation from the enum
            RandomSource random = player.level().getRandom();
            PhysiqueType randomPhysique = PhysiqueType.generateRandom(random);

            // Get the unified talent attachment
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talentAttachment == null) {
                context.getSource().sendFailure(Component.literal("Failed to get talent data for player"));
                return 0;
            }

            // Set the random physique type
            talentAttachment.assignPhysique(randomPhysique);
            talentAttachment.markDirty();
            // Send success message
            context.getSource().sendSuccess(() ->
                Component.literal("Set random physique for " + player.getName().getString() + " to " +
                    randomPhysique.name()), true);

            // Send message to the target player if different from command source
            if (context.getSource().getEntity() != player) {
                player.sendSystemMessage(Component.literal("Your physique has been randomly set to " +
                    randomPhysique.name()));
            }

            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error setting random physique: " + e.getMessage()));
            return 0;
        }
    }
}
