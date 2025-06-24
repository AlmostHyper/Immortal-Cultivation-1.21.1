package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.network.DamageFeedbackSyncPacket;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Handles vanilla-style damage feedback for the custom health system.
 * This ensures players get the same visual and physical feedback as vanilla damage.
 */
public class DamageFeedbackHandler {

    /**
     * Apply vanilla-style damage feedback when custom damage is taken.
     * This should be called from ICModHealthEventHandler after custom damage is applied.
     *
     * @param player The player who took damage
     * @param damageAmount The amount of damage taken
     * @param damageSource The source of the damage
     */
    public static void applyVanillaDamageFeedback(Player player, long damageAmount, DamageSource damageSource) {
        if (player.level().isClientSide()) {
            // On client side, trigger local effects
            triggerClientSideEffects(player, damageAmount);
        } else {
            // On server side, trigger server effects and sync to client
            triggerServerSideEffects(player, damageAmount, damageSource);
        }
    }

    /**
     * Trigger client-side damage effects (hurt animation, red overlay)
     */
    private static void triggerClientSideEffects(Player player, long damageAmount) {
        // Convert damage to vanilla scale for proper feedback intensity
        float vanillaDamage = Math.min(20.0f, damageAmount / 10.0f);

        // Trigger the hurt animation and red screen overlay
        player.hurtTime = 10; // Duration of hurt animation (10 ticks = 0.5 seconds)
        player.hurtDuration = 10; // How long the hurt effect lasts

        // Set the hurt direction using reflection (protected field)
        setHurtDirection(player);

        ImmortalCultivation.LOGGER.debug("Applied client-side damage feedback for {} damage", damageAmount);
    }

    /**
     * Set hurt direction using reflection to access protected field
     */
    private static void setHurtDirection(Player player) {
        try {
            // Use reflection to access the protected hurtDir field
            java.lang.reflect.Field hurtDirField = Player.class.getDeclaredField("hurtDir");
            hurtDirField.setAccessible(true);
            hurtDirField.setFloat(player, (float) (Math.random() * Math.PI * 2));
        } catch (Exception e) {
            // If reflection fails, that's okay - the hurt animation will still work without direction
            ImmortalCultivation.LOGGER.debug("Could not set hurt direction (this is normal): {}", e.getMessage());
        }
    }

    /**
     * Trigger server-side effects and send feedback to client
     */
    private static void triggerServerSideEffects(Player player, long damageAmount, DamageSource damageSource) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Apply knockback
        applyVanillaKnockback(player, damageAmount, damageSource);

        // Send damage feedback packet to client
        DamageFeedbackSyncPacket packet = new DamageFeedbackSyncPacket(damageAmount, damageSource.getMsgId());
        ICModNetwork.sendToPlayer(packet, serverPlayer);

        // Set server-side hurt time for animation sync
        player.hurtTime = 10;
        player.hurtDuration = 10;

        ImmortalCultivation.LOGGER.debug("Applied server-side damage feedback for {} damage from {}",
            damageAmount, damageSource.getMsgId());
    }

    /**
     * Apply vanilla-style knockback based on damage source and amount
     */
    private static void applyVanillaKnockback(Player player, long damageAmount, DamageSource damageSource) {
        // Don't apply knockback if damage source bypasses it or player is in creative
        if (damageSource.is(net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK) ||
            player.getAbilities().instabuild) {
            return;
        }

        // Calculate knockback strength based on damage (vanilla uses 0.4 * damage strength)
        float knockbackStrength = Math.min(1.0f, damageAmount / 20.0f * 0.4f);

        // Get knockback direction
        Vec3 knockbackDirection = getKnockbackDirection(player, damageSource);
        if (knockbackDirection == null) {
            return;
        }

        // Apply the knockback velocity
        Vec3 currentVelocity = player.getDeltaMovement();
        Vec3 knockbackVelocity = knockbackDirection.scale(knockbackStrength);

        // Vanilla knockback only affects horizontal movement and adds upward velocity
        double newVelX = currentVelocity.x + knockbackVelocity.x;
        double newVelY = currentVelocity.y + Math.max(0.0, knockbackVelocity.y * 0.5);
        double newVelZ = currentVelocity.z + knockbackVelocity.z;

        // Apply velocity limits (same as vanilla)
        newVelX = Math.max(-1.5, Math.min(1.5, newVelX));
        newVelY = Math.max(-1.5, Math.min(1.5, newVelY));
        newVelZ = Math.max(-1.5, Math.min(1.5, newVelZ));

        player.setDeltaMovement(newVelX, newVelY, newVelZ);
        player.hasImpulse = true;

        ImmortalCultivation.LOGGER.debug("Applied knockback: {} strength, direction: {}",
            knockbackStrength, knockbackDirection);
    }

    /**
     * Calculate knockback direction based on damage source (vanilla logic)
     */
    private static Vec3 getKnockbackDirection(Player player, DamageSource damageSource) {
        Vec3 direction = null;

        // Try to get direction from direct entity (attacker, projectile)
        if (damageSource.getDirectEntity() != null) {
            Vec3 attackerPos = damageSource.getDirectEntity().position();
            Vec3 playerPos = player.position();
            direction = playerPos.subtract(attackerPos);
        }
        // Try to get direction from source entity (explosion center, etc.)
        else if (damageSource.getEntity() != null) {
            Vec3 sourcePos = damageSource.getEntity().position();
            Vec3 playerPos = player.position();
            direction = playerPos.subtract(sourcePos);
        }
        // For environmental damage, use player's facing direction (reversed)
        else {
            direction = player.getLookAngle().scale(-1.0);
        }

        if (direction != null) {
            // Normalize and ensure some horizontal component
            direction = direction.normalize();

            // Vanilla adds some randomness to knockback direction
            double randomX = (Math.random() - 0.5) * 0.2;
            double randomZ = (Math.random() - 0.5) * 0.2;

            direction = direction.add(randomX, 0.0, randomZ).normalize();

            // Ensure minimum upward component for dramatic effect
            if (direction.y < 0.1) {
                direction = new Vec3(direction.x, 0.1, direction.z).normalize();
            }
        }

        return direction;
    }

    /**
     * Check if the player should receive knockback based on their state
     */
    private static boolean shouldApplyKnockback(Player player, DamageSource damageSource) {
        // Don't apply knockback if:
        // - Player is in creative mode
        // - Damage source specifically avoids knockback
        // - Player is already being knocked back
        return !player.getAbilities().instabuild &&
            !damageSource.is(net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK) &&
            player.hurtTime <= 0;
    }

    /**
     * Convert custom damage amount to vanilla-equivalent for proper scaling
     */
    private static float convertToVanillaScale(long customDamage) {
        // Adjust this conversion based on your health scaling
        // This assumes custom health is roughly 10x vanilla health
        return Math.min(20.0f, customDamage / 10.0f);
    }
}