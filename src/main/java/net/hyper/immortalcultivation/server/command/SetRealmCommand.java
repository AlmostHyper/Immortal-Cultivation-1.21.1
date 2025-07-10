package net.hyper.immortalcultivation.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
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
        return SharedSuggestionProvider.suggest(ICModCultivationRealms.getRealmIds(), builder);
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
            ICultivationData.CultivationType cultivationType;
            switch (cultivationTypeString) {
                case "qi" -> cultivationType = ICultivationData.CultivationType.QI;
                case "body" -> cultivationType = ICultivationData.CultivationType.BODY;
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
                return 0;
            }

            // Get the unified cultivation attachment
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation == null) {
                context.getSource().sendFailure(Component.literal("Failed to get cultivation data for player"));
                return 0;
            }

            // Set the realm using the unified system
            cultivation.setRealm(cultivationType, realm);

            context.getSource().sendSuccess(() ->
                Component.literal("Set " + cultivationType.name() + " cultivation realm of " + player.getName().getString() +
                    " to " + realm.getId()), true);


            // Send message to the target player if different from command source
            if (context.getSource().getEntity() != player) {
                player.sendSystemMessage(Component.literal("Your " + cultivationType.name() + " cultivation realm has been set to " +
                    realm.getId()));
            }

            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error setting realm: " + e.getMessage()));
            return 0;
        }
    }
}
