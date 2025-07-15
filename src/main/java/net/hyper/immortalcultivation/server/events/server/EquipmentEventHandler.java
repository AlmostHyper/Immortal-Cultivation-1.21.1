package net.hyper.immortalcultivation.server.events.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

public class EquipmentEventHandler {

    /**
     * Handles equipment changes for attribute recalculation
     *
     * @param event Equipment change event
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Recalculate attributes when equipment changes
        recalculatePlayerAttributes(player);

        ImmortalCultivation.LOGGER.debug("Recalculated attributes for {} due to equipment change",
            player.getName().getString());
    }
}
