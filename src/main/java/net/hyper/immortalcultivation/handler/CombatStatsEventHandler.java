package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCombatStatsAttachment;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OPTIMIZED: Handles application of combat stats bonuses to player attributes.
 *
 * Key optimizations:
 * 1. Removed redundant tick handler (CultivationTickHandler now handles this)
 * 2. Staggered modifier updates to prevent synchronization spikes
 * 3. Cached last update times to avoid unnecessary recalculations
 * 4. Reduced sync frequency and improved sync timing
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CombatStatsEventHandler {

    // === MODIFIER UUIDs AND IDs ===
    private static final UUID CULTIVATION_ATTACK_DAMAGE_UUID = UUID.fromString("a1b2c3d4-e5f6-1234-5678-9abcdef01234");
    private static final UUID CULTIVATION_MOVEMENT_SPEED_UUID = UUID.fromString("b2c3d4e5-f6a1-2345-6789-abcdef012345");
    private static final UUID CULTIVATION_JUMP_HEIGHT_UUID = UUID.fromString("c3d4e5f6-a1b2-3456-789a-bcdef0123456");
    private static final UUID CULTIVATION_WEAPON_MODIFIER_UUID = UUID.fromString("d1e2f3a4-b5c6-7890-abcd-ef1234567890");

    private static final ResourceLocation CULTIVATION_ATTACK_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_attack_damage");
    private static final ResourceLocation CULTIVATION_MOVEMENT_SPEED_ID = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_movement_speed");
    private static final ResourceLocation CULTIVATION_JUMP_HEIGHT_ID = ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "cultivation_jump_height");

    // Cache for tracking last modifier update times to prevent unnecessary recalculations
    private static final Map<UUID, Long> lastModifierUpdateTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastKnownRealmHash = new ConcurrentHashMap<>();

    // Stagger modifier updates across different intervals to prevent sync spikes
    private static final int MODIFIER_UPDATE_INTERVAL = 200; // Every 10 seconds instead of 5
    private static final int SYNC_CHECK_INTERVAL = 40;       // Check sync every 2 seconds instead of 1

    // === PLAYER LOGIN INITIALIZATION ===

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("Initializing combat stats for player {} on login",
            player.getName().getString());

        // Initialize and apply combat stats with proper player reference
        initializeCombatStatsAttachment(player);
        applyCombatStatsModifiers(player);

        // Schedule a delayed sync to ensure everything is ready
        scheduleDelayedSync(player, 5); // 5 tick delay

        ImmortalCultivation.LOGGER.info("Combat stats initialized for player {}",
            player.getName().getString());
    }

    // === PLAYER RESPAWN HANDLING ===

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("Resetting combat stats for player {} on respawn",
            player.getName().getString());

        // Clear cached data for this player
        clearPlayerCache(player.getUUID());

        // Reapply combat stats modifiers after respawn
        applyCombatStatsModifiers(player);

        // Force sync combat stats
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        if (combatStats != null) {
            scheduleDelayedSync(player, 3); // 3 tick delay for respawn
        }

        ImmortalCultivation.LOGGER.info("Combat stats reset for player {} on respawn",
            player.getName().getString());
    }

    // === OPTIMIZED PERIODIC UPDATES ===
    // NOTE: The main tick handler is now removed since CultivationTickHandler handles it more efficiently

    /**
     * OPTIMIZED: Staggered modifier updates based on realm changes only
     * This is called from CultivationTickHandler at reduced frequency
     */
    public static void updateCombatStatsModifiersIfNeeded(ServerPlayer player) {
        try {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();

            // Calculate current realm hash to detect changes
            int currentRealmHash = calculateRealmHash(player);
            Integer lastRealmHash = lastKnownRealmHash.get(playerId);

            // Only update if realm changed or it's been a while since last update
            Long lastUpdate = lastModifierUpdateTime.get(playerId);
            boolean realmChanged = lastRealmHash == null || !lastRealmHash.equals(currentRealmHash);
            boolean timeForUpdate = lastUpdate == null || (currentTime - lastUpdate) > 30000; // 30 seconds

            if (realmChanged || timeForUpdate) {
                updateCombatStatsModifiers(player);
                lastModifierUpdateTime.put(playerId, currentTime);
                lastKnownRealmHash.put(playerId, currentRealmHash);

                if (realmChanged) {
                    ImmortalCultivation.LOGGER.debug("Updated combat stats modifiers for {} due to realm change",
                        player.getName().getString());
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating combat stats modifiers for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Calculate a hash of the player's current realms to detect changes
     */
    private static int calculateRealmHash(ServerPlayer player) {
        try {
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivation != null) {
                // Create hash based on realm levels
                int qiRealmLevel = cultivation.getCurrentQiRealm().getRealmLevel();
                int bodyRealmLevel = cultivation.getCurrentBodyRealm().getRealmLevel();
                return (qiRealmLevel * 31) + bodyRealmLevel;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating realm hash for player {}",
                player.getName().getString(), e);
        }
        return 0;
    }

    // === INITIALIZATION METHODS ===

    private static void initializeCombatStatsAttachment(ServerPlayer player) {
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

    // === EQUIPMENT CHANGE EVENTS ===

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Handle weapon/equipment changes
            updateWeaponModifiers(player, event.getTo());
        }
    }

    // === TOOLTIP EVENTS ===

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem) {
            addCombatStatsTooltip(stack, tooltip, event.getEntity());
        }
    }

    /**
     * Handle jump height enhancement through LivingJumpEvent
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
                    player.setDeltaMovement(currentMovement.x, enhancedY, currentMovement.z);

                    ImmortalCultivation.LOGGER.debug("Enhanced jump for {}: {}% boost ({} -> {})",
                        player.getName().getString(),
                        String.format("%.1f", jumpMultiplier * 100),
                        String.format("%.3f", currentMovement.y),
                        String.format("%.3f", enhancedY));
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying jump height bonus for player {}",
                player.getName().getString(), e);
        }
    }

    // === UTILITY METHODS ===

    /**
     * Schedule a delayed sync to avoid immediate sync conflicts
     */
    private static void scheduleDelayedSync(ServerPlayer player, int delayTicks) {
        player.server.execute(() -> {
            try {
                // Wait for specified ticks
                Thread.sleep(delayTicks * 50); // 50ms per tick

                PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                if (combatStats != null) {
                    combatStats.forceSync();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Still try to sync even if interrupted
                PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                if (combatStats != null) {
                    combatStats.forceSync();
                }
            }
        });
    }

    /**
     * Clear cached data for a player
     */
    private static void clearPlayerCache(UUID playerId) {
        lastModifierUpdateTime.remove(playerId);
        lastKnownRealmHash.remove(playerId);
    }

    /**
     * Clear cache for player logout
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerCache(player.getUUID());
        }
    }

    // === EXISTING METHODS (preserved for functionality) ===

    /**
     * Manually refresh combat stats for a player (useful for commands/debugging)
     * This is the method you were asking about!
     */
    public static void refreshCombatStats(ServerPlayer player) {
        try {
            ImmortalCultivation.LOGGER.info("Manually refreshing combat stats for player {}",
                player.getName().getString());

            removeCombatStatsModifiers(player);
            applyCombatStatsModifiers(player);

            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats != null) {
                combatStats.forceSync();
            }

            ImmortalCultivation.LOGGER.info("Combat stats refreshed for player {}",
                player.getName().getString());
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error manually refreshing combat stats for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Special refresh method for realm changes with timing considerations
     */
    public static void refreshCombatStatsForRealmChange(ServerPlayer player) {
        try {
            ImmortalCultivation.LOGGER.info("Refreshing combat stats for realm change - player: {}",
                player.getName().getString());

            PlayerCombatStatsAttachment combatStats = PlayerCombatStatsAttachment.getOrCreate(player);
            double oldMultiplier = combatStats.getEffectiveAttackDamageMultiplier();

            updateCombatStatsModifiers(player);

            double newMultiplier = combatStats.getEffectiveAttackDamageMultiplier();

            // Delayed sync to ensure realm data is synced first
            scheduleDelayedSync(player, 3);

            ImmortalCultivation.LOGGER.info("Combat stats refreshed after realm change - {} -> {}",
                oldMultiplier, newMultiplier);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error refreshing combat stats for realm change", e);
        }
    }

    /**
     * Detailed attribute modifier debugging
     */
    public static void debugAttributeModifiers(ServerPlayer player) {
        try {
            ImmortalCultivation.LOGGER.info("=== DETAILED ATTRIBUTE DEBUG FOR {} ===", player.getName().getString());

            // Movement Speed Debug
            AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                ImmortalCultivation.LOGGER.info("MOVEMENT SPEED:");
                ImmortalCultivation.LOGGER.info("  Base Value: {}", speedAttr.getBaseValue());
                ImmortalCultivation.LOGGER.info("  Final Value: {}", speedAttr.getValue());
                ImmortalCultivation.LOGGER.info("  Difference: {}", speedAttr.getValue() - speedAttr.getBaseValue());

                // Check if our modifier exists
                AttributeModifier ourModifier = speedAttr.getModifier(CULTIVATION_MOVEMENT_SPEED_ID);
                if (ourModifier != null) {
                    ImmortalCultivation.LOGGER.info("  Our Modifier: amount={}, operation={}",
                        ourModifier.amount(), ourModifier.operation());
                } else {
                    ImmortalCultivation.LOGGER.warn("  OUR MODIFIER IS MISSING!");
                }

                // List ALL modifiers
                ImmortalCultivation.LOGGER.info("  All Modifiers ({}):", speedAttr.getModifiers().size());
                speedAttr.getModifiers().forEach(modifier -> {
                    ImmortalCultivation.LOGGER.info("    - ID: {}, Amount: {}, Op: {}",
                        modifier.id(), modifier.amount(), modifier.operation());
                });
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error debugging attribute modifiers", e);
        }
    }

    private static void applyCombatStatsModifiers(ServerPlayer player) {
        try {
            PlayerCombatStatsAttachment combatStats = PlayerCombatStatsAttachment.getOrCreate(player);

            // Get effective bonuses
            double attackMultiplier = combatStats.getEffectiveAttackDamageMultiplier();
            double speedMultiplier = combatStats.getEffectiveMovementSpeedMultiplier();
            double jumpMultiplier = combatStats.getEffectiveJumpHeightMultiplier();

            ImmortalCultivation.LOGGER.info("Applying modifiers for {}: ATK={}, SPD={}, JUMP={}",
                player.getName().getString(), attackMultiplier, speedMultiplier, jumpMultiplier);

            // Apply attack damage modifier (this one works)
            AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttr != null) {
                attackAttr.removeModifier(CULTIVATION_ATTACK_DAMAGE_ID);
                if (attackMultiplier != 1.0) {
                    attackAttr.addPermanentModifier(new AttributeModifier(
                        CULTIVATION_ATTACK_DAMAGE_ID,
                        attackMultiplier - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ));
                    ImmortalCultivation.LOGGER.info("Applied attack modifier: +{}%", (attackMultiplier - 1.0) * 100);
                }
            }

            // Apply movement speed modifier with detailed logging
            AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                double oldValue = speedAttr.getValue();
                double baseValue = speedAttr.getBaseValue();

                // Remove existing modifier first
                boolean hadModifier = speedAttr.getModifier(CULTIVATION_MOVEMENT_SPEED_ID) != null;
                speedAttr.removeModifier(CULTIVATION_MOVEMENT_SPEED_ID);

                ImmortalCultivation.LOGGER.info("MOVEMENT SPEED DEBUG for {}:", player.getName().getString());
                ImmortalCultivation.LOGGER.info("  Base Speed: {}", baseValue);
                ImmortalCultivation.LOGGER.info("  Speed Before: {}", oldValue);
                ImmortalCultivation.LOGGER.info("  Had Previous Modifier: {}", hadModifier);
                ImmortalCultivation.LOGGER.info("  Speed Multiplier to Apply: {} ({}%)", speedMultiplier, speedMultiplier * 100);

                if (speedMultiplier > 0.0) {
                    // Calculate absolute speed increase for cumulative bonuses
                    double speedIncrease = baseValue * speedMultiplier;

                    ImmortalCultivation.LOGGER.info("  Calculated Speed Increase: {} (base {} * multiplier {})",
                        speedIncrease, baseValue, speedMultiplier);

                    // Use ADD_VALUE for predictable cumulative results
                    AttributeModifier modifier = new AttributeModifier(
                        CULTIVATION_MOVEMENT_SPEED_ID,
                        speedIncrease,
                        AttributeModifier.Operation.ADD_VALUE
                    );

                    speedAttr.addPermanentModifier(modifier);

                    // Immediately check results
                    double newValue = speedAttr.getValue();
                    AttributeModifier appliedModifier = speedAttr.getModifier(CULTIVATION_MOVEMENT_SPEED_ID);

                    ImmortalCultivation.LOGGER.info("  Speed After Application: {}", newValue);
                    ImmortalCultivation.LOGGER.info("  Actual Speed Change: {}", newValue - baseValue);
                    ImmortalCultivation.LOGGER.info("  Modifier Applied Successfully: {}", appliedModifier != null);

                    if (appliedModifier != null) {
                        ImmortalCultivation.LOGGER.info("  Applied Modifier Details: amount={}, operation={}",
                            appliedModifier.amount(), appliedModifier.operation());
                    }

                    // Check if speed actually changed meaningfully
                    if (Math.abs(newValue - baseValue) < 0.001) {
                        ImmortalCultivation.LOGGER.error("  ERROR: Speed did not change despite modifier application!");
                    } else {
                        ImmortalCultivation.LOGGER.info("  SUCCESS: Speed increased by {}", newValue - baseValue);
                    }
                } else {
                    ImmortalCultivation.LOGGER.info("  No speed bonus to apply (multiplier: {})", speedMultiplier);
                }
            }

            // Apply jump height modifier
            AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepAttr != null) {
                stepAttr.removeModifier(CULTIVATION_JUMP_HEIGHT_ID);
                if (jumpMultiplier > 0.0) {
                    stepAttr.addPermanentModifier(new AttributeModifier(
                        CULTIVATION_JUMP_HEIGHT_ID,
                        jumpMultiplier,
                        AttributeModifier.Operation.ADD_VALUE
                    ));
                    ImmortalCultivation.LOGGER.info("Applied step height modifier: +{}", jumpMultiplier);
                }
            }

            // Detailed debug output
            debugAttributeModifiers(player);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying combat stats modifiers for player {}",
                player.getName().getString(), e);
        }
    }

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

            // Remove jump height modifier
            AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepAttr != null) {
                stepAttr.removeModifier(CULTIVATION_JUMP_HEIGHT_ID);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error removing combat stats modifiers for player {}",
                player.getName().getString(), e);
        }
    }

    private static void updateCombatStatsModifiers(ServerPlayer player) {
        try {
            // Simply reapply all modifiers
            removeCombatStatsModifiers(player);
            applyCombatStatsModifiers(player);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating combat stats modifiers for player {}",
                player.getName().getString(), e);
        }
    }

    private static void updateWeaponModifiers(ServerPlayer player, ItemStack weapon) {
        // Handle weapon-specific modifications if needed
        // This can be implemented based on your specific weapon enhancement system
    }

    private static void addCombatStatsTooltip(ItemStack stack, List<Component> tooltip, Player player) {
        if (player == null) return;
        if (!(stack.getItem() instanceof SwordItem) && !(stack.getItem() instanceof AxeItem)) return;

        PlayerCombatStatsAttachment combatStats = PlayerCombatStatsAttachment.getOrCreate(player);
        double attackBonus = combatStats.getDisplayAttackBonus();
        double bonusPercent = attackBonus * 100;

        if (bonusPercent <= 0.1) return;

        // Calculate attack multiplier for damage calculation
        double attackMultiplier = 1.0 + attackBonus;

        // Get the weapon's base damage
        double baseDamage = 0;
        if (stack.getItem() instanceof SwordItem sword) {
            baseDamage = sword.getTier().getAttackDamageBonus() + 4; // 4 is base player damage
        } else if (stack.getItem() instanceof AxeItem axe) {
            baseDamage = axe.getTier().getAttackDamageBonus() + 6; // 6 is base axe damage
        }

        double totalDamage = baseDamage * attackMultiplier;

        // Look for existing attack damage line in tooltip
        boolean foundAndModified = false;
        for (int i = 0; i < tooltip.size(); i++) {
            Component component = tooltip.get(i);
            String text = component.getString();

            // Look for attack damage line and replace it
            if (text.contains("Attack Damage") && text.contains(" ")) {
                String modifiedText = String.format(" %.1f Attack Damage", totalDamage);
                tooltip.set(i, Component.literal(modifiedText).withStyle(ChatFormatting.DARK_GREEN));

                // Add explanation line
                tooltip.add(i + 1, Component.literal(
                    String.format("  §7(%.1f base + %.0f%% cultivation)", baseDamage, bonusPercent)
                ).withStyle(ChatFormatting.GRAY));

                foundAndModified = true;
                break;
            }
        }

        // If we didn't find an existing line, add our own
        if (!foundAndModified) {
            tooltip.add(Component.literal("").withStyle(ChatFormatting.GRAY)); // Empty line
            tooltip.add(Component.literal("When in Main Hand:").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(
                String.format(" %.1f Attack Damage", totalDamage)
            ).withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.literal(
                String.format("  §7(%.1f base + %.0f%% cultivation)", baseDamage, bonusPercent)
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void forceSyncCombatStats(ServerPlayer player) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats != null) {
                combatStats.forceSync();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error force syncing combat stats for player {}",
                player.getName().getString(), e);
        }
    }
}