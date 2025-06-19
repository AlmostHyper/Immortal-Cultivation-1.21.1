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

    // === Player Reference Management ===

    /**
     * Dynamically finds the player that owns this attachment.
     * This handles the serialization issue where player references are lost.
     */
    private Player findPlayer() {
        // Check client-side first
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_HEALTH.get()) == this) {
                return clientPlayer;
            }
        }

        // Check server-side
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (serverPlayer.getData(ICModAttachmentTypes.PLAYER_HEALTH.get()) == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    /**
     * Safe player retrieval with optional debug logging
     */
    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("Could not find player for HealthAttachment");
        }
        return player;
    }

    // === Core Health Management ===

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

    // === Capacity Management ===

    @Override
    public long getBaseMaxHealth() {
        Player player = getPlayerSafe();
        if (player == null) return 20;

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 20;

            // Health capacity is determined by the highest realm between Qi and Body
            // This represents overall cultivation affecting life force
            long qiRealmHealth = realmAttachment.getCurrentQiRealm().getBaseHealth();
            long bodyRealmHealth = realmAttachment.getCurrentBodyRealm().getBaseHealth();

            // Add both
            return qiRealmHealth + bodyRealmHealth;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base max health from realms", e);
            return 20;
        }
    }

    @Override
    public long getMaxHealth() {
        long baseMax = getBaseMaxHealth();

        Player player = getPlayerSafe();
        if (player == null) return baseMax;

        // Add bonuses from both cultivation paths
        // Both Qi and Body cultivation enhance overall vitality
        long cultivationBonus = 0;

        try {
            // Body cultivation bonus (5% of cultivation as health)
            PlayerBodyCultivationAttachment bodyCultivation =
                player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyCultivation != null) {
                cultivationBonus += (long)(bodyCultivation.getCurrentBodyCultivation() * 0.05);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating cultivation health bonuses", e);
        }

        return baseMax + cultivationBonus;
    }

    // === Regeneration System ===

    @Override
    public double getBaseHealthRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) return 0.0;

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 0.0;

            // Health regeneration comes from both realms
            // Qi provides spiritual healing, Body provides physical recovery
            double qiRegen = realmAttachment.getCurrentQiRealm().getBaseHealthRegeneration();
            double bodyRegen = realmAttachment.getCurrentBodyRealm().getBaseHealthRegeneration();

            // Combine both regeneration rates (they stack)
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
        if (player == null) return baseRate;

        // Apply spiritual roots multiplier to health regeneration
        // Good roots enhance all aspects of cultivation including recovery
        try {
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier();
                return baseRate * multiplier;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying spiritual roots to health regen", e);
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

    // === Tick Update ===

    @Override
    public void tick() {
        tickCounter++;

        Player player = getPlayerSafe();
        if (player == null) return;

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

    // === Event Callbacks ===

    @Override
    public void onHealthChanged(long oldHealth, long newHealth) {
        Player player = getPlayerSafe();
        if (player != null) {
            // Sync with vanilla health system for compatibility
            // This keeps the vanilla health bar somewhat accurate
            float healthPercentage = (float)newHealth / getMaxHealth();
            float targetVanillaHealth = player.getMaxHealth() * healthPercentage;

            // Only update vanilla health if significantly different
            if (Math.abs(player.getHealth() - targetVanillaHealth) > 1.0f) {
                player.setHealth(targetVanillaHealth);
            }

            // Log significant health changes
            long difference = Math.abs(oldHealth - newHealth);
            if (difference > 10) {
                ImmortalCultivation.LOGGER.debug("Player {} health changed by {}: {} -> {}",
                    player.getName().getString(), difference, oldHealth, newHealth);
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

    // === Synchronization ===

    /**
     * Determines if we should sync to client based on change magnitude
     */
    private boolean shouldSync(long oldHealth) {
        long difference = Math.abs(currentHealth - lastSyncedHealth);
        // Always sync health changes - it's critical information
        return difference > 0;
    }

    /**
     * Sync current health to client
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
        }
    }

    /**
     * Force immediate sync to client
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

    // === Serialization ===

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

    // === Codec ===

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

    // === Debug Utilities ===

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
