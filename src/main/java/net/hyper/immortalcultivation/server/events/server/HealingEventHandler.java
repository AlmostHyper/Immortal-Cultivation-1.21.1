package net.hyper.immortalcultivation.server.events.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

public class HealingEventHandler {

    /**
     * Fixed healing event handler that maintains vanilla compatibility.
     *
     * @param event Healing event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHealing(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        PlayerStatsAttachment statsAttachment = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
        if (statsAttachment == null) {
            return;
        }

        float vanillaHealing = event.getAmount();

        // Cancel vanilla healing
        event.setAmount(0.0f);

        // Convert to custom healing with 1:1 scaling (no more 100x multiplier!)
        long customHealing = convertToCustomHealing(vanillaHealing, player);

        if (customHealing > 0) {
            long actualHealing = statsAttachment.addStat(IStatsData.StatType.HEALTH, customHealing);

            if (actualHealing > 0) {
                // Update vanilla health display
                updateVanillaHealthDisplay(player, statsAttachment);

                // Mark for sync
                statsAttachment.markDirty();

                ImmortalCultivation.LOGGER.debug("Applied {} custom healing to {} (vanilla: {})",
                    actualHealing, player.getName().getString(), vanillaHealing);
            }
        }
    }
}
