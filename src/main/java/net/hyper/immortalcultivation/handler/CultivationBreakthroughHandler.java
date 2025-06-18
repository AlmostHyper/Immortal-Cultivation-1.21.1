package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerQiCultivationAttachment;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

public class CultivationBreakthroughHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Check for breakthrough every 5 seconds to avoid spam
        if (serverPlayer.tickCount % 100 == 0) {
            checkForAutomaticBreakthrough(serverPlayer);
        }
    }

    private static void checkForAutomaticBreakthrough(ServerPlayer player) {
        PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
        if (realmAttachment == null || realmAttachment.getCurrentRealm() == null) {
            return;
        }

        CultivationRealm currentRealm = realmAttachment.getCurrentRealm();

        // Check if player meets breakthrough requirements
        if (canBreakthrough(player, currentRealm)) {
            // You can choose automatic breakthrough or just notify the player
            notifyPlayerOfBreakthrough(player, currentRealm);

            // Uncomment for automatic breakthrough:
            // attemptBreakthrough(player, currentRealm);
        }
    }

    public static boolean canBreakthrough(ServerPlayer player, CultivationRealm currentRealm) {
        PlayerQiCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivationAttachment == null) {
            return false;
        }

        // Check if cultivation is at maximum
        if (!cultivationAttachment.isCultivationFull()) {
            return false;
        }

        // Check if there are next realms available
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();
        if (nextRealms.isEmpty()) {
            return false;
        }

        // Check realm-specific requirements
        return currentRealm.meetsAdditionalRequirements(player, cultivationAttachment);
    }

    private static void notifyPlayerOfBreakthrough(ServerPlayer player, CultivationRealm currentRealm) {
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();
        if (!nextRealms.isEmpty()) {
            CultivationRealm nextRealm = nextRealms.get(0); // Usually just one next realm

            player.sendSystemMessage(
                Component.literal("🌟 Breakthrough Available! 🌟")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            );
            player.sendSystemMessage(
                Component.literal("You are ready to advance from ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(currentRealm.getDisplayName())
                        .withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" to ")
                        .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(nextRealm.getDisplayName())
                        .withStyle(ChatFormatting.GOLD))
            );
            player.sendSystemMessage(
                Component.literal("Use /breakthrough to advance your cultivation!")
                    .withStyle(ChatFormatting.GREEN)
            );

            // Play sound effect
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    public static boolean attemptBreakthrough(ServerPlayer player, CultivationRealm currentRealm) {
        if (!canBreakthrough(player, currentRealm)) {
            return false;
        }

        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();
        if (nextRealms.isEmpty()) {
            return false;
        }

        CultivationRealm nextRealm = nextRealms.get(0);

        // Perform the breakthrough
        PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
        PlayerQiCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

        // Reset cultivation to 0 (breakthrough consumes all cultivation)
        cultivationAttachment.setCurrentCultivation(0);

        // Advance to next realm
        realmAttachment.setCurrentRealm(nextRealm);

        // Trigger realm advancement events
        nextRealm.onRealmAdvancement(player, cultivationAttachment, currentRealm);

        // Breakthrough effects
        playBreakthroughEffects(player, currentRealm, nextRealm);

        ImmortalCultivation.LOGGER.info("Player {} broke through from {} to {}",
            player.getName().getString(),
            currentRealm.getDisplayName(),
            nextRealm.getDisplayName());

        return true;
    }

    private static void playBreakthroughEffects(ServerPlayer player, CultivationRealm oldRealm, CultivationRealm newRealm) {
        // Dramatic breakthrough message
        player.sendSystemMessage(
            Component.literal("⚡ BREAKTHROUGH SUCCESSFUL! ⚡")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        );
        player.sendSystemMessage(
            Component.literal("You have advanced to ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(newRealm.getDisplayName())
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
        );

        // Particle effects
        if (player.level() instanceof ServerLevel serverLevel) {
            // Golden particles around player
            serverLevel.sendParticles(
                ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0, player.getZ(),
                50, 1.0, 1.0, 1.0, 0.1
            );

            // Lightning effect
            serverLevel.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.5, 0.5, 0.5, 0.2
            );
        }

        // Sound effects
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Health and effects boost
        player.heal(player.getMaxHealth()); // Full heal
        player.removeAllEffects(); // Clear negative effects

        // Temporary breakthrough glow effect
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0)); // 10 seconds
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1)); // 10 seconds of regen
    }
}
