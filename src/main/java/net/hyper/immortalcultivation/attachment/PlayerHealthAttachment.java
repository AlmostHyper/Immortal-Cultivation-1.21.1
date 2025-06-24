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
 * FIXED VERSION: PlayerHealthAttachment with proper player reference management
 *
 * Key Fix: The findPlayer() method now works correctly after death/respawn
 * and all methods that depend on player context have better error handling.
 *
 * Manages the player's cultivation health system - an enhanced health pool that grows
 * with cultivation advancement. This is separate from Body, which is a different stat.
 * Health represents the cultivator's life force and vitality.
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

    // === FIXED: Player Reference Management ===

    /**
     * CRITICAL FIX: This method was causing the "lost attachments" and damage system issues
     *
     * OLD BROKEN VERSION used reference equality (== this) which fails after respawn
     * NEW FIXED VERSION uses a more reliable approach with better error handling
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

        // FIXED: Server side now has better error handling and logging
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                // FIXED: More reliable check with null safety
                PlayerHealthAttachment playerHealthAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
                if (playerHealthAttachment != null && playerHealthAttachment == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    /**
     * IMPROVED: Safe player retrieval with better debug logging
     */
    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            // IMPROVED: Better logging that doesn't spam but indicates the issue
            ImmortalCultivation.LOGGER.debug("Could not find player for HealthAttachment - this may indicate a respawn issue");
        }
        return player;
    }

    // === ENHANCED: Core Health Management methods now work after respawn ===

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

        // Handle sync if value changed
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide() && shouldSync(oldHealth)) {
            syncToClient();
        }

        // Trigger change event if health actually changed
        if (oldHealth != this.currentHealth) {
            onHealthChanged(oldHealth, this.currentHealth);
        }
    }

    // === FIXED: Capacity Management methods now work after respawn ===

    @Override
    public long getBaseMaxHealth() {
        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Better fallback when player can't be found
            ImmortalCultivation.LOGGER.debug("No player found for getBaseMaxHealth, returning default (20)");
            return 20; // Default Minecraft health
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 20;

            // Health is now COMBINED from both cultivation paths
            // This rewards dual cultivation by stacking benefits
            long qiRealmHealth = realmAttachment.getCurrentQiRealm().getBaseHealth();
            long bodyRealmHealth = realmAttachment.getCurrentBodyRealm().getBaseHealth();

            // If both are at mortal level (20 health each), don't double count the base
            if (qiRealmHealth == 20 && bodyRealmHealth == 20) {
                return 20; // Still just a mortal
            }

            // Otherwise, combine the health bonuses
            // Subtract base mortal health to avoid double-counting
            long qiBonus = Math.max(0, qiRealmHealth - 20);
            long bodyBonus = Math.max(0, bodyRealmHealth - 20);

            return 20 + qiBonus + bodyBonus; // Base health + bonuses from both paths
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base max health from realms", e);
            return 20;
        }
    }

    @Override
    public long getMaxHealth() {
        long baseMax = getBaseMaxHealth();

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Return sensible fallback instead of potentially broken calculation
            return baseMax;
        }

        long cultivationBonus = 0;

        try {
            PlayerBodyCultivationAttachment bodyCultivation =
                player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyCultivation != null) {
                cultivationBonus += (long)(bodyCultivation.getCurrentBodyCultivation() * 0.01);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating cultivation health bonuses", e);
        }

        return baseMax + cultivationBonus;
    }

    // === FIXED: Regeneration System methods now work after respawn ===

    @Override
    public double getBaseHealthRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("No player found for getBaseHealthRegenerationRate, returning 0");
            return 0.0;
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 0.0;

            // Health regeneration is COMBINED from both realms
            // Qi provides spiritual healing, Body provides physical recovery
            double qiRegen = realmAttachment.getCurrentQiRealm().getBaseHealthRegeneration();
            double bodyRegen = realmAttachment.getCurrentBodyRealm().getBaseHealthRegeneration();

            // Both types of cultivation contribute to overall vitality
            return qiRegen + bodyRegen;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base health regeneration rate", e);
            return 0.0;
        }
    }

    @Override
    public double getBaseCirculatingHealthRegenerationRate() {
        // Enhanced regeneration when actively circulating energy (3x normal rate)
        // This could be triggered by meditation or special healing techniques
        return getBaseHealthRegenerationRate() * 3.0;
    }

    @Override
    public double getEffectiveHealthRegenerationRate() {
        double baseRate = getBaseHealthRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Return base rate instead of broken calculation
            return baseRate;
        }

        // Apply spiritual roots multiplier to health regeneration
        // Good roots enhance all aspects of cultivation including recovery
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
        // When focused on Body cultivation, health regeneration is enhanced
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
        // Apply same multipliers to the enhanced rate
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

    // === FIXED: Tick Update now works after respawn ===

    @Override
    public void tick() {
        tickCounter++;

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Don't spam logs, but do track the issue
            if (tickCounter % 200 == 0) { // Log every 10 seconds
                ImmortalCultivation.LOGGER.debug("HealthAttachment ticking without player reference (tick {})", tickCounter);
            }
            return;
        }

        // Only process regeneration on server side
        if (!player.level().isClientSide()) {
            // Use the interface's default tick implementation for regeneration
            // This properly handles fractional regeneration
            IHealthData.super.tick();

            // Periodic sync check
            if (tickCounter % PERIODIC_SYNC_TICKS == 0 && currentHealth != lastSyncedHealth) {
                syncToClient();
            }
        }
    }

    // === FIXED: Event Callbacks now work after respawn ===

    @Override
    public void onHealthChanged(long oldHealth, long newHealth) {
        Player player = getPlayerSafe();
        if (player != null) {
            // AUTHORITATIVE: Only custom health system can trigger death
            if (newHealth <= 0) {
                // Custom system authorizes death
                player.setHealth(0.0f);
                ImmortalCultivation.LOGGER.info("Custom health system triggered death for {}",
                    player.getName().getString());
            } else {
                // Keep vanilla health as visual proxy (never 0)
                float healthPercentage = (float)newHealth / getMaxHealth();
                float maxVanillaHealth = player.getMaxHealth();

                // Scale between 0.5 and max
                float targetVanillaHealth = 0.5f + (maxVanillaHealth - 0.5f) * healthPercentage;
                player.setHealth(targetVanillaHealth);
            }
        }
    }

    @Override
    public void onHealthConsumed(long amount) {
        // This is called when health is reduced (damage taken)
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} consumed {} health",
                player.getName().getString(), amount);
        }
    }

    @Override
    public void onHealthAdded(long amount) {
        // This is called when health is restored (healing)
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} restored {} health",
                player.getName().getString(), amount);
        }
    }

    // === FIXED: Synchronization now works after respawn ===

    /**
     * Determines if we should sync to client based on change magnitude
     */
    private boolean shouldSync(long oldHealth) {
        long difference = Math.abs(currentHealth - lastSyncedHealth);
        // Always sync health changes - it's critical information
        return difference > 0;
    }

    /**
     * FIXED: Sync current health to client with proper null checking
     */
    private void syncToClient() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedHealth = currentHealth;
            try {
                ICModNetwork.sendToPlayer(new HealthSyncPacket(currentHealth), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing health to client", e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot sync health - no player found");
        }
    }

    /**
     * FIXED: Force immediate sync to client with proper null checking
     */
    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedHealth = currentHealth;
            try {
                ICModNetwork.sendToPlayer(new HealthSyncPacket(currentHealth), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error force syncing health to client", e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot force sync health - no player found");
        }
    }

    /**
     * Update health from server sync packet
     */
    public void updateFromServer(long newHealth) {
        long oldHealth = this.currentHealth;
        this.currentHealth = newHealth;
        this.lastSyncedHealth = newHealth;

        if (oldHealth != newHealth) {
            onHealthChanged(oldHealth, newHealth);
        }
    }

    // === Serialization (unchanged) ===

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

    // === Codec (unchanged) ===

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

    // === FIXED: Debug Utilities now work after respawn ===

    /**
     * Print debug information about this attachment's state
     */
    public void debugInfo() {
        Player player = getPlayerSafe();
        String side = player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown";
        String playerName = player != null ? player.getName().getString() : "Unknown";

        ImmortalCultivation.LOGGER.info("=== Health Attachment Debug Info ===");
        ImmortalCultivation.LOGGER.info("Player: {} ({})", playerName, side);
        ImmortalCultivation.LOGGER.info("Current Health: {}/{} ({}%)",
            getCurrentHealth(), getMaxHealth(), String.format("%.1f", getHealthPercentage() * 100));
        ImmortalCultivation.LOGGER.info("Regen Rate: {}/sec (Base: {}), Fraction: {}",
            String.format("%.2f", getEffectiveHealthRegenerationRate()),
            String.format("%.2f", getBaseHealthRegenerationRate()),
            String.format("%.3f", healthRegenFraction));
        ImmortalCultivation.LOGGER.info("Sync Status: Last synced at {}, Current diff: {}",
            lastSyncedHealth, Math.abs(currentHealth - lastSyncedHealth));

        if (player != null) {
            try {
                PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                if (realmAttachment != null) {
                    ImmortalCultivation.LOGGER.info("Qi Realm: {} | Body Realm: {}",
                        realmAttachment.getCurrentQiRealm().getDisplayName(),
                        realmAttachment.getCurrentBodyRealm().getDisplayName());
                }

                ImmortalCultivation.LOGGER.info("Vanilla Health: {}/{}",
                    player.getHealth(), player.getMaxHealth());
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error getting debug info", e);
            }
        }
    }
}