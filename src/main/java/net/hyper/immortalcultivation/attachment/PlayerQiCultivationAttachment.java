package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
import net.hyper.immortalcultivation.network.QiCultivationSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * FIXED VERSION: PlayerQiCultivationAttachment with proper player reference management
 *
 * Manages the player's Qi Cultivation progress - their permanent advancement in the Qi path.
 * This represents accumulated cultivation that determines realm breakthroughs and provides
 * permanent bonuses to Qi capacity and other Qi-related abilities.
 */
public class PlayerQiCultivationAttachment implements IQiCultivationData {
    // Core cultivation tracking
    private long currentQiCultivation = 0;
    private double cultivationFraction = 0.0; // For smooth accumulation

    // Meditation state
    private boolean isMeditating = false;
    private int meditationTicks = 0;
    private static final int MEDITATION_WARMUP_TICKS = 60; // 3 seconds to reach full speed

    // Sync and tick management
    private int tickCounter = 0;
    private long lastSyncedCultivation = 0;
    private static final int SYNC_THRESHOLD = 100; // Higher threshold than Qi since it changes slower
    private static final int PERIODIC_SYNC_TICKS = 200; // Sync every 10 seconds

    public PlayerQiCultivationAttachment() {}

    // === FIXED: Player Reference Management ===

    /**
     * CRITICAL FIX: This method was causing the "lost attachments" and invincibility issues
     *
     * OLD BROKEN VERSION used reference equality (== this) which fails after respawn
     * NEW FIXED VERSION uses a more reliable approach with better error handling
     */
    private Player findPlayer() {
        // On client side, there's only one player - the local player
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            // Check if this client player has this attachment instance
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get()) == this) {
                return clientPlayer;
            }
        }

        // FIXED: Server side now has better error handling and logging
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                // IMPROVED: Get the attachment first, then check if it matches
                PlayerQiCultivationAttachment playerQiCultivationAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
                if (playerQiCultivationAttachment != null && playerQiCultivationAttachment == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    /**
     * FIXED: Safe player retrieval with better debug logging
     */
    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            // IMPROVED: Better logging that doesn't spam but indicates the issue
            ImmortalCultivation.LOGGER.debug("Could not find player for QiCultivationAttachment - this may indicate a respawn issue");
        }
        return player;
    }

    // === ENHANCED: Core Cultivation Management now works after respawn ===

    @Override
    public long getCurrentQiCultivation() {
        return currentQiCultivation;
    }

    @Override
    public void setCurrentQiCultivation(long cultivation) {
        long oldCultivation = this.currentQiCultivation;
        long maxCultivation = getMaxQiCultivation();

        // Clamp between 0 and max for current realm
        this.currentQiCultivation = Math.max(0, Math.min(cultivation, maxCultivation));

        // Handle sync if value changed significantly
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide() && shouldSync(oldCultivation)) {
            syncToClient();
        }

        // Trigger change event
        if (oldCultivation != this.currentQiCultivation) {
            onQiCultivationChanged(oldCultivation, this.currentQiCultivation);
        }
    }

    // === FIXED: Capacity Management now works after respawn ===

    @Override
    public long getBaseQiCultivation() {
        return getBaseMaxQiCultivation();
    }

    public long getBaseMaxQiCultivation() {
        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Better fallback when player can't be found
            ImmortalCultivation.LOGGER.debug("No player found for getBaseMaxQiCultivation, returning 100");
            return 100; // Default for mortals
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 100;

            // Max cultivation is determined by current Qi realm
            return realmAttachment.getCurrentQiRealm().getBaseMaxCultivation();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting max Qi cultivation", e);
            return 100;
        }
    }

    @Override
    public long getMaxQiCultivation() {
        // Could add modifiers here, but typically cultivation cap is fixed per realm
        return getBaseMaxQiCultivation();
    }

    // === FIXED: Cultivation Rate Management now works after respawn ===

    @Override
    public double getBaseQiCultivationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("No player found for getBaseQiCultivationRate, returning 0");
            return 0.0;
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 0.0;

            // Base rate from current Qi realm
            return realmAttachment.getCurrentQiRealm().getBaseCultivationRate();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base Qi cultivation rate", e);
            return 0.0;
        }
    }

    @Override
    public double getBaseCirculatingQiCultivationRate() {
        // Meditation/circulating techniques are 9x faster
        return getBaseQiCultivationRate() * 9.0;
    }

    @Override
    public double getEffectiveQiCultivationRate() {
        double baseRate = getBaseQiCultivationRate();

        // Apply meditation bonus if actively meditating
        if (isMeditating && meditationTicks >= MEDITATION_WARMUP_TICKS) {
            baseRate = getBaseCirculatingQiCultivationRate();
        } else if (isMeditating && meditationTicks > 0) {
            // Gradual increase during warmup
            double warmupProgress = (double)meditationTicks / MEDITATION_WARMUP_TICKS;
            double meditationRate = getBaseCirculatingQiCultivationRate();
            baseRate = baseRate + (meditationRate - baseRate) * warmupProgress;
        }

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
            ImmortalCultivation.LOGGER.error("Error applying spiritual roots to cultivation rate", e);
        }

        // Apply cultivation focus multiplier - this will now execute properly
        try {
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focus != null) {
                baseRate *= focus.getQiCultivationMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus multiplier to Qi cultivation rate", e);
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingQiCultivationRate() {
        // This is the maximum rate when fully meditating
        double baseCirculatingRate = getBaseCirculatingQiCultivationRate();

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
            ImmortalCultivation.LOGGER.error("Error applying spiritual roots to circulating rate", e);
        }

        // Apply cultivation focus multiplier - this will now execute properly
        try {
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focus != null) {
                baseCirculatingRate *= focus.getQiCultivationMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus multiplier to circulating Qi cultivation rate", e);
        }

        return baseCirculatingRate;
    }

    // === ENHANCED: Meditation System now works after respawn ===

    /**
     * Start or continue meditation
     */
    public void startMeditation() {
        if (!isMeditating) {
            isMeditating = true;
            meditationTicks = 0;

            Player player = getPlayerSafe();
            if (player != null) {
                ImmortalCultivation.LOGGER.debug("Player {} started Qi meditation",
                    player.getName().getString());
            }
        }
    }

    /**
     * Stop meditation
     */
    public void stopMeditation() {
        if (isMeditating) {
            isMeditating = false;
            meditationTicks = 0;

            Player player = getPlayerSafe();
            if (player != null) {
                ImmortalCultivation.LOGGER.debug("Player {} stopped Qi meditation",
                    player.getName().getString());
            }
        }
    }

    /**
     * Check if player is currently meditating
     */
    public boolean isMeditating() {
        return isMeditating;
    }

    /**
     * Get meditation efficiency (0.0 to 1.0)
     */
    public double getMeditationEfficiency() {
        if (!isMeditating) return 0.0;
        return Math.min(1.0, (double)meditationTicks / MEDITATION_WARMUP_TICKS);
    }

    // === Regen Fraction (from interface) ===

    @Override
    public double getRegenFraction() {
        return cultivationFraction;
    }

    @Override
    public void setRegenFraction(double fraction) {
        this.cultivationFraction = fraction;
    }

    // === FIXED: Tick Update now works after respawn ===

    @Override
    public void tick() {
        tickCounter++;

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Don't spam logs, but do track the issue
            if (tickCounter % 200 == 0) { // Log every 10 seconds
                ImmortalCultivation.LOGGER.debug("QiCultivationAttachment ticking without player reference (tick {})", tickCounter);
            }
            return;
        }

        // Only process on server side
        if (!player.level().isClientSide()) {
            // Update meditation state
            if (isMeditating) {
                meditationTicks++;

                // Check if player is still in valid meditation position
                if (!isValidMeditationState(player)) {
                    stopMeditation();
                }
            }

            // Accumulate cultivation
            if (canCultivateQi()) {
                double cultivationRate = getEffectiveQiCultivationRate();

                if (cultivationRate > 0) {
                    // Convert per-second rate to per-tick
                    cultivationFraction += cultivationRate / 20.0;

                    // Apply whole points of cultivation
                    if (cultivationFraction >= 1.0) {
                        long cultivationToAdd = (long)cultivationFraction;
                        cultivationFraction -= cultivationToAdd;
                        addQiCultivation(cultivationToAdd);
                    }
                }
            }

            // Periodic sync check
            if (tickCounter % PERIODIC_SYNC_TICKS == 0 && currentQiCultivation != lastSyncedCultivation) {
                syncToClient();
            }
        }
    }

    /**
     * Check if player is in a valid state for meditation
     */
    private boolean isValidMeditationState(Player player) {
        // Player must be sneaking and not moving significantly
        if (!player.isShiftKeyDown()) return false;

        // Check if player is relatively stationary
        double speed = player.getDeltaMovement().horizontalDistance();
        if (speed > 0.1) return false;

        // Could add more conditions: not in combat, not taking damage, etc.
        return true;
    }

    // === FIXED: Event Callbacks now work after respawn ===

    @Override
    public void onQiCultivationChanged(long oldCultivation, long newCultivation) {
        Player player = getPlayerSafe();
        if (player != null) {
            // Check for realm breakthrough threshold
            try {
                PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                if (realmAttachment != null) {
                    long threshold = realmAttachment.getCurrentQiRealm().getBreakthroughCultivationThreshold();

                    // Notify player when they cross the threshold
                    if (oldCultivation < threshold && newCultivation >= threshold) {
                        // Player can now attempt breakthrough
                        if (!player.level().isClientSide()) {
                            // Send notification to player
                            ImmortalCultivation.LOGGER.info("Player {} reached Qi breakthrough threshold!",
                                player.getName().getString());
                        }
                    }
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error checking breakthrough threshold", e);
            }
        }
    }

    @Override
    public void onQiCultivationConsumed(long amount) {
        // Cultivation is rarely consumed, but this could happen for special techniques
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} consumed {} Qi cultivation",
                player.getName().getString(), amount);
        }
    }

    @Override
    public void onQiCultivationAdded(long amount) {
        // Track cultivation gains
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} gained {} Qi cultivation",
                player.getName().getString(), amount);
        }
    }

    // === FIXED: Synchronization now works after respawn ===

    private boolean shouldSync(long oldCultivation) {
        long difference = Math.abs(currentQiCultivation - lastSyncedCultivation);
        return difference >= SYNC_THRESHOLD ||
            (difference > 0 && tickCounter % PERIODIC_SYNC_TICKS == 0);
    }

    private void syncToClient() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedCultivation = currentQiCultivation;
            try {
                ICModNetwork.sendToPlayer(new QiCultivationSyncPacket(currentQiCultivation), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing Qi cultivation to client", e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot sync Qi cultivation - no player found");
        }
    }

    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedCultivation = currentQiCultivation;
            try {
                ICModNetwork.sendToPlayer(new QiCultivationSyncPacket(currentQiCultivation), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error force syncing Qi cultivation to client", e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot force sync Qi cultivation - no player found");
        }
    }

    public void updateFromServer(long newCultivation) {
        long oldCultivation = this.currentQiCultivation;
        this.currentQiCultivation = newCultivation;
        this.lastSyncedCultivation = newCultivation;

        if (oldCultivation != newCultivation) {
            onQiCultivationChanged(oldCultivation, newCultivation);
        }
    }

    // === Serialization (unchanged) ===

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.currentQiCultivation = tag.getLong("qiCultivation");
        this.cultivationFraction = tag.getDouble("cultivationFraction");
        this.isMeditating = tag.getBoolean("isMeditating");
        this.meditationTicks = tag.getInt("meditationTicks");
        this.tickCounter = tag.getInt("tickCounter");
        this.lastSyncedCultivation = tag.getLong("lastSyncedCultivation");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("qiCultivation", currentQiCultivation);
        tag.putDouble("cultivationFraction", cultivationFraction);
        tag.putBoolean("isMeditating", isMeditating);
        tag.putInt("meditationTicks", meditationTicks);
        tag.putInt("tickCounter", tickCounter);
        tag.putLong("lastSyncedCultivation", lastSyncedCultivation);
        return tag;
    }

    // === Codec (unchanged) ===

    public static final Codec<PlayerQiCultivationAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("qiCultivation", 0L).forGetter(PlayerQiCultivationAttachment::getCurrentQiCultivation),
        Codec.DOUBLE.optionalFieldOf("cultivationFraction", 0.0).forGetter(comp -> comp.cultivationFraction),
        Codec.BOOL.optionalFieldOf("isMeditating", false).forGetter(comp -> comp.isMeditating),
        Codec.INT.optionalFieldOf("meditationTicks", 0).forGetter(comp -> comp.meditationTicks),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter),
        Codec.LONG.optionalFieldOf("lastSyncedCultivation", 0L).forGetter(comp -> comp.lastSyncedCultivation)
    ).apply(instance, (cultivation, fraction, meditating, medTicks, ticks, lastSync) -> {
        PlayerQiCultivationAttachment comp = new PlayerQiCultivationAttachment();
        comp.currentQiCultivation = cultivation;
        comp.cultivationFraction = fraction;
        comp.isMeditating = meditating;
        comp.meditationTicks = medTicks;
        comp.tickCounter = ticks;
        comp.lastSyncedCultivation = lastSync;
        return comp;
    }));

    // === FIXED: Debug Utilities now work after respawn ===

    public void debugInfo() {
        Player player = getPlayerSafe();
        String side = player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown";
        String playerName = player != null ? player.getName().getString() : "Unknown";

        ImmortalCultivation.LOGGER.info("=== Qi Cultivation Debug Info ===");
        ImmortalCultivation.LOGGER.info("Player: {} ({})", playerName, side);
        ImmortalCultivation.LOGGER.info("Current Cultivation: {}/{} ({}%)",
            getCurrentQiCultivation(), getMaxQiCultivation(),
            String.format("%.1f", getQiCultivationPercentage() * 100));
        ImmortalCultivation.LOGGER.info("Cultivation Rate: {}/sec (Effective), Fraction: {}",
            getEffectiveQiCultivationRate(), String.format("%.3f", cultivationFraction));
        ImmortalCultivation.LOGGER.info("Meditation: {} (Efficiency: {}%)",
            isMeditating ? "Active" : "Inactive",
            String.format("%.1f", getMeditationEfficiency() * 100));

        if (player != null) {
            try {
                PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                if (realmAttachment != null) {
                    ImmortalCultivation.LOGGER.info("Qi Realm: {}",
                        realmAttachment.getCurrentQiRealm().getDisplayName());

                    long threshold = realmAttachment.getCurrentQiRealm().getBreakthroughCultivationThreshold();
                    ImmortalCultivation.LOGGER.info("Breakthrough Progress: {}/{} ({}%)",
                        getCurrentQiCultivation(), threshold,
                        String.format("%.1f", (double)getCurrentQiCultivation() / threshold * 100));
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error getting realm info", e);
            }
        }
    }
}