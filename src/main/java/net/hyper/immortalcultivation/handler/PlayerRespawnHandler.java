package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class PlayerRespawnHandler {

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("Player {} respawned, resetting attachment states",
            player.getName().getString());

        // Reset health attachment to prevent 0-health issues
        resetHealthAttachment(player);

        // Reset resource attachments 
        resetResourceAttachments(player);

        // Re-sync all data to client
        resyncAllAttachments(player);

        ImmortalCultivation.LOGGER.info("Attachment reset complete for player {}",
            player.getName().getString());
    }

    private static void resetHealthAttachment(ServerPlayer player) {
        try {
            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                // Calculate max health based on current cultivation
                long maxHealth = calculateMaxHealthForPlayer(player);

                // Reset to full health
                healthAttachment.setCurrentHealth(maxHealth);

                // Reset regeneration fraction
                healthAttachment.setRegenFraction(0.0);

                // Sync vanilla health bar
                float healthPercentage = 1.0f; // Full health
                player.setHealth(player.getMaxHealth() * healthPercentage);

                ImmortalCultivation.LOGGER.info("Reset player {} health to {}/{}",
                    player.getName().getString(), maxHealth, maxHealth);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error resetting health for player {}",
                player.getName().getString(), e);
        }
    }

    private static void resetResourceAttachments(ServerPlayer player) {
        try {
            // Reset Qi attachment
            PlayerQiAttachment qiAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI.get());
            if (qiAttachment != null) {
                long maxQi = calculateMaxQiForPlayer(player);
                qiAttachment.setCurrentQi(maxQi);
                qiAttachment.setRegenFraction(0.0);

                ImmortalCultivation.LOGGER.info("Reset player {} Qi to {}/{}",
                    player.getName().getString(), maxQi, maxQi);
            }

            // Reset Body attachment
            PlayerBodyAttachment bodyAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY.get());
            if (bodyAttachment != null) {
                long maxBody = calculateMaxBodyForPlayer(player);
                bodyAttachment.setCurrentBody(maxBody);
                bodyAttachment.setRegenFraction(0.0);

                ImmortalCultivation.LOGGER.info("Reset player {} Body to {}/{}",
                    player.getName().getString(), maxBody, maxBody);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error resetting resources for player {}",
                player.getName().getString(), e);
        }
    }

    private static void resyncAllAttachments(ServerPlayer player) {
        try {
            // Force sync all attachments with proper player context

            // FIXED: Sync spiritual roots (this was missing!)
            PlayerSpiritualRootAttachment rootAttachment = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (rootAttachment != null) {
                rootAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Resynced spiritual roots after respawn for player {}",
                    player.getName().getString());
            }

            // FIXED: Sync physique (this was missing!)
            PlayerPhysiqueAttachment physiqueAttachment = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
            if (physiqueAttachment != null) {
                physiqueAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Resynced physique after respawn for player {}",
                    player.getName().getString());
            }

            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                healthAttachment.forceSync();
            }

            PlayerQiAttachment qiAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI.get());
            if (qiAttachment != null) {
                qiAttachment.forceSync();
            }

            PlayerBodyAttachment bodyAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY.get());
            if (bodyAttachment != null) {
                bodyAttachment.forceSync();
            }

            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment != null) {
                realmAttachment.forceSync();
            }

            PlayerQiCultivationAttachment qiCultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiCultivationAttachment != null) {
                qiCultivationAttachment.forceSync();
            }

            PlayerBodyCultivationAttachment bodyCultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyCultivationAttachment != null) {
                bodyCultivationAttachment.forceSync();
            }

            PlayerCultivationFocusAttachment focusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focusAttachment != null) {
                focusAttachment.forceSync();
            }

            ImmortalCultivation.LOGGER.info("Successfully resynced all attachments after respawn for player {}",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error syncing attachments for player {}",
                player.getName().getString(), e);
        }
    }

    // Helper methods to calculate max values safely
    private static long calculateMaxHealthForPlayer(ServerPlayer player) {
        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 20;

            long qiRealmHealth = realmAttachment.getCurrentQiRealm().getBaseHealth();
            long bodyRealmHealth = realmAttachment.getCurrentBodyRealm().getBaseHealth();

            if (qiRealmHealth == 20 && bodyRealmHealth == 20) {
                return 20;
            }

            long qiBonus = Math.max(0, qiRealmHealth - 20);
            long bodyBonus = Math.max(0, bodyRealmHealth - 20);
            return 20 + qiBonus + bodyBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating max health", e);
            return 20;
        }
    }

    private static long calculateMaxQiForPlayer(ServerPlayer player) {
        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 0;

            long baseQi = realmAttachment.getCurrentQiRealm().getBaseMaxQi();

            PlayerQiCultivationAttachment cultivationAttachment =
                player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (cultivationAttachment != null) {
                long cultivationBonus = (long)(cultivationAttachment.getCurrentQiCultivation() * 0.01);
                return baseQi + cultivationBonus;
            }

            return baseQi;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating max Qi", e);
            return 0;
        }
    }

    private static long calculateMaxBodyForPlayer(ServerPlayer player) {
        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 0;

            long baseBody = realmAttachment.getCurrentBodyRealm().getBaseMaxQi(); // Using same as Qi

            PlayerBodyCultivationAttachment cultivationAttachment =
                player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (cultivationAttachment != null) {
                long cultivationBonus = (long)(cultivationAttachment.getCurrentBodyCultivation() * 0.10);
                return baseBody + cultivationBonus;
            }

            return baseBody;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating max Body", e);
            return 0;
        }
    }
}