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

/**
 * Manages the player's current Qi pool - their spiritual energy resource.
 * This is separate from Qi Cultivation, which represents permanent progress.
 * Qi is consumed for abilities and regenerates over time based on realm and cultivation.
 */
public class PlayerQiAttachment implements IQiData {
    // Core Qi tracking
    private long currentQi = 0;
    private double qiRegenFraction = 0.0; // Fractional Qi for smooth regeneration

    // Sync and tick management
    private int tickCounter = 0;
    private long lastSyncedQi = 0;
    private static final int SYNC_THRESHOLD = 10; // Sync when Qi changes by this amount
    private static final int PERIODIC_SYNC_TICKS = 100; // Force sync every 5 seconds

    public PlayerQiAttachment() {}

    // === Player Reference Management ===

    /**
     * Dynamically finds the player that owns this attachment.
     * This approach handles the serialization issue where player references are lost.
     */
    private Player findPlayer() {
        // Check client-side first
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_QI.get()) == this) {
                return clientPlayer;
            }
        }

        // Check server-side
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
     * Safe player retrieval with debug logging
     */
    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("Could not find player for QiAttachment");
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
        long maxQi = getMaxQi();

        // Clamp between 0 and max
        this.currentQi = Math.max(0, Math.min(qi, maxQi));

        // Handle sync if value changed significantly
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide() && shouldSync(oldQi)) {
            syncToClient();
        }

        // Trigger change event
        if (oldQi != this.currentQi) {
            onQiChanged(oldQi, this.currentQi);
        }
    }

    // === Capacity Management ===

    @Override
    public long getBaseMaxQi() {
        Player player = getPlayerSafe();
        if (player == null) return 0; // Mortals have no Qi

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 0;

            // Get base from Qi realm only (Body realm doesn't affect Qi capacity)
            return realmAttachment.getCurrentQiRealm().getBaseMaxQi();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base max Qi", e);
            return 0;
        }
    }

    @Override
    public long getMaxQi() {
        long baseMax = getBaseMaxQi();

        Player player = getPlayerSafe();
        if (player == null) return baseMax;

        // Add bonus from Qi cultivation progress
        try {
            PlayerQiCultivationAttachment cultivationAttachment =
                player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (cultivationAttachment != null) {
                // 10% of cultivation as bonus Qi capacity
                long cultivationBonus = (long)(cultivationAttachment.getCurrentQiCultivation() * 0.10);
                return baseMax + cultivationBonus;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating Qi cultivation bonus", e);
        }

        return baseMax;
    }

    // === Regeneration System ===

    @Override
    public double getBaseQiRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) return 0.0;

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 0.0;

            // Base regen from Qi realm
            return realmAttachment.getCurrentQiRealm().getBaseQiRegenerationRate();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base Qi regeneration rate", e);
            return 0.0;
        }
    }

    @Override
    public double getBaseCirculatingQiRegenerationRate() {
        // Circulating Qi (meditation/special techniques) is typically 9x faster
        return getBaseQiRegenerationRate() * 9.0;
    }

    @Override
    public double getEffectiveQiRegenerationRate() {
        double baseRate = getBaseQiRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) return baseRate;

        try {
            // Apply spiritual roots multiplier
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier();
                return baseRate * multiplier;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying spiritual roots to Qi regen", e);
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingQiRegenerationRate() {
        // Apply same multipliers to circulating rate
        return getEffectiveQiRegenerationRate() * 9.0;
    }

    @Override
    public double getRegenFraction() {
        return qiRegenFraction;
    }

    @Override
    public void setRegenFraction(double fraction) {
        this.qiRegenFraction = fraction;
    }

    // === Tick Update ===

    @Override
    public void tick() {
        tickCounter++;

        Player player = getPlayerSafe();
        if (player == null) return;

        // Only process on server side
        if (!player.level().isClientSide()) {
            // Regenerate Qi
            if (canRegenerateQi()) {
                double regenRate = getEffectiveQiRegenerationRate();

                if (regenRate > 0) {
                    // Convert per-second rate to per-tick (20 ticks = 1 second)
                    qiRegenFraction += regenRate / 20.0;

                    // Apply whole points of regeneration
                    if (qiRegenFraction >= 1.0) {
                        long qiToAdd = (long)qiRegenFraction;
                        qiRegenFraction -= qiToAdd;
                        addQi(qiToAdd);
                    }
                }
            }

            // Periodic sync check
            if (tickCounter % PERIODIC_SYNC_TICKS == 0 && currentQi != lastSyncedQi) {
                syncToClient();
            }
        }
    }

    // === Event Callbacks ===

    @Override
    public void onQiChanged(long oldQi, long newQi) {
        // This is where you'd trigger visual effects, update HUD, etc.
        Player player = getPlayerSafe();
        if (player != null && player.level().isClientSide()) {
            long difference = Math.abs(oldQi - newQi);
            if (difference > 5) {
                // Significant change - could trigger HUD animation
                // QiHUDOverlay.showChange(oldQi, newQi);
            }
        }
    }

    @Override
    public void onQiConsumed(long amount) {
        // Could add consumption effects, sounds, particles
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} consumed {} Qi",
                player.getName().getString(), amount);
        }
    }

    @Override
    public void onQiAdded(long amount) {
        // Could add regeneration effects
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} regenerated {} Qi",
                player.getName().getString(), amount);
        }
    }

    // === Synchronization ===

    private boolean shouldSync(long oldQi) {
        long difference = Math.abs(currentQi - lastSyncedQi);
        // Sync if change exceeds threshold or on periodic updates
        return difference >= SYNC_THRESHOLD ||
            (difference > 0 && tickCounter % PERIODIC_SYNC_TICKS == 0);
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
     * Called on client when receiving sync packet from server
     */
    public void updateFromServer(long newQi) {
        long oldQi = this.currentQi;
        this.currentQi = newQi;
        this.lastSyncedQi = newQi;

        if (oldQi != newQi) {
            onQiChanged(oldQi, newQi);
        }
    }

    // === Serialization ===

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.currentQi = tag.getLong("qi");
        this.qiRegenFraction = tag.getDouble("qiRegenFraction");
        this.tickCounter = tag.getInt("tickCounter");
        this.lastSyncedQi = tag.getLong("lastSyncedQi");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("qi", currentQi);
        tag.putDouble("qiRegenFraction", qiRegenFraction);
        tag.putInt("tickCounter", tickCounter);
        tag.putLong("lastSyncedQi", lastSyncedQi);
        return tag;
    }

    // === Codec ===

    public static final Codec<PlayerQiAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("qi", 0L).forGetter(PlayerQiAttachment::getCurrentQi),
        Codec.DOUBLE.optionalFieldOf("qiRegenFraction", 0.0).forGetter(comp -> comp.qiRegenFraction),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter),
        Codec.LONG.optionalFieldOf("lastSyncedQi", 0L).forGetter(comp -> comp.lastSyncedQi)
    ).apply(instance, (qi, fraction, ticks, lastSync) -> {
        PlayerQiAttachment comp = new PlayerQiAttachment();
        comp.currentQi = qi;
        comp.qiRegenFraction = fraction;
        comp.tickCounter = ticks;
        comp.lastSyncedQi = lastSync;
        return comp;
    }));

    // === Debug Utilities ===

    public void debugInfo() {
        Player player = getPlayerSafe();
        String side = player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown";
        String playerName = player != null ? player.getName().getString() : "Unknown";

        ImmortalCultivation.LOGGER.info("=== Qi Attachment Debug Info ===");
        ImmortalCultivation.LOGGER.info("Player: {} ({})", playerName, side);
        ImmortalCultivation.LOGGER.info("Current Qi: {}/{} ({}%)",
            getCurrentQi(), getMaxQi(), String.format("%.1f", getQiPercentage() * 100));
        ImmortalCultivation.LOGGER.info("Regen Rate: {}/sec (Effective), Fraction: {}",
            getEffectiveQiRegenerationRate(), String.format("%.3f", qiRegenFraction));
        ImmortalCultivation.LOGGER.info("Sync Status: Last synced at {}, Current diff: {}",
            lastSyncedQi, Math.abs(currentQi - lastSyncedQi));

        if (player != null) {
            try {
                PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                if (realmAttachment != null) {
                    ImmortalCultivation.LOGGER.info("Qi Realm: {}",
                        realmAttachment.getCurrentQiRealm().getDisplayName());
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error getting realm info", e);
            }
        }
    }
}
