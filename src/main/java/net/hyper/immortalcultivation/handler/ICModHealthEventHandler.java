package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerHealthAttachment;
import net.hyper.immortalcultivation.handler.DamageFeedbackHandler;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DEBUG VERSION: ICModHealthEventHandler with extensive logging to diagnose damage interception issues.
 */
public class ICModHealthEventHandler {

    // Track last damage for immunity calculations and feedback
    private static final Map<UUID, Float> lastDamageMap = new ConcurrentHashMap<>();
    private static final Map<UUID, DamageSource> lastDamageSourceMap = new ConcurrentHashMap<>();

    // === DAMAGE INTERCEPTION WITH DEBUG LOGGING ===

    /**
     * DEBUG: Primary damage event handler with extensive logging
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST) // Changed to HIGHEST to ensure we run first
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        // DEBUG: Log every damage event
        ImmortalCultivation.LOGGER.info("=== DAMAGE EVENT START ===");
        ImmortalCultivation.LOGGER.info("Player: {}", player.getName().getString());
        ImmortalCultivation.LOGGER.info("Damage Source: {}", event.getSource().getMsgId());
        ImmortalCultivation.LOGGER.info("Original Damage: {}", event.getContainer().getNewDamage());
        ImmortalCultivation.LOGGER.info("Player Invulnerable Time: {}", player.invulnerableTime);
        ImmortalCultivation.LOGGER.info("Player Vanilla Health: {}/{}", player.getHealth(), player.getMaxHealth());

        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment == null) {
            ImmortalCultivation.LOGGER.error("No health attachment found for player {}!", player.getName().getString());
            return;
        }

        // DEBUG: Log custom health state
        ImmortalCultivation.LOGGER.info("Player Custom Health: {}/{}",
            healthAttachment.getCurrentHealth(), healthAttachment.getMaxHealth());

        float originalDamage = event.getContainer().getNewDamage();
        if (originalDamage <= 0) {
            ImmortalCultivation.LOGGER.info("No damage to apply, exiting");
            ImmortalCultivation.LOGGER.info("=== DAMAGE EVENT END ===");
            return;
        }

        // Store damage source for feedback system
        lastDamageSourceMap.put(player.getUUID(), event.getSource());

        // DEBUG: Check immunity
        boolean isImmune = false;
        if (player.invulnerableTime > 0) {
            Float lastDamage = lastDamageMap.get(player.getUUID());
            ImmortalCultivation.LOGGER.info("Player has invulnerability time. Last damage: {}", lastDamage);

            if (lastDamage != null && originalDamage <= lastDamage) {
                ImmortalCultivation.LOGGER.info("BLOCKING damage - player is immune");
                isImmune = true;
            } else {
                ImmortalCultivation.LOGGER.info("ALLOWING damage - higher than last damage");
            }
        } else {
            ImmortalCultivation.LOGGER.info("Player has no invulnerability time - damage allowed");
        }

        if (isImmune) {
            ImmortalCultivation.LOGGER.info("=== DAMAGE EVENT END (IMMUNE) ===");
            return;
        }

        // Calculate damage reduction manually
        float finalDamage = calculateDamageWithArmor(originalDamage, player, event.getSource());
        ImmortalCultivation.LOGGER.info("Damage after armor: {} (from {})", finalDamage, originalDamage);

        // Convert and apply through custom system
        long moddedDamage = convertFinalDamageToModded(finalDamage, event, player);
        ImmortalCultivation.LOGGER.info("Converted to modded damage: {}", moddedDamage);

        // CRITICAL: Cancel vanilla damage FIRST
        ImmortalCultivation.LOGGER.info("CANCELLING vanilla damage (setting to 0)");
        event.getContainer().setNewDamage(0.0f);

        // DEBUG: Verify cancellation
        ImmortalCultivation.LOGGER.info("Damage after cancellation: {}", event.getContainer().getNewDamage());

        // Apply custom damage
        long healthBefore = healthAttachment.getCurrentHealth();
        ImmortalCultivation.LOGGER.info("Health before custom damage: {}", healthBefore);

        boolean damageApplied = applyCustomDamageSimple(healthAttachment, moddedDamage, player);

        long healthAfter = healthAttachment.getCurrentHealth();
        ImmortalCultivation.LOGGER.info("Health after custom damage: {}", healthAfter);
        ImmortalCultivation.LOGGER.info("Custom damage applied: {}", damageApplied);
        ImmortalCultivation.LOGGER.info("Actual health lost: {}", healthBefore - healthAfter);

        if (damageApplied) {
            // Set immunity frames
            player.invulnerableTime = 20;
            lastDamageMap.put(player.getUUID(), originalDamage);

            if (isDamageSourceEnvironmental(event.getSource())) {
                player.invulnerableTime = 10;
            }

            ImmortalCultivation.LOGGER.info("Set invulnerability time to: {}", player.invulnerableTime);

            // Damage armor items
            damageArmor(player, originalDamage, event.getSource());

            // Sync vanilla health
            float vanillaHealthBefore = player.getHealth();
            synchronizeVanillaHealthForDamage(player, healthAttachment);
            float vanillaHealthAfter = player.getHealth();

            ImmortalCultivation.LOGGER.info("Vanilla health sync: {} -> {}", vanillaHealthBefore, vanillaHealthAfter);

            checkAndHandleDeath(player, healthAttachment);
            verifyHealthSync(player, healthAttachment);

            ImmortalCultivation.LOGGER.info("SUCCESS: Applied {} modded damage", moddedDamage);
        } else {
            // If custom damage failed, restore vanilla damage
            ImmortalCultivation.LOGGER.error("FAILED to apply custom damage! Restoring vanilla damage");
            event.getContainer().setNewDamage(originalDamage);
        }

        ImmortalCultivation.LOGGER.info("Final vanilla health: {}/{}", player.getHealth(), player.getMaxHealth());
        ImmortalCultivation.LOGGER.info("Final custom health: {}/{}",
            healthAttachment.getCurrentHealth(), healthAttachment.getMaxHealth());
        ImmortalCultivation.LOGGER.info("=== DAMAGE EVENT END ===");
    }

    /**
     * NEW: Check if damage source is environmental (should have shorter immunity)
     */
    private static boolean isDamageSourceEnvironmental(DamageSource damageSource) {
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
     * Calculate damage reduction from armor and enchantments
     */
    private static float calculateDamageWithArmor(float damage, Player player, DamageSource damageSource) {
        if (damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return damage;
        }

        // Calculate armor value
        int armorValue = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armorPiece = player.getItemBySlot(slot);
                if (!armorPiece.isEmpty() && armorPiece.getItem() instanceof net.minecraft.world.item.ArmorItem armorItem) {
                    armorValue += armorItem.getDefense();
                }
            }
        }

        // Apply armor reduction (vanilla formula)
        float armorReduction = Math.min(20, Math.max(armorValue / 5.0F, armorValue - damage / 2.0F)) / 25.0F;
        damage = damage * (1.0F - armorReduction);

        // Apply enchantment protection
        Optional<Holder.Reference<Enchantment>> protectionHolder = player.level().registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getHolder(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
        Optional<Holder.Reference<Enchantment>> fireProtectionHolder = player.level().registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getHolder(net.minecraft.world.item.enchantment.Enchantments.FIRE_PROTECTION);
        Optional<Holder.Reference<Enchantment>> blastProtectionHolder = player.level().registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getHolder(net.minecraft.world.item.enchantment.Enchantments.BLAST_PROTECTION);
        Optional<Holder.Reference<Enchantment>> projectileProtectionHolder = player.level().registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getHolder(net.minecraft.world.item.enchantment.Enchantments.PROJECTILE_PROTECTION);

        int protectionLevel = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armorPiece = player.getItemBySlot(slot);
                if (!armorPiece.isEmpty()) {
                    ItemEnchantments enchantments = armorPiece.get(DataComponents.ENCHANTMENTS);
                    if (enchantments != null) {
                        if (protectionHolder.isPresent()) {
                            protectionLevel += enchantments.getLevel(protectionHolder.get());
                        }

                        if (damageSource.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && fireProtectionHolder.isPresent()) {
                            protectionLevel += enchantments.getLevel(fireProtectionHolder.get());
                        } else if (damageSource.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && blastProtectionHolder.isPresent()) {
                            protectionLevel += enchantments.getLevel(blastProtectionHolder.get());
                        } else if (damageSource.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) && projectileProtectionHolder.isPresent()) {
                            protectionLevel += enchantments.getLevel(projectileProtectionHolder.get());
                        }
                    }
                }
            }
        }

        // Apply protection reduction (each level reduces damage by 4%, max 80% reduction)
        if (protectionLevel > 0) {
            float reduction = Math.min(protectionLevel * 0.04F, 0.8F);
            damage = damage * (1.0F - reduction);
        }

        return damage;
    }

    /**
     * Convert final vanilla damage to modded damage scale
     */
    private static long convertFinalDamageToModded(float finalDamage, LivingIncomingDamageEvent event, Player player) {
        long baseDamage = Math.round(finalDamage);

        try {
            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                float damageMultiplier = Math.max(1.0f, healthAttachment.getMaxHealth() / 200.0f);
                baseDamage = Math.round(baseDamage * damageMultiplier);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating damage multiplier for player {}",
                player.getName().getString(), e);
        }

        return Math.max(1, baseDamage);
    }

    /**
     * DEBUG: Simplified custom damage application with logging
     */
    private static boolean applyCustomDamageSimple(PlayerHealthAttachment healthAttachment, long damage, Player player) {
        if (damage <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to apply damage <= 0: {}", damage);
            return false;
        }

        try {
            ImmortalCultivation.LOGGER.info("Attempting to consume {} health from attachment", damage);
            long oldHealth = healthAttachment.getCurrentHealth();
            boolean success = healthAttachment.consumeHealth(damage);
            long newHealth = healthAttachment.getCurrentHealth();

            ImmortalCultivation.LOGGER.info("consumeHealth() returned: {}", success);
            ImmortalCultivation.LOGGER.info("Health change: {} -> {} (diff: {})", oldHealth, newHealth, oldHealth - newHealth);

            if (success) {
                onCustomDamageApplied(player, damage, oldHealth, newHealth);
                return true;
            } else {
                ImmortalCultivation.LOGGER.warn("consumeHealth() returned false - damage not applied");
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Exception in applyCustomDamageSimple for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }

        return false;
    }

    /**
     * DEBUG: Custom damage applied callback with logging
     */
    private static void onCustomDamageApplied(Player player, long damage, long oldHealth, long newHealth) {
        long healthLost = oldHealth - newHealth;

        ImmortalCultivation.LOGGER.info("onCustomDamageApplied: Player {} lost {} health from {} damage ({}->{})",
            player.getName().getString(), healthLost, damage, oldHealth, newHealth);

        // Apply vanilla-style damage feedback
        DamageSource damageSource = getLastDamageSource(player);
        ImmortalCultivation.LOGGER.info("Applying damage feedback with source: {}", damageSource.getMsgId());

        try {
            DamageFeedbackHandler.applyVanillaDamageFeedback(player, healthLost, damageSource);
            ImmortalCultivation.LOGGER.info("Damage feedback applied successfully");
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying damage feedback: {}", e.getMessage(), e);
        }

        if (healthLost > 50) {
            ImmortalCultivation.LOGGER.info("Player {} took major damage!", player.getName().getString());
        }
    }

    /**
     * Get the last damage source for a player, with fallback
     */
    private static DamageSource getLastDamageSource(Player player) {
        DamageSource source = lastDamageSourceMap.get(player.getUUID());
        if (source == null) {
            source = player.damageSources().generic();
        }
        return source;
    }

    /**
     * NEW: Damage armor items when taking damage
     */
    private static void damageArmor(Player player, float damageAmount, DamageSource damageSource) {
        if (damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        int armorDamage = Math.max(1, (int)(damageAmount / 4.0F));

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armorPiece = player.getItemBySlot(slot);
                if (!armorPiece.isEmpty() && armorPiece.isDamageableItem()) {
                    damageArmorItem(player, armorPiece, armorDamage, slot);
                }
            }
        }
    }

    /**
     * Damage individual armor item
     */
    private static void damageArmorItem(Player player, ItemStack armorItem, int damage, EquipmentSlot slot) {
        try {
            armorItem.hurtAndBreak(damage, player, slot);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error damaging armor item in slot {}", slot, e);
        }
    }

    private static void checkAndHandleDeath(Player player, PlayerHealthAttachment healthAttachment) {
        long currentModdedHealth = healthAttachment.getCurrentHealth();
        float currentVanillaHealth = player.getHealth();

        ImmortalCultivation.LOGGER.debug("Death check - Modded: {}, Vanilla: {}",
            currentModdedHealth, currentVanillaHealth);

        if (currentModdedHealth <= 0) {
            // Player should be dead according to modded system
            ImmortalCultivation.LOGGER.info("Player {} has died (modded health reached zero)",
                player.getName().getString());
            player.setHealth(0.0f); // Trigger vanilla death
        } else if (currentVanillaHealth <= 0.1f && currentModdedHealth > 0) {
            // CRITICAL FIX: Vanilla health is too low but modded health is positive
            // This is the desync issue - fix it by resyncing vanilla health
            ImmortalCultivation.LOGGER.warn("Health desync detected for player {} - resyncing vanilla health",
                player.getName().getString());

            synchronizeVanillaHealthForDamage(player, healthAttachment);

            // Double-check: if vanilla health is still 0 after sync, there's a bigger problem
            if (player.getHealth() <= 0.1f) {
                ImmortalCultivation.LOGGER.error("Failed to sync vanilla health for player {} - forcing to safe value",
                    player.getName().getString());

                // Force vanilla health to safe minimum based on modded health percentage
                long maxModdedHealth = healthAttachment.getMaxHealth();
                if (maxModdedHealth > 0) {
                    float healthPercentage = (float) currentModdedHealth / maxModdedHealth;
                    float safeVanillaHealth = Math.max(0.5f, player.getMaxHealth() * healthPercentage);
                    player.setHealth(safeVanillaHealth);
                }
            }
        }
    }

    private static void verifyHealthSync(Player player, PlayerHealthAttachment healthAttachment) {
        long moddedHealth = healthAttachment.getCurrentHealth();
        float vanillaHealth = player.getHealth();

        // If vanilla health is 0 but modded health is positive, fix it immediately
        if (vanillaHealth <= 0.1f && moddedHealth > 0) {
            ImmortalCultivation.LOGGER.warn("Prevented health desync for player {} immediately after damage",
                player.getName().getString());

            synchronizeVanillaHealthForDamage(player, healthAttachment);

            // Double-check the fix worked
            if (player.getHealth() <= 0.1f) {
                float emergencyHealth = Math.max(1.0f, player.getMaxHealth() * 0.1f);
                player.setHealth(emergencyHealth);
                ImmortalCultivation.LOGGER.error("Emergency health fix applied for player {}",
                    player.getName().getString());
            }
        }
    }

    // Also add this emergency recovery method you can call manually
    public static void emergencyHealthSync(Player player) {
        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment == null) {
            ImmortalCultivation.LOGGER.error("No health attachment found for emergency sync!");
            return;
        }

        long currentModdedHealth = healthAttachment.getCurrentHealth();
        long maxModdedHealth = healthAttachment.getMaxHealth();

        if (maxModdedHealth > 0 && currentModdedHealth > 0) {
            float healthPercentage = (float) currentModdedHealth / maxModdedHealth;
            float targetVanillaHealth = Math.max(1.0f, player.getMaxHealth() * healthPercentage);
            player.setHealth(targetVanillaHealth);

            ImmortalCultivation.LOGGER.info("Emergency health sync completed for {} - set to {} vanilla health",
                player.getName().getString(), targetVanillaHealth);
        }
    }

    // === HEALING INTERCEPTION ===

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
            return;
        }

        float originalHealing = event.getAmount();
        if (originalHealing <= 0) {
            return;
        }

        long moddedHealing = convertVanillaHealingToModded(originalHealing, event, player);
        long actualHealing = applyCustomHealing(healthAttachment, moddedHealing, player);

        if (actualHealing > 0) {
            event.setAmount(0.0f);
            ImmortalCultivation.LOGGER.debug("Applied {} modded healing to player {} (from {} vanilla healing)",
                actualHealing, player.getName().getString(), originalHealing);
        }
    }

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

        try {
            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                float healingBonus = Math.min(2.0f, healthAttachment.getMaxHealth() / 200.0f);
                baseHealing = Math.round(baseHealing * (1.0f + healingBonus));
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating healing bonus for player {}",
                player.getName().getString(), e);
        }

        return baseHealing;
    }

    // === SYNCHRONIZATION UTILITIES ===

    private static void synchronizeVanillaHealthForDamage(Player player, PlayerHealthAttachment healthAttachment) {
        try {
            long currentModdedHealth = healthAttachment.getCurrentHealth();
            long maxModdedHealth = healthAttachment.getMaxHealth();

            if (maxModdedHealth <= 0) {
                return;
            }

            float healthPercentage = (float) currentModdedHealth / maxModdedHealth;
            float maxVanillaHealth = player.getMaxHealth();

            float targetVanillaHealth = Math.min(maxVanillaHealth, maxVanillaHealth * healthPercentage);

            if (currentModdedHealth > 0) {
                targetVanillaHealth = Math.max(0.1f, targetVanillaHealth);
            }

            player.setHealth(targetVanillaHealth);

            ImmortalCultivation.LOGGER.debug("Synced vanilla health for damage: {}% = {}/{} vanilla health (modded: {}/{})",
                String.format("%.1f", healthPercentage * 100), targetVanillaHealth, maxVanillaHealth,
                currentModdedHealth, maxModdedHealth);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error synchronizing vanilla health for damage for player {}",
                player.getName().getString(), e);
        }
    }

    private static void synchronizeVanillaHealthCapped(Player player, PlayerHealthAttachment healthAttachment) {
        try {
            long currentModdedHealth = healthAttachment.getCurrentHealth();
            long maxModdedHealth = healthAttachment.getMaxHealth();

            if (maxModdedHealth <= 0) {
                return;
            }

            float healthPercentage = (float) currentModdedHealth / maxModdedHealth;
            float maxVanillaHealth = player.getMaxHealth();

            float targetVanillaHealth = Math.min(maxVanillaHealth, maxVanillaHealth * healthPercentage);

            if (currentModdedHealth > 0) {
                targetVanillaHealth = Math.max(0.1f, targetVanillaHealth);
            }

            player.setHealth(targetVanillaHealth);

            ImmortalCultivation.LOGGER.debug("Synced capped vanilla health: {}% = {}/{} vanilla health (modded: {}/{})",
                String.format("%.1f", healthPercentage * 100), targetVanillaHealth, maxVanillaHealth,
                currentModdedHealth, maxModdedHealth);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error synchronizing vanilla health for player {}",
                player.getName().getString(), e);
        }
    }

    // === PLAYER LIFECYCLE EVENTS ===

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerHealthAttachment healthAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                synchronizeVanillaHealthCapped(serverPlayer, healthAttachment);
                healthAttachment.forceSync();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerHealthAttachment healthAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                synchronizeVanillaHealthCapped(serverPlayer, healthAttachment);
                healthAttachment.forceSync();
            }
        }
    }

    private static final Map<UUID, Integer> healthCheckCooldown = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Only check every 20 ticks (1 second) to avoid performance issues
        UUID playerId = player.getUUID();
        int cooldown = healthCheckCooldown.getOrDefault(playerId, 0);

        if (cooldown > 0) {
            healthCheckCooldown.put(playerId, cooldown - 1);
            return;
        }

        // Reset cooldown
        healthCheckCooldown.put(playerId, 20);

        // Check for health desync
        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment == null) {
            return;
        }

        long moddedHealth = healthAttachment.getCurrentHealth();
        long maxModdedHealth = healthAttachment.getMaxHealth();
        float vanillaHealth = player.getHealth();
        float maxVanillaHealth = player.getMaxHealth();

        // Skip if player is actually dead
        if (moddedHealth <= 0) {
            return;
        }

        // Check for critical desync
        boolean isDesyncCritical = vanillaHealth <= 0.1f && moddedHealth > 0;
        boolean isDesyncMinor = maxModdedHealth > 0 &&
            Math.abs((vanillaHealth / maxVanillaHealth) - ((float) moddedHealth / maxModdedHealth)) > 0.1f;

        if (isDesyncCritical) {
            ImmortalCultivation.LOGGER.warn("CRITICAL health desync detected for player {} - auto-fixing",
                player.getName().getString());

            // Emergency fix
            float healthPercentage = (float) moddedHealth / maxModdedHealth;
            float targetVanillaHealth = Math.max(1.0f, maxVanillaHealth * healthPercentage);
            player.setHealth(targetVanillaHealth);

            // Reset stuck states
            player.invulnerableTime = 0;
            player.hurtTime = 0;
            player.deathTime = 0;

            // Force sync
            healthAttachment.forceSync();

        } else if (isDesyncMinor) {
            // Minor desync - gentle correction
            synchronizeVanillaHealthCapped(player, healthAttachment);
        }
    }

    // === CLEANUP EVENTS ===

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        lastDamageMap.remove(playerId);
        lastDamageSourceMap.remove(playerId);

        ImmortalCultivation.LOGGER.debug("Cleaned up damage tracking data for player {}",
            event.getEntity().getName().getString());
    }
}