package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerQiCultivationAttachment;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.attachment.PlayerSpiritualRootAttachment;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class PlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Player {} logged in, initializing attachments", player.getName().getString());

            // === SPIRITUAL ROOTS INITIALIZATION (existing code) ===
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());

            // Check if this is first time spawning
            if (roots != null && !roots.hasBeenAssigned()) {
                // Generate spiritual roots
                SpiritualRootType generatedType = SpiritualRootType.generateRandom(player.getRandom());
                roots.assignRootType(generatedType);

                // Notify player
                Component message = Component.literal("You have been born with ")
                    .append(Component.literal(generatedType.getDisplayName())
                        .withStyle(Style.EMPTY.withColor(generatedType.getColor())))
                    .append(" spiritual roots!");

                player.sendSystemMessage(message);

                // Optional: Play sound effect
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            // === REALM ATTACHMENT INITIALIZATION (new code) ===
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment != null) {
                // Ensure player reference is set
                realmAttachment.setPlayer(player);

                // Ensure realm is set to default if null
                if (realmAttachment.getCurrentRealm() == null) {
                    ImmortalCultivation.LOGGER.warn("Player {} has null realm, setting to default", player.getName().getString());
                    realmAttachment.setCurrentRealm(ICModCultivationRealms.MORTAL, false);
                }

                ImmortalCultivation.LOGGER.info("Player {} realm initialized: {}",
                    player.getName().getString(),
                    realmAttachment.getCurrentRealm().getDisplayName());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null realm attachment!", player.getName().getString());
            }

            // === CULTIVATION ATTACHMENT INITIALIZATION (new code) ===
            PlayerQiCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationAttachment != null) {
                ImmortalCultivation.LOGGER.info("Player {} cultivation initialized: {}/{}",
                    player.getName().getString(),
                    cultivationAttachment.getCurrentCultivation(),
                    cultivationAttachment.getMaxCultivation());

                // Force sync to client
                cultivationAttachment.forceSync();
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null cultivation attachment!", player.getName().getString());
            }
        }
    }
}
