package net.hyper.immortalcultivation.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.PlayerQiData;
import net.hyper.immortalcultivation.network.QiSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachments;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class PlayerQiComponent implements PlayerQiData {
    private long currentQi = 0;
    private int tickCounter = 0;
    private double qiFraction = 0;
    private long lastSyncedQi = 0;

    public PlayerQiComponent() {}

    // === Player Reference Management ===

    /**
     * Dynamically get the player that owns this component
     * This works around the issue where player references are lost during serialization
     */
    private Player findPlayer() {
        // Try client-side first
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getData(ICModAttachments.PLAYER_QI.get()) == this) {
                return clientPlayer;
            }
        }

        // Try server-side
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (serverPlayer.getData(ICModAttachments.PLAYER_QI.get()) == this) {
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
            ImmortalCultivation.LOGGER.debug("Could not find player for QiComponent");
        }
        return player;
    }

    // === Core Qi Management ===

    @Override
    public long getCurrentQi() {
        return currentQi;
    }

    @Override
    public void setCurrentQi(long qi) {
        long oldQi = this.currentQi;
        this.currentQi = Math.max(0, qi);

        // Sync to client if this is server-side and value changed significantly
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide() && shouldSync()) {
            syncToClient();
        }

        // Call change event
        onQiChanged(oldQi, this.currentQi);
    }

    @Override
    public long getMaxQi() {
        return getBaseMaxQi(); // Can be overridden for modifiers later
    }

    @Override
    public long getBaseMaxQi() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 100; // Safe fallback
        }

        try {
            PlayerRealmComponent realmComponent = player.getData(ICModAttachments.PLAYER_REALM.get());
            if (realmComponent == null || realmComponent.getCurrentRealm() == null) {
                return 100; // Safe fallback
            }
            return realmComponent.getCurrentRealm().getBaseMaxQi();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base max qi", e);
            return 100;
        }
    }

    @Override
    public double getBaseQiRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 0.0; // Safe fallback
        }

        try {
            PlayerRealmComponent realmComponent = player.getData(ICModAttachments.PLAYER_REALM.get());
            if (realmComponent == null || realmComponent.getCurrentRealm() == null) {
                return 0.0; // Safe fallback
            }
            return realmComponent.getCurrentRealm().getBaseQiRegenerationRate();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base qi regeneration rate", e);
            return 0.0;
        }
    }

    @Override
    public double getBaseAcceleratedQiRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 0.0; // Safe fallback
        }

        try {
            PlayerRealmComponent realmComponent = player.getData(ICModAttachments.PLAYER_REALM.get());
            if (realmComponent == null || realmComponent.getCurrentRealm() == null) {
                return 0.0; // Safe fallback
            }
            return realmComponent.getCurrentRealm().getBaseQiRegenerationRate();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base qi regeneration rate", e);
            return 0.0;
        }
    }

    @Override
    public double getEffectiveQiRegenerationRate() {
        // Base implementation - can be overridden for modifiers
        return getBaseQiRegenerationRate();
    }

    // === Qi Operations ===

    @Override
    public long addQi(long amount) {
        long current = getCurrentQi();
        long max = getMaxQi();
        long newAmount = Math.min(current + amount, max);
        setCurrentQi(newAmount);
        long actualAdded = newAmount - current;

        if (actualAdded > 0) {
            onQiAdded(actualAdded);
        }

        return actualAdded;
    }

    @Override
    public boolean consumeQi(long amount) {
        if (getCurrentQi() >= amount) {
            setCurrentQi(getCurrentQi() - amount);
            onQiConsumed(amount);
            return true;
        }
        return false;
    }

    @Override
    public long tryConsumeQi(long amount) {
        long current = getCurrentQi();
        long toConsume = Math.min(current, amount);
        setCurrentQi(current - toConsume);

        if (toConsume > 0) {
            onQiConsumed(toConsume);
        }

        return toConsume;
    }

    @Override
    public boolean hasQi(long amount) {
        return getCurrentQi() >= amount;
    }

    @Override
    public double getQiPercentage() {
        long max = getMaxQi();
        return max > 0 ? (double) getCurrentQi() / max : 0.0;
    }

    @Override
    public boolean isQiFull() {
        return getCurrentQi() >= getMaxQi();
    }

    @Override
    public boolean isQiEmpty() {
        return getCurrentQi() <= 0;
    }

    @Override
    public long getRemainingCapacity() {
        return getMaxQi() - getCurrentQi();
    }

    @Override
    public boolean canRegenerateQi() {
        return getCurrentQi() < getMaxQi();
    }

    // === Regeneration Logic ===

    @Override
    public void tick() {
        tickCounter++;

        Player player = getPlayerSafe();
        if (player == null) return;

        // Only regenerate on server side
        if (!player.level().isClientSide() && tickCounter >= 20) {
            tickCounter = 0;

            if (canRegenerateQi()) {
                try {
                    double regenRate = getEffectiveQiRegenerationRate();
                    qiFraction += regenRate;

                    if (qiFraction >= 1.0) {
                        long qiToAdd = (long) qiFraction;
                        qiFraction -= qiToAdd;
                        addQi(qiToAdd);
                    }
                } catch (Exception e) {
                    ImmortalCultivation.LOGGER.error("Error during qi regeneration tick", e);
                }
            }
        }
    }

    // === Event Callbacks ===

    @Override
    public void onQiChanged(long oldQi, long newQi) {
        // Trigger HUD update on client side
        Player player = getPlayerSafe();
        if (player != null && player.level().isClientSide()) {
            // Show HUD when Qi changes significantly
            long difference = Math.abs(oldQi - newQi);
            if (difference > 5) { // Threshold for showing HUD
                // Force show HUD (you'll need to implement this in your HUD class)
                // QiHUDScreen.forceShow();
            }
        }
    }

    @Override
    public void onQiConsumed(long amount) {
        // Override for custom behavior like particles, sounds, etc.
    }

    @Override
    public void onQiAdded(long amount) {
        // Override for custom behavior like particles, sounds, etc.
    }

    // === Syncing Logic ===

    private boolean shouldSync() {
        long difference = Math.abs(currentQi - lastSyncedQi);
        return difference > 10 || (difference > 0 && tickCounter % 100 == 0);
    }

    private void syncToClient() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedQi = currentQi;
            try {
                ICModNetwork.sendToPlayer(new QiSyncPacket(currentQi), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing Qi to client", e);
            }
        }
    }

    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedQi = currentQi;
            try {
                ICModNetwork.sendToPlayer(new QiSyncPacket(currentQi), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error force syncing Qi to client", e);
            }
        }
    }

    /**
     * Called on client to update from server
     */
    public void updateFromServer(long newQi) {
        this.currentQi = newQi;
        this.lastSyncedQi = newQi;
    }

    // === Debug and Utility Methods ===

    /**
     * Debug method to check component state
     */
    public void debugInfo() {
        Player player = getPlayerSafe();
        if (player == null) {
            ImmortalCultivation.LOGGER.info("Player is null in QiComponent!");
        } else {
            ImmortalCultivation.LOGGER.info("Player found: {} ({})",
                player.getName().getString(),
                player.level().isClientSide() ? "Client" : "Server");
            ImmortalCultivation.LOGGER.info("Current Qi: {}/{} ({}%)",
                getCurrentQi(), getMaxQi(), String.format("%.1f", getQiPercentage() * 100));
            ImmortalCultivation.LOGGER.info("Regen Rate: {}/sec, Qi Fraction: {}",
                getEffectiveQiRegenerationRate(), qiFraction);

            try {
                PlayerRealmComponent realmComponent = player.getData(ICModAttachments.PLAYER_REALM.get());
                if (realmComponent != null && realmComponent.getCurrentRealm() != null) {
                    ImmortalCultivation.LOGGER.info("Current Realm: {}",
                        realmComponent.getCurrentRealm().getDisplayName());
                } else {
                    ImmortalCultivation.LOGGER.info("Realm component or realm is null");
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
     * Check if this component is on the client or server side
     */
    public boolean isClientSide() {
        Player player = getPlayerSafe();
        return player != null && player.level().isClientSide();
    }

    // === Serialization ===

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.currentQi = tag.getLong("qi");
        this.qiFraction = tag.getDouble("qiFraction");
        this.tickCounter = tag.getInt("tickCounter");
        this.lastSyncedQi = tag.getLong("lastSyncedQi");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("qi", currentQi);
        tag.putDouble("qiFraction", qiFraction);
        tag.putInt("tickCounter", tickCounter);
        tag.putLong("lastSyncedQi", lastSyncedQi);
        return tag;
    }

    public static final Codec<PlayerQiComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("qi", 0L).forGetter(PlayerQiComponent::getCurrentQi),
        Codec.DOUBLE.optionalFieldOf("qiFraction", 0.0).forGetter(comp -> comp.qiFraction),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter),
        Codec.LONG.optionalFieldOf("lastSyncedQi", 0L).forGetter(comp -> comp.lastSyncedQi)
    ).apply(instance, (qi, fraction, ticks, lastSync) -> {
        PlayerQiComponent comp = new PlayerQiComponent();
        comp.currentQi = qi;
        comp.qiFraction = fraction;
        comp.tickCounter = ticks;
        comp.lastSyncedQi = lastSync;
        return comp;
    }));

    // === Getter Methods for Internal State (for debugging) ===

    public double getQiFraction() {
        return qiFraction;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public long getLastSyncedQi() {
        return lastSyncedQi;
    }

    // === Legacy Methods (if needed for compatibility) ===

    public void setPlayer(Player player) {
        // This method is kept for compatibility, but the new system
        // doesn't rely on storing the player reference
        ImmortalCultivation.LOGGER.debug("setPlayer called on QiComponent - using dynamic lookup instead");
    }

    public Player getPlayer() {
        return getPlayerSafe();
    }

    public double setBaseQiRegenerationRate(double regenRate) {
        // This method doesn't make sense for this component
        // Regeneration rate comes from the cultivation realm
        ImmortalCultivation.LOGGER.warn("setBaseQiRegenerationRate called - this should be set on the cultivation realm instead");
        return getBaseQiRegenerationRate();
    }
}
