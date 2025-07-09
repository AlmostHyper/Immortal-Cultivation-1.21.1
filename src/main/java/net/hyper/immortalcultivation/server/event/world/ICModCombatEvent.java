package net.hyper.immortalcultivation.server.event.world;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerCombatStatsAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.core.network.packet.DamageFeedbackSyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CULTIVATION COMBAT SYSTEM
 *
 * This consolidated event handler manages all combat-related functionality for the cultivation system,
 * combining attribute management, damage feedback, equipment handling, and combat effects into a
 * single cohesive system.
 *
 * Key Features:
 * - Dynamic attribute modifier application based on cultivation stats
 * - Sophisticated damage feedback with knockback and visual effects
 * - Equipment enhancement and tooltip integration
 * - Jump height enhancement with cultivation scaling
 * - Performance optimized with caching and staggered updates
 * - Integration with ICModTickEvent and NetworkSyncManager
 * - Comprehensive debug and monitoring capabilities
 *
 * Architecture:
 * - Login initialization at NORMAL priority for proper dependency order
 * - Equipment changes at NORMAL priority for responsive updates
 * - Jump enhancement at LOW priority to avoid interfering with other systems
 * - Tooltip modification at LOW priority for compatibility
 * - Integrates with dirty flag system for efficient network sync
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ICModCombatEvent {

    // === ATTRIBUTE MODIFIER CONSTANTS ===

    /**
     * UUID constants for cultivation-based attribute modifiers.
     * These must be unique and persistent across sessions for proper modifier management.
     */
    private static final UUID CULTIVATION_ATTACK_DAMAGE_UUID = UUID.fromString("a1b2c3d4-e5f6-1234-5678-9abcdef01234");
    private static final UUID CULTIVATION_MOVEMENT_SPEED_UUID = UUID.fromString("b2c3d4e5-f6a1-2345-6789-abcdef012345");
    private static final UUID CULTIVATION_JUMP_HEIGHT_UUID = UUID.fromString("c3d4e5f6-a1b2-3456-789a-bcdef0123456");
    private static final UUID CULTIVATION_WEAPON_MODIFIER_UUID = UUID.fromString("d1e2f3a4-b5c6-7890-abcd-ef1234567890");

    private static final ResourceLocation CULTIVATION_ATTACK_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_attack_damage");
    private static final ResourceLocation CULTIVATION_MOVEMENT_SPEED_ID = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_movement_speed");
    private static final ResourceLocation CULTIVATION_JUMP_HEIGHT_ID = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_jump_height");

    // === PERFORMANCE OPTIMIZATION CACHING ===

    /**
     * Performance optimization caches to prevent unnecessary recalculations and updates.
     * These caches are cleaned up periodically to prevent memory leaks.
     */
    private static final Map<UUID, Long> lastModifierUpdateTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastKnownRealmHash = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastDamageFeedbackTime = new ConcurrentHashMap<>();

    // Performance constants
    private static final int MODIFIER_UPDATE_INTERVAL = 200;  // Every 10 seconds
    private static final int SYNC_CHECK_INTERVAL = 40;        // Check sync every 2 seconds
    private static final long MAX_PROCESSING_TIME_NANOS = 2_000_000;  // 2ms max per operation
    private static final long DAMAGE_FEEDBACK_COOLDOWN = 5;   // 5 ticks between feedback

    // === PLAYER LOGIN AND INITIALIZATION ===

    /**
     * Handles player login initialization for combat stats and attribute modifiers.
     * This ensures all combat-related systems are properly initialized and synchronized.
     *
     * Runs at NORMAL priority to execute after basic attachment initialization but
     * before network synchronization in the login sequence.
     *
     * @param event The player login event
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        long startTime = System.nanoTime();

        try {
            // Initialize combat stats attachment
            initializeCombatStats(serverPlayer);

            // Apply initial attribute modifiers
            applyCombatStatsModifiers(serverPlayer);

            // Initialize performance tracking
            UUID playerId = serverPlayer.getUUID();
            lastModifierUpdateTime.put(playerId, System.currentTimeMillis());
            lastKnownRealmHash.put(playerId, getCombatStatsHash(serverPlayer));

            ImmortalCultivation.LOGGER.info("Combat system initialized for player {} - ATK: +{}%, SPD: +{}%, JUMP: +{}%",
                serverPlayer.getName().getString(),
                getFormattedBonusPercentage(serverPlayer, "attack"),
                getFormattedBonusPercentage(serverPlayer, "speed"),
                getFormattedBonusPercentage(serverPlayer, "jump"));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing combat system for player {}",
                serverPlayer.getName().getString(), e);
        } finally {
            long processingTime = System.nanoTime() - startTime;
            if (processingTime > MAX_PROCESSING_TIME_NANOS) {
                ImmortalCultivation.LOGGER.warn("Combat initialization took {}ms for player {} (exceeds 2ms threshold)",
                    processingTime / 1_000_000.0, serverPlayer.getName().getString());
            }
        }
    }

    /**
     * Handles player logout cleanup to prevent memory leaks and ensure clean state.
     *
     * @param event The player logout event
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();

        // Clean up all tracking caches
        lastModifierUpdateTime.remove(playerId);
        lastKnownRealmHash.remove(playerId);
        lastDamageFeedbackTime.remove(playerId);

        ImmortalCultivation.LOGGER.debug("Cleaned up combat tracking for player {}",
            event.getEntity().getName().getString());
    }

    // === EQUIPMENT AND WEAPON HANDLING ===

    /**
     * Handles equipment changes to update weapon-specific modifiers and combat effectiveness.
     * This ensures that weapon enhancements and cultivation bonuses are properly applied.
     *
     * @param event The equipment change event
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        try {
            // Handle weapon/equipment changes
            updateWeaponModifiers(serverPlayer, event.getTo());

            // If main hand weapon changed, refresh all modifiers
            if (event.getSlot().getType() == net.minecraft.world.entity.EquipmentSlot.Type.HAND) {
                refreshCombatStats(serverPlayer);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error handling equipment change for player {}",
                serverPlayer.getName().getString(), e);
        }
    }

    // === JUMP ENHANCEMENT SYSTEM ===

    /**
     * Handles jump height enhancement based on cultivation progression.
     * This provides vertical mobility scaling that feels natural and responsive.
     *
     * Runs at LOW priority to avoid interfering with other jump modifications.
     *
     * @param event The living jump event
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats == null) {
                return;
            }

            double jumpMultiplier = combatStats.getEffectiveJumpHeightMultiplier();

            if (jumpMultiplier > 0.0) {
                // Get current movement and only modify Y if it's a fresh jump
                Vec3 currentMovement = player.getDeltaMovement();

                // Only enhance if this looks like a fresh jump (Y velocity around vanilla jump)
                if (currentMovement.y > 0.3 && currentMovement.y < 0.5) {
                    // Enhance the existing jump velocity instead of replacing it
                    double enhancedY = currentMovement.y * (1.0 + jumpMultiplier);

                    // Cap enhanced jump to prevent excessive heights
                    enhancedY = Math.min(enhancedY, 1.2);

                    player.setDeltaMovement(currentMovement.x, enhancedY, currentMovement.z);

                    ImmortalCultivation.LOGGER.debug("Enhanced jump for {}: {}% boost ({} -> {})",
                        player.getName().getString(),
                        String.format("%.1f", jumpMultiplier * 100),
                        String.format("%.3f", currentMovement.y),
                        String.format("%.3f", enhancedY));
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error enhancing jump for player {}",
                player.getName().getString(), e);
        }
    }

    // === TOOLTIP ENHANCEMENT SYSTEM ===

    /**
     * Adds cultivation-based combat bonuses to weapon tooltips for player information.
     * This helps players understand how their cultivation affects weapon effectiveness.
     *
     * Runs at LOW priority for compatibility with other tooltip modifications.
     *
     * @param event The item tooltip event
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem) {
            addCombatStatsTooltip(stack, tooltip, event.getEntity());
        }
    }

    // === DAMAGE FEEDBACK SYSTEM ===

    /**
     * Applies comprehensive damage feedback including knockback, visual effects, and network sync.
     * This method should be called from ICModHealthEvent after damage is applied to provide
     * proper combat feedback that maintains the vanilla feel while working with custom health.
     *
     * @param player The player who took damage
     * @param damageAmount The amount of custom damage taken
     * @param maxHealth The player's maximum custom health for scaling calculations
     * @param damageSource The source of the damage for appropriate feedback
     */
    public static void applyDamageFeedback(Player player, long damageAmount, long maxHealth, DamageSource damageSource) {
        if (player.level().isClientSide()) {
            applyClientSideFeedback(player, damageAmount);
        } else {
            applyServerSideFeedback(player, damageAmount, maxHealth, damageSource);
        }
    }

    /**
     * Applies client-side damage feedback for immediate visual response.
     *
     * @param player The player taking damage
     * @param damageAmount The damage amount for feedback intensity
     */
    private static void applyClientSideFeedback(Player player, long damageAmount) {
        // Convert damage to vanilla scale for proper feedback intensity
        float vanillaDamage = convertToVanillaScale(damageAmount);

        // Trigger the hurt animation and red screen overlay
        player.hurtTime = 10; // Duration of hurt animation (10 ticks = 0.5 seconds)
        player.hurtDuration = 10; // How long the hurt effect lasts

        ImmortalCultivation.LOGGER.debug("Applied client-side damage feedback for {} damage", damageAmount);
    }

    /**
     * Applies server-side damage feedback including knockback and network synchronization.
     *
     * @param player The player taking damage
     * @param damageAmount The custom damage amount
     * @param maxHealth The player's maximum health for scaling
     * @param damageSource The source of the damage
     */
    private static void applyServerSideFeedback(Player player, long damageAmount, long maxHealth, DamageSource damageSource) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Check feedback cooldown to prevent spam
        UUID playerId = player.getUUID();
        Long lastFeedback = lastDamageFeedbackTime.get(playerId);
        long currentTime = player.level().getGameTime();

        if (lastFeedback != null && (currentTime - lastFeedback) < DAMAGE_FEEDBACK_COOLDOWN) {
            return;
        }

        lastDamageFeedbackTime.put(playerId, currentTime);

        // Apply knockback with cultivation-based scaling
        applyCustomKnockback(player, damageAmount, maxHealth, damageSource);

        // Send damage feedback packet to client if available
        try {
            DamageFeedbackSyncPacket packet = new DamageFeedbackSyncPacket(damageAmount, damageSource.getMsgId());
            ICModPacketRegistry.sendToPlayer(packet, serverPlayer);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.debug("Could not send damage feedback packet (packet may not exist): {}", e.getMessage());
        }

        // Set server-side hurt time for animation sync
        player.hurtTime = 10;
        player.hurtDuration = 10;

        ImmortalCultivation.LOGGER.debug("Applied server-side damage feedback for {} damage from {}",
            damageAmount, damageSource.getMsgId());
    }

    // === ATTRIBUTE MODIFIER MANAGEMENT ===

    /**
     * Initializes combat stats attachment and ensures proper player reference.
     *
     * @param player The player to initialize combat stats for
     */
    private static void initializeCombatStats(ServerPlayer player) {
        try {
            PlayerCombatStatsAttachment combatStats = PlayerCombatStatsAttachment.getOrCreate(player);

            ImmortalCultivation.LOGGER.info("Combat stats initialized for player {} - ATK: +{}%, SPD: +{}%, JUMP: +{}%",
                player.getName().getString(),
                String.format("%.1f", combatStats.getAttackDamageBonus() * 100),
                String.format("%.1f", combatStats.getMovementSpeedBonus() * 100),
                String.format("%.1f", combatStats.getJumpHeightBonus() * 100));
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing combat stats attachment for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Applies all cultivation-based attribute modifiers to the player.
     * This method ensures proper modifier application with error handling and logging.
     *
     * @param player The player to apply modifiers to
     */
    public static void applyCombatStatsModifiers(ServerPlayer player) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats == null) {
                return;
            }

            // Apply attack damage modifier
            double attackMultiplier = combatStats.getEffectiveAttackDamageMultiplier();
            if (attackMultiplier != 1.0) {
                AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackAttr != null) {
                    attackAttr.removeModifier(CULTIVATION_ATTACK_DAMAGE_ID);
                    AttributeModifier attackModifier = new AttributeModifier(
                        CULTIVATION_ATTACK_DAMAGE_ID,
                        attackMultiplier - 1.0, // Convert multiplier to additive bonus
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );
                    attackAttr.addPermanentModifier(attackModifier);
                }
            }

            // Apply movement speed modifier
            double speedBonus = combatStats.getEffectiveMovementSpeedMultiplier();
            if (speedBonus > 0.0) {
                AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null) {
                    speedAttr.removeModifier(CULTIVATION_MOVEMENT_SPEED_ID);
                    AttributeModifier speedModifier = new AttributeModifier(
                        CULTIVATION_MOVEMENT_SPEED_ID,
                        speedBonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );
                    speedAttr.addPermanentModifier(speedModifier);
                }
            }

            // Apply step height modifier for enhanced movement (instead of jump height attribute)
            double jumpBonus = combatStats.getEffectiveJumpHeightMultiplier();
            if (jumpBonus > 0.0) {
                AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
                if (stepAttr != null) {
                    stepAttr.removeModifier(CULTIVATION_JUMP_HEIGHT_ID);
                    // Convert jump bonus to step height increase (scaled down)
                    double stepBonus = jumpBonus * 0.3; // 30% of jump bonus as step height
                    AttributeModifier stepModifier = new AttributeModifier(
                        CULTIVATION_JUMP_HEIGHT_ID,
                        stepBonus,
                        AttributeModifier.Operation.ADD_VALUE
                    );
                    stepAttr.addPermanentModifier(stepModifier);
                }
            }

            ImmortalCultivation.LOGGER.debug("Applied combat modifiers for player {}: ATK x{}, SPD +{}%, STEP +{}",
                player.getName().getString(),
                String.format("%.3f", attackMultiplier),
                String.format("%.1f", speedBonus * 100),
                String.format("%.1f", jumpBonus * 30));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying combat stats modifiers for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Removes all cultivation-based attribute modifiers from the player.
     *
     * @param player The player to remove modifiers from
     */
    private static void removeCombatStatsModifiers(ServerPlayer player) {
        try {
            // Remove attack damage modifier
            AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttr != null) {
                attackAttr.removeModifier(CULTIVATION_ATTACK_DAMAGE_ID);
            }

            // Remove movement speed modifier
            AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.removeModifier(CULTIVATION_MOVEMENT_SPEED_ID);
            }

            // Remove step height modifier
            AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepAttr != null) {
                stepAttr.removeModifier(CULTIVATION_JUMP_HEIGHT_ID);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error removing combat stats modifiers for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Refreshes all combat stats modifiers by removing and reapplying them.
     * This method can be called externally when combat stats change significantly.
     *
     * @param player The player to refresh modifiers for
     */
    public static void refreshCombatStats(ServerPlayer player) {
        try {
            // Simply reapply all modifiers
            removeCombatStatsModifiers(player);
            applyCombatStatsModifiers(player);

            // Update tracking
            lastModifierUpdateTime.put(player.getUUID(), System.currentTimeMillis());
            lastKnownRealmHash.put(player.getUUID(), getCombatStatsHash(player));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error refreshing combat stats modifiers for player {}",
                player.getName().getString(), e);
        }
    }

    // === KNOCKBACK SYSTEM ===

    /**
     * Applies cultivation-enhanced knockback based on damage and player progression.
     * Higher cultivation provides some resistance to knockback while maintaining combat feel.
     *
     * @param player The player to apply knockback to
     * @param damageAmount The amount of custom damage taken
     * @param maxHealth The player's maximum health for scaling
     * @param damageSource The source of the damage
     */
    private static void applyCustomKnockback(Player player, long damageAmount, long maxHealth, DamageSource damageSource) {
        // Don't apply knockback if damage source bypasses it or player is in creative
        if (damageSource.is(net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK) ||
            player.getAbilities().instabuild) {
            return;
        }

        // Calculate knockback strength based on damage percentage
        float damagePercentage = maxHealth > 0 ? (float) damageAmount / maxHealth : 0.0f;
        float baseKnockback = Math.min(1.5f, damagePercentage * 3.0f); // Max 1.5 block knockback

        // Determine knockback direction
        Vec3 knockbackDirection = getKnockbackDirection(player, damageSource);
        if (knockbackDirection == null) {
            return;
        }

        // Apply knockback with cultivation-based resistance
        float knockbackResistance = getKnockbackResistance(player);
        float finalKnockback = baseKnockback * (1.0f - knockbackResistance);

        if (finalKnockback > 0.1f) {
            applyKnockbackMotion(player, knockbackDirection, finalKnockback);
        }
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

        if (direction != null) {
            // Add some randomness for natural feel
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
     * Applies the calculated knockback motion to the player with proper velocity limits.
     *
     * @param player The player to apply knockback to
     * @param direction The knockback direction
     * @param strength The knockback strength
     */
    private static void applyKnockbackMotion(Player player, Vec3 direction, float strength) {
        Vec3 currentVelocity = player.getDeltaMovement();
        Vec3 knockbackVelocity = direction.scale(strength);

        // Apply knockback with proper velocity composition
        double newVelX = currentVelocity.x + knockbackVelocity.x;
        double newVelY = Math.max(currentVelocity.y, knockbackVelocity.y * 0.5);
        double newVelZ = currentVelocity.z + knockbackVelocity.z;

        // Apply velocity limits (prevent excessive knockback)
        newVelX = Mth.clamp(newVelX, -1.5, 1.5);
        newVelY = Mth.clamp(newVelY, -1.5, 1.5);
        newVelZ = Mth.clamp(newVelZ, -1.5, 1.5);

        player.setDeltaMovement(newVelX, newVelY, newVelZ);
        player.hasImpulse = true;

        ImmortalCultivation.LOGGER.debug("Applied knockback to {}: strength {}, direction {}",
            player.getName().getString(), strength, direction);
    }

    /**
     * Calculates knockback resistance based on cultivation progression.
     * Higher cultivation realms provide some resistance to knockback effects.
     *
     * @param player The player to calculate resistance for
     * @return Knockback resistance factor (0.0 to 0.8)
     */
    private static float getKnockbackResistance(Player player) {
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivation == null) {
            return 0.0f;
        }

        // Higher realms provide knockback resistance (max 80% reduction)
        // This can be adjusted based on your realm progression system
        try {
            // Basic resistance based on realm level - implement according to your realm system
            return 0.0f; // Placeholder - implement based on your cultivation realm data
        } catch (Exception e) {
            return 0.0f;
        }
    }

    // === TOOLTIP ENHANCEMENT ===

    /**
     * Adds cultivation-based combat bonuses to weapon tooltips.
     *
     * @param stack The weapon item stack
     * @param tooltip The tooltip component list to modify
     * @param player The player viewing the tooltip
     */
    private static void addCombatStatsTooltip(ItemStack stack, List<Component> tooltip, Player player) {
        if (player == null) return;
        if (!(stack.getItem() instanceof SwordItem) && !(stack.getItem() instanceof AxeItem)) return;

        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        if (combatStats == null) return;

        try {
            // Calculate effective bonuses
            double attackBonus = combatStats.getDisplayAttackBonus();
            double speedBonus = combatStats.getDisplaySpeedBonus();

            if (attackBonus > 0.0 || speedBonus > 0.0) {
                // Add header
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("Cultivation Bonuses:").withStyle(ChatFormatting.GOLD));

                // Add attack bonus if significant
                if (attackBonus > 0.01) {
                    tooltip.add(Component.literal(" ⚔ Attack Damage: +")
                        .append(Component.literal(String.format("%.1f%%", attackBonus * 100))
                            .withStyle(ChatFormatting.GREEN))
                        .withStyle(ChatFormatting.GRAY));
                }

                // Add speed bonus if significant
                if (speedBonus > 0.01) {
                    tooltip.add(Component.literal(" ⚡ Attack Speed: +")
                        .append(Component.literal(String.format("%.1f%%", speedBonus * 100))
                            .withStyle(ChatFormatting.GREEN))
                        .withStyle(ChatFormatting.GRAY));
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.debug("Error adding combat stats tooltip: {}", e.getMessage());
        }
    }

    // === WEAPON MODIFIER SYSTEM ===

    /**
     * Updates weapon-specific modifiers when equipment changes.
     * This can be extended to handle weapon enchantments or special properties.
     *
     * @param player The player whose weapon changed
     * @param weapon The new weapon item stack
     */
    private static void updateWeaponModifiers(ServerPlayer player, ItemStack weapon) {
        // This method is a placeholder for weapon-specific enhancement logic
        // It can be implemented based on your specific weapon enhancement system

        if (weapon.isEmpty() || (!(weapon.getItem() instanceof SwordItem) && !(weapon.getItem() instanceof AxeItem))) {
            return;
        }

        try {
            // Check for custom weapon data or enchantments
            CustomData customData = weapon.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();

                // Handle custom weapon bonuses if they exist
                if (tag.contains("cultivation_bonus")) {
                    // Implement custom weapon bonus logic here
                    ImmortalCultivation.LOGGER.debug("Weapon with cultivation bonus equipped by {}",
                        player.getName().getString());
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.debug("Error processing weapon modifiers: {}", e.getMessage());
        }
    }

    // === UTILITY METHODS ===

    /**
     * Converts custom damage amount to vanilla-equivalent scale for feedback calculations.
     *
     * @param customDamage The custom damage amount
     * @return Equivalent vanilla damage for feedback scaling
     */
    private static float convertToVanillaScale(long customDamage) {
        // Adjust this conversion based on your health scaling
        // This assumes custom health is roughly proportional to vanilla health
        return Math.min(20.0f, customDamage / 10.0f);
    }

    /**
     * Gets a formatted bonus percentage string for a specific stat type.
     *
     * @param player The player to get bonus for
     * @param statType The type of stat ("attack", "speed", "jump")
     * @return Formatted percentage string
     */
    private static String getFormattedBonusPercentage(ServerPlayer player, String statType) {
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        if (combatStats == null) return "0.0";

        try {
            switch (statType.toLowerCase()) {
                case "attack":
                    return String.format("%.1f", combatStats.getDisplayAttackBonus() * 100);
                case "speed":
                    return String.format("%.1f", combatStats.getDisplaySpeedBonus() * 100);
                case "jump":
                    return String.format("%.1f", combatStats.getDisplayJumpBonus() * 100);
                default:
                    return "0.0";
            }
        } catch (Exception e) {
            return "0.0";
        }
    }

    /**
     * Generates a hash of the player's combat-relevant stats for change detection.
     *
     * @param player The player to generate hash for
     * @return Hash value representing current combat state
     */
    private static int getCombatStatsHash(ServerPlayer player) {
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        if (combatStats == null) return 0;

        try {
            // Create hash based on all relevant combat stats
            return (int) (
                (combatStats.getEffectiveAttackDamageMultiplier() * 1000) +
                    (combatStats.getEffectiveMovementSpeedMultiplier() * 1000) +
                    (combatStats.getEffectiveJumpHeightMultiplier() * 1000)
            );
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Provides emergency combat stats refresh for debugging and recovery.
     * This method can be called externally when combat system issues are detected.
     *
     * @param player The player to perform emergency refresh for
     */
    public static void emergencyCombatRefresh(ServerPlayer player) {
        try {
            // Remove all existing modifiers
            removeCombatStatsModifiers(player);

            // Reapply all modifiers
            applyCombatStatsModifiers(player);

            // Reset tracking
            UUID playerId = player.getUUID();
            lastModifierUpdateTime.put(playerId, System.currentTimeMillis());
            lastKnownRealmHash.put(playerId, getCombatStatsHash(player));

            ImmortalCultivation.LOGGER.warn("Performed emergency combat refresh for player {}",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Emergency combat refresh failed for player {}: {}",
                player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Cleanup method that can be called periodically to prevent memory leaks.
     * This integrates with ICModTickEvent's periodic cleanup systems.
     */
    public static void performPeriodicCleanup() {
        long cleanupThreshold = System.currentTimeMillis() - 300_000; // 5 minutes

        // Remove old tracking entries
        lastModifierUpdateTime.entrySet().removeIf(entry -> entry.getValue() < cleanupThreshold);
        lastKnownRealmHash.entrySet().removeIf(entry ->
            lastModifierUpdateTime.get(entry.getKey()) == null);
        lastDamageFeedbackTime.entrySet().removeIf(entry -> entry.getValue() < cleanupThreshold / 1000);
    }
}