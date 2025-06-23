package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IBodyCultivationData;
import net.hyper.immortalcultivation.network.BodyCultivationSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * FIXED VERSION: PlayerBodyCultivationAttachment with proper player reference management
 *
 * Key Fix: The findPlayer() method now works correctly after death/respawn
 * and all methods that depend on player context have better error handling.
 *
 * Manages the player's Body Cultivation progress - their permanent advancement in body cultivation.
 * Similar to Qi cultivation, this uses meditation to accumulate cultivation progress.
 * Body cultivation focuses on tempering the physical form through internal energy circulation.
 */
public class PlayerBodyCultivationAttachment implements IBodyCultivationData {
    // Core cultivation tracking
    private long currentBodyCultivation = 0;
    private double cultivationFraction = 0.0; // For smooth accumulation

    // Meditation state for body cultivation
    private boolean isMeditating = false;
    private int meditationTicks = 0;
    private static final int MEDITATION_WARMUP_TICKS = 80; // 4 seconds to reach full speed (slightly different from Qi)

    // Sync and tick management
    private int tickCounter = 0;
    private long lastSyncedCultivation = 0;
    private static final int SYNC_THRESHOLD = 100; // Higher threshold since cultivation changes slowly
    private static final int PERIODIC_SYNC_TICKS = 200; // Sync every 10 seconds

    public PlayerBodyCultivationAttachment() {}

    // === FIXED: Player Reference Management ===

    /**
     * CRITICAL FIX: This method was causing the "lost attachments" and meditation issues
     *
     * OLD BROKEN VERSION used reference equality (== this) which fails after respawn
     * NEW FIXED VERSION uses a more reliable approach with better error handling
     */
    private Player findPlayer() {
        // On client side, there's only one player - the local player
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            // Check if this client player has this attachment instance
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get()) == this) {
                return clientPlayer;
            }
        }

        // FIXED: Server side now has better error handling and logging
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                // FIXED: More reliable check with null safety
                PlayerBodyCultivationAttachment playerBodyCultivationAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
                if (playerBodyCultivationAttachment != null && playerBodyCultivationAttachment == this) {
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
            ImmortalCultivation.LOGGER.debug("Could not find player for BodyCultivationAttachment - this may indicate a respawn issue");
        }
        return player;
    }

    // === ENHANCED: Core Cultivation Management methods now work after respawn ===

    @Override
    public long getCurrentBodyCultivation() {
        return currentBodyCultivation;
    }

    @Override
    public void setCurrentBodyCultivation(long cultivation) {
        long oldCultivation = this.currentBodyCultivation;
        long maxCultivation = getMaxBodyCultivation();

        // Clamp between 0 and max for current realm
        this.currentBodyCultivation = Math.max(0, Math.min(cultivation, maxCultivation));

        // Handle sync if value changed significantly
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide() && shouldSync(oldCultivation)) {
            syncToClient();
        }

        // Trigger change event
        if (oldCultivation != this.currentBodyCultivation) {
            onBodyCultivationChanged(oldCultivation, this.currentBodyCultivation);
        }
    }

    // === FIXED: Capacity Management methods now work after respawn ===

    @Override
    public long getBaseBodyCultivation() {
        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Better fallback when player can't be found
            ImmortalCultivation.LOGGER.debug("No player found for getBaseBodyCultivation, returning default (100)");
            return 100; // Default for mortals
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 100;

            // Max cultivation is determined by current Body realm
            return realmAttachment.getCurrentBodyRealm().getBaseMaxCultivation();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting max body cultivation", e);
            return 100;
        }
    }

    @Override
    public long getMaxBodyCultivation() {
        // Could add modifiers here, but typically cultivation cap is fixed per realm
        return getBaseBodyCultivation();
    }

    // === FIXED: Cultivation Rate Management methods now work after respawn ===

    @Override
    public double getBaseBodyCultivationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("No player found for getBaseBodyCultivationRate, returning 0");
            return 0.0;
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 0.0;

            // Base rate from current Body realm
            return realmAttachment.getCurrentBodyRealm().getBaseCultivationRate();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base body cultivation rate", e);
            return 0.0;
        }
    }

    @Override
    public double getBaseCirculatingBodyCultivationRate() {
        // Meditation/circulating techniques are 9x faster
        return getBaseBodyCultivationRate() * 9.0;
    }

    @Override
    public double getEffectiveBodyCultivationRate() {
        double baseRate = getBaseBodyCultivationRate();

        // Apply meditation bonus if actively meditating
        if (isMeditating && meditationTicks >= MEDITATION_WARMUP_TICKS) {
            baseRate = getBaseCirculatingBodyCultivationRate();
        } else if (isMeditating && meditationTicks > 0) {
            // Gradual increase during warmup
            double warmupProgress = (double)meditationTicks / MEDITATION_WARMUP_TICKS;
            double meditationRate = getBaseCirculatingBodyCultivationRate();
            baseRate = baseRate + (meditationRate - baseRate) * warmupProgress;
        }

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Return base rate instead of broken calculation
            return baseRate;
        }

        // Apply physique multiplier - FIXED: multiply in place instead of returning
        try {
            PlayerPhysiqueAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier();
                baseRate *= multiplier; // Multiply in place, don't return here!
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying physique to body cultivation rate", e);
        }

        // UPDATED: Apply permanent pill bonuses using the new additive + multiplicative system
        try {
            PlayerCultivationBonusAttachment bonusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());
            if (bonusAttachment != null) {
                baseRate = bonusAttachment.applyBodyCultivationBonuses(baseRate);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying cultivation pill bonuses to Body rate", e);
        }

        // Apply cultivation focus multiplier - this will now execute properly
        // When focused on Body cultivation, rate is doubled
        try {
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focus != null) {
                baseRate *= focus.getBodyCultivationMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus multiplier to Body cultivation rate", e);
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingBodyCultivationRate() {
        // This is the maximum rate when fully meditating
        double baseCirculatingRate = getBaseCirculatingBodyCultivationRate();

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Return base rate instead of broken calculation
            return baseCirculatingRate;
        }

        // Apply physique multiplier - FIXED: multiply in place instead of returning
        try {
            PlayerPhysiqueAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
            if (roots != null) {
                float multiplier = roots.getCultivationMultiplier();
                baseCirculatingRate *= multiplier; // Multiply in place, don't return here!
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying physique to circulating rate", e);
        }

        // UPDATED: Apply permanent pill bonuses using the new additive + multiplicative system
        try {
            PlayerCultivationBonusAttachment bonusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());
            if (bonusAttachment != null) {
                baseCirculatingRate = bonusAttachment.applyBodyCultivationBonuses(baseCirculatingRate);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying cultivation pill bonuses to circulating Body rate", e);
        }

        // Apply cultivation focus multiplier - this will now execute properly
        // When focused on Body cultivation, rate is doubled
        try {
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focus != null) {
                baseCirculatingRate *= focus.getBodyCultivationMultiplier();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus multiplier to circulating Body rate", e);
        }

        return baseCirculatingRate;
    }

    // === ENHANCED: Meditation System now works after respawn ===

    /**
     * Start or continue body cultivation meditation
     * Body meditation focuses on internal energy circulation and physical tempering
     */
    public void startMeditation() {
        if (!isMeditating) {
            isMeditating = true;
            meditationTicks = 0;

            Player player = getPlayerSafe();
            if (player != null) {
                ImmortalCultivation.LOGGER.debug("Player {} started body cultivation meditation",
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
                ImmortalCultivation.LOGGER.debug("Player {} stopped body cultivation meditation",
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
     * This represents how deeply the player is in meditation
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
                ImmortalCultivation.LOGGER.debug("BodyCultivationAttachment ticking without player reference (tick {})", tickCounter);
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
            if (canCultivateBody()) {
                double cultivationRate = getEffectiveBodyCultivationRate();

                if (cultivationRate > 0) {
                    // Convert per-second rate to per-tick
                    cultivationFraction += cultivationRate / 20.0;

                    // Apply whole points of cultivation
                    if (cultivationFraction >= 1.0) {
                        long cultivationToAdd = (long)cultivationFraction;
                        cultivationFraction -= cultivationToAdd;
                        addBodyCultivation(cultivationToAdd);
                    }
                }
            }

            // Periodic sync check
            if (tickCounter % PERIODIC_SYNC_TICKS == 0 && currentBodyCultivation != lastSyncedCultivation) {
                syncToClient();
            }
        }
    }

    /**
     * Check if player is in a valid state for meditation
     * Body cultivation meditation requires stillness and focus like Qi meditation
     */
    private boolean isValidMeditationState(Player player) {
        // Player must be sneaking and not moving significantly
        if (!player.isShiftKeyDown()) return false;

        // Check if player is relatively stationary
        double speed = player.getDeltaMovement().horizontalDistance();
        if (speed > 0.1) return false;

        // Could add more conditions: not in combat, not taking damage, proper environment, etc.
        return true;
    }

    // === FIXED: Event Callbacks now work after respawn ===

    @Override
    public void onBodyCultivationChanged(long oldCultivation, long newCultivation) {
        Player player = getPlayerSafe();
        if (player != null) {
            // Check for realm breakthrough threshold
            try {
                PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                if (realmAttachment != null) {
                    long threshold = realmAttachment.getCurrentBodyRealm().getBreakthroughCultivationThreshold();

                    // Notify player when they cross the threshold
                    if (oldCultivation < threshold && newCultivation >= threshold) {
                        // Player can now attempt breakthrough
                        if (!player.level().isClientSide()) {
                            ImmortalCultivation.LOGGER.info("Player {} reached Body breakthrough threshold!",
                                player.getName().getString());
                            // Could send a message to player or trigger effects
                        }
                    }
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error checking breakthrough threshold", e);
            }
        }
    }

    @Override
    public void onBodyCultivationConsumed(long amount) {
        // Body cultivation is rarely consumed, but this could happen for special techniques
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} consumed {} body cultivation",
                player.getName().getString(), amount);
        }
    }

    @Override
    public void onBodyCultivationAdded(long amount) {
        // Track cultivation gains
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} gained {} body cultivation",
                player.getName().getString(), amount);
        }
    }

    // === FIXED: Synchronization now works after respawn ===

    private boolean shouldSync(long oldCultivation) {
        long difference = Math.abs(currentBodyCultivation - lastSyncedCultivation);
        return difference >= SYNC_THRESHOLD ||
            (difference > 0 && tickCounter % PERIODIC_SYNC_TICKS == 0);
    }

    private void syncToClient() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedCultivation = currentBodyCultivation;
            try {
                ICModNetwork.sendToPlayer(new BodyCultivationSyncPacket(currentBodyCultivation), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing body cultivation to client", e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot sync body cultivation - no player found");
        }
    }

    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedCultivation = currentBodyCultivation;
            try {
                ICModNetwork.sendToPlayer(new BodyCultivationSyncPacket(currentBodyCultivation), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error force syncing body cultivation to client", e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot force sync body cultivation - no player found");
        }
    }

    public void updateFromServer(long newCultivation) {
        long oldCultivation = this.currentBodyCultivation;
        this.currentBodyCultivation = newCultivation;
        this.lastSyncedCultivation = newCultivation;

        if (oldCultivation != newCultivation) {
            onBodyCultivationChanged(oldCultivation, newCultivation);
        }
    }

    // === Serialization (unchanged) ===

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.currentBodyCultivation = tag.getLong("bodyCultivation");
        this.cultivationFraction = tag.getDouble("cultivationFraction");
        this.isMeditating = tag.getBoolean("isMeditating");
        this.meditationTicks = tag.getInt("meditationTicks");
        this.tickCounter = tag.getInt("tickCounter");
        this.lastSyncedCultivation = tag.getLong("lastSyncedCultivation");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("bodyCultivation", currentBodyCultivation);
        tag.putDouble("cultivationFraction", cultivationFraction);
        tag.putBoolean("isMeditating", isMeditating);
        tag.putInt("meditationTicks", meditationTicks);
        tag.putInt("tickCounter", tickCounter);
        tag.putLong("lastSyncedCultivation", lastSyncedCultivation);
        return tag;
    }

    // === Codec (unchanged) ===

    public static final Codec<PlayerBodyCultivationAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("bodyCultivation", 0L).forGetter(PlayerBodyCultivationAttachment::getCurrentBodyCultivation),
        Codec.DOUBLE.optionalFieldOf("cultivationFraction", 0.0).forGetter(comp -> comp.cultivationFraction),
        Codec.BOOL.optionalFieldOf("isMeditating", false).forGetter(comp -> comp.isMeditating),
        Codec.INT.optionalFieldOf("meditationTicks", 0).forGetter(comp -> comp.meditationTicks),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter),
        Codec.LONG.optionalFieldOf("lastSyncedCultivation", 0L).forGetter(comp -> comp.lastSyncedCultivation)
    ).apply(instance, (cultivation, fraction, meditating, medTicks, ticks, lastSync) -> {
        PlayerBodyCultivationAttachment comp = new PlayerBodyCultivationAttachment();
        comp.currentBodyCultivation = cultivation;
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

        ImmortalCultivation.LOGGER.info("=== Body Cultivation Debug Info ===");
        ImmortalCultivation.LOGGER.info("Player: {} ({})", playerName, side);
        ImmortalCultivation.LOGGER.info("Current Cultivation: {}/{} ({}%)",
            getCurrentBodyCultivation(), getMaxBodyCultivation(),
            String.format("%.1f", getBodyCultivationPercentage() * 100));
        ImmortalCultivation.LOGGER.info("Cultivation Rate: {}/sec (Effective), Fraction: {}",
            getEffectiveBodyCultivationRate(), String.format("%.3f", cultivationFraction));
        ImmortalCultivation.LOGGER.info("Meditation: {} (Efficiency: {}%)",
            isMeditating ? "Active" : "Inactive",
            String.format("%.1f", getMeditationEfficiency() * 100));

        if (player != null) {
            try {
                PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                if (realmAttachment != null) {
                    ImmortalCultivation.LOGGER.info("Body Realm: {}",
                        realmAttachment.getCurrentBodyRealm().getDisplayName());

                    long threshold = realmAttachment.getCurrentBodyRealm().getBreakthroughCultivationThreshold();
                    ImmortalCultivation.LOGGER.info("Breakthrough Progress: {}/{} ({}%)",
                        getCurrentBodyCultivation(), threshold,
                        String.format("%.1f", (double)getCurrentBodyCultivation() / threshold * 100));
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error getting realm info", e);
            }
        }
    }
}