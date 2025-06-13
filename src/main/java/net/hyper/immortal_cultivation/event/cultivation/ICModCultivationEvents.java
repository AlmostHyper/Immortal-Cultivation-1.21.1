package net.hyper.immortal_cultivation.event.cultivation;

import net.hyper.immortal_cultivation.registry.ICModAttachments;
import net.hyper.immortal_cultivation.component.PlayerQiComponent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = "immortal_cultivation")
public class ICModCultivationEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        for (Player player : level.players()) {
            PlayerQiComponent qi = player.getData(ICModAttachments.PLAYER_QI.get());
            qi.addQi(1);
        }
    }
}
