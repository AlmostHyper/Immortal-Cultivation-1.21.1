package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerHealthAttachment;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ICModHealthEventHandler {

    // Enchantment resource keys for efficient lookups
    private static final ResourceKey<Enchantment> UNBREAKING_KEY = ResourceKey.create(Registries.ENCHANTMENT,
        ResourceLocation.withDefaultNamespace("unbreaking"));
    private static final ResourceKey<Enchantment> PROTECTION_KEY = ResourceKey.create(Registries.ENCHANTMENT,
        ResourceLocation.withDefaultNamespace("protection"));
    private static final ResourceKey<Enchantment> FIRE_PROTECTION_KEY = ResourceKey.create(Registries.ENCHANTMENT,
        ResourceLocation.withDefaultNamespace("fire_protection"));
    private static final ResourceKey<Enchantment> BLAST_PROTECTION_KEY = ResourceKey.create(Registries.ENCHANTMENT,
        ResourceLocation.withDefaultNamespace("blast_protection"));
    private static final ResourceKey<Enchantment> PROJECTILE_PROTECTION_KEY = ResourceKey.create(Registries.ENCHANTMENT,
        ResourceLocation.withDefaultNamespace("projectile_protection"));

    // Track last damage amounts for immunity checking
    private static final java.util.Map<java.util.UUID, Float> lastDamageMap = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * FIXED: Manual armor calculation that respects vanilla damage immunity
     * This prevents rapid damage from environmental sources like lava, fire, berry bushes
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerIncomingDamageManual(LivingIncomingDamageEvent event) {

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

        float originalDamage = event.getContainer().getNewDamage();
        if (originalDamage <= 0) {
            return;
        }

        // CRITICAL FIX: Check vanilla damage immunity
        // This prevents rapid damage from environmental sources
        if (player.invulnerableTime > 0) {
            // Player is still immune from recent damage
            // Only allow damage if it's significantly higher than the last damage
            Float lastDamage = lastDamageMap.get(player.getUUID());

            // If new damage isn't significantly higher, skip it
            if (lastDamage != null && originalDamage <= lastDamage) {
                ImmortalCultivation.LOGGER.debug("Skipping damage for player {} - still immune (invulnTime: {}, lastDamage: {}, newDamage: {})",
                    player.getName().getString(), player.invulnerableTime, lastDamage, originalDamage);
                return;
            }

            // If new damage is higher, we'll process it but reset immunity timer
            if (lastDamage != null) {
                ImmortalCultivation.LOGGER.debug("Processing higher damage for player {} despite immunity (lastDamage: {}, newDamage: {})",
                    player.getName().getString(), lastDamage, originalDamage);
            }
        }

        // Calculate damage reduction manually
        float finalDamage = calculateDamageWithArmor(originalDamage, player, event.getSource());

        // Convert and apply through custom system
        long moddedDamage = convertFinalDamageToModded(finalDamage, event, player);
        boolean damageApplied = applyCustomDamage(healthAttachment, moddedDamage, event, player);

        if (damageApplied) {
            event.getContainer().setNewDamage(0.0f);

            // CRITICAL FIX: Set vanilla immunity frames to prevent rapid damage
            // This matches vanilla behavior for damage immunity
            player.invulnerableTime = 20; // 20 ticks = 1 second immunity
            lastDamageMap.put(player.getUUID(), originalDamage); // Track this damage amount

            // For certain damage types, use shorter immunity
            if (isDamageSourceEnvironmental(event.getSource())) {
                player.invulnerableTime = 10; // 0.5 seconds for environmental damage
            }

            // NEW: Damage armor items based on the original damage amount
            damageArmor(player, originalDamage, event.getSource());

            // Only sync vanilla health for damage (not healing) to prevent shake effects
            synchronizeVanillaHealthForDamage(player, healthAttachment);
            checkAndHandleDeath(player, healthAttachment);

            ImmortalCultivation.LOGGER.debug("Applied {} modded damage to player {} (from {} vanilla damage), immunity set to {} ticks",
                moddedDamage, player.getName().getString(), originalDamage, player.invulnerableTime);
        }

    }

    /**
     * NEW: Damage armor items when taking damage
     * Armor takes 1 durability damage for every 4 points of damage (before reduction)
     */
    private static void damageArmor(Player player, float damageAmount, DamageSource damageSource) {
        // Don't damage armor if the damage source bypasses armor
        if (damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        // Calculate armor damage (1 durability per 4 damage points)
        int armorDamage = Math.max(1, (int)(damageAmount / 4.0F));

        // Damage each armor piece
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
     * NEW: Damage a specific armor item, respecting Unbreaking enchantment
     */
    private static void damageArmorItem(Player player, ItemStack armorItem, int damage, EquipmentSlot slot) {
        if (armorItem.isEmpty() || !armorItem.isDamageableItem()) {
            return;
        }

        // Check for Unbreaking enchantment
        int unbreakingLevel = getUnbreakingLevel(player, armorItem);

        // Apply Unbreaking chance (chance to not take damage)
        if (unbreakingLevel > 0) {
            // Armor has 60% + 40%/(level+1) chance to not lose durability
            float chance = 0.6F + (0.4F / (float)(unbreakingLevel + 1));
            if (player.getRandom().nextFloat() < chance) {
                return; // Unbreaking prevented the durability loss
            }
        }

        // Actually damage the item
        armorItem.hurtAndBreak(damage, player, slot);

        ImmortalCultivation.LOGGER.debug("Damaged {} in slot {} by {} points (Unbreaking {})",
            armorItem.getItem().getDescriptionId(), slot.getName(), damage, unbreakingLevel);
    }

    /**
     * NEW: Get the Unbreaking enchantment level for NeoForge 1.21.1 using Holders
     */
    private static int getUnbreakingLevel(Player player, ItemStack itemStack) {
        // In 1.21.1, enchantments are stored as a component
        ItemEnchantments enchantments = itemStack.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && player.level() != null) {
            // Get the enchantment holder from the registry
            var registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            var unbreakingHolder = registry.getHolder(UNBREAKING_KEY);

            if (unbreakingHolder.isPresent()) {
                return enchantments.getLevel(unbreakingHolder.get());
            }
        }
        return 0;
    }

    /**
     * Manual armor calculation method (use with HIGH priority approach)
     */
    private static float calculateDamageWithArmor(float originalDamage, Player player, DamageSource damageSource) {
        // If damage bypasses armor, return original damage
        if (damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return originalDamage;
        }

        float damage = originalDamage;

        // Calculate armor reduction
        float armorValue = player.getArmorValue();
        float armorToughness = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);

        // Minecraft's armor calculation formula
        if (armorValue > 0) {
            float f = 2.0F + armorToughness / 4.0F;
            float f1 = Math.max(armorValue - damage / f, armorValue * 0.2F);
            damage = damage * (1.0F - f1 / 25.0F);
        }

        // Calculate enchantment protection
        if (!damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            damage = calculateEnchantmentReduction(damage, player, damageSource);
        }

        // Calculate resistance effect
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE)) {
            int amplifier = player.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE).getAmplifier();
            damage = damage * (1.0F - (amplifier + 1) * 0.2F);
        }

        return Math.max(0, damage);
    }

    /**
     * Calculate enchantment-based damage reduction
     * Updated for NeoForge 1.21.1 enchantment system using Holders
     */
    private static float calculateEnchantmentReduction(float damage, Player player, DamageSource damageSource) {
        if (player.level() == null) {
            return damage;
        }

        // Get the enchantment registry
        var registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        // Get holders for all protection enchantments
        var protectionHolder = registry.getHolder(PROTECTION_KEY);
        var fireProtectionHolder = registry.getHolder(FIRE_PROTECTION_KEY);
        var blastProtectionHolder = registry.getHolder(BLAST_PROTECTION_KEY);
        var projectileProtectionHolder = registry.getHolder(PROJECTILE_PROTECTION_KEY);

        // Get total protection level from all armor pieces
        int protectionLevel = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armorPiece = player.getItemBySlot(slot);
                if (!armorPiece.isEmpty()) {
                    // Get enchantments from the item's components
                    ItemEnchantments enchantments = armorPiece.get(DataComponents.ENCHANTMENTS);
                    if (enchantments != null) {
                        // Add up protection levels from different protection enchantments
                        if (protectionHolder.isPresent()) {
                            protectionLevel += enchantments.getLevel(protectionHolder.get());
                        }

                        // Add specific protections if applicable
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
     * FIXED: Healing interception with NO vanilla health sync to prevent shake effects
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

            // REMOVED: Don't sync vanilla health for healing to prevent shake effects
            // synchronizeVanillaHealth(player, healthAttachment);
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

        // Sync vanilla health to match modded health percentage on login
        synchronizeVanillaHealthCapped(player, healthAttachment);

        // Force sync to client to ensure GUI displays correctly
        healthAttachment.forceSync();

        ImmortalCultivation.LOGGER.debug("Synchronized health for player {} on login: {}/{}",
            player.getName().getString(),
            healthAttachment.getCurrentHealth(),
            healthAttachment.getMaxHealth());
    }

    /**
     * Handle player respawn to fix attachment states
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ImmortalCultivation.LOGGER.info("Player {} respawned, resetting health attachment state",
            player.getName().getString());

        // Clear damage tracking for respawned player
        lastDamageMap.remove(player.getUUID());

        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment != null) {
            // Reset health to full on respawn
            long maxHealth = calculateMaxHealthForPlayer(player);
            healthAttachment.setCurrentHealth(maxHealth);

            // Reset regeneration fraction
            healthAttachment.setRegenFraction(0.0);

            // Sync vanilla health bar to full on respawn
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

    /**
     * Clean up damage tracking when players disconnect
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Clean up damage tracking to prevent memory leaks
            lastDamageMap.remove(player.getUUID());
            ImmortalCultivation.LOGGER.debug("Cleaned up damage tracking for disconnected player: {}",
                player.getName().getString());
        }
    }

    // === DAMAGE CONVERSION AND APPLICATION ===

    /**
     * FIXED: Convert FINAL damage (after armor) to modded damage
     */
    private static long convertFinalDamageToModded(float finalVanillaDamage, LivingIncomingDamageEvent event, Player player) {
        // The damage we receive here has already been reduced by armor/enchantments
        // So we can do a more direct conversion
        long baseDamage = Math.round(finalVanillaDamage);

        // Apply cultivation-based damage reduction (additional to vanilla armor)
        try {
            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                long maxHealth = healthAttachment.getMaxHealth();

                // Higher max health provides additional damage resistance beyond armor
                if (maxHealth > 100) {
                    float cultivationResistance = Math.min(0.3f, (maxHealth - 100) / 2000.0f);
                    baseDamage = Math.round(baseDamage * (1.0f - cultivationResistance));

                    if (cultivationResistance > 0) {
                        ImmortalCultivation.LOGGER.debug("Applied {}% cultivation damage resistance to player {}",
                            String.format("%.1f", cultivationResistance * 100), player.getName().getString());
                    }
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating cultivation damage reduction for player {}",
                player.getName().getString(), e);
        }

        return Math.max(1, baseDamage);
    }

    /**
     * Custom damage application with better error handling
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
     * Custom healing application
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

    /**
     * Synchronize vanilla health for damage events only (prevents healing shake effects)
     */
    private static void synchronizeVanillaHealthForDamage(Player player, PlayerHealthAttachment healthAttachment) {
        try {
            long currentModdedHealth = healthAttachment.getCurrentHealth();
            long maxModdedHealth = healthAttachment.getMaxHealth();

            if (maxModdedHealth <= 0) {
                return;
            }

            float healthPercentage = (float) currentModdedHealth / maxModdedHealth;
            float maxVanillaHealth = player.getMaxHealth();

            // Cap the vanilla health to prevent shake effects
            float targetVanillaHealth = Math.min(maxVanillaHealth, maxVanillaHealth * healthPercentage);

            // Ensure we never set health to exactly 0 unless the player should be dead
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

    /**
     * Synchronize vanilla health for login/respawn events (uses capped approach)
     */
    private static void synchronizeVanillaHealthCapped(Player player, PlayerHealthAttachment healthAttachment) {
        try {
            long currentModdedHealth = healthAttachment.getCurrentHealth();
            long maxModdedHealth = healthAttachment.getMaxHealth();

            if (maxModdedHealth <= 0) {
                return;
            }

            float healthPercentage = (float) currentModdedHealth / maxModdedHealth;
            float maxVanillaHealth = player.getMaxHealth();

            // Cap the vanilla health to prevent shake effects
            float targetVanillaHealth = Math.min(maxVanillaHealth, maxVanillaHealth * healthPercentage);

            // Ensure we never set health to exactly 0 unless the player should be dead
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

    // === CUSTOM DAMAGE LOGIC ===

    /**
     * NEW: Check if damage source is environmental (should have shorter immunity)
     */
    private static boolean isDamageSourceEnvironmental(DamageSource damageSource) {
        // Check for environmental damage types that should have shorter immunity
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
     * ENHANCED: Better damage prevention logic that considers immunity
     */
    private static boolean shouldPreventDamage(Player player, long damage, LivingIncomingDamageEvent event) {
        if (damage <= 0) {
            return true;
        }

        // Additional check: if player has recently taken damage and this is the same or less
        // This is a backup check in case the main immunity check missed something
        if (player.invulnerableTime > 10) {
            Float lastDamage = lastDamageMap.get(player.getUUID());
            if (lastDamage != null && damage <= lastDamage) {
                ImmortalCultivation.LOGGER.debug("Preventing duplicate damage for player {} (lastDamage: {}, newDamage: {})",
                    player.getName().getString(), lastDamage, damage);
                return true;
            }
        }

        // Add custom immunity checks here
        // Example: Check for special damage immunities based on cultivation realm
        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment != null) {
                // High level cultivators might have immunity to certain damage types
                int realmLevel = Math.max(
                    realmAttachment.getCurrentQiRealm().getRealmLevel(),
                    realmAttachment.getCurrentBodyRealm().getRealmLevel()
                );

                // Example: Fire immunity for high level cultivators
                if (realmLevel >= 5 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                    if (damage < 10) { // Only small fire damage is negated
                        ImmortalCultivation.LOGGER.debug("High realm cultivator {} immune to minor fire damage",
                            player.getName().getString());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error checking cultivation immunities", e);
        }

        return false;
    }

    /**
     * DEBUG: Add this method to help debug damage immunity issues
     * Call this when you want to check why damage isn't being applied
     */
    public static void debugDamageImmunity(Player player, DamageSource damageSource, float damage) {
        ImmortalCultivation.LOGGER.info("=== Damage Immunity Debug for {} ===", player.getName().getString());
        ImmortalCultivation.LOGGER.info("Damage Source: {} ({})", damageSource.getMsgId(), damageSource.getClass().getSimpleName());
        ImmortalCultivation.LOGGER.info("Damage Amount: {}", damage);
        ImmortalCultivation.LOGGER.info("Player Invulnerable Time: {} ticks", player.invulnerableTime);

        Float lastDamage = lastDamageMap.get(player.getUUID());
        ImmortalCultivation.LOGGER.info("Last Hurt Amount: {}", lastDamage != null ? lastDamage : "none");
        ImmortalCultivation.LOGGER.info("Is Environmental: {}", isDamageSourceEnvironmental(damageSource));
        ImmortalCultivation.LOGGER.info("Should Skip: {}", (player.invulnerableTime > 0 && lastDamage != null && damage <= lastDamage));

        // Check custom health system
        PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
        if (healthAttachment != null) {
            ImmortalCultivation.LOGGER.info("Current Health: {}/{}",
                healthAttachment.getCurrentHealth(), healthAttachment.getMaxHealth());
        }
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
     * Helper method to safely calculate max health for a player
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

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only tick on server side to avoid double-processing
        if (player.level().isClientSide()) {
            return;
        }

        try {
            // Tick the health attachment for regeneration
            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                healthAttachment.tick();
            }

            // Also tick other cultivation attachments if they have tick methods
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment != null) {
                realmAttachment.tick();
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error ticking player attachments for player: {}",
                player.getName().getString(), e);
        }
    }
}