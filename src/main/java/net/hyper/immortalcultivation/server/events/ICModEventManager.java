package net.hyper.immortalcultivation.server.events;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.*;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.cultivation.realms.JsonCultivationRealm;
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

import java.util.*;
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
     * Handles player respawn events - reapplies realm modifiers and restores stats to full
     *
     * @param event Player respawn event
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Processing respawn for: {}", player.getName().getString());

            // Clear immunity tracking on respawn
            clearPlayerImmunity(player.getUUID());

            // FIRST: Reapply realm modifiers to set up proper max stats
            applyCurrentRealmModifiers(player);

            // SECOND: Get player stats attachment AFTER realm modifiers are applied
            PlayerStatsAttachment statsAttachment = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
            if (statsAttachment != null) {
                // Restore all stats to their proper maximum values
                restoreStatsToFull(player, statsAttachment);

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("Restored stats to full for {}: H:{}/{} Q:{}/{} B:{}/{}",
                        player.getName().getString(),
                        statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                        statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                        statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                        statsAttachment.getMaxStat(IStatsData.StatType.QI),
                        statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                        statsAttachment.getMaxStat(IStatsData.StatType.BODY));
                }
            }

            // THIRD: Synchronize all player data
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
     * Applies current realm modifiers for all cultivation types.
     * This method clears existing REALM bonuses and reapplies them to prevent
     * accumulation of bonuses on player login/respawn, while preserving
     * bonuses from other sources like pills.
     *
     * @param player The player to apply modifiers for
     */
    private static void applyCurrentRealmModifiers(ServerPlayer player) {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Silently reapplying current realm modifiers for player: {}",
                player.getName().getString());
        }

        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        PlayerStatsAttachment stats = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());

        if (cultivation == null || stats == null) {
            ImmortalCultivation.LOGGER.warn("Missing attachments for player {}: cultivation={}, stats={}",
                player.getName().getString(), cultivation != null, stats != null);
            return;
        }

        // CRITICAL FIX: Clear only REALM bonuses, preserving pill and other bonuses
        stats.clearBonusesFromSource(BonusSource.REALM);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Cleared REALM bonuses, preserved other sources (pills, equipment, etc.)");
        }

        // Apply CUMULATIVE realm bonuses for each cultivation type IN SILENT MODE
        for (ICultivationData.CultivationType type : ICultivationData.CultivationType.values()) {
            CultivationRealm realm = cultivation.getRealm(type);
            if (realm != null) {
                // Apply cumulative bonuses from mortal to current realm SILENTLY
                applySilentCumulativeRealmBonuses(player, type, realm, cultivation);

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("Silently reapplied cumulative {} realm modifiers up to: {}",
                        type.getDisplayName(), realm.getDisplayName());
                }
            }
        }

        // Refresh stats to apply changes
        refreshStats(player);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Silent cumulative realm modifier reapplication complete for player: {}",
                player.getName().getString());
        }
    }

    /**
     * Example method for pills to add stat bonuses (if you want pills to affect stats directly).
     * This would go in your pill consumption logic as an alternative or addition to cultivation rate bonuses.
     *
     * @param player The player consuming the pill
     * @param statType The stat type to boost
     * @param bonusAmount The amount of bonus to add
     */
    public static void addPillStatBonus(ServerPlayer player, IStatsData.StatType statType, long bonusAmount) {
        PlayerStatsAttachment stats = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
        if (stats != null && bonusAmount > 0) {
            long currentPillBonus = stats.getSourcedStatBonus(statType, BonusSource.PILL);
            stats.setSourcedStatBonus(statType, BonusSource.PILL, currentPillBonus + bonusAmount);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Added pill stat bonus for {}: +{} {} (total from pills: {})",
                    player.getName().getString(), bonusAmount, statType,
                    stats.getSourcedStatBonus(statType, BonusSource.PILL));
            }
        }
    }

    /**
     * Example method for pills to add regeneration bonuses.
     *
     * @param player The player consuming the pill
     * @param statType The stat type to boost regeneration for
     * @param bonusAmount The regeneration bonus to add
     */
    public static void addPillRegenBonus(ServerPlayer player, IStatsData.StatType statType, double bonusAmount) {
        PlayerStatsAttachment stats = player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
        if (stats != null && bonusAmount > 0.0) {
            double currentPillBonus = stats.getSourcedStatRegenBonus(statType, BonusSource.PILL);
            stats.setSourcedStatRegenBonus(statType, BonusSource.PILL, currentPillBonus + bonusAmount);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Added pill regen bonus for {}: +{} {} regen (total from pills: {})",
                    player.getName().getString(), bonusAmount, statType,
                    stats.getSourcedStatRegenBonus(statType, BonusSource.PILL));
            }
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
     * Updates vanilla health display to match custom health and handles death when appropriate
     *
     * @param player The player
     * @param statsAttachment The stats attachment
     */
    private static void updateVanillaHealthDisplay(Player player, PlayerStatsAttachment statsAttachment) {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Updating vanilla health for {}: customHealth={}/{}",
                player.getName().getString(),
                statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                statsAttachment.getMaxStat(IStatsData.StatType.HEALTH));
        }

        long currentCustomHealth = statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH);
        long maxCustomHealth = statsAttachment.getMaxStat(IStatsData.StatType.HEALTH);

        // Check if player should die
        if (currentCustomHealth <= 0) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Player {} custom health reached 0, triggering death",
                    player.getName().getString());
            }

            // Set vanilla health to 0 to trigger death
            player.setHealth(0.0f);
            lastKnownVanillaHealth.put(player.getUUID(), 0.0f);
            return;
        }

        // Calculate health percentage and update vanilla display
        float percentage = (float) currentCustomHealth / maxCustomHealth;
        float vanillaHealth = percentage * player.getMaxHealth();

        // Ensure minimum of 0.1f when alive (but allow 0.0f for death)
        float adjustedHealth = Math.max(0.1f, vanillaHealth);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Setting vanilla health to {} (percentage: {}, vanillaHealth: {})",
                adjustedHealth, percentage, vanillaHealth);
        }

        player.setHealth(adjustedHealth);
        lastKnownVanillaHealth.put(player.getUUID(), adjustedHealth);
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

            // Apply step height modifier (add this after the jump height section)
            AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepHeight != null) {
                double totalJumpBonus = statsAttachment.getPermanentJumpHeightBonus() +
                    statsAttachment.getJumpHeightBonus();

                // Scale step height with jump bonus (typically smaller multiplier than jump)
                double stepHeightBonus = totalJumpBonus * 0.5; // 50% of jump bonus for step height

                // Remove existing cultivation step height modifier
                ResourceLocation cultivationStepModifierId = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_step_bonus");
                stepHeight.removeModifier(cultivationStepModifierId);

                if (stepHeightBonus != 0.0) {
                    stepHeight.addPermanentModifier(new AttributeModifier(
                        cultivationStepModifierId,
                        stepHeightBonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

                    if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                        ImmortalCultivation.LOGGER.debug("Applied step height modifier: {} for player {}",
                            stepHeightBonus, player.getName().getString());
                    }
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying attribute modifiers for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Applies cumulative realm bonuses silently by simulating natural progression.
     * This method applies each realm's bonuses in sequence from mortal to target realm
     * WITHOUT any advancement messages or notifications.
     * This is the silent version for login reapplication.
     *
     * @param player The player
     * @param cultivationType The cultivation type being reapplied
     * @param targetRealm The target realm to reach
     * @param cultivation The player's cultivation attachment
     */
    private static void applySilentCumulativeRealmBonuses(ServerPlayer player, ICultivationData.CultivationType cultivationType,
                                                          CultivationRealm targetRealm, PlayerCultivationAttachment cultivation) {
        try {
            // Get the progression path from mortal to target realm
            List<CultivationRealm> progressionPath = getRealmProgressionPath(targetRealm);

            if (progressionPath.isEmpty()) {
                ImmortalCultivation.LOGGER.warn("No progression path found for realm: {}", targetRealm.getId());
                return;
            }

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Silently applying cumulative realm bonuses for {} progression to {} ({} realms)",
                    cultivationType.getDisplayName(), targetRealm.getDisplayName(), progressionPath.size());
            }

            // Apply each realm's bonuses in sequence SILENTLY (this simulates natural progression)
            for (CultivationRealm realm : progressionPath) {
                // Set realm temporarily and apply its modifiers SILENTLY
                cultivation.setRealm(cultivationType, realm, true);

                // Apply realm modifiers silently
                if (realm instanceof JsonCultivationRealm jsonRealm) {
                    jsonRealm.applyRealmModifiers(player, cultivation, cultivationType, true);
                }

                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("Silently applied bonuses for realm: {} (Level {})",
                        realm.getDisplayName(), realm.getRealmLevel());
                }
            }

            // Ensure final realm is set correctly
            cultivation.setRealm(cultivationType, targetRealm, true);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Silently applied {} realms in progression to reach {}",
                    progressionPath.size(), targetRealm.getDisplayName());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying silent cumulative realm bonuses for player {} in realm progression: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Gets the complete progression path from mortal realm to the target realm.
     * This uses the realm level system to include ALL realms that should be traversed,
     * including different stages and special realms nested between regular realms.
     *
     * @param targetRealm The target realm
     * @return List of realms in progression order (including target)
     */
    private static List<CultivationRealm> getRealmProgressionPath(CultivationRealm targetRealm) {
        List<CultivationRealm> path = new ArrayList<>();

        try {
            int targetLevel = targetRealm.getRealmLevel();

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Building progression path to realm: {} (Level {})",
                    targetRealm.getId(), targetLevel);
            }

            // Get all available realms as a collection
            Collection<CultivationRealm> allRealms = ICModCultivationRealms.getAllRealms();

            // Create a list of all realms with levels from 1 to target level
            List<CultivationRealm> candidateRealms = new ArrayList<>();
            for (CultivationRealm realm : allRealms) {
                int realmLevel = realm.getRealmLevel();
                if (realmLevel > 0 && realmLevel <= targetLevel) {
                    candidateRealms.add(realm);
                    if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                        ImmortalCultivation.LOGGER.debug("Added candidate realm: {} (Level {})",
                            realm.getId(), realmLevel);
                    }
                }
            }

            // Sort by realm level to ensure proper progression order
            candidateRealms.sort((a, b) -> Integer.compare(a.getRealmLevel(), b.getRealmLevel()));

            // Add all realms in order
            path.addAll(candidateRealms);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Generated complete progression path with {} realms for target: {} (Level {})",
                    path.size(), targetRealm.getId(), targetLevel);

                // Log the complete path for debugging
                StringBuilder pathStr = new StringBuilder("Progression path: ");
                for (int i = 0; i < path.size(); i++) {
                    CultivationRealm realm = path.get(i);
                    pathStr.append(realm.getId()).append("(L").append(realm.getRealmLevel()).append(")");
                    if (i < path.size() - 1) pathStr.append(" -> ");
                }
                ImmortalCultivation.LOGGER.debug(pathStr.toString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error building progression path for realm {}: {}",
                targetRealm.getId(), e.getMessage(), e);
            // Fallback: just add the target realm
            path.add(targetRealm);
        }

        return path;
    }

    /**
     * Restores all player stats (Health, Qi, Body) to their maximum values
     * Used during respawn to give players a fresh start
     *
     * @param player The player whose stats should be restored
     * @param statsAttachment The player's stats attachment
     */
    private static void restoreStatsToFull(ServerPlayer player, PlayerStatsAttachment statsAttachment) {
        try {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Restoring stats to full for {}: Before - H:{}/{} Q:{}/{} B:{}/{}",
                    player.getName().getString(),
                    statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                    statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                    statsAttachment.getMaxStat(IStatsData.StatType.QI),
                    statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                    statsAttachment.getMaxStat(IStatsData.StatType.BODY));
            }

            // Restore Health to maximum using the correct method name
            long maxHealth = statsAttachment.getMaxStat(IStatsData.StatType.HEALTH);
            statsAttachment.setCurrentStat(IStatsData.StatType.HEALTH, maxHealth);

            // Restore Qi to maximum
            long maxQi = statsAttachment.getMaxStat(IStatsData.StatType.QI);
            statsAttachment.setCurrentStat(IStatsData.StatType.QI, maxQi);

            // Restore Body to maximum
            long maxBody = statsAttachment.getMaxStat(IStatsData.StatType.BODY);
            statsAttachment.setCurrentStat(IStatsData.StatType.BODY, maxBody);

            // Update vanilla health display to match restored custom health
            updateVanillaHealthDisplay(player, statsAttachment);

            // Mark for synchronization
            statsAttachment.markDirty();

            ImmortalCultivation.LOGGER.info("Successfully restored all stats to full for player: {} - H:{}/{} Q:{}/{} B:{}/{}",
                player.getName().getString(),
                statsAttachment.getCurrentStat(IStatsData.StatType.HEALTH),
                statsAttachment.getMaxStat(IStatsData.StatType.HEALTH),
                statsAttachment.getCurrentStat(IStatsData.StatType.QI),
                statsAttachment.getMaxStat(IStatsData.StatType.QI),
                statsAttachment.getCurrentStat(IStatsData.StatType.BODY),
                statsAttachment.getMaxStat(IStatsData.StatType.BODY));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error restoring stats to full for player {}: {}",
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