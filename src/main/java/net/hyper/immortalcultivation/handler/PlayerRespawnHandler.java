package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
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

        ImmortalCultivation.LOGGER.info("Player {} respawned, resetting unified attachment states",
            player.getName().getString());

        // Reset unified attachments in proper order
        resetRaceAttachment(player);
        resetCultivationAttachment(player);
        resetResourceAttachment(player);
        resetTalentAttachment(player);

        // Re-sync all data to client
        resyncAllAttachments(player);

        ImmortalCultivation.LOGGER.info("Unified attachment reset complete for player {}",
            player.getName().getString());
    }

    /**
     * Reset race attachment - handles player race
     */
    private static void resetRaceAttachment(ServerPlayer player) {
        try {
            PlayerRaceAttachment raceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            if (raceAttachment != null) {
                // IMPORTANT: Reset player reference after respawn
                raceAttachment.setPlayer(player);

                // NOTE: Don't reset race itself - it is a permanent character trait!
                // Race should persist through death and respawn

                ImmortalCultivation.LOGGER.info("Reset player {} race references - Race: {} (Qi: {}x, Body: {}x)",
                    player.getName().getString(),
                    raceAttachment.getRaceDisplayName(),
                    raceAttachment.getQiMultiplier(),
                    raceAttachment.getBodyMultiplier());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null race attachment after respawn!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error resetting race for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Reset cultivation attachment - handles realms, cultivation progress, focus, bonuses
     */
    private static void resetCultivationAttachment(ServerPlayer player) {
        try {
            PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationAttachment != null) {
                // IMPORTANT: Reset player reference after respawn
                cultivationAttachment.setPlayer(player);

                // Calculate max values based on current realms
                long maxQiCultivation = cultivationAttachment.getCurrentQiRealm().getBaseMaxCultivation();
                long maxBodyCultivation = cultivationAttachment.getCurrentBodyRealm().getBaseMaxCultivation();

                // Reset cultivation progress to safe values (don't lose all progress on death)
                // Keep some progress to avoid complete loss
                long preservedQiCultivation = Math.max(0, cultivationAttachment.getCurrentQiCultivation() - (maxQiCultivation / 10));
                long preservedBodyCultivation = Math.max(0, cultivationAttachment.getCurrentBodyCultivation() - (maxBodyCultivation / 10));

                cultivationAttachment.setCurrentQiCultivation(preservedQiCultivation);
                cultivationAttachment.setCurrentBodyCultivation(preservedBodyCultivation);

                // Reset cultivation fractions
                cultivationAttachment.setRegenFraction(0.0);

                // NOTE: Don't reset pill bonuses - they should persist through death!
                // The bonuses from consumed pills are permanent character progression

                ImmortalCultivation.LOGGER.info("Reset player {} cultivation - Qi: {}/{}, Body: {}/{}, Pill bonuses preserved",
                    player.getName().getString(),
                    preservedQiCultivation, maxQiCultivation,
                    preservedBodyCultivation, maxBodyCultivation);
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null cultivation attachment after respawn!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error resetting cultivation for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Reset resource attachment - handles health, qi, body energy
     */
    private static void resetResourceAttachment(ServerPlayer player) {
        try {
            PlayerResourceAttachment resourceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resourceAttachment != null) {
                // IMPORTANT: Reset player reference after respawn
                resourceAttachment.setPlayer(player);

                // Calculate max values based on current cultivation
                long maxHealth = calculateMaxHealthForPlayer(player);
                long maxQi = calculateMaxQiForPlayer(player);
                long maxBody = calculateMaxBodyForPlayer(player);

                // Reset to full resources on respawn
                resourceAttachment.setCurrentHealth(maxHealth);
                resourceAttachment.setMaxHealth(maxHealth);
                resourceAttachment.setCurrentQi(maxQi);
                resourceAttachment.setMaxQi(maxQi);
                resourceAttachment.setCurrentBody(maxBody);
                resourceAttachment.setMaxBody(maxBody);

                // Reset regeneration fractions
                resourceAttachment.setRegenFraction(0.0, 0.0, 0.0);

                // Reset meditation state
                resourceAttachment.setMeditating(false);

                // Sync vanilla health bar
                float healthPercentage = 1.0f; // Full health
                player.setHealth(player.getMaxHealth() * healthPercentage);

                ImmortalCultivation.LOGGER.info("Reset player {} resources - Health: {}/{}, Qi: {}/{}, Body: {}/{}",
                    player.getName().getString(),
                    maxHealth, maxHealth, maxQi, maxQi, maxBody, maxBody);
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null resource attachment after respawn!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error resetting resources for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Reset talent attachment - handles spiritual roots and physique
     */
    private static void resetTalentAttachment(ServerPlayer player) {
        try {
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talentAttachment != null) {
                // IMPORTANT: Reset player reference after respawn
                talentAttachment.setPlayer(player);

                // NOTE: Don't reset talents themselves - they are permanent character traits!
                // Talents (spiritual roots and physique) should persist through death

                ImmortalCultivation.LOGGER.info("Reset player {} talent references - Spiritual Root: {} ({}x), Physique: {} ({}x)",
                    player.getName().getString(),
                    talentAttachment.getSpiritualRootDisplayName(),
                    talentAttachment.getSpiritualRootMultiplier(),
                    talentAttachment.getPhysiqueDisplayName(),
                    talentAttachment.getPhysiqueMultiplier());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null talent attachment after respawn!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error resetting talents for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Re-sync all unified attachments to client
     */
    private static void resyncAllAttachments(ServerPlayer player) {
        try {
            ImmortalCultivation.LOGGER.info("Force syncing all unified attachments after respawn for player {}",
                player.getName().getString());

            // Sync cultivation attachment (realms, cultivation progress, focus, bonuses)
            PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationAttachment != null) {
                cultivationAttachment.setPlayer(player); // IMPORTANT: Reset player reference
                cultivationAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Resynced cultivation attachment after respawn for player {}",
                    player.getName().getString());
            }

            // Sync resource attachment (health, qi, body energy)
            PlayerResourceAttachment resourceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resourceAttachment != null) {
                resourceAttachment.setPlayer(player); // IMPORTANT: Reset player reference
                resourceAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Resynced resource attachment after respawn for player {}",
                    player.getName().getString());
            }

            // Sync talent attachment (spiritual roots, physique)
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talentAttachment != null) {
                talentAttachment.setPlayer(player); // IMPORTANT: Reset player reference
                talentAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Resynced talent attachment after respawn for player {}",
                    player.getName().getString());
            }

            // OPTIONAL: Sync combat stats if still using separate attachment
            try {
                PlayerCombatStatsAttachment combatStatsAttachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                if (combatStatsAttachment != null) {
                    combatStatsAttachment.setPlayer(player);
                    combatStatsAttachment.forceSync();
                    ImmortalCultivation.LOGGER.debug("Resynced combat stats after respawn for player {}",
                        player.getName().getString());
                }
            } catch (Exception e) {
                // Combat stats might not exist in unified system
                ImmortalCultivation.LOGGER.debug("Combat stats attachment not available (might be unified): {}",
                    e.getMessage());
            }

            ImmortalCultivation.LOGGER.info("Successfully resynced all unified attachments after respawn for player {}",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error syncing unified attachments for player {}",
                player.getName().getString(), e);
        }
    }

    // === Helper methods to calculate max values safely ===

    private static long calculateMaxHealthForPlayer(ServerPlayer player) {
        try {
            PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationAttachment == null) return 20;

            long qiRealmHealth = cultivationAttachment.getCurrentQiRealm().getBaseHealth();
            long bodyRealmHealth = cultivationAttachment.getCurrentBodyRealm().getBaseHealth();

            if (qiRealmHealth == 20 && bodyRealmHealth == 20) {
                return 20; // Still mortal
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
            PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationAttachment == null) return 100;

            long baseQi = cultivationAttachment.getCurrentQiRealm().getBaseMaxQi();
            long cultivationBonus = (long)(cultivationAttachment.getCurrentQiCultivation() * 0.01);
            return baseQi + cultivationBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating max Qi", e);
            return 100;
        }
    }

    private static long calculateMaxBodyForPlayer(ServerPlayer player) {
        try {
            PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationAttachment == null) return 100;

            long baseBody = cultivationAttachment.getCurrentBodyRealm().getBaseMaxQi(); // Using same as Qi
            long cultivationBonus = (long)(cultivationAttachment.getCurrentBodyCultivation() * 0.01);
            return baseBody + cultivationBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating max Body", e);
            return 100;
        }
    }
}