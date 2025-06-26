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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles application of combat stats bonuses to player attributes.
 * Manages attack damage, movement speed, and jump height bonuses from cultivation.
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

        // Force a sync after a short delay to ensure everything is ready
        player.server.execute(() -> {
            try {
                Thread.sleep(100); // Wait for other attachments to initialize
                forceSyncCombatStats(player);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                forceSyncCombatStats(player); // Sync anyway
            }
        });

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

        // Reapply combat stats modifiers after respawn
        applyCombatStatsModifiers(player);

        // Force sync combat stats
        PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        if (combatStats != null) {
            combatStats.forceSync();
        }

        ImmortalCultivation.LOGGER.info("Combat stats reset for player {} on respawn",
            player.getName().getString());
    }

    // === PERIODIC TICK UPDATES ===

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Only update every 20 ticks (1 second) to avoid performance issues
        if (player.tickCount % 20 != 0) {
            return;
        }

        try {
            // Tick the combat stats attachment
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats != null) {
                combatStats.tick();
            }

            // Update combat stats modifiers every 5 seconds to account for realm changes
            if (player.tickCount % 100 == 0) {
                updateCombatStatsModifiers(player);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking combat stats for player {}",
                player.getName().getString(), e);
        }
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
            ImmortalCultivation.LOGGER.error("Error initializing combat stats for player {}",
                player.getName().getString(), e);
        }
    }

    // === MODIFIER APPLICATION METHODS ===

    private static void applyCombatStatsModifiers(ServerPlayer player) {
        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats == null) {
                ImmortalCultivation.LOGGER.warn("No combat stats attachment found for player {}",
                    player.getName().getString());
                return;
            }

            // Apply attack damage modifier
            applyAttackDamageModifier(player, combatStats);

            // Apply movement speed modifier
            applyMovementSpeedModifier(player, combatStats);

            // Apply jump height modifier
            applyJumpHeightModifier(player, combatStats);

            ImmortalCultivation.LOGGER.debug("Applied all combat stats modifiers for player {}",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying combat stats modifiers for player {}",
                player.getName().getString(), e);
        }
    }

    private static void updateCombatStatsModifiers(ServerPlayer player) {
        try {
            // Remove existing modifiers
            removeCombatStatsModifiers(player);

            // Apply updated modifiers
            applyCombatStatsModifiers(player);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating combat stats modifiers for player {}",
                player.getName().getString(), e);
        }
    }

    private static void removeCombatStatsModifiers(ServerPlayer player) {
        try {
            // Remove attack damage modifier
            AttributeInstance attackAttribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttribute != null) {
                attackAttribute.removeModifier(CULTIVATION_ATTACK_DAMAGE_ID);
            }

            // Remove movement speed modifier
            AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                speedAttribute.removeModifier(CULTIVATION_MOVEMENT_SPEED_ID);
            }

            // Remove jump height modifier (if using step height as proxy)
            AttributeInstance stepHeightAttribute = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepHeightAttribute != null) {
                stepHeightAttribute.removeModifier(CULTIVATION_JUMP_HEIGHT_ID);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error removing combat stats modifiers for player {}",
                player.getName().getString(), e);
        }
    }

    // === INDIVIDUAL MODIFIER APPLICATION ===

    private static void applyAttackDamageModifier(ServerPlayer player, PlayerCombatStatsAttachment combatStats) {
        try {
            AttributeInstance attackAttribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttribute == null) {
                ImmortalCultivation.LOGGER.error("Attack attribute is null for player {}!", player.getName().getString());
                return;
            }

            // Remove existing modifier
            attackAttribute.removeModifier(CULTIVATION_ATTACK_DAMAGE_ID);

            // Calculate effective attack damage multiplier
            double multiplier = combatStats.getEffectiveAttackDamageMultiplier();

            if (multiplier > 1.0) {
                // Create and apply new modifier (multiplicative)
                AttributeModifier modifier = new AttributeModifier(
                    CULTIVATION_ATTACK_DAMAGE_ID,
                    multiplier - 1.0, // Convert to additive value
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                attackAttribute.addPermanentModifier(modifier);

                // Debug logging
                ImmortalCultivation.LOGGER.info("Applied attack modifier to {}: base value: {}, total value: {}, modifier: +{}%",
                    player.getName().getString(),
                    attackAttribute.getBaseValue(),
                    attackAttribute.getValue(),
                    (multiplier - 1.0) * 100);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying attack damage modifier for player {}",
                player.getName().getString(), e);
        }
    }

    private static void applyMovementSpeedModifier(ServerPlayer player, PlayerCombatStatsAttachment combatStats) {
        try {
            AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute == null) {
                return;
            }

            // Remove existing modifier
            speedAttribute.removeModifier(CULTIVATION_MOVEMENT_SPEED_ID);

            // Calculate effective movement speed multiplier
            double speedBonus = combatStats.getEffectiveMovementSpeedMultiplier();

            if (speedBonus > 0.0) {
                // Create and apply new modifier (additive to base speed)
                AttributeModifier modifier = new AttributeModifier(
                    CULTIVATION_MOVEMENT_SPEED_ID,
                    speedBonus,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                speedAttribute.addPermanentModifier(modifier);

                ImmortalCultivation.LOGGER.debug("Applied movement speed modifier: +{}% for player {}",
                    String.format("%.1f", speedBonus * 100),
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying movement speed modifier for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Handle actual jump height increases through jump event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST) // Changed to HIGHEST to run first
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        // Accept any living entity, not just ServerPlayer for debugging
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Add debug logging to see if event fires at all
        ImmortalCultivation.LOGGER.info("LivingJumpEvent fired for player: {}", player.getName().getString());

        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats == null) {
                ImmortalCultivation.LOGGER.warn("No combat stats attachment for player: {}", player.getName().getString());
                return;
            }

            // Get the effective jump height multiplier
            double jumpMultiplier = combatStats.getEffectiveJumpHeightMultiplier();

            ImmortalCultivation.LOGGER.info("Jump multiplier for {}: {}", player.getName().getString(), jumpMultiplier);

            if (jumpMultiplier > 0.0) {
                // Get current movement - log it for debugging
                Vec3 currentMovement = player.getDeltaMovement();
                ImmortalCultivation.LOGGER.info("Current movement before jump: X={}, Y={}, Z={}",
                    currentMovement.x, currentMovement.y, currentMovement.z);

                // Try different approaches based on current Y velocity
                double newJumpVelocity;

                if (Math.abs(currentMovement.y) < 0.1) {
                    // If Y velocity is near zero, set it to enhanced jump velocity
                    newJumpVelocity = 0.42 * (1.0 + jumpMultiplier); // Base jump is ~0.42
                } else {
                    // If Y velocity already exists, enhance it
                    newJumpVelocity = currentMovement.y * (1.0 + jumpMultiplier);
                }

                // Update player's movement with enhanced jump
                player.setDeltaMovement(currentMovement.x, newJumpVelocity, currentMovement.z);

                // Log the change
                ImmortalCultivation.LOGGER.info("Applied jump height bonus: {}% (velocity: {} -> {}) for player {}",
                    String.format("%.1f", jumpMultiplier * 100),
                    String.format("%.3f", currentMovement.y),
                    String.format("%.3f", newJumpVelocity),
                    player.getName().getString());

                // Verify the change was applied
                Vec3 newMovement = player.getDeltaMovement();
                ImmortalCultivation.LOGGER.info("Movement after setting: X={}, Y={}, Z={}",
                    newMovement.x, newMovement.y, newMovement.z);
            } else {
                ImmortalCultivation.LOGGER.info("No jump bonus to apply (multiplier: {})", jumpMultiplier);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying jump height bonus for player {}",
                player.getName().getString(), e);
        }
    }

    private static void applyJumpHeightModifier(ServerPlayer player, PlayerCombatStatsAttachment combatStats) {
        try {
            // Keep step height enhancement as an additional perk (optional)
            AttributeInstance stepHeightAttribute = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepHeightAttribute == null) {
                return;
            }

            // Remove existing modifier
            stepHeightAttribute.removeModifier(CULTIVATION_JUMP_HEIGHT_ID);

            // Calculate effective jump height multiplier
            double jumpBonus = combatStats.getEffectiveJumpHeightMultiplier();

            if (jumpBonus > 0.0) {
                // Convert jump height bonus to step height (allows stepping up higher blocks)
                // This is now just a secondary benefit, real jump height is handled by LivingJumpEvent
                double stepHeightBonus = Math.min(jumpBonus * 0.5, 1.0); // Reduced bonus for step height

                AttributeModifier modifier = new AttributeModifier(
                    CULTIVATION_JUMP_HEIGHT_ID,
                    stepHeightBonus,
                    AttributeModifier.Operation.ADD_VALUE
                );

                stepHeightAttribute.addPermanentModifier(modifier);

                ImmortalCultivation.LOGGER.debug("Applied step height modifier: +{} blocks for player {}",
                    String.format("%.1f", stepHeightBonus),
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying step height modifier for player {}",
                player.getName().getString(), e);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH) // Higher priority to run before other mods
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof SwordItem) && !(stack.getItem() instanceof AxeItem)) return;

        // Use the helper method to ensure player reference is set
        PlayerCombatStatsAttachment combatStats = PlayerCombatStatsAttachment.getOrCreate(player);

        // Get the effective attack bonus
        double attackMultiplier = combatStats.getEffectiveAttackDamageMultiplier();
        double bonusPercent = (attackMultiplier - 1.0) * 100;

        // Debug logging to track what's happening
        ImmortalCultivation.LOGGER.debug("Tooltip for {} - Multiplier: {}, Bonus: {}%, Player: {}, ClientSide: {}",
            stack.getDisplayName().getString(), attackMultiplier, bonusPercent,
            player.getName().getString(), player.level().isClientSide());

        if (bonusPercent <= 0.1) { // Small threshold for floating point precision
            return;
        }

        // Get the weapon's base damage
        double baseDamage = 0;
        if (stack.getItem() instanceof SwordItem sword) {
            baseDamage = sword.getTier().getAttackDamageBonus() + 4; // 4 is base player damage
        } else if (stack.getItem() instanceof AxeItem axe) {
            baseDamage = axe.getTier().getAttackDamageBonus() + 6; // 6 is base axe damage
        }

        double totalDamage = baseDamage * attackMultiplier;

        List<Component> tooltip = event.getToolTip();
        boolean foundAndModified = false;

        // Look for the attribute modifiers section with more robust text matching
        boolean inAttributes = false;
        for (int i = 0; i < tooltip.size(); i++) {
            Component component = tooltip.get(i);
            String text = component.getString();

            // Check for attribute section start (more flexible matching)
            if (text.toLowerCase().contains("when in main hand") ||
                text.toLowerCase().contains("mainhand") ||
                text.toLowerCase().contains("when held") ||
                text.contains("When in Main Hand:")) {
                inAttributes = true;
                continue;
            }

            // Look for attack damage line in attributes section
            if (inAttributes && (text.toLowerCase().contains("attack damage") ||
                text.toLowerCase().contains("attack"))) {

                // Try to extract and replace the damage value with more robust regex
                String modifiedText = replaceDamageValue(text, totalDamage);
                if (modifiedText != null && !modifiedText.equals(text)) {
                    // Replace the component with modified text and green color
                    tooltip.set(i, Component.literal(modifiedText).withStyle(ChatFormatting.DARK_GREEN));

                    // Add explanation line
                    tooltip.add(i + 1, Component.literal(
                        String.format("  §7(Base: %.1f + %.0f%% cultivation)", baseDamage, bonusPercent)
                    ).withStyle(ChatFormatting.GRAY));

                    foundAndModified = true;
                    ImmortalCultivation.LOGGER.debug("Successfully modified tooltip for {}: {} -> {}",
                        stack.getDisplayName().getString(), text, modifiedText);
                    break;
                }
            }

            // Exit attributes section if we hit an empty line or different section
            if (inAttributes && (text.trim().isEmpty() ||
                text.toLowerCase().contains("when in off hand") ||
                text.toLowerCase().contains("offhand"))) {
                if (!foundAndModified) {
                    // If we didn't find a damage line to modify, add our own
                    tooltip.add(i, Component.literal(
                        String.format(" %.1f Attack Damage", totalDamage)
                    ).withStyle(ChatFormatting.DARK_GREEN));

                    tooltip.add(i + 1, Component.literal(
                        String.format("  §7(%.1f base + %.0f%% cultivation)", baseDamage, bonusPercent)
                    ).withStyle(ChatFormatting.GRAY));

                    foundAndModified = true;
                    ImmortalCultivation.LOGGER.debug("Added new damage line for {}: {}",
                        stack.getDisplayName().getString(), totalDamage);
                }
                break;
            }
        }

        // If we never found an attributes section, add our own damage info at the end
        if (!foundAndModified) {
            tooltip.add(Component.literal("").withStyle(ChatFormatting.GRAY)); // Empty line
            tooltip.add(Component.literal("When in Main Hand:").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(
                String.format(" %.1f Attack Damage", totalDamage)
            ).withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.literal(
                String.format("  §7(%.1f base + %.0f%% cultivation)", baseDamage, bonusPercent)
            ).withStyle(ChatFormatting.GRAY));

            ImmortalCultivation.LOGGER.debug("Added complete damage section for {}: {}",
                stack.getDisplayName().getString(), totalDamage);
        }
    }


    private static void addCultivationModifier(ItemStack stack, double bonus) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        ListTag modifiers = tag.getList("AttributeModifiers", Tag.TAG_COMPOUND);

        // Remove existing cultivation modifier if present
        modifiers.removeIf(nbt -> {
            CompoundTag compound = (CompoundTag) nbt;
            return compound.getString("Name").equals("cultivation_weapon_bonus");
        });

        // Add new modifier
        CompoundTag modifier = new CompoundTag();
        modifier.putString("AttributeName", "minecraft:generic.attack_damage");
        modifier.putString("Name", "cultivation_weapon_bonus");
        modifier.putDouble("Amount", bonus);
        modifier.putInt("Operation", 1); // MULTIPLY_BASE
        modifier.putLong("UUIDMost", CULTIVATION_WEAPON_MODIFIER_UUID.getMostSignificantBits());
        modifier.putLong("UUIDLeast", CULTIVATION_WEAPON_MODIFIER_UUID.getLeastSignificantBits());
        modifier.putString("Slot", "mainhand");

        modifiers.add(modifier);
        tag.put("AttributeModifiers", modifiers);

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void removesCultivationModifier(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("AttributeModifiers")) {
                ListTag modifiers = tag.getList("AttributeModifiers", Tag.TAG_COMPOUND);
                modifiers.removeIf(nbt -> {
                    CompoundTag compound = (CompoundTag) nbt;
                    return compound.getString("Name").equals("cultivation_weapon_bonus");
                });
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    // === UTILITY METHODS ===

    /**
     * Get debug information about a player's combat stats
     */
    public static String getCombatStatsDebugInfo(ServerPlayer player) {
        StringBuilder debug = new StringBuilder();
        debug.append("=== Combat Stats Debug Info for ").append(player.getName().getString()).append(" ===\n");

        try {
            PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStats != null) {
                debug.append("Attack Damage Bonus: +").append(String.format("%.1f", combatStats.getAttackDamageBonus() * 100)).append("%\n");
                debug.append("Effective Attack Multiplier: ").append(String.format("%.2fx", combatStats.getEffectiveAttackDamageMultiplier())).append("\n");
                debug.append("Movement Speed Bonus: +").append(String.format("%.1f", combatStats.getMovementSpeedBonus() * 100)).append("%\n");
                debug.append("Effective Speed Multiplier: +").append(String.format("%.2f", combatStats.getEffectiveMovementSpeedMultiplier())).append("\n");
                debug.append("Jump Height Bonus: +").append(String.format("%.1f", combatStats.getJumpHeightBonus() * 100)).append("%\n");
                debug.append("Effective Jump Multiplier: +").append(String.format("%.2f", combatStats.getEffectiveJumpHeightMultiplier())).append("\n");
                debug.append("Total Bonus Score: ").append(String.format("%.1f", combatStats.getTotalBonusScore())).append("\n");
            } else {
                debug.append("No combat stats attachment found!\n");
            }

            // Show current attribute values
            AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttr != null) {
                debug.append("Current Attack Damage: ").append(String.format("%.2f", attackAttr.getValue())).append("\n");
            }

            AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                debug.append("Current Movement Speed: ").append(String.format("%.4f", speedAttr.getValue())).append("\n");
            }

            AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepAttr != null) {
                debug.append("Current Step Height: ").append(String.format("%.2f", stepAttr.getValue())).append("\n");
            }

        } catch (Exception e) {
            debug.append("Error gathering debug info: ").append(e.getMessage()).append("\n");
        }

        return debug.toString();
    }

    /**
     * Manually refresh combat stats for a player (useful for commands/debugging)
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

    public static void refreshCombatStatsForRealmChange(ServerPlayer player) {
        try {
            ImmortalCultivation.LOGGER.info("Refreshing combat stats for realm change - player: {}",
                player.getName().getString());

            // Get the combat stats attachment and ensure player reference is set
            PlayerCombatStatsAttachment combatStats = PlayerCombatStatsAttachment.getOrCreate(player);

            // Log current values for debugging
            double oldMultiplier = combatStats.getEffectiveAttackDamageMultiplier();
            ImmortalCultivation.LOGGER.debug("Combat stats before realm change - Attack multiplier: {}", oldMultiplier);

            // Update the attribute modifiers
            updateCombatStatsModifiers(player);

            // Log new values
            double newMultiplier = combatStats.getEffectiveAttackDamageMultiplier();
            ImmortalCultivation.LOGGER.debug("Combat stats after realm change - Attack multiplier: {}", newMultiplier);

            // Force sync to client with some delay to ensure realm data is synced first
            player.server.execute(() -> {
                try {
                    Thread.sleep(50); // Small delay to ensure realm sync completes first
                    combatStats.forceSync();
                    ImmortalCultivation.LOGGER.debug("Combat stats sync sent to client");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    combatStats.forceSync(); // Sync anyway
                }
            });

            ImmortalCultivation.LOGGER.info("Combat stats refreshed after realm change - {} -> {}", oldMultiplier, newMultiplier);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error refreshing combat stats for realm change", e);
        }
    }

    // Helper method for robust damage value replacement
    private static String replaceDamageValue(String text, double newDamage) {
        try {
            // Multiple regex patterns to catch different formats
            String[] patterns = {
                "(\\+?)(\\d+(?:\\.\\d+)?)( Attack Damage)",  // " +7 Attack Damage"
                "(\\+?)(\\d+(?:\\.\\d+)?)( attack damage)",  // " +7 attack damage"
                "(\\d+(?:\\.\\d+)?)( Attack Damage)",        // "7 Attack Damage"
                "(\\d+(?:\\.\\d+)?)( attack damage)",        // "7 attack damage"
                "( )(\\d+(?:\\.\\d+)?)",                     // Just the number if at start of line
            };

            for (String pattern : patterns) {
                if (text.matches(".*" + pattern + ".*")) {
                    String replacement = String.format("%.1f", newDamage);
                    String result = text.replaceFirst(pattern,
                        pattern.contains("Attack Damage") || pattern.contains("attack damage") ?
                            "$1" + replacement + "$3" : // Keep prefix and suffix for full patterns
                            " " + replacement);         // Simple replacement for number-only patterns

                    if (!result.equals(text)) {
                        return result;
                    }
                }
            }

            // Fallback: if no pattern matched, just replace any number
            return text.replaceFirst("\\d+(?:\\.\\d+)?", String.format("%.1f", newDamage));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error replacing damage value in text: {}", text, e);
            return null;
        }
    }

    public static void forceSyncCombatStats(ServerPlayer player) {
        try {
            PlayerCombatStatsAttachment combatStats = PlayerCombatStatsAttachment.getOrCreate(player);
            combatStats.markForSync();
            combatStats.forceSync();
            ImmortalCultivation.LOGGER.debug("Forced combat stats sync for player: {}", player.getName().getString());
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error forcing combat stats sync", e);
        }
    }
}