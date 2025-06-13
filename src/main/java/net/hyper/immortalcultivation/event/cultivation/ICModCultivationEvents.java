package net.hyper.immortalcultivation.event.cultivation;

import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachments;
import net.hyper.immortalcultivation.component.PlayerQiComponent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = "immortalcultivation")
public class ICModCultivationEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        for (Player player : level.players()) {
            PlayerQiComponent qi = player.getData(ICModAttachments.PLAYER_QI.get());
            qi.tick();
        }
    }
}
