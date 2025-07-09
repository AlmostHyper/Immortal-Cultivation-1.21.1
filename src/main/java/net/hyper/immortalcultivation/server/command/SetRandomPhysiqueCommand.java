package net.hyper.immortalcultivation.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
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

            // Set the random physique type using the unified system
            if (talentAttachment.hasPhysiqueAssigned()) {
                // Force reassignment (admin override)
                talentAttachment.forceReassignPhysique(randomPhysique);
            } else {
                // Normal assignment
                talentAttachment.assignPhysique(randomPhysique);
            }

            // Check if we need to broadcast to server
            float multiplier = randomPhysique.getCultivationMultiplier();
            if (multiplier == 1.4f) {
                // Legendary - broadcast to server
                broadcastLegendaryAchievement(player, "physique", randomPhysique.getDisplayName(), "1.4x");
            } else if (multiplier == 1.5f) {
                // Heavenly - broadcast to server
                broadcastHeavenlyAchievement(player, "physique", randomPhysique.getDisplayName(), "1.5x");
            }

            // Check for ultimate destiny (both heavenly talents)
            checkForUltimateDestiny(player, talentAttachment);

            // Send success message with rarity information
            String rarityInfo = getRarityInfo(randomPhysique);
            context.getSource().sendSuccess(() ->
                Component.literal("🎲 The heavens have determined " + player.getName().getString() +
                    "'s fate: " + randomPhysique.getDisplayName() + " " + rarityInfo +
                    " (Multiplier: " + String.format("%.1fx", randomPhysique.getCultivationMultiplier()) + ")"), true);

            // Send message to the target player if different from command source
            if (!context.getSource().getEntity().equals(player)) {
                String rarityMessage = getRarityMessage(randomPhysique);
                player.sendSystemMessage(Component.literal("🎲 " + rarityMessage + " " +
                    randomPhysique.getDisplayName() + "! " +
                    "(Cultivation multiplier: " + String.format("%.1fx", randomPhysique.getCultivationMultiplier()) + ")"));
            } else {
                // Message to self
                String rarityMessage = getRarityMessage(randomPhysique);
                player.sendSystemMessage(Component.literal("🎲 " + rarityMessage + " " +
                    randomPhysique.getDisplayName() + "!"));
            }

            // Show combined talent status if both talents are assigned
            if (talentAttachment.hasSpiritualRootAssigned() && talentAttachment.hasPhysiqueAssigned()) {
                String combinedDesc = talentAttachment.getCombinedTalentDescription();
                float overallMultiplier = talentAttachment.getOverallCultivationMultiplier();

                context.getSource().sendSuccess(() ->
                    Component.literal("Combined Talent Status: " + combinedDesc +
                        " (Overall: " + String.format("%.2fx", overallMultiplier) + " multiplier)"), false);
            }

            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error setting random physique: " + e.getMessage()));
            e.printStackTrace(); // For debugging
            return 0;
        }
    }

    /**
     * Get rarity information for command feedback
     */
    private static String getRarityInfo(PhysiqueType physique) {
        float multiplier = physique.getCultivationMultiplier();
        if (multiplier == 1.0f) return "[Common]";
        else if (multiplier == 1.1f) return "[Common]";
        else if (multiplier == 1.2f) return "[Uncommon]";
        else if (multiplier == 1.3f) return "[Rare]";
        else if (multiplier == 1.4f) return "[Legendary]";
        else if (multiplier == 1.5f) return "[Mythical]";
        else return "[Unknown]";
    }

    /**
     * Get rarity-appropriate message for players
     */
    private static String getRarityMessage(PhysiqueType physique) {
        float multiplier = physique.getCultivationMultiplier();
        if (multiplier == 1.0f) return "You possess a mortal physique:";
        else if (multiplier == 1.1f) return "The heavens have blessed you with the";
        else if (multiplier == 1.2f) return "Fortune smiles upon you! You have awakened the";
        else if (multiplier == 1.3f) return "Incredible luck! The dao has granted you the";
        else if (multiplier == 1.4f) return "By the will of heaven! You have been chosen for the";
        else if (multiplier == 1.5f) return "HEAVEN-DEFYING DESTINY! You have transcended fate with the";
        else return "You have awakened the";
    }

    /**
     * Broadcast legendary achievement to all players on the server
     */
    private static void broadcastLegendaryAchievement(ServerPlayer player, String type, String talentName, String multiplier) {
        try {
            Component playerName = Component.literal(player.getName().getString())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            Component talent = Component.literal(talentName)
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);

            Component broadcast = Component.literal("🌟 ═══ LEGENDARY AWAKENING ═══ 🌟\n")
                .append(Component.literal("The dao trembles as "))
                .append(playerName)
                .append(Component.literal(" awakens the legendary "))
                .append(talent)
                .append(Component.literal("!\n"))
                .append(Component.literal("A prodigy emerges! ("))
                .append(Component.literal(multiplier + " cultivation multiplier"))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal(")"))
                .withStyle(ChatFormatting.GOLD);

            // Send to all players except the one who achieved it
            player.getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
                if (!serverPlayer.equals(player)) {
                    serverPlayer.sendSystemMessage(broadcast);
                }
            });
        } catch (Exception e) {
            // Log error instead of sending to context
            ImmortalCultivation.LOGGER.error("Error broadcasting legendary physique achievement", e);
        }
    }

    /**
     * Broadcast heavenly achievement to all players on the server
     */
    private static void broadcastHeavenlyAchievement(ServerPlayer player, String type, String talentName, String multiplier) {
        try {
            Component playerName = Component.literal(player.getName().getString())
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
            Component talent = Component.literal(talentName)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            Component broadcast = Component.literal("💫 ════ HEAVEN-DEFYING MIRACLE ════ 💫\n")
                .append(Component.literal("THE HEAVENS SHAKE! "))
                .append(playerName)
                .append(Component.literal(" has DEFIED FATE!\n"))
                .append(Component.literal("The legendary "))
                .append(talent)
                .append(Component.literal(" has chosen them!\n"))
                .append(Component.literal("DESTINED FOR IMMORTALITY! ("))
                .append(Component.literal(multiplier + " cultivation multiplier"))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal(")"))
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);

            // Send to all players except the one who achieved it
            player.getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
                if (!serverPlayer.equals(player)) {
                    serverPlayer.sendSystemMessage(broadcast);
                }
            });
        } catch (Exception e) {
            // Log error instead of sending to context
            ImmortalCultivation.LOGGER.error("Error broadcasting heavenly physique achievement", e);
        }
    }

    /**
     * Check if player has achieved ultimate destiny (both heavenly talents) using unified attachment
     */
    private static void checkForUltimateDestiny(ServerPlayer player, PlayerTalentAttachment talentAttachment) {
        try {
            if (talentAttachment.hasDivineDaoDestiny()) {
                // ULTIMATE DESTINY ACHIEVED!
                broadcastUltimateDestiny(player,
                    talentAttachment.getSpiritualRootDisplayName(),
                    talentAttachment.getPhysiqueDisplayName());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error checking ultimate destiny", e);
        }
    }

    /**
     * Broadcast ultimate destiny achievement - the rarest possible: both heavenly talents
     */
    private static void broadcastUltimateDestiny(ServerPlayer player, String rootName, String physiqueName) {
        try {
            Component playerName = Component.literal(player.getName().getString())
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            Component root = Component.literal(rootName)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            Component physique = Component.literal(physiqueName)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            Component broadcast = Component.literal("⚡ ════════ ULTIMATE DESTINY ════════ ⚡\n")
                .append(Component.literal("🌟💫 THE WORLD TREMBLES! THE DAO ITSELF BOWS! 💫🌟\n"))
                .append(playerName)
                .append(Component.literal(" has achieved ULTIMATE PERFECTION!\n"))
                .append(Component.literal("Blessed with BOTH the "))
                .append(root)
                .append(Component.literal(" AND "))
                .append(physique)
                .append(Component.literal("!\n"))
                .append(Component.literal("CHOSEN BY DESTINY! MASTER OF HEAVEN AND EARTH!\n"))
                .append(Component.literal("🌟 Combined 2.25x Cultivation Multiplier! 🌟\n"))
                .append(Component.literal("⚡ Once in ten thousand generations! ⚡"))
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);

            // Send to ALL players including the achiever
            player.getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
                serverPlayer.sendSystemMessage(broadcast);
            });
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error broadcasting ultimate destiny", e);
        }
    }
}