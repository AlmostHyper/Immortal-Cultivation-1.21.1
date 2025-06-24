package net.hyper.immortalcultivation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SetRealmCommand {

    private static final SuggestionProvider<CommandSourceStack> CULTIVATION_TYPE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(new String[]{"qi", "body"}, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> REALM_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(new String[]{
            "mortal",
            "qi_condensation_l1",
            "qi_condensation_l2",
            "qi_condensation_l3",
            "qi_condensation_l4",
            "qi_condensation_l5",
            "qi_condensation_l6",
            "qi_condensation_l7",
            "qi_condensation_l8",
            "qi_condensation_l9",
            "qi_condensation_l10",
            "foundation_establishment_early"
        }, builder);
    };

    public SetRealmCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("setrealm")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .then(Commands.argument("cultivation_type", StringArgumentType.word())
                    .suggests(CULTIVATION_TYPE_SUGGESTIONS)
                    .then(Commands.argument("realm_id", StringArgumentType.word())
                        .suggests(REALM_SUGGESTIONS)
                        .executes(context -> setRealm(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> setRealm(context, EntityArgument.getPlayer(context, "player")))
                        )
                    )
                )
        );
    }

    private static int setRealm(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        String cultivationTypeString = StringArgumentType.getString(context, "cultivation_type").toLowerCase();
        String realmId = StringArgumentType.getString(context, "realm_id").toLowerCase();

        try {
            // Parse cultivation type
            PlayerRealmAttachment.CultivationType cultivationType;
            switch (cultivationTypeString) {
                case "qi" -> cultivationType = PlayerRealmAttachment.CultivationType.QI;
                case "body" -> cultivationType = PlayerRealmAttachment.CultivationType.BODY;
                default -> {
                    context.getSource().sendFailure(Component.literal("Invalid cultivation type: " + cultivationTypeString));
                    context.getSource().sendFailure(Component.literal("Available types: qi, body"));
                    return 0;
                }
            }

            // Get the cultivation realm
            CultivationRealm realm = ICModCultivationRealms.get(realmId);
            if (realm == null) {
                context.getSource().sendFailure(Component.literal("Invalid realm ID: " + realmId));
                context.getSource().sendFailure(Component.literal("Available realms: mortal, qi_condensation_l1-l10, foundation_establishment_early"));
                return 0;
            }

            // Get the player's realm attachment
            var realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) {
                context.getSource().sendFailure(Component.literal("Failed to get realm data for player"));
                return 0;
            }

            // Set the realm
            realmAttachment.setCurrentRealm(cultivationType, realm);

            // Send success message
            context.getSource().sendSuccess(() ->
                Component.literal("Set " + cultivationType.getDisplayName() + " cultivation realm of " +
                    player.getName().getString() + " to " + realm.getDisplayName()), true);

            // Send message to the target player if different from command source
            if (!context.getSource().getEntity().equals(player)) {
                player.sendSystemMessage(Component.literal("Your " + cultivationType.getDisplayName() +
                    " cultivation realm has been set to " + realm.getDisplayName()));
            }

            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error setting realm: " + e.getMessage()));
            e.printStackTrace(); // For debugging
            return 0;
        }
    }
}