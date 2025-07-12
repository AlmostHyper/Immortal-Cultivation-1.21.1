package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.IDebug;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Abstract base class for all player-attached data with comprehensive debugging support.
 * Provides generic debugging capabilities with type-specific information, focusing on
 * network synchronization, performance monitoring, and data integrity validation.
 *
 * @param <T> The specific attachment type extending this class
 */
public abstract class PlayerAttachment<T extends PlayerAttachment<T>> implements IAttachmentHolder, ISyncable, IDebug  {

    // === CORE FIELDS === //

    @Nullable
    protected transient Player player;
    protected transient boolean isDirty = false;
    @Nullable
    protected transient T lastSyncedState = null;

    // === DEBUG FIELDS === //

    private transient boolean debugEnabled = true;
    private transient final Map<String, Long> performanceTimers = new ConcurrentHashMap<>();
    private transient long lastSyncTime = 0L;
    private transient long syncCount = 0L;
    private transient long errorCount = 0L;
    private transient String lastError = null;
    private transient long lastStateChangeTime = System.currentTimeMillis();

    // === PLAYER REFERENCE MANAGEMENT === //

    /**
     * Sets the player reference and triggers debug logging if enabled.
     * @param player The player to associate with this attachment
     */
    public void setPlayer(Player player) {
        if (debugEnabled) {
            debugLog("Setting player reference: {} -> {}",
                this.player != null ? this.player.getName().getString() : "NULL",
                player != null ? player.getName().getString() : "NULL");
        }

        this.player = player;
        onPlayerSet(player);

        if (debugEnabled && player != null) {
            debugLog("Player reference set successfully for {}", player.getName().getString());
        }
    }

    /**
     * Gets the current player reference.
     * @return The associated player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Called when a player is set. Override for specific behavior.
     * @param player The player that was set
     */
    protected void onPlayerSet(Player player) {}

    // === DIRTY FLAG MANAGEMENT === //

    @Override
    public boolean isDirty() {
        startPerformanceTracking("isDirty");
        try {
            // Automatic dirty checking by comparing with last synced state
            if (lastSyncedState == null) {
                if (debugEnabled) {
                    debugLog("Attachment is dirty: no last synced state");
                }
                return true;
            }

            boolean dirty = !this.isEqualTo(lastSyncedState);
            if (debugEnabled && dirty) {
                debugLog("Attachment is dirty: state changed");
            }
            return dirty;
        } finally {
            endPerformanceTracking("isDirty", 1); // 1ms warning threshold
        }
    }

    @Override
    public void markDirty() {
        if (debugEnabled) {
            debugLog("Marking attachment as dirty");
        }

        this.isDirty = true;
        this.lastStateChangeTime = System.currentTimeMillis();

        // Auto-schedule sync if we have a server player
        if (this.player instanceof ServerPlayer serverPlayer) {
            if (debugEnabled) {
                debugLog("Scheduling sync for server player: {}", serverPlayer.getName().getString());
            }
            // Integration point with your NetworkSyncManager
        }
    }

    @Override
    public void clearDirty() {
        if (debugEnabled) {
            debugLog("Clearing dirty flag and updating last synced state");
        }

        this.isDirty = false;
        this.lastSyncedState = this.copy();
        this.lastSyncTime = System.currentTimeMillis();
    }

    // === SERIALIZATION === //

    protected abstract Codec<T> getCodec();
    protected abstract ISyncPacket createSyncPacket(CompoundTag data);

    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        startPerformanceTracking("createPacket");

        try {
            if (player == null) {
                if (debugEnabled) {
                    debugWarn("Cannot create packet: no player reference");
                }
                return Optional.empty();
            }

            if (!isFullSync && !isDirty()) {
                if (debugEnabled) {
                    debugLog("Skipping packet creation: not dirty and not full sync");
                }
                return Optional.empty();
            }

            CompoundTag data = serialize();
            if (data.isEmpty()) {
                if (debugEnabled) {
                    debugWarn("Skipping packet creation: empty serialized data");
                }
                return Optional.empty();
            }

            ISyncPacket packet = createSyncPacket(data);
            onSyncPacketCreated(data, isFullSync, player);

            syncCount++;
            return Optional.of(packet);

        } catch (Exception e) {
            onSyncError("Failed to create sync packet: " + e.getMessage(), player);
            return Optional.empty();
        } finally {
            endPerformanceTracking("createPacket", 5); // 5ms warning threshold
        }
    }

    /**
     * Serializes this attachment to NBT.
     * @return CompoundTag containing the serialized data
     */
    public CompoundTag serialize() {
        startPerformanceTracking("serialize");

        try {
            return (CompoundTag) getCodec()
                .encodeStart(NbtOps.INSTANCE, (T) this)
                .getOrThrow();
        } catch (Exception e) {
            onSyncError("Serialization failed: " + e.getMessage(), player);
            throw e;
        } finally {
            endPerformanceTracking("serialize", 2); // 2ms warning threshold
        }
    }

    /**
     * Deserializes NBT data into this attachment.
     * @param tag The NBT data to deserialize
     */
    public void deserialize(CompoundTag tag) {
        startPerformanceTracking("deserialize");

        try {
            getCodec().parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> {
                    String errorMsg = "Failed to decode " + getDebugTypeName() + ": " + error;
                    ImmortalCultivation.LOGGER.error(errorMsg);
                    onSyncError(errorMsg, player);
                })
                .ifPresent(decoded -> this.copyFrom(decoded));
        } finally {
            endPerformanceTracking("deserialize", 2); // 2ms warning threshold
        }
    }

    /**
     * Applies synchronized data from a network packet.
     * @param data The NBT data from the sync packet
     */
    public void applySync(CompoundTag data) {
        startPerformanceTracking("applySync");

        try {
            deserialize(data);
            clearDirty(); // Important: mark as clean after sync
            onSyncPacketApplied(data, player);
        } finally {
            endPerformanceTracking("applySync", 3); // 3ms warning threshold
        }
    }

    // === COPY FUNCTIONALITY === //

    /**
     * Creates a deep copy of this attachment.
     * @return A new instance with the same data
     */
    public abstract T copy();

    /**
     * Copies data from another attachment instance.
     * @param other The attachment to copy from
     */
    protected abstract void copyFrom(T other);

    /**
     * Checks if this attachment is equal to another instance.
     * @param other The attachment to compare with
     * @return True if they are equal
     */
    protected abstract boolean isEqualTo(T other);

    // === COMMON SERIALIZER === //

    /**
     * Base serializer with debug support for PlayerAttachment subclasses.
     * @param <T> The specific attachment type
     */
    public static class BaseSerializer<T extends PlayerAttachment<T>> implements IAttachmentSerializer<CompoundTag, T> {
        private final Supplier<T> factory;
        private final Codec<T> codec;

        public BaseSerializer(Supplier<T> factory, Codec<T> codec) {
            this.factory = factory;
            this.codec = codec;
        }

        @Override
        public T read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            T attachment = factory.get();

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            if (attachment.debugEnabled()) {
                attachment.debugLog("Reading attachment from NBT - Size: {} bytes", tag.toString().length());
            }

            try {
                attachment.deserialize(tag);
            } catch (Exception e) {
                attachment.onSyncError("Failed to read attachment: " + e.getMessage(),
                    holder instanceof Player player ? player : null);
            }

            return attachment;
        }

        @Override
        public CompoundTag write(T attachment, HolderLookup.Provider provider) {
            if (attachment.debugEnabled()) {
                attachment.debugLog("Writing attachment to NBT");
            }

            try {
                return attachment.serialize();
            } catch (Exception e) {
                attachment.onSyncError("Failed to write attachment: " + e.getMessage(), attachment.getPlayer());
                return new CompoundTag(); // Return empty tag on error
            }
        }
    }

    // === DEBUG === //

    @Override
    public boolean debugEnabled() {
        return debugEnabled;
    }

    @Override
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        if (enabled) {
            debugLog("Debug logging enabled for {}", getDebugTypeName());
        }
    }

    @Override
    public String getDebugTypeName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getDebugShortName() {
        // Extract short name from class name (e.g., PlayerCultivationAttachment -> CULT)
        String className = getDebugTypeName();
        if (className.startsWith("Player") && className.endsWith("Attachment")) {
            String middle = className.substring(6, className.length() - 10);
            return middle.toUpperCase().substring(0, Math.min(4, middle.length()));
        }
        return className.substring(0, Math.min(4, className.length())).toUpperCase();
    }

    @Override
    public Map<String, Object> getSyncDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("isDirty", isDirty());
        info.put("hasPlayer", player != null);
        info.put("playerName", player != null ? player.getName().getString() : "NULL");
        info.put("lastSyncTime", lastSyncTime);
        info.put("syncCount", syncCount);
        info.put("timeSinceLastSync", System.currentTimeMillis() - lastSyncTime);
        info.put("hasLastSyncedState", lastSyncedState != null);
        return info;
    }

    @Override
    public void onSyncPacketCreated(CompoundTag packetData, boolean isFullSync, @Nullable Player player) {
        if (debugEnabled) {
            debugLog("Sync packet created - Size: {} bytes, FullSync: {}, Player: {}",
                packetData.toString().length(),
                isFullSync,
                player != null ? player.getName().getString() : "NULL");
        }
    }

    @Override
    public void onSyncPacketApplied(CompoundTag packetData, @Nullable Player player) {
        if (debugEnabled) {
            debugLog("Sync packet applied - Size: {} bytes, Player: {}",
                packetData.toString().length(),
                player != null ? player.getName().getString() : "NULL");
        }
    }

    @Override
    public void onSyncError(String error, @Nullable Player player) {
        this.lastError = error;
        this.errorCount++;

        if (debugEnabled) {
            debugError("Sync error for player {}: {}",
                null,
                player != null ? player.getName().getString() : "NULL",
                error);
        }
    }

    @Override
    public Map<String, Object> getPerformanceDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("activeTimers", performanceTimers.size());
        info.put("lastStateChangeTime", lastStateChangeTime);
        info.put("errorCount", errorCount);
        info.put("lastError", lastError);

        // Add timing statistics if available
        for (Map.Entry<String, Long> entry : performanceTimers.entrySet()) {
            info.put("timer_" + entry.getKey(), System.currentTimeMillis() - entry.getValue());
        }

        return info;
    }

    @Override
    public void startPerformanceTracking(String operationName) {
        if (debugEnabled) {
            performanceTimers.put(operationName, System.nanoTime());
        }
    }

    @Override
    public void endPerformanceTracking(String operationName, long warnThresholdMs) {
        if (debugEnabled) {
            Long startTime = performanceTimers.remove(operationName);
            if (startTime != null) {
                long duration = System.nanoTime() - startTime;
                long durationMs = duration / 1_000_000;

                if (durationMs > warnThresholdMs) {
                    debugWarn("Operation '{}' took {}ms (exceeds {}ms threshold)",
                        operationName, durationMs, warnThresholdMs);
                } else {
                    debugLog("Operation '{}' completed in {}ms", operationName, durationMs);
                }
            }
        }
    }

    @Override
    public Map<String, Object> getStateDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("isDirty", isDirty);
        info.put("hasLastSyncedState", lastSyncedState != null);
        info.put("lastStateChangeTime", lastStateChangeTime);

        // Subclasses should override this to add specific state information
        addSpecificStateDebugInfo(info);

        return info;
    }

    /**
     * Override this method to add type-specific state debug information.
     * @param info The map to add debug information to
     */
    protected void addSpecificStateDebugInfo(Map<String, Object> info) {
        // Default implementation - subclasses should override
    }

    @Override
    public Map<String, String> validateState() {
        Map<String, String> issues = new HashMap<>();

        // Basic validation
        if (player == null) {
            issues.put("player", "Player reference is null");
        }

        // Subclasses should override this to add specific validation
        addSpecificStateValidation(issues);

        return issues;
    }

    /**
     * Override this method to add type-specific state validation.
     * @param issues The map to add validation issues to
     */
    protected void addSpecificStateValidation(Map<String, String> issues) {
        // Default implementation - subclasses should override
    }

    @Override
    public String getStateHash() {
        // Simple hash implementation - can be improved with better algorithms
        CompoundTag serialized = serialize();
        return String.valueOf(serialized.toString().hashCode());
    }

    @Override
    public void debugLog(String message, Object... args) {
        if (debugEnabled) {
            ImmortalCultivation.LOGGER.debug("[{}] {}", getDebugShortName(), String.format(message, args));
        }
    }

    @Override
    public void debugWarn(String message, Object... args) {
        if (debugEnabled) {
            ImmortalCultivation.LOGGER.warn("[{}] {}", getDebugShortName(), String.format(message, args));
        }
    }

    @Override
    public void debugError(String message, @Nullable Throwable throwable, Object... args) {
        if (debugEnabled) {
            String formattedMessage = String.format("[%s] %s", getDebugShortName(), String.format(message, args));
            if (throwable != null) {
                ImmortalCultivation.LOGGER.error(formattedMessage, throwable);
            } else {
                ImmortalCultivation.LOGGER.error(formattedMessage);
            }
        }
    }
}
