package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerHealthAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * FIXED VERSION: Health Event Handler for NeoForge 1.21.1
 *
 * Key fixes:
 * 1. Only bypass vanilla damage when custom damage succeeds
 * 2. Add proper respawn handling to reset health states
 * 3. Better fallback behavior when attachment system fails
 * 4. Proper player context handling
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ICModHealthEventHandler {

    /**
     * FIXED: Only bypass vanilla damage when our custom system successfully handles it
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerIncomingDamage(LivingIncomingDamageEvent event) {
        // Only handle player damage
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only process on server side to avoid duplicate processing
        if (player.level().isClientSide()) {
            return;
        }

        // Get our custom health attachment
        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment == null) {
            // Fallback: if our system isn't available, let vanilla handle it
            ImmortalCultivation.LOGGER.warn("No health attachment found for player {}, using vanilla damage",
                player.getName().getString());
            return; // Let vanilla handle the damage normally
        }

        // Get the current damage amount from the container
        float originalDamage = event.getContainer().getNewDamage();

        if (originalDamage <= 0) {
            return; // No damage to process
        }

        // Convert vanilla damage to our modded damage system
        long moddedDamage = convertVanillaDamageToModded(originalDamage, event, player);

        // CRITICAL FIX: Try to apply damage through our custom system
        boolean damageApplied = applyCustomDamage(healthAttachment, moddedDamage, event, player);

        if (damageApplied) {
            // Custom damage was successfully applied
            ImmortalCultivation.LOGGER.debug("Applied {} modded damage to player {} (from {} vanilla damage)",
                moddedDamage, player.getName().getString(), originalDamage);

            // Set vanilla damage to 0 to prevent double damage
            event.getContainer().setNewDamage(0.0f);

            // Handle death if health reaches zero
            checkAndHandleDeath(player, healthAttachment);
        } else {
            // Custom damage failed - let vanilla handle it as fallback
            ImmortalCultivation.LOGGER.warn("Custom damage failed for player {}, falling back to vanilla damage",
                player.getName().getString());
            // Don't modify the damage container - let vanilla process normally
        }
    }

    /**
     * FIXED: Healing interception with better error handling
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment == null) {
            // Let vanilla handle healing if our system isn't available
            ImmortalCultivation.LOGGER.warn("No health attachment found for player {}, using vanilla healing",
                player.getName().getString());
            return;
        }

        // Cancel vanilla healing and handle through our system
        event.setCanceled(true);

        // Convert and apply healing through our system
        float vanillaHealing = event.getAmount();
        long moddedHealing = convertVanillaHealingToModded(vanillaHealing, event, player);

        long actualHealing = applyCustomHealing(healthAttachment, moddedHealing, player);

        if (actualHealing > 0) {
            ImmortalCultivation.LOGGER.debug("Applied {} modded healing to player {} (from {} vanilla healing)",
                actualHealing, player.getName().getString(), vanillaHealing);
        }
    }

    /**
     * FIXED: Player login synchronization with better error handling
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment == null) {
            ImmortalCultivation.LOGGER.error("No health attachment found for player {} on login!",
                player.getName().getString());
            return;
        }

        // Sync vanilla health to match modded health percentage
        synchronizeVanillaHealth(player, healthAttachment);

        // Force sync to client to ensure GUI displays correctly
        healthAttachment.forceSync();

        ImmortalCultivation.LOGGER.debug("Synchronized health for player {} on login: {}/{}",
            player.getName().getString(),
            healthAttachment.getCurrentHealth(),
            healthAttachment.getMaxHealth());
    }

    /**
     * NEW: Handle player respawn to fix attachment states
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("Player {} respawned, resetting health attachment state",
            player.getName().getString());

        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment != null) {
            // Reset health to full on respawn
            long maxHealth = calculateMaxHealthForPlayer(player);
            healthAttachment.setCurrentHealth(maxHealth);

            // Reset regeneration fraction
            healthAttachment.setRegenFraction(0.0);

            // Sync vanilla health bar to full
            player.setHealth(player.getMaxHealth());

            // Force sync to client
            healthAttachment.forceSync();

            ImmortalCultivation.LOGGER.info("Reset health for player {} on respawn: {}/{}",
                player.getName().getString(),
                healthAttachment.getCurrentHealth(),
                maxHealth);
        } else {
            ImmortalCultivation.LOGGER.error("No health attachment found for player {} on respawn!",
                player.getName().getString());
        }
    }

    // === DAMAGE CONVERSION AND APPLICATION ===

    private static long convertVanillaDamageToModded(float vanillaDamage, LivingIncomingDamageEvent event, Player player) {
        // Basic conversion: 1 vanilla damage = 1 modded damage
        long baseDamage = Math.round(vanillaDamage);

        // Apply cultivation-based damage reduction
        try {
            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                long maxHealth = healthAttachment.getMaxHealth();

                // Higher max health means more resistance to damage
                if (maxHealth > 100) {
                    float resistance = Math.min(0.5f, (maxHealth - 100) / 1000.0f);
                    baseDamage = Math.round(baseDamage * (1.0f - resistance));
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating damage reduction for player {}",
                player.getName().getString(), e);
        }

        return Math.max(1, baseDamage);
    }

    /**
     * FIXED: Custom damage application with better error handling
     */
    private static boolean applyCustomDamage(PlayerHealthAttachment healthAttachment, long damage,
                                             LivingIncomingDamageEvent event, Player player) {

        // Check for damage immunity or special conditions
        if (shouldPreventDamage(player, damage, event)) {
            return false;
        }

        try {
            // Apply the damage
            long oldHealth = healthAttachment.getCurrentHealth();
            boolean success = healthAttachment.consumeHealth(damage);

            if (success) {
                // Trigger custom damage effects
                onCustomDamageApplied(player, damage, oldHealth, healthAttachment.getCurrentHealth());
                return true;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying custom damage to player {}",
                player.getName().getString(), e);
        }

        return false;
    }

    /**
     * FIXED: Custom healing application
     */
    private static long applyCustomHealing(PlayerHealthAttachment healthAttachment, long healing, Player player) {
        try {
            return healthAttachment.addHealth(healing);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying custom healing to player {}",
                player.getName().getString(), e);
            return 0;
        }
    }

    private static long convertVanillaHealingToModded(float vanillaHealing, LivingHealEvent event, Player player) {
        long baseHealing = Math.round(vanillaHealing);

        // Apply cultivation bonuses
        try {
            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                float healingBonus = healthAttachment.getMaxHealth() / 100.0f;
                baseHealing = Math.round(baseHealing * (1.0f + healingBonus));
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating healing bonus for player {}",
                player.getName().getString(), e);
        }

        return baseHealing;
    }

    // === SYNCHRONIZATION UTILITIES ===

    private static void synchronizeVanillaHealth(Player player, PlayerHealthAttachment healthAttachment) {
        try {
            long currentModdedHealth = healthAttachment.getCurrentHealth();
            long maxModdedHealth = healthAttachment.getMaxHealth();

            if (maxModdedHealth <= 0) {
                return;
            }

            float healthPercentage = (float) currentModdedHealth / maxModdedHealth;
            float maxVanillaHealth = player.getMaxHealth();
            float targetVanillaHealth = maxVanillaHealth * healthPercentage;

            // Ensure we never set health to exactly 0 unless the player should be dead
            if (currentModdedHealth > 0) {
                targetVanillaHealth = Math.max(0.1f, targetVanillaHealth);
            }

            player.setHealth(targetVanillaHealth);

            ImmortalCultivation.LOGGER.debug("Synced vanilla health: {}% = {}/{} vanilla health",
                String.format("%.1f", healthPercentage * 100), targetVanillaHealth, maxVanillaHealth);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error synchronizing vanilla health for player {}",
                player.getName().getString(), e);
        }
    }

    // === CUSTOM DAMAGE LOGIC ===

    private static boolean shouldPreventDamage(Player player, long damage, LivingIncomingDamageEvent event) {
        if (damage <= 0) {
            return true;
        }

        // Add custom immunity checks here
        // Example: Check for special damage immunities based on cultivation

        return false;
    }

    private static void onCustomDamageApplied(Player player, long damage, long oldHealth, long newHealth) {
        long healthLost = oldHealth - newHealth;

        ImmortalCultivation.LOGGER.debug("Player {} lost {} health from {} damage ({}->{})",
            player.getName().getString(), healthLost, damage, oldHealth, newHealth);

        if (healthLost > 50) {
            ImmortalCultivation.LOGGER.info("Player {} took major damage!", player.getName().getString());
        }
    }

    private static void checkAndHandleDeath(Player player, PlayerHealthAttachment healthAttachment) {
        if (healthAttachment.getCurrentHealth() <= 0) {
            ImmortalCultivation.LOGGER.info("Player {} has died (custom health reached zero)",
                player.getName().getString());

            // Set vanilla health to 0 to trigger death
            player.setHealth(0.0f);
        }
    }

    /**
     * NEW: Helper method to safely calculate max health for a player
     */
    private static long calculateMaxHealthForPlayer(ServerPlayer player) {
        try {
            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                return healthAttachment.getMaxHealth();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating max health for player {}",
                player.getName().getString(), e);
        }

        return 20; // Default Minecraft health
    }
}