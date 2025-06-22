package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment.CultivationType;
import net.hyper.immortalcultivation.network.CultivationFocusSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Manages the player's cultivation focus - a toggle between Qi and Body cultivation.
 * When focused on one cultivation type, the player receives significant bonuses:
 * - 2x cultivation rate for the focused type
 * - Additional regeneration bonuses (Qi regen for Qi focus, Health regen for Body focus)
 *
 * This system encourages strategic switching based on the player's current needs.
 */
public class PlayerCultivationFocusAttachment {
    // Current focus (true = Qi, false = Body)
    private boolean focusedOnQi = true; // Default to Qi focus

    // Track focus duration for potential future features
    private long ticksInCurrentFocus = 0;
    private long lastFocusChangeTime = 0;

    // Sync management
    private boolean lastSyncedFocus = true;
    private int tickCounter = 0;
    private static final int PERIODIC_SYNC_TICKS = 200; // Sync every 10 seconds

    // Focus bonuses
    private static final double CULTIVATION_FOCUS_MULTIPLIER = 2.0; // 2x cultivation rate when focused
    private static final double QI_REGEN_BONUS_MULTIPLIER = 1.5; // 50% extra Qi regen when Qi-focused
    private static final double HEALTH_REGEN_BONUS_MULTIPLIER = 1.5; // 50% extra Health regen when Body-focused

    // Focus change cooldown
    private static final long FOCUS_SWITCH_COOLDOWN_TICKS = 6000;
    private long lastFocusSwitchTime = 0;
    private boolean onFocusSwitchCooldown = false;

    public PlayerCultivationFocusAttachment() {}

    // === Player Reference Management ===

    private Player findPlayer() {
        // Check client-side first
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get()) == this) {
                return clientPlayer;
            }
        }

        // Check server-side
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get()) == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("Could not find player for CultivationFocusAttachment");
        }
        return player;
    }

    // === Core Focus Management ===

    /**
     * Check if currently focused on Qi cultivation
     * @return true if Qi-focused, false if Body-focused
     */
    public boolean isFocusedOnQi() {
        return focusedOnQi;
    }

    /**
     * Check if currently focused on Body cultivation
     * @return true if Body-focused, false if Qi-focused
     */
    public boolean isFocusedOnBody() {
        return !focusedOnQi;
    }

    /**
     * Get the current focus type
     * @return The CultivationType currently focused on
     */
    public CultivationType getCurrentFocus() {
        return focusedOnQi ? CultivationType.QI : CultivationType.BODY;
    }

    /**
     * Check if player can switch focus (not on cooldown)
     * @return true if focus can be switched
     */
    public boolean canSwitchFocus() {
        if (!onFocusSwitchCooldown) return true;

        Player player = getPlayerSafe();
        if (player == null) return false;

        long currentTime = player.level().getGameTime();
        if (currentTime - lastFocusSwitchTime >= FOCUS_SWITCH_COOLDOWN_TICKS) {
            onFocusSwitchCooldown = false;
            return true;
        }

        return false;
    }

    /**
     * Get remaining cooldown ticks
     * @return Ticks until focus can be switched
     */
    public long getRemainingCooldownTicks() {
        if (!onFocusSwitchCooldown) return 0;

        Player player = getPlayerSafe();
        if (player == null) return 0;

        long currentTime = player.level().getGameTime();
        return Math.max(0, FOCUS_SWITCH_COOLDOWN_TICKS - (currentTime - lastFocusSwitchTime));
    }

    /**
     * Set focus to Qi cultivation
     */
    public void focusOnQi() {
        if (!focusedOnQi && canSwitchFocus()) {
            setFocus(true);
        } else if (!canSwitchFocus()) {
            notifyPlayerOfCooldown();
        }
    }

    /**
     * Set focus to Body cultivation
     */
    public void focusOnBody() {
        if (focusedOnQi && canSwitchFocus()) {
            setFocus(false);
        } else if (!canSwitchFocus()) {
            notifyPlayerOfCooldown();
        }
    }

    /**
     * Toggle between Qi and Body focus
     */
    public void toggleFocus() {
        if (canSwitchFocus()) {
            setFocus(!focusedOnQi);
        } else {
            notifyPlayerOfCooldown();
        }
    }

    /**
     * Notify player that focus switch is on cooldown
     */
    private void notifyPlayerOfCooldown() {
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide()) {
            long remainingSeconds = getRemainingCooldownTicks() / 20;
            ImmortalCultivation.LOGGER.debug("Player {} tried to switch focus but is on cooldown for {} more seconds",
                player.getName().getString(), remainingSeconds);
            // You can send a message to the player here
        }
    }

    /**
     * Internal method to change focus
     */
    private void setFocus(boolean qiFocus) {
        boolean oldFocus = this.focusedOnQi;
        this.focusedOnQi = qiFocus;

        // Reset focus duration tracking and apply cooldown
        if (oldFocus != qiFocus) {
            Player player = getPlayerSafe();
            if (player != null) {
                lastFocusChangeTime = player.level().getGameTime();
                lastFocusSwitchTime = player.level().getGameTime();
                onFocusSwitchCooldown = true;
                ticksInCurrentFocus = 0;
            }

            // Sync to client
            if (player != null && !player.level().isClientSide()) {
                syncToClient();
            }

            // Trigger change event
            onFocusChanged(oldFocus, qiFocus);
        }
    }

    // === Cultivation Multipliers ===

    /**
     * Get the Qi cultivation multiplier based on current focus
     * @return 2.0 if Qi-focused, 1.0 if Body-focused
     */
    public double getQiCultivationMultiplier() {
        return focusedOnQi ? CULTIVATION_FOCUS_MULTIPLIER : 1.0;
    }

    /**
     * Get the Body cultivation multiplier based on current focus
     * @return 2.0 if Body-focused, 1.0 if Qi-focused
     */
    public double getBodyCultivationMultiplier() {
        return focusedOnQi ? 1.0 : CULTIVATION_FOCUS_MULTIPLIER;
    }

    /**
     * Get the Qi regeneration bonus multiplier
     * @return 1.5 if Qi-focused, 1.0 if Body-focused
     */
    public double getQiRegenerationBonus() {
        return focusedOnQi ? QI_REGEN_BONUS_MULTIPLIER : 1.0;
    }

    /**
     * Get the Health regeneration bonus multiplier
     * @return 1.5 if Body-focused, 1.0 if Qi-focused
     */
    public double getHealthRegenerationBonus() {
        return focusedOnQi ? 1.0 : HEALTH_REGEN_BONUS_MULTIPLIER;
    }

    // === Focus Duration Tracking ===

    /**
     * Get how long the player has been in their current focus
     * @return Ticks in current focus
     */
    public long getTicksInCurrentFocus() {
        return ticksInCurrentFocus;
    }

    /**
     * Get when the focus was last changed
     * @return Game time of last focus change
     */
    public long getLastFocusChangeTime() {
        return lastFocusChangeTime;
    }

    // === Focus Change Event ===

    private void onFocusChanged(boolean wasQiFocused, boolean isNowQiFocused) {
        Player player = getPlayerSafe();
        if (player != null) {
            String oldFocus = wasQiFocused ? "Qi" : "Body";
            String newFocus = isNowQiFocused ? "Qi" : "Body";

            ImmortalCultivation.LOGGER.info("Player {} switched cultivation focus from {} to {}",
                player.getName().getString(), oldFocus, newFocus);

            // Apply immediate effects of focus change
            if (isNowQiFocused) {
                // Switching to Qi focus - could add visual effects
                ImmortalCultivation.LOGGER.debug("Applying Qi focus bonuses: 2x Qi cultivation, 1.5x Qi regeneration");
            } else {
                // Switching to Body focus - could add visual effects
                ImmortalCultivation.LOGGER.debug("Applying Body focus bonuses: 2x Body cultivation, 1.5x Health regeneration");
            }

            // Force sync cultivation attachments to apply new rates immediately
            try {
                PlayerQiCultivationAttachment qiCultivation = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
                PlayerBodyCultivationAttachment bodyCultivation = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());

                if (qiCultivation != null) qiCultivation.forceSync();
                if (bodyCultivation != null) bodyCultivation.forceSync();
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing cultivation attachments after focus change", e);
            }
        }
    }

    // === Tick Update ===

    public void tick() {
        tickCounter++;
        ticksInCurrentFocus++;

        Player player = getPlayerSafe();
        if (player == null) return;

        // Periodic sync check
        if (!player.level().isClientSide() && tickCounter % PERIODIC_SYNC_TICKS == 0) {
            if (focusedOnQi != lastSyncedFocus) {
                syncToClient();
            }
        }
    }

    // === Synchronization ===

    private void syncToClient() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedFocus = focusedOnQi;
            try {
                // Send 1 for Qi focus, 0 for Body focus
                long focusValue = focusedOnQi ? 1L : 0L;
                ICModNetwork.sendToPlayer(new CultivationFocusSyncPacket(focusValue), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing cultivation focus to client", e);
            }
        }
    }

    public void forceSync() {
        syncToClient();
    }

    /**
     * Update focus from server sync packet
     */
    public void updateFromServer(long focusValue) {
        boolean newFocus = focusValue > 0; // 1 = Qi, 0 = Body
        if (this.focusedOnQi != newFocus) {
            boolean oldFocus = this.focusedOnQi;
            this.focusedOnQi = newFocus;
            this.lastSyncedFocus = newFocus;
            onFocusChanged(oldFocus, newFocus);
        }
    }

    // === Serialization ===

    public void deserializeNBT(CompoundTag tag) {
        this.focusedOnQi = tag.getBoolean("focusedOnQi");
        this.ticksInCurrentFocus = tag.getLong("ticksInCurrentFocus");
        this.lastFocusChangeTime = tag.getLong("lastFocusChangeTime");
        this.lastFocusSwitchTime = tag.getLong("lastFocusSwitchTime");
        this.onFocusSwitchCooldown = tag.getBoolean("onFocusSwitchCooldown");
        this.lastSyncedFocus = tag.getBoolean("lastSyncedFocus");
        this.tickCounter = tag.getInt("tickCounter");
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("focusedOnQi", focusedOnQi);
        tag.putLong("ticksInCurrentFocus", ticksInCurrentFocus);
        tag.putLong("lastFocusChangeTime", lastFocusChangeTime);
        tag.putLong("lastFocusSwitchTime", lastFocusSwitchTime);
        tag.putBoolean("onFocusSwitchCooldown", onFocusSwitchCooldown);
        tag.putBoolean("lastSyncedFocus", lastSyncedFocus);
        tag.putInt("tickCounter", tickCounter);
        return tag;
    }

    // === Codec ===

    public static final Codec<PlayerCultivationFocusAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("focusedOnQi", true).forGetter(comp -> comp.focusedOnQi),
        Codec.LONG.optionalFieldOf("ticksInCurrentFocus", 0L).forGetter(comp -> comp.ticksInCurrentFocus),
        Codec.LONG.optionalFieldOf("lastFocusChangeTime", 0L).forGetter(comp -> comp.lastFocusChangeTime),
        Codec.LONG.optionalFieldOf("lastFocusSwitchTime", 0L).forGetter(comp -> comp.lastFocusSwitchTime),
        Codec.BOOL.optionalFieldOf("onFocusSwitchCooldown", false).forGetter(comp -> comp.onFocusSwitchCooldown),
        Codec.BOOL.optionalFieldOf("lastSyncedFocus", true).forGetter(comp -> comp.lastSyncedFocus),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter)
    ).apply(instance, (focused, ticks, changeTime, switchTime, onCooldown, lastSync, counter) -> {
        PlayerCultivationFocusAttachment comp = new PlayerCultivationFocusAttachment();
        comp.focusedOnQi = focused;
        comp.ticksInCurrentFocus = ticks;
        comp.lastFocusChangeTime = changeTime;
        comp.lastFocusSwitchTime = switchTime;
        comp.onFocusSwitchCooldown = onCooldown;
        comp.lastSyncedFocus = lastSync;
        comp.tickCounter = counter;
        return comp;
    }));

    // === Debug Utilities ===

    public void debugInfo() {
        Player player = getPlayerSafe();
        String side = player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown";
        String playerName = player != null ? player.getName().getString() : "Unknown";

        ImmortalCultivation.LOGGER.info("=== Cultivation Focus Debug Info ===");
        ImmortalCultivation.LOGGER.info("Player: {} ({})", playerName, side);
        ImmortalCultivation.LOGGER.info("Current Focus: {} Cultivation", focusedOnQi ? "Qi" : "Body");
        ImmortalCultivation.LOGGER.info("Time in Focus: {} ticks ({} seconds)",
            ticksInCurrentFocus, ticksInCurrentFocus / 20);
        ImmortalCultivation.LOGGER.info("Bonuses Active:");
        ImmortalCultivation.LOGGER.info("  - {} Cultivation: {}x rate",
            focusedOnQi ? "Qi" : "Body", CULTIVATION_FOCUS_MULTIPLIER);
        ImmortalCultivation.LOGGER.info("  - {} Regeneration: {}x rate",
            focusedOnQi ? "Qi" : "Health", focusedOnQi ? QI_REGEN_BONUS_MULTIPLIER : HEALTH_REGEN_BONUS_MULTIPLIER);
        ImmortalCultivation.LOGGER.info("Sync Status: {}", lastSyncedFocus == focusedOnQi ? "Synced" : "Pending");
    }
}