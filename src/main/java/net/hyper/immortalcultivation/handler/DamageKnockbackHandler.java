package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerBodyCultivationAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class DamageKnockbackHandler {

    /**
     * Apply knockback based on damage source and amount
     * Call this from your ICModHealthEventHandler after damage is applied
     */
    public static void applyCustomKnockback(Player player, long damageAmount, long maxHealth, DamageSource damageSource) {
        if (player.level().isClientSide()) return;

        // Calculate knockback strength based on damage percentage
        float damagePercentage = maxHealth > 0 ? (float) damageAmount / maxHealth : 0.0f;
        float baseKnockback = Math.min(1.5f, damagePercentage * 3.0f); // Max 1.5 block knockback

        // Determine knockback direction
        Vec3 knockbackDirection = getKnockbackDirection(player, damageSource);
        if (knockbackDirection == null) return;

        // Apply knockback with cultivation-based resistance
        float knockbackResistance = getKnockbackResistance(player);
        float finalKnockback = baseKnockback * (1.0f - knockbackResistance);

        if (finalKnockback > 0.1f) {
            applyKnockbackMotion(player, knockbackDirection, finalKnockback);

            // Play knockback sound effect
            playKnockbackEffect(player, finalKnockback);
        }
    }

    private static Vec3 getKnockbackDirection(Player player, DamageSource damageSource) {
        Vec3 direction = null;

        // Check if damage has a direct source (entity or projectile)
        if (damageSource.getDirectEntity() != null) {
            Vec3 sourcePos = damageSource.getDirectEntity().position();
            Vec3 playerPos = player.position();
            direction = playerPos.subtract(sourcePos).normalize();
        }
        // Check if damage has an indirect source (e.g., explosion center)
        else if (damageSource.getEntity() != null) {
            Vec3 sourcePos = damageSource.getEntity().position();
            Vec3 playerPos = player.position();
            direction = playerPos.subtract(sourcePos).normalize();
        }
        // For environmental damage, use random horizontal direction
        else {
            double angle = Math.random() * Math.PI * 2;
            direction = new Vec3(Math.cos(angle), 0.1, Math.sin(angle)).normalize();
        }

        // Ensure some upward component for dramatic effect
        if (direction != null && direction.y < 0.2) {
            direction = new Vec3(direction.x, 0.2, direction.z).normalize();
        }

        return direction;
    }

    private static float getKnockbackResistance(Player player) {
        // Base resistance from cultivation level
        float resistance = 0.0f;

        try {
            // Higher cultivation levels provide knockback resistance
            PlayerBodyCultivationAttachment bodyCultivation =
                player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());

            if (bodyCultivation != null) {
                long maxBodyCultivation = bodyCultivation.getMaxBodyCultivation();
                // Every 100 body cultivation points = 1% knockback resistance, max 50%
                resistance = Math.min(0.5f, maxBodyCultivation / 10000.0f);
            }

            // Additional resistance from armor
            // (You can expand this based on your armor system)

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating knockback resistance", e);
        }

        return resistance;
    }

    private static void applyKnockbackMotion(Player player, Vec3 direction, float strength) {
        // Calculate velocity based on direction and strength
        Vec3 knockbackVelocity = direction.scale(strength);

        // Add current velocity to create cumulative effect
        Vec3 currentVelocity = player.getDeltaMovement();
        Vec3 newVelocity = currentVelocity.add(knockbackVelocity);

        // Limit maximum velocity to prevent launching players too far
        double maxVelocity = 2.0;
        double velocityMagnitude = newVelocity.length();
        if (velocityMagnitude > maxVelocity) {
            newVelocity = newVelocity.normalize().scale(maxVelocity);
        }

        // Apply the velocity
        player.setDeltaMovement(newVelocity);
        player.hasImpulse = true; // Important: tells Minecraft to sync velocity

        // Send velocity update to client if on server
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private static void playKnockbackEffect(Player player, float strength) {
        // Sound effect based on knockback strength
        SoundEvent sound;
        float volume;
        float pitch;

        if (strength > 1.0f) {
            sound = SoundEvents.PLAYER_HURT;
            volume = 0.3f;
            pitch = 1.5f;
        } else if (strength > 0.5f) {
            sound = SoundEvents.PLAYER_HURT;
            volume = 0.5f;
            pitch = 0.8f;
        } else {
            sound = SoundEvents.PLAYER_HURT;
            volume = 0.3f;
            pitch = 1.0f;
        }

        player.level().playSound(null, player.blockPosition(), sound,
            SoundSource.PLAYERS, volume, pitch);

        // Particle effect for dramatic knockback
        if (strength > 0.5f && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.POOF,
                player.getX(),
                player.getY() + 0.5,
                player.getZ(),
                (int) (strength * 10),
                0.5, 0.5, 0.5, 0.1
            );
        }
    }
}

