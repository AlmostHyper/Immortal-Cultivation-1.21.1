package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerSpiritualRootsAttachment;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerSpiritualRootsAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT);

            // Check if this is first time spawning
            if (!roots.hasBeenAssigned()) {
                // Generate spiritual roots
                SpiritualRootType generatedType = SpiritualRootType.generateRandom(player.getRandom());
                roots.assignRootType(generatedType);

                // Notify player
                Component message = Component.literal("You have awakened ")
                    .append(Component.literal(generatedType.getDisplayName())
                        .withStyle(Style.EMPTY.withColor(generatedType.getColor())))
                    .append(" spiritual roots!");

                player.sendSystemMessage(message);

                // Optional: Play sound effect
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
    }
}
