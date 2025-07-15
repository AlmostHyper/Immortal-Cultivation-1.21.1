package net.hyper.immortalcultivation.server.events.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatEventHandler {

    /**
     * Intercepts and processes incoming damage for cultivation system
     *
     * @param event Incoming damage event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        UUID playerId = player.getUUID();
        DamageSource damageSource = event.getSource();
        String sourceKey = generateDamageSourceKey(damageSource);

        // Check immunity
        if (isPlayerImmune(playerId, sourceKey)) {
            event.setCanceled(true);
            return;
        }

        PlayerStatsAttachment statsAttachment = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
        if (statsAttachment == null) {
            return;
        }

        float vanillaDamage = event.getAmount();

        // Cancel vanilla damage
        event.setCanceled(true);

        // Convert to custom damage
        long customDamage = convertToCustomDamage(vanillaDamage, player);

        if (customDamage > 0) {
            // Apply custom damage
            long healthBefore = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);
            statsAttachment.consumeStat(IStatsData.StatType.HEALTH, customDamage);
            long healthAfter = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);

            long actualDamage = healthBefore - healthAfter;

            if (actualDamage > 0) {
                // Set immunity
                setPlayerImmunity(playerId, sourceKey, calculateImmunityDuration(customDamage));

                // Update vanilla health display
                updateVanillaHealthDisplay(player, statsAttachment);

                // Apply damage feedback
                applyDamageFeedback(player, actualDamage, statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH), damageSource);

                // Mark for sync
                statsAttachment.markDirty();

                ImmortalCultivation.LOGGER.debug("Applied {} custom damage to {} (vanilla: {})",
                    actualDamage, player.getName().getString(), vanillaDamage);
            }
        }
    }

    /**
     * Simple damage event handler with 1:1 conversion.
     *
     * @param event Damage event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        UUID playerId = player.getUUID();
        DamageSource damageSource = event.getSource();
        String sourceKey = generateDamageSourceKey(damageSource);

        // Check immunity
        if (isPlayerImmune(playerId, sourceKey)) {
            event.setCanceled(true);
            return;
        }

        PlayerStatsAttachment statsAttachment = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
        if (statsAttachment == null) {
            return;
        }

        float vanillaDamage = event.getAmount();

        // Cancel vanilla damage
        event.setCanceled(true);

        // Convert to custom damage with 1:1 scaling
        long customDamage = convertToCustomDamage(vanillaDamage, player);

        if (customDamage > 0) {
            // Apply custom damage
            long actualDamage = statsAttachment.consumeStat(IStatsData.StatType.HEALTH, customDamage);

            if (actualDamage > 0) {
                // Set immunity
                setPlayerImmunity(playerId, sourceKey, calculateImmunityDuration(customDamage));

                // Update vanilla health display
                updateVanillaHealthDisplay(player, statsAttachment);

                // Apply damage feedback
                applyDamageFeedback(player, actualDamage, statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH), damageSource);

                // Mark for sync
                statsAttachment.markDirty();

                ImmortalCultivation.LOGGER.debug("Applied {} custom damage to {} (vanilla: {})",
                    actualDamage, player.getName().getString(), vanillaDamage);
            }
        }
    }

    // === IMMUNITY TRACKING === //

    /**
     * Tracks immunity per player per damage source for sophisticated multi-enemy combat
     */
    private static final Map<UUID, Map<String, Long>> playerImmunityMap = new ConcurrentHashMap<>();

    /**
     * Tracks last known vanilla health values for protection system
     */
    private static final Map<UUID, Float> lastKnownVanillaHealth = new ConcurrentHashMap<>();

}
