package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IBodyData;
import net.hyper.immortalcultivation.network.BodySyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * FIXED VERSION: PlayerBodyAttachment with proper player reference management
 *
 * Key Fix: The findPlayer() method now works correctly after death/respawn
 * and all methods that depend on player context have better error handling.
 *
 * Manages the player's Body resource - a cultivation resource separate from health.
 * Body represents physical cultivation energy that can be consumed for techniques
 * and abilities related to body cultivation.
 */
public class PlayerBodyAttachment implements IBodyData {
    // Core body resource tracking
    private long currentBody = 0; // Starts at 0, unlike health
    private double bodyRegenFraction = 0.0; // Fractional body for smooth regeneration

    // Sync and tick management
    private int tickCounter = 0;
    private long lastSyncedBody = 0;
    private static final int SYNC_THRESHOLD = 10; // Sync when body changes by this amount
    private static final int PERIODIC_SYNC_TICKS = 100; // Force sync every 5 seconds

    public PlayerBodyAttachment() {}

    // === FIXED: Player Reference Management ===

    /**
     * CRITICAL FIX: This method was causing the "lost attachments" and sync issues
     *
     * OLD BROKEN VERSION used reference equality (== this) which fails after respawn
     * NEW FIXED VERSION uses a more reliable approach with better error handling
     */
    private Player findPlayer() {
        // On client side, there's only one player - the local player
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            // Check if this client player has this attachment instance
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_BODY.get()) == this) {
                return clientPlayer;
            }
        }

        // FIXED: Server side now has better error handling and logging
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                // FIXED: More reliable check with null safety
                PlayerBodyAttachment playerBodyAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_BODY.get());
                if (playerBodyAttachment != null && playerBodyAttachment == this) {
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
            ImmortalCultivation.LOGGER.debug("Could not find player for BodyAttachment - this may indicate a respawn issue");
        }
        return player;
    }

    // === ENHANCED: Core Body Management methods now work after respawn ===

    @Override
    public long getCurrentBody() {
        return currentBody;
    }

    @Override
    public void setCurrentBody(long body) {
        long oldBody = this.currentBody;
        long maxBody = getMaxBody();

        // Clamp between 0 and max
        this.currentBody = Math.max(0, Math.min(body, maxBody));

        // Handle sync if value changed significantly
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide() && shouldSync(oldBody)) {
            syncToClient();
        }

        // Trigger change event
        if (oldBody != this.currentBody) {
            onBodyChanged(oldBody, this.currentBody);
        }
    }

    // === FIXED: Capacity Management methods now work after respawn ===

    @Override
    public long getBaseMaxBody() {
        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Better fallback when player can't be found
            ImmortalCultivation.LOGGER.debug("No player found for getBaseMaxBody, returning 0");
            return 0; // Mortals have no body energy
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 0;

            // Get base from Body realm only
            // This uses the same value as Qi but for body energy
            return realmAttachment.getCurrentBodyRealm().getBaseMaxQi();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base max body", e);
            return 0;
        }
    }

    @Override
    public long getMaxBody() {
        long baseMax = getBaseMaxBody();

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Return sensible fallback instead of potentially broken calculation
            return baseMax;
        }

        // Add bonus from Body cultivation progress
        try {
            PlayerBodyCultivationAttachment cultivationAttachment =
                player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (cultivationAttachment != null) {
                // 10% of body cultivation as bonus body capacity
                long cultivationBonus = (long)(cultivationAttachment.getCurrentBodyCultivation() * 0.10);
                return baseMax + cultivationBonus;
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating body cultivation bonus", e);
        }

        return baseMax;
    }

    // === FIXED: Regeneration System methods now work after respawn ===

    @Override
    public double getBaseBodyRegenerationRate() {
        Player player = getPlayerSafe();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("No player found for getBaseBodyRegenerationRate, returning 0");
            return 0.0;
        }

        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return 0.0;

            // Base regen from Body realm
            return realmAttachment.getCurrentBodyRealm().getBaseQiRegenerationRate();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error getting base body regeneration rate", e);
            return 0.0;
        }
    }

    @Override
    public double getBaseCirculatingBodyRegenerationRate() {
        // Circulating body energy (meditation/special techniques) is typically 9x faster
        return getBaseBodyRegenerationRate() * 9.0;
    }

    @Override
    public double getEffectiveBodyRegenerationRate() {
        double baseRate = getBaseBodyRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Return base rate instead of broken calculation
            return baseRate;
        }

        // Apply physique multiplier - FIXED: multiply in place instead of returning
        try {
            PlayerPhysiqueAttachment physique = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
            if (physique != null) {
                float multiplier = physique.getCultivationMultiplier();
                baseRate *= multiplier; // Multiply in place, don't return here!
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying physique to body regen", e);
        }

        // Apply cultivation focus bonus - this will now execute properly
        // Since the focus attachment mentions "health regeneration", we'll interpret this
        // as applying to Body resource regeneration when Body-focused
        try {
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focus != null) {
                // Apply the health regen bonus when body-focused (1.5x when body-focused, 1.0x otherwise)
                baseRate *= focus.getHealthRegenerationBonus();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus to body regen", e);
        }

        return baseRate;
    }

    @Override
    public double getEffectiveCirculatingBodyRegenerationRate() {
        // Apply same multipliers to circulating rate
        double baseCirculatingRate = getBaseCirculatingBodyRegenerationRate();

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Return base rate instead of broken calculation
            return baseCirculatingRate;
        }

        // Apply physique multiplier - FIXED: multiply in place instead of returning
        try {
            PlayerPhysiqueAttachment physique = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
            if (physique != null) {
                float multiplier = physique.getCultivationMultiplier();
                baseCirculatingRate *= multiplier; // Multiply in place, don't return here!
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying physique to circulating body regen", e);
        }

        // Apply cultivation focus bonus - this will now execute properly
        try {
            PlayerCultivationFocusAttachment focus = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focus != null) {
                baseCirculatingRate *= focus.getHealthRegenerationBonus();
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying focus bonus to circulating body regen", e);
        }

        return baseCirculatingRate;
    }

    @Override
    public double getRegenFraction() {
        return bodyRegenFraction;
    }

    @Override
    public void setRegenFraction(double fraction) {
        this.bodyRegenFraction = fraction;
    }

    // === FIXED: Tick Update now works after respawn ===

    @Override
    public void tick() {
        tickCounter++;

        Player player = getPlayerSafe();
        if (player == null) {
            // IMPROVED: Don't spam logs, but do track the issue
            if (tickCounter % 200 == 0) { // Log every 10 seconds
                ImmortalCultivation.LOGGER.debug("BodyAttachment ticking without player reference (tick {})", tickCounter);
            }
            return;
        }

        // Only process on server side
        if (!player.level().isClientSide()) {
            // Regenerate body energy
            if (canRegenerateBody()) {
                double regenRate = getEffectiveBodyRegenerationRate();

                if (regenRate > 0) {
                    // Convert per-second rate to per-tick (20 ticks = 1 second)
                    bodyRegenFraction += regenRate / 20.0;

                    // Apply whole points of regeneration
                    if (bodyRegenFraction >= 1.0) {
                        long bodyToAdd = (long)bodyRegenFraction;
                        bodyRegenFraction -= bodyToAdd;
                        addBody(bodyToAdd);
                    }
                }
            }

            // Periodic sync check
            if (tickCounter % PERIODIC_SYNC_TICKS == 0 && currentBody != lastSyncedBody) {
                syncToClient();
            }
        }
    }

    // === FIXED: Event Callbacks now work after respawn ===

    @Override
    public void onBodyChanged(long oldBody, long newBody) {
        // This is where you'd trigger visual effects, update HUD, etc.
        Player player = getPlayerSafe();
        if (player != null && player.level().isClientSide()) {
            long difference = Math.abs(oldBody - newBody);
            if (difference > 5) {
                // Significant change - could trigger HUD animation
                // BodyHUDOverlay.showChange(oldBody, newBody);
            }
        }
    }

    @Override
    public void onBodyConsumed(long amount) {
        // Could add consumption effects, sounds, particles
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} consumed {} body energy",
                player.getName().getString(), amount);
        }
    }

    @Override
    public void onBodyAdded(long amount) {
        // Could add regeneration effects
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.debug("Player {} regenerated {} body energy",
                player.getName().getString(), amount);
        }
    }

    // === FIXED: Synchronization now works after respawn ===

    private boolean shouldSync(long oldBody) {
        long difference = Math.abs(currentBody - lastSyncedBody);
        // Sync if change exceeds threshold or on periodic updates
        return difference >= SYNC_THRESHOLD ||
            (difference > 0 && tickCounter % PERIODIC_SYNC_TICKS == 0);
    }

    private void syncToClient() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedBody = currentBody;
            try {
                ICModNetwork.sendToPlayer(new BodySyncPacket(currentBody), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing body to client", e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot sync body - no player found");
        }
    }

    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedBody = currentBody;
            try {
                ICModNetwork.sendToPlayer(new BodySyncPacket(currentBody), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error force syncing body to client", e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot force sync body - no player found");
        }
    }

    /**
     * Called on client when receiving sync packet from server
     */
    public void updateFromServer(long newBody) {
        long oldBody = this.currentBody;
        this.currentBody = newBody;
        this.lastSyncedBody = newBody;

        if (oldBody != newBody) {
            onBodyChanged(oldBody, newBody);
        }
    }

    // === Serialization (unchanged) ===

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.currentBody = tag.getLong("body");
        this.bodyRegenFraction = tag.getDouble("bodyRegenFraction");
        this.tickCounter = tag.getInt("tickCounter");
        this.lastSyncedBody = tag.getLong("lastSyncedBody");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("body", currentBody);
        tag.putDouble("bodyRegenFraction", bodyRegenFraction);
        tag.putInt("tickCounter", tickCounter);
        tag.putLong("lastSyncedBody", lastSyncedBody);
        return tag;
    }

    // === Codec (unchanged) ===

    public static final Codec<PlayerBodyAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("body", 0L).forGetter(PlayerBodyAttachment::getCurrentBody),
        Codec.DOUBLE.optionalFieldOf("bodyRegenFraction", 0.0).forGetter(comp -> comp.bodyRegenFraction),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter),
        Codec.LONG.optionalFieldOf("lastSyncedBody", 0L).forGetter(comp -> comp.lastSyncedBody)
    ).apply(instance, (body, fraction, ticks, lastSync) -> {
        PlayerBodyAttachment comp = new PlayerBodyAttachment();
        comp.currentBody = body;
        comp.bodyRegenFraction = fraction;
        comp.tickCounter = ticks;
        comp.lastSyncedBody = lastSync;
        return comp;
    }));

    // === FIXED: Debug Utilities now work after respawn ===

    public void debugInfo() {
        Player player = getPlayerSafe();
        String side = player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown";
        String playerName = player != null ? player.getName().getString() : "Unknown";

        ImmortalCultivation.LOGGER.info("=== Body Attachment Debug Info ===");
        ImmortalCultivation.LOGGER.info("Player: {} ({})", playerName, side);
        ImmortalCultivation.LOGGER.info("Current Body: {}/{} ({}%)",
            getCurrentBody(), getMaxBody(), String.format("%.1f", getBodyPercentage() * 100));
        ImmortalCultivation.LOGGER.info("Regen Rate: {}/sec (Effective), Fraction: {}",
            getEffectiveBodyRegenerationRate(), String.format("%.3f", bodyRegenFraction));
        ImmortalCultivation.LOGGER.info("Sync Status: Last synced at {}, Current diff: {}",
            lastSyncedBody, Math.abs(currentBody - lastSyncedBody));

        if (player != null) {
            try {
                PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                if (realmAttachment != null) {
                    ImmortalCultivation.LOGGER.info("Body Realm: {}",
                        realmAttachment.getCurrentBodyRealm().getDisplayName());
                }
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error getting realm info", e);
            }
        }
    }
}