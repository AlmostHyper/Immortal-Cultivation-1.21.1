package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerResourceAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ENHANCED HEALTH PROTECTION SYSTEM
 *
 * This system ensures vanilla health NEVER decreases unless authorized by the custom health system.
 * Vanilla health becomes purely a visual indicator scaled to match custom health percentage.
 *
 * Key Features:
 * - Proactive vanilla health protection via constant monitoring
 * - Complete interception of ALL damage and healing
 * - Vanilla health death only when custom health authorizes it
 * - Seamless compatibility with vanilla systems
 */
public class ICModHealthHandler {

    // Track player vanilla health to detect unauthorized changes
    private static final Map<UUID, Float> lastKnownVanillaHealth = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastKnownCustomHealth = new ConcurrentHashMap<>();

    // Track damage immunity for proper feedback
    private static final Map<UUID, Float> lastAppliedDamage = new ConcurrentHashMap<>();

    // Emergency protection flags
    private static final Map<UUID, Boolean> emergencyProtectionActive = new ConcurrentHashMap<>();

    private static final float MINIMUM_VANILLA_HEALTH = 0.1f; // Never let vanilla health go below this when alive
    private static final int HEALTH_CHECK_INTERVAL = 5; // Check every 5 ticks for unauthorized changes

    // === ABSOLUTE DAMAGE INTERCEPTION ===

    /**
     * CRITICAL: Intercept ALL damage with HIGHEST priority.
     * This completely prevents vanilla damage from ever being applied.
     *
     * NOTE: ICModImmunityHandler runs first to filter out immune damage,
     * so any damage that reaches here should be processed.
     */
    @SubscribeEvent(priority = EventPriority.HIGH) // Changed from HIGHEST to HIGH (immunity handler runs at HIGHEST)
    public static void interceptAllDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        PlayerResourceAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (healthAttachment == null) {
            ImmortalCultivation.LOGGER.warn("No health attachment found for player {} - allowing vanilla damage as fallback",
                player.getName().getString());
            return;
        }

        float originalDamage = event.getContainer().getNewDamage();

        // ABSOLUTE CANCELLATION: Vanilla damage is NEVER applied
        event.getContainer().setNewDamage(0.0f);

        ImmortalCultivation.LOGGER.debug("Intercepted {} vanilla damage for player {}",
            originalDamage, player.getName().getString());

        // REMOVED: Immunity check - ICModImmunityHandler handles all immunity logic
        // If damage reaches here, it means ICModImmunityHandler allowed it through

        // Convert vanilla damage to custom damage with proper scaling
        long customDamage = calculateCustomDamage(originalDamage, player, event.getSource());

        if (customDamage > 0) {
            // Apply damage to custom health system
            long healthBefore = healthAttachment.getCurrentHealth();
            healthAttachment.consumeHealth(customDamage);
            long healthAfter = healthAttachment.getCurrentHealth();

            long actualDamage = healthBefore - healthAfter;

            if (actualDamage > 0) {
                // Set minimal vanilla immunity for visual feedback only
                // ICModImmunityHandler manages the real immunity logic
                player.invulnerableTime = isEnvironmentalDamage(event.getSource()) ? 10 : 5;
                lastAppliedDamage.put(player.getUUID(), originalDamage);

                // Apply visual damage feedback
                applyDamageFeedback(player, actualDamage, event.getSource());

                // Damage armor
                damagePlayerArmor(player, originalDamage, event.getSource());

                // Immediately update vanilla health to match custom health
                updateVanillaHealthFromCustom(player, healthAttachment);

                ImmortalCultivation.LOGGER.debug("Applied {} custom damage to player {} (from {} vanilla damage)",
                    actualDamage, player.getName().getString(), originalDamage);
            }
        }
    }

    // === ABSOLUTE HEALING INTERCEPTION ===

    /**
     * Intercept ALL healing to apply to custom health system instead
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interceptAllHealing(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        PlayerResourceAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (healthAttachment == null) {
            return;
        }

        float originalHealing = event.getAmount();

        // Cancel vanilla healing completely
        event.setAmount(0.0f);

        if (originalHealing > 0) {
            // Convert to custom healing with bonuses
            long customHealing = calculateCustomHealing(originalHealing, player);

            if (customHealing > 0) {
                long actualHealing = healthAttachment.addHealth(customHealing);

                if (actualHealing > 0) {
                    // Update vanilla health to match
                    updateVanillaHealthFromCustom(player, healthAttachment);

                    ImmortalCultivation.LOGGER.debug("Applied {} custom healing to player {} (from {} vanilla healing)",
                        actualHealing, player.getName().getString(), originalHealing);
                }
            }
        }
    }

    // === PROACTIVE VANILLA HEALTH PROTECTION ===

    /**
     * Constantly monitor and protect vanilla health from unauthorized changes.
     * This is the key to preventing vanilla health from ever decreasing unexpectedly.
     */
    @SubscribeEvent
    public static void protectVanillaHealth(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        PlayerResourceAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (healthAttachment == null) {
            return;
        }

        UUID playerId = player.getUUID();

        // Check for unauthorized vanilla health changes every few ticks
        if (player.tickCount % HEALTH_CHECK_INTERVAL == 0) {
            float currentVanillaHealth = player.getHealth();
            long currentCustomHealth = healthAttachment.getCurrentHealth();

            Float lastVanillaHealth = lastKnownVanillaHealth.get(playerId);
            Long lastCustomHealth = lastKnownCustomHealth.get(playerId);

            if (lastVanillaHealth != null && lastCustomHealth != null) {
                // Check if vanilla health decreased without authorization
                if (currentVanillaHealth < lastVanillaHealth && currentCustomHealth == lastCustomHealth) {
                    // UNAUTHORIZED VANILLA HEALTH REDUCTION DETECTED
                    ImmortalCultivation.LOGGER.warn("Detected unauthorized vanilla health reduction for player {} - restoring from {} to {}",
                        player.getName().getString(), currentVanillaHealth, lastVanillaHealth);

                    // Restore vanilla health immediately
                    player.setHealth(lastVanillaHealth);
                    emergencyProtectionActive.put(playerId, true);
                }

                // Check if vanilla health is too low compared to custom health
                else if (currentCustomHealth > 0 && currentVanillaHealth < MINIMUM_VANILLA_HEALTH) {
                    ImmortalCultivation.LOGGER.warn("Vanilla health too low for living player {} - correcting",
                        player.getName().getString());

                    updateVanillaHealthFromCustom(player, healthAttachment);
                    emergencyProtectionActive.put(playerId, true);
                }
            }

            // Update tracking values only if no emergency protection was needed
            if (!emergencyProtectionActive.getOrDefault(playerId, false)) {
                lastKnownVanillaHealth.put(playerId, player.getHealth());
                lastKnownCustomHealth.put(playerId, currentCustomHealth);
            } else {
                // Reset emergency flag after correction
                emergencyProtectionActive.put(playerId, false);
            }
        }

        // Ensure vanilla health is always properly scaled to custom health
        if (player.tickCount % 20 == 0) { // Every second
            long customHealth = healthAttachment.getCurrentHealth();

            // Only allow vanilla health to be 0 if custom health is 0 (player should be dead)
            if (customHealth <= 0) {
                if (player.getHealth() > 0) {
                    // Custom system says player should be dead
                    player.setHealth(0.0f);
                    ImmortalCultivation.LOGGER.info("Custom health system authorized death for player {}",
                        player.getName().getString());
                }
            } else {
                // Player should be alive - ensure vanilla health reflects custom health percentage
                float expectedVanillaHealth = calculateVanillaHealthFromCustom(healthAttachment, player);
                float currentVanillaHealth = player.getHealth();

                // Allow small variance to avoid constant corrections
                if (Math.abs(currentVanillaHealth - expectedVanillaHealth) > 0.5f) {
                    player.setHealth(expectedVanillaHealth);
                    ImmortalCultivation.LOGGER.debug("Corrected vanilla health for player {} to {} (should be {} based on custom health)",
                        player.getName().getString(), expectedVanillaHealth, currentVanillaHealth);
                }
            }
        }
    }

    // === PLAYER LIFECYCLE PROTECTION ===

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerResourceAttachment healthAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (healthAttachment != null) {
                // Immediately sync vanilla health to custom health
                updateVanillaHealthFromCustom(serverPlayer, healthAttachment);

                // Initialize tracking
                UUID playerId = serverPlayer.getUUID();
                lastKnownVanillaHealth.put(playerId, serverPlayer.getHealth());
                lastKnownCustomHealth.put(playerId, healthAttachment.getCurrentHealth());

                ImmortalCultivation.LOGGER.info("Initialized health protection for player {}",
                    serverPlayer.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerResourceAttachment healthAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (healthAttachment != null) {
                // Respawn should restore custom health to full
                healthAttachment.setCurrentHealth(healthAttachment.getMaxHealth());

                // Update vanilla health to match
                updateVanillaHealthFromCustom(serverPlayer, healthAttachment);

                // Reset tracking
                UUID playerId = serverPlayer.getUUID();
                lastKnownVanillaHealth.put(playerId, serverPlayer.getHealth());
                lastKnownCustomHealth.put(playerId, healthAttachment.getCurrentHealth());
                emergencyProtectionActive.remove(playerId);
                lastAppliedDamage.remove(playerId);

                ImmortalCultivation.LOGGER.info("Reset health protection for respawned player {}",
                    serverPlayer.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Clean up tracking maps
        UUID playerId = event.getEntity().getUUID();
        lastKnownVanillaHealth.remove(playerId);
        lastKnownCustomHealth.remove(playerId);
        emergencyProtectionActive.remove(playerId);
        lastAppliedDamage.remove(playerId);
    }

    // === UTILITY METHODS ===

    /**
     * Update vanilla health to properly reflect custom health percentage
     */
    private static void updateVanillaHealthFromCustom(Player player, PlayerResourceAttachment healthAttachment) {
        long customHealth = healthAttachment.getCurrentHealth();
        long maxCustomHealth = healthAttachment.getMaxHealth();

        if (maxCustomHealth <= 0) {
            return;
        }

        if (customHealth <= 0) {
            // Player should be dead
            player.setHealth(0.0f);
        } else {
            // Scale vanilla health based on custom health percentage
            float healthPercentage = (float) customHealth / maxCustomHealth;
            float maxVanillaHealth = player.getMaxHealth();

            // Scale between minimum safe health and max health
            float targetVanillaHealth = MINIMUM_VANILLA_HEALTH +
                (maxVanillaHealth - MINIMUM_VANILLA_HEALTH) * healthPercentage;

            player.setHealth(targetVanillaHealth);
        }

        // Update tracking
        UUID playerId = player.getUUID();
        lastKnownVanillaHealth.put(playerId, player.getHealth());
        lastKnownCustomHealth.put(playerId, customHealth);
    }

    /**
     * Calculate what vanilla health should be based on custom health
     */
    private static float calculateVanillaHealthFromCustom(PlayerResourceAttachment healthAttachment, Player player) {
        long customHealth = healthAttachment.getCurrentHealth();
        long maxCustomHealth = healthAttachment.getMaxHealth();

        if (maxCustomHealth <= 0 || customHealth <= 0) {
            return 0.0f;
        }

        float healthPercentage = (float) customHealth / maxCustomHealth;
        float maxVanillaHealth = player.getMaxHealth();

        return MINIMUM_VANILLA_HEALTH + (maxVanillaHealth - MINIMUM_VANILLA_HEALTH) * healthPercentage;
    }

    /**
     * Convert vanilla damage to custom damage with proper scaling and armor calculations
     */
    private static long calculateCustomDamage(float vanillaDamage, Player player, DamageSource damageSource) {
        // Apply armor reduction to vanilla damage first
        float damageAfterArmor = calculateDamageAfterArmor(vanillaDamage, player, damageSource);

        // Convert to custom damage (you can adjust this scaling as needed)
        long baseDamage = Math.round(damageAfterArmor);

        // Apply cultivation-based damage reduction here if needed
        // For now, use 1:1 scaling but you can enhance this

        return Math.max(0, baseDamage);
    }

    /**
     * Convert vanilla healing to custom healing with cultivation bonuses
     */
    private static long calculateCustomHealing(float vanillaHealing, Player player) {
        long baseHealing = Math.round(vanillaHealing);

        // Apply cultivation bonuses here
        // You can access other attachments to calculate bonuses

        return Math.max(0, baseHealing);
    }

    /**
     * Calculate damage after armor and enchantments (vanilla formula)
     */
    private static float calculateDamageAfterArmor(float damage, Player player, DamageSource damageSource) {
        if (damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return damage;
        }

        // Calculate total armor value
        int totalArmor = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armorPiece = player.getItemBySlot(slot);
                if (!armorPiece.isEmpty() && armorPiece.getItem() instanceof net.minecraft.world.item.ArmorItem armorItem) {
                    totalArmor += armorItem.getDefense();
                }
            }
        }

        // Apply vanilla armor formula: damage * (1 - min(20, max(armor/5, armor - damage/2)) / 25)
        float armorValue = Math.min(20, Math.max(totalArmor / 5.0f, totalArmor - damage / 2.0f));
        float damageReduction = armorValue / 25.0f;

        return damage * (1.0f - damageReduction);
    }

    /**
     * Damage player armor when taking damage
     */
    private static void damagePlayerArmor(Player player, float damage, DamageSource damageSource) {
        if (damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        // Damage armor pieces
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armorPiece = player.getItemBySlot(slot);
                if (!armorPiece.isEmpty()) {
                    // Calculate durability damage (simplified)
                    int durabilityDamage = Math.max(1, (int) (damage / 4.0f));
                    armorPiece.hurtAndBreak(durabilityDamage, player, slot);
                }
            }
        }
    }

    /**
     * Apply visual damage feedback without affecting health
     */
    private static void applyDamageFeedback(Player player, long damage, DamageSource damageSource) {
        // Apply hurt animation
        player.hurtTime = 10;
        player.hurtDuration = 10;

        // Set hurt direction
        try {
            java.lang.reflect.Field hurtDirField = Player.class.getDeclaredField("hurtDir");
            hurtDirField.setAccessible(true);
            hurtDirField.setFloat(player, (float) (Math.random() * Math.PI * 2));
        } catch (Exception e) {
            // Ignore reflection failures
        }

        // Add knockback if appropriate
        if (!damageSource.is(net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK)) {
            // Apply small knockback based on damage
            // This would need to be implemented based on your damage feedback system
        }
    }

    /**
     * Check if damage source is environmental (shorter immunity frames)
     */
    private static boolean isEnvironmentalDamage(DamageSource damageSource) {
        return damageSource.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ||
            damageSource.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING) ||
            damageSource.getMsgId().equals("lava") ||
            damageSource.getMsgId().equals("inFire") ||
            damageSource.getMsgId().equals("onFire") ||
            damageSource.getMsgId().equals("sweetBerryBush") ||
            damageSource.getMsgId().equals("cactus") ||
            damageSource.getMsgId().equals("hotFloor") ||
            damageSource.getMsgId().equals("magma");
    }

    /**
     * Emergency method to force correct vanilla health for a player
     */
    public static void emergencyHealthCorrection(Player player) {
        PlayerResourceAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (healthAttachment != null) {
            updateVanillaHealthFromCustom(player, healthAttachment);
            emergencyProtectionActive.put(player.getUUID(), false);
            ImmortalCultivation.LOGGER.info("Emergency health correction applied for player {}",
                player.getName().getString());
        }
    }
}