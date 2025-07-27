package net.hyper.immortalcultivation.server.events.server;

import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.core.attachments.ICultivationData;
import net.hyper.immortalcultivation.core.attachments.player.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.core.network.packets.CultivationPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
        boolean hasChanges = false;

        for (ICultivationData.CultivationType type : ICultivationData.CultivationType.values()) {
            double baseRate = cultivation.getBaseCultivationRate(type);

            CultivationTickEvent tickEvent = new CultivationTickEvent(player, type, baseRate);

            double focusMultiplier = (type == cultivation.getCurrentFocus()) ? 2.0 : 1.0;
            if (focusMultiplier != 1.0) {
                tickEvent.addModifier(new CultivationTickEvent.CultivationModifier.Multiplicative(
                    "Focus Bonus", 50, focusMultiplier));
            }

            double meditationMultiplier = cultivation.getMeditationMultiplier();
            if (meditationMultiplier != 1.0) {
                tickEvent.addModifier(new CultivationTickEvent.CultivationModifier.Multiplicative(
                    "Meditation Bonus", 60, meditationMultiplier));
            }

            double attachmentMultiplier = cultivation.getTotalCultivationRateMultiplier();
            if (attachmentMultiplier != 1.0) {
                tickEvent.addModifier(new CultivationTickEvent.CultivationModifier.Multiplicative(
                    "Multiplicative Bonuses", 100, attachmentMultiplier));
            }

            NeoForge.EVENT_BUS.post(tickEvent);

            double finalRate = tickEvent.calculateFinalRate();

            if (finalRate > 0) {
                long oldValue = cultivation.getCurrentCultivation(type);
                applyCultivationProgress(cultivation, type, finalRate);
                if (cultivation.getCurrentCultivation(type) != oldValue) {
                    hasChanges = true;
                }
            }
        }

        if (hasChanges && player instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer, cultivation);
        }
    }

    private static void syncToClient(ServerPlayer player, PlayerCultivationAttachment cultivation) {
        CompoundTag data = PlayerCultivationAttachment.serialize(cultivation, player.level().registryAccess());
        PacketDistributor.sendToPlayer(player, new CultivationPacket(
            CultivationPacket.Action.SYNC_DATA,
            ICultivationData.CultivationType.QI,
            0, data
        ));
    }

    private static void applyCultivationProgress(PlayerCultivationAttachment cultivation,
                                                 ICultivationData.CultivationType type,
                                                 double ratePerSecond) {

        double ratePerTick = ratePerSecond / 20.0;
        cultivation.addRegenFraction (type, ratePerTick);
    }

    /*
    private void processMeditationTick() {
        if (!isMeditating) {return;}

        meditationTicks++;

        updateMeditationMultiplier();
        maintainMeditationPose();
        checkMeditationInterruption();
    }

    private void updateMeditationMultiplier() {
        double progress = getMeditationProgress();
        double smoothProgress = 1.0 - Math.cos(progress * Math.PI / 2.0);

        currentMeditationMultiplier = 1.0 + (smoothProgress * (MAX_MEDITATION_MULTIPLIER - 1.0));
        currentMeditationMultiplier = Math.min(currentMeditationMultiplier, MAX_MEDITATION_MULTIPLIER);
    }

    private void maintainMeditationPose() {
        if (meditationTicks - lastPoseUpdateTick >= MEDITATION_POSE_UPDATE_INTERVAL) {
            if (serverPlayer.getPose() != Pose.SITTING) {serverPlayer.setPose(Pose.SITTING);}

            lastPoseUpdateTick = meditationTicks;
        }
    }

    private void checkMeditationInterruption(@Nullable ServerPlayer serverPlayer) {
        boolean shouldInterrupt = serverPlayer.getDeltaMovement().lengthSqr() > 0.001;

        if (serverPlayer.getPose() != Pose.SITTING) {shouldInterrupt = true;}

        if (shouldInterrupt) {
            isMeditating = false;
            stopMeditation();

            serverPlayer.sendSystemMessage(Component.literal("Meditation was interrupted!")
                .withStyle(ChatFormatting.YELLOW));
        }
    } */

    // Server Logic
    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {

    }
}
