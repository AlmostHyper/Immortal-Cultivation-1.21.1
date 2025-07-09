package net.hyper.immortalcultivation.core.network.helpers;

import net.hyper.immortalcultivation.common.attachments.PlayerBreakthroughAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Helper class for providing immediate breakthrough feedback to players.
 * This handles the immediate response aspects that don't require sync packets,
 * such as chat messages, sounds, and server announcements.
 */
public class BreakthroughFeedbackHelper {

    /**
     * Sends comprehensive breakthrough feedback including chat messages, sounds, and announcements.
     * This method provides immediate feedback while the sync system handles data updates separately.
     *
     * @param player The player who attempted the breakthrough
     * @param result The result of the breakthrough attempt
     * @param breakthroughType The type of breakthrough that was attempted
     */
    public static void sendBreakthroughFeedback(ServerPlayer player,
                                                PlayerBreakthroughAttachment.BreakthroughResult result,
                                                PlayerBreakthroughAttachment.BreakthroughType breakthroughType) {

        // Send chat message feedback
        ChatFormatting color = result.success() ? ChatFormatting.GREEN : ChatFormatting.RED;
        Component message = Component.literal(result.message()).withStyle(color);
        Component typeInfo = Component.literal(" [" + breakthroughType.getDisplayName() + "]")
            .withStyle(ChatFormatting.GRAY);

        player.sendSystemMessage(message.copy().append(typeInfo));

        // Play appropriate sound effects
        Level level = player.level();
        if (result.success()) {
            playSuccessEffects(level, player, breakthroughType);

            // Handle server announcements for major breakthroughs
            if (result.newRealm() != null && result.newRealm().isMajorRealm()) {
                announceBreakthrough(player, result.newRealm(), breakthroughType);
            }
        } else {
            playFailureEffects(level, player, breakthroughType);
        }
    }

    /**
     * Plays success sound effects scaled by breakthrough type importance.
     */
    private static void playSuccessEffects(Level level, ServerPlayer player,
                                           PlayerBreakthroughAttachment.BreakthroughType type) {
        switch (type) {
            case MINOR -> {
                level.playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            case MAJOR -> {
                level.playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.3f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.0f);
            }
            case SPECIAL -> {
                level.playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            case BOUNDARY -> {
                level.playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2f, 2.0f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0f, 2.0f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.3f, 1.0f);
            }
        }
    }

    /**
     * Plays failure sound effects scaled by breakthrough type.
     */
    private static void playFailureEffects(Level level, ServerPlayer player,
                                           PlayerBreakthroughAttachment.BreakthroughType type) {
        switch (type) {
            case MINOR -> level.playSound(null, player.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 0.8f);
            case MAJOR -> {
                level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
            }
            case SPECIAL -> {
                level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 0.5f);
            }
            case BOUNDARY -> {
                level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
                level.playSound(null, player.blockPosition(),
                    SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 0.3f, 0.5f);
            }
        }
    }

    /**
     * Announces major breakthroughs to all players on the server.
     */
    private static void announceBreakthrough(ServerPlayer player,
                                             net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm newRealm,
                                             PlayerBreakthroughAttachment.BreakthroughType breakthroughType) {
        Component announcement = Component.literal("🌟 ")
            .append(player.getDisplayName())
            .append(" has achieved a ")
            .append(breakthroughType.getDisplayName())
            .append(" breakthrough to ")
            .append(newRealm.getDisplayName())
            .append("!")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        player.getServer().getPlayerList().broadcastSystemMessage(announcement, false);
    }
}