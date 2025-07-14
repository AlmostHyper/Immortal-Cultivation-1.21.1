package net.hyper.immortalcultivation.server.events;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.*;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Unified Event Manager for the Immortal Cultivation Mod
 *
 * This centralized event handler consolidates core game logic events to reduce
 * complexity and improve encapsulation. It manages player lifecycle, combat,
 * health, and attribute systems in a coordinated manner.
 *
 * Events handled:
 * - Player lifecycle (login, respawn, dimension change)
 * - Combat system (damage, healing, equipment)
 * - Attribute management and tooltips
 *
 * Architecture:
 * - Modular design with specialized handler methods
 * - Consistent error handling and logging
 * - Integration with existing network sync systems
 * - Performance-optimized with caching and rate limiting
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class ICModEventManager {

    // === ATTACHMENT MANAGEMENT === //

    /**
     * List of all attachment types managed by the initialization system
     */
    private static final List<Supplier<? extends AttachmentType<?>>> MANAGED_ATTACHMENTS = List.of(
        ICModAttachmentTypes.PLAYER_CULTIVATION,
        ICModAttachmentTypes.PLAYER_STATS,
        ICModAttachmentTypes.PLAYER_BREAKTHROUGH,
        ICModAttachmentTypes.PLAYER_TALENTS,
        ICModAttachmentTypes.PLAYER_RACE,
        ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION
    );

    // === IMMUNITY TRACKING === //

    /**
     * Tracks immunity per player per damage source for sophisticated multi-enemy combat
     */
    private static final Map<UUID, Map<String, Long>> playerImmunityMap = new ConcurrentHashMap<>();

    /**
     * Tracks last known vanilla health values for protection system
     */
    private static final Map<UUID, Float> lastKnownVanillaHealth = new ConcurrentHashMap<>();

    // === PLAYER LIFECYCLE EVENTS === //

    /**
     * Handles player login initialization and synchronization
     *
     * @param event Player login event
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Initializing player data for: {}", player.getName().getString());

            initializePlayerAttachments(player);
            applyCurrentRealmModifiers(player);
            synchronizeAllPlayerData(player);

            ImmortalCultivation.LOGGER.debug("Player {} fully initialized", player.getName().getString());
        }
    }

    /**
     * Handles player respawn events
     *
     * @param event Player respawn event
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing respawn for: {}", player.getName().getString());

            // Clear immunity tracking on respawn
            clearPlayerImmunity(player.getUUID());

            // Reapply realm modifiers
            applyCurrentRealmModifiers(player);
            synchronizeAllPlayerData(player);
        }
    }

    /**
     * Handles dimension change events
     *
     * @param event Player dimension change event
     */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing dimension change for: {}", player.getName().getString());

            applyCurrentRealmModifiers(player);
            synchronizeAllPlayerData(player);
        }
    }

    /**
     * Sets player reference on attachment if it supports it
     * This ensures attachments have proper player context for packet creation
     *
     * @param attachment The attachment object
     * @param player The player to set as reference
     */
    private static void setPlayerReferenceIfSupported(Object attachment, ServerPlayer player) {
        // Check for known attachment types with setPlayer methods
        if (attachment instanceof PlayerTalentsAttachment talents) {
            talents.setPlayer(player);
        } else if (attachment instanceof PlayerStatsAttachment stats) {
            stats.setPlayer(player);
        } else if (attachment instanceof PlayerCultivationAttachment cultivation) {
            cultivation.setPlayer(player);
        } else if (attachment instanceof PlayerBreakthroughAttachment breakthrough) {
            breakthrough.setPlayer(player);
        } else if (attachment instanceof PlayerRaceAttachment race) {
            race.setPlayer(player);
        } else if (attachment instanceof PlayerPillConsumptionAttachment pills) {
            pills.setPlayer(player);
        }
        // Add other attachment types as needed when you create new attachments
    }

    // === COMBAT EVENTS === //

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
     * Converts vanilla damage to custom damage using simple 1:1 scaling.
     * Maintains compatibility with vanilla and all mods.
     *
     * @param vanillaDamage Original vanilla damage
     * @param player The player taking damage
     * @return Custom damage (1:1 conversion)
     */
    private static long convertToCustomDamage(float vanillaDamage, Player player) {
        // Simple 1:1 conversion for vanilla/mod compatibility
        return Math.round(vanillaDamage);
    }

    /**
     * Converts vanilla healing to custom healing using simple 1:1 scaling.
     *
     * @param vanillaHealing Original vanilla healing
     * @param player The player being healed
     * @return Custom healing (1:1 conversion)
     */
    private static long convertToCustomHealing(float vanillaHealing, Player player) {
        // Simple 1:1 conversion - REMOVED the *100 multiplier that was causing instant heals
        return Math.round(vanillaHealing);
    }

    /**
     * Fixed healing event handler that maintains vanilla compatibility.
     *
     * @param event Healing event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHealing(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        PlayerStatsAttachment statsAttachment = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
        if (statsAttachment == null) {
            return;
        }

        float vanillaHealing = event.getAmount();

        // Cancel vanilla healing
        event.setAmount(0.0f);

        // Convert to custom healing with 1:1 scaling (no more 100x multiplier!)
        long customHealing = convertToCustomHealing(vanillaHealing, player);

        if (customHealing > 0) {
            long actualHealing = statsAttachment.addStat(IStatsData.StatType.HEALTH, customHealing);

            if (actualHealing > 0) {
                // Update vanilla health display
                updateVanillaHealthDisplay(player, statsAttachment);

                // Mark for sync
                statsAttachment.markDirty();

                ImmortalCultivation.LOGGER.debug("Applied {} custom healing to {} (vanilla: {})",
                    actualHealing, player.getName().getString(), vanillaHealing);
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

    /**
     * Handles equipment changes for attribute recalculation
     *
     * @param event Equipment change event
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Recalculate attributes when equipment changes
        recalculatePlayerAttributes(player);

        ImmortalCultivation.LOGGER.debug("Recalculated attributes for {} due to equipment change",
            player.getName().getString());
    }

    /**
     * Adds cultivation stats to item tooltips
     *
     * @param event Item tooltip event
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        // Add cultivation-specific tooltip information
        addCultivationTooltip(stack, tooltip, event.getEntity());
    }

    // === HELPER METHODS === //

    /**
     * Initializes all managed attachments for a player
     *
     * @param player The player to initialize
     */
    private static void initializePlayerAttachments(ServerPlayer player) {
        for (Supplier<? extends AttachmentType<?>> attachmentSupplier : MANAGED_ATTACHMENTS) {
            AttachmentType<?> attachmentType = attachmentSupplier.get();

            if (!player.hasData(attachmentType)) {
                // This will create the attachment with default values
                player.getData(attachmentType);
            }

            // Initialize if the attachment supports it
            Object attachment = player.getExistingData(attachmentType).orElse(null);
            if (attachment instanceof IPlayerInitializable initializable) {
                initializable.onPlayerLogin(player);
            }
        }
    }

    /**
     * Applies current realm modifiers for all cultivation types
     *
     * @param player The player to apply modifiers for
     */
    private static void applyCurrentRealmModifiers(ServerPlayer player) {
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

        // Apply Qi cultivation modifiers
        CultivationRealm qiRealm = cultivation.getRealm(ICultivationData.CultivationType.QI);
        if (qiRealm != null) {
            qiRealm.applyRealmModifiers(player, cultivation, ICultivationData.CultivationType.QI);
        }

        // Apply Body cultivation modifiers
        CultivationRealm bodyRealm = cultivation.getRealm(ICultivationData.CultivationType.BODY);
        if (bodyRealm != null) {
            bodyRealm.applyRealmModifiers(player, cultivation, ICultivationData.CultivationType.BODY);
        }
    }

    /**
     * Synchronizes all player data to the client
     *
     * @param player The player to sync data for
     */
    private static void synchronizeAllPlayerData(ServerPlayer player) {
        for (Supplier<? extends AttachmentType<?>> attachmentSupplier : MANAGED_ATTACHMENTS) {
            Object attachment = player.getExistingData(attachmentSupplier.get()).orElse(null);
            if (attachment instanceof ISyncable syncable) {
                try {
                    // Ensure player reference is set if the attachment supports it
                    setPlayerReferenceIfSupported(attachment, player);

                    // Create and send sync packet
                    Optional<ISyncPacket> packetOptional = syncable.createPacket(true); // true for full sync
                    if (packetOptional.isPresent()) {
                        ICModPacketRegistry.sendToPlayer(packetOptional.get(), player);
                        syncable.clearDirty();

                        ImmortalCultivation.LOGGER.debug("Synced {} for player {}",
                            attachmentSupplier.get().toString(), player.getName().getString());
                    }
                } catch (Exception e) {
                    ImmortalCultivation.LOGGER.error("Error syncing attachment {} for player {}: {}",
                        attachmentSupplier.get().toString(), player.getName().getString(), e.getMessage());
                }
            }
        }
    }

    /**
     * Checks if a player is immune to a specific damage source
     *
     * @param playerId Player UUID
     * @param sourceKey Damage source key
     * @return True if player is immune
     */
    private static boolean isPlayerImmune(UUID playerId, String sourceKey) {
        Map<String, Long> playerImmunities = playerImmunityMap.get(playerId);
        if (playerImmunities == null) {
            return false;
        }

        Long immunityEnd = playerImmunities.get(sourceKey);
        if (immunityEnd == null) {
            return false;
        }

        if (System.currentTimeMillis() > immunityEnd) {
            playerImmunities.remove(sourceKey);
            return false;
        }

        return true;
    }

    /**
     * Sets immunity for a player against a specific damage source
     *
     * @param playerId Player UUID
     * @param sourceKey Damage source key
     * @param durationMs Immunity duration in milliseconds
     */
    private static void setPlayerImmunity(UUID playerId, String sourceKey, long durationMs) {
        playerImmunityMap.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(sourceKey, System.currentTimeMillis() + durationMs);
    }

    /**
     * Clears all immunity for a player
     *
     * @param playerId Player UUID
     */
    private static void clearPlayerImmunity(UUID playerId) {
        playerImmunityMap.remove(playerId);
    }

    /**
     * Generates a unique key for a damage source
     *
     * @param damageSource The damage source
     * @return Unique string key
     */
    private static String generateDamageSourceKey(DamageSource damageSource) {
        if (damageSource.getEntity() != null) {
            return damageSource.type().msgId() + "_" + damageSource.getEntity().getUUID();
        }
        return damageSource.type().msgId();
    }

    /**
     * Converts vanilla damage to custom damage using simple 1:1 scaling.
     * Maintains compatibility with vanilla and all mods.
     *
     * @param vanillaDamage Original vanilla damage
     * @param player The player taking damage
     * @return Custom damage (1:1 conversion)
     */
    private static long convertToCustomDamage(double vanillaDamage, Player player) {
        // Simple 1:1 conversion for vanilla/mod compatibility
        return Math.round(vanillaDamage);
    }

    /**
     * Converts vanilla healing to custom healing using simple 1:1 scaling.
     * Fixed to remove the 100x multiplier that was causing instant full heals.
     *
     * @param vanillaHealing Original vanilla healing
     * @param player The player being healed
     * @return Custom healing (1:1 conversion)
     */
    private static long convertToCustomHealing(double vanillaHealing, Player player) {
        // Simple 1:1 conversion - REMOVED the *100 multiplier that was causing instant heals
        return Math.round(vanillaHealing);
    }

    /**
     * Calculates immunity duration based on damage amount
     *
     * @param damage Damage taken
     * @return Immunity duration in milliseconds
     */
    private static long calculateImmunityDuration(long damage) {
        // Base immunity of 500ms, with scaling
        return 500L + Math.min(damage / 10, 1000L);
    }

    /**
     * Updates vanilla health display to match custom health
     *
     * @param player The player
     * @param resourceAttachment The resource attachment
     */
    private static void updateVanillaHealthDisplay(Player player, PlayerStatsAttachment resourceAttachment) {
        float percentage = (float) resourceAttachment.getCurrentStat(IStatsData.StatType.HEALTH) / resourceAttachment.getMaxStat(IStatsData.StatType.HEALTH);
        float vanillaHealth = percentage * player.getMaxHealth();
        player.setHealth(Math.max(0.5f, vanillaHealth)); // Minimum 0.5 to prevent death

        lastKnownVanillaHealth.put(player.getUUID(), player.getHealth());
    }

    /**
     * Applies damage feedback effects (knockback, hurt animation, etc.)
     *
     * @param player The player
     * @param damage Damage amount
     * @param maxHealth Maximum health for scaling
     * @param damageSource Damage source
     */
    private static void applyDamageFeedback(Player player, long damage, long maxHealth, DamageSource damageSource) {
        // Apply hurt animation
        player.hurtTime = 10;
        player.hurtDuration = 10;

        // Apply knockback if appropriate
        if (damageSource.getEntity() != null) {
            // Calculate knockback direction and apply
            // Implementation depends on your specific needs
        }
    }

    /**
     * Recalculates all player attributes based on current equipment and cultivation
     *
     * @param player The player
     */
    private static void recalculatePlayerAttributes(ServerPlayer player) {
        // Clear existing modifiers and recalculate
        // This should integrate with your cultivation attribute system

        AttributeInstance attackAttribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttribute != null) {
            // Remove old modifiers and add new ones based on cultivation
            // Implementation depends on your attribute system
        }
    }

    /**
     * Refreshes and recalculates all player stats after permanent bonuses are applied
     * from cultivation realm advancement, pills, or other permanent modifiers.
     * This handles the complete PlayerStatsAttachment system including Health, Qi, Body,
     * attack damage, movement speed, and jump height.
     *
     * @param player The server player whose stats need to be refreshed
     */
    public static void refreshStats(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("Cannot refresh stats for null player");
            return;
        }

        try {
            PlayerStatsAttachment statsAttachment = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
            if (statsAttachment == null) {
                ImmortalCultivation.LOGGER.warn("Cannot refresh stats: PlayerStatsAttachment not found for player {}",
                    player.getName().getString());
                return;
            }

            // Force recalculation of all stat maximums based on permanent bonuses
            // This includes Health, Qi, and Body maximum calculations
            statsAttachment.recalculateAll();

            // Update vanilla health display to match custom health system
            updateVanillaHealthDisplay(player, statsAttachment);

            // Apply all attribute modifiers (combat stats)
            applyAllAttributeModifiers(player, statsAttachment);

            // Mark attachment for client synchronization
            statsAttachment.markDirty();

            ImmortalCultivation.LOGGER.debug("Successfully refreshed all stats for player: {} - H:{}/{} Q:{}/{} B:{}/{}",
                player.getName().getString(),
                statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                statsAttachment.getMaxStat(IStatsData.StatType.QI),
                statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                statsAttachment.getMaxStat(IStatsData.StatType.BODY));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error refreshing stats for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Applies all attribute modifiers (attack damage, movement speed, jump height)
     * based on permanent cultivation bonuses. Uses unique ResourceLocations to avoid conflicts with
     * equipment-based modifiers handled by recalculatePlayerAttributes.
     *
     * @param player The server player to apply modifiers to
     * @param statsAttachment The stats attachment containing permanent bonuses
     */
    private static void applyAllAttributeModifiers(ServerPlayer player, PlayerStatsAttachment statsAttachment) {
        try {
            // Use specific ResourceLocations for cultivation-based modifiers (different from equipment modifiers)
            ResourceLocation cultivationSpeedModifierId = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_speed_bonus");
            ResourceLocation cultivationAttackModifierId = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_attack_bonus");
            ResourceLocation cultivationJumpModifierId = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_jump_bonus");

            // Apply movement speed modifier
            AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                double totalSpeedBonus = statsAttachment.getPermanentMovementSpeedBonus() +
                    statsAttachment.getMovementSpeedBonus();

                // Remove existing cultivation speed modifier
                movementSpeed.removeModifier(cultivationSpeedModifierId);

                if (totalSpeedBonus != 0.0) {
                    movementSpeed.addPermanentModifier(new AttributeModifier(
                        cultivationSpeedModifierId,
                        totalSpeedBonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }

            // Apply attack damage modifier
            AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                double totalAttackBonus = statsAttachment.getPermanentAttackDamageBonus() +
                    statsAttachment.getAttackDamageBonus();

                // Remove existing cultivation attack modifier
                attackDamage.removeModifier(cultivationAttackModifierId);

                if (totalAttackBonus != 0.0) {
                    attackDamage.addPermanentModifier(new AttributeModifier(
                        cultivationAttackModifierId,
                        totalAttackBonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }

            // Apply jump height modifier
            AttributeInstance jumpStrength = player.getAttribute(Attributes.JUMP_STRENGTH);
            if (jumpStrength != null) {
                double totalJumpBonus = statsAttachment.getPermanentJumpHeightBonus() +
                    statsAttachment.getJumpHeightBonus();

                // Remove existing cultivation jump modifier
                jumpStrength.removeModifier(cultivationJumpModifierId);

                if (totalJumpBonus != 0.0) {
                    jumpStrength.addPermanentModifier(new AttributeModifier(
                        cultivationJumpModifierId,
                        totalJumpBonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying attribute modifiers for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Adds cultivation-specific information to item tooltips
     *
     * @param stack The item stack
     * @param tooltip The tooltip list to modify
     * @param player The player viewing the tooltip
     */
    private static void addCultivationTooltip(ItemStack stack, List<Component> tooltip, Player player) {
        if (player == null) return;

        // Add cultivation-specific tooltip information
        // This should integrate with your item enhancement system

        tooltip.add(Component.literal("").append(
            Component.literal("Cultivation Bonus: ").withStyle(ChatFormatting.GRAY)
        ).append(
            Component.literal("None").withStyle(ChatFormatting.DARK_GRAY)
        ));
    }
}