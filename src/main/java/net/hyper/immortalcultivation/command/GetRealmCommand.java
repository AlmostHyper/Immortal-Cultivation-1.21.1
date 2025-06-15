package net.hyper.immortalcultivation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.hyper.immortalcultivation.attachment.PlayerQiAttachment;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GetRealmCommand {
    public GetRealmCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("get").then(Commands.literal("realm")
            .executes(this::execute)));
    }

    private int execute(CommandContext<CommandSourceStack> commandSourceStackCommandContext) {
        ServerPlayer player = commandSourceStackCommandContext.getSource().getPlayer();

        PlayerRealmAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_REALM);
        String currentRealm = String.valueOf(attachment.getCurrentRealm());

        commandSourceStackCommandContext.getSource().sendSuccess(() ->
            Component.literal("Your current Qi is: " + currentRealm), true);
        return 1;
    }
}
