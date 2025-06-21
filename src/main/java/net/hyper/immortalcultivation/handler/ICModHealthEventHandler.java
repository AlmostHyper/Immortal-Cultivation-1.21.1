package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerHealthAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * UPDATED FOR NEOFORGE 1.21.1: Advanced Damage System Integration
 *
 * This updated version demonstrates how to work with NeoForge 1.21.1's new
 * damage event system, which provides much more sophisticated control over
 * how damage is calculated and applied.
 *
 * Key Changes from Previous Versions:
 * 1. Bus.NEOFORGE → Bus.GAME (naming update)
 * 2. LivingDamageEvent → LivingIncomingDamageEvent (early pipeline event)
 * 3. Simple damage amounts → DamageContainer system
 * 4. setCanceled() → event cancellation through container modification
 * 5. getAmount() → getDamageContainer().getNewDamage()
 *
 * Learning Opportunity: This shows how major APIs evolve to provide better
 * functionality. The new system is more complex but gives us much finer
 * control over damage processing.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ICModHealthEventHandler {

    /**
     * UPDATED: Damage Interception Using New NeoForge 1.21.1 API
     *
     * The new damage system works fundamentally differently:
     * - LivingIncomingDamageEvent fires early in the damage pipeline
     * - We use DamageContainer to modify damage amounts and behavior
     * - Instead of canceling events, we set damage to 0 or modify reductions
     * - Much more granular control over different damage components
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
            return;
        }

        // CRITICAL INSIGHT: Instead of canceling the event, we intercept and modify
        // the damage through the DamageContainer system

        // Get the current damage amount from the container
        float originalDamage = event.getContainer().getNewDamage();

        // Convert vanilla damage to our modded damage system
        long moddedDamage = convertVanillaDamageToModded(originalDamage, event, player);

        // Apply damage through our custom system
        boolean damageApplied = applyCustomDamage(healthAttachment, moddedDamage, event, player);

        if (damageApplied) {
            ImmortalCultivation.LOGGER.debug("Applied {} modded damage to player {} (from {} vanilla damage)",
                moddedDamage, player.getName().getString(), originalDamage);

            // IMPORTANT: Set vanilla damage to 0 to prevent double damage
            // This is the new way to "cancel" damage - we handle it ourselves
            // and tell the vanilla system there's no damage left to apply
            event.getContainer().setNewDamage(0.0f);

            // Handle death if health reaches zero
            checkAndHandleDeath(player, healthAttachment);
        } else {
            // If our system blocked the damage, also set vanilla damage to 0
            event.getContainer().setNewDamage(0.0f);
            ImmortalCultivation.LOGGER.debug("Damage blocked for player {}",
                player.getName().getString());
        }
    }

    /**
     * UPDATED: Healing Interception for NeoForge 1.21.1
     *
     * The healing event system appears to be less changed, but we still need
     * to use the updated bus reference and ensure compatibility.
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
            return; // Let vanilla handle it
        }

        // The healing event still supports the traditional cancel approach
        event.setCanceled(true);

        // Convert and apply healing through our system
        float vanillaHealing = event.getAmount();
        long moddedHealing = convertVanillaHealingToModded(vanillaHealing, event, player);

        long actualHealing = healthAttachment.addHealth(moddedHealing);

        if (actualHealing > 0) {
            ImmortalCultivation.LOGGER.debug("Applied {} modded healing to player {} (from {} vanilla healing)",
                actualHealing, player.getName().getString(), vanillaHealing);
        }
    }

    /**
     * Player login synchronization - unchanged from previous version
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment == null) {
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

    // === DAMAGE CONVERSION AND APPLICATION ===

    /**
     * UPDATED: Damage conversion with awareness of new damage system
     *
     * The conversion logic is similar, but now we receive the damage from
     * the DamageContainer which has already had some processing applied.
     */
    private static long convertVanillaDamageToModded(float vanillaDamage, LivingIncomingDamageEvent event, Player player) {
        // Basic conversion: 1 vanilla damage = 1 modded damage
        long baseDamage = Math.round(vanillaDamage);

        // Example: Reduce damage based on cultivation level
        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment != null) {
            long maxHealth = healthAttachment.getMaxHealth();

            // Higher max health means more resistance to damage
            if (maxHealth > 100) {
                float resistance = Math.min(0.5f, (maxHealth - 100) / 1000.0f);
                baseDamage = Math.round(baseDamage * (1.0f - resistance));
            }
        }

        // NEW: We could also examine the DamageContainer to see what
        // modifications have already been applied by vanilla or other mods
        // Example: event.getDamageContainer().getOriginalDamage() vs getNewDamage()

        return Math.max(1, baseDamage);
    }

    /**
     * Custom damage application - logic unchanged but context updated
     */
    private static boolean applyCustomDamage(PlayerHealthAttachment healthAttachment, long damage,
                                             LivingIncomingDamageEvent event, Player player) {

        // Check for damage immunity or special conditions
        if (shouldPreventDamage(player, damage, event)) {
            return false;
        }

        // Apply the damage
        long oldHealth = healthAttachment.getCurrentHealth();
        boolean success = healthAttachment.consumeHealth(damage);

        if (success) {
            // Trigger custom damage effects
            onCustomDamageApplied(player, damage, oldHealth, healthAttachment.getCurrentHealth());
        }

        return success;
    }

    /**
     * Healing conversion - logic largely unchanged
     */
    private static long convertVanillaHealingToModded(float vanillaHealing, LivingHealEvent event, Player player) {
        long baseHealing = Math.round(vanillaHealing);

        // Apply cultivation bonuses
        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment != null) {
            float healingBonus = healthAttachment.getMaxHealth() / 100.0f;
            baseHealing = Math.round(baseHealing * (1.0f + healingBonus));
        }

        return baseHealing;
    }

    // === SYNCHRONIZATION UTILITIES ===

    /**
     * Health bar synchronization - unchanged from previous version
     */
    private static void synchronizeVanillaHealth(Player player, PlayerHealthAttachment healthAttachment) {
        long currentModdedHealth = healthAttachment.getCurrentHealth();
        long maxModdedHealth = healthAttachment.getMaxHealth();

        if (maxModdedHealth <= 0) {
            return;
        }

        float healthPercentage = (float) currentModdedHealth / maxModdedHealth;
        float maxVanillaHealth = player.getMaxHealth();
        float targetVanillaHealth = maxVanillaHealth * healthPercentage;

        player.setHealth(Math.max(0.1f, targetVanillaHealth));

        ImmortalCultivation.LOGGER.debug("Synced vanilla health: {}% = {}/{} vanilla health",
            String.format("%.1f", healthPercentage * 100), targetVanillaHealth, maxVanillaHealth);
    }

    // === CUSTOM DAMAGE LOGIC ===

    /**
     * UPDATED: Damage prevention checks with new event type
     */
    private static boolean shouldPreventDamage(Player player, long damage, LivingIncomingDamageEvent event) {
        if (damage <= 0) {
            return true;
        }

        // NEW POSSIBILITIES: With the DamageContainer system, we could check
        // for much more sophisticated conditions:
        // - What types of damage reduction have already been applied
        // - Whether this damage bypasses armor, absorption, etc.
        // - Custom immunity types based on damage source characteristics

        // Example: Check damage source for special immunities
        // if (event.getDamageContainer().getDamageSource().is(DamageTypeTags.IS_FIRE)) {
        //     // Apply fire immunity logic
        // }

        return false;
    }

    /**
     * Custom damage effects - unchanged
     */
    private static void onCustomDamageApplied(Player player, long damage, long oldHealth, long newHealth) {
        long healthLost = oldHealth - newHealth;

        ImmortalCultivation.LOGGER.debug("Player {} lost {} health from {} damage ({}->{})",
            player.getName().getString(), healthLost, damage, oldHealth, newHealth);

        if (healthLost > 50) {
            ImmortalCultivation.LOGGER.info("Player {} took major damage!", player.getName().getString());
        }
    }

    /**
     * Death handling - unchanged but important
     */
    private static void checkAndHandleDeath(Player player, PlayerHealthAttachment healthAttachment) {
        if (healthAttachment.getCurrentHealth() <= 0) {
            ImmortalCultivation.LOGGER.info("Player {} has died (custom health reached zero)",
                player.getName().getString());

            // Set vanilla health to 0 to trigger death
            player.setHealth(0.0f);
        }
    }
}