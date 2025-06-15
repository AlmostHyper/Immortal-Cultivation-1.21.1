package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.hyper.immortalcultivation.attachment.PlayerSpiritualRootsAttachment;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class CultivationTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerCultivationAttachment cultivationAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivationAttachment != null) {
            // TEMPORARILY COMMENT OUT: This might be interfering
            // cultivationAttachment.tick();

            // Only process every tick for now (we'll optimize later)
            processCultivationRegeneration(serverPlayer);
        }
    }

    private static void processCultivationRegeneration(ServerPlayer player) {
        PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivationAttachment == null) {
            ImmortalCultivation.LOGGER.error("CultivationAttachment is null for player {}", player.getName().getString());
            return;
        }

        // Skip if cultivation is already full
        if (cultivationAttachment.isCultivationFull()) {
            if (player.tickCount % 100 == 0) {
                ImmortalCultivation.LOGGER.info("Player {} cultivation is full: {}/{}",
                    player.getName().getString(),
                    cultivationAttachment.getCurrentCultivation(),
                    cultivationAttachment.getMaxCultivation());
            }
            return;
        }

        // Get base cultivation rate and spiritual root info
        double baseCultivationRate = cultivationAttachment.getBaseCultivationRate();
        double effectiveCultivationRate = cultivationAttachment.getEffectiveCultivationRate();

        // DEBUG: Log rates every 5 seconds
        if (player.tickCount % 100 == 0) {
            String rootInfo = getSpiritualRootInfo(player);
            ImmortalCultivation.LOGGER.info("Player {}: Base rate={}, Effective rate={}, Roots={}",
                player.getName().getString(), baseCultivationRate, effectiveCultivationRate, rootInfo);
        }

        // Check if player is meditating for bonus rate
        boolean isMeditating = isPlayerMeditating(player);
        double finalRate = effectiveCultivationRate;

        if (isMeditating) {
            double baseMeditationRate = cultivationAttachment.getBaseMeditationCultivationRate();
            double effectiveMeditationRate = cultivationAttachment.getEffectiveMeditationCultivationRate();
            finalRate = effectiveMeditationRate;

            if (player.tickCount % 100 == 0) {
                ImmortalCultivation.LOGGER.info("Player {} is meditating: Base meditation={}, Effective meditation={}",
                    player.getName().getString(), baseMeditationRate, effectiveMeditationRate);
            }
        }

        // Convert per-second rate to per-tick (20 ticks = 1 second)
        double cultivationPerTick = finalRate / 20.0;

        if (player.tickCount % 100 == 0) {
            ImmortalCultivation.LOGGER.info("Player {}: Final rate={}/sec, Per tick={}, Meditating={}",
                player.getName().getString(), finalRate, cultivationPerTick, isMeditating);
        }

        // Add to cultivation
        if (cultivationPerTick > 0) {
            addFractionalCultivation(cultivationAttachment, cultivationPerTick, player);
        } else {
            if (player.tickCount % 100 == 0) {
                ImmortalCultivation.LOGGER.warn("Player {} has cultivation rate of 0!", player.getName().getString());
            }
        }
    }

    private static void addFractionalCultivation(PlayerCultivationAttachment attachment, double amount, ServerPlayer player) {
        try {
            // Get current fraction - use the correct method name
            double currentFraction = attachment.getRegenFraction(); // Changed from getCultivationFraction()
            double oldFraction = currentFraction;
            currentFraction += amount;

            // DEBUG: Log fraction changes every 5 seconds
            if (player.tickCount % 100 == 0) {
                ImmortalCultivation.LOGGER.info("Player {}: Old fraction={}, Adding={}, New fraction={}",
                    player.getName().getString(), oldFraction, amount, currentFraction);
            }

            // If we've accumulated at least 1 whole cultivation point
            if (currentFraction >= 1.0) {
                long wholePoints = (long) currentFraction;
                currentFraction -= wholePoints;

                // Add the whole points
                long oldCultivation = attachment.getCurrentCultivation();
                long actuallyAdded = attachment.addCultivation(wholePoints);
                long newCultivation = attachment.getCurrentCultivation();

                // Update the fraction
                attachment.setRegenFraction(currentFraction);

                // LOG: Always log when cultivation is actually added
                ImmortalCultivation.LOGGER.info("Player {} gained {} cultivation! ({} -> {}, remaining fraction={})",
                    player.getName().getString(), actuallyAdded, oldCultivation, newCultivation, currentFraction);
            } else {
                // Just update the fraction
                attachment.setRegenFraction(currentFraction);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error adding fractional cultivation to player {}",
                player.getName().getString(), e);
        }
    }

    private static boolean isPlayerMeditating(ServerPlayer player) {
        // Check if player is crouching and not moving
        if (!player.isCrouching()) {
            return false;
        }

        // Check velocity to see if player is moving
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        return horizontalSpeed < 0.01;
    }

    private static String getSpiritualRootInfo(ServerPlayer player) {
        try {
            PlayerSpiritualRootsAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                return String.format("%s (%.1fx)",
                    roots.getRootType().getDisplayName(),
                    roots.getCultivationMultiplier());
            } else {
                return "Roots attachment is null";
            }
        } catch (Exception e) {
            return "Error getting roots: " + e.getMessage();
        }
    }
}