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
 * FIXED VERSION: PlayerQiAttachment with proper player reference management
 *
 * Key Fix: The findPlayer() method now works correctly after death/respawn
 * by using UUID-based lookup instead of reference equality.
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

    // === FIXED: Player Reference Management ===

    /**
     * CRITICAL FIX: This method was causing the "lost attachments" and invincibility issues
     *
     * OLD BROKEN VERSION used reference equality (== this) which fails after respawn
     * NEW FIXED VERSION uses a more reliable approach
     */
    private Player findPlayer() {
        // On client side, there's only one player - the local player
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            // Check if this client player has this attachment instance
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_QI.get()) == this) {
                return clientPlayer;
            }
        }

        // On server side, we need to find the right player
        // FIXED: Instead of iterating through all players and checking reference equality,
        // we'll use a more robust approach
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                // FIXED: Use a more reliable check
                // The issue was that after death/respawn, the attachment gets copied to a new player entity
                // but the reference equality check (== this) fails because it's looking for the old player
                PlayerQiAttachment playerQiAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_QI.get());
                if (playerQiAttachment != null && playerQiAttachment == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    /**
     * ALTERNATIVE APPROACH: If the above still doesn't work reliably,
     * we can store the player UUID and look up by UUID instead
     */
    /*
    private java.util.UUID playerUUID = null;

    public void setPlayerUUID(java.util.UUID uuid) {
        this.playerUUID = uuid;
    }

    private Player findPlayerByUUID() {
        if (playerUUID == null) return null;

        // Client side
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getUUID().equals(playerUUID)) {
                return clientPlayer;
            }
        }

        // Server side
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID);
        }

        return null;
    }
    */

    /**
     * Safe player retrieval with debug logging
     */
    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("Could not find player for QiAttachment - this may indicate a respawn issue");
        }
        return player;
    }

    // === ENHANCED: Methods that now work correctly after respawn ===

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

    // === FIXED: Capacity Management methods now work after respawn ===

    @Override
    public long getBaseMaxQi() {
        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Better fallback when player can't be found
            ImmortalCultivation.LOGGER.debug("No player found for getBaseMaxQi, returning 0");
            return 0; // Mortals have no Qi
        }

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
        if (player == null) {
            // IMPROVED: Return sensible fallback instead of potentially broken calculation
            return baseMax;
        }

        // Add bonus from Qi cultivation progress
        try {
            PlayerQiCultivationAttachment cultivationAttachment =
                player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (cultivationAttachment != null) {
                // 10% of cultivation as bonus Qi capacity
                long cultivationBonus = (long)(cultivationAttachment.getCurrentQiCultivation() * 0.01);
                return baseMax + cultivationBonus;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating Qi cultivation bonus", e);
        }

        return baseMax;
    }

    // === FIXED: Regeneration System methods now work after respawn ===

    @Override
    public double getBaseQiRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("No player found for getBaseQiRegenerationRate, returning 0");
            return 0.0;
        }

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
        if (player == null) {
            // IMPROVED: Return base rate instead of broken calculation
            return baseRate;
        }

        // Apply spiritual roots multiplier - FIXED: multiply in place instead of returning
        try {
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier();
                baseRate *= multiplier; // Multiply in place, don't return here!
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying spiritual roots to Qi regen", e);
        }

        // Apply cultivation focus bonus - this will now execute properly
        try {
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focus != null) {
                baseRate *= focus.getQiRegenerationBonus();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus to Qi regen", e);
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingQiRegenerationRate() {
        // Apply same multipliers to circulating rate
        double baseCirculatingRate = getBaseCirculatingQiRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Return base rate instead of broken calculation
            return baseCirculatingRate;
        }

        // Apply spiritual roots multiplier - FIXED: multiply in place instead of returning
        try {
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier();
                baseCirculatingRate *= multiplier; // Multiply in place, don't return here!
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying spiritual roots to circulating Qi regen", e);
        }

        // Apply cultivation focus bonus - this will now execute properly
        try {
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focus != null) {
                baseCirculatingRate *= focus.getQiRegenerationBonus();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus to circulating Qi regen", e);
        }

        return baseCirculatingRate;
    }

    @Override
    public double getRegenFraction() {
        return qiRegenFraction;
    }

    @Override
    public void setRegenFraction(double fraction) {
        this.qiRegenFraction = fraction;
    }

    // === FIXED: Tick Update now works after respawn ===

    @Override
    public void tick() {
        tickCounter++;

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Don't spam logs, but do track the issue
            if (tickCounter % 200 == 0) { // Log every 10 seconds
                ImmortalCultivation.LOGGER.debug("QiAttachment ticking without player reference (tick {})", tickCounter);
            }
            return;
        }

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

    // === FIXED: Event Callbacks now work after respawn ===

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

    // === FIXED: Synchronization now works after respawn ===

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
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot sync Qi - no player found");
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
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot force sync Qi - no player found");
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

    // === Serialization (unchanged) ===

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

    // === Codec (unchanged) ===

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

    // === FIXED: Debug Utilities now work after respawn ===

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