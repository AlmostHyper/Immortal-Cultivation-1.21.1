package net.hyper.immortalcultivation.server.events;

import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.core.attachments.ICultivationData;
import net.hyper.immortalcultivation.core.attachments.PlayerCultivationAttachment;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class OnTickEvent extends Event {
    public static final int CULTIVATION_RATE = 20;
    public static final int REGENERATION = 10;

    // Player Logic
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (player.tickCount % CULTIVATION_RATE == 0) {
            player.getExistingData(AttachmentRegistry.PLAYER_CULTIVATION.get())
                .ifPresent(cultivation -> processCultivationTick(player, cultivation));
        }

        if (player.tickCount % REGENERATION == 0) {

        }

        if (player.tickCount % 100 == 0) {

        }
    }

    private static void processCultivationTick(Player player, PlayerCultivationAttachment cultivation) {
        for (ICultivationData.CultivationType type : ICultivationData.CultivationType.values()) {
            double baseRate = cultivation.getAccumulationRate(type);

            CultivationTickEvent tickEvent = new CultivationTickEvent(player, type, baseRate);
            NeoForge.EVENT_BUS.post(tickEvent);

            double finalRate = tickEvent.calculateFinalRate();

            if (finalRate > 0) {
                applyCultivationProgress(cultivation, type, finalRate);
            }
        }
    }

    private static void applyCultivationProgress(PlayerCultivationAttachment cultivation,
                                                 ICultivationData.CultivationType type,
                                                 double rate) {

        cultivation.addCultivation (type, (long) rate, true);
        cultivation.markDirty();
    }

    // Server Logic
    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {

    }
}
