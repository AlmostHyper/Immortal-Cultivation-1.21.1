package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Handles all cultivation-related ticking for players.
 * This includes both Qi and Body cultivation paths, as well as meditation detection,
 * resource regeneration, and bonus management for the dual cultivation system.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class CultivationTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Process all cultivation attachments - they handle their own logic now
        tickCultivationAttachments(serverPlayer);

        // Process cultivation bonus attachment for sync management
        tickCultivationBonus(serverPlayer);

        // Process meditation detection for both cultivation types
        processMeditationDetection(serverPlayer);

        // Process resource regeneration (Qi and Body energy, not cultivation)
        processResourceRegeneration(serverPlayer);

        // Tick the realm attachment for time tracking and periodic effects
        tickRealmAttachment(serverPlayer);

        // Tick the cultivation focus attachment
        tickCultivationFocus(serverPlayer);

        // Tick combat stats attachment
        tickCombatStats(serverPlayer);
    }

    /**
     * Tick all cultivation attachments. Each attachment has its own tick method
     * that handles accumulation, syncing, and other cultivation-specific logic.
     */
    private static void tickCultivationAttachments(ServerPlayer player) {
        try {
            // Tick Qi Cultivation attachment - handles Qi cultivation accumulation
            PlayerQiCultivationAttachment qiCultivation = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiCultivation != null) {
                qiCultivation.tick();
            }

            // Tick Body Cultivation attachment - handles Body cultivation accumulation
            PlayerBodyCultivationAttachment bodyCultivation = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyCultivation != null) {
                bodyCultivation.tick();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking cultivation attachments for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Tick cultivation bonus attachment for periodic sync management and internal logic.
     * This ensures that pill bonuses are properly maintained and synced to the client.
     */
    private static void tickCultivationBonus(ServerPlayer player) {
        try {
            PlayerCultivationBonusAttachment bonusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());
            if (bonusAttachment != null) {
                bonusAttachment.tick();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking cultivation bonus attachment for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Process meditation detection for both Qi and Body cultivation.
     * Meditation enhances cultivation rates significantly, so we need to detect
     * when players are in the proper meditation posture.
     */
    private static void processMeditationDetection(ServerPlayer player) {
        boolean isMeditating = isPlayerMeditating(player);

        try {
            // Update Qi cultivation meditation state
            PlayerQiCultivationAttachment qiCultivation = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiCultivation != null) {
                if (isMeditating) {
                    qiCultivation.startMeditation();
                } else {
                    qiCultivation.stopMeditation();
                }
            }

            // Update Body cultivation meditation state
            PlayerBodyCultivationAttachment bodyCultivation = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyCultivation != null) {
                if (isMeditating) {
                    bodyCultivation.startMeditation();
                } else {
                    bodyCultivation.stopMeditation();
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing meditation for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Process Qi and Body resource regeneration.
     * These are the consumable resources (like mana/stamina), not cultivation progress.
     */
    private static void processResourceRegeneration(ServerPlayer player) {
        try {
            // Tick Qi resource attachment - handles Qi energy regeneration
            PlayerQiAttachment qiResource = player.getData(ICModAttachmentTypes.PLAYER_QI.get());
            if (qiResource != null) {
                qiResource.tick();
            }

            // Tick Body resource attachment - handles Body energy regeneration
            PlayerBodyAttachment bodyResource = player.getData(ICModAttachmentTypes.PLAYER_BODY.get());
            if (bodyResource != null) {
                bodyResource.tick();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing resource regeneration for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Tick the realm attachment for time tracking and periodic realm effects.
     * This handles things like realm-based attribute bonuses and breakthrough cooldowns.
     */
    private static void tickRealmAttachment(ServerPlayer player) {
        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment != null) {
                realmAttachment.tick();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking realm attachment for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Tick the cultivation focus attachment for cooldown management and time tracking.
     */
    private static void tickCultivationFocus(ServerPlayer player) {
        try {
            PlayerCultivationFocusAttachment focusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focusAttachment != null) {
                focusAttachment.tick();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking cultivation focus for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Tick combat stats attachment for sync management and internal logic.
     * This ensures that combat bonuses are properly maintained and synced to the client.
     */
    private static void tickCombatStats(ServerPlayer player) {
        try {
            PlayerCombatStatsAttachment combatStatsAttachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStatsAttachment != null) {
                combatStatsAttachment.tick();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking combat stats attachment for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Determine if a player is in meditation posture.
     * Meditation requires the player to be crouching and relatively stationary.
     * This is used by both Qi and Body cultivation for enhanced cultivation rates.
     */
    private static boolean isPlayerMeditating(ServerPlayer player) {
        // Check if player is crouching (shift key held)
        if (!player.isCrouching()) {
            return false;
        }

        // Check velocity to see if player is moving
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Allow very small movement (account for server lag/float precision)
        return horizontalSpeed < 0.01;
    }

    /**
     * Get debug information about a player's cultivation state.
     * Useful for troubleshooting cultivation issues.
     */
    public static String getCultivationDebugInfo(ServerPlayer player) {
        StringBuilder debug = new StringBuilder();
        debug.append("=== Cultivation Debug Info for ").append(player.getName().getString()).append(" ===\n");

        try {
            // Spiritual roots info
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                debug.append("Spiritual Roots: ").append(roots.getRootType().getDisplayName())
                    .append(" (").append(String.format("%.1fx", roots.getCultivationMultiplier())).append(")\n");
            }

            // Physique info
            PlayerPhysiqueAttachment physique = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
            if (physique != null) {
                debug.append("Physique: ").append(physique.getRootType().getDisplayName())
                    .append(" (").append(String.format("%.1fx", physique.getCultivationMultiplier())).append(")\n");
            }

            // Realm info
            PlayerRealmAttachment realm = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realm != null) {
                debug.append("Qi Realm: ").append(realm.getCurrentQiRealm().getDisplayName()).append("\n");
                debug.append("Body Realm: ").append(realm.getCurrentBodyRealm().getDisplayName()).append("\n");
            }

            // Cultivation focus
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focus != null) {
                debug.append("Cultivation Focus: ").append(focus.getCurrentFocus().getDisplayName()).append("\n");
            }

            // ADDED: Cultivation bonus info
            PlayerCultivationBonusAttachment bonus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());
            if (bonus != null) {
                debug.append("Cultivation Bonuses:\n");
                debug.append("  - Qi Additive: +").append(String.format("%.3f", bonus.getQiCultivationRawAdditiveBonus())).append("/sec\n");
                debug.append("  - Body Additive: +").append(String.format("%.3f", bonus.getBodyCultivationRawAdditiveBonus())).append("/sec\n");
                debug.append("  - Qi Multiplicative: ").append(String.format("%.1fx", bonus.getQiCultivationMultiplicativeBonus())).append("\n");
                debug.append("  - Body Multiplicative: ").append(String.format("%.1fx", bonus.getBodyCultivationMultiplicativeBonus())).append("\n");
            }

            // Combat stats info
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats != null) {
                debug.append("Combat Stats:\n");
                debug.append("  - Attack Damage: +").append(String.format("%.1f", combatStats.getAttackDamageBonus() * 100)).append("%\n");
                debug.append("  - Movement Speed: +").append(String.format("%.1f", combatStats.getMovementSpeedBonus() * 100)).append("%\n");
                debug.append("  - Jump Height: +").append(String.format("%.1f", combatStats.getJumpHeightBonus() * 100)).append("%\n");
                debug.append("  - Total Score: ").append(String.format("%.1f", combatStats.getTotalBonusScore())).append("\n");
            }

            // Meditation state
            debug.append("Is Meditating: ").append(isPlayerMeditating(player)).append("\n");

        } catch (Exception e) {
            debug.append("Error gathering debug info: ").append(e.getMessage()).append("\n");
        }

        return debug.toString();
    }
}