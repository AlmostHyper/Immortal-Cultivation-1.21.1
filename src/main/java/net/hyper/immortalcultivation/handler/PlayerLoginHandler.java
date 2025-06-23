package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.hyper.immortalcultivation.cultivation.PhysiqueType;
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

/**
 * Handles player login initialization for the dual cultivation system.
 * This ensures that all cultivation attachments are properly initialized when
 * a player joins the world, including both Qi and Body cultivation paths.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class PlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Player {} logged in, initializing dual cultivation system attachments",
                player.getName().getString());

            // Initialize all cultivation attachments in the proper order
            // Order matters here because some attachments depend on others
            initializeSpiritualRoots(player);
            initializePhysique(player);
            initializeRealms(player);
            initializeCultivationFocus(player);
            initializeQiCultivation(player);
            initializeBodyCultivation(player);
            initializeResources(player);

            // IMPORTANT: Force sync all data to ensure client has latest values
            forceSyncAllData(player);

            ImmortalCultivation.LOGGER.info("Dual cultivation system fully initialized for player: {}",
                player.getName().getString());
        }
    }

    /**
     * Initialize spiritual roots for Qi cultivation.
     * This determines the player's natural affinity for Qi cultivation and provides
     * a permanent multiplier to all Qi-related cultivation and regeneration.
     */
    private static void initializeSpiritualRoots(ServerPlayer player) {
        try {
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());

            if (roots != null && !roots.hasBeenAssigned()) {
                // Generate spiritual roots for new players
                SpiritualRootType generatedType = SpiritualRootType.generateRandom(player.getRandom());
                roots.assignRootType(generatedType);

                // Notify player with a colored message based on root quality
                Component message = Component.literal("Born with ")
                    .append(Component.literal(generatedType.getDisplayName())
                        .withStyle(Style.EMPTY.withColor(generatedType.getColor())))
                    .append("");

                player.sendSystemMessage(message);

                // Play sound effect for root assignment
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);

                ImmortalCultivation.LOGGER.info("Player {} assigned spiritual roots: {} ({}x multiplier)",
                    player.getName().getString(), generatedType.getDisplayName(), generatedType.getCultivationMultiplier());
            }

            // Log current state for debugging
            if (roots != null) {
                ImmortalCultivation.LOGGER.info("Player {} spiritual roots state - Type: {}, Assigned: {}, Multiplier: {}",
                    player.getName().getString(),
                    roots.getRootType().getDisplayName(),
                    roots.hasBeenAssigned(),
                    roots.getCultivationMultiplier());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing spiritual roots for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize physique for Body cultivation.
     * Similar to spiritual roots but for the Body cultivation path.
     * This provides a multiplier for Body cultivation and physical enhancement.
     */
    private static void initializePhysique(ServerPlayer player) {
        try {
            PlayerPhysiqueAttachment physique = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());

            if (physique != null && !physique.hasBeenAssigned()) {
                // Generate physique for new players
                PhysiqueType generatedType = PhysiqueType.generateRandom(player.getRandom());
                physique.assignRootType(generatedType);

                // Notify player about their physique
                Component message = Component.literal("Born with ")
                    .append(Component.literal(generatedType.getDisplayName())
                        .withStyle(Style.EMPTY.withColor(generatedType.getColor())))
                    .append("");

                player.sendSystemMessage(message);

                // Play a slightly different sound for physique
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.8f);

                ImmortalCultivation.LOGGER.info("Player {} assigned physique: {} ({}x multiplier)",
                    player.getName().getString(), generatedType.getDisplayName(), generatedType.getCultivationMultiplier());
            }

            // Log current state for debugging
            if (physique != null) {
                ImmortalCultivation.LOGGER.info("Player {} physique state - Type: {}, Assigned: {}, Multiplier: {}",
                    player.getName().getString(),
                    physique.getRootType().getDisplayName(),
                    physique.hasBeenAssigned(),
                    physique.getCultivationMultiplier());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing physique for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize realm attachments for both Qi and Body cultivation.
     * This sets up the dual realm system and ensures both cultivation paths
     * start at the proper mortal realm.
     */
    private static void initializeRealms(ServerPlayer player) {
        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment != null) {
                // Ensure player reference is set for realm calculations
                realmAttachment.setPlayer(player);

                // Initialize Qi realm if null
                if (realmAttachment.getCurrentQiRealm() == null) {
                    ImmortalCultivation.LOGGER.warn("Player {} has null Qi realm, setting to default mortal realm",
                        player.getName().getString());
                    realmAttachment.setCurrentRealm(PlayerRealmAttachment.CultivationType.QI,
                        ICModCultivationRealms.MORTAL, false);
                }

                // Initialize Body realm if null
                if (realmAttachment.getCurrentBodyRealm() == null) {
                    ImmortalCultivation.LOGGER.warn("Player {} has null Body realm, setting to default mortal realm",
                        player.getName().getString());
                    realmAttachment.setCurrentRealm(PlayerRealmAttachment.CultivationType.BODY,
                        ICModCultivationRealms.MORTAL, false);
                }

                ImmortalCultivation.LOGGER.info("Player {} realms initialized - Qi: {}, Body: {}",
                    player.getName().getString(),
                    realmAttachment.getCurrentQiRealm().getDisplayName(),
                    realmAttachment.getCurrentBodyRealm().getDisplayName());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null realm attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing realms for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize cultivation focus system.
     * This allows players to focus their cultivation efforts on either Qi or Body
     * cultivation for enhanced rates and bonuses.
     */
    private static void initializeCultivationFocus(ServerPlayer player) {
        try {
            PlayerCultivationFocusAttachment focusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focusAttachment != null) {
                // Default focus is Qi cultivation
                // The attachment initializes with Qi focus by default, so we just log it
                ImmortalCultivation.LOGGER.info("Player {} cultivation focus initialized: {} cultivation (2x rate bonus)",
                    player.getName().getString(),
                    focusAttachment.getCurrentFocus().getDisplayName());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null cultivation focus attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing cultivation focus for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize Qi cultivation attachment.
     * This handles the permanent Qi cultivation progress that determines
     * breakthrough eligibility and provides bonuses.
     */
    private static void initializeQiCultivation(ServerPlayer player) {
        try {
            PlayerQiCultivationAttachment qiCultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiCultivationAttachment != null) {
                ImmortalCultivation.LOGGER.info("Player {} Qi cultivation initialized: {}/{}",
                    player.getName().getString(),
                    qiCultivationAttachment.getCurrentQiCultivation(),
                    qiCultivationAttachment.getMaxQiCultivation());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null Qi cultivation attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing Qi cultivation for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize Body cultivation attachment.
     * This handles the permanent Body cultivation progress, similar to Qi cultivation
     * but for the physical enhancement path.
     */
    private static void initializeBodyCultivation(ServerPlayer player) {
        try {
            PlayerBodyCultivationAttachment bodyCultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyCultivationAttachment != null) {
                ImmortalCultivation.LOGGER.info("Player {} Body cultivation initialized: {}/{}",
                    player.getName().getString(),
                    bodyCultivationAttachment.getCurrentBodyCultivation(),
                    bodyCultivationAttachment.getMaxBodyCultivation());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null Body cultivation attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing Body cultivation for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize resource attachments (Qi and Body energy).
     * These are the consumable resources used for techniques and abilities,
     * separate from the cultivation progress values.
     */
    private static void initializeResources(ServerPlayer player) {
        try {
            // Initialize Qi resource attachment
            PlayerQiAttachment qiAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI.get());
            if (qiAttachment != null) {
                ImmortalCultivation.LOGGER.info("Player {} Qi resource initialized: {}/{} (regen: {}/sec)",
                    player.getName().getString(),
                    qiAttachment.getCurrentQi(),
                    qiAttachment.getMaxQi(),
                    String.format("%.2f", qiAttachment.getEffectiveQiRegenerationRate()));
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null Qi resource attachment!",
                    player.getName().getString());
            }

            // Initialize Body resource attachment
            PlayerBodyAttachment bodyAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY.get());
            if (bodyAttachment != null) {
                ImmortalCultivation.LOGGER.info("Player {} Body resource initialized: {}/{} (regen: {}/sec)",
                    player.getName().getString(),
                    bodyAttachment.getCurrentBody(),
                    bodyAttachment.getMaxBody(),
                    String.format("%.2f", bodyAttachment.getEffectiveBodyRegenerationRate()));
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null Body resource attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing resources for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Force sync all cultivation data to ensure client has the latest values.
     * This is critical for proper display in GUIs and HUD elements.
     */
    private static void forceSyncAllData(ServerPlayer player) {
        try {
            ImmortalCultivation.LOGGER.info("Force syncing all cultivation data for player {}",
                player.getName().getString());

            // FIXED: Sync spiritual roots (this was missing!)
            PlayerSpiritualRootAttachment rootAttachment = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (rootAttachment != null) {
                rootAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Synced spiritual roots for player {}", player.getName().getString());
            }

            // FIXED: Sync physique (this was missing!)
            PlayerPhysiqueAttachment physiqueAttachment = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
            if (physiqueAttachment != null) {
                physiqueAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Synced physique for player {}", player.getName().getString());
            }

            // Sync realm data
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment != null) {
                realmAttachment.forceSync();
            }

            // Sync cultivation focus
            PlayerCultivationFocusAttachment focusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focusAttachment != null) {
                focusAttachment.forceSync();
            }

            // Sync Qi cultivation
            PlayerQiCultivationAttachment qiCultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiCultivationAttachment != null) {
                qiCultivationAttachment.forceSync();
            }

            // Sync Body cultivation
            PlayerBodyCultivationAttachment bodyCultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyCultivationAttachment != null) {
                bodyCultivationAttachment.forceSync();
            }

            // Sync resources
            PlayerQiAttachment qiAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI.get());
            if (qiAttachment != null) {
                qiAttachment.forceSync();
            }

            PlayerBodyAttachment bodyAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY.get());
            if (bodyAttachment != null) {
                bodyAttachment.forceSync();
            }

            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                healthAttachment.forceSync();
            }

            ImmortalCultivation.LOGGER.info("Successfully synced all cultivation data for player {}",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error force syncing data for player {}",
                player.getName().getString(), e);
        }
    }


}