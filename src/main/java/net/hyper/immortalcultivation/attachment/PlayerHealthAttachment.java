package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IHealthData;
import net.hyper.immortalcultivation.network.HealthSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * ENHANCED COMPATIBILITY VERSION: PlayerHealthAttachment optimized for the Enhanced Health Protection System
 *
 * This version removes all vanilla health synchronization logic since that's now handled
 * by the EnhancedHealthProtectionHandler. This attachment focuses purely on managing
 * the custom health system without interfering with vanilla health.
 *
 * Key Changes:
 * - Removed onHealthChanged vanilla health management
 * - Simplified health operations
 * - Enhanced error handling and logging
 * - Better integration with the protection system
 */
public class PlayerHealthAttachment implements IHealthData {
    // Core health tracking
    private long currentHealth = 20; // Start with base Minecraft health equivalent
    private double healthRegenFraction = 0.0; // Fractional health for smooth regeneration

    // Sync and tick management
    private int tickCounter = 0;
    private long lastSyncedHealth = 20;
    private static final int SYNC_THRESHOLD = 5; // Health changes are important, sync frequently
    private static final int PERIODIC_SYNC_TICKS = 60; // Force sync every 3 seconds

    public PlayerHealthAttachment() {}

    // === ENHANCED: Player Reference Management ===

    /**
     * IMPROVED: More reliable player finding with better error handling
     */
    private Player findPlayer() {
        // On client side, there's only one player - the local player
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            // Check if this client player has this attachment instance
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_HEALTH.get()) == this) {
                return clientPlayer;
            }
        }

        // Server side player lookup with enhanced error handling
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                // More reliable check with null safety
                PlayerHealthAttachment playerHealthAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
                if (playerHealthAttachment != null && playerHealthAttachment == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    /**
     * Safe player retrieval with minimal logging to avoid spam
     */
    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null && tickCounter % 200 == 0) { // Log every 10 seconds max
            ImmortalCultivation.LOGGER.debug("Could not find player for HealthAttachment - this may indicate a respawn issue");
        }
        return player;
    }

    // === CORE HEALTH MANAGEMENT (SIMPLIFIED) ===

    @Override
    public long getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public void setCurrentHealth(long health) {
        long oldHealth = this.currentHealth;
        long maxHealth = getMaxHealth();

        // Clamp between 0 and max
        this.currentHealth = Math.max(0, Math.min(health, maxHealth));

        // Handle sync if value changed significantly
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide() && shouldSync(oldHealth)) {
            syncToClient();
        }

        // Log significant health changes for debugging
        if (Math.abs(oldHealth - this.currentHealth) > 10) {
            ImmortalCultivation.LOGGER.debug("Health changed for player: {} -> {} (max: {})",
                oldHealth, this.currentHealth, maxHealth);
        }

        // NOTE: Vanilla health synchronization is now handled by EnhancedHealthProtectionHandler
        // No need to manage vanilla health here anymore
    }

    @Override
    public long addHealth(long amount) {
        if (amount <= 0) {
            return 0;
        }

        long oldHealth = this.currentHealth;
        long maxHealth = getMaxHealth();
        long newHealth = Math.min(oldHealth + amount, maxHealth);
        long actualHealing = newHealth - oldHealth;

        if (actualHealing > 0) {
            this.currentHealth = newHealth;

            // Sync to client if significant healing
            Player player = getPlayerSafe();
            if (player != null && !player.level().isClientSide() && shouldSync(oldHealth)) {
                syncToClient();
            }

            ImmortalCultivation.LOGGER.debug("Applied {} healing to player (requested {}, actual {})",
                actualHealing, amount, actualHealing);
        }

        return actualHealing;
    }

    @Override
    public long consumeHealth(long amount) {
        if (amount <= 0) {
            return 0;
        }

        long oldHealth = this.currentHealth;
        long newHealth = Math.max(0, oldHealth - amount);
        long actualDamage = oldHealth - newHealth;

        if (actualDamage > 0) {
            this.currentHealth = newHealth;

            // Sync to client for any damage
            Player player = getPlayerSafe();
            if (player != null && !player.level().isClientSide()) {
                syncToClient();
            }

            ImmortalCultivation.LOGGER.debug("Applied {} damage to player (requested {}, actual {})",
                actualDamage, amount, actualDamage);

            // Check for death condition
            if (newHealth <= 0) {
                handleCustomDeath();
            }
        }

        return actualDamage;
    }

    /**
     * NEW: Handle death when custom health reaches 0
     * The EnhancedHealthProtectionHandler will detect this and authorize vanilla death
     */
    private void handleCustomDeath() {
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.info("Custom health system: Player {} has died (health reached 0)",
                player.getName().getString());

            // The EnhancedHealthProtectionHandler will detect this condition
            // and set vanilla health to 0 to trigger proper death

            // Force immediate sync to ensure the protection handler sees the death state
            syncToClient();
        }
    }

    // === CAPACITY MANAGEMENT ===

    @Override
    public long getBaseMaxHealth() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 20; // Default Minecraft health
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 20;

            // Health is COMBINED from both cultivation paths
            long qiRealmHealth = realmAttachment.getCurrentQiRealm().getBaseHealth();
            long bodyRealmHealth = realmAttachment.getCurrentBodyRealm().getBaseHealth();

            // If both are at mortal level (20 health each), don't double count the base
            if (qiRealmHealth == 20 && bodyRealmHealth == 20) {
                return 20; // Still just a mortal
            }

            // Otherwise, add the bonuses from both paths
            long totalBonus = Math.max(0, qiRealmHealth - 20) + Math.max(0, bodyRealmHealth - 20);
            return 20 + totalBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating base max health", e);
            return 20;
        }
    }

    public long getMaxHealthBonus() {
        // Additional health bonuses from other sources (items, effects, etc.)
        return 0; // Implement as needed
    }

    // === REGENERATION SYSTEM ===

    @Override
    public double getBaseHealthRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 0.1; // Default slow regeneration
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) {
                return 0.1;
            }

            // Health regeneration from both cultivation paths
            double qiRegen = realmAttachment.getCurrentQiRealm().getBaseHealthRegeneration();
            double bodyRegen = realmAttachment.getCurrentBodyRealm().getBaseHealthRegeneration();

            // Both cultivation paths contribute to overall vitality
            return qiRegen + bodyRegen;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base health regeneration rate", e);
            return 0.1;
        }
    }

    @Override
    public double getBaseCirculatingHealthRegenerationRate() {
        // Enhanced regeneration when actively circulating energy (3x normal rate)
        return getBaseHealthRegenerationRate() * 3.0;
    }

    @Override
    public double getEffectiveHealthRegenerationRate() {
        double baseRate = getBaseHealthRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) {
            return baseRate;
        }

        // Apply spiritual roots multiplier to health regeneration
        try {
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier();
                baseRate *= multiplier;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying spiritual roots to health regen", e);
        }

        // Apply cultivation focus bonus
        try {
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focus != null) {
                baseRate *= focus.getHealthRegenerationBonus();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus to health regen", e);
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingHealthRegenerationRate() {
        return getEffectiveHealthRegenerationRate() * 3.0;
    }

    @Override
    public double getRegenFraction() {
        return healthRegenFraction;
    }

    @Override
    public void setRegenFraction(double fraction) {
        this.healthRegenFraction = fraction;
    }

    // === TICK PROCESSING ===

    @Override
    public void tick() {
        tickCounter++;

        Player player = getPlayerSafe();
        if (player == null) {
            return; // Can't process without player reference
        }

        // Only process regeneration on server side
        if (!player.level().isClientSide()) {
            // Use the interface's default tick implementation for regeneration
            IHealthData.super.tick();

            // Periodic sync check
            if (tickCounter % PERIODIC_SYNC_TICKS == 0 && currentHealth != lastSyncedHealth) {
                syncToClient();
            }
        }
    }

    // === SYNCHRONIZATION ===

    private boolean shouldSync(long oldHealth) {
        return Math.abs(currentHealth - lastSyncedHealth) >= SYNC_THRESHOLD ||
            (oldHealth != currentHealth && currentHealth <= 0);
    }

    public void syncToClient() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                HealthSyncPacket packet = new HealthSyncPacket(currentHealth);
                ICModNetwork.sendToPlayer(packet, serverPlayer);
                lastSyncedHealth = currentHealth;

                ImmortalCultivation.LOGGER.debug("Synced health {} to client for player {}",
                    currentHealth, serverPlayer.getName().getString());
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing health to client for player {}",
                    player.getName().getString(), e);
            }
        }
    }

    public void forceSync() {
        long oldSynced = lastSyncedHealth;
        lastSyncedHealth = -1; // Force sync by making them different
        syncToClient();
        ImmortalCultivation.LOGGER.debug("Force synced health (was {}, now {})", oldSynced, currentHealth);
    }

    /**
     * Update health from server sync packet
     */
    public void updateFromServer(long newHealth) {
        long oldHealth = this.currentHealth;
        this.currentHealth = newHealth;
        this.lastSyncedHealth = newHealth;

        if (Math.abs(oldHealth - newHealth) > 0) {
            ImmortalCultivation.LOGGER.debug("Health updated from server: {} -> {}", oldHealth, newHealth);
        }
    }

    // === SERIALIZATION ===

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.currentHealth = tag.getLong("health");
        this.healthRegenFraction = tag.getDouble("healthRegenFraction");
        this.tickCounter = tag.getInt("tickCounter");
        this.lastSyncedHealth = tag.getLong("lastSyncedHealth");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("health", currentHealth);
        tag.putDouble("healthRegenFraction", healthRegenFraction);
        tag.putInt("tickCounter", tickCounter);
        tag.putLong("lastSyncedHealth", lastSyncedHealth);
        return tag;
    }

    // === CODEC ===

    public static final Codec<PlayerHealthAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("health", 20L).forGetter(PlayerHealthAttachment::getCurrentHealth),
        Codec.DOUBLE.optionalFieldOf("healthRegenFraction", 0.0).forGetter(comp -> comp.healthRegenFraction),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter),
        Codec.LONG.optionalFieldOf("lastSyncedHealth", 20L).forGetter(comp -> comp.lastSyncedHealth)
    ).apply(instance, (health, fraction, ticks, lastSync) -> {
        PlayerHealthAttachment comp = new PlayerHealthAttachment();
        comp.currentHealth = health;
        comp.healthRegenFraction = fraction;
        comp.tickCounter = ticks;
        comp.lastSyncedHealth = lastSync;
        return comp;
    }));

    // === DEBUG UTILITIES ===

    /**
     * Print debug information about this attachment's state
     */
    public void debugInfo() {
        Player player = getPlayerSafe();
        String side = player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown";
        String playerName = player != null ? player.getName().getString() : "Unknown";

        ImmortalCultivation.LOGGER.info("=== HEALTH DEBUG INFO ===");
        ImmortalCultivation.LOGGER.info("Side: {}", side);
        ImmortalCultivation.LOGGER.info("Player: {}", playerName);
        ImmortalCultivation.LOGGER.info("Current Health: {}/{}", currentHealth, getMaxHealth());
        ImmortalCultivation.LOGGER.info("Regen Fraction: {}", healthRegenFraction);
        ImmortalCultivation.LOGGER.info("Tick Counter: {}", tickCounter);
        ImmortalCultivation.LOGGER.info("Last Synced Health: {}", lastSyncedHealth);
        if (player != null) {
            ImmortalCultivation.LOGGER.info("Vanilla Health: {}/{}", player.getHealth(), player.getMaxHealth());
        }
        ImmortalCultivation.LOGGER.info("=== END DEBUG INFO ===");
    }

    /**
     * Emergency health restoration - sets health to a safe percentage of max
     */
    public void emergencyRestore(double percentage) {
        long maxHealth = getMaxHealth();
        long targetHealth = Math.max(1, Math.round(maxHealth * Math.max(0.1, Math.min(1.0, percentage))));

        long oldHealth = currentHealth;
        this.currentHealth = targetHealth;

        ImmortalCultivation.LOGGER.warn("Emergency health restore: {} -> {} ({}% of max {})",
            oldHealth, targetHealth, percentage * 100, maxHealth);

        // Force sync the emergency restoration
        forceSync();
    }
}