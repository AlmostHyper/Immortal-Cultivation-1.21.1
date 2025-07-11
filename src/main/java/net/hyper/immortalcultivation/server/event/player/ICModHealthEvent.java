package net.hyper.immortalcultivation.server.event.player;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerResourceAttachment;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UNIFIED CULTIVATION HEALTH AND IMMUNITY SYSTEM
 *
 * This consolidated event handler manages all health-related functionality for the cultivation system,
 * combining damage interception, immunity tracking, and health synchronization into a single cohesive system.
 *
 * Key Features:
 * - Complete vanilla damage interception and conversion to custom damage
 * - Sophisticated per-source immunity system for multi-enemy combat
 * - Vanilla health protection and synchronization as visual indicator
 * - Integration with ICModTickEvent for performance optimization
 * - Network synchronization through NetworkSyncManager and dirty flags
 * - Comprehensive armor durability and knockback calculations
 * - Emergency protection systems for edge cases
 *
 * Architecture:
 * - Immunity processing at HIGHEST priority (blocks immune damage completely)
 * - Damage interception at HIGH priority (processes allowed damage)
 * - Health protection at NORMAL priority (prevents unauthorized changes)
 * - Integrates with dirty flag system for efficient network sync
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class ICModHealthEvent {

    // === IMMUNITY TRACKING SYSTEM ===

    /**
     * Tracks immunity per player per damage source for sophisticated multi-enemy combat.
     * Map structure: PlayerUUID -> DamageSourceKey -> LastDamageTime
     */
    private static final Map<UUID, Map<String, Long>> playerSourceImmunity = new ConcurrentHashMap<>();

    // Immunity durations (in ticks)
    private static final long ENTITY_DAMAGE_IMMUNITY_TICKS = 10;           // 0.5 seconds per entity
    private static final long ENVIRONMENTAL_DAMAGE_IMMUNITY_TICKS = 20;    // 1 second for environmental
    private static final long RAPID_ENVIRONMENTAL_IMMUNITY_TICKS = 40;     // 2 seconds for rapid sources
    private static final long GLOBAL_MIN_DAMAGE_INTERVAL = 2;             // 0.1 seconds minimum

    // === HEALTH PROTECTION SYSTEM ===

    /**
     * Tracks vanilla health states to detect unauthorized changes.
     * This system ensures vanilla health never decreases without custom system authorization.
     */
    private static final Map<UUID, Float> lastKnownVanillaHealth = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastKnownCustomHealth = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastProcessedDamageTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> emergencyProtectionActive = new ConcurrentHashMap<>();

    // Health protection constants
    private static final float MINIMUM_VANILLA_HEALTH = 0.1f;
    private static final int HEALTH_CHECK_INTERVAL = 5;  // Check every 5 ticks
    private static final long MAX_TICK_TIME_NANOS = 1_000_000;  // 1ms max processing time

    // === PRIMARY DAMAGE IMMUNITY CHECK ===

    /**
     * Primary immunity check that runs before all damage processing.
     * This determines if damage should be blocked entirely based on per-source immunity tracking.
     *
     * If damage is blocked here, no other damage processing occurs, providing clean separation
     * between immune and processable damage.
     *
     * @param event The incoming damage event containing damage source and amount information
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void checkDamageImmunity(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        // Check if player is immune to this specific damage source
        if (isImmuneToSource(player, event.getSource())) {
            ImmortalCultivation.LOGGER.debug("Player {} is immune to damage from source: {} - blocking damage",
                player.getName().getString(), event.getSource().getMsgId());

            // Completely cancel the damage event - no further processing occurs
            event.setCanceled(true);
            return;
        }

        // If damage is allowed, set immunity for this source and continue to damage processing
        setSourceImmunity(player, event.getSource());

        ImmortalCultivation.LOGGER.debug("Player {} can take damage from source: {} - setting immunity",
            player.getName().getString(), event.getSource().getMsgId());
    }

    // === DAMAGE INTERCEPTION AND PROCESSING ===

    /**
     * Intercepts all non-immune damage and converts it to the custom health system.
     * This completely prevents vanilla damage from being applied while ensuring proper
     * custom damage calculation, armor interaction, and feedback effects.
     *
     * Runs at HIGH priority after immunity checking but before any other damage handlers.
     *
     * @param event The damage event containing vanilla damage information
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interceptAndProcessDamage(LivingIncomingDamageEvent event) {
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

        // Convert vanilla damage to custom damage with proper scaling and armor calculation
        long customDamage = calculateCustomDamage(originalDamage, player, event.getSource());

        if (customDamage > 0) {
            // Apply damage to custom health system
            long healthBefore = healthAttachment.getCurrentHealth();
            healthAttachment.consumeHealth(customDamage);
            long healthAfter = healthAttachment.getCurrentHealth();

            long actualDamage = healthBefore - healthAfter;

            if (actualDamage > 0) {
                // Mark that we've legitimately processed damage for protection system
                lastProcessedDamageTime.put(player.getUUID(), player.level().getGameTime());

                // Apply visual and mechanical effects
                applyDamageEffects(player, event.getSource(), originalDamage, actualDamage);

                // CRITICAL: Force immediate network sync after any health change
                forceImmediateHealthSync((ServerPlayer) player, healthAttachment);

                ImmortalCultivation.LOGGER.debug("Applied {} custom damage to player {} (vanilla: {})",
                    actualDamage, player.getName().getString(), originalDamage);
            }
        }
    }

    // === HEALING INTERCEPTION ===

    /**
     * Intercepts vanilla healing and converts it to custom healing with potential cultivation bonuses.
     * This ensures that all healing goes through the custom system for consistency and cultivation integration.
     *
     * @param event The healing event containing vanilla healing information
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interceptHealing(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        PlayerResourceAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (healthAttachment == null) {
            return;
        }

        float originalHealing = event.getAmount();

        // Cancel vanilla healing
        event.setAmount(0.0f);

        // Convert to custom healing with potential bonuses
        long customHealing = calculateCustomHealing(originalHealing, player);

        if (customHealing > 0) {
            long healthBefore = healthAttachment.getCurrentHealth();
            healthAttachment.addHealth(customHealing);
            long healthAfter = healthAttachment.getCurrentHealth();

            long actualHealing = healthAfter - healthBefore;

            if (actualHealing > 0) {
                // Mark as dirty for network sync
                healthAttachment.markDirty();

                // Update vanilla health to match
                updateVanillaHealthFromCustom(player, healthAttachment);

                ImmortalCultivation.LOGGER.debug("Applied {} custom healing to player {} (vanilla: {})",
                    actualHealing, player.getName().getString(), originalHealing);
            }
        }
    }

    // === HEALTH PROTECTION SYSTEM ===

    /**
     * Proactive health protection system that monitors and corrects unauthorized vanilla health changes.
     * This runs during player ticks to ensure vanilla health never decreases without custom system authorization.
     *
     * Integrates with ICModTickEvent intervals for performance optimization.
     *
     * @param event The player tick event
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void protectVanillaHealth(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        // Use staggered checking for performance (integrates with ICModTickEvent patterns)
        int playerOffset = Math.abs(player.getUUID().hashCode()) % HEALTH_CHECK_INTERVAL;
        if ((player.tickCount + playerOffset) % HEALTH_CHECK_INTERVAL != 0) {
            return;
        }

        long startTime = System.nanoTime();

        PlayerResourceAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (healthAttachment == null) {
            return;
        }

        UUID playerId = player.getUUID();
        float currentVanillaHealth = player.getHealth();
        long currentCustomHealth = healthAttachment.getCurrentHealth();

        Float lastVanillaHealth = lastKnownVanillaHealth.get(playerId);
        Long lastCustomHealth = lastKnownCustomHealth.get(playerId);

        if (lastVanillaHealth != null && lastCustomHealth != null) {
            // Check for unauthorized vanilla health changes
            boolean vanillaHealthDecreased = currentVanillaHealth < lastVanillaHealth - 0.01f;
            boolean customHealthUnchanged = Math.abs(currentCustomHealth - lastCustomHealth) < 1;
            boolean recentlyProcessedDamage = wasRecentlyProcessed(playerId, player.level().getGameTime());

            if (vanillaHealthDecreased && customHealthUnchanged && !recentlyProcessedDamage) {
                // Unauthorized change detected - restore vanilla health
                float targetVanillaHealth = calculateTargetVanillaHealth(healthAttachment);
                player.setHealth(targetVanillaHealth);

                emergencyProtectionActive.put(playerId, true);

                ImmortalCultivation.LOGGER.warn("Corrected unauthorized vanilla health decrease for player {} ({} -> {})",
                    player.getName().getString(), currentVanillaHealth, targetVanillaHealth);
            } else if (emergencyProtectionActive.getOrDefault(playerId, false)) {
                // Clear emergency protection if no longer needed
                emergencyProtectionActive.remove(playerId);
            }
        }

        // Update tracking
        lastKnownVanillaHealth.put(playerId, player.getHealth());
        lastKnownCustomHealth.put(playerId, currentCustomHealth);

        // Performance monitoring
        long processingTime = System.nanoTime() - startTime;
        if (processingTime > MAX_TICK_TIME_NANOS) {
            ImmortalCultivation.LOGGER.warn("Health protection took {}ms for player {} (exceeds 1ms threshold)",
                processingTime / 1_000_000.0, player.getName().getString());
        }
    }

    // === PLAYER LIFECYCLE EVENTS ===

    /**
     * Handles player login to ensure proper health system initialization and sync.
     * Integrates with the existing login system and NetworkSyncManager.
     *
     * @param event The player login event
     */
    @SubscribeEvent(priority = EventPriority.LOW)  // Run after other login initialization
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerResourceAttachment healthAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (healthAttachment != null) {
            // Ensure vanilla health matches custom health
            updateVanillaHealthFromCustom(serverPlayer, healthAttachment);

            // Initialize tracking
            UUID playerId = serverPlayer.getUUID();
            lastKnownVanillaHealth.put(playerId, serverPlayer.getHealth());
            lastKnownCustomHealth.put(playerId, healthAttachment.getCurrentHealth());

            // Clear any emergency states
            emergencyProtectionActive.remove(playerId);

            ImmortalCultivation.LOGGER.debug("Initialized health protection for player {}",
                serverPlayer.getName().getString());
        }
    }

    /**
     * Handles player respawn to restore health and reset protection systems.
     *
     * @param event The player respawn event
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerResourceAttachment healthAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (healthAttachment != null) {
            // Respawn should restore custom health to full
            healthAttachment.setCurrentHealth(healthAttachment.getMaxHealth());
            healthAttachment.markDirty();  // Ensure sync

            // Update vanilla health to match
            updateVanillaHealthFromCustom(serverPlayer, healthAttachment);

            // Reset all tracking
            UUID playerId = serverPlayer.getUUID();
            lastKnownVanillaHealth.put(playerId, serverPlayer.getHealth());
            lastKnownCustomHealth.put(playerId, healthAttachment.getCurrentHealth());
            emergencyProtectionActive.remove(playerId);
            lastProcessedDamageTime.remove(playerId);

            ImmortalCultivation.LOGGER.info("Reset health protection for respawned player {}",
                serverPlayer.getName().getString());
        }
    }

    /**
     * Handles player logout cleanup to prevent memory leaks.
     *
     * @param event The player logout event
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();

        // Clean up all tracking maps
        lastKnownVanillaHealth.remove(playerId);
        lastKnownCustomHealth.remove(playerId);
        emergencyProtectionActive.remove(playerId);
        lastProcessedDamageTime.remove(playerId);
        playerSourceImmunity.remove(playerId);

        ImmortalCultivation.LOGGER.debug("Cleaned up health tracking for player {}",
            event.getEntity().getName().getString());
    }

    // === IMMUNITY SYSTEM METHODS ===

    /**
     * Checks if a player is immune to damage from a specific source based on recent damage history.
     * Uses sophisticated per-source tracking to enable multi-enemy combat while preventing spam.
     *
     * @param player The player being damaged
     * @param damageSource The source of the damage
     * @return true if the player is immune to this damage source
     */
    private static boolean isImmuneToSource(Player player, DamageSource damageSource) {
        UUID playerId = player.getUUID();
        Map<String, Long> sourceImmunity = playerSourceImmunity.get(playerId);

        if (sourceImmunity == null) {
            return false;
        }

        String sourceKey = createSourceKey(damageSource);
        Long lastDamageTime = sourceImmunity.get(sourceKey);

        if (lastDamageTime == null) {
            return false;
        }

        long currentTime = player.level().getGameTime();
        long timeSinceLastDamage = currentTime - lastDamageTime;
        long immunityDuration = getImmunityDuration(damageSource);

        boolean isImmune = timeSinceLastDamage < immunityDuration;

        if (isImmune) {
            ImmortalCultivation.LOGGER.debug("Source {} immune for {} more ticks",
                sourceKey, immunityDuration - timeSinceLastDamage);
        }

        return isImmune;
    }

    /**
     * Sets immunity for a damage source, preventing spam while allowing multi-enemy combat.
     *
     * @param player The player taking damage
     * @param damageSource The source of the damage
     */
    private static void setSourceImmunity(Player player, DamageSource damageSource) {
        UUID playerId = player.getUUID();
        Map<String, Long> sourceImmunity = playerSourceImmunity.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        String sourceKey = createSourceKey(damageSource);
        long currentTime = player.level().getGameTime();

        sourceImmunity.put(sourceKey, currentTime);

        ImmortalCultivation.LOGGER.debug("Set immunity for player {} from source {} at time {}",
            player.getName().getString(), sourceKey, currentTime);
    }

    /**
     * Creates a unique key for damage source tracking that enables proper per-source immunity.
     * Different damage types get different key formats for optimal immunity behavior.
     *
     * @param damageSource The damage source to create a key for
     * @return A unique string key for this damage source
     */
    private static String createSourceKey(DamageSource damageSource) {
        StringBuilder keyBuilder = new StringBuilder();

        // Start with damage type
        keyBuilder.append(damageSource.getMsgId());

        // Add entity-specific information for entity damage
        if (damageSource.getDirectEntity() != null) {
            keyBuilder.append(":entity:").append(damageSource.getDirectEntity().getUUID());
        } else if (damageSource.getEntity() != null) {
            keyBuilder.append(":source:").append(damageSource.getEntity().getUUID());
        }

        // Add position for location-based environmental damage
        if (isEnvironmentalDamage(damageSource) && damageSource.getSourcePosition() != null) {
            Vec3 pos = damageSource.getSourcePosition();
            keyBuilder.append(":pos:")
                .append((int)pos.x).append(",")
                .append((int)pos.y).append(",")
                .append((int)pos.z);
        }

        return keyBuilder.toString();
    }

    /**
     * Determines the appropriate immunity duration based on damage source characteristics.
     * Balances spam prevention with responsive combat feel.
     *
     * @param damageSource The damage source to get immunity duration for
     * @return Immunity duration in ticks
     */
    private static long getImmunityDuration(DamageSource damageSource) {
        // Entity-based damage (players, mobs) - short immunity for responsive combat
        if (damageSource.getDirectEntity() != null || damageSource.getEntity() != null) {
            return ENTITY_DAMAGE_IMMUNITY_TICKS;
        }

        // Rapid environmental sources need longer immunity
        if (isRapidEnvironmentalDamage(damageSource)) {
            return RAPID_ENVIRONMENTAL_IMMUNITY_TICKS;
        }

        // Standard environmental damage
        if (isEnvironmentalDamage(damageSource)) {
            return ENVIRONMENTAL_DAMAGE_IMMUNITY_TICKS;
        }

        // Default fallback
        return GLOBAL_MIN_DAMAGE_INTERVAL;
    }

    /**
     * Checks if damage is from environmental sources (non-entity damage).
     *
     * @param damageSource The damage source to check
     * @return true if this is environmental damage
     */
    private static boolean isEnvironmentalDamage(DamageSource damageSource) {
        return damageSource.getDirectEntity() == null && damageSource.getEntity() == null;
    }

    /**
     * Checks if damage is from rapid environmental sources that need extended immunity.
     *
     * @param damageSource The damage source to check
     * @return true if this is rapid environmental damage
     */
    private static boolean isRapidEnvironmentalDamage(DamageSource damageSource) {
        String msgId = damageSource.getMsgId();
        return msgId.contains("cactus") || msgId.contains("berry") || msgId.contains("sweet_berry");
    }

    // === DAMAGE CALCULATION METHODS ===

    /**
     * Converts vanilla damage to custom damage with proper armor calculation and cultivation scaling.
     * This ensures that damage feels appropriate in the cultivation system while maintaining
     * vanilla mechanics like armor effectiveness.
     *
     * @param vanillaDamage The original vanilla damage amount
     * @param player The player taking damage
     * @param damageSource The source of the damage
     * @return The calculated custom damage amount
     */
    private static long calculateCustomDamage(float vanillaDamage, Player player, DamageSource damageSource) {
        float processedDamage = vanillaDamage;

        // Apply armor reduction if damage doesn't bypass armor
        if (!damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            processedDamage = applyArmorReduction(processedDamage, player);
        }

        // Convert to custom damage scale (can be adjusted based on cultivation system scaling)
        long customDamage = Math.round(processedDamage);

        // Ensure minimum damage for feedback
        return Math.max(customDamage, 1L);
    }

    /**
     * Applies vanilla armor reduction calculations to maintain familiar damage scaling.
     *
     * @param damage The damage amount before armor reduction
     * @param player The player with armor
     * @return The damage amount after armor reduction
     */
    private static float applyArmorReduction(float damage, Player player) {
        int armorValue = player.getArmorValue();
        float armorToughness = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);

        // Vanilla armor calculation
        float armorReduction = Math.min(20.0f, Math.max(armorValue / 5.0f,
            armorValue - damage / (2.0f + armorToughness / 4.0f))) / 25.0f;

        return damage * (1.0f - armorReduction);
    }

    /**
     * Converts vanilla healing to custom healing with potential cultivation bonuses.
     * This provides a hook for cultivation-based healing enhancements.
     *
     * @param vanillaHealing The original vanilla healing amount
     * @param player The player being healed
     * @return The calculated custom healing amount
     */
    private static long calculateCustomHealing(float vanillaHealing, Player player) {
        // Base 1:1 conversion - can be enhanced with cultivation bonuses
        return Math.round(vanillaHealing);
    }

    // === DAMAGE EFFECTS AND FEEDBACK ===

    /**
     * Applies visual and mechanical effects when damage is taken, including knockback,
     * armor damage, and player immunity timing for proper combat feedback.
     *
     * @param player The player taking damage
     * @param damageSource The source of the damage
     * @param originalDamage The original vanilla damage amount
     * @param actualDamage The actual custom damage applied
     */
    private static void applyDamageEffects(Player player, DamageSource damageSource,
                                           float originalDamage, long actualDamage) {
        // Apply minimal vanilla immunity for visual feedback only
        // The real immunity is handled by our per-source system
        player.invulnerableTime = isEnvironmentalDamage(damageSource) ? 10 : 20;

        // Apply knockback for combat feedback
        if (shouldApplyKnockback(player, damageSource)) {
            applyKnockback(player, damageSource, originalDamage);
        }

        // Damage armor based on damage taken
        damagePlayerArmor(player, originalDamage, damageSource);

    }

    /**
     * Applies knockback effects based on damage source and amount for proper combat feel.
     *
     * @param player The player to apply knockback to
     * @param damageSource The source of the damage
     * @param damage The damage amount for knockback scaling
     */
    private static void applyKnockback(Player player, DamageSource damageSource, float damage) {
        Vec3 knockbackDirection = getKnockbackDirection(player, damageSource);
        if (knockbackDirection == null) {
            return;
        }

        // Calculate knockback strength based on damage
        float knockbackStrength = Math.min(damage * 0.1f, 1.5f);

        // Apply knockback with proper velocity limits
        Vec3 currentVel = player.getDeltaMovement();
        double newVelX = currentVel.x + knockbackDirection.x * knockbackStrength;
        double newVelY = Math.max(currentVel.y, knockbackDirection.y * knockbackStrength * 0.5);
        double newVelZ = currentVel.z + knockbackDirection.z * knockbackStrength;

        // Clamp velocities to reasonable limits
        newVelX = Math.clamp(newVelX, -1.5, 1.5);
        newVelY = Math.clamp(newVelY, -1.5, 1.5);
        newVelZ = Math.clamp(newVelZ, -1.5, 1.5);

        player.setDeltaMovement(newVelX, newVelY, newVelZ);
        player.hasImpulse = true;
    }

    /**
     * Calculates knockback direction based on damage source for consistent combat feel.
     *
     * @param player The player being knocked back
     * @param damageSource The source of the damage
     * @return The knockback direction vector, or null if no knockback should be applied
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
            direction = direction.normalize();

            // Add some randomness for natural feel
            double randomX = (Math.random() - 0.5) * 0.2;
            double randomZ = (Math.random() - 0.5) * 0.2;
            direction = direction.add(randomX, 0.0, randomZ).normalize();

            // Ensure minimum upward component
            if (direction.y < 0.1) {
                direction = new Vec3(direction.x, 0.1, direction.z).normalize();
            }
        }

        return direction;
    }

    /**
     * Determines if knockback should be applied based on player state and damage source.
     *
     * @param player The player potentially receiving knockback
     * @param damageSource The source of the damage
     * @return true if knockback should be applied
     */
    private static boolean shouldApplyKnockback(Player player, DamageSource damageSource) {
        return !player.getAbilities().instabuild &&
            !damageSource.is(net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK) &&
            player.hurtTime <= 0;
    }

    /**
     * Damages player armor when taking damage, maintaining vanilla armor durability mechanics.
     *
     * @param player The player whose armor should be damaged
     * @param damage The damage amount for armor durability calculation
     * @param damageSource The source of the damage
     */
    private static void damagePlayerArmor(Player player, float damage, DamageSource damageSource) {
        if (damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        for (EquipmentSlot slot : armorSlots) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamageableItem()) {
                // Calculate armor damage based on damage taken
                int armorDamage = Math.max(1, Math.round(damage / 4.0f));

                int currentDamage = armor.getDamageValue();
                int newDamage = Math.min(currentDamage + armorDamage, armor.getMaxDamage());
                armor.setDamageValue(newDamage);

                // Handle armor breaking
                if (newDamage >= armor.getMaxDamage()) {
                    player.setItemSlot(slot, ItemStack.EMPTY);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
        }
    }

    // === HEALTH SYNCHRONIZATION METHODS ===

    /**
     * Updates vanilla health to properly reflect custom health percentage while maintaining
     * vanilla health as a visual indicator. This ensures UI elements and other systems
     * that depend on vanilla health display correctly.
     *
     * @param player The player whose vanilla health should be updated
     * @param healthAttachment The custom health attachment containing true health values
     */
    private static void updateVanillaHealthFromCustom(Player player, PlayerResourceAttachment healthAttachment) {
        long customHealth = healthAttachment.getCurrentHealth();
        long maxCustomHealth = healthAttachment.getMaxHealth();

        if (maxCustomHealth <= 0) {
            return;
        }

        if (customHealth <= 0) {
            // Player should die - set vanilla health to 0
            player.setHealth(0.0f);
        } else {
            // Calculate proportional vanilla health (scale to vanilla max health)
            float maxVanillaHealth = player.getMaxHealth();
            float proportionalHealth = (float) customHealth / maxCustomHealth * maxVanillaHealth;

            // Ensure minimum health when alive for UI consistency
            proportionalHealth = Math.max(proportionalHealth, MINIMUM_VANILLA_HEALTH);

            player.setHealth(proportionalHealth);
        }
    }

    /**
     * Calculates the target vanilla health that should be displayed based on custom health percentage.
     *
     * @param healthAttachment The custom health attachment
     * @return The target vanilla health value
     */
    private static float calculateTargetVanillaHealth(PlayerResourceAttachment healthAttachment) {
        long customHealth = healthAttachment.getCurrentHealth();
        long maxCustomHealth = healthAttachment.getMaxHealth();

        if (maxCustomHealth <= 0 || customHealth <= 0) {
            return 0.0f;
        }

        // Scale to 20 hearts (40 half-hearts) maximum
        float proportion = (float) customHealth / maxCustomHealth;
        return Math.max(proportion * 20.0f, MINIMUM_VANILLA_HEALTH);
    }

    /**
     * Checks if damage was recently processed to avoid false positives in unauthorized change detection.
     *
     * @param playerId The player's UUID
     * @param currentTime The current game time
     * @return true if damage was recently processed
     */
    private static boolean wasRecentlyProcessed(UUID playerId, long currentTime) {
        Long lastProcessTime = lastProcessedDamageTime.get(playerId);
        if (lastProcessTime == null) {
            return false;
        }

        // Consider damage "recent" if it happened within the last 10 ticks
        return (currentTime - lastProcessTime) <= 10;
    }

    /**
     * Provides emergency health sync recovery for edge cases and debugging.
     * This method can be called from external systems when health sync issues are detected.
     *
     * @param player The player to perform emergency sync for
     */
    public static void emergencyHealthSync(ServerPlayer player) {
        PlayerResourceAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
        if (healthAttachment != null) {
            // Force sync of health attachment
            healthAttachment.markDirty();

            // Update vanilla health immediately
            updateVanillaHealthFromCustom(player, healthAttachment);

            // Reset tracking
            UUID playerId = player.getUUID();
            lastKnownVanillaHealth.put(playerId, player.getHealth());
            lastKnownCustomHealth.put(playerId, healthAttachment.getCurrentHealth());
            emergencyProtectionActive.remove(playerId);

            // Trigger immediate network sync through NetworkSyncManager
            NetworkSyncManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_RESOURCES::get, true);

            ImmortalCultivation.LOGGER.warn("Performed emergency health sync for player {}",
                player.getName().getString());
        }
    }

    /**
     * Cleanup method that can be called periodically to prevent memory leaks from inactive players.
     * This integrates with ICModTickEvent's periodic cleanup systems.
     */
    public static void performPeriodicCleanup() {
        // Remove immunity entries older than 5 minutes for inactive players
        long cleanupThreshold = System.currentTimeMillis() - 300_000; // 5 minutes

        playerSourceImmunity.entrySet().removeIf(entry -> {
            Map<String, Long> sourceMap = entry.getValue();
            sourceMap.entrySet().removeIf(sourceEntry -> sourceEntry.getValue() < cleanupThreshold);
            return sourceMap.isEmpty();
        });
    }

    private static void forceImmediateHealthSync(ServerPlayer player, PlayerResourceAttachment healthAttachment) {
        // Mark as dirty for network sync
        healthAttachment.markDirty();

        // Schedule immediate sync (highest priority)
        NetworkSyncManager.INSTANCE.scheduleSync(player, ICModAttachmentTypes.PLAYER_RESOURCES::get, true);

        // Update vanilla health for UI consistency
        updateVanillaHealthFromCustom(player, healthAttachment);

        // Update tracking
        UUID playerId = player.getUUID();
        lastKnownVanillaHealth.put(playerId, player.getHealth());
        lastKnownCustomHealth.put(playerId, healthAttachment.getCurrentHealth());

        ImmortalCultivation.LOGGER.debug("Forced immediate health sync for player {}: {}/{}",
            player.getName().getString(), healthAttachment.getCurrentHealth(), healthAttachment.getMaxHealth());
    }
}