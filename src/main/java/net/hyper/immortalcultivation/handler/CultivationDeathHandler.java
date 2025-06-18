package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerQiCultivationAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public class CultivationDeathHandler {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // Only apply to players
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Get cultivation attachment
        PlayerQiCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivationAttachment == null) {
            return;
        }

        // Calculate 5% loss
        long currentCultivation = cultivationAttachment.getCurrentCultivation();
        long cultivationLoss = (long) (currentCultivation * 0.05);
        cultivationAttachment.tryConsumeCultivation(cultivationLoss);

        // Apply the loss
        long newCultivation = currentCultivation - cultivationLoss;
        cultivationAttachment.setCurrentCultivation(Math.max(0, newCultivation)); // Don't go below 0

        // Optional: Send message to player
        if (cultivationLoss > 0) {
            player.sendSystemMessage(
                Component.literal("Your cultivation has been damaged by death! Lost: " + cultivationLoss + " cultivation.")
                    .withStyle(ChatFormatting.RED)
            );
        }

        // Optional: Log for debugging
        ImmortalCultivation.LOGGER.info("Player {} lost {} cultivation on death ({} -> {})",
            player.getName().getString(), cultivationLoss, currentCultivation, newCultivation);
    }
}
