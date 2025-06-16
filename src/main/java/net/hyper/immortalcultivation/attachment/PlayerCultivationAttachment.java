package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.ICultivationData;
import net.hyper.immortalcultivation.network.CultivationSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class PlayerCultivationAttachment implements ICultivationData {
    private long currentCultivation = 0;
    private int tickCounter = 0;
    private double cultivationFraction = 0;
    private long lastSyncedCultivation = 0;
    private static final float BASE_CULTIVATION_RATE = 1.0f;


    // Player Reference
    /**
     * Dynamically get the player that owns this attachment
     * This works around the issue where player references are lost during serialization
     */
    private Player findPlayer() {
        // Try client-side first
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get()) == this) {
                return clientPlayer;
            }
        }

        // Try server-side
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get()) == this) {
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
            ImmortalCultivation.LOGGER.debug("Could not find player for CultivationAttachment");
        }
        return player;
    }

    // Core
    @Override
    public long getCurrentCultivation() {
        return currentCultivation;
    }

    @Override
    public long setCurrentCultivation(long cultivation) {
        long oldCultivation = this.currentCultivation;
        this.currentCultivation = Math.max(0, cultivation);

        // Sync to client if this is server-side and value changed significantly
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide() && shouldSync()) {
            syncToClient();
        }

        // Call change event
        onCultivationChanged(oldCultivation, this.currentCultivation);
        return oldCultivation;
    }

    @Override
    public long getBaseMaxCultivation() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 100; // Safe fallback
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null || realmAttachment.getCurrentRealm() == null) {
                return 100; // Safe fallback
            }
            return realmAttachment.getCurrentRealm().getBaseMaxCultivation();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting max cultivation", e);
            return 100;
        }
    }
    @Override
    public long getMaxCultivation() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 100; // Safe fallback
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null || realmAttachment.getCurrentRealm() == null) {
                return 100; // Safe fallback
            }
            return realmAttachment.getCurrentRealm().getBaseMaxCultivation();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting max cultivation", e);
            return 100;
        }
    }

    @Override
    public double getBaseCultivationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 0.0; // Safe fallback
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null || realmAttachment.getCurrentRealm() == null) {
                return 0.0; // Safe fallback
            }
            return realmAttachment.getCurrentRealm().getBaseCultivationRate();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base cultivation regeneration rate", e);
            return 0.0;
        }
    }

    @Override
    public double getEffectiveCultivationRate() {
        double baseRate = getBaseCultivationRate();

        Player player = getPlayerSafe();
        if (player == null) {
            return baseRate; // Safe fallback
        }

        try {
            // Get spiritual roots multiplier
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier();
                return baseRate * multiplier;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting spiritual roots for cultivation rate", e);
        }

        return baseRate; // Fallback to base rate if spiritual roots unavailable
    }

    @Override
    public double getBaseMeditationCultivationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            return 0.0; // Safe fallback
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null || realmAttachment.getCurrentRealm() == null) {
                return 0.0; // Safe fallback
            }
            return realmAttachment.getCurrentRealm().getBaseCultivationRate();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base cultivation regeneration rate", e);
            return 0.0;
        }
    }

    @Override
    public double getEffectiveMeditationCultivationRate() {
        double baseMeditationRate = getBaseMeditationCultivationRate();

        Player player = getPlayerSafe();
        if (player == null) {
            return baseMeditationRate; // Safe fallback
        }

        try {
            // Get spiritual roots multiplier
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier();
                // Meditation could have an additional bonus multiplier
                float meditationBonus = 1.2f; // 20% bonus for meditation
                return baseMeditationRate * multiplier * meditationBonus;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting spiritual roots for meditation rate", e);
        }

        return baseMeditationRate; // Fallback to base rate if spiritual roots unavailable
    }

    @Override
    public double addCultivationWithSpiritualRoots(long baseAmount) {
        Player player = getPlayerSafe();
        if (player == null) {
            return addCultivation(baseAmount); // Safe fallback
        }

        try {
            // Get spiritual roots multiplier
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier();
                long modifiedAmount = Math.round(baseAmount * multiplier);
                return addCultivation(modifiedAmount);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying spiritual roots to cultivation gain", e);
        }

        return addCultivation(baseAmount); // Fallback to normal addition
    }

    @Override
    public double getRegenFraction() {
        return cultivationFraction;
    }

    @Override
    public void setRegenFraction(double fraction) {
        this.cultivationFraction = fraction;
    }

    @Override
    public void addRegenFraction(double amount) {
        this.cultivationFraction += amount;
    }

    // Cultivation Operations
    @Override
    public long addCultivation(long amount) {
        long current = getCurrentCultivation();
        long max = getMaxCultivation();
        long newAmount = Math.min(current + amount, max);
        setCurrentCultivation(newAmount);
        long actualAdded = newAmount - current;

        if (actualAdded > 0) {
            onCultivationAdded(actualAdded);
        }

        return actualAdded;
    }

    @Override
    public boolean consumeCultivation(long amount) {
        if (getCurrentCultivation() >= amount) {
            setCurrentCultivation(getCurrentCultivation() - amount);
            onCultivationConsumed(amount);
            return true;
        }
        return false;
    }

    @Override
    public long tryConsumeCultivation(long amount) {
        long current = getCurrentCultivation();
        long toConsume = Math.min(current, amount);
        setCurrentCultivation(current - toConsume);

        if (toConsume > 0) {
            onCultivationConsumed(toConsume);
        }

        return toConsume;
    }

    @Override
    public boolean hasCultivation(long amount) {
        return getCurrentCultivation() >= amount;
    }

    @Override
    public double getCultivationPercentage() {
        long max = getMaxCultivation();
        return max > 0 ? (double) getCurrentCultivation() / max : 0.0;
    }

    @Override
    public boolean isCultivationFull() {
        return getCurrentCultivation() >= getMaxCultivation();
    }

    @Override
    public boolean isCultivationEmpty() {
        return getCurrentCultivation() <= 0;
    }

    public long getRemainingCapacity() {
        return getMaxCultivation() - getCurrentCultivation();
    }

    @Override
    public boolean canCultivate() {
        return getCurrentCultivation() < getMaxCultivation();
    }

    // Syncing
    private boolean shouldSync() {
        long difference = Math.abs(currentCultivation - lastSyncedCultivation);
        return difference > 10 || (difference > 0 && tickCounter % 100 == 0);
    }

    private void syncToClient() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedCultivation = currentCultivation;
            try {
                ICModNetwork.sendToPlayer(new CultivationSyncPacket(currentCultivation), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing Cultivation to client", e);
            }
        }
    }

    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedCultivation = currentCultivation;
            try {
                ICModNetwork.sendToPlayer(new CultivationSyncPacket(currentCultivation), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error force syncing Cultivation to client", e);
            }
        }
    }

    /**
     * Called on client to update from server
     */
    public void updateFromServer(long newCultivation) {
        this.currentCultivation = newCultivation;
        this.lastSyncedCultivation = newCultivation;
    }

    // Debug & Utility
    /**
     * Debug method to check Attachment state
     */
    public void debugInfo() {
        Player player = getPlayerSafe();
        if (player == null) {
            ImmortalCultivation.LOGGER.info("Player is null in CultivationAttachment!");
        } else {
            ImmortalCultivation.LOGGER.info("Player found: {} ({})",
                player.getName().getString(),
                player.level().isClientSide() ? "Client" : "Server");
            ImmortalCultivation.LOGGER.info("Current Cultivation: {}/{} ({}%)",
                getCurrentCultivation(), getCurrentCultivation(), String.format("%.1f", getCultivationPercentage() * 100));
            ImmortalCultivation.LOGGER.info("Regen Rate: {}/sec, Cultivation Fraction: {}",
                getEffectiveCultivationRate(), cultivationFraction);

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
        this.currentCultivation = tag.getLong("cultivation");
        this.cultivationFraction = tag.getDouble("cultivationFraction");
        this.tickCounter = tag.getInt("tickCounter");
        this.lastSyncedCultivation = tag.getLong("lastSyncedCultivation");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("cultivation", currentCultivation);
        tag.putDouble("cultivationFraction", cultivationFraction);
        tag.putInt("tickCounter", tickCounter);
        tag.putLong("lastSyncedCultivation", lastSyncedCultivation);
        return tag;
    }

    // Codec
    public static final Codec<PlayerCultivationAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("cultivation", 0L).forGetter(PlayerCultivationAttachment::getCurrentCultivation),
        Codec.DOUBLE.optionalFieldOf("cultivationFraction", 0.0).forGetter(comp -> comp.cultivationFraction),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter),
        Codec.LONG.optionalFieldOf("lastSyncedCultivation", 0L).forGetter(comp -> comp.lastSyncedCultivation)
    ).apply(instance, (cultivation, fraction, ticks, lastSync) -> {
        PlayerCultivationAttachment comp = new PlayerCultivationAttachment();
        comp.currentCultivation = cultivation;
        comp.cultivationFraction = fraction;
        comp.tickCounter = ticks;
        comp.lastSyncedCultivation = lastSync;
        return comp;
    }));

    // Debugging
    public double getCultivationFraction() {
        return cultivationFraction;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public long getLastSyncedCultivation() {
        return lastSyncedCultivation;
    }
}
