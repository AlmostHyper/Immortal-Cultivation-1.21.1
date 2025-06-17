package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IBodyData;
import net.hyper.immortalcultivation.network.HealthSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class PlayerBodyAttachment implements IBodyData {
    private long currentHealth = 20; // Start with base Minecraft health
    private int tickCounter = 0;
    private double healthFraction = 0;
    private long lastSyncedHealth = 0;

    public PlayerBodyAttachment() {}

    // === Player Reference ===

    /**
     * Dynamically get the player that owns this attachment
     * This works around the issue where player references are lost during serialization
     */
    private Player findPlayer() {
        // Try client-side first
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_BODY.get()) == this) {
                return clientPlayer;
            }
        }

        // Try server-side
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (serverPlayer.getData(ICModAttachmentTypes.PLAYER_BODY.get()) == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    /**
     * Get player with null safety and logging
     */
    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("Could not find player for HealthAttachment");
        }
        return player;
    }

    // === Core Health Methods ===

    @Override
    public long getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public void setCurrentHealth(long health) {
        long oldHealth = this.currentHealth;
        this.currentHealth = Math.max(0, Math.min(health, getMaxHealth()));

        // Sync to client if this is server-side and value changed significantly
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide() && shouldSync()) {
            syncToClient();
        }

        // Check for death/revival events
        if (oldHealth > 0 && this.currentHealth <= 0) {
            onDeath();
        } else if (oldHealth <= 0 && this.currentHealth > 0) {
            onRevive();
        }

        // Call change event
        onHealthChanged(oldHealth, this.currentHealth);
    }

    @Override
    public long getBaseMaxHealth() {
        Player player = getPlayerSafe();
        if (player == null) return 20; // Default Minecraft health

        // Get realm base
        long realmBase = getRealmBaseMaxHealth();

        // Get cultivation bonus
        PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        long cultivationBonus = cultivationAttachment != null ?
            (long) (cultivationAttachment.getCurrentCultivation() * 0.05) : 0; // 5% of cultivation as health bonus

        return realmBase + cultivationBonus;
    }

    @Override
    public long getMaxHealth() {
        return getBaseMaxHealth(); // Can be overridden for additional modifiers
    }

    private long getRealmBaseMaxHealth() {
        Player player = getPlayerSafe();
        if (player == null) return 20;

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null || realmAttachment.getCurrentRealm() == null) {
                return 20; // Default Minecraft health
            }
            return realmAttachment.getCurrentRealm().getBaseHealth();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting realm base max health", e);
            return 20;
        }
    }

    // === Regeneration Methods ===

    @Override
    public double getBaseHealthRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 0.0;
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null || realmAttachment.getCurrentRealm() == null) {
                return 0.0; // No base regeneration for mortals
            }
            return realmAttachment.getCurrentRealm().getBaseHealthRegeneration();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base health regeneration rate", e);
            return 0.0;
        }
    }

    @Override
    public double getBaseAcceleratedHealthRegenerationRate() {
        // Accelerated rate is typically 3x normal rate (like meditation for cultivation)
        return getBaseHealthRegenerationRate() * 3.0;
    }

    @Override
    public double getEffectiveHealthRegenerationRate() {
        double baseRate = getBaseHealthRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) {
            return baseRate;
        }

        try {
            // Get spiritual roots multiplier
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier(); // Reuse cultivation multiplier for health
                return baseRate * multiplier;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting spiritual roots for health regen rate", e);
        }

        return baseRate;
    }

    @Override
    public double getEffectiveAcceleratedHealthRegenerationRate() {
        return getEffectiveHealthRegenerationRate() * 3.0;
    }

    // === HealthRegenerationState Implementation ===

    public double getRegenFraction() {
        return healthFraction;
    }

    public void setRegenFraction(double fraction) {
        this.healthFraction = fraction;
    }

    public void addRegenFraction(double amount) {
        this.healthFraction += amount;
    }

    // === Health Operations ===

    @Override
    public long addHealth(double amount) {
        long current = getCurrentHealth();
        long max = getMaxHealth();
        long newAmount = (long) Math.min(current + amount, max);
        setCurrentHealth(newAmount);
        long actualAdded = newAmount - current;

        if (actualAdded > 0) {
            onHealed(actualAdded);
        }

        return actualAdded;
    }

    @Override
    public boolean damage(long amount) {
        if (getCurrentHealth() > 0) {
            setCurrentHealth(getCurrentHealth() - amount);
            onDamageTaken(amount);
            return true;
        }
        return false;
    }

    @Override
    public long tryHeal(long amount) {
        long current = getCurrentHealth();
        long max = getMaxHealth();
        long toHeal = Math.min(amount, max - current);
        setCurrentHealth(current + toHeal);

        if (toHeal > 0) {
            onHealed(toHeal);
        }

        return toHeal;
    }

    @Override
    public boolean hasHealth(long amount) {
        return getCurrentHealth() >= amount;
    }

    @Override
    public double getHealthPercentage() {
        long max = getMaxHealth();
        return max > 0 ? (double) getCurrentHealth() / max : 0.0;
    }

    public long getRemainingCapacity() {
        return getMaxHealth() - getCurrentHealth();
    }

    // === Regeneration Logic ===

    @Override
    public void tick() {
        tickCounter++;

        Player player = getPlayerSafe();
        if (player == null) return;

        // Only regenerate on server side
        if (!player.level().isClientSide() && tickCounter >= 20) { // Every second
            tickCounter = 0;

            if (canRegenerateHealth()) {
                try {
                    double regenRate = getEffectiveHealthRegenerationRate();
                    healthFraction += regenRate;

                    if (healthFraction >= 1.0) {
                        long healthToAdd = (long) healthFraction;
                        healthFraction -= healthToAdd;
                        addHealth(healthToAdd);
                    }
                } catch (Exception e) {
                    ImmortalCultivation.LOGGER.error("Error during health regeneration tick", e);
                }
            }
        }
    }

    // === Event Callbacks ===

    @Override
    public void onHealthChanged(double oldHealth, double newHealth) {
        // Trigger HUD update on client side
        Player player = getPlayerSafe();
        if (player != null && player.level().isClientSide()) {
            // Show HUD when health changes significantly
            long difference = Math.abs((long)(oldHealth - newHealth));
            if (difference > 1) { // Threshold for showing HUD
                // Force show HUD (you'll need to implement this in your HUD class)
                // HealthHUDScreen.forceShow();
            }
        }
    }

    @Override
    public void onDamageTaken(double amount) {
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} took {} damage", player.getName().getString(), amount);
            // Could add particles, sounds, screen effects here
        }
    }

    @Override
    public void onHealed(double amount) {
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} healed {} health", player.getName().getString(), amount);
            // Could add healing particles, sounds here
        }
    }

    @Override
    public void onDeath() {
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.info("Player {} has died from cultivation injuries", player.getName().getString());
            // Could trigger cultivation-specific death effects
        }
    }

    @Override
    public void onRevive() {
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.info("Player {} has been revived", player.getName().getString());
            // Could trigger revival effects
        }
    }

    // === Syncing ===

    private boolean shouldSync() {
        long difference = Math.abs(currentHealth - lastSyncedHealth);
        return difference > 1 || (difference > 0 && tickCounter % 100 == 0);
    }

    private void syncToClient() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedHealth = currentHealth;
            try {
                ICModNetwork.sendToPlayer(new HealthSyncPacket(currentHealth), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing Health to client", e);
            }
        }
    }

    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedHealth = currentHealth;
            try {
                ICModNetwork.sendToPlayer(new HealthSyncPacket(currentHealth), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error force syncing Health to client", e);
            }
        }
    }

    /**
     * Called on client to update from server
     */
    public void updateFromServer(long newHealth) {
        this.currentHealth = newHealth;
        this.lastSyncedHealth = newHealth;
    }

    // === Debug & Utility ===

    /**
     * Debug method to check Attachment state
     */
    public void debugInfo() {
        Player player = getPlayerSafe();
        if (player == null) {
            ImmortalCultivation.LOGGER.info("Player is null in HealthAttachment!");
        } else {
            ImmortalCultivation.LOGGER.info("Player found: {} ({})",
                player.getName().getString(),
                player.level().isClientSide() ? "Client" : "Server");
            ImmortalCultivation.LOGGER.info("Current Health: {}/{} ({}%)",
                getCurrentHealth(), getMaxHealth(), String.format("%.1f", getHealthPercentage() * 100));
            ImmortalCultivation.LOGGER.info("Regen Rate: {}/sec, Health Fraction: {}",
                getEffectiveHealthRegenerationRate(), healthFraction);
            ImmortalCultivation.LOGGER.info("State: {}, Critically Injured: {}",
                isAlive() ? "Alive" : "Dead", isCriticallyInjured());

            try {
                PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                if (realmAttachment != null && realmAttachment.getCurrentRealm() != null) {
                    ImmortalCultivation.LOGGER.info("Current Realm: {}",
                        realmAttachment.getCurrentRealm().getDisplayName());
                } else {
                    ImmortalCultivation.LOGGER.info("Realm Attachment or realm is null");
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error getting realm info", e);
            }
        }
    }

    /**
     * Get player name for logging (safe)
     */
    public String getPlayerName() {
        Player player = getPlayerSafe();
        return player != null ? player.getName().getString() : "Unknown";
    }

    /**
     * Check if this Attachment is on the client or server side
     */
    public boolean isClientSide() {
        Player player = getPlayerSafe();
        return player != null && player.level().isClientSide();
    }

    // === Serialization ===

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.currentHealth = tag.getLong("health");
        this.healthFraction = tag.getDouble("healthFraction");
        this.tickCounter = tag.getInt("tickCounter");
        this.lastSyncedHealth = tag.getLong("lastSyncedHealth");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("health", currentHealth);
        tag.putDouble("healthFraction", healthFraction);
        tag.putInt("tickCounter", tickCounter);
        tag.putLong("lastSyncedHealth", lastSyncedHealth);
        return tag;
    }

    // === Codec ===

    public static final Codec<PlayerBodyAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("health", 20L).forGetter(PlayerBodyAttachment::getCurrentHealth),
        Codec.DOUBLE.optionalFieldOf("healthFraction", 0.0).forGetter(comp -> comp.healthFraction),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter),
        Codec.LONG.optionalFieldOf("lastSyncedHealth", 20L).forGetter(comp -> comp.lastSyncedHealth)
    ).apply(instance, (health, fraction, ticks, lastSync) -> {
        PlayerBodyAttachment comp = new PlayerBodyAttachment();
        comp.currentHealth = health;
        comp.healthFraction = fraction;
        comp.tickCounter = ticks;
        comp.lastSyncedHealth = lastSync;
        return comp;
    }));

    // === Debugging Getters ===

    public double getHealthFraction() {
        return healthFraction;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public long getLastSyncedHealth() {
        return lastSyncedHealth;
    }
}