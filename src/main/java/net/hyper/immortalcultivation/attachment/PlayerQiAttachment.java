package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IQiData;
import net.hyper.immortalcultivation.network.QiSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class PlayerQiAttachment implements IQiData {
    private long currentQi = 0;
    private int tickCounter = 0;
    private double qiFraction = 0;
    private long lastSyncedQi = 0;

    public PlayerQiAttachment() {}

    //  Player Reference

    /**
     * Dynamically get the player that owns this attachment
     * This works around the issue where player references are lost during serialization
     */
    private Player findPlayer() {
        // Try client-side first
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_QI.get()) == this) {
                return clientPlayer;
            }
        }

        // Try server-side
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (serverPlayer.getData(ICModAttachmentTypes.PLAYER_QI.get()) == this) {
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
            ImmortalCultivation.LOGGER.debug("Could not find player for QiAttachment");
        }
        return player;
    }

    // Core
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
        return getBaseMaxQi();
    }

    @Override
    public long getBaseMaxQi() {
        Player player = getPlayerSafe();
        if (player == null) return 100;

        // Get realm base
        long realmBase = getRealmBaseMaxQi(); // Get from realm

        // Get cultivation bonus
        PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        long cultivationBonus = cultivationAttachment != null ?
            (long) (cultivationAttachment.getCurrentCultivation() * 0.10) : 0;

        return realmBase + cultivationBonus;
    }

    private long getRealmBaseMaxQi() {
        Player player = getPlayerSafe();
        if (player == null) return 100;

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null || realmAttachment.getCurrentRealm() == null) {
                return 100;
            }
            return realmAttachment.getCurrentRealm().getBaseMaxQi();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting realm base max Qi", e);
            return 100;
        }
    }

    @Override
    public double getBaseQiRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 0.0;
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null || realmAttachment.getCurrentRealm() == null) {
                return 0.0; // Safe fallback
            }
            return realmAttachment.getCurrentRealm().getBaseQiRegenerationRate();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base qi regeneration rate", e);
            return 0.0;
        }
    }

    @Override
    public double getBaseCirculatingQiRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 0.0; // Safe fallback
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null || realmAttachment.getCurrentRealm() == null) {
                return 0.0; // Safe fallback
            }
            return realmAttachment.getCurrentRealm().getBaseQiRegenerationRate();
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

    @Override
    public double getRegenFraction() {
        return qiFraction;
    }

    @Override
    public void setRegenFraction(double fraction) {
        this.qiFraction = fraction;
    }

    @Override
    public void addRegenFraction(double amount) {
        this.qiFraction += amount;
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

    // Syncing
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

    // Debug & Utility
    /**
     * Debug method to check Attachment state
     */
    public void debugInfo() {
        Player player = getPlayerSafe();
        if (player == null) {
            ImmortalCultivation.LOGGER.info("Player is null in QiAttachment!");
        } else {
            ImmortalCultivation.LOGGER.info("Player found: {} ({})",
                player.getName().getString(),
                player.level().isClientSide() ? "Client" : "Server");
            ImmortalCultivation.LOGGER.info("Current Qi: {}/{} ({}%)",
                getCurrentQi(), getMaxQi(), String.format("%.1f", getQiPercentage() * 100));
            ImmortalCultivation.LOGGER.info("Regen Rate: {}/sec, Qi Fraction: {}",
                getEffectiveQiRegenerationRate(), qiFraction);

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

    // Serialization
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

    // Codec
    public static final Codec<PlayerQiAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("qi", 0L).forGetter(PlayerQiAttachment::getCurrentQi),
        Codec.DOUBLE.optionalFieldOf("qiFraction", 0.0).forGetter(comp -> comp.qiFraction),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter),
        Codec.LONG.optionalFieldOf("lastSyncedQi", 0L).forGetter(comp -> comp.lastSyncedQi)
    ).apply(instance, (qi, fraction, ticks, lastSync) -> {
        PlayerQiAttachment comp = new PlayerQiAttachment();
        comp.currentQi = qi;
        comp.qiFraction = fraction;
        comp.tickCounter = ticks;
        comp.lastSyncedQi = lastSync;
        return comp;
    }));

    // Debugging
    public double getQiFraction() {
        return qiFraction;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public long getLastSyncedQi() {
        return lastSyncedQi;
    }
}
