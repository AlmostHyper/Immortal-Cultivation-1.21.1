package net.hyper.immortalcultivation.core.interfaces;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Enhanced debugging interface for comprehensive monitoring of attachments.
 * Provides generic debugging capabilities with type-specific information,
 * focusing on network synchronization, performance, and data integrity.
 */
public interface IDebug {

    // === BASIC DEBUG STATE === //

    /**
     * Determines whether debug logging is enabled for this component.
     * @return True if debug logging should occur
     */
    boolean debugEnabled();

    /**
     * Sets the debug state for this component.
     * @param enabled Whether to enable debug logging
     */
    void setDebugEnabled(boolean enabled);

    // === TYPE INFORMATION === //

    /**
     * Gets the specific type name for this debuggable component.
     * This should return a human-readable identifier for logging.
     * @return The type identifier (e.g., "PlayerCultivationAttachment")
     */
    String getDebugTypeName();

    /**
     * Gets a short identifier for this component type for concise logging.
     * @return Short identifier (e.g., "CULT", "TALENTS", "RESOURCES")
     */
    String getDebugShortName();

    // === SYNCHRONIZATION DEBUG === //

    /**
     * Gets detailed information about the current sync state.
     * @return Map containing sync-related debug information
     */
    Map<String, Object> getSyncDebugInfo();

    /**
     * Called when a sync packet is created. Allows for logging sync details.
     * @param packetData The NBT data being synchronized
     * @param isFullSync Whether this is a full synchronization
     * @param player The player being synchronized (null if not available)
     */
    void onSyncPacketCreated(CompoundTag packetData, boolean isFullSync, @Nullable Player player);

    /**
     * Called when a sync packet is received and applied.
     * @param packetData The NBT data that was applied
     * @param player The player receiving the sync
     */
    void onSyncPacketApplied(CompoundTag packetData, @Nullable Player player);

    /**
     * Called when sync fails or encounters an error.
     * @param error The error message or exception details
     * @param player The player involved (null if not available)
     */
    void onSyncError(String error, @Nullable Player player);

    // === PERFORMANCE DEBUG === //

    /**
     * Gets performance metrics for this component.
     * @return Map containing performance-related information
     */
    Map<String, Object> getPerformanceDebugInfo();

    /**
     * Records the start time of an operation for performance tracking.
     * @param operationName The name of the operation being tracked
     */
    void startPerformanceTracking(String operationName);

    /**
     * Records the end time of an operation and logs if it exceeds thresholds.
     * @param operationName The name of the operation that finished
     * @param warnThresholdMs Warning threshold in milliseconds
     */
    void endPerformanceTracking(String operationName, long warnThresholdMs);

    // === DATA INTEGRITY DEBUG === //

    /**
     * Gets detailed information about the current state for debugging.
     * This should include key fields and their current values.
     * @return Map containing state information
     */
    Map<String, Object> getStateDebugInfo();

    /**
     * Validates the internal state and returns any issues found.
     * @return Map of validation issues (empty if no issues)
     */
    Map<String, String> validateState();

    /**
     * Gets a hash or checksum of the current state for comparison.
     * Useful for detecting unexpected state changes.
     * @return State hash as a string
     */
    String getStateHash();

    // === UTILITY METHODS === //

    /**
     * Logs a debug message if debugging is enabled for this component.
     * @param message The message to log
     * @param args Optional arguments for message formatting
     */
    void debugLog(String message, Object... args);

    /**
     * Logs a warning message if debugging is enabled for this component.
     * @param message The warning message to log
     * @param args Optional arguments for message formatting
     */
    void debugWarn(String message, Object... args);

    /**
     * Logs an error message if debugging is enabled for this component.
     * @param message The error message to log
     * @param throwable Optional throwable for stack trace
     * @param args Optional arguments for message formatting
     */
    void debugError(String message, @Nullable Throwable throwable, Object... args);
}
